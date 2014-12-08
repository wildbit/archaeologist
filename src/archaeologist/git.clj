(ns archaeologist.git
  (:require [archaeologist.core :refer :all]
            [archaeologist.utils :refer :all]
            [clojure.java.io :refer [file]])
  (:import [org.eclipse.jgit.lib Repository Ref RepositoryCache$FileKey ObjectId ObjectLoader]
           [org.eclipse.jgit.revwalk RevWalk RevCommit]
           [org.eclipse.jgit.treewalk TreeWalk]
           org.eclipse.jgit.storage.file.FileRepositoryBuilder
           org.eclipse.jgit.treewalk.filter.PathFilter
           [org.eclipse.jgit.util FS]
           java.io.ByteArrayOutputStream))

(defn open-repository
  "Open a repository at given path."
  ^Repository [path]
  (FileRepositoryBuilder/create (file path)))

(defn repository?
  "Guess if a given path is a Git repository."
  [path]
  (RepositoryCache$FileKey/isGitRepository (file path) FS/DETECTED))

(defn- load-commit
  "Loads a commit object into memory."
  ^RevCommit [^Repository repo commit]
  (when-let [obj-id ^Ref (.resolve repo commit)]
    (.parseCommit (RevWalk. repo) obj-id)))

(defn- new-tree-walk
  "Create a new TreeWalk instance (mutable), automatically enters
   subtrees if a given path is a subtree."
  (^TreeWalk [^Repository repo ^RevCommit rev-commit]
    (doto (TreeWalk. repo)
      (.addTree (.getTree rev-commit))))
  (^TreeWalk [^Repository repo ^String path ^RevCommit rev-commit]
    (if (or (nil? path) (empty? path))
      (new-tree-walk repo rev-commit)
      (let [walk (TreeWalk/forPath repo path (.getTree rev-commit))]
        (when (.isSubtree walk) (.enterSubtree walk))
        walk))))

(defn- map-tree-walk
  "Eagerly traverse a Git tree building a vector of f applied to each node."
  [f depth ^TreeWalk walk]
  (let [depth (and depth (+ (.getDepth walk) depth))]
    (.setRecursive walk false)
    (loop [res-seq []]
      (if (and (or (nil? depth) (> depth 0)) (.next walk))
        (let [next-val (f walk)]
          (when (and (.isSubtree walk) (or (nil? depth) (> depth (inc (.getDepth walk)))))
            (.enterSubtree walk))
          (recur (conj res-seq next-val)))
        res-seq))))

(defn- find-object-id
  "Traverse the git tree looking for a file."
  ^ObjectId [^TreeWalk walk file-path]
  (let [path-filter (PathFilter/create file-path)
        walk (doto walk
               (.setRecursive true)
               (.setFilter path-filter))]
    (when (.next walk)
      (.getObjectId walk 0))))

(defn- read-object
  "Read an object from the repo."
  ^bytes [^Repository repo ^ObjectId object-id]
  (let [loader ^ObjectLoader (.open repo object-id)
        out-stream (ByteArrayOutputStream.)]
    (.copyTo loader out-stream)
    (.toByteArray out-stream)))

(defn- TreeWalk->artifact
  "Extracts a archaeologist's artifact out of current TreeWalk's position."
  [^TreeWalk walk]
  (->artifact (if (.isSubtree walk) "dir" "file")
              (String. (.getRawPath walk) "UTF-8")))

(extend-type Repository
  Archaeology
  (list-files
    ([repo commit] (list-files repo commit nil nil))
    ([repo commit path] (list-files repo commit path nil))
    ([repo commit path depth]
      (if-let [commit (load-commit repo commit)]
        (let [walk (new-tree-walk repo path commit)
              artifacts (map-tree-walk TreeWalk->artifact depth walk)]
          (mapv #(update-in % [:path] relativize-path (or path "")) artifacts))
        (throw (UnsupportedOperationException. "Can't list files for a non-existent commit.")))))

  (read-file
    ([repo commit file-path] (read-file repo commit nil file-path))
    ([repo commit path file-path]
     (let [commit (load-commit repo commit)
           walk (new-tree-walk repo path commit)]
       (when-let [obj-id (find-object-id walk (join-paths path file-path))]
         (read-object repo obj-id)))))

  (get-default-version [_] "HEAD")

  (close [repo] (.close repo)))


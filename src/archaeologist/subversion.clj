(ns archaeologist.subversion
  (:require [archaeologist.core :refer :all]
            [clojure.java.io :refer [file]]
            [archaeologist.utils :refer :all])
  (:import [org.tmatesoft.svn.core.io SVNRepository]
           [org.tmatesoft.svn.core SVNDirEntry SVNURL SVNDepth SVNNodeKind]
           [org.tmatesoft.svn.core.wc SVNRevision]
           [org.tmatesoft.svn.core.wc2 SvnList SvnOperationFactory SvnTarget ISvnObjectReceiver]
           [org.tmatesoft.svn.core SVNException]
           org.tmatesoft.svn.core.io.SVNRepositoryFactory
           java.io.ByteArrayOutputStream))

(declare ^{:dynamic true :tag SvnOperationFactory} *operation-factory*)

(defmacro ^:private bind-operation-factory
  "Creates a thread-local instance of SvnOperationFactory that is guaranteed to be disposed
   after the body is executed."
  [& body]
  `(binding [*operation-factory* (SvnOperationFactory.)]
    (try
      ~@body
      (finally (.dispose *operation-factory*)))))

(defn open-repository
  "Opens a Subversion repository at a given path."
  ^SVNRepository [path]
  (SVNRepositoryFactory/create (->> path to-file-uri SVNURL/parseURIDecoded)))

(defn repository?
  "Tries to guess if a given path contains a Subversion repository."
  [path]
  (try
    (doto (open-repository path) .testConnection)
    true
    (catch SVNException _ false)))

(defn- revision->SVNRevision
  "Convert numeric revision to SVNRevision instance."
  ^SVNRevision [^long revision]
  (SVNRevision/create revision))

(defn- depth->SVNDepth
  "Convert numeric depth to SVNDepth. Pass nil for infinite depth."
  ^SVNDepth [depth]
  (cond 
    (nil? depth) SVNDepth/INFINITY
    (zero? depth) SVNDepth/EMPTY
    :else SVNDepth/IMMEDIATES))

(defn- ->SVNList
  "Create an SVNList instance for a repo poiting to path at specified revision."
  ^SvnList [^SVNRepository repo revision path]
  (let [url (.appendPath (.getLocation repo) path false)]
    (doto ^SvnList (.createList *operation-factory*)
      (.setSingleTarget ^SvnTarget (SvnTarget/fromURL url (revision->SVNRevision revision)))
      (.setRevision (revision->SVNRevision revision)))))

(defn- run-SVNList
  "Runs an SVNList instance with specified numeric depth."
  [^SvnList l depth]
  (let [result (transient [])]
    (doto l
      (.setDepth (depth->SVNDepth depth))
      (.setReceiver (reify ISvnObjectReceiver
                      (receive [_ _ entry]
                        (when-not (empty? (.getRelativePath ^SVNDirEntry entry))
                          (conj! result entry)))))
      (.run))
    (persistent! result)))

(defn- load-files
  "Loads a list of files for a repo located at path limited by given depth."
  [repo revision path depth]
  (let [svn-list (->SVNList repo revision path)
        result (run-SVNList svn-list depth)]
    (apply concat
           result
           (when (and depth (> depth 1))
             (doall (map (fn [^SVNDirEntry entry]
                           (when (= SVNNodeKind/DIR (.getKind entry))
                             (load-files repo
                                         revision
                                         (join-paths path (.getRelativePath entry))
                                         (dec depth))))
                         result))))))

(defn- read-file-bytes
  "Read bytes of a file existed in given repo for given revision at given path."
  ^bytes [^SVNRepository repo ^long revision path]
  (try
    (let [out-stream (ByteArrayOutputStream.)]
      (.getFile repo path revision nil out-stream)
      (.toByteArray out-stream))
    (catch SVNException _ nil)))

(defn- expand-path
  "Get full path for a given SVNDirEntry instance."
  [^SVNDirEntry entry]
  (let [url ^SVNURL (.getURL entry)]
    (.getPath url)))

(defn- get-repository-path
  "Get full path for a given SVNRepository instance."
  [^SVNRepository repo]
  (let [url ^SVNURL (.getRepositoryRoot repo false)]
    (.getPath url)))

(defn- SVNDirEntry->artifact
  "Transform a given SVNDirEntry into a pathologist's artifact with appropriate
   kind and path without specified prefix."
  [^SVNDirEntry entry]
  (->artifact (.. entry getKind toString)
              (expand-path entry)))

(extend-type SVNRepository
  Archaeology
  (list-files
    ([repo revision] (list-files repo revision nil nil))
    ([repo revision path] (list-files repo revision path nil))
    ([repo ^String revision path depth]
     (bind-operation-factory
       (let [revision (Integer/parseInt revision)]
         (if (<= revision (.getLatestRevision repo))
           (let [prefix (join-paths (get-repository-path repo) path)
                 files (load-files repo revision path depth)]
             (->> files
                  (map SVNDirEntry->artifact)
                  (mapv #(update-in % [:path] relativize-path prefix))))
           (throw (UnsupportedOperationException. "Can't list files for a non-existent revision.")))))))

  (read-file
    ([repo revision file-path] (read-file repo revision "" file-path))
    ([repo revision path file-path]
     (read-file-bytes repo
                      (Long/parseLong revision)
                      (join-paths path file-path))))

  (get-default-version [repo]
    (str (.getLatestRevision repo)))

  (close [repo] (.closeSession repo)))


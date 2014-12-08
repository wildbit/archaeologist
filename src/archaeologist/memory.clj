(ns archaeologist.memory
  (:require [archaeologist.core :refer :all]
            [archaeologist.utils :refer :all]
            [clojure.string :as string])
  (:import java.util.regex.Pattern))

(defn extract-artifacts
  "Extracts all dir and file artifacts from a given path."
  [path]
  (let [parts (re-seq #"(?<=^|\/)[^\/]+(?=$|\/)" path)
        ancestors (map #(take % parts) (range 1 (count parts)))]
    (conj (map #(->Artifact "dir" (string/join "/" %)) ancestors)
          (->Artifact "file" path))))

(defn descend-into
  [path {akind :kind apath :path :as artifact}]
  (if path
    (let [pattern (re-pattern (str "^" (Pattern/quote (trailing-slash path))))]
      (when (re-find pattern apath)
        (->Artifact akind (string/replace apath pattern ""))))
    artifact))

(defn deeper-than?
  [depth {path :path}]
  (and depth (> (get-depth path) depth)))

(defrecord Repository [files]
  Archaeology
  (list-files [repo _] (list-files repo nil nil nil))
  (list-files [repo _ path] (list-files repo nil path nil))
  (list-files [repo _ path depth]
    (->> files
         (mapcat (comp extract-artifacts key))
         (into #{})
         (keep #(descend-into path %))
         (remove #(deeper-than? depth %))
         (into [])))

  (read-file [repo _ file-path] (read-file repo nil nil file-path))
  (read-file [repo _ path file-path]
    (if-let [f (get files (join-paths path file-path))]
      (if (string? f) (.getBytes ^String f) f)))
  
  (get-default-version [_] nil)

  (close [repo] nil))

(def open-repository ->Repository)


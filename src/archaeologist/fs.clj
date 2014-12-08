(ns archaeologist.fs
  (:require [archaeologist.core :refer :all]
            [archaeologist.utils :refer :all]
            [clojure.java.io :refer [file]])
  (:import java.io.File))

(defn hidden?
  "Returns true if a given path is hidden. Any file that is hidden itself or contained
   inside a hidden directory is considdered hidden."
  [path]
  (boolean (re-find #"(^|\/)\." (.toString path))))

(defn deeper-than?
  "Returns true if a file is located deeper than a given depth. Pass nil to allow
   for infinite depth."
  [depth path]
  (and depth (> (get-depth path) depth)))

(defn File->artifact
  "Transforms a java.io.File into an archaeologist's artifact."
  [^File f]
  (->artifact (if (.isDirectory f) "dir" "file")
              (.toString f)))

(defrecord Repository [path]
  Archaeology
  (list-files [repo _] (list-files repo nil nil nil))
  (list-files [repo _ path] (list-files repo nil path nil))
  (list-files [repo _ path depth]
   (let [ls-path (join-paths (:path repo) path)
         files (-> ls-path file file-seq rest)]
     (->> files
          (map File->artifact)
          (map #(update-in % [:path] relativize-path ls-path))
          (remove #(or (hidden? (:path %)) (deeper-than? depth (:path %))))
          (into []))))

  (read-file [repo _ file-path]
    (read-file repo nil "" file-path))
  (read-file [repo _ path file-path]
    (let [^String string (-> repo :path (file path file-path) slurp)]
      (.getBytes string)))

  (get-default-version [_] nil)

  (close [_] nil))
 
(def open-repository ->Repository)


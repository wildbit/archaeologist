(ns archaeologist.utils
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import java.io.File
           [java.nio.file Path Paths]
           java.net.URI))

(defn join-paths
  [& paths]
  (.toString ^File (apply io/file (remove nil? paths))))

(defn get-depth
  [path]
  (inc (count (filter #{\/} (str path)))))

(defn no-trailing-slash
  [path]
  (string/replace path #"\/+(?=$)" ""))

(defn trailing-slash
  [path]
  (string/replace path #"([^\/])\/*$" "$1/"))

(defn path->Path
  [path]
  (Paths/get path (into-array String [])))

(defn relativize-path
  [^String file ^String base]
  (let [^Path base-path (path->Path base)
        ^Path file-path (path->Path file)
        ^Path relative-path (.relativize base-path file-path)]
    (.toString relative-path)))

(defn to-file-uri
  [path]
  (let [^File file (io/file path)
        ^Path path (.toPath file)
        ^Path absolute-path (.toAbsolutePath path)
        ^URI uri (.toUri absolute-path)]
    (.toString uri)))


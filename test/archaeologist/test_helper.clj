(ns archaeologist.test-helper
  (:require [archaeologist.core :refer :all]
            [clojure.test :refer :all]
            [clojure.string :as string]))

(defmacro setup-fn [*var* open-fn]
  `(fn [f#]
     (with-repository [repo# (~open-fn)]
       (binding [~*var* repo#]
         (f#)))))

(defn list-files-test [repo version]
  (testing "list files"
    (let [files (list-files repo version)]
      (is (vector? files))
      (is (= #{"a" "b" "c" "c/d" "c/e" "c/f" "c/f/g"}
             (->> files (map :path) (into #{}))))
      (is (= {"file" 5 "dir" 2}
             (->> files (map :kind) frequencies))))))

(defn list-files-depth-test [repo version]
  (testing "list files with limited depth"
    (let [files-0 (list-files repo version nil 0)
          files-1 (list-files repo version nil 1)
          files-2 (list-files repo version nil 2)
          files-3 (list-files repo version nil 3)]
      (is (empty? files-0))
      (is (= #{"a" "b" "c"}
             (->> files-1 (map :path) (into #{}))))
      (is (= #{"a" "b" "c" "c/d" "c/e" "c/f"}
             (->> files-2 (map :path) (into #{}))))
      (is (= #{"a" "b" "c" "c/d" "c/e" "c/f" "c/f/g"}
             (->> files-3 (map :path) (into #{})))))))

(defn list-files-path-test [repo version]
  (testing "list files in path"
    (let [files (list-files repo version "c")]
      (is (= #{"d" "e" "f" "f/g"}
             (->> files (map :path) (into #{})))))))

(defn list-files-path+depth-test [repo version]
  (testing "list files in path with limited depth"
    (let [files (list-files repo version "c" 1)]
      (is (= #{"d" "e" "f"}
             (->> files (map :path) (into #{})))))))

(defn read-file-test [repo version]
  (testing "read file"
    (let [files ["a" "b" "c/d" "c/e" "c/f/g"]]
      (doseq [file files]
        (let [expected-content (last (string/split file #"\/"))
              file-bytes (read-file repo version file)]
          (is (and file-bytes
                   (= expected-content
                      (string/trim (String. ^bytes file-bytes))))))))))

(defn read-file-path-test [repo version]
  (testing "read file in path"
    (let [files ["d" "e" "f/g"]]
      (doseq [file files]
        (let [expected-content (last (string/split file #"\/"))
              file-bytes (read-file repo version "c" file)]
          (is (and file-bytes
                   (= expected-content
                      (string/trim (String. ^bytes file-bytes))))))))))


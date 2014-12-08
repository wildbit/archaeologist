(ns archaeologist.memory-test
  (:require [clojure.test :refer :all]
            [archaeologist.core :refer :all]
            [archaeologist.memory :refer :all]
            [archaeologist.test-helper :refer :all]))

(def structure {"a" "a"
                "b" "b"
                "c/d" "d"
                "c/e" "e"
                "c/f/g" "g"})
(declare ^:dynamic *repository*)
(use-fixtures :once (setup-fn *repository* #(open-repository structure)))

(deftest memory-repository-test
  (let [version (get-default-version *repository*)]
    (list-files-test *repository* version)
    (list-files-depth-test *repository* version)
    (list-files-path-test *repository* version)
    (list-files-path+depth-test *repository* version)
    (read-file-test *repository* version)
    (read-file-path-test *repository* version)))


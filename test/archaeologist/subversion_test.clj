(ns archaeologist.subversion-test
  (:require [clojure.test :refer :all]
            [archaeologist.test-helper :refer :all]
            [archaeologist.core :refer :all]
            [archaeologist.subversion :refer :all]))

(declare ^:dynamic *repository*)
(use-fixtures :once (setup-fn *repository* #(open-repository "test/fixtures/svn")))

(deftest subversion-repository-test
  (let [version (get-default-version *repository*)]
    (list-files-test *repository* version)
    (list-files-depth-test *repository* version)
    (list-files-path-test *repository* version)
    (list-files-path+depth-test *repository* version)
    (read-file-test *repository* version)
    (read-file-path-test *repository* version)
    (read-file-missing-test *repository* version)))


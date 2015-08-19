(ns archaeologist.utils-test
  (:require [clojure.test :refer :all]
            [archaeologist.test-helper :refer :all]
            [archaeologist.utils :refer :all]))

(deftest join-paths-test
  (is (= "foo" (join-paths "foo")))
  (is (= "foo" (join-paths "" "foo") (join-paths "foo" "")))
  (is (= "foo/bar" (join-paths "foo" "bar")))
  (is (= "foo/bar/buz" (join-paths "foo" "bar/buz") (join-paths "foo/bar" "buz"))))


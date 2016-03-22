(defproject archaeologist "0.1.4"
  :description "An unified interface for reading versioned directories at a specific moment of time."
  :url "http://github.com/wildbit/archaeologist"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.tmatesoft.svnkit/svnkit "1.8.11"]
                 [org.eclipse.jgit/org.eclipse.jgit "3.7.1.201504261725-r"]]
  :profiles {:1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :aliases {"test-all" ["with-profile" "+1.6:+1.7:+1.8" "test"]})

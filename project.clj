(defproject archaeologist "0.1.1"
  :description "An interface for reading versioned directories at a specific moment of time."
  :url "http://github.com/wildbit/archaeologist"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.tmatesoft.svnkit/svnkit "1.8.5" ]
                 [org.eclipse.jgit/org.eclipse.jgit "3.5.0.201409260305-r"]])

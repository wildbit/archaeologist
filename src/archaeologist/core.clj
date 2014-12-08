(ns archaeologist.core)

(defrecord
  ^{:doc "A file or a directory found by archaeologist."}
  Artifact
  [kind path])

(defn ->artifact
  [kind path]
  {:pre [(contains? #{"dir" "file"} kind)
         (string? path)
         (not (empty? path))]}
  {:kind kind :path path})

(defprotocol Archaeology
  "Protocol for working with repositories."
  (list-files [repo version] [repo version path] [repo version path depth]
    "List directory tree of a given repo.")

  (read-file [repo version file-path] [repo version path file-path]
    "Read file as a bytes array.")

  (get-default-version [repo]
    "Get the latest mainstream version for a given repo.")

  (close [repo]
    "Perform all required actions to dispose a given repo."))

(defmacro with-repository
  "Ensures a repository is properly closed after the body is executed."
  [bindings & body]
  {:pre [(vector? bindings)
         (= 2 (count bindings))
         (symbol? (bindings 0))]}
  `(let ~bindings
     (try
       ~@body
       (finally (close ~(bindings 0))))))


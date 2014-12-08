# Archaeologist

A Clojure library to help you read versioned directories at a specific moment of time. Used internally in [Beanstalk](http://beanstalkapp.com). It works with local Subversion and Git repositories as well as regular unversioned directories. VCS support is provided by [SVNKit](http://svnkit.com) and [JGit](https://eclipse.org/jgit/).

## Installation

Add the following dependency to your `project.clj` file:

```
[archaeologist "0.1.0"]
```

## Usage

Require the core and all needed VCS adapters:

``` clojure
(require '[archaeologist.core :refer :all])
(require '[archaeologist.git :as git])
(require '[archaeologist.subversion :as subversion])
(require '[archaeologist.fs :as fs])
```

Recursively list files for current mainstream version:

``` clojure
(with-repository [repo (git/open-repository "test/fixtures/git")]
  (list-files repo (get-default-version repo)))

; => [{:kind "file", :path "a"} {:kind "file", :path "b"} {:kind "dir", :path "c"} {:kind "file", :path "c/d"} {:kind "file", :path "c/e"} {:kind "dir", :path "c/f"} {:kind "file", :path "c/f/g"}]
```

Recursively list files in directory:

``` clojure
(with-repository [repo (subversion/open-repository "test/fixtures/svn")]
  (list-files repo (get-default-version repo) "c"))

; => [{:kind "dir", :path "f"} {:kind "file", :path "f/g"} {:kind "file", :path "d"} {:kind "file", :path "e"}]
```

Read a file:

``` clojure
(with-repository [repo (fs/open-repository "test/fixtures/fs")]
  (read-file repo (get-default-version repo) "c/f/g"))

; => #<byte[] [B@6c017b07>
```

By composing these simple operations you should be able to work with versioned directory trees as if they were regular directories. Check out the tests for more usage examples.

## Running tests locally

Unzip fixtures into `test` directory:

``` sh
unzip test/fixtures.zip -d test
```

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

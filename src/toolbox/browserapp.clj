(ns toolbox.browserapp
  (:require
   [clojure.java.shell :as shell]
   [clojure.java.io :as io]

   [toolbox.project :as project]
   [toolbox.cli :as cli]))

(defn- build-with-figwheel []
  (cli/print-op "Figwheel JavaScript Compilation")

  (let [result (shell/sh "clojure" "-A:prod-js")]
    (println)
    (println (:out result))
    (println (:err result))
    (if-not (= 0 (:exit result))
      (cli/abort-with-failure)))

  (let [src-path "target/public/cljs-out/prod-main.js"
        dst-path "target/uberjar-resources/public/cljs-out/prod-main.js"]
    (-> dst-path java.io.File. .getParentFile .mkdirs)
    (io/copy (io/file src-path) (io/file dst-path))

    (cli/print-created-artifact dst-path)))


(defn build-with-shadow []
  (cli/print-op "Shadow-CLJS JavaScript Compilation")

  (let [browserapp (-> project/info :browserapp)
        release? (if (contains? browserapp :shadow-cljs/release?)
                   (-> browserapp :shadow-cljs/release?)
                   true)
        debug? (-> project/info :browserapp :shadow-cljs/debug?)]

    (when-not release?
      (cli/print-wrn ":shadow-cljs/release? -> false"))
    (when debug?
      (cli/print-wrn ":shadow-cljs/debug? -> true"))
    (let [args ["shadow-cljs" (if release? "release" "compile") "browserapp"]
          args (if debug?
                 (conj args "--debug")
                 args)
          result (apply shell/sh args)]
      (println)
      (println (:out result))
      (println (:err result))
      (if-not (= 0 (:exit result))
        (cli/abort-with-failure)))

    (let [src-path "target/public/main.js"
          dst-path "target/uberjar-resources/public/main.js"]
      (-> dst-path java.io.File. .getParentFile .mkdirs)
      (io/copy (io/file src-path) (io/file dst-path))

      (cli/print-created-artifact dst-path))))


(defn build-browserapp! []
  (build-with-shadow))

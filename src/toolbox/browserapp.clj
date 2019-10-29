(ns toolbox.browserapp
  (:require
   [clojure.java.shell :as shell]
   [clojure.java.io :as io]

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

  (let [result (shell/sh "shadow-cljs" "release" "browserapp")]
    (println)
    (println (:out result))
    (println (:err result))
    (if-not (= 0 (:exit result))
      (cli/abort-with-failure)))

  (let [src-path "target/public/main.js"
        dst-path "target/uberjar-resources/public/main.js"]
    (-> dst-path java.io.File. .getParentFile .mkdirs)
    (io/copy (io/file src-path) (io/file dst-path))

    (cli/print-created-artifact dst-path)))


(defn build-browserapp! []
  (build-with-shadow))

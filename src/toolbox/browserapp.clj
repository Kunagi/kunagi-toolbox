(ns toolbox.browserapp
  (:require
   [clojure.java.shell :as shell]
   [clojure.java.io :as io]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))

(defn build-browserapp! []
  (cli/print-op "Figwheel JavaScript Compilation")

  (let [result (shell/sh "clojure" "-A:build-js")]
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

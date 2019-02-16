(ns toolbox.browserapp
  (:require
   [clojure.java.shell :as shell]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))

(defn build-browserapp! []
  (cli/print-op "Figwheel JavaScript Compilation")

  (let [result (shell/sh "clojure" "-A:build-js")]
    (print (:out result))
    (print (:err result))
    (if-not (= 0 (:exit result))
      (cli/abort-with-failure))))

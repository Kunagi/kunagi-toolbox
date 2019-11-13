(ns toolbox.sync
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.java.shell :as shell]

   [puget.printer :as puget]
   [cheshire.core :as cheshire]

   [toolbox.cli :as cli]
   [toolbox.configuration :as configuration]
   [toolbox.project :as project]))


(defn cleanup-ret [ret]
  (-> ret
      (update :err #(when (> (count %) 1) %))
      (update :out #(when (> (count %) 1) %))))


(defn check-status [dir]
  (cli/print-op "git status" (-> dir .getName))
  (let [ret (shell/sh "git" "status" "--porcelain"
                      :dir dir)
        ret (cleanup-ret ret)]
    (when (not= 0 (-> ret :exit))
      (cli/abort-with-failure (-> ret :exit) (-> ret :out) (-> ret :err)))
    (when-let [out (-> ret :out)]
      (cli/abort-with-failure out))
    (when-let [out (-> ret :err)]
      (cli/abort-with-failure out))))


(defn sync! []
  (let [paths (configuration/paths-from-own-deps)
        paths (conj paths ".")
        dirs (map #(-> % io/as-file .getAbsoluteFile .getCanonicalFile) paths)]
    (doseq [dir dirs]
      (check-status dir))))

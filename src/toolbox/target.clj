(ns toolbox.target
  (:require
   [clojure.java.io :as io]

   [mach.pack.alpha.capsule :as capsule]

   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(defn delete-file [file]
  (when (.exists file)
    (when (.isDirectory file)
      (doseq [file (.listFiles file)]
        (delete-file file)))
    (io/delete-file file)))


(defn clean! []
  (cli/print-op "Cleanup")
  (let [path "target"]
    (-> path java.io.File. delete-file)))

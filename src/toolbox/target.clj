(ns toolbox.target
  (:require
   [clojure.java.io :as io]

   [toolbox.cli :as cli]))


(defn delete-file [file]
  (when (.exists file)
    (when (.isDirectory file)
      (doseq [file (.listFiles file)]
        (delete-file file)))
    (io/delete-file file)))


(defn clean! []
  (cli/print-op "Cleanup")
  (-> "target" java.io.File. delete-file)
  (-> ".cpcache" java.io.File. delete-file))

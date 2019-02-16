(ns toolbox.release
  (:require
   [clojure.java.io :as io]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))

(defn release! []
  (cli/print-op "Release")
  (let [id (:id project/info)
        src-path (str "target/" id)
        dst-path (str "/release-targets/apps/" id "/development/" id)]
    (io/copy (io/file src-path) (io/file dst-path))

    (cli/print-created-artifact dst-path)))

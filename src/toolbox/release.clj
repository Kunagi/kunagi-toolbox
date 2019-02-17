(ns toolbox.release
  (:require
   [clojure.java.io :as io]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]
   [toolbox.installer :as installer]))


(defn- copy-target [src-path dst-path]
  (let [id (:id project/info)
        src-path (str "target/" src-path)
        dst-path (str "/release-targets/apps/" id "/development/" dst-path)]
    (io/copy (io/file src-path) (io/file dst-path))
    (cli/print-created-artifact dst-path)))


(defn release! []
  (cli/print-op "Release")

  (let [id (:id project/info)]
    (copy-target id
                 id)
    (copy-target "install.bsh"
                 "install.bsh")
    (when (:systemd project/info)
      (copy-target (str "systemd/" id ".service")
                   (str id ".service")))))

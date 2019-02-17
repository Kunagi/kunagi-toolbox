(ns toolbox.systemd
  (:require
   [clojure.java.io :as io]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(defn target-path-unitfile []
  (str "target/systemd/" (-> project/info :systemd :unit-name) ".service"))


(defn build-systemd-unitfile! []
  (cli/print-op "Systemd Unit File")

  (let [script (str "[Unit]
Description=" (or (-> project/info :project :name)
                  (-> project/info :systemd :unit-name)) "
After=network.target

[Service]
Type=simple
User=" (-> project/info :serverapp :user-name) "
Group=" (-> project/info :serverapp :user-name) "
WorkingDirectory=" (-> project/info :serverapp :working-dir) "
ExecStart=" (-> project/info :serverapp :executable-path) "

[Install]
WantedBy=multi-user.target
")
        path (target-path-unitfile)]
    (-> path java.io.File. .getParentFile .mkdirs)
    (spit (io/as-file path) script)

    (cli/print-created-artifact path)))

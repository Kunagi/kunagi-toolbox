(ns toolbox.systemd
  (:require
   [clojure.java.io :as io]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(defn unit-name []
  (or (-> project/info :systemd :unit)
      (:id project/info)))


(defn user-name []
  (or (-> project/info :systemd :user)
      (unit-name)))


(defn group-name []
  (or (-> project/info :systemd :group)
      (user-name)))


(defn description []
  (or (-> project/info :systemd :description)
      (unit-name)))


(defn working-directory []
  (or (-> project/info :systemd :working-directory)
      (str "/var/local/" (unit-name))))


(defn exec-start []
  (or (-> project/info :systemd :exec-start)
      (str "/usr/local/bin/" (:id project/info))))



(defn target-path-unitfile []
  (str "target/systemd/" (unit-name) ".service"))


(defn build-systemd-unitfile! []
  (cli/print-op "Systemd Unit File")

  (let [script (str "[Unit]
Description=" (description) "
After=network.target

[Service]
Type=simple
User=" (user-name) "
Group=" (group-name) "
WorkingDirectory=" (working-directory) "
ExecStart=" (exec-start) "

[Install]
WantedBy=multi-user.target
")
        path (target-path-unitfile)]
    (-> path java.io.File. .getParentFile .mkdirs)
    (spit (io/as-file path) script)

    (cli/print-created-artifact path)))

(ns toolbox.installer
  (:require
   [clojure.java.io :as io]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]
   [toolbox.systemd :as systemd]))


(defn target-path-installer []
  (str "target/install.bsh"))


(defn build-installer! []
  (cli/print-op "Installer Script")

  (let [path (target-path-installer)
        file (io/as-file path)
        script (str "#!/bin/bash -e

# bin
sudo cp -v " (:id project/info) " /usr/local/bin/
"
                    (when (-> project/info :systemd)
                      (str "
# systemd
sudo adduser " (systemd/user-name) "
sudo mkdir -pv " (systemd/working-directory) "
sudo chown " (systemd/working-directory) " " (systemd/user-name) ":" (systemd/group-name) "
sudo cp -v " (systemd/unit-name) ".service /etc/systemd/system/")))]

    (spit file script)
    (.setExecutable file true false)
    (cli/print-created-artifact path)))

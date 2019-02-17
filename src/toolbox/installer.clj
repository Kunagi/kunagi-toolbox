(ns toolbox.installer
  (:require
   [clojure.java.io :as io]
   [puget.printer :as puget]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]
   [toolbox.systemd :as systemd]))


(defn target-path-installer []
  (str "target/install.bsh"))


(defn build-install-script []
  (let [executable-name (or (-> project/info :commandapp :executable-name)
                            (-> project/info :serverapp :executable-name))
        path (target-path-installer)
        file (io/as-file path)
        config-path (str "/etc/" (-> project/info :id))
        config-secrets-path (str config-path "/secrets.edn")
        script (str "#!/bin/bash -e

# bin
sudo cp `dirname $0`/" executable-name " /usr/local/bin/
"
                    (when (-> project/info :serverapp)
                      (str "
# serverapp
echo \"working-directory: " (-> project/info :serverapp :working-dir) "\"
echo \"user: " (-> project/info :serverapp :user-name) "\"
sudo adduser --system --group --home " (-> project/info :serverapp :working-dir) " " (-> project/info :serverapp :user-name) "
#sudo mkdir -p " (-> project/info :serverapp :working-dir) "
sudo chmod 770 " (-> project/info :serverapp :working-dir) "
echo \"config-directory: " config-path "\"
sudo mkdir -p " config-path "
sudo cp --no-clobber configs/* " config-path "/
sudo chmod 400 " config-secrets-path "
sudo chown " (-> project/info :serverapp :user-name) ":root " config-secrets-path "
"))
                    (when (-> project/info :systemd)
                      (str "
# systemd
echo \"systemd: " (-> project/info :systemd :unit-name) ".service\"
sudo cp `dirname $0`/" (-> project/info :systemd :unit-name) ".service /etc/systemd/system/
")))]


    (spit file script)
    (.setExecutable file true false)
    (cli/print-created-artifact path)))


(defn build-config-file [file-key content]
  (let [path (str "target/configs/" (name file-key) ".edn")
        file (io/as-file path)]
    (-> file .getParentFile .mkdirs)
    (spit file (puget/pprint-str content))
    (cli/print-created-artifact path)))


(defn build-installer! []
  (cli/print-op "Installer Script")
  (when (-> project/info :serverapp)
    (build-config-file :config {:http-server/port (-> project/info :serverapp :http-port)
                                :http-server/uri (-> project/info :serverapp :uri)
                                :oauth {:google {}}})
    (build-config-file :secrets {:oauth {:google {:client-id "?"
                                                  :client-secret "?"}}}))
  (build-install-script))

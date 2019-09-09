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
sudo chmod 600 " config-secrets-path "
sudo chown " (-> project/info :serverapp :user-name) ":root " config-secrets-path "
"))
                    (when (-> project/info :systemd)
                      (let [unit-name (-> project/info :systemd :unit-name)]
                        (str "
# systemd
echo \"systemd: " unit-name ".service\"
sudo cp `dirname $0`/" unit-name ".service /etc/systemd/system/
sudo systemctl enable --force " unit-name ".service
sudo systemctl restart " unit-name ".service
")))
                    (when (-> project/info :serverapp :vhost)
                      (let [vhost (-> project/info :serverapp :vhost)
                            sites-available (str "/etc/nginx/sites-available/" vhost)
                            sites-enabled (str "/etc/nginx/sites-enabled/" vhost)]
                        (str "
# nginx
sudo cp --no-clobber `dirname $0`/nginx-vhost " sites-available "
sudo ln -s " sites-available " " sites-enabled "
sudo systemctl reload nginx
sudo certbot --nginx -n -d " vhost " run enhance
"))))]

    (spit file script)
    (.setExecutable file true false)
    (cli/print-created-artifact path)))


(defn build-nginx-config []
  (let [vhost (-> project/info :serverapp :vhost)
        port (-> project/info :serverapp :http-port)
        path (str "target/nginx-vhost")
        file (io/as-file path)
        content (str "
server {

    server_name " vhost ";
    port 80;

    location / {

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_pass http://localhost:" port ";

    }

}
")]
    (spit file content)
    (.setExecutable file true false)
    (cli/print-created-artifact path)))


(defn build-config-file [file-key content]
  (let [path (str "target/configs/" (name file-key) ".edn")
        file (io/as-file path)]
    (-> file .getParentFile .mkdirs)
    (spit file (puget/pprint-str content))
    (cli/print-created-artifact path)))


(defn build-installer! []
  (cli/print-op "Installer")
  (when (-> project/info :serverapp)
    (build-config-file :config {:http-server/port (-> project/info :serverapp :http-port)
                                :http-server/uri (-> project/info :serverapp :uri)
                                :oauth {:google {:enabled? false}}})
    (build-config-file :secrets {:oauth {:google {:client-id "?"
                                                  :client-secret "?"}}}))
  (when (-> project/info :serverapp :vhost)
    (build-nginx-config))
  (build-install-script))

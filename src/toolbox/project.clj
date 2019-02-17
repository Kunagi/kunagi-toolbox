(ns toolbox.project
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]

   [toolbox.cli :as cli]))


(defn load-edn [file]
  (try
    (edn/read-string (slurp file))
    (catch Exception ex
      (cli/abort-with-failure "Failed to read conf file:" (.getPath file)))))


(defn load-conf [info conf-key mandatory-keys]
  (let [path (str "conf/" (name conf-key) ".edn")
        file (io/as-file path)]

    (when (and (> (count mandatory-keys) 0)
               (not (.exists file)))
      (cli/abort-with-failure "Missing mandatory conf file:" path))

    (if-not (.exists file)
      info
      (let [data (load-edn file) ]

        (doseq [mandatory-key mandatory-keys]
          (when-not (get data mandatory-key)
            (cli/abort-with-failure "Missing mandatory key" mandatory-key "in" path)))

        (assoc info conf-key data)))))


(defn move-id [info]
  (assoc info :id (get-in info [:project :id])))


(defn complete-project-name [info]
  (update-in info [:project :name] #(or % (:id info))))


(defn print-info [info]
  (cli/print-edn info)
  info)


(defn complete-serverapp [info]
  (if-not (:serverapp info)
    info
    (let [serverapp (:serverapp info)
          executable-name (or (:executable-name serverapp)
                              (str (:id info) "d"))
          user-name (or (-> serverapp :user)
                        (-> info :id))
          config-dir (or (-> serverapp :config-dir)
                         (str "/etc/" (-> info :id)))
          working-dir (or (-> serverapp :working-dir)
                          (str "/var/local/" (-> info :id)))
          executable-path (str "/usr/local/bin/" executable-name)
          http-port (or (-> serverapp :http-port)
                        3000)
          vhost (-> serverapp :vhost)
          uri (if vhost
                (str "https://" vhost)
                (str "http://localhost:" http-port))]
      (assoc info :serverapp (merge serverapp
                                    {:executable-name executable-name
                                     :user-name user-name
                                     :config-dir config-dir
                                     :working-dir working-dir
                                     :executable-path executable-path
                                     :http-port http-port
                                     :uri uri})))))


(defn complete-commandapp [info]
  (if-not (:commandapp info)
    info
    (let [commandapp (:commandapp info)
          executable-name (or (:executable-name commandapp)
                              (:id info))]
      (assoc info :commandapp (merge commandapp
                                     {:executable-name executable-name})))))


(defn complete-systemd [info]
  (if-not (:systemd info)
    info
    (let [systemd (:systemd info)
          unit-name (or (-> systemd :unit)
                        (:id info))]
      (assoc info :systemd (merge systemd
                                  {:unit-name unit-name})))))

(defn create-info []
  (-> {}
      (load-conf :project [:id])
      (move-id)
      (complete-project-name)
      (load-conf :commandapp [])
      (complete-commandapp)
      (load-conf :serverapp [])
      (complete-serverapp)
      (load-conf :browserapp [])
      (load-conf :systemd [])
      (complete-systemd)
      (load-conf :release [])))


(def info (create-info))

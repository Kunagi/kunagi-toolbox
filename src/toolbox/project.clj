(ns toolbox.project
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]

   [toolbox.cli :as cli]))


(defn load-conf [info conf-key mandatory-keys]
  (let [path (str "conf/" (name conf-key) ".edn")
        file (io/as-file path)]

    (when (and (> (count mandatory-keys) 0)
               (not (.exists file)))
      (cli/abort-with-failure "Missing mandatory conf file:" path))

    (if-not (.exists file)
      info
      (let [data (edn/read-string (slurp file))]

        (doseq [mandatory-key mandatory-keys]
          (when-not (get data mandatory-key)
            (cli/abort-with-failure "Missing mandatory key" mandatory-key "in" path)))

        (assoc info conf-key data)))))


(defn move-id [info]
  (assoc info :id (get-in info [:project :id])))


(defn complete-project-name [info]
  (update-in info [:project :name] #(or % (:id info))))

(defn print-info [info]
  (println info)
  info)


(defn create-info []
  (-> {}
      (load-conf :project [:id])
      (move-id)
      (complete-project-name)
      (load-conf :browserapp [])
      (load-conf :systemd [])
      (load-conf :release [])
      (print-info)))


(def info (create-info))

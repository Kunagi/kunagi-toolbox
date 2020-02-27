(ns toolbox.prepare
  (:require
   [clojure.string :as string]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(def date-format-iso (-> "yyyy-MM-dd HH:mm" java.text.SimpleDateFormat.))


(defn- create-appinfo-file []
  (let [file (str "src/" (-> project/info :id) "/appinfo.cljc")
        appinfo {:build-time (.format date-format-iso (java.util.Date.))
                 :project (-> project/info :project)
                 :release (-> project/info :release)
                 :serverapp (-> project/info :serverapp)
                 :browserapp (-> project/info :browserapp)}]
    (spit file (str "(ns " (-> project/info :id) ".appinfo)

(def appinfo
  " (pr-str appinfo) ")"))
    (cli/print-created-artifact file)))


(defn prepare! []
  (cli/print-op "Prepare")
  (create-appinfo-file))

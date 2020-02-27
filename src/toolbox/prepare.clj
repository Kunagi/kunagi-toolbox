(ns toolbox.prepare
  (:require
   [datenschutzerklaerung.generator :as dsegen]
   [toolbox.utils :as utils]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(def date-format-iso (-> "yyyy-MM-dd HH:mm" java.text.SimpleDateFormat.))


(defn- create-appinfo-file []
  (let [appinfo {:build-time (.format date-format-iso (java.util.Date.))
                 :project (-> project/info :project)
                 :release (-> project/info :release)
                 :serverapp (-> project/info :serverapp)
                 :browserapp (-> project/info :browserapp)
                 :legal (-> project/info :legal)}]
    (when-let [file (utils/write-cljc (str (-> project/info :id)
                                           ".appinfo")
                                      [(list 'def 'appinfo appinfo)])]
      (cli/print-created-artifact file))))

(defn- create-datenschutzerklaerung-file []
  (when-let [dseb (-> project/info :legal :datenschutzerklaerung-bausteine)]
    (when-let [file (utils/write-cljc
                     (str (-> project/info :id) ".datenschutzerklaerung")
                     [(list 'def 'html (dsegen/generate-html
                                        {:bausteine dseb
                                         :verantwortlicher (-> project/info
                                                               :legal
                                                               :vendor)}))])]
      (cli/print-created-artifact file))))


(defn prepare! []
  (cli/print-op "Prepare")
  (create-appinfo-file)
  (create-datenschutzerklaerung-file))

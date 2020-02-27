(ns toolbox.prepare
  (:require
   [clojure.string :as string]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(def date-format-iso (-> "yyyy-MM-dd HH:mm" java.text.SimpleDateFormat.))


(defn- create-build-info-file []
  (let [file (str "src/" (-> project/info :id) "/build_info.cljc")
        build {:date-time (.format date-format-iso (java.util.Date.))}]
    (spit file (str "(ns " (-> project/info :id) ".build-info)

(def build
  " (pr-str build) ")"))
    (cli/print-created-artifact file)))


(defn prepare! []
  (cli/print-op "Prepare")
  (create-build-info-file))

(ns toolbox.project)


(defn determine-project-id []
  (-> "src"
      java.io.File.
      .listFiles
      first
      .getName))
  ;; (-> "dummy-filename"
  ;;     java.io.File.
  ;;     .getAbsoluteFile
  ;;     .getParentFile
  ;;     .getName))


(defn assoc-browserapp [info]
  (let [id (:id info)
        path (str "src/" id "/main.cljs")
        file (java.io.File. path)]
    (assoc info :browserapp? (.exists file))))


(defn create-info []
  (-> {:id (determine-project-id)}
      assoc-browserapp))


(def info (create-info))

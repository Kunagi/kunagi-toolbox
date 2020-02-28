(ns toolbox.prepare
  (:require
   [datenschutzerklaerung.generator :as dsegen]
   [toolbox.utils :as utils]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(def date-format-iso (-> "yyyy-MM-dd HH:mm" java.text.SimpleDateFormat.))


(defn- create-appinfo-cljc []
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

(defn- create-datenschutzerklaerung-cljc []
  (when-let [dseb (-> project/info :legal :datenschutzerklaerung-bausteine)]
    (when-let [file (utils/write-cljc
                     (str (-> project/info :id) ".datenschutzerklaerung")
                     [(list 'def 'html (dsegen/generate-html
                                        {:bausteine dseb
                                         :verantwortlicher (-> project/info
                                                               :legal
                                                               :vendor)}))])]
      (cli/print-created-artifact file))))


(defn- create-serviceworker []
  (when-let [sw (-> project/info :browserapp :serviceworker)]
    (let [pre-cache (concat
                     (-> sw :pre-cache)
                     (utils/list-paths "resources/public" "fonts")
                     (utils/list-paths "resources/public" "img"))
          no-cache (-> sw :no-cache)
          cache-first (-> sw :cache-first)
          sw-code (utils/load-string-resource "toolbox/serviceworker.js")
          sw-code (str "// " utils/gen-comment "\n\n" sw-code)
          sw-code (.replace sw-code
                            "$VERSION"
                            (str (get sw :version 1)))
          sw-code (.replace sw-code
                            "$PRE_CACHE"
                            (utils/as-js-array-of-strings pre-cache))
          sw-code (.replace sw-code
                            "$NO_CACHE"
                            (utils/as-js-array-of-regexes no-cache))
          sw-code (.replace sw-code
                            "$CACHE_FIRST"
                            (utils/as-js-array-of-regexes cache-first))
          file "resources/public/serviceworker.js"]
      (spit file sw-code)
      (cli/print-created-artifact file))))


(defn prepare! []
  (cli/print-op "Prepare")
  (create-appinfo-cljc)
  (create-datenschutzerklaerung-cljc)
  (create-serviceworker))

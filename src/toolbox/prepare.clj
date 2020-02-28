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
          base-url (str "https://" (-> project/info :serverapp :vhost))
          sw-code (utils/load-string-resource "toolbox/serviceworker.js")
          sw-code (str "// " utils/gen-comment "\n\n" sw-code)
          sw-code (.replace sw-code "$BASE_URL" (str "\"" base-url "\""))
          sw-code (.replace sw-code
                            "$VERSION"
                            (str (get sw :version 1)))
          sw-code (.replace sw-code
                            "$PRE_CACHE"
                            (utils/as-js-array-of-strings
                             (map #(str base-url "/" %) pre-cache)))
          sw-code (.replace sw-code
                            "$NO_CACHE"
                            (utils/as-js-array-of-regexes no-cache))
          sw-code (.replace sw-code
                            "$CACHE_FIRST"
                            (utils/as-js-array-of-regexes cache-first))
          file "resources/public/serviceworker.js"]
      (utils/mkdir-for-file file)
      (spit file sw-code)
      (cli/print-created-artifact file))))


(defn- create-manifest []
  (when-let [browserapp (-> project/info :browserapp)]
    (let [colors (-> browserapp :colors)
          related-apps (-> browserapp :related-apps)
          data {:short_name (-> project/info :project :name)
                :name (-> project/info :project :name)
                :icons [{:src "/img/app-icon_192.png"
                         :type "image/png"
                         :sizes "192x192"}
                        {:src "/img/app-icon_512.png"
                         :type "image/png"
                         :sizes "512x512"}]
                :start_url "/ui/?utm_source=a2hs"
                :scope "/"
                :display :standalone
                :theme_color (get colors :primary "#000000")
                :background_color (get colors :background "#E1E2E1")
                :prefer_related_applications (boolean (seq related-apps))
                :related_applications (map (fn [[platform id]]
                                             {:platform platform
                                              :id id})
                                           related-apps)}
          file (str "resources/public/manifest.json")]
     (utils/write-json file data)
     (cli/print-created-artifact file))))


(defn prepare! []
  (cli/print-op "Prepare")
  (create-appinfo-cljc)
  (create-datenschutzerklaerung-cljc)
  (create-manifest)
  (create-serviceworker))

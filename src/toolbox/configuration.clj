(ns toolbox.configuration
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.java.shell :as shell]

   [puget.printer :as puget]
   [cheshire.core :as cheshire]

   [toolbox.utils :as utils]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))



(defn create-own-deps []
  (reduce
   (fn [deps lib]
     (assoc deps lib {:local/root (str "../" (name lib))}))
   {}
   (-> project/info :deps :own)))


(defn load-deps-from-local-project [project-name]
  (let [deps-file (io/as-file (str "../" project-name "/conf/deps.edn"))]
    (when-not (.exists deps-file)
      (cli/abort-with-failure "Missing file:" (.getAbsolutePath deps-file)))
    (-> deps-file slurp edn/read-string :own)))


(defn collect-deps-from-local-projects [deps]
  (reduce
   (fn [deps dep]
     (let [child-deps (load-deps-from-local-project (name dep))]
       (-> deps
           (into child-deps)
           (into (collect-deps-from-local-projects child-deps)))))
   (into #{} deps)
   deps))


(defn paths-from-own-deps []
  (sort
   (map
    (fn [lib]
      (str "../" (name lib)))
    (collect-deps-from-local-projects (-> project/info :deps :own)))))


(defn src-paths-from-own-deps []
  (map #(str % "/src") (paths-from-own-deps)))


(defn create-aliases []
  (cond-> {
           :ancient {:main-opts ["-m" "deps-ancient.deps-ancient"]
                     :extra-deps {'deps-ancient {:mvn/version "RELEASE"}}}

           :shadow-cljs {:main-opts ["-m" "shadow.cljs.devtools.cli"]
                         :extra-deps {'thheller/shadow-cljs {:mvn/version "RELEASE"}}}}

                 ;;:dev {
                 ;; :extra-paths (into ["target"]
                 ;;                    (src-paths-from-own-deps))
                 ;; :extra-deps {}
                              ;; 'com.bhauman/rebel-readline-cljs {:mvn/version "RELEASE"}
                              ;; 'binaryage/devtools              {:mvn/version "RELEASE"}
                              ;; 'day8.re-frame/re-frame-10x      {:mvn/version "RELEASE"}}
                 ;; :main-opts ["--main" "figwheel.main" "--build" "dev"]}

    true
    (assoc :kunagi-toolbox
           {:extra-deps {'kunagi-toolbox {:local/root "../kunagi-toolbox"}}
            :main-opts ["--main" "toolbox.main"]})))

          ;; (-> project/info :browserapp)))
          ;; (assoc :prod-js {:main-opts ["--main"          "figwheel.main"
          ;;                              "--build-once"    "prod"]})))
                           ;;:extra-deps {'com.bhauman/figwheel-main {:mvn/version "RELEASE"}}})))

(defn generate-deps []
  (cli/print-op "Generate deps.end")

  (let [default-deps (if (-> project/info :browserapp)
                      {} ;;{'com.bhauman/figwheel-main {:mvn/version "RELEASE"}}
                      {})
        deps-file (io/as-file "deps.edn")
        deps {:deps (merge default-deps
                           (-> project/info :deps :foreign)
                           (create-own-deps))
              :paths (-> [];;"target"]
                         (into (-> project/info :deps :paths)))
                         ;;(into (src-paths-from-own-deps)))
              :aliases (create-aliases)}]


    ;; TODO --verbose (puget/cprint deps)
    (spit deps-file (str (puget/pprint-str deps)
                         "\n\n;; " utils/gen-comment))
    (cli/print-created-artifact "deps.edn")))


(defn generate-dev []
  (cli/print-op "Development Script")
  (let [file (io/as-file "dev")]
    (spit file (str "#!/bin/bash -e
rm -rf .cpcache
rm -rf target/*
clojure -A:dev

# " utils/gen-comment))
    (.setExecutable file true false)
    (cli/print-created-artifact (.getName file))))


(defn generate-dev-cljs []
  (cli/print-op "ClojureScript Development Configuration")
  (let [file (io/as-file "dev.cljs.edn")
        figwheel-meta {:watch-dirs (into ["src"]
                                         (src-paths-from-own-deps))
                       :open-file-command "emacsclient"
                       :launch-js ["google-chrome" :open-url]}

        figwheel-meta (if (-> project/info :serverapp)
                        (assoc figwheel-meta
                               :ring-handler
                               (symbol (str (-> project/info :id) ".figwheel-adapter")
                                       "ring-handler-for-figwheel"))
                        figwheel-meta)
        configuration {:main (symbol (str (-> project/info :id) ".figwheel-adapter"))
                       :optimizations :none
                       :output-to "resources/public/cljs-out/dev-main.js"
                       :output-dir "resources/public/cljs-out/dev"
                       :asset-path "/cljs-out/dev"
                       :preloads ['devtools.preload
                                  'day8.re-frame-10x.preload]
                       :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}}]
    (spit file (str "^" (puget/pprint-str figwheel-meta) "\n"
                    (puget/pprint-str configuration)
                    "\n\n;; " utils/gen-comment))
    (cli/print-created-artifact (.getName file))))


(defn generate-prod-cljs []
  (cli/print-op "ClojureScript Production Configuration")
  (let [file (io/as-file "prod.cljs.edn")
        configuration {:main (symbol (str (-> project/info :id) ".main"))
                       :optimizations (-> project/info :browserapp :js-optimizations)
                       :closure-defines {"goog.DEBUG" false}}]
    (spit file (str (puget/pprint-str configuration)
                    "\n\n;; " utils/gen-comment))
    (cli/print-created-artifact (.getName file))))


(defn generate-package-json []
  (cli/print-op "NPM Configuration")
  (let [file (io/as-file "package.json")
        m {"name" (-> project/info :id),
           "version" (or (-> project/info :release :version) "1.0.0")
           "description" (or (-> project/info :project :description) (-> project/info :id)),
           "private" true
           "license" (or (-> project/info :project :license :name) "none")
           "dependencies" ;; FIXME get dependencies from own-dependencies
           {"@material-ui/core"          "^4.9.9"
            "@material-ui/icons"         "^4.9.1"
            "react"                      "^16.13.1"
            "react-dom"                  "^16.13.1"}} ;; FIXME read from conf/deps.edn
        json (cheshire/generate-string m {:pretty true})]
    (spit file json)
    (cli/print-created-artifact (.getName file))))


(defn generate-shadow-cljs-edn []
  (cli/print-op "Shadow-CLJS Configuration")
  (let [file (io/as-file "shadow-cljs.edn")
        id (-> project/info :id)
        configuration {:deps true
                       :builds
                       {:browserapp
                        {:output-dir "target/public/"
                         :asset-path "/"
                         :target :browser
                         :modules {:main {:init-fn (symbol (str id ".main/init"))}}
                         :compiler-options {:infer-externs :auto
                                            :externs ["datascript/externs.js"]} ;; FIXME
                         :devtools {:preloads '[kunagi-base-browserapp.modules.devtools.model
                                                shadow.remote.runtime.cljs.browser]
                                    :after-load (symbol (str id ".main/shadow-after-load"))
                                    :repl-pprint true}
                         :dev {:compiler-options {:devcards true}}}}}]
    (spit file (str (puget/pprint-str configuration)
                    "\n\n;; " utils/gen-comment "\n"))
    (cli/print-created-artifact (.getName file))))


(defn npm-install []
  (cli/print-op "NPM install")
  (let [result (shell/sh "npm" "install")]
    (println)
    (println (:out result))
    (println (:err result))
    (if-not (= 0 (:exit result))
      (cli/abort-with-failure))))


(defn configure! []
  (generate-deps)
  (when (-> project/info :browserapp)
    (generate-package-json)
    (generate-shadow-cljs-edn)
    (npm-install)))
  ;; (generate-dev)
  ;; (generate-dev-cljs)
  ;; (generate-prod-cljs))

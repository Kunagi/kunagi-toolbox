(ns toolbox.configuration
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [puget.printer :as puget]

   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(def gen-comment "Generated by kunagi-toolbox.")


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
      (str "../" (name lib) "/src"))
    (collect-deps-from-local-projects (-> project/info :deps :own)))))


(defn create-aliases []
  (cond-> {:dev {
                 ;; :extra-paths (into ["target"]
                 ;;                    (paths-from-own-deps))
                 ;; :extra-deps {}
                              ;; 'com.bhauman/rebel-readline-cljs {:mvn/version "RELEASE"}
                              ;; 'binaryage/devtools              {:mvn/version "RELEASE"}
                              ;; 'day8.re-frame/re-frame-10x      {:mvn/version "RELEASE"}}
                 :main-opts ["--main" "figwheel.main" "--build" "dev"]}
           :ancient {:main-opts ["-m" "deps-ancient.deps-ancient"]
                     :extra-deps {'deps-ancient {:mvn/version "RELEASE"}}}}

          (-> project/info :browserapp)
          (assoc :prod-js {:main-opts ["--main"          "figwheel.main"
                                       "--build-once"    "prod"]})))
                           ;;:extra-deps {'com.bhauman/figwheel-main {:mvn/version "RELEASE"}}})))

(defn generate-deps []
  (cli/print-op "Generate deps.end")

  (let [default-deps (if (-> project/info :browserapp)
                      {'com.bhauman/figwheel-main {:mvn/version "RELEASE"}}
                      {})
        deps-file (io/as-file "deps.edn")
        deps {:deps (merge default-deps
                           (-> project/info :deps :foreign)
                           (create-own-deps))
              :paths (-> ["target"]
                         (into (-> project/info :deps :paths))
                         (into (paths-from-own-deps)))
              :aliases (create-aliases)}]


    ;; TODO --verbose (puget/cprint deps)
    (spit deps-file (str (puget/pprint-str deps)
                         "\n\n;; " gen-comment))
    (cli/print-created-artifact "deps.edn")))


(defn generate-dev []
  (cli/print-op "Development Script")
  (let [file (io/as-file "dev")]
    (spit file (str "#!/bin/bash -e
rm -rf .cpcache
rm -rf target/*
clojure -A:dev

# " gen-comment))
    (.setExecutable file true false)
    (cli/print-created-artifact (.getName file))))


(defn generate-dev-cljs []
  (cli/print-op "ClojureScript Development Configuration")
  (let [file (io/as-file "dev.cljs.edn")
        figwheel-meta {:watch-dirs (into ["src"]
                                         (paths-from-own-deps))
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
                       :preloads ['devtools.preload
                                  'day8.re-frame-10x.preload]
                       :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}}]
    (spit file (str "^" (puget/pprint-str figwheel-meta) "\n"
                    (puget/pprint-str configuration)
                    "\n\n;; " gen-comment))
    (cli/print-created-artifact (.getName file))))


(defn generate-prod-cljs []
  (cli/print-op "ClojureScript Production Configuration")
  (let [file (io/as-file "prod.cljs.edn")
        configuration {:main (symbol (str (-> project/info :id) ".main"))
                       :optimizations (-> project/info :browserapp :js-optimizations)
                       :closure-defines {"goog.DEBUG" false}}]
    (spit file (str (puget/pprint-str configuration)
                    "\n\n;; " gen-comment))
    (cli/print-created-artifact (.getName file))))


(defn configure! []
  (generate-deps)
  (generate-dev)
  (generate-dev-cljs)
  (generate-prod-cljs))

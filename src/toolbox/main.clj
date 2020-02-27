(ns toolbox.main
  (:require
   [clojure.string :as string]
   [toolbox.cli :as cli]
   [toolbox.project :as project]
   [toolbox.configuration :as configuration]
   [toolbox.target :as target]
   [toolbox.prepare :as prepare]
   [toolbox.browserapp :as browserapp]
   [toolbox.uberjar :as uberjar]
   [toolbox.systemd :as systemd]
   [toolbox.installer :as installer]
   [toolbox.release :as release]
   [toolbox.sync :as sync]))


(defn info! []
  (project/print-info project/info))


(defn configure! []
  (configuration/configure!))


(defn clean! []
  (target/clean!))


(defn sync! []
  (sync/sync!))


(defn build! []
  (clean!)
  (prepare/prepare!)
  (when (:browserapp project/info)
    (browserapp/build-browserapp!))
  (uberjar/build-uberjar!)
  (uberjar/build-executable!)
  (when (:systemd project/info)
    (systemd/build-systemd-unitfile!))
  (installer/build-installer!))


(defn release! []
  (build!)
  (when (:release project/info)
    (release/release!)))


(defn full-build! []
  (release!))



(def cli-options
  [["-h" "--help"]])


(defn usage [options-summary]
  (->> ["Kunagi Toolbox"
        ""
        "Usage: kunagi-toolbox [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  info        Show project information"
        "  configure   Generate project configuration"
        "  clean       Delete target and .cpcache"
        "  sync        Synchronize git repositories of dependent projects"
        "  build       Build project artifacts"
        "  release     Build and release project artifacts"
        ""
        "This probram must be run in a compatible Kunagi project directory."]
       (string/join \newline)))


(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))


(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"info" "configure" "clean" "sync" "build" "release"} (first arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))


(defn exit [status msg]
  (println msg)
  (System/exit status))


(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (try
        (case action
          "info" (info!)
          "configure" (configure!)
          "clean" (clean!)
          "sync" (sync!)
          "build" (build!)
          "release" (release!))
        (System/exit 0)
        (catch Throwable ex
          (.printStackTrace ex)
          (System/exit 1))))))


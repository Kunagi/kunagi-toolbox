(ns toolbox.main
  (:require
   [toolbox.project :as project]
   [toolbox.target :as target]
   [toolbox.browserapp :as browserapp]
   [toolbox.uberjar :as uberjar]
   [toolbox.systemd :as systemd]
   [toolbox.installer :as installer]
   [toolbox.release :as release]))


(defn full-build! []
  (target/clean!)
  (when (:browserapp project/info)
    (browserapp/build-browserapp!))
  (uberjar/build-uberjar!)
  (uberjar/build-executable!)
  (when (:systemd project/info)
    (systemd/build-systemd-unitfile!))
  (installer/build-installer!)
  (when (:release project/info)
    (release/release!)))


(defn -main []
  (try
    (full-build!)
    (catch Throwable ex
      (.printStackTrace ex)
      (System/exit 1)))
  (System/exit 0))

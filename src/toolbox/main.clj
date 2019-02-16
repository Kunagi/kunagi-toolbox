(ns toolbox.main
  (:require
   [toolbox.project :as project]
   [toolbox.target :as target]
   [toolbox.browserapp :as browserapp]
   [toolbox.uberjar :as uberjar]
   [toolbox.release :as release]))


(defn full-build! []
  (target/clean!)
  (when (:browserapp? project/info)
    (browserapp/build-browserapp!))
  (uberjar/build-uberjar!)
  (uberjar/build-executable!)
  (release/release!))


(defn -main []
  (try
    (full-build!)
    (catch Throwable ex
      (System/exit 1)))
  (System/exit 0))

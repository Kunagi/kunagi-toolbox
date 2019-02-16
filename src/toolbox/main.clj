(ns toolbox.main
  (:require
   [toolbox.project :as project]
   [toolbox.target :as target]
   [toolbox.browserapp :as browserapp]
   [toolbox.uberjar :as uberjar]))


(defn full-build! []
  (target/clean!)
  (when (:browserapp? project/info)
    (browserapp/build-browserapp!))
  (uberjar/build-uberjar!)
  (uberjar/build-executable!))


(defn -main []
  (full-build!)
  (System/exit 0))

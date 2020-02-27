(ns toolbox.utils
  (:require
   [clojure.string :as string]
   [puget.printer :as puget]))


(defn pprint [data]
  (puget/pprint-str data))


(defn write-clojure-file [dir namespace suffix contents]
  (let [file (-> namespace
                 (.replace "." "/")
                 (.replace "-" "_")
                 (str "." (name suffix))
                 (->> (str dir "/")))
        nsdef (list 'ns (symbol namespace))
        code (->> contents
                  (into [nsdef])
                  (map (fn [content]
                         (pprint content)))
                  (string/join "\n\n"))]
    (spit file code)
    file))


(defn write-cljc [namespace contents]
  (write-clojure-file "src" namespace :cljc contents))

(ns toolbox.utils
  (:require
   [clojure.string :as string]
   [clojure.java.io :as io]
   [puget.printer :as puget]
   [cheshire.core :as cheshire]))


(def gen-comment "Generated by kunagi-toolbox.")


(defn pprint [data]
  (puget/pprint-str data))


(defn as-js-array-of-strings [items]
  (->> items
       (map (fn [item] (str "\n\"" item "\"")))
       (string/join ", ")))


(defn as-js-array-of-regexes [items]
  (->> items
       (map (fn [item]
              (str "/"
                   (-> item
                       (.replace "/" "\\/"))
                   "/")))
       (string/join ", ")))


(defn list-paths [root-dir dir]
  (->> (str root-dir "/" dir)
       (java.io.File.)
       .list
       (map (fn [file] (str dir "/" file)))))


(defn load-string-resource [path]
  (when-let [uri (io/resource path)]
    (slurp uri)))


(defn mkdir-for-file [file]
  (-> file
      io/as-file
      .getParentFile
      .mkdirs))


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
                  (string/join "\n\n"))
        code (str ";; " gen-comment "\n\n"
                  code
                  "\n")]
    (mkdir-for-file file)
    (spit file code)
    file))


(defn write-cljc [namespace contents]
  (write-clojure-file "src" namespace :cljc contents))


(defn write-json [file data]
  (mkdir-for-file file)
  (spit file
        (str
         (cheshire/generate-string data
                                   {:pretty true})
         "\n")))

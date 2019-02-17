(ns toolbox.uberjar
  (:require
   [clojure.java.io :as io]

   [mach.pack.alpha.capsule :as capsule]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]))


(defn build-uberjar! []
  (cli/print-op "Capsule Uberjar")

  (let [id (:id project/info)
        path (str "target/" id ".jar")]

    (-> "target" java.io.File. .mkdirs)

    (capsule/-main path
                   "--application-id" id
                   "--extra-path" "target/uberjar-resources"
                   "--main" (str id ".main"))

    (cli/print-created-artifact path)))


(def exec-header
  (str "#!/bin/sh"
       "\n\n"
       "exec bash -c \"exec $(java -Dcapsule.trampoline -jar $0) $@\""
       "\n\n\n\n"))


(defn build-executable! []
  (cli/print-op "Executable")

  (let [id (:id project/info)
        uberjar-path (str "target/" id ".jar")
        uberjar-file (java.io.File. uberjar-path)
        output-path (str "target/" id)
        output-file (java.io.File. output-path)
        output-stream (java.io.FileOutputStream. output-file true)]

    (spit output-file exec-header)
    (io/copy uberjar-file output-stream)
    (.close output-stream)
    (.setExecutable output-file true false)

    (cli/print-created-artifact output-path)))


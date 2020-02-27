(ns toolbox.release
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [puget.printer :as puget]

   [toolbox.target :as target]
   [toolbox.cli :as cli]
   [toolbox.project :as project]
   [toolbox.installer :as installer]))


(defn recursive-copy [src dst]
  (when (.isFile src)
    (-> dst .getParentFile .mkdirs)
    (io/copy src dst))
  (when (.isDirectory src)
    (.mkdirs dst)
    (doseq [src-sub (.listFiles src)]
      (recursive-copy src-sub (java.io.File. (str (.getPath dst) "/" (.getName src-sub)))))))


(defn- copy-dir [path]
  (let [id (:id project/info)
        src-path (str "target/" path)
        src-file (io/as-file src-path)]
    (when (.exists src-file)
      (let [dst-path (str "/release-targets/apps/" id "/development/" path)
            dst-file (io/as-file dst-path)]
        (recursive-copy src-file dst-file)
        (cli/print-created-artifact (str dst-path "/*"))))))


(defn- copy-target [src-path dst-path]
  (let [id (:id project/info)
        src-path (str "target/" src-path)
        src-file (io/as-file src-path)
        dst-path (str "/release-targets/apps/" id "/development/" dst-path)
        dst-file (io/as-file dst-path)]
    (-> src-file .getParentFile .mkdirs)
    (io/copy src-file dst-file)
    (when (.canExecute src-file)
      (.setExecutable dst-file true false))
    (cli/print-created-artifact dst-path)))


(defn- bump-minor []
  (let [file (str "conf/release.edn")
        data (-> file slurp edn/read-string)
        minor (get data :minor 0)
        minor (inc minor)
        data (assoc data :minor minor)]
    (spit file (puget/pprint-str data))
    (cli/print-created-artifact file)))


(defn release! []
  (cli/print-op "Release")

  (let [executable-name (:or (-> project/info :commandapp :executable-name)
                             (-> project/info :serverapp :executable-name))]

    (copy-target executable-name
                 executable-name)

    (when (:systemd project/info)
      (let [unit-name (-> project/info :systemd :unit-name)]
        (copy-target (str "systemd/" unit-name ".service")
                     (str unit-name ".service"))))

    (when (-> project/info :serverapp :vhost)
      (copy-target "nginx-vhost"
                   "nginx-vhost"))

    (copy-dir "configs")

    (copy-target "install.bsh"
                 "install.bsh")

    (bump-minor)))

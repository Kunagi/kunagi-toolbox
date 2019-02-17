(ns toolbox.cli
  (:require
   [clojure.term.colors :as c]))


(defn print-op
  [operation & args]
  (println)
  (print
   (c/on-grey (c/white (str " " operation " "))))
  (print " ")
  (doseq [arg args]
    (print arg))
  (println))


(defn print-created-artifact
  [path]
  (println)
  (print " -> ")
  (println (c/on-green (c/white (str " " path " ")))))


(defn print-info
  [])


(defn abort-with-failure
  [& texts]
  (println)
  (when (seq texts)
    (apply print texts)
    (println)
    (println))
  (println (c/on-red (c/white " FAILED ")))
  (System/exit 1))

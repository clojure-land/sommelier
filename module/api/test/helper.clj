(ns helper
                              (:require [cheshire.core :refer :all]
                                        [clojure.walk :refer :all]))

(defn parse-body [body]
  (if (not= body "")
    (->
      (slurp body)
      (parse-string)
      (keywordize-keys))))
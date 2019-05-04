(ns apriori.util.storage.redis
  (:require [taoensso.carmine :as car :refer (wcar)]))

(def redis-conn {:pool {} :spec {:host "redis" :port 6379}})

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn set [key value]
  (car/set key value))

(defn expire [key expire]
  (car/expire key expire))

(defn get [key]
  (car/get key))
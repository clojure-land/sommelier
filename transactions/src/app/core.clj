(ns app.core
  (:refer-clojure :exclude [sort find])
  (:require [monger.core :as mg]
            [monger.query :as mq]
            [monger.operators :as mo]
            [monger.collection :as mc]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.math.combinatorics :as combo]
            [amazonica.aws.sqs :as sqs])
  (:gen-class))

(def host
  "localhost")

(def queue
  "http://localstack:4576/queue/sommelier-apriori")

(ns core
    (:require [monger.core :as mg]
      [monger.query :as mq]
      [monger.collection :as mc]
      [clojure.tools.logging :as log]
      [amazonica.aws.sqs :as sqs]
      [cheshire.core :as json])
    (:import [org.bson.types ObjectId])
    (:gen-class))

(def host
  "localhost")

(def queue
  "http://localhost:4576/queue/sommelier-apriori")

(def batch-size
  1000)

(def conn
  (mg/connect {:host host :port 27017}))

(defn- fetch-scheduled
  "Retrieves scheduled jobs"
  []

  (let [db (mg/get-db conn "sommelier")]

      (mq/with-collection db "job"
                          (mq/find {:state "scheduled"}))))

(defn- processing-job
  "Updates a job."
  [job]

  (let [db (mg/get-db conn "sommelier")]
    (mc/update-by-id db "job" (ObjectId. (str (job :_id))) {:project-id (ObjectId. (str (job :project-id)))
                                                            :state "scheduled" ;todo: processing
                                                            :transactions (job :transactions)})))

(defn count-transactions
  "Fetches total number of transactions."
  [job-id]

  (let [db (mg/get-db conn (str "job_" job-id))]
    (mc/count db "transactions")))

(defn -main []
  (doseq [job (fetch-scheduled)]
    (log/info {:job-id (str (job :_id))})

    (let [pages (int (Math/ceil (float (/ (count-transactions (str (job :_id))) batch-size))))]
      (processing-job job)

      (dotimes [page pages]
        (sqs/send-message queue (json/encode {:job-id (str (job :_id))
                                              :project-id (str (job :project-id))
                                              :transactions-total (job :transactions)
                                              :page page
                                              :per-page batch-size
                                              :total-pages pages}))))))

(ns util.task
  (:require [amazonica.aws.sqs :as sqs]
            [cheshire.core :as json]
            [util.mdb :as mdb])
  (:import [org.bson.types ObjectId])
  (:gen-class))

(def queue
  "http://localhost:4576/queue/sommelier-apriori")

(def batch-size
  1000)

(defn run [task-id project-id]
  (let [task (first (mdb/get-tasks {:_id (ObjectId. (str task-id))}))
        pages (int (Math/ceil (float (/ (mdb/count-transactions task-id) batch-size))))]
    (mdb/save-task task-id {"$set" {:state "processing"}})

    (dotimes [page pages]
      (sqs/send-message queue (json/encode {:task-id task-id
                                            :project-id project-id
                                            :transactions-total (get task :transactions) ;;todo: pull pack from task not count
                                            :page page
                                            :per-page batch-size
                                            :total-pages pages})))))

(ns core
    (:require [monger.core :as mg]
      [monger.query :as mq]
      [monger.collection :as mc]
      [clojure.tools.logging :as log]
      [amazonica.aws.sqs :as sqs]
      [cheshire.core :as json])
    (:gen-class))

(def host
  "localhost")

(def queue
  "http://localhost:4576/queue/sommelier-event")

(def conn
  (mg/connect {:host host :port 27017}))

(defn- fetch-scheduled
  "Retrieves scheduled tasks"
  []

  (let [db (mg/get-db conn "sommelier")]
      (mq/with-collection db "task"
                          (mq/find {:state "scheduled"}))))

(defn -main []
  (doseq [task (fetch-scheduled)]
    (log/info {:task-id (str (task :_id))})

   (sqs/send-message queue (json/encode {:type "task"
                                         :task-id (str (task :_id))
                                         :project-id (str (task :project-id))}))))

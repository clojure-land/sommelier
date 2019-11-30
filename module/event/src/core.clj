(ns core
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [util.task :as task]
            [util.transaction :as transaction]
            [amazonica.aws.sqs :as sqs])
  (:gen-class))

(def queue
  "http://localhost:4576/queue/sommelier-event")

;aws --endpoint http://localhost:4576 sqs list-queues
;aws --endpoint http://localhost:4576 sqs get-queue-attributes --queue-url=http://localhost:4576/queue/sommelier-event
;aws --endpoint http://localhost:4576 sqs receive-message --queue-url=http://localhost:4576/queue/sommelier-event
;aws --endpoint http://localhost:4576 sqs send-message --queue-url http://localhost:4576/queue/sommelier-event --message-body 'Test Message!'
;aws --endpoint http://localhost:4576  sqs delete-message --queue-url http://localhost:4576/queue/sommelier-event --receipt-handle 'd037d9e7-f142-4047-af83-c7b460cc302d#bcd95dd3-b345-459a-8384-d4c0907b441b'

(def max-threads 10)

(def lock (atom 0))

(defn- process-message
  "Processes queued message."
  [msg]

  (try
    (let [body (json/decode (msg :body) true)]
      (log/info body)

      (case (body :type)
        "task" (task/run (body :task-id)
                         (body :project-id))
        "transaction" (transaction/run (body :task-id)
                                       (body :transaction)
                                       (body :count))
        (log/error "Invalid msg type!"))

      (sqs/delete-message (assoc msg :queue-url queue)))
    (catch Exception e (prn e))))

(defn -main []
  (while true
    (log/info "Running" @lock "of" max-threads "threads.")
    (if (and (< (deref lock) max-threads) (> (- max-threads @lock) 0))
      (let [messages (sqs/receive-message :queue-url queue :max-number-of-messages (- max-threads @lock))]
        (doseq [msg (get messages :messages)]
          (future (swap! lock inc)
                  (process-message msg)
                  (swap! lock dec))))
      (log/info "Waiting for free thread slot to become available."))
    (Thread/sleep 10000)))

(ns core
    (:refer-clojure :exclude [sort find])
    (:require [monger.core :as mg]
      [monger.query :as mq]
      [monger.operators :as mo]
      [monger.collection :as mc]
      [amazonica.aws.sqs :as sqs]
      [cheshire.core :as json])
    (:import [org.bson.types ObjectId])
    (:gen-class))

(def host
  "localhost")

(def queue
  "http://localhost:4576/queue/sommelier-apriori")

(def batch-size
  100)

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
                                                            :state "processing"
                                                            :transactions (job :transactions)})))

(defn count-transactions
  "Fetches total number of transactions."
  [job-id]

  (let [db (mg/get-db conn (str "job_" job-id))]
    (mc/count db "transactions")))

(defn -main []
  (doseq [job (fetch-scheduled)]
    (let [pages (int (Math/ceil (float (/ (count-transactions (str (job :_id))) batch-size))))]
      (processing-job job)

      (dotimes [page pages]
        (sqs/send-message queue (json/encode {:job-id (str (job :_id))
                                              :project-id (str (job :project-id))
                                              :transactions-total (job :transactions)
                                              :page page
                                              :per-page batch-size
                                              :total-pages pages}))))))








;[clj-http.client :as client]

;(defn- fetch-transactions
;       "Retrieves transactions paginated."
;       [db-name page]
;
;       (let [conn (mg/connect {:host host :port 27017})
;             db   (mg/get-db conn db-name)]
;
;            (mq/with-collection db "transactions"
;                                (mq/paginate :page page :per-page 1000))))

;(defn random-transaction [x]
;      (let [transaction (take
;                          (rand-int x)
;                          ((partial shuffle ["a" "b" "c" "d" "e", "f", "g", "h", "i", "j", "k"])))]
;
;           (if (empty? transaction)
;             (random-transaction x) transaction)))

;(defn test-data []
;      (reduce (fn [x number]
;                  (conj x (vec
;                            (if (= (rand-int 4) 1)
;                              (random-transaction 10)
;                              (try
;                                (rand-nth x)
;                                (catch Exception e (random-transaction 10)))))))
;              []
;              (take 1000 (iterate inc 1))))

;(defn post-data []
;      (dotimes [i 10]
;               (->
;                 (client/post "http://localhost:3000/v1/project/5d481cc224aa9a00077152b5/transactions"
;                              {:body (json/generate-string {:transactions (test-data)})
;                               :content-type :json
;                               :accept :json})
;                 (get :body)
;                 (json/decode)
;                 (get "data")
;                 (first)
;                 (get-in ["attributes" "transactions"])
;                 (println))))

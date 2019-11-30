(ns util.mdb
  (:refer-clojure :exclude [sort find])
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [clojure.tools.logging :as log]
            [monger.result :refer [acknowledged?]]
            [monger.query :refer :all])
  (:import [org.bson.types ObjectId]))

(def conn
  (mg/connect {:host "localhost" :port 27017}))

;; ***** Task collection ********************************************************

(defn get-tasks
  "Retrieves tasks from mdb."
  [ref]

  (let [db (mg/get-db conn "sommelier")]
    (mc/find-maps db "task" ref)))

(defn save-task
  "Inserts or updates a task."
  [^org.bson.types.ObjectId id document]

  (let [db   (mg/get-db conn "sommelier")]
    (if (some? id)
      (do
        (mc/update-by-id db "task" (ObjectId. (str id)) document)
        (get-tasks {:_id (ObjectId. (str id))}))
      (mc/insert-and-return db "task" document))))

;; ***** Transactions collection ********************************************************

(defn count-transactions
  "Fetches total number of transactions."
  [task-id]

  (let [db (mg/get-db conn (str "task_" task-id))]
    (mc/count db "transactions")))

(defn get-transactions
  "Retrieves transactions from mdb."
  [task-id ref]

  (let [db   (mg/get-db conn (str "task_" task-id))]
    (mc/find-maps db "transactions" ref)))

(defn save-transactions
  "Inserts or updates transactions in mdb."
  [task-id conditions document]

  (let [db (mg/get-db conn (str "task_"task-id))]
    (mc/ensure-index db "transactions" (array-map :transaction 1) {:unique true})

    (if (some? conditions)
      (mc/update db "transactions" conditions document {:multi true})
      (mc/insert-batch db "transactions" document))))

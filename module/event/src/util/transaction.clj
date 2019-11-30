(ns util.transaction
  (:require [util.mdb :as mdb]
            [monger.json]
            [clojure.tools.logging :as log]
            [clojure.core.matrix :as matrix]
            [clojure.core.reducers :as reducers]
            [clojure.math.combinatorics :as combo])
  (:import [org.bson.types ObjectId]))

(defn- matrix-concat
  "Concatenates each coll in matrix.

  e.g. (matrix-concat [[1 2 3] [4 5 6]])"
  [matrix]

  (if (matrix/matrix? matrix) (map (fn [coll] (clojure.string/join "," coll)) matrix) []))

(defn- matrix-subsets
  "Returns subsets of each coll in matrix.

   e.g. (matrix-subsets [[1 2 3] [4 5 6]])"
  [matrix]

  (if (matrix/matrix? matrix)
    (apply concat (map (fn [coll]
                         (log/info (combo/count-subsets coll) "subsets found.")
                         (combo/subsets coll)) matrix)) []))

(defn- get-transactions
  "This function partitions transactions to prevent exceeding the mdb maximum document size."
  [task-id transactions]

  (reduce (fn [transactions in]
            (log/info "Retrieve" (count in) "transactions.")
            (concat transactions (mdb/get-transactions task-id {:transaction {"$in" in}})))
          [] (partition 100000 100000 [""] transactions)))

(defn- group-transactions-by-operation
  "Groups transaction frequencies by insert or update operation.

  e.g. (group-by-operation #object[org.bson.types.ObjectId 0x9359d8f '5da049fb9194be00066ac076'] ['a'])"
  [^org.bson.types.ObjectId task-id subsets]

  (let [transaction-ids (reduce
                          (fn [transactions transaction]
                            (assoc transactions (str (get transaction :transaction)) (get transaction :_id)))
                          {} (get-transactions task-id subsets))]

    (reducers/fold
      (fn
        ([operations transaction]
          (if (contains? transaction-ids transaction)
            (update operations :update conj (get transaction-ids transaction))
            (update operations :insert conj transaction)))
       ([] [] {:insert [] :update []}))
      subsets)))

(defn- update-transactions-freq
  "Updates frequency of transactions.

  e.g (update-transactions-freq #object[org.bson.types.ObjectId 0x76e5c85d '5d9f562b0e8e3d00066e6ab9'] [#object[org.bson.types.ObjectId 0x30af87ef '5da049fb9194be00066ac077']] 1)"
  [^org.bson.types.ObjectId task-id update-transactions amount]

  (if (not (empty? update-transactions))
    (do
      (log/info "Update" (count update-transactions) "transaction frequencies.")
      (mdb/save-transactions task-id {:_id {"$in" update-transactions}} {"$inc" {:count amount}}))))

(defn- insert-transactions-freq
  "Inserts frequency of transactions.

  e.g. (insert-transactions-freq #object[org.bson.types.ObjectId 0x76e5c85d '5d9f562b0e8e3d00066e6ab9'] ['a', 'b', 'c'] 1)"
  [^org.bson.types.ObjectId task-id insert-transactions amount retry]

  (if (not (empty? insert-transactions))
    (do
      (log/info "Insert" (count insert-transactions) "transaction frequencies.")

      (try
        (mdb/save-transactions task-id nil (map (fn [transaction] {:transaction transaction :count amount}) insert-transactions))
        (catch com.mongodb.DuplicateKeyException e
          (log/warn "Duplicate key, this may happen when another thread has already done the insert.")

          (let [wait (rand-int 5000)]
            (log/info "Retry in" wait "seconds.")
            (Thread/sleep wait)

            (let [operation (group-transactions-by-operation task-id insert-transactions)]
              (update-transactions-freq task-id (get operation :update) amount)

              (if (> retry 0)
                (insert-transactions-freq task-id (get operation :insert) amount (dec retry))
                (log/error "Retry failed!")))))))))

(defn- get-subsets [transaction]
  (->>
    (vector transaction (reverse transaction))
    (matrix-subsets)
    (matrix-concat)
    (filter (fn [x] (not= x "")))
    (distinct)))

(defn run
  "Inserts or updates frequency of transactions.

  e.g. (save-transactions-freq '507f191e810c19729de860ea' ['a' 'b' 'c'] 1)"
  [task-id transaction amount]

  ; race condition some where here.
  (Thread/sleep (rand-int 5000))

  (let [operation (group-transactions-by-operation task-id (get-subsets transaction))
        task (mdb/get-tasks {:_id (ObjectId. (str task-id)) :state "scheduled"})]
    (if (not= task [])
      (do
        (update-transactions-freq task-id (get operation :update) amount)
        (insert-transactions-freq task-id (get operation :insert) amount 3)
        (mdb/save-task task-id {"$inc" {:transactions amount}}))
      (log/warn "Task" task-id "is not scheduled."))))

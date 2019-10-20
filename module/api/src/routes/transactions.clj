(ns routes.transactions
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :as mdb]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.transactions :refer :all]
            [domain.job :refer :all]
            [monger.json]
            [clojure.core.matrix :refer :all]
            [clojure.math.combinatorics :as combo])
  (:import [org.bson.types ObjectId]))

;; ***** Transactions implementation ********************************************************

(defn- matrix-concat
  "Concatenates each coll in matrix.

  e.g. (matrix-concat [[1 2 3] [4 5 6]])"
  [matrix]

  (if (matrix? matrix)
    (map (fn [coll] (clojure.string/join "," coll)) matrix) []))

(defn- matrix-subsets
  "Returns subsets of each coll in matrix.

   e.g. (matrix-subsets [[1 2 3] [4 5 6]])"
  [matrix]

  (if (matrix? matrix)
    (apply concat (map (fn [coll] (combo/subsets coll)) matrix)) []))

(defn- group-by-val
  "Groups keys by value.

  e.g (group-by-val {:key 1})"
  [coll]

  (if (coll? coll)
    (reduce (fn [grouped item]
              (->>
                (key item)
                (conj (get grouped (val item)))
                (assoc grouped (val item)))) {} coll) {}))

(defn- group-transactions-by-operation
  "Groups transaction frequencies by insert or update operation.

  e.g. (group-by-operation #object[org.bson.types.ObjectId 0x9359d8f '5da049fb9194be00066ac076'] {'a' 1})"
  [^org.bson.types.ObjectId job-id transaction-freq]

  (let [transactions (mdb/get-transactions job-id {:transaction {"$in" (keys transaction-freq)}})]
    (reduce (fn [group freq]
              (let [transaction (first (filter #(= (get % :transaction) (key freq)) transactions))]
                (if (empty? transaction)
                  (assoc-in group [:insert (key freq)] (val freq))
                  (assoc-in group [:update (get transaction :_id)] (val freq))))) {:insert {} :update {}} transaction-freq)))

(defn- scheduled-job!
  "Retrieves the scheduled job, if none exists then creates a new one.

  e.g. (get-scheduled-job '18cc8865-0868-42ba-8668-0821e976e3b9')"
  [project-id]

  (let [job (mdb/get-job {:project-id (ObjectId. (str project-id)) :state "scheduled"})]
    (if (= job [])
      (mdb/save-job nil {:project-id (ObjectId. (str project-id)) :state "scheduled" :transactions 0})
      (first job))))

(defn- update-transactions-freq
  "Updates frequency of transactions.

  e.g (update-transactions-freq #object[org.bson.types.ObjectId 0x76e5c85d '5d9f562b0e8e3d00066e6ab9'] {#object[org.bson.types.ObjectId 0x30af87ef '5da049fb9194be00066ac077'] 2})"
  [^org.bson.types.ObjectId job-id update-transactions]

  (if (not (empty? update-transactions))
    (doseq [row (group-by-val update-transactions)]
      (mdb/save-transactions job-id {:_id {"$in" (val row)}} {"$inc" {:count (key row)}}))))

(defn- insert-transactions-freq
  "Inserts frequency of transactions.

  e.g. (insert-transactions-freq #object[org.bson.types.ObjectId 0x76e5c85d '5d9f562b0e8e3d00066e6ab9'] {'a' 1, 'b' 3, 'c' 7})"
  [^org.bson.types.ObjectId job-id insert-transactions]

  (if (not (empty? insert-transactions))
    (mdb/save-transactions job-id nil (map (fn [row] {:transaction (key row) :count (val row)}) insert-transactions))))

(defn- save-transactions-freq
  "Inserts or updates frequency of transactions.

  e.g. (save-transactions-freq '5d9f562a0e8e3d00066e6ab8' 'user' {:transactions [['a' 'b' 'c']]})"
  [project-id author body]

  (if (and (mdb/objectId? project-id)
           (not= (mdb/get-project {:_id (ObjectId. (str project-id)) :author author}) []))

    (let [job (scheduled-job! project-id)
          transactions (->>
                         (get body :transactions)
                         (matrix-subsets)
                         (matrix-concat)
                         (frequencies)
                         (group-transactions-by-operation (get job :_id)))]

      (update-transactions-freq (get job :_id) (get transactions :update))
      (insert-transactions-freq (get job :_id) (get transactions :insert))

      (api-response
        (->>
          (mdb/save-job (get job :_id) {"$inc" {:transactions (count (get body :transactions))}})
          (job->resource-object)
          (->ApiData {:status "accepted"}))
        202 [{:content-location (str "/v1/job/" (get job :_id))}]))
  (project-not-found project-id)))

;; ***** Transactions definition *******************************************************

(def transactions-routes
  (POST "/:id/transactions" []
    :path-params [id :- ProjectId]
    :tags ["project"]
    :operationId "appendTransactions"
    :summary "Appends transactions for processing."
    :body [transactions TransactionsSchema]
    ;:middleware [#(util.auth/auth! %)]
    ;:current-user profile
    :responses {202 {:schema {:meta Meta :data (vector (assoc DataObject :attributes JobSchema))}
                     :description "accepted"}
                400 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "bad request"}
                403 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "unauthorized"}
                404 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "not found"}} (save-transactions-freq id "" transactions)))
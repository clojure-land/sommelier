(ns routes.transactions
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :as mdb]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.transactions :refer :all]
            [domain.task :refer :all]
            [cheshire.core :as json]
            [amazonica.aws.sqs :as sqs]
            [monger.json])
  (:import [org.bson.types ObjectId]))

;; ***** Transactions implementation ********************************************************

(def queue
  "http://localstack:4576/queue/sommelier-event")

(defn- scheduled-task!
  "Retrieves the scheduled task, if none exists then creates a new one.

  e.g. (get-scheduled-task '18cc8865-0868-42ba-8668-0821e976e3b9')"
  [project-id]

  (let [task (mdb/get-tasks {:project-id (ObjectId. (str project-id)) :state "scheduled"})]
    (if (= task [])
      (mdb/save-task nil {:project-id (ObjectId. (str project-id)) :state "scheduled" :transactions 0})
      (first task))))

(defn- save-transactions-freq
  "Inserts or updates frequency of transactions.

  e.g. (save-transactions-freq {:sub ''} '5d9f562a0e8e3d00066e6ab8' {:transactions [['a' 'b' 'c']]})"
  [profile id body]

  (if-let [project (mdb/get-project-if-exists id)]
    ; todo: check if project is archived.
    (if (has-permission? (get profile :sub) id "project")
      (let [task (scheduled-task! id)]
        (doseq [transaction (frequencies (partition 1 (get body :transactions)))]
              (sqs/send-message queue (json/encode {:type "transaction"
                                                    :project-id (get task :project-id)
                                                    :task-id (get task :_id)
                                                    :transaction (first (key transaction))
                                                    :count (val transaction)})))

        (api-response (->>
                        (update task :transactions + (count (get body :transactions)))
                        (vector)
                        (task->resource-object)
                        (->ApiData {:status "accepted"})) 202 [{:content-location (str "/v1/task/" (get task :_id))}]))
      (forbidden))
  (project-not-found id)))

;; ***** Transactions definition *******************************************************

(def transactions-routes
  (POST "/:id/transactions" []
    :path-params [id :- ProjectId]
    :tags ["project"]
    :operationId "appendTransactions"
    :summary "Appends transactions to a new or existing scheduled task."
    :body [transactions TransactionsSchema]
    :middleware [#(auth! %)]
    :current-user profile
    :responses {202 {:schema {:meta Meta :data (vector (assoc DataObject :attributes TaskSchema))}
                     :description "accepted"}
                400 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "bad request"}
                401 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "unauthorized"}
                403 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "forbidden"}
                404 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "not found"}} (save-transactions-freq profile id transactions)))

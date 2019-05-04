(ns util.mdb
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(defn objectId? [id]
  "Verify that id is an objectId."

  (try
    (do
      (ObjectId. (str id)) true)
    (catch Exception ex false)))

;; ***** Project collection ********************************************************

(defn get-project
  "Retrieves projects from mdb."
  [ref]

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db   (mg/get-db conn "sommelier")]

    (mc/find-maps db "project" ref)))

(defn save-project
  "Inserts or updates a project in mdb."
  [id project]

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db   (mg/get-db conn "sommelier")]

    (if (some? id)
      (do
        (mc/update-by-id db "project" (ObjectId. (str id)) project)
        (get-project {:_id (ObjectId. (str id))}))
      (vector (mc/insert-and-return db "project" project)))))

(defn remove-project!
  "Permanently removes a project from mdb."
  [id]

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db (mg/get-db conn "sommelier")]

    (mc/remove-by-id db "project" id)))

;; ***** Transactions collection ********************************************************

(defn get-transactions
  [jobId ref]

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db   (mg/get-db conn (str "job_" jobId))]

    (mc/find-maps db "transactions" ref)))

(defn update-transactions
  [jobId ref document]

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db (mg/get-db conn (str "job_" jobId))]

    (mc/update db "transactions" ref document)))

(defn insert-transactions
  [jobId documents]

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db (mg/get-db conn (str "job_" jobId))]

    (mc/insert-batch db "transactions" documents)))

(defn count-transactions [projectId]
  (let [conn (mg/connect {:host "mongo" :port 27017})
        db (mg/get-db conn (str "job_" projectId))]

    (mc/aggregate db "transactions" [{"$group" {:_id "$transaction" :count {"$sum" 1}}}])))

;; ***** Associations collection ********************************************************

; job => {:jobId "", :projectId "", status "", datetime ""}
; rules => {:itemSet "", :jobId "", :support 0, :confidence 0, :lift 0}
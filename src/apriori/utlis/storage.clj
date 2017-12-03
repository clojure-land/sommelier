(ns apriori.utlis.storage
  (:import [org.postgresql.util PGobject])
  (:require [clojure.java.jdbc :refer :all]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]))

; database config
(def db {:dbtype   "postgresql"
         :dbname   (env :db-name)
         :port     (env :db-port)
         :host     (env :db-host)
         :user     (env :db-user)
         :password (env :db-password)})

;(defn create-db []
;  ; comment if you don't want to recreate the table during compilation
;  ;(execute! db ["drop table if exists project"])
;  (execute! db ["drop table if exists data_set"])
;  (execute! db ["drop table if exists frequency_item_set"])
;
;  ; create project table
;  (try
;    (db-do-commands db
;                    (create-table-ddl :project
;                                      [[:id "varchar(256)" "PRIMARY KEY"]
;                                       [:name "string"]
;                                       [:support "integer"]
;                                       [:confidence "integer"]
;                                       ; [:schema "text"]
;                                       ; [:data-set "string"]
;                                       [:modified "timestamp"]]))
;    (catch Exception e
;      (log/warn (.getMessage e))))
;
;  ; create data_set tables
;  (try
;    (db-do-commands db
;                    (create-table-ddl :data_set
;                                      [[:id "integer" "PRIMARY KEY", "AUTOINCREMENT"]
;                                       [:data "text"]
;                                       [:created "timestamp"]]))
;    (catch Exception e
;      (log/warn (.getMessage e))))
;
;  ; create frequency_item_set tables
;  (try
;    (db-do-commands db
;                    (create-table-ddl :frequency_item_set
;                                      [[:id "integer" "PRIMARY KEY", "AUTOINCREMENT"]
;                                       [:item "text"]
;                                       [:frequency "integer"]
;                                       [:support "integer"]
;                                       [:confidence "integer"]
;                                       [:lift "integer"]
;                                       [:created "timestamp"]]))
;    (catch Exception e
;      (log/warn (.getMessage e)))))
;
;; create the db
;(create-db)
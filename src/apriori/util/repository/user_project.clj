(ns apriori.util.repository.user_project
  (:require [apriori.util.storage :as storage]
            [apriori.util.logger :as logger]
            [apriori.util.repository.user :as user]
            [schema.core :as schema]))

(def UserId
  schema/Uuid)

(def ProjectId
  schema/Uuid)

(schema/defschema UserProjectRepository
  {(schema/required-key :user_id)    UserId
   (schema/required-key :project_id) ProjectId})

(schema/defrecord ^UserProjectRepository UserProjectRecord
  [user_id :- UserId
   project_id :- ProjectId])

(schema/defn get-user-id :- UserId [user-project :- UserProjectRecord] (:user_id user-project))
(schema/defn get-project-id :- ProjectId [user-project :- UserProjectRecord] (:project_id user-project))

(schema/defn filter-by-user-id :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by UserId."
  [user_id :- UserId]

  (logger/log :debug {:meta       #'filter-by-user-id
                      :arg-values {:id (str user_id)}})

  (if (not-empty (str user_id))
    {:user_project.user_id user_id}))

(schema/defn filter-by-project-id :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by ProjectId."
  [project_id :- ProjectId]

  (logger/log :debug {:meta       #'filter-by-user-id
                      :arg-values {:id (str project_id)}})

  (if (not-empty (str project_id))
    {:user_project.project_id project_id}))

(schema/defn fetch :- [UserProjectRepository]
  "Fetch user_project records from storage.

  e.g. (fetch ([filter-by-user-id f7d9ffed-e535-4cd7-8f01-78948af8f528]
               [filter-by-project-id f7d9ffed-e535-4cd7-8f01-78948af8f528])"
  [conditions :- [(schema/maybe {schema/Keyword schema/Any})]]

  (logger/log :debug {:meta       #'fetch
                      :arg-values {:conditions (str conditions)}})

  (map (fn [row]
         (map->UserProjectRecord
           (storage/transform row [[:user_id :read-uuid]
                                   [:project_id :read-uuid]] [:*] [])))
       (storage/fetch-entity storage/project [:*] conditions nil [:user_id :project_id])))

(schema/defn save :- UserProjectRepository
  "Save user_project record to storage.

  e.g. (save (->UserProjectRecord(...))"
  [user-project :- UserProjectRepository]

  (logger/log :debug {:meta       #'save
                      :arg-values {:user (str user-project)}})

  (storage/save-entity storage/user_project
                       (storage/transform user-project [[:user_id :write-uuid]
                                                        [:project_id :write-uuid]] [:*] [])
                       {}
                       [(filter-by-user-id (get-user-id user-project))
                        (filter-by-project-id (get-project-id user-project))]))
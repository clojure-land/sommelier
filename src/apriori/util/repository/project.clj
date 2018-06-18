(ns apriori.util.repository.project
  (:require [apriori.util.storage :as storage]
            [apriori.util.logger :as logger]
            [schema.core :as schema]))

(def Id
  schema/Uuid)

(def Name
  schema/Str)

(def Author
  schema/Uuid)

(def Transactions
  schema/Num)

(def Modified
  (schema/maybe schema/Inst))

(def Created
  (schema/maybe schema/Inst))

(schema/defschema ProjectRepository
  {(schema/required-key :id)           Id
   (schema/required-key :name)         Name
   (schema/required-key :author)       Author
   (schema/required-key :transactions) Transactions
   (schema/optional-key :modified)     Modified
   (schema/optional-key :created)      Created})

(schema/defrecord ^ProjectRepository ProjectRecord
  [id :- Id
   name :- Name
   author :- Author
   transactions :- Transactions
   modified :- Modified
   created :- Created])

(schema/defn get-id :- Id [project-record :- ProjectRecord] (:id project-record))

(schema/defn filter-by-id :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by Id."
  [id :- Id]

  (logger/log :debug {:meta       #'filter-by-id
                      :arg-values {:id (str id)}})

  (if (not-empty (str id))
    {:project.id id}))

(schema/defn filter-by-name :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by Name."
  [name :- Name]

  (logger/log :debug {:meta       #'filter-by-name
                      :arg-values {:name (str name)}})

  (if (not-empty (str name))
    {:project.name name}))

(schema/defn fetch :- [ProjectRepository]
  "Fetch project records from storage.

  e.g. (fetch ([filter-by-id f7d9ffed-e535-4cd7-8f01-78948af8f528])"
  [conditions :- [(schema/maybe {schema/Keyword schema/Any})]]

  (logger/log :debug {:meta       #'fetch
                      :arg-values {:conditions (str conditions)}})

  (map (fn [row]
         (map->ProjectRecord
           (storage/transform row [[:id :read-uuid]
                                   [:author :read-uuid]
                                   [:created :read-timestamp]] [:*] [])))
       (storage/fetch-entity storage/project [:*] conditions nil [:id])))

(schema/defn save :- ProjectRepository
  "Save project record to storage.

  e.g. (save (->ProjectRecord(...))"
  [project :- ProjectRepository]

  (logger/log :debug {:meta       #'save
                      :arg-values {:user (str project)}})

  (storage/save-entity storage/project
                       (storage/transform project [[:author :write-uuid]] [:*] [:id :created])
                       (storage/transform project [[:id :write-uuid]
                                                   [:created :write-timestamp]] [:id :created] [])
                       [(filter-by-id (get-id project))]))
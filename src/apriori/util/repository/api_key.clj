(ns apriori.util.repository.api_key
  (:require [apriori.util.storage :as storage]
            [apriori.util.logger :as logger]
            [schema.core :as schema]))

(def UserId
  schema/Uuid)

(def Token
  schema/Str)

(def RefreshToken
  (schema/maybe schema/Str))

(def Expires
  (schema/maybe schema/Inst))

(schema/defschema ApiKeyRepository
  {(schema/required-key :user_id)       UserId
   (schema/required-key :token)         Token
   (schema/optional-key :refresh_token) RefreshToken
   (schema/optional-key :expires)       Expires})

(schema/defrecord ^ApiKeyRepository ApiKeyRecord
  [user_id :- UserId
   token :- Token
   refresh_token :- RefreshToken
   expires :- Expires])

(schema/defn get-user-id :- UserId [user-record :- ApiKeyRecord] (:user_id user-record))

(schema/defn filter-by-user-id :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by UserId."
  [user_id :- UserId]

  (logger/log :debug {:meta       #'filter-by-user-id
                      :arg-values {:user_id (str user_id)}})

  (if (not-empty (str user_id))
    {:api_key.user_id user_id}))

(schema/defn fetch :- [ApiKeyRepository]
  "Fetch database api_key records.

  e.g. (fetch [(filter-by-id f7d9ffed-e535-4cd7-8f01-78948af8f528)]"
  [conditions :- [(schema/maybe {schema/Keyword schema/Any})]]

  (logger/log :debug {:meta       #'fetch
                      :arg-values {:conditions (str conditions)}})

  (map (fn [row]
         (map->ApiKeyRecord
           (storage/transform row [[:user_id :read-uuid]] [:*] [])))
       (storage/fetch-entity storage/api_key [:*] conditions nil [:id])))

(schema/defn save :- ApiKeyRepository
  "Save database api_key record.

  e.g. (save (->ApiKeyRecord(...))"
  [api_key :- ApiKeyRepository]

  (logger/log :debug {:meta       #'save
                      :arg-values {:user (str api_key)}})

  (storage/save-entity storage/user
                       (storage/transform api_key [] [:*] [:user_id])
                       (storage/transform api_key [[:user_id :write-uuid]] [:user_id] [])
                       [(filter-by-user-id (get-user-id api_key))]))
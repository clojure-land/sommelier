(ns domain.response
  (:require [schema.core :as schema]
            [clj-time.local :as local]
            [util.request :as request]))

(defprotocol ResponseProtocol
  (Get [this]))

(schema/def Meta
  {schema/Keyword schema/Any})

(schema/def DataObject
  {(schema/required-key :type)        schema/Str
   (schema/optional-key :id)          schema/Any
   (schema/required-key :attributes)  {schema/Keyword schema/Any}})

(schema/def ErrorObject
  {(schema/required-key :message) schema/Str
   (schema/optional-key :pointer) schema/Str})

(schema/defrecord ApiData
  [meta :- Meta
   data :- [DataObject]]

  ResponseProtocol

  (Get [this]
    {:meta (merge (:meta this) {:timestamp  (clj-time.coerce/to-date (local/local-now)) :request_id request/*id*})
     :data (:data this)}))

(schema/defrecord ApiError
  [meta :- Meta
   errors :- [ErrorObject]]

  ResponseProtocol

  (Get [this]
    {:meta (merge (:meta this) {:timestamp  (clj-time.coerce/to-date (local/local-now)) :request_id request/*id*})
     :errors (:errors this)}))
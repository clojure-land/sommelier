(ns apriori.domain.api
  (:require [apriori.util.logger :as logger]
            [schema.core :as schema]
            [clj-time.local :as local]
            [cheshire.core :as json]
            [clojure.walk :as walk]))

(def Meta
  {schema/Keyword schema/Any
   :status        schema/Str
   :timestamp     schema/Inst
   :request_id    (schema/maybe schema/Uuid)})

(def Data
  schema/Any)

(schema/defschema ApiResponseSchema
  {(schema/required-key :meta) Meta
   (schema/optional-key :data) Data})

(schema/defrecord ApiResponse
  [meta :- Meta
   data :- Data])

(schema/defn ->Meta [status :- schema/Str
                     meta-data :- (schema/maybe {schema/Keyword schema/Any})] :- Meta
  (let [meta {:status     status
              :timestamp  (clj-time.coerce/to-date (local/local-now))
              :request_id logger/*request-id*}]

    (if (not-empty meta-data)
      (merge meta meta-data) meta)))

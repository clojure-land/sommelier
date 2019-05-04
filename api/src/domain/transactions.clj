(ns domain.transactions
  (:require [schema.core :as schema]))

(schema/defschema TransactionsSchema
  {:transactions [[schema/Str]]})
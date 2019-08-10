(ns domain.transactions
  (:require [schema.core :as schema]))

(schema/def Transaction
  (->
    [schema/Any]
    (schema/constrained #(<= 1 (count %) 12) (list 'size? 1 12))))

(schema/defschema TransactionsSchema
  {:transactions [Transaction]})
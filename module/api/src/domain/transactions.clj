(ns domain.transactions
  (:require [schema.core :as schema]))

(schema/def Transaction
  (->
    [schema/Any]
    (schema/constrained #(<= 1 (count %) 15) (list 'size? 1 15))))

(schema/def Transactions
  (->
    [Transaction]
    (schema/constrained #(<= 1 (count %) 100) (list 'size? 1 100))))

(schema/defschema TransactionsSchema
  {:transactions Transactions})

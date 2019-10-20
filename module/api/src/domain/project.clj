(ns domain.project
  (:require [schema.core :as schema]))

(schema/def ProjectId
  schema/Str)

(schema/def ProjectName
  (->
    schema/Str
    (schema/constrained #(re-matches #"^[a-zA-Z0-9\s\d]*" %) 'alphanumeric?)
    (schema/constrained #(<= 0 (count %) 225) (list 'length? 0 225))))

(schema/def ProjectDescription
  (->
    (schema/maybe schema/Str)
    (schema/constrained #(re-matches #"^[a-zA-Z0-9\s\d,.!?]*" %) 'alphanumeric?)
    (schema/constrained #(<= 0 (count %) 1000) (list 'length? 0 1000))))

(schema/def Threshold
  (->
    schema/Num
    (schema/constrained #(<= 0 % 1.0) (list 'between? 0 1.0))))

(schema/def Window
  (->
    schema/Num
    (schema/constrained #(<= 0 % 30) (list 'between? 0 30))))

(schema/defschema ProjectSchema
  {(schema/required-key :name)            ProjectName
   (schema/required-key :description)     ProjectDescription
   (schema/required-key :window)          Window
   (schema/required-key :min-support)     Threshold
   (schema/required-key :min-confidence)  Threshold})

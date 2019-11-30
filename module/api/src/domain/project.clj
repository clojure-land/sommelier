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
    (schema/constrained #(<= 0 (count %) 1000) (list 'length? 0 1000))))

(schema/def Threshold
  (->
    schema/Num
    (schema/constrained #(<= 0 % 1.0) (list 'between? 0 1.0))))

(schema/def Mode
  (schema/enum "manual" "scheduled"))

(schema/def Scheduled
  (schema/enum "daily" "weekly" "monthly"))

(schema/def CleanUp
  (->
    schema/Num
    (schema/constrained #(<= 2 % 20) (list 'between? 2 20))))

(schema/defschema ProjectSchema
  {(schema/required-key :name)            ProjectName
   (schema/required-key :description)     ProjectDescription
   (schema/required-key :mode)            Mode
   (schema/optional-key :scheduled)       Scheduled
   (schema/required-key :clean-up)        CleanUp
   (schema/required-key :min-support)     Threshold
   (schema/required-key :min-confidence)  Threshold})

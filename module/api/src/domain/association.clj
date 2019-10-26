(ns domain.association
  (:require [schema.core :as schema]))

(schema/defschema AssociationSchema
  {(schema/required-key :antecedents) schema/Str
   (schema/required-key :antecedent-support) Double
   (schema/required-key :consequents) schema/Str
   (schema/required-key :consequent-support) Double
   (schema/required-key :support) Double
   (schema/required-key :confidence) Double
   (schema/required-key :lift) (schema/maybe Double)
   (schema/required-key :leverage) (schema/maybe Double)
   (schema/required-key :conviction) (schema/maybe Double)})

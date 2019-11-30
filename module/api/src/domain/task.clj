(ns domain.task
  (:require [schema.core :as schema]
            [domain.project :as project]))

(schema/def TaskId
  schema/Str)

(schema/def State
  (schema/enum "scheduled" "running" "done"))

(schema/defschema TaskSchema
  {(schema/required-key :project-id)    project/ProjectId
   (schema/required-key :state)         State
   (schema/required-key :transactions)  schema/Int})

(ns domain.job
  (:require [schema.core :as schema]))

(schema/def Status
  (schema/enum "idle" "pending" "running"))
(ns apriori.domain.base)

(defn get-base-uri [request]
  "Generate a base uri from a ring request."
  (let [scheme (name (:scheme request))
        context (:context request)
        hostname (get (:headers request) "host")]
    (str scheme "://" hostname)))
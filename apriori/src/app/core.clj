(ns app.core
  (:refer-clojure :exclude [sort find])
  (:require [monger.core :as mg]
            [monger.query :as mq]
            [monger.operators :as mo]
            [monger.collection :as mc]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.math.combinatorics :as combo]
            [amazonica.aws.sqs :as sqs])
  (:gen-class))

(def host
  "localhost")

(def queue
  "http://localstack:4576/queue/sommelier-apriori")

(def min-support
  0.3)

(def min-confidence
  0.3)

(def conn
  (mg/connect {:host host :port 27017}))

(defn- transactions->frequencies [transactions]
  (reduce (fn [coll row]
            (assoc coll (row :transaction) (row :count))) {} transactions))

(defn- transactions->antecedents [transactions]
  (reduce (fn [antecedents row]
            (let [antecedent (map #(clojure.string/join "," (first %)) (combo/partitions (str/split (row :transaction) #",") :min 2 :max 2))]
              (if (not (empty? antecedent))
                (concat antecedents antecedent) antecedents)))
          [] transactions))

(defn- transactions->consequents [transactions]
  (reduce (fn [consequents row]
            (let [consequent (map #(clojure.string/join "," (last %)) (combo/partitions (str/split (row :transaction) #",") :min 2 :max 2))]
              (if (not (empty? consequent))
                (concat consequents consequent) consequents)))
          [] transactions))

(defn- fetch-scheduled
  "Retrieves scheduled jobs."
  []

  (let [db (mg/get-db conn "sommelier")]

    (mq/with-collection db "job"
      (mq/find {:state "scheduled"}))))

(defn- fetch-transactions
  "Retrieves transactions paginated."
  [db-name page]

  (let [db   (mg/get-db conn db-name)]

  (mq/with-collection db "transactions"
    (mq/paginate :page page :per-page 1000))))

(defn- fetch-transactions-in-coll
  "Retrieves transactions in coll."
  [db-name coll]

  (let [db   (mg/get-db conn db-name)]

    (->>
      (mq/find {:transaction {"$in" coll}})
      (mq/with-collection db "transactions")
      (transactions->frequencies))))

(defn- save-rules [db-name rules]
  (let [db   (mg/get-db conn db-name)]

    ))

(defn- support
  "Indicates how frequently the item set appears in the data set.

  formula = support(A∪C)
  range = (0,1)"
  [total-transactions transactions]

  (reduce (fn [coll transaction]
            (let [support (float (/ (val transaction) total-transactions))]
              (assoc coll (key transaction) support))) {} transactions))

(defn- confidence
  "Indicates how often the rule has been found to be true.

  formula = support(A->C) / support(A)
  range = (0,1)"
  [support antecedent-support]

  (if (not (nil? support))
    (float (/ support antecedent-support))))

(defn- lift
  "Indicates the importance of the rule.

  > 1 lets us know the degree to which the occurrences are dependent on one another. This makes the rule potentially useful for predicting the consequent in future data sets.
  < 1 lets us know the items are substitute to each other. This means that the presence of one item has a negative effect on the presence of another item and vice versa.

  A lift value of 1 means independence. When two items are independent of each other, no rule can be drawn involving those two events.

  formula = confidence(A->C) / support(C)
  range = (0,∞)"
  [confidence consequent-support]

  (if (not (nil? confidence))
    (float (/ confidence consequent-support))))

(defn- leverage
  "Computes the difference between the observed frequency and independent frequencies.

  A leverage value of 0 indicates independence.

  formula = support(A->C) − support(A) * support(C)
  range = [-1,1]"
  [support antecedent-support consequent-support]

  (if (not (nil? support))
    (float (- support (* antecedent-support consequent-support)))))

(defn- conviction
  "Indicates by what factor the correctness of the rule would reduce if the antecedent and the consequent were independent.

  A conviction value of 1 indicates independence.

  Higher values far from 1 indicate interesting rules.

  formula = 1 − support(C) / 1 − confidence(A->C)
  range = [0,∞]"
  [confidence consequent-support]

  (try
    (float (/ (- 1 consequent-support) (- 1 confidence)))
    (catch Exception e)))

(defn- generate-rules
  "Generates association rules."
  [antecedents consequents support]

  (map (fn [antecedent consequent]
         (let [antecedent-support (get support antecedent)
               consequent-support  (get support consequent)
               support (min (get support antecedent) (get support consequent))
               confidence (confidence support antecedent-support)
               lift (lift confidence consequent-support)
               leverage (leverage support antecedent-support consequent-support)
               conviction (conviction confidence consequent-support)]

       {:antecedents antecedent
        :consequents consequent
        :antecedent-support antecedent-support
        :consequent-support consequent-support
        :support support
        :confidence confidence
        :lift lift
        :leverage leverage
        :conviction conviction})) antecedents consequents))

(defn- print-rules
       "Outputs association rules to the console."
       [rules]

       (doseq [x (sort-by :lift > rules)]
              (println x)))

(defn- apriori
  "Runs apriori for specified job batch."
  [job-id transactions-total page]

  (let [db-name (str "job_" job-id)
        transactions (fetch-transactions db-name page)
        antecedents (transactions->antecedents transactions)
        consequents (transactions->consequents transactions)]

      (->>
        (map (fn [antecedent consequent]
               (str antecedent "," consequent)) antecedents consequents)

        (concat antecedents consequents)
        (fetch-transactions-in-coll db-name)

        (support transactions-total)
        (generate-rules antecedents consequents)

        (filter #(and (not (nil? (get % :support))) (>= (get % :support) min-support)))
        (filter #(and (not (nil? (get % :confidence))) (>= (get % :confidence) min-confidence)))

        (print-rules)
        )))

(defn processed!
  "Removes message from queue."
  [msg]

  (sqs/delete-message (assoc msg :queue-url queue)))

(defn process-message
  "Processes queued message."
  [msg]

  (try
    (let [body (json/decode (msg :body) true)]
      (apriori (body :job-id)
               (body :transactions-total)
               (body :page))

      (processed! msg))
    (catch Exception e (prn e))))

(defn -main []
  (while true
    (let [messages (sqs/receive-message :queue-url queue
                                        :max-number-of-messages 10)]
      (doseq [msg (get messages :messages)]
        (future (process-message msg))))
    (Thread/sleep 20000)))

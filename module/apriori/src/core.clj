(ns core
  (:refer-clojure :exclude [sort find])
  (:require [monger.core :as mg]
            [monger.query :as mq]
            [monger.collection :as mc]
            [monger.result :refer [acknowledged?]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.math.combinatorics :as combo]
            [amazonica.aws.sqs :as sqs])
  (:gen-class))

(def queue
  "http://localhost:4576/queue/sommelier-apriori")

(def max-threads 10)

(def lock (atom 0))

(def batch-size
  1000)

(def min-support
  0.3)

(def min-confidence
  0.3)

(def conn
  (mg/connect {:host "localhost" :port 27017}))

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

(defn- fetch-transactions
  "Retrieves transactions paginated."
  [db-name page]

  (let [db (mg/get-db conn db-name)]

  (mq/with-collection db "transactions"
    (mq/paginate :page page :per-page batch-size))))

(defn- fetch-transactions-in-coll
  "Retrieves transactions in coll."
  [db-name coll]

  (let [db (mg/get-db conn db-name)]

    (->>
      (mq/find {:transaction {"$in" coll}})
      (mq/with-collection db "transactions")
      (transactions->frequencies))))

(defn- save-rules
  "Inserts rules into mongodb."
  [db-name rules]

  (if (not-empty rules)
    (let [db (mg/get-db conn db-name)]
      (log/info {:insert-rules (acknowledged? (mc/insert-batch db "rules" rules))}))))

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
  "Runs apriori for specified task batch."
  [task-id transactions-total page]

  (let [db-name (str "task_" task-id)
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

        ; prune all rules which don't meet min-support or min-confidence
        (filter #(and (not (nil? (get % :support))) (>= (get % :support) min-support)))
        (filter #(and (not (nil? (get % :confidence))) (>= (get % :confidence) min-confidence)))

        (print-rules)

        ;(save-rules db-name)

        )))

(defn process-message
  "Processes queued message."
  [msg]

  (try
    (let [body (json/decode (msg :body) true)]
      (log/info body)
      (apriori (body :task-id)
               (body :transactions-total)
               (body :page))

      (sqs/delete-message (assoc msg :queue-url queue)))
    (catch Exception e (prn e))))

(defn -main []
  (while true
    (log/info "Running" @lock "of" max-threads "threads.")
    (if (and (< (deref lock) max-threads) (> (- max-threads @lock) 0))
      (let [messages (sqs/receive-message :queue-url queue :max-number-of-messages (- max-threads @lock))]
        (doseq [msg (get messages :messages)]
          (future (swap! lock inc)
                  (process-message msg)
                  (swap! lock dec))))
      (log/info "Waiting for free thread slot to become available."))
    (Thread/sleep 10000)))
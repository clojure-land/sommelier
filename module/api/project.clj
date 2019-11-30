(defproject api "0.1.0"
  :dependencies [
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [org.clojure/tools.trace "0.7.10"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [metosin/compojure-api "1.1.11"]
                 [metosin/ring-swagger "0.24.3"]
                 [net.mikera/core.matrix "0.62.0"]
                 [compojure "1.6.0"]
                 [cheshire "5.8.0"]
                 [clj-time "0.14.2"]
                 [clj-http "3.7.0"]
                 [pandect "0.6.1"]
                 [ring "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [ring-logger "0.7.7"]
                 [environ "1.1.0"]
                 [commons-codec "1.10"]
                 [com.taoensso/carmine "2.18.1"]
                 [prismatic/schema "1.1.7"]
                 [siili/humanize "0.1.1"]
                 [crypto-random "1.2.0"]
                 [buddy/buddy-core "1.5.0"]
                 [buddy/buddy-sign "3.0.0"]
                 [com.novemberain/monger "3.5.0"]
                 [amazonica "0.3.145"]
                ]

  :ring {:handler handler/app}

  :source-paths ["src"]
  :resource-paths ["resources"]
  :test-paths ["test"]

  :profiles {:default [:user]
             :uberjar {:aot :all}
             :user   {:plugins [
                                 [lein-ring "0.12.1"]
                                 [lein-kibit "0.1.6"]
                                 [lein-cloverage "1.1.2"]
                               ]
                     }
            }
  )

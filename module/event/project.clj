(defproject event "0.1.0"
  :dependencies [
                    [org.clojure/clojure "1.9.0"]
                    [org.clojure/tools.logging "0.4.0"]
                    [org.clojure/tools.cli "0.3.5"]
                    [org.clojure/math.combinatorics "0.1.4"]
                    [net.mikera/core.matrix "0.62.0"]
                    [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                       javax.jms/jms
                                                       com.sun.jmdk/jmxtools
                                                       com.sun.jmx/jmxri]]
                    [cheshire "5.8.0"]
                    [environ "1.1.0"]
                    [com.novemberain/monger "3.5.0"]
                    [org.slf4j/slf4j-log4j12 "1.7.25"]
                    [amazonica "0.3.145"]
                    ]

  :main core

  ;:jvm-opts ["-Xmx3g"]

  :source-paths ["src"]

  :resource-paths ["resources"]

  :profiles {:default [:local]
             :uberjar {:aot :all}
             :local   {:plugins [[lein-kibit "0.1.6"]]}})
(defproject apriori "0.1.0-SNAPSHOT"

  :dependencies []

  :resource-paths ["resources"]

  :sub ["api"] ; "apriori"]

  :profiles {:default [:user]
             :user {:plugins [
                               [lein-sub "0.3.0"]
                               [lein-ring "0.12.1"]
                               [com.siili/lein-cucumber "1.0.7"]
                               [lein-cloverage "1.0.10"]
                               [lein-kibit "0.1.6"]
                               [lein-cloverage "1.0.10"]
                               [lein-try "0.4.3"]
                               [nightlight/lein-nightlight "1.0.0"]
                               [jonase/eastwood "0.2.3"]
                               [cider/cider-nrepl "0.16.0"]
                              ]
                       }
             }
  )
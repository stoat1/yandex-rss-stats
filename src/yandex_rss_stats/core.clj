(ns yandex-rss-stats.core
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [run-server]]
            [compojure.core        :refer [defroutes GET]]
            [compojure.route       :as route])
  (:require [yandex-rss-stats.controller :refer [handler]]))

(defonce server (atom nil))

(add-watch server :log-server-state (fn [key ref old new]
                                      (if-let [port  (some-> new meta :local-port)]
                                        (log/info "Server started on port" port)
                                        (log/info "Server stopped"))))

(defn start-server []
  (swap! server (fn [current]
                  (assert (nil? current) "Already started")
                  (run-server handler {:port 8080}))))

(defn stop-server []
  (swap! server (fn [stop-fn]
                  (assert stop-fn)
                  (stop-fn)
                  nil)))

(defn -main []
  (log/info "Application started")
  (start-server))

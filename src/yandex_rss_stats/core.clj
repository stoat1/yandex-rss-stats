(ns yandex-rss-stats.core
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [run-server]]
            [compojure.core        :refer [defroutes GET]]
            [compojure.route       :as route])
  (:require [yandex-rss-stats.controller :refer [handler]]))

(defonce server (atom nil))

(defn start-server []
  (swap! server (fn [current]
                  (assert (nil? current))
                  (run-server handler {:port 8080})))
  (log/info "Server started"))

(defn stop-server []
  (swap! server (fn [stop-fn]
                  (assert stop-fn)
                  (stop-fn)
                  nil))
  (log/info "Server stopped"))

(defn -main []
  (log/info "Application started")
  (start-server))

;; pieces of code for instarepl
(comment

  (stop-server)

  (start-server)
  )


(ns yandex-rss-stats.core
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [run-server]]
            [compojure.core        :refer [defroutes GET]]
            [compojure.route       :as route]
            [ring.middleware.params :refer [wrap-params]])
  (:require [yandex-rss-stats.controller :as controller]))

(defonce server (atom nil))

;; TODO move routes to controller namespace
(defroutes routes
  (GET "/search" [query :as req]
    (controller/search req))
  (route/not-found "Use /search end point"))

(def handler (wrap-params routes))

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


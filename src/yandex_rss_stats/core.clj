(ns yandex-rss-stats.core
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :refer [with-channel send! run-server]]))

(defonce server (atom nil))

(defn async-handler [ring-request]
  (with-channel ring-request channel
    (send! channel {:status  200
                    :headers {"Content-Type" "text/plain"}
                    :body    "http-kit is working"})))

(defn start-server []
  (swap! server (fn [current]
                  (assert (nil? current))
                  (run-server async-handler {:port 8080})))
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


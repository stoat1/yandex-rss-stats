(ns yandex-rss-stats.controller
  ;; TODO rename to handler
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [with-channel send!]]
            [compojure.core        :refer [routes GET]]
            [compojure.route       :as route]
            [cheshire.core         :refer [generate-string]]
            [ring.middleware.params :refer [wrap-params]]))

(defn- search [ring-request]
  (with-channel ring-request channel
    (send! channel {:status  200
                    :headers {"Content-Type" "application/json"}
                    :body    (generate-string {:status "ok"
                                               :message "http-kit is working"}
                                              {:pretty true})})))

(def ^:private unwrapped-routes
  (routes
    (GET "/search" [query :as req]
      (search req))
    (route/not-found "Use /search end point")))

(def handler (wrap-params unwrapped-routes))

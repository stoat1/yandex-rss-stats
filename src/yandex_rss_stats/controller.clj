(ns yandex-rss-stats.controller
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [with-channel send!]]
            [cheshire.core         :refer [generate-string]]))

(defn search [ring-request]
  (with-channel ring-request channel
    (send! channel {:status  200
                    :headers {"Content-Type" "application/json"}
                    :body    (generate-string {:status "ok"
                                               :message "http-kit is working"}
                                              {:pretty true})})))
(ns yandex-rss-stats.controller
  ;; TODO rename to handler
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [with-channel send!]]
            [compojure.core        :refer [routes GET]]
            [compojure.route       :as route]
            [cheshire.core         :refer [generate-string]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.core.async         :refer [chan go >! <!] :as async])
  (:require [yandex-rss-stats.yandex-api :refer [blog-search]]))

(defn- search [{{:strs [query]} :query-params, :as ring-request}]
  (with-channel ring-request channel
    (let [query (if (string? query) ;; treat single query as a singleton array
                  [query]
                  query)
          n (count query)
          results (chan n)
          ;; force redefs to happen now, in current thread (needed for unit tests)
          send! send!]

      ;; call blog search
      ;; FIXME if query is a string then we are iterating over its characters. Fix it!
      (doseq [query-elem query]
        (blog-search query-elem (fn [ok? links]
                                  ;; TODO check `ok?`
                                  (log/info "Completed search for" query-elem)
                                  (go (>! results links)))))

      ;; get search results and make the response
      (let [aggregated-result (->> results
                                   ;; close chan after n elements
                                   (async/take n)
                                   ;; squash n elements into one collection
                                   (async/into []))]
        (go (let [links (<! aggregated-result)]
              (send! channel {:status  200
                              :headers {"Content-Type" "application/json"}
                              :body    (generate-string {:status "ok"
                                                         :links links}
                                                        {:pretty true})})))))))

(def ^:private unwrapped-routes
  (routes
    (GET "/search" [query :as req]
      (search req))
    (route/not-found "Use /search end point")))

(def handler (wrap-params unwrapped-routes))

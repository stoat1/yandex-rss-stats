(ns yandex-rss-stats.controller
  ;; TODO rename to handler
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [with-channel send!]]
            [compojure.core        :refer [routes GET]]
            [compojure.route       :as route]
            [cheshire.core         :refer [generate-string]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.core.async         :refer [chan go >! <!] :as async])
  (:require [yandex-rss-stats.yandex-api :refer [blog-search]]
            [yandex-rss-stats.stats :refer [make-stats]]))

(defn- search
  "Controller function for /search endpoint"
  [{{:strs [query]} :query-params, :as ring-request}]
  (with-channel ring-request channel
    (let [query   (if (string? query) ;; treat single query as a singleton array
                    [query]           ;; TODO put it shorter: (flatten [query])
                    query)
          n       (count query)
          results (chan n)]

      ;; call blog search
      (doseq [query-elem query]
        (blog-search query-elem (fn [ok? links]
                                  ;; TODO check `ok?`
                                  (log/info "Completed search for" query-elem)
                                  (go (>! results [query-elem links])))))

      ;; get search results and make the response
      (let [aggregated-result (->> results
                                   ;; close channel after all elements received
                                   (async/take n)
                                   ;; squash n elements into one collection
                                   (async/into {}))
            ;; put var into closure in order to force current thread bindings to take effect inside async code
            ;; (needed for unit tests)
            send! send!]
        (go (let [body (-> (<! aggregated-result)              ;; get collection of results
                            make-stats                         ;; calculate stats
                            (generate-string {:pretty true}))] ;; serialize
              (send! channel {:status  200 ;; TODO remove default status
                              ;; TODO use middleware to add content type
                              :headers {"Content-Type" "application/json"}
                              :body    body})))))))

(def ^:private unwrapped-routes
  (routes
    (GET "/search" [:as req]
      (search req))
    (route/not-found "Use /search end point")))

(def handler (wrap-params unwrapped-routes))

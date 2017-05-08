(ns yandex-rss-stats.controller
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [with-channel send!]]
            [compojure.core        :refer [routes GET]]
            [compojure.route       :as route]
            [cheshire.core         :refer [generate-string]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json  :refer [json-response]]
            [clojure.core.async         :refer [chan go >!! <! alt!] :as async])
  (:require [yandex-rss-stats.yandex-api :refer [blog-search]]
            [yandex-rss-stats.stats :refer [make-stats]]))

(defn- search
  "Controller function for /search endpoint"
  [{{:strs [query]} :query-params, :as ring-request}]
  (with-channel ring-request channel
    (let [query      (if (string? query) ;; treat single query as a singleton array
                       [query]           ;; TODO put it shorter: (flatten [query])
                       query)
          n          (count query)

          ;; channel to conduct blog search results
          results (chan n)

          ;; separate sucessful results and errors
          [ok-results failures] (async/split :ok? results)

          ;; accumulate successful results into collection
          aggregated-result (->> ok-results
                                 ;; close channel after all elements received
                                 (async/take n)
                                 ;; convert to pairs
                                 (vector)
                                 (async/map (juxt :query :links))
                                 ;; squash n elements into one collection
                                 (async/into {}))

          ;; put var into closure in order to force current thread bindings to take effect inside async code
          ;; (needed for unit tests)
          send! send!]

      ;; call blog search
      (doseq [query-elem query]
        (blog-search query-elem #(>!! results {:query query-elem,
                                               :ok?   %1,
                                               :links %2
                                               :error %3})))

      ;; get search results and make the response
      (go (alt!
            ;; if all results were ok than put them into response
            aggregated-result
            ([result] (let [stats (make-stats result)] ;; calculate stats
                        (as-> {:status 200             ;; make response
                               :body   stats} res

                          ;; serialize and add headers
                          (json-response res {:pretty true})

                          ;; write response
                          (send! channel res))))

            ;; if there was a failure then respond with server error
            failures
            ([{:keys [query]}] (send! channel {:status 500
                                               :body   (str "Query " query " failed")})))))))

(def ^:private unwrapped-routes
  (routes
    (GET "/search" [:as req]
      (search req))
    (route/not-found "Use /search end point")))

(def handler (wrap-params unwrapped-routes))

(ns yandex-rss-stats.controller
  ;; TODO rename to handler
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server    :refer [with-channel send!]]
            [compojure.core        :refer [routes GET]]
            [compojure.route       :as route]
            [cheshire.core         :refer [generate-string]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.core.async         :refer [chan go >!! <! alt!] :as async])
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
          results (chan n)
          failure (chan 1)]

      ;; call blog search
      (doseq [query-elem query]
        (blog-search query-elem (fn [ok? links] ;; TODO can we use multimethods here?
                                  (if ok?
                                    (do
                                      (log/info "Completed search for" query-elem)
                                      (>!! results [query-elem links]))
                                    (do
                                      (log/error "Failed search for" query-elem)
                                      (>!! failure query-elem))))))

      ;; get search results and make the response
      (let [aggregated-result (->> results
                                   ;; close channel after all elements received
                                   (async/take n)
                                   ;; squash n elements into one collection
                                   (async/into {}))
            ;; put var into closure in order to force current thread bindings to take effect inside async code
            ;; (needed for unit tests)
            send! send!]
        (go (alt! aggregated-result ([result] (let [body (-> result              ;; get collection of results
                                                             make-stats                         ;; calculate stats
                                                             (generate-string {:pretty true}))] ;; serialize
                                                (send! channel {:status  200 ;; TODO remove default status
                                                                ;; TODO use middleware to add content type
                                                                :headers {"Content-Type" "application/json"}
                                                                :body    body})))
                  failure           ([elem] (send! channel {:status 500
                                                            :body   (str "Query " elem " failed")}))))))))

(def ^:private unwrapped-routes
  (routes
    (GET "/search" [:as req]
      (search req))
    (route/not-found "Use /search end point")))

(def handler (wrap-params unwrapped-routes))

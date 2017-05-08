(ns yandex-rss-stats.yandex-api
  (:require [clojure.tools.logging :as log]
            [org.httpkit.client :as http]
            [clj-xpath.core   :refer :all])
  (:import  [org.httpkit.client HttpClient]))

(def YANDEX_API_URL "https://yandex.ru/blogs/rss/search")

(def MAX_CONNECTIONS 10)

(def client (HttpClient. MAX_CONNECTIONS))

(defn blog-search [query callback]
  (letfn [(on-response [{:keys [status body error] :as res}]
            ;; FIXME try-catch inside letfn looks ugly, other options?
            (try
              (cond (some? error)  (callback false nil error)
                    (= 200 status) (let [result ($x:text* "/rss/channel/item/link" body)]
                                     (callback true result nil))
                    :else          (callback false nil (str "Unexpected status code " status)))
              (catch Throwable e
                (log/error "Unexpected exception" e)
                (callback false nil e))))]
    ;; TODO limit number or parallel connections
    (http/get YANDEX_API_URL {:query-params {"text" query}
                              :client client} on-response)))



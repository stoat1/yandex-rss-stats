(ns yandex-rss-stats.yandex-api
  (:require [clojure.tools.logging :as log]
            [org.httpkit.client :as http]
            [clj-xpath.core   :refer :all]))

(def YANDEX_API_URL "https://yandex.ru/blogs/rss/search")

(defn blog-search [query callback]
  (letfn [(on-response [{:keys [status body error] :as res}]
            ;; FIXME try-catch inside letfn looks ugly, other options?
            ;; TODO check `error`
            (try
              (if (= 200 status)
                (let [result ($x:text* "/rss/channel/item/link" body)]
                  (callback true result))
                (callback false "Unexpected status code"))
              (catch Throwable e
                (callback false e))))]
    ;; TODO limit number or parallel connections
    (http/get YANDEX_API_URL {:query-params {"text" query}} on-response)))



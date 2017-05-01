(ns yandex-rss-stats.yandex-api
  (:require [clojure.tools.logging :as log]
            [org.httpkit.client :as http]
            [clj-xpath.core   :refer :all]))

(def YANDEX_API_URL "https://yandex.ru/blogs/rss/search")

(defn blog-search [query callback]
  ;; FIXME implement error handling
  (letfn [(on-response [{:keys [status body error] :as res}]
            (let [result ($x:text* "/rss/channel/item/link" body)]
              (callback result)))]
    (http/get YANDEX_API_URL {:query-params {"text" query}} on-response)))



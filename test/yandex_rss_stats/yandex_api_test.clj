(ns yandex-rss-stats.yandex-api-test
  (:require [clojure.test :refer :all]
            [yandex-rss-stats.yandex-api :refer :all])
  (:require [clojure.java.io  :refer [resource]]
            [org.httpkit.fake :refer [with-fake-http]]))

(def SAMPLE_RESPONSE_RESOURCE_NAME "yandex_rss_stats/yandex_api_test/yandex_response.rss")

(defn- load-sample []
  (-> SAMPLE_RESPONSE_RESOURCE_NAME
      resource
      slurp))

(deftest yandex-api-test
  (with-fake-http ["https://yandex.ru/blogs/rss/search" (load-sample)]
    (testing "api"
      (is (= ["http://vk.com/wall-75338648_3206"
              "http://vk.com/wall-10175642_2570314"
              "http://liaclub.info/2017/05/01/epson-artisan-730-troubleshooting_gk/"]
             (deref (let [p (promise)]
                (blog-search "foo" #(deliver p %))
                p) 100 :timeout))))))

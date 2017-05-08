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

(defn blocking-blog-search [query]
  (let [p (promise)]
    (blog-search query #(deliver p [%1 %2 %3]))
    (deref p 1000 [false nil :timeout])))

(deftest yandex-api-test
  (with-fake-http ["https://yandex.ru/blogs/rss/search" (load-sample)]
    (testing "api"
      (let [[ok links error] (blocking-blog-search "foo")]
        (is (= true ok))
        (is (= ["http://vk.com/wall-75338648_3206"
                "http://vk.com/wall-10175642_2570314"
                "http://liaclub.info/2017/05/01/epson-artisan-730-troubleshooting_gk/"]
               links))
        (is (= nil error)))))

  (with-fake-http ["https://yandex.ru/blogs/rss/search" 500]
    (testing "api unavailable"
      (let [[ok links error] (blocking-blog-search "foo")]
        (is (= false ok))
        (is (= nil links))
        (is (not (= nil error))))))

  (with-fake-http ["https://yandex.ru/blogs/rss/search" "<?malformed </xml"]
    (testing "malformed XML"
      (let [[ok links error] (blocking-blog-search "foo")]
        (is (= false ok))
        (is (= nil links))
        (is (not (= nil error)))))))

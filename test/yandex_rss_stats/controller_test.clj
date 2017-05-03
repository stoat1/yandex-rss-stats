(ns yandex-rss-stats.controller-test
  (:require [clojure.test :refer :all]
            [yandex-rss-stats.controller :refer :all]
            [org.httpkit.client :as http]
            [ring.mock.request :as mock]))

(deftest handler-test

  (testing "404"
    (let [result (handler (mock/request :get "/foo"))]
      (is (= (:status result) 404))
      (is (= (:body result) "Use /search end point")))))
(ns yandex-rss-stats.core-test
  (:require [clojure.test :refer :all]
            [yandex-rss-stats.core :refer :all]
            [org.httpkit.client :as http]
            [ring.mock.request :as mock]))

;; TODO don't reinvent the wheel. Use mock library
(defonce controller-args (atom nil))

(defn with-server-started [f]
  (with-redefs [yandex-rss-stats.controller/search (fn [& args] (reset! controller-args args)
                                                                {:status 200
                                                                 :body "Mock response body"})]
    (start-server)
    (try
      (f)
      (finally
        (reset! controller-args nil)
        (stop-server)))))

;; TODO get rid of this complicated end-to-end sytle test
(deftest a-test
  (testing "/search"
    (is (= "Mock response body"
           (:body @(http/get "http://localhost:8080/search" {:as :text})))))
  (testing "404"
    (is (= 404
           (:status @(http/get "http://localhost:8080/foobar"))))))

(deftest mock-test
  (testing "404"
    (let [result (handler (mock/request :get "/foo"))]
      (is (= (:status result) 404))
      (is (= (:body result) "Use /search end point"))))
  (testing "/search one keyword"
    (with-redefs [yandex-rss-stats.controller/search (fn [req] (:query-params req))]
      (let [result (handler (mock/request :get "/search?query=foo&query=bar"))]
        (is (= (result "query") ["foo" "bar"]))))))


(use-fixtures :each with-server-started)

(comment
  (run-tests)
  )

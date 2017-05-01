(ns yandex-rss-stats.core-test
  (:require [clojure.test :refer :all]
            [yandex-rss-stats.core :refer :all]
            [org.httpkit.client :as http]))

(defn with-server-started [f]
  (with-redefs [yandex-rss-stats.controller/search (fn [req] {:status 200
                                                              :body "Mock response body"})]
    (start-server)
    (try
      (f)
      (finally
        (stop-server)))))

(deftest a-test
  (testing "/search"
    (is (= "Mock response body"
           (:body @(http/get "http://localhost:8080/search" {:as :text})))))
  (testing "404"
    (is (= 404
           (:status @(http/get "http://localhost:8080/foobar"))))))

(use-fixtures :each with-server-started)

(comment
  (run-tests)
  )

(ns yandex-rss-stats.core-test
  (:require [clojure.test :refer :all]
            [yandex-rss-stats.core :refer :all]
            [org.httpkit.client :as http]))

(defn with-server-started [f]
  (with-redefs [yandex-rss-stats.controller/handler (fn [req] {:status 200
                                                               :body (:body req)})]
    (start-server)
    (try
      (f)
      (finally
        (stop-server)))))

(deftest a-test
  (testing "/search"
    (is (= "ping"
           (:body @(http/post "http://localhost:8080/foo/bar" {:body "ping"
                                                              :as :text}))))))


(use-fixtures :each with-server-started)

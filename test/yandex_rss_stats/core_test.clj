(ns yandex-rss-stats.core-test
  (:require [clojure.test :refer :all]
            [yandex-rss-stats.core :refer :all]
            [org.httpkit.server    :refer [with-channel send!]]
            [org.httpkit.client :as http]))

(defn with-server-started [f]
  (letfn [(echo-async-handler [{:keys [async-channel body]}]
            (send! async-channel {:status 200
                                  :body   body})
            {:body async-channel})]
    (with-redefs [yandex-rss-stats.controller/handler echo-async-handler]
      (start-server)
      (try
        (f)
        (finally
          (stop-server))))))

;; test that server supports async handlers
(deftest a-test
  (testing "/search"
    (is (= "ping"
           (:body @(http/post "http://localhost:8080/foo/bar" {:body "ping"
                                                               :as   :text}))))))

(use-fixtures :each with-server-started)

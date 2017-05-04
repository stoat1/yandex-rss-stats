(ns yandex-rss-stats.controller-test
  (:require [clojure.test :refer :all]
            [yandex-rss-stats.controller :refer :all]
            [org.httpkit.client :as http]
            [ring.mock.request :as mock]
            [cheshire.core :refer [parse-string]]))

(deftest handler-test

  (testing "404"
    (let [result (handler (mock/request :get "/foo"))]
      (is (= (:status result) 404))
      (is (= (:body result) "Use /search end point"))))

  (testing "vanilla mock"
    (let [mock-channel 'mock-channel
          send!-calls  (atom [])
          req          (assoc (mock/request :get "/search?query=foo&query=bar")
                         :async-channel mock-channel)]
      (with-redefs [org.httpkit.server/send! (fn [& args]
                                               (swap! send!-calls conj args))]
        (handler req))
      (let [[[arg1 arg2]] @send!-calls]
        (is (= arg1 mock-channel))
        (is (= 200 (:status arg2)))
        (is (= (-> arg2
                   :body
                   parse-string)
               {"message" "http-kit is working"
                "status" "ok"}))))))

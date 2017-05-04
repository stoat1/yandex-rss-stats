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

      ;; replace send! function with mocked implementation
      (with-redefs [org.httpkit.server/send! (fn [& args]
                                               (swap! send!-calls conj args))]

        ;; invoke the handler
        (let [ret (handler req)]

          ;; check that channel response was returned
          (is (= {:status  200
                  :headers {}
                  :body mock-channel}
                 ret))))

      ;; check send! invocations
      (let [[[arg1 arg2]] @send!-calls]

        ;; check how many times it was invoked
        (is (= (count @send!-calls) 1))

        ;; check how many arguments were passed
        (is (= (count (first @send!-calls)) 2))

        ;; check the first argument
        (is (= arg1 mock-channel))

        ;; check the second argument
        (is (= (-> arg2
                   :body
                   parse-string)
               {"message" "http-kit is working"
                "status"  "ok"}))
        (is (= {:status 200
                :headers {"Content-Type" "application/json"}}
               (dissoc arg2 :body)))))))

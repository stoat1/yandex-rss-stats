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
    (let [mock-channel      'mock-channel
          send!-calls       (atom [])
          blog-search-calls (atom [])
          make-stats-calls  (atom [])
          req               (assoc (mock/request :get "/search?query=foo&query=bar")
                              :async-channel mock-channel)]

      ;; replace send! and blog-search functions with mocked implementation
      (with-redefs [org.httpkit.server/send!                (fn [& args]
                                                              (swap! send!-calls conj args))
                    yandex-rss-stats.yandex-api/blog-search (fn [& args]
                                                              (swap! blog-search-calls conj args))
                    yandex-rss-stats.stats/make-stats       (fn [& args]
                                                              (swap! make-stats-calls conj args)
                                                              {:stats "stub stats"})]

        ;; invoke the handler
        (let [ret (handler req)]

          ;; check that channel response was returned
          (is (= {:status  200
                  :headers {}
                  :body mock-channel}
                 ret)))

        ;; check blog-search invocations
        (let [[[arg-1-1 arg-1-2] [arg-2-1 arg-2-2]] @blog-search-calls]

          ;; check how many times it was invoked
          (is (= (count @blog-search-calls) 2))

          ;; check how many arguments were passed
          (for [args @blog-search-calls]
            (is (= (count args) 2)))

          ;; first argument should come from query params
          (is (= arg-1-1 "foo"))
          (is (= arg-2-1 "bar"))

          ;; check that send! isn't yet invoked (handler should wait for the client to respond)
          (is (= [] @send!-calls))

          ;; at this point in time, we emulate that the second client is ready
          (arg-2-2 true ["link B1", "link B2"])

          ;; send! should not yet be invoked
          (is (= [] @send!-calls))

          ;; now the first client is ready
          (arg-1-2 true ["link A1", "link A2"]))

        ;; TODO wait for condition using clj-async-test
        (Thread/sleep 1000)

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
                 {"stats" "stub stats"}))
          (is (= {:status 200
                  :headers {"Content-Type" "application/json"}}
                 (dissoc arg2 :body))))

        ;; check make-stats invocation
        (let [[[arg]] @make-stats-calls]
          (is (= {"foo" ["link A1" "link A2"]
                  "bar" ["link B1" "link B2"]}
                 arg))))))

  ;; FIXME get rid of boilerplate code
  (testing "single query"
    (let [mock-channel      'mock-channel
          send!-calls       (atom [])
          blog-search-calls (atom [])
          req               (assoc (mock/request :get "/search?query=foo")
                              :async-channel mock-channel)]

      ;; replace send! and blog-search functions with mocked implementation
      (with-redefs [org.httpkit.server/send!                (fn [& args]
                                                              (swap! send!-calls conj args))
                    yandex-rss-stats.yandex-api/blog-search (fn [& args]
                                                              (swap! blog-search-calls conj args))
                    yandex-rss-stats.stats/make-stats (fn [& args] "stub")]

        ;; invoke the handler
        (handler req)

        ;; check that blog-saarch wasn't invoked for each of \f \o \o letters
        (let [[[arg1 arg2] :as args] @blog-search-calls]
          ;; check how many times it was invoked
          (is (= (count args) 1))
          (is (= arg1 "foo"))))))

  (testing "client failure"
    (let [mock-channel 'mock-channel
          send!-calls  (atom [])
          blog-search-calls (atom {})
          req          (assoc (mock/request :get "/search?query=foo&query=bar")
                         :async-channel mock-channel)]
      (with-redefs [org.httpkit.server/send!                (fn [& args]
                                                              (swap! send!-calls conj args))
                    yandex-rss-stats.yandex-api/blog-search (fn [query callback]
                                                              (swap! blog-search-calls assoc query callback))
                    yandex-rss-stats.stats/make-stats       (fn [& args] (is false "make-stats should not be invoked"))]

        ;; invoke the handler
        (handler req)

        ;; wait until client is invoked
        ;; TODO use clj-async-test
        (Thread/sleep 100)
        (is (= (count @blog-search-calls) 2))

        ;; mock responses from client
        (let [{:strs [foo bar]}  @blog-search-calls]
          ;; the foo call will succeed
          (foo true ["linke1", "linke2"])
          ;; the bar call will fail
          (bar false "I'm failed"))

        ;; give it time to think and invoke send! function
        (Thread/sleep 100)
        (is (= (count @send!-calls) 1))

        (let [[[arg1 arg2]] @send!-calls]
          (is (= arg1 mock-channel))
          (is (= arg2 {:status 500
                       :body   "Query bar failed"})))))))

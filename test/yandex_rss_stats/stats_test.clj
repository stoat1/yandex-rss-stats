(ns yandex-rss-stats.stats-test
  (:require [clojure.test :refer :all]
            [yandex-rss-stats.stats :refer :all]))

(deftest make-stats-test
  (are [input output] (= (make-stats input) output)
       ;; empty input give empty
       {} {}

       ;; query with no results give zero
       {"foo" []} {}

       ;; 2nd domain name is extracted
       {"foo" ["http://www.example.com/path/and?some=query&params"]} {"example.com" 1}
       {"foo" ["https://www.example.com"]} {"example.com" 1}
       {"foo" ["http://x.y.z.www.example.com"]} {"example.com" 1}

       ;; check counts
       {"foo" ["http://www.example.com/one" "http://example.com/two"]} {"example.com" 2}

       ;; check duplicates
       {"foo" ["http://example.com/same" "http://example.com/same"]} {"example.com" 1}

       ;; check multiple queries
       {"foo" ["http://example.com/one"
               "http://example.com/two"]
        "bar" ["http://example.com/three"]}
       {"example.com" 3}))
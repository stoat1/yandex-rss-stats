(ns yandex-rss-stats.stats
  (:import [java.net URL]))

(defn- second-level-domain [link]
  (->> link
       (URL.)
       (.getHost)
       (re-find #"\w+.\w+$")))

(defn make-stats [links]
  "Calculate statistics from links"
  (->> links
      ;; ignore keys, merge all values together
      vals
      flatten

      ;; deduplicate
      (into #{})

      ;; extract 2nd level domains
      (map second-level-domain)

      ;; count everything
      frequencies))

(defproject yandex-rss-stats "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main yandex-rss-stats.core
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [http-kit "2.2.0"]
                 [cheshire "5.7.1"]
                 [compojure "1.5.2"]
                 [com.github.kyleburton/clj-xpath "1.4.3"]
                 [ring/ring-mock "0.3.0"]]
  :profiles {:dev {:resource-paths ["test_resources"]}
             :test {:dependencies [[http-kit.fake "0.2.1"]]}})

(require '[clojure.edn :as edn])

(def +deps+ (-> "deps.edn" slurp edn/read-string))

(defn deps->vec [deps]
  (mapv (fn [[dep {:keys [:mvn/version exclusions]}]]
            (cond-> [dep version]
              exclusions (conj :exclusions exclusions)))
        deps))

(defn- filter-datomic [deps]
  (->> deps
       (filter #(-> % first str (clojure.string/includes? "datomic") not))))

(def dependencies
  (->> (:deps +deps+)
       deps->vec))

(def dev-dependencies
  (deps->vec (get-in +deps+ [:aliases :dev :extra-deps])))

(def source-paths
  (vec (:paths +deps+)))

(def repos
  (->> +deps+
       :mvn/repos
       (mapv #(vector (first %) (-> % second :url)))))

(defproject calendar-defender "0.1.0-SNAPSHOT"
  :description "Calendar Defender"
  :url "http://www.calendardefender.com"
  :plugins [[s3-wagon-private "1.1.2"]
            [lein-cljsbuild "1.1.7"]]
  :dependencies ~(concat
                   (filter-datomic dependencies)
                   '[[reagent "0.8.1"]
                     [cljs-http "0.1.46"]
                     [clj-commons/secretary "1.2.4"]])
  :source-paths ~source-paths
  :profiles {:dev
              {:dependencies ~(concat (filter-datomic dev-dependencies)
                                      '[[org.clojure/clojurescript "1.10.516"]
                                        [com.bhauman/figwheel-main "0.2.0"]
                                        [com.bhauman/rebel-readline-cljs "0.1.4"]
                                        [org.eclipse.jetty/jetty-server "9.2.24.v20180105"]
                                        [org.eclipse.jetty.websocket/websocket-servlet "9.2.24.v20180105"]
                                        [org.eclipse.jetty.websocket/websocket-server "9.2.24.v20180105"]
                                        [com.google.guava/guava "28.0-jre"]])}}
  :repositories ~(filter-datomic repos)
  :resource-paths ["target"]
  :figwheel {:hawk-options {:watcher :polling}}
  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:main calendar-defender.core
                                   :npm-deps {"@mrblenny/react-flow-chart" "0.0.6"
                                              react "16.8.6"
                                              react-dom "16.8.6"}
                                   :install-deps true}}]}
  :repl-options {:init-ns calendar-defender.core})

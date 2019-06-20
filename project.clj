(require '[clojure.edn :as edn])

(def +deps+ (-> "deps.edn" slurp edn/read-string))

(defn deps->vec [deps]
  (mapv (fn [[dep {:keys [:mvn/version exclusions]}]]
            (cond-> [dep version]
              exclusions (conj :exclusions exclusions)))
        deps))

(def dependencies
  (deps->vec (:deps +deps+)))

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
                   dependencies
                   '[[reagent "0.8.1"]])
  :source-paths ~source-paths
  :profiles {:dev
              {:dependencies ~(concat dev-dependencies
                                      '[[org.clojure/clojurescript "1.10.516"]
                                        [com.bhauman/figwheel-main "0.2.0"]
                                        [com.bhauman/rebel-readline-cljs "0.1.4"]
                                        [org.eclipse.jetty/jetty-server "9.2.24.v20180105"]
                                        [org.eclipse.jetty.websocket/websocket-servlet "9.2.24.v20180105"]
                                        [org.eclipse.jetty.websocket/websocket-server "9.2.24.v20180105"]])}}
  :repositories ~repos
  :resource-paths ["target"]
  :repl-options {:init-ns calendar-defender.core})

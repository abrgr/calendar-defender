(require '[clojure.edn :as edn])

(def +deps+ (-> "deps.edn" slurp edn/read-string))

(defn deps->vec [deps]
  (vec (map (fn [[dep {:keys [:mvn/version exclusions]}]]
                (cond-> [dep version]
                                exclusions (conj :exclusions exclusions)))
              deps)))

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
  :plugins [[s3-wagon-private "1.1.2"]]
  :dependencies ~dependencies
  :source-paths ~source-paths
  :profiles {:dev {:dependencies ~dev-dependencies}}
  :repositories ~repos
  :repl-options {:init-ns calendar-defender.core})

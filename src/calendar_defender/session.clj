(ns calendar-defender.session
  (:require [clojure.data.json :as json]
            [datomic.client.api :as d]
            [calendar-defender.db :as db]
            [datomic.ion.lambda.api-gateway :as apigw]))

(defn- create-from-google* [{:keys [headers body]}]
  (let [{:keys [email]} (-> body json/read-str)
        tx [{:user/email email}]
        result (d/transact (db/get-conn) {:tx-data tx})]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:session "sess" :t (-> result :db-after :t)})}))

(def create-from-google (apigw/ionize create-from-google*))

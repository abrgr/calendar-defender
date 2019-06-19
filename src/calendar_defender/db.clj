(ns calendar-defender.db
  (:require [datomic.client.api :as d]
            [datomic.ion :as ion]))

(def ^:private env 
  (get (ion/get-env) :env))

(def ^:private system-name
  (get (ion/get-app-info) :app-name))

(def ^:private region "us-east-1")

(def ^:private cfg
  {:server-type :ion
   :region region
   :system system-name
   :endpoint (str "http://entry." system-name "." region ".datomic.net:8182/")
   :proxy-port (when (= env :local) 8182)})

(def ^:private client (d/client cfg))

(def conn (d/connect client {:db-name "calendar-defender"}))

(def schema
  [{:db/ident :user/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "User's email address"}
   {:db/ident :user.google/refresh-token
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "User's google refresh token"}])

(defn ensure-schema []
  (d/transact conn {:tx-data schema}))

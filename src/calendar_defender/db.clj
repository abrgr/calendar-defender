(ns calendar-defender.db
  (:require [datomic.client.api :as d]
            [datomic.ion :as ion]
            [datomic.ion.cast :as cast]))

(def ^:private env 
  (memoize #(get (ion/get-env) :env)))

(def ^:private system-name
  (memoize #(get (ion/get-app-info) :app-name)))

(def ^:private region "us-east-1")

(def ^:private cfg
  (memoize
    (fn []
      (let [c {:server-type :ion
       :region region
       :system (system-name)
       :endpoint (str "http://entry." (system-name) "." region ".datomic.net:8182/")
       :proxy-port (when (= (env) :local) 8182)}]
       (cast/event {:msg "Config" :cfg c})
       c))))

(def ^:private client (memoize #(d/client (cfg))))

(def get-conn (memoize #(d/connect (client) {:db-name "calendar-defender"})))

(def schema
  [{:db/ident :user/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "User's email address"}
   {:db/ident :user/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "User's name"}
   {:db/ident :user/locale
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "User's locale"}
   {:db/ident :user/picture-url
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "User's picture url"}
   {:db/ident :user.google/refresh-token
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "User's google refresh token"}
   {:db/ident :user.google/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "User's google id"}])

(defn ensure-schema []
  (d/transact (get-conn) {:tx-data schema}))

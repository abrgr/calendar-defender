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

(def get-db (memoize #(d/db (get-conn))))

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
    :db/doc "User's google id"}
   {:db/ident :n/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Node's id"}
   {:db/ident :n/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Node's type"}
   {:db/ident :n/next
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "Node's nexts"}
   {:db/ident :n/op
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Operation"}
   {:db/ident :n.op/args
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "Operation arguments"}
   {:db/ident :n.op.args/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Operation argument type"}
   {:db/ident :n.op.args.str/str
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "String argument"}
   {:db/ident :n.op.args.ref/ref
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Ref argument"}
   {:db/ident :n.mult-choice/q
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Multiple choice node's question"}
   {:db/ident :n.mult-choice/ans
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Multiple choice node's answers"}
   {:db/ident :n.decline/reason
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Decline node's decline reason"}
   {:db/ident :n.type/mult-choice-single}
   {:db/ident :n.type/decline}
   {:db/ident :n.op/=}
   {:db/ident :n.op/not}
   {:db/ident :n.op/or}
   {:db/ident :n.op/and}
   {:db/ident :n.op.args.type/ref}
   {:db/ident :n.op.args.type/str}])

(defn ensure-schema []
  (d/transact (get-conn) {:tx-data schema}))

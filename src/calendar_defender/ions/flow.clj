(ns calendar-defender.ions.flow
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [datomic.client.api :as d]
            [datomic.ion :as ion]
            [calendar-defender.db :as db]
            [calendar-defender.ions.utils :as iu]))

(defn- upsert-flow [{:user/keys [email]} flow]
  (d/transact (db/get-conn) {:tx-data [{:user/email email
                                        :user/flow flow}]}))

(defn- upsert* [{:keys [headers body] :as args}]
  (let [user (iu/get-user args)
        flow (iu/body->edn body)
        is-valid-flow (s/valid? ::flow-node flow)]
    (if is-valid-flow
      (throw (ex-info "Bad request" {:anomaly :bad-req}))
      {:status 200
       :headers iu/std-headers
       :body (upsert-flow user flow)})))

(def upsert (iu/apigw-ionize upsert*))

; TODO: find a way to generate this off of schema or schema off of this
(s/def :n/type #{:n.type/mult-choice-single :n.type/cancel})
(s/def :n/id string?)
(s/def :n/next
  (s/coll-of ::next-node :kind vector?))
(s/def :n/op #{:n.op/= :n.op/and :n.op/or :n.op/not})
(s/def :n.op/args
  (s/coll-of ::typed-data))
(s/def :n.op.args/type #{:n.op.args.type/ref :n.op.args.type/str})
(s/def :n.op.args.ref/ref string?)
(s/def :n.op.args.str/str string?)
(s/def ::typed-data
  ; TODO: add assertion that we have the right value set for the type
  (s/keys :req [:n.op.args/type] :opt [:n.op.args.ref/ref :n.op.args.str/str]))
(s/def :n/when
  (s/keys :req [:n/op :n.op/args]))
(s/def ::next-node
  (s/and ::flow-node
         (s/keys :req [:n/when])))
(s/def ::flow-node
  (s/keys :req [:n/type :n/id]
          :opt [:n/next]))

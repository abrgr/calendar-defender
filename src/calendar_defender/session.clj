(ns calendar-defender.session
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [datomic.client.api :as d]
            [datomic.ion :as ion]
            [datomic.ion.lambda.api-gateway :as apigw]
            [calendar-defender.db :as db])
  (:import (com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeTokenRequest)
           (com.google.api.client.http.javanet NetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)))

(def cors-headers
  {"Access-Control-Allow-Headers" "*"
   "Access-Control-Allow-Methods" "*"
   "Access-Control-Allow-Origin" "*"})

(defn fail
  [k]
    (throw (RuntimeException. (str "Unable to get a value for " k))))

(def get-params
  (memoize
    #(let [app (or (get (ion/get-app-info) :app-name) (fail :app-name))
           env (or (get (ion/get-env) :env) (fail :env))]
       (ion/get-params {:path (str "/datomic-shared/" (name env) "/" app "/")}))))

(defn get-param
  [k]
    (or (get (get-params) k) (fail k)))

(defn- make-token-req [code]
  (GoogleAuthorizationCodeTokenRequest.
    (NetHttpTransport.)
    (JacksonFactory/getDefaultInstance)
    (get-param "google-client-id")
    (get-param "google-client-secret")
    code
    "postmessage"))

(defn- make-user-txn-from-code [code]
  (let [token-req (make-token-req code)
        token-res (.execute token-req)
        refresh-token (.getRefreshToken token-res)
        id-token (.parseIdToken token-res)
        payload (.getPayload id-token)]
    [{:user/email (.getEmail payload)
      :user.google/id (.getSubject payload)
      :user.google/refresh-token refresh-token
      :user/name (.get payload "name")
      :user/locale (.get payload "locale")
      :user/picture-url (.get payload "picture")}]))

(defn- create-from-google* [{:keys [headers body]}]
  (let [{:keys [code]} (-> body io/reader (json/read :key-fn keyword))
        tx (make-user-txn-from-code code)
        result (d/transact (db/get-conn) {:tx-data tx})]
    {:status 200
     :headers (merge cors-headers {"Content-Type" "application/json"})
     :body (json/write-str {:session "sess" :t (-> result :db-after :t)})}))

(def create-from-google (apigw/ionize create-from-google*))

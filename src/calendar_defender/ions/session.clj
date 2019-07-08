(ns calendar-defender.ions.session
  (:require [clojure.data.json :as json]
            [datomic.client.api :as d]
            [datomic.ion :as ion]
            [calendar-defender.db :as db]
            [calendar-defender.ions.utils :as iu])
  (:import (com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeTokenRequest)
           (com.google.api.client.http.javanet NetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)))

(defn- make-token-req [code]
  (GoogleAuthorizationCodeTokenRequest.
    (NetHttpTransport.)
    (JacksonFactory/getDefaultInstance)
    (iu/get-param "google-client-id")
    (iu/get-param "google-client-secret")
    code
    "postmessage"))

(defn- user-info-from-code [code]
  (let [token-req (make-token-req code)
        token-res (.execute token-req)
        refresh-token (.getRefreshToken token-res)
        id-token (.getIdToken token-res)
        user-info (-> token-res .parseIdToken iu/token->user)]
    {:user user-info
     :secret {:user.google/refresh-token refresh-token}
     :session {:goog id-token}}))

(defn- create-from-code [code]
  (let [user-info (user-info-from-code code)
        tx [(merge (:user user-info) (:secret user-info))]
        result (d/transact (db/get-conn) {:tx-data tx})]
    {:user (:user user-info)
     :session (:session user-info)
     :t (-> result :db-after :t)}))

(defn- is-existing-google-user? [{goog-id :user.google/id email :user/email}]
  (-> (d/q '[:find ?refresh-token
             :in $ ?goog-id ?email
             :where [?u :user.google/id ?goog-id]
                    [?u :user/email ?email]
                    [?u :user.google/refresh-token ?refresh-token]]
           (db/get-db)
           goog-id
           email)
      ffirst
      some?))

(defn- create-from-token [id-token]
  (let [user (iu/verify-goog-token id-token)]
    (when-not (is-existing-google-user? user)
      (throw (ex-info "Require sign up" {:anomaly :require-sign-up})))
    {:user user
     :session {:goog id-token}}))

(defn- create-from-google* [{:keys [headers body]}]
  (let [{:keys [code id-token]} (iu/body->edn body)
        result (if (some? code)
                 (create-from-code code)
                 (create-from-token id-token))]
    {:status 200
     :headers iu/std-headers
     :body (pr-str result)}))

(def create-from-google (iu/apigw-ionize create-from-google*))

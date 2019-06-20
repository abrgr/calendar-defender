(ns calendar-defender.session
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [datomic.client.api :as d]
            [datomic.ion :as ion]
            [datomic.ion.cast :as cast]
            [datomic.ion.lambda.api-gateway :as apigw]
            [calendar-defender.db :as db])
  (:import (com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeTokenRequest GoogleIdTokenVerifier$Builder)
           (com.google.api.client.http.javanet NetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)))

(def cors-headers
  {"Access-Control-Allow-Headers" "*"
   "Access-Control-Allow-Methods" "*"
   "Access-Control-Allow-Origin" "*"})

(def std-headers (merge cors-headers {"Content-Type" "application/json"}))

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

(defn- token->user [token]
  (let [payload (.getPayload token)]
    {:user/email (.getEmail payload)
     :user.google/id (.getSubject payload)
     :user/name (.get payload "name")
     :user/locale (.get payload "locale")
     :user/picture-url (.get payload "picture")}))

(defn- user-info-from-code [code]
  (let [token-req (make-token-req code)
        token-res (.execute token-req)
        refresh-token (.getRefreshToken token-res)
        id-token (.getIdToken token-res)
        user-info (-> token-res .parseIdToken token->user)]
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
  (let [builder (GoogleIdTokenVerifier$Builder. (NetHttpTransport.) (JacksonFactory/getDefaultInstance))
        verifier (-> builder
                     (.setAudience [(get-param "google-client-id")])
                     (.build))]
    (let [token (.verify verifier id-token)]
      (when (nil? token)
        (throw (ex-info "Bad token" {:anomaly :unauthorized})))
      (let [user (token->user token)]
        (when-not (is-existing-google-user? user)
          (throw (ex-info "Require sign up" {:anomaly :require-sign-up})))
        {:user user
         :session {:goog id-token}}))))

(defn- create-from-google* [{:keys [headers body]}]
  (try
    (let [{:keys [code id-token]} (-> body io/reader (json/read :key-fn keyword))
          result (if (some? code)
                   (create-from-code code)
                   (create-from-token id-token))]
      {:status 200
       :headers std-headers
       :body (json/write-str result)})
    (catch Exception e
      (case (-> e ex-data :anomaly)
        :unauthorized {:status 403
                       :headers std-headers
                       :body (json/write-str {:err-code :unauthorized})}
        :require-sign-up {:status 404
                          :headers std-headers
                          :body (json/write-str {:err-code :require-sign-up})}
        (do (cast/alert {:msg "Authentication failure" :ex e})
            {:status 500
             :headers std-headers
             :body (json/write-str {:err-code :error})})))))

(def create-from-google (apigw/ionize create-from-google*))

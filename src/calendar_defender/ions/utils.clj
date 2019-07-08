(ns calendar-defender.ions.utils
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [datomic.ion :as ion]
            [datomic.ion.lambda.api-gateway :as apigw]
            [datomic.ion.cast :as cast])
  (:import (java.io PushbackReader)
           (com.google.api.client.googleapis.auth.oauth2 GoogleIdTokenVerifier$Builder)
           (com.google.api.client.http.javanet NetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)))

(def cors-headers
  {"Access-Control-Allow-Headers" "*"
   "Access-Control-Allow-Methods" "*"
   "Access-Control-Allow-Origin" "*"})

(def std-headers (merge cors-headers {"Content-Type" "application/edn"}))

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

(defn apigw-ionize [handler]
  (apigw/ionize
    (fn [args]
      (try
        (handler args)
        (catch Exception e
          (case (-> e ex-data :anomaly)
            :unauthorized {:status 403
                           :headers std-headers
                           :body (pr-str {:err-code :unauthorized})}
            :require-sign-up {:status 404
                              :headers std-headers
                              :body (pr-str {:err-code :require-sign-up})}
            :bad-req {:status 400
                      :headers std-headers
                      :body (pr-str {:err-code :bad-req})}
            (do (cast/alert {:msg "Authentication failure" :ex e})
                {:status 500
                 :headers std-headers
                 :body (pr-str {:err-code :error})})))))))

(defn token->user [token]
  (let [payload (.getPayload token)]
    {:user/email (.getEmail payload)
     :user.google/id (.getSubject payload)
     :user/name (.get payload "name")
     :user/locale (.get payload "locale")
     :user/picture-url (.get payload "picture")}))

(defn verify-goog-token [id-token]
  (let [builder (GoogleIdTokenVerifier$Builder. (NetHttpTransport.) (JacksonFactory/getDefaultInstance))
        verifier (-> builder
                     (.setAudience [(get-param "google-client-id")])
                     (.build))]
    (let [token (.verify verifier id-token)]
      (when (nil? token)
        (throw (ex-info "Bad token" {:anomaly :unauthorized})))
      (token->user token))))

(defn get-user [{:keys [headers]}]
  (let [session (get headers "authorization")
        [sess-type id-token] (string/split session #" " 2)]
    (case sess-type
      "goog" (verify-goog-token id-token)
      (throw (ex-info "No token" {:anomaly :unauthorized})))))

(defn body->edn [body]
  (-> body
      io/reader
      (PushbackReader.)
      read))

(ns ^:figwheel-hooks calendar-defender.core
  (:require [reagent.core :as r]
            [cljs.core.async :as async]
            [cljs-http.client :as http]))

(defonce ^:private auth-state (r/atom {:session nil :user nil :err-msg nil}))
(defonce ^:private goog-auth (atom nil))

(defn- with-goog-auth [f]
  (if-let [a @goog-auth]
    (f a)
    (add-watch goog-auth f #(do (remove-watch goog-auth f)
                                (f %4)))))

(defn- session-from-google [code]
  (http/post
    "https://m03xfezsf4.execute-api.us-east-1.amazonaws.com/prod/session-from-google"
    {:json-params {:code code}
     :with-credentials? false}))

(defn- sign-in [code]
  (async/go
    (let [{:keys [session user]} (async/<! (session-from-google code))]
      (reset! auth-state {:session session :user user}))))

(defn- sign-in-with-google []
  (with-goog-auth
    (fn [auth]
      (-> (.grantOfflineAccess auth)
          (.then #(sign-in (.-code %)))
          (.catch #(reset! auth-state {:user nil :err-msg "Failed to sign in"}))))))

(defn- simple-component []
  [:div
     (if (nil? (:user @auth-state))
       [:button.google-sign-in {:on-click sign-in-with-google}]
       [:div "signed in"])])

(defn- got-user [user]
  (println "got user")
  (println user)
  (println "id" (. user getId))
  (println "signed in" (. user isSignedIn))
  (println "prof" (. user getBasicProfile))
  (println "auth" (. user getAuthResponse)))

(defn- init-auth []
  (let [client-id "155476034635-qu1n2kgsb73okovcd11j86ng13j2omad.apps.googleusercontent.com"
        scopes "https://www.googleapis.com/auth/calendar"
        gauth2 (.. js/gapi -auth2)]
    (.init gauth2 #js{"client_id" client-id
                      "scope" scopes})
    (let [auth (.getAuthInstance gauth2)
          user (.-currentUser auth)]
      (reset! goog-auth auth)
      (.listen user got-user))))

(defn- mount []
  (.load js/gapi "auth2" init-auth)
  (r/render [simple-component]
            (js/document.getElementById "app")))

(defn ^:after-load re-render []
  (mount))

(defonce start-up (do (mount) true))

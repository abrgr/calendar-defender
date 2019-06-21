(ns calendar-defender.containers.home
  (:require [reagent.core :as r]
            [cljs.core.async :as async]
            [cljs-http.client :as http]
            [calendar-defender.app-state :as app-state]))

(declare sign-in-with-google)

(defonce ^:private goog-auth (atom nil))

(defn- with-goog-auth [f]
  (if-let [a @goog-auth]
    (f a)
    (add-watch goog-auth f #(do (remove-watch goog-auth f)
                                (f %4)))))

(defn- session-from-google [opts]
  "opts takes keys of either :code or :id-token"
  (http/post
    "https://m03xfezsf4.execute-api.us-east-1.amazonaws.com/prod/session-from-google"
    {:edn-params opts
     :headers {"Accept" "application/edn"}
     :with-credentials? false}))

(defn- sign-in-error [{:keys [err-code]}]
  (case err-code
    :require-sign-up (sign-in-with-google)
    (reset! app-state/auth {:session nil :user nil :loading false :err-msg "Login failed"})))
    
(defn- sign-in [opts]
  "opts takes keys of either :code or :id-token"
  (async/go
    (let [{:keys [body status] :as resp} (async/<! (session-from-google opts))
          {:keys [session user]} body]
      (if (= status 200)
        (reset! app-state/auth {:session session :user user :loading false :err-msg nil})
        (sign-in-error body)))))

(defn- sign-in-with-google []
  (swap! app-state/auth assoc :loading true)
  (with-goog-auth
    (fn [auth]
      (-> (.grantOfflineAccess auth)
          (.then #(sign-in (.-code %)))
          (.catch #(reset! app-state/auth {:session nil :user nil :loading false :err-msg "Failed to sign in"}))))))

(defn- establish-session [id-token]
  (when-not (:loading @app-state/auth)
    (swap! app-state/auth assoc :loading true)
    (sign-in {:id-token id-token})))

(defn component []
  [:div
     (if (nil? (:user @app-state/auth))
       [:button.google-sign-in {:on-click sign-in-with-google}]
       [:div "signed in"])])

(defn- got-user [user]
  (when (and (some? user)
             (.isSignedIn user))
    (establish-session (.-id_token (.getAuthResponse user)))))

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

(.load js/gapi "auth2" init-auth)

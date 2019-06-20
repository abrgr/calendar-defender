(ns ^:figwheel-hooks calendar-defender.core
  (:require [reagent.core :as r]))

(defn simple-component []
  [:div
     [:button.google-sign-in]
     [:p "I am a component!"]
        [:p.someclass
            "I have " [:strong "bold"]
                [:span {:style {:color "red"}} " and red "] "text."]])

(defn- init-auth []
  (let [client-id "155476034635-qu1n2kgsb73okovcd11j86ng13j2omad.apps.googleusercontent.com"
        scopes "https://www.googleapis.com/auth/calendar"
        auth (-> (.. js/gapi -auth2)
                 (. init #js{"client_id" client-id
                             "scope" scopes}))]
    (println auth)))

(defn- mount []
  (.load js/gapi "auth2" init-auth)
  (r/render [simple-component]
            (js/document.getElementById "app")))

(defn ^:after-load re-render []
  (mount))

(defonce start-up (do (mount) true))

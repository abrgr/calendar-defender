(ns calendar-defender.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.history.Html5History)
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent]
            [calendar-defender.app-state :as app-state]))

(defn- hook-browser-navigation! []
  (doto (Html5History.)
        (events/listen
           EventType/NAVIGATE
           (fn [event]
             (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

(defn init! []
  (secretary/set-config! :prefix "#")
  
  (defroute "/" []
    (swap! app-state/nav assoc :page :home))
  
  (defroute "/flow" []
    (swap! app-state/nav assoc :page :flow))
  
  (hook-browser-navigation!))

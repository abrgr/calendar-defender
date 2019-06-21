(ns ^:figwheel-hooks calendar-defender.core
  (:require [reagent.core :as r]
            [calendar-defender.app-state :as app-state]
            [calendar-defender.routes :as routes]
            [calendar-defender.containers.home :as home]
            [calendar-defender.containers.flow :as flow]))

(defn- current-page []
  (case (:page @app-state/nav)
    :home [home/component]
    :flow [flow/component]
    [home/component]))

(defn- mount []
  (routes/init!)
  (r/render [current-page]
            (js/document.getElementById "app")))

(defn ^:after-load re-render []
  (mount))

(defonce start-up (do (mount) true))

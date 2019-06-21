(ns calendar-defender.app-state
  (:require [reagent.core :as r]))

(defonce nav (r/atom {:page :home :params {}}))
(defonce auth (r/atom {:goog-session nil :user nil :loading false :err-msg nil}))

(ns calendar-defender.app-state
  (:require [reagent.core :as r]))

(defn- start-node []
  (let [id (str (random-uuid))]
    {id {:id id
         :type :start
         :position {:x 100 :y 100}
         :ports {"out" {:id "out"
                        :type :output
                        :properties {:node-type :start}}}}}))

(defonce nav (r/atom {:page :home
                      :params {}}))
(defonce auth (r/atom {:goog-session nil
                       :user nil
                       :loading false
                       :err-msg nil}))
(defonce flow (r/atom {:offset {:x 0 :y 0}
                       :nodes (start-node)
                       :links {}
                       :selected {}
                       :hovered {}}))

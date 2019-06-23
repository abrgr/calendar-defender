(ns calendar-defender.app-state
  (:require [reagent.core :as r]))

(comment
(def chart {:offset {:x 0 :y 0}
            :nodes {"n1" {:id "n1"
                         :type :output-only
                         :position {:x 100
                                    :y 100}
                         :size {:width 100
                                :height 100}
                         :ports {
                           "p1" {:id "p1"
                                :type :output}
                           "p2" {:id "p2"
                                :type :output}}}
                    "n2" {:id "n2"
                         :type :input-output
                         :position {:x 100
                                    :y 100}
                         :size {:width 100
                                :height 100}
                         :ports {
                           "p1" {:id "p1"
                                :type :input}
                           "p2" {:id "p2"
                                :type :output}}}}
            :links {}
            :selected {}
            :hovered {}}))

(defonce nav (r/atom {:page :home
                      :params {}}))
(defonce auth (r/atom {:goog-session nil
                       :user nil
                       :loading false
                       :err-msg nil}))
(defonce flow (r/atom {:offset {:x 0 :y 0}
                       :nodes {}
                       :links {}
                       :selected {}
                       :hovered {}}))

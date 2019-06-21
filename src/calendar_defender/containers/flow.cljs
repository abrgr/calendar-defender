(ns calendar-defender.containers.flow
  (:require [reagent.core :as r]
            ["@mrblenny/react-flow-chart" :as react-flow-chart]))

(def ^:private flow-chart-component (r/adapt-react-class react-flow-chart/FlowChartWithState))

(defn component []
  [:div "hello world"]
  [flow-chart-component {:initialValue (clj->js {:offset {:x 0 :y 0}
                                                 :nodes {}
                                                 :links {}
                                                 :selected {}
                                                 :hovered {}})}])

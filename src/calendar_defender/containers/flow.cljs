(ns calendar-defender.containers.flow
  (:require [reagent.core :as r]
            ["@mrblenny/react-flow-chart" :as react-flow-chart]
            [calendar-defender.app-state :as app-state])
  (:require-macros [calendar-defender.containers.flow-macros :as flow-macros]))

(def ^:private flow-chart-component (r/adapt-react-class react-flow-chart/FlowChart))

(defn- handle-delete [state] state)

(def ^:private chart-callbacks
  #js{:onDragNode
        (flow-macros/handler-3 _ data id
          (swap! app-state/flow assoc-in [:nodes id :position] data))
      :onDragCanvas
        (flow-macros/handler-2 _ data
          (swap! app-state/flow assoc :offset data))
      :onLinkStart
        (flow-macros/handler-1 {:keys [linkId fromNodeId fromPortId]}
          (swap! app-state/flow assoc-in [:links linkId] {:id linkId
                                                          :from {:nodeId fromNodeId
                                                                 :portId fromPortId}
                                                          :to {}}))
      :onLinkMove
        (flow-macros/handler-1 {:keys [linkId toPosition]}
          (swap! app-state/flow assoc-in [:links linkId :to :position] toPosition))
      :onLinkComplete
        (flow-macros/handler-1 {:keys [linkId fromNodeId fromPortId toNodeId toPortId]}
          (let [s @app-state/flow
                from-type (get-in s [:nodes fromNodeId :ports fromPortId :type])
                to-type (get-in s [:nodes toNodeId :ports toPortId :type])]
            (if (or (not= [from-type to-type] [:input :output])
                    (not= [from-type to-type] [:output :input]))
              (swap! app-state/flow update :links #(dissoc % linkId))
              (swap! app-state/flow assoc-in [:links linkId :to] {:nodeId toNodeId
                                                                  :portId toPortId}))))
      :onLinkCancel
        (flow-macros/handler-1 {:keys [linkId]}
          (swap! app-state/flow update :links #(dissoc % linkId)))
      :onLinkMouseEnter
        (flow-macros/handler-1 {:keys [linkId]}
          (swap! app-state/flow assoc :hovered {:type :link
                                                :id linkId}))
      :onLinkMouseLeave
        (flow-macros/handler-1 _
          (swap! app-state/flow assoc :hovered {}))
      :onLinkClick
        (flow-macros/handler-1 {:keys [linkId]}
          (swap! app-state/flow assoc :selected {:type :link
                                                 :id linkId}))
      :onCanvasClick
        (flow-macros/handler-0
          (swap! app-state/flow assoc :selected {}))
      :onDeleteKey
        (flow-macros/handler-0
          (swap! app-state/flow handle-delete))
      :onNodeClick
         (flow-macros/handler-1 {:keys [nodeId]}
           (swap! app-state/flow assoc :selected {:type :node
                                                  :id nodeId}))
      :onNodeSizeChange
         (flow-macros/handler-1 {:keys [nodeId size]}
           (swap! app-state/flow assoc-in [:nodes nodeId :size] size))
      :onCanvasDrop
         (flow-macros/handler-1 {:keys [data position]}
           (let [id (random-uuid)]
             (swap! app-state/flow assoc-in [:nodes id] (merge data {:id id
                                                                     :position (select-keys position [:x :y])}))))
      :onPortPositionChange
         (fn [node port position]
           (clj->js (swap! app-state/flow assoc-in [:nodes (.-id node) :ports (.-id port) :position] {:x (.-x position)
                                                                                                      :y (.-y position)})))})

(defn component []
  [flow-chart-component {:chart (clj->js @app-state/flow)
                         :callbacks chart-callbacks}])


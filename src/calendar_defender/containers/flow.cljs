(ns calendar-defender.containers.flow
  (:require [reagent.core :as r]
            ["@mrblenny/react-flow-chart" :as react-flow-chart]
            [calendar-defender.app-state :as app-state])
  (:require-macros [calendar-defender.containers.flow-macros :as flow-macros]))

(def ^:private flow-chart-component (r/adapt-react-class react-flow-chart/FlowChart))

(defn- handle-delete [state] state)

(defn- stop-prop [e]
  (.stopPropagation e))

(defn- node-input [props]
  (let [node-input-props {:on-click stop-prop
                          :on-mouse-up stop-prop
                          :on-mouse-down stop-prop}]
    [:input (merge node-input-props props)]))

(defmulti node-component* #(-> % :node .-type keyword))
(defmethod node-component* :mult-choice-single
  [{:keys [node]}]
  (let [{{:keys [question answers]} :properties :keys [id ports]} (js->clj node :keywordize-keys true)]
    (->> (concat [:div.flow-node]
           [[node-input {:class :question
                         :value question
                         :on-change #(swap! app-state/flow assoc-in [:nodes id :properties :question] (-> % .-target .-value))}]]
           (->> answers
                (map-indexed
                   (fn [idx answer]
                     ^{:key idx} [node-input {:class :answer
                                              :value answer
                                              :on-change #(swap! app-state/flow assoc-in [:nodes id :properties :answers idx] (-> % .-target .-value))}])))
           [[:button.add-answer
              {:on-click #(swap! app-state/flow update-in [:nodes id] (fn [node]
                                                                          (let [port-idx (-> node
                                                                                             (get-in [:properties :answers])
                                                                                             count)
                                                                                port-id (str port-idx)]
                                                                            (-> node
                                                                                (update-in [:properties :answers] conj "")
                                                                                (assoc-in [:ports port-id] {:id port-id
                                                                                                            :type :output
                                                                                                            :properties {:idx port-idx}})))))}
              "+"]])
         (into []))))
(defmethod node-component* :decline
  [{:keys [node]}]
  (let [{{:keys [reason]} :properties :keys [id]} (js->clj node :keywordize-keys true)]
    [:div.flow-node
       [node-input {:class :reason
                    :value reason
                    :on-change #(swap! app-state/flow assoc-in [:nodes id :properties :reason] (-> % .-target .-value))}]]))

(def ^:private node-component (r/reactify-component node-component*))

(defn- map-keys-to-str [m]
  (->> m
       (mapv #(vector (-> % first name) (second %)))
       (into {})))

(def ^:private chart-callbacks
  #js{:onDragNode
        (flow-macros/handler-3 _ data id
          (swap! app-state/flow assoc-in [:nodes id :position] (select-keys data [:x :y])))
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
          (swap! app-state/flow assoc-in [:links linkId :to :position] (select-keys toPosition [:x :y])))
      :onLinkComplete
        (flow-macros/handler-1 {:keys [linkId fromNodeId fromPortId toNodeId toPortId]}
          (let [s @app-state/flow
                from-type (get-in s [:nodes fromNodeId :ports fromPortId :type])
                to-type (get-in s [:nodes toNodeId :ports toPortId :type])]
            (if (not= #{from-type to-type} #{:input :output})
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
           (let [id (str (random-uuid))
                 node (update data :ports map-keys-to-str)]
             (swap!
               app-state/flow
               assoc-in
               [:nodes id]
               (-> node
                   (merge {:id id
                           :position (select-keys position [:x :y])})
                   (update-in [:type] keyword)
                   (update-in [:ports] (fn [ports] (->> ports (map #(update-in % [1 :type] keyword)) (into {}))))))))
      :onPortPositionChange
         (fn [node port position]
           (let [node-id (.-id node)
                 port-id (.-id port)
                 pos {:x (.-x position) :y (.-y position)}]
             (clj->js (swap! app-state/flow assoc-in [:nodes node-id :ports port-id :position] pos))))})

(defn component []
  [:div.flow-page
    [flow-chart-component {:chart (clj->js @app-state/flow)
                           :callbacks chart-callbacks
                           :Components #js{"NodeInner" node-component}}]
    [:div.sidebar
      [:div.item {:draggable true
                  :onDragStart #(-> %
                                    .-dataTransfer
                                    (.setData
                                       react-flow-chart/REACT_FLOW_CHART
                                       (.stringify js/JSON (clj->js {:type :decline
                                                                     :ports {"input" {:id "input"
                                                                                      :type :input
                                                                                      :properties {}}}
                                                                     :properties {:reason ""}}))))}
                 "Decline meeting"]
      [:div.item {:draggable true
                  :onDragStart #(-> %
                                    .-dataTransfer
                                    (.setData
                                       react-flow-chart/REACT_FLOW_CHART
                                       (.stringify js/JSON (clj->js {:type :mult-choice-single
                                                                     :ports {"all" {:id "all"
                                                                                    :type :output
                                                                                    :properties {}}}
                                                                     :properties {:question ""
                                                                                  :answers []}}))))}
                 "Multiple choice with single selection"]]])


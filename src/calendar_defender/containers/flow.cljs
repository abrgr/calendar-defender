(ns calendar-defender.containers.flow
  (:require [reagent.core :as r]
            ["@mrblenny/react-flow-chart" :as react-flow-chart]
            [calendar-defender.app-state :as app-state])
  (:require-macros [calendar-defender.containers.flow-macros :as flow-macros]))

(def ^:private flow-chart-component (r/adapt-react-class react-flow-chart/FlowChart))

(def ^:private color-palette
  ["#561D25" "#CE8147" "#ECDD7B" "#D3E298" "#CDE7BE"])

(defn- handle-delete [state] state)

(defn- stop-prop [e]
  (.stopPropagation e))

(defn- node-input [props]
  (let [node-input-props {:on-click stop-prop
                          :on-mouse-up stop-prop
                          :on-mouse-down stop-prop}]
    [:input (merge node-input-props props)]))

(def ^:private num-border-width "4px")

(defn- number-component [{:keys [idx]}]
  (let [title (inc idx)
        color (get color-palette (mod idx (count color-palette)))]
    [:div {:style {:border-bottom (str num-border-width " solid " color)}}
      (if (some? idx) (inc idx) "Other")]))

(defmulti node-component* #(-> % :node .-type keyword))
(defmethod node-component* :mult-choice-single
  [{:keys [node]}]
  (let [{{:keys [question answers]} :properties :keys [id ports]} (js->clj node :keywordize-keys true)]
    (->> (concat [:div.flow-node]
           [[:div.node-title "Multiple choice"]
            [:div "Question"]
            [node-input {:class :question
                         :value question
                         :on-change #(swap! app-state/flow assoc-in [:nodes id :properties :question] (-> % .-target .-value))}]
            [:div "Answers"]]
           (->> answers
                (map-indexed
                   (fn [idx answer]
                     ^{:key idx} [:div {:style {:display "flex"}}
                                    [number-component {:idx idx}]
                                    [node-input {:class :answer
                                                 :value answer
                                                 :on-change #(swap! app-state/flow assoc-in [:nodes id :properties :answers idx] (-> % .-target .-value))}]])))
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
                                                                                                            :properties {:node-type :mult-choice-single
                                                                                                                         :idx port-idx}})))))}
              "+"]])
         (into []))))
(defmethod node-component* :decline
  [{:keys [node]}]
  (let [{{:keys [reason]} :properties :keys [id]} (js->clj node :keywordize-keys true)]
    [:div.flow-node
       [:div.node-title "Decline Meeting"]
       [:div "Decline Reason"]
       [node-input {:class :reason
                    :value reason
                    :on-change #(swap! app-state/flow assoc-in [:nodes id :properties :reason] (-> % .-target .-value))}]]))
(defmethod node-component* :start
  [{:keys [node]}]
  [:div.flow-node {:style {:background-color "green"
                           :color "white"}}
     [:div.node-title "Calendar Event Received"]])

(def ^:private node-component (r/reactify-component node-component*))

(defn- port-inner-component [{:keys [selected]}]
  [:div {:style {:width "24px"
                 :height "24px"
                 :border-radius "50%"
                 :background "white"
                 :cursor "pointer"
                 :display "flex"
                 :justify-content "center"
                 :align-items "center"}}
    [:div {:style {:width "12px"
                   :height "12px"
                   :border-radius "50%"
                   :background (if selected "cornflowerblue" "grey")
                   :cursor "pointer"}}]])

(defmulti port-component* #(-> % :port .-properties (aget "node-type") keyword))
(defmethod port-component* :mult-choice-single
  [{:keys [port isLinkSelected isLinkHovered]}]
  (let [idx (-> port .-properties .-idx)]
    [:div {:style {:cursor "pointer"
                   :display "flex"
                   :flex-direction "column"
                   :justify-content "center"
                   :align-items "center"}}
      (if (some? idx)
        [number-component {:idx idx}]
        [:div {:style {:border-bottom (str num-border-width " solid transparent")}} "Other"])
      [port-inner-component {:selected (or isLinkSelected isLinkHovered)}]]))
(defmethod port-component* :default
  [{:keys [port isLinkSelected isLinkHovered]}]
  [port-inner-component {:selected (or isLinkSelected isLinkHovered)}])

(def ^:private port-component (r/reactify-component port-component*))

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

(def ^:private sidebar-nodes
  {"Decline meeting"
    {:type :decline
     :ports {"input" {:id "input"
                      :type :input
                      :properties {}}}
     :properties {:reason ""}}
   "Multiple choice with single selection"
     {:type :mult-choice-single
      :ports {"in" {:id "in"
                    :type :input
                    :properties {}}
              "other" {:id "other"
                       :type :output
                       :properties {:node-type :mult-choice-single}}}
      :properties {:question ""
                   :answers []}}})

(defn- sidebar-item [[title node]]
  ^{:key title} [:div.item {:draggable true
                            :onDragStart #(-> %
                                              .-dataTransfer
                                              (.setData
                                                 react-flow-chart/REACT_FLOW_CHART
                                                 (.stringify js/JSON (clj->js node))))}
     title])

(defmulti nodeify (fn [edges {:keys [type]}] type))
(defmethod nodeify :start
  [edges {:keys [id ports]}]
  (nodeify edges (get-in edges [{:node-id id :port-id (-> ports keys first)} :node])))
(defmethod nodeify :mult-choice-single
  [edges {:keys [id ports] {:keys [question answers]} :properties}]
  (let [other-node (get-in edges [{:node-id id :port-id "other"} :node])
        regular-nexts (->> ports
                           (map second)
                           (map #(hash-map :edge (get edges {:node-id id :port-id (:id %)}) :port %))
                           (filter #(and (-> % :edge some?)
                                         (not= (-> % :port :id) "other")))
                           (mapv #(merge (nodeify edges (-> % :edge :node))
                                         {:n/when {:n/op :n.op/=
                                                   :n.op/args [{:n.op.args/type :n.op.args.type/ref
                                                                :n.op.args.ref/ref id}
                                                               {:n.op.args/type :n.op.args.type/str
                                                                :n.op.args.str/str (nth answers (-> % :port :properties :idx))}]}})))
        other-nexts (if (nil? other-node)
                        []
                        [(merge (nodeify edges other-node)
                                {:n/when {:n/op :n.op/not
                                          :n.op/args [{:n/op :n.op/or
                                                       :n.op/args (mapv :n/when regular-nexts)}]}})])]
    {:n/type :n.type/mult-choice-single
     :n/id id
     :n.mult-choice/q question
     :n.mult-choice/ans (mapv #(hash-map :n.mult-choice.ans/text % :n.mult-choice.ans/val %) answers)
     :n/next (->> regular-nexts
                  (concat other-nexts)
                  (into []))}))
(defmethod nodeify :decline
  [edges {:keys [id] {:keys [reason]} :properties}]
  {:n/type :n.type/decline
   :n.decline/reason reason})

(defn- to-nodes [{:keys [nodes links]}]
  (let [edges (->> links
                   vals
                   (reduce
                     (fn [edges
                          {{from-node-id :nodeId from-port-id :portId} :from
                           {to-node-id :nodeId to-port-id :portId} :to}]
                       (assoc
                         edges
                         {:node-id from-node-id
                          :port-id from-port-id}
                         {:node-id to-node-id
                          :port-id to-port-id
                          :node (get nodes to-node-id)}))
                     {}))
        start (->> nodes
                   (filter #(-> % second :type (= :start)))
                   (map second)
                   first)]
    (nodeify edges start)))

(defn component []
  [:div.flow-page
    [:div.top-nav
      [:button {:onClick #(println "VAL" (to-nodes @app-state/flow))} "Save"]]
    [:div.main
      [flow-chart-component {:chart (clj->js @app-state/flow)
                             :callbacks chart-callbacks
                             :Components #js{"NodeInner" node-component
                                             "Port" port-component}}]
      [:div.sidebar
        (map sidebar-item sidebar-nodes)]]])

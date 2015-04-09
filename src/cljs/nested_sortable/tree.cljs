(ns nested-sortable.tree
  (:require [reagent.core :as r :refer [atom wrap]]
            [goog.events :as events]))

(defn visible-if [pred]
  (if pred "visible" "hidden"))

(defn tree [node]
  [:div
   [:div.node (:name node)]
   [:div.children
    (for [child (:children node)]
      [tree child])]])

(def state (atom {:drag-path []
                  :start-path []
                  :drag-node {}}))

(defn node-attr [node path]
  {:draggable true
   :on-drag-start (fn [event]
                    (swap! state assoc :drag-node node)
                    (swap! state assoc :start-path path)
                    (.stopPropagation event))
   :on-drag-over (fn [event]
                   (swap! state assoc :drag-path path)
                   (.stopPropagation event))
   :class (visible-if (not= (:id node) (get-in @state [:drag-node :id])))})

(defn get-node [node path]
  (if (empty? path)
    node
    (get-node (get-in node [:children (first path)])
              (rest path))))

(defn update-node [node path value]
  (if (empty? path)
    value
    (assoc-in node [:children (first path)]
              (update-node (get-in node [:children (first path)])
                           (rest path)
                           value))))

(defn remove-child [parent id]
  (assoc parent :children
         (into [] (filter #(not= (:id %) id)
                          (:children parent)))))

(defn add-child [parent pos node]
  (let [children (:children parent)]
    (assoc parent :children
           (let [[head tail] (split-at pos children)]
             (into [] (concat head (cons node tail)))))))

(defn update-children [root path f]
  (let [parent-path (butlast path) 
        parent (get-node root parent-path)]
    (update-node root
                 parent-path
                 (f parent))))

(defn remove-node [root path]
  (let [removed-node (get-node root path)]
    (update-children root path
                     (fn [parent]
                       (remove-child parent (:id removed-node))))))

(defn add-node [root path node]
  (update-children root path
                   (fn [parent]
                     (add-child parent (last path) node))))

(defn move-node [root old-path new-path]
  (let [node (get-node root old-path)]
    (-> (remove-node root old-path)
        (add-node new-path node))))

(defn placeholder [display path]
  [:div.placeholder {:class (visible-if (= path (:drag-path @state)))}
   [display (:drag-node @state)]])

(defn tree-node [node path display is-root?]
  [:div
   ^{:key (:id node)}
   (node-attr node path)
   [placeholder display path]
   [:div
    (if-not is-root? [display node path])
    [:div.children
     (if-not (empty? (:children node))
       (for [[pos child] (map vector (range) (:children node))]
         [tree-node child (conj path pos) display false]))]]])

(defn tree [root-node]
  (let [display (fn [item path]
                  [:div.node
                   (:name item)
                   (str " " path)])]
    [:div.tree
     {:on-drag-end (fn [event] 
                     (.stopPropagation event)
                     (reset! root-node
                             (move-node @root-node
                                        (:start-path @state)
                                        (:drag-path @state)))
                     (swap! state assoc
                            :drag-path []
                            :start-path []
                            :drag-node {}))}
     [:div
      (str @state)]
     [tree-node @root-node [] display true]]))

(enable-console-print!)

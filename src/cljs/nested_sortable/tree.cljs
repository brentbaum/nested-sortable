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

(defn remove-child [parent pos]
  (let [children (:children parent)]
    (assoc parent :children
           (into [] (concat (subvec children 0 pos)
                            (subvec children (inc pos) (count children)))))))

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
                       (println (remove-child parent (last path)))
                       (remove-child parent (last path))))))

(defn add-node [root path node]
  (update-children root path
                   (fn [parent]
                     (add-child parent (last path) node))))

(defn update-path [add-path remove-path]
  (let [i (dec (count add-path))]
    (if (and (<= i (dec (count remove-path)))
            (<= (get add-path i) (get remove-path i)))
      (update-in remove-path [i] inc)
      remove-path))
)

(defn move-node [root old-path add-path]
  ;; If there's actually been movement (start != finish)
  (println "Moving from" old-path " to " add-path)
  (if (not= add-path
            (update-in old-path [(dec (count old-path))] inc))
    (let [node (get-node root old-path)
          remove-path (update-path add-path old-path)]
      (-> root
          (add-node add-path node)
          (remove-node remove-path)))
    root))

(defn placeholder [display path]
  [:div.placeholder {:class (visible-if (= path (:drag-path @state)))}
   [display (:drag-node @state)]])

(defn tree-node [node path display is-root?]
  [:div
   ^{:key (:id node)}
   (if-not is-root?
     (node-attr node path))
   [:div
    (if-not is-root?
      [:div
       [placeholder display path]
       [display node path]])
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

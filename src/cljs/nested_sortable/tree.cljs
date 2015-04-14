(ns nested-sortable.tree
  (:require [reagent.core :as r :refer [atom wrap]]
            [goog.events :as events]))

(defn visible-if [pred]
  (if pred "visible" "hidden"))

(def state (atom {:drag-path []
                  :start-path []
                  :drag-node {}
                  :add-as-child false}))

(def mouse (atom {:pos {:x 0
                        :y 0}
                  :dragging? false
                  :down? false
                  :in-tree? false}))

(defn allow-drop [e]
  (.preventDefault e)) 

(defn add-as-child? [event node]
  (let [event-x (.-clientX event)
        element-x (-> event .-target .getBoundingClientRect .-left)]
    (< 50 (- event-x element-x))))

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
      remove-path)))

(defn move-node [root old-path add-path]
  ;; If there's actually been movement (start != finish)
  (if (not= add-path
            (update-in old-path [(dec (count old-path))] inc))
    (let [node (get-node root old-path)
          remove-path (update-path add-path old-path)]
      (-> root
          (add-node add-path node)
          (remove-node remove-path)))
    root))

(defn path-below [root path]
  (if path
    (let [parent (get-node root (butlast path))]
      (if (< (last path) (count (:children parent)))
        (update-in path [(dec (count path))] inc)
        (path-below root (butlast path))))
    []))

(defn placeholder [display path]
  [:div.placeholder {:class (visible-if (= path (:drag-path @state)))}
   [display (:drag-node @state)]])

(defn node-attr [node path is-root?]
  {:on-mouse-down (fn [event]
                    (.stopPropagation event)
                    (swap! state assoc
                           :drag-node node
                           :drag-path path
                           :start-path path)
                    (swap! mouse assoc
                           :dragging? true
                           :in-tree? true
                           :pos {:x (+ 20 (.-clientX event))
                                 :y (.-clientY event)}))
   :on-mouse-over (fn [event]
                    (.stopPropagation event)
                    (.preventDefault event)
                    (if (and (:in-tree? @mouse) (:dragging? @mouse))
                      (swap! state assoc
                             :add-as-child (and (:children node)
                                                (not= (-> @state :drag-node :id)
                                                      (:id node))
                                                (add-as-child? event node))
                             :drag-path path)))
   })

(defn end-drag [root-node]
  (let [{:keys [start-path drag-path add-as-child]} @state]
    (reset! root-node
            (move-node @root-node
                       start-path
                       (if add-as-child
                         (conj drag-path 0)
                         (update-in drag-path [(dec (count drag-path))] inc))))
    (swap! state assoc
           :drag-path []
           :start-path []
           :drag-node {})
    (swap! mouse assoc
           :dragging? false
           :in-tree? false)))

(defn tree-node [node path display is-root?]
  [:div
   ^{:key (:id node)}
   (node-attr node path is-root?)
   [:div
    (if-not is-root?
      [:div
       {:class (visible-if (not= (:id node)
                                 (get-in @state [:drag-node :id])))}
       [display node path]])
    [:div.children
     {:class (visible-if (not= (:id node)
                               (get-in @state [:drag-node :id])))}
     (if (and (not is-root?) (:add-as-child @state))
       [placeholder display path])
     (if-not (empty? (:children node))
       (for [[pos child] (map vector (range) (:children node))]
         [tree-node child (conj path pos) display false]))]
    (if-not (or is-root? (:add-as-child @state))
      [placeholder display path])]])

(defn tree [root-node display]
  (track-mouse-state root-node)
  [:div.tree
   {
    :on-mouse-move (fn [event]
                     (if (:dragging? @mouse)
                       (swap! mouse assoc :pos {:x (+ 20 (.-clientX event))
                                                :y (- (.-clientY event) 20)})))
    :on-mouse-enter (fn [event]
                      (if (:down? @mouse)
                        (swap! mouse assoc :in-tree? true)
                        (end-drag root-node)))

    :on-mouse-leave (fn [event]
                      (if (:dragging? @mouse)
                        (do (swap! mouse assoc :in-tree? false)
                            (swap! state (fn [s]
                                           (assoc s
                                             :drag-path (:start-path s)
                                             :add-as-child false))))))} 
   [:div.drag-ghost
    {:style {:top (-> @mouse :pos :y)
             :left (-> @mouse :pos :x)}}
    (if (and (:in-tree? @mouse) (:dragging? @mouse))
      [display (:drag-node @state) (:drag-path @state) false])]
   [:div (str @state) "   " (str @mouse)]
   [tree-node @root-node [] display true]])

(defn track-mouse-state [root-node]
  (aset js/document "onmousedown" (fn [] (swap! mouse assoc :down? true)))
  (aset js/document "onmouseup" (fn [event]
                                  (.preventDefault event)
                                  (if (:dragging? @mouse)
                                    (end-drag root-node))
                                  (swap! mouse assoc :down? false))))

(enable-console-print!)

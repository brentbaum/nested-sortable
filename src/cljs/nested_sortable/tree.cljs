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

(defn top-region? [event]
  (let [top (-> event .-target .getBoundingClientRect .-top)
        event-y (.-clientY event)]
    (< (- event-y top)
       10)))

(defn count-tree [node]
  (if node
    (+ 1 (reduce #(+ %1 (count-tree %2)) 0 (:children node)))))

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
  (if (and (< 0 (count old-path))
           (not= add-path 
                 (update-in old-path [(dec (count old-path))] inc)))
    (let [node (get-node root old-path)
          remove-path (update-path add-path old-path)]
      (-> root
          (add-node add-path node)
          (remove-node remove-path)))
    root))

(defn placeholder [display path]
  [:div.placeholder {:class (visible-if (= path (:drag-path @state)))}
   [wrap-display display (:drag-node @state) path]])

(defn node-attr [node path]
  {:on-mouse-over (fn [event]
                    (.stopPropagation event)
                    (.preventDefault event)
                    (if (and (:in-tree? @mouse) (:dragging? @mouse))
                      (swap! state assoc
                             :add-as-child (and (:children node)
                                                (not= (-> @state :drag-node :id)
                                                      (:id node))
                                                (add-as-child? event node))
                             :drag-path path)))})

(defn mouse-down [node path]
  (fn [event]
    (.stopPropagation event)
    (swap! state assoc
           :drag-node node
           :drag-path path
           :start-path path)
    (swap! mouse assoc
           :dragging? true
           :in-tree? true
           :pos {:x (+ 20 (.-clientX event))
                 :y (.-clientY event)})
    ;; Make the rest of the document unselectable
    (-> js/document .-documentElement .-classList
        (.add "no-select"))))

(defn end-drag [root-node]
  (let [{:keys [start-path drag-path add-as-child]} @state]
    (if (not= drag-path [])
      (do
        (reset! root-node
                (move-node @root-node
                           start-path
                           (if (and add-as-child
                                    (< 0 (count drag-path)))
                             (conj drag-path 0)
                             (update-in drag-path [(dec (count drag-path))] inc))))))
    (swap! state assoc
           :drag-path []
           :start-path []
           :drag-node {})
    (swap! mouse assoc
           :dragging? false
           :in-tree? false)))

(defn drag-ghost [display]
  [:div.drag-ghost
   {:style {:top (-> @mouse :pos :y)
            :left (-> @mouse :pos :x)}}
   (if (and (:in-tree? @mouse) (:dragging? @mouse))
     [wrap-display display (:drag-node @state) (:drag-path @state)])])

(defn attach-node-attr [block node path root-node]
  (if (and (vector? block)
           (map? (second block)))
    (assoc block 1
           (-> (second block)
               (#(if (:drag-grip %)
                   (merge % {:on-mouse-down (mouse-down node path)}) %))
               ;; Any other attributes we want to add will go here
               ))
    block))

(defn set-attrs [block node path root-node]
  (if (vector? block)
    (into []
          (map #(-> %
                    (attach-node-attr node path root-node)
                    (set-attrs node path root-node))
               block))
    block))

(defn track-mouse-state [root-node]
  (aset js/document "onmousedown" (fn []
                                    (swap! mouse assoc :down? true)))
  (aset js/document "onmouseup" (fn [event]
                                  (.preventDefault event)
                                  (-> js/document .-documentElement .-classList
                                      (.remove "no-select"))
                                  (if (:dragging? @mouse)
                                    (end-drag root-node))
                                  (if :dragging? @mouse
                                      (println (count-tree @root-node))) 
                                  (swap! mouse assoc :down? false))))

(defn wrap-display [display node path root]
  (display (r/wrap node
                   (fn [val] (reset! root (update-node @root path val))))
           path))

(defn tree-node [node path display root-node is-root?]
  [:div
   ^{:key (:id node)}
   (node-attr node path)
   [:div
    (if-not is-root?
      [:div
       {:class (visible-if (not= (:id node)
                                 (get-in @state [:drag-node :id])))}
       (-> (wrap-display display node path root-node)
           (set-attrs node path root-node))])
    [:div.children
     {:class (visible-if (not= (:id node)
                               (get-in @state [:drag-node :id])))}
     (if is-root? [:div [placeholder display [-1]]])
     (if (and (not is-root?) (:add-as-child @state))
       [placeholder display path])
     (if-not (empty? (:children node))
       (for [[pos child] (map vector (range) (:children node))]
         [tree-node child (conj path pos) display root-node false]))]
    (if-not (or is-root? (:add-as-child @state))
      [placeholder display path])]])

(defn tree [root-node display]
  (track-mouse-state root-node)
  (fn [root-node display]
    [:div.tree-container
     [:div (str @state @mouse)]
     [:div.top-region
      {:on-mouse-move
       (fn [] (if (:dragging? @mouse)
                (swap! state assoc
                       :add-as-child false
                       :drag-path [-1])))}]
     [:div.tree.no-select
      {:on-mouse-move (fn [event]
                        (if (:dragging? @mouse)
                          (do 
                            (swap! mouse assoc :pos {:x (+ 20 (.-clientX event))
                                                     :y (- (.-clientY event) 20)}))))
       :on-mouse-enter (fn [event]
                         (if (and (:dragging? @mouse) (:down? @mouse))
                           (swap! mouse assoc :in-tree? true)))

       :on-mouse-leave (fn [event]
                         (if (:dragging? @mouse)
                           (do (swap! mouse assoc :in-tree? false)
                               (swap! state (fn [s]
                                              (assoc s
                                                :drag-path (:start-path s)
                                                :add-as-child false))))))} 
      [drag-ghost display]
      [tree-node @root-node [] display root-node true]]]))

(enable-console-print!)

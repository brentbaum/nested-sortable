(ns nested-sortable.list
  (:require 
            [reagent.core :as r :refer [atom]] 
            [goog.events :as events]))

(defn visible-if [pred]
  (if pred "visible" "hidden"))

(defn find-node [coll node]
  (first (keep-indexed #(when (= (:id %2) (:id node)) %1) coll)))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn insert-node-at-pos [coll node pos]
  (apply merge
         (subvec coll 0 pos)
         node
         (subvec coll pos)))

(defn after-node? [event]
  (let [target (.-target event)
        relY (- (.-clientY event) (.-offsetTop target))
        height (/ (.-offsetHeight target) 2)]
    (< height relY)))

(defn drag-end [event coll node insert-pos]
  (let [remove-pos (find-node @coll node)
        shifted-pos (if (< insert-pos remove-pos)
                      (inc insert-pos)
                      insert-pos)]
    (swap! coll (fn [c] (-> c
                            (vec-remove remove-pos)
                            (insert-node-at-pos node shifted-pos))))))

(defn drag-over [event index dragover-index]
  (reset! dragover-index (if (after-node? event)
                           index
                           (dec index))))

(defn wrap-node [node coll index]
  (r/wrap node
        swap! coll assoc index))

(defn sortable-list [node-component coll]  
  (let [dragged-node (atom {})
        dragover-index (atom -2)]
    (r/create-class
     {:render
      (fn [this]
        [:div
         [:div @dragged-node @dragover-index]
         [:div.list
          (for [[index node] (map vector (range) @coll)]
            (let [{:keys [id name]} node]
              [:div {:data-id  id}
               [:div.placeholder {:class (visible-if (and (not= dragged-node node)
                                                          (= (inc @dragover-index) index)))}
                [node-component dragged-node @dragover-index]] 
               [:div.node {:key id
                           :class (visible-if (not= (:id node) (:id @dragged-node)))
                           :draggable true
                           :on-drag-start (fn [event]
                                            (reset! dragged-node node)
                                            (drag-over event index dragover-index))
                           :on-drag-over (fn [event]
                                           (drag-over event index dragover-index))
                           :on-drag-end (fn [event]
                                          (drag-end event coll @dragged-node @dragover-index)
                                          (reset! dragover-index -2)
                                          (reset! dragged-node {}))}
                [node-component (wrap-node node coll index) index]]]))
          [:div.placeholder {:class (visible-if (= (inc @dragover-index) (count @coll)))}
           [node-component dragged-node @dragover-index]]]])}))) 

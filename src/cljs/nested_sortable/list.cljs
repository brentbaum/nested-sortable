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
  (if (< pos 0)
    (apply merge node coll)
    (apply merge
           (subvec coll 0 pos)
           node
           (subvec coll pos))))

(defn after-node? [event]
  (let [target (.-target event)
        relY (- (.-clientY event) (.-offsetTop target))
        height (.-offsetHeight target) ]
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

(defn sortable-list-impl [node-component coll dragged-node]
  (let [dragover-index (atom -2)]
    (r/create-class
     {:render
      (fn [this]
        [:div.list
         {:on-drag-over (fn [event]
                           (reset! dragover-index -2)
                           (.stopPropagation event))}
         [:div @dragged-node @dragover-index]
         (for [[index node] (map vector (range) @coll)]
           ^{:key (:id node)}
           (let [{:keys [id name]} node]
             [:div {:data-id  id}
              [:div.placeholder {:class (visible-if (and (not= dragged-node node)
                                                         (= (inc @dragover-index) index)))}
               [node-component dragged-node dragged-node]]
              [:div {:key id
                     :class (visible-if (not= (:id node) (:id @dragged-node)))
                     :draggable true
                     :on-drag-start (fn [event]
                                      (reset! dragged-node node)
                                      (drag-over event index dragover-index)
                                      (.stopPropagation event))
                     :on-drag-over (fn [event]
                                     (drag-over event index dragover-index)
                                     (.stopPropagation event))
                     :on-drag-end (fn [event]
                                    (if-not (empty? @dragged-node)
                                      (drag-end event coll @dragged-node @dragover-index))
                                    (reset! dragover-index -2)
                                    (reset! dragged-node {})
                                    (.stopPropagation event))}
               [node-component (wrap-node node coll index) dragged-node]]]))
         [:div.placeholder {:class (visible-if (= (inc @dragover-index) (count @coll)))}
          [node-component dragged-node dragged-node]]])}))) 

(defn sortable-list [node-component coll]
  (let [dragged-node (atom {})]
    [sortable-list-impl node-component coll dragged-node]))

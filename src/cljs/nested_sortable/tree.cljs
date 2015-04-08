(ns nested-sortable.tree
  (:require [reagent.core :as r :refer [atom wrap]]
            [goog.events :as events]
            [nested-sortable.list :refer [sortable-list-impl]]))

(defn tree [node]
  [:div
   [:div.node (:name node)]
   [:div.children
    (for [child (:children node)]
      [tree child])]])

(defn list-sortable-tree [node dragged-node]
  [:div 
   (if-not (empty? @node)
     [:div
      [:div.node (:name @node)]
      [:div.children
       (if-not (empty? (:children @node))
         [sortable-list-impl
          sortable-tree
          (wrap (:children @node)
                swap! node assoc :children)
          dragged-node])]])])

(defn sortable-tree-root [coll]
  (let [dragged-node (atom {})]
    [sortable-list-impl sortable-tree coll dragged-node]))


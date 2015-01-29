(ns nested-sortable.tree
  (:require [reagent.core :as r :refer [atom wrap]]
            [goog.events :as events]
            [nested-sortable.list :refer [sortable-list]]))

(defn tree [node]
  [:div
   [:div.node (:name node)]
   [:div.children
    (for [child (:children node)]
      [tree child])]])

(defn sortable-tree [coll]
  [sortable-list
   (fn [node index]
     [:div
      (:name @node)
      [tree @node]])
   coll])

(ns nested-sortable.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [nested-sortable.tree :refer [tree]])
    (:import goog.History))

;; -------------------------
;; Views

(def tree-data (atom {:name "Root"
                      :id 0
                      :children [{:name "Child 1"
                                  :id 1
                                  :children [{:name "Child 1-1"
                                              :id 11
                                              :children []}
                                             {:name "Child 1-2"
                                              :id 12
                                              :children [{:name "Child 1-2-1"
                                                          :id 121
                                                          :children []}]}
                                             {:name "Child 1-3"
                                              :id 13
                                              :children []}]}
                                 {:name "Child 2"
                                  :id 2
                                  :children [{:name "Child 2-1"
                                              :id 21
                                              :children []}
                                             {:name "Child 2-2"
                                              :id 22
                                              :children []}]}]}))

(def list-data (atom [{:name "One"
                       :id 1}
                      {:name "Two"
                       :id 2}
                      {:name "Three"
                       :id 3}
                      {:name "Four"
                       :id 4}]))

(defn display [node path]
  [:div.node
   [:span.grip {:drag-grip true}]
   (:name node)
   [:span.remove {:on-click #(println path)}
    "x"]])

(defn home-page []
  [:div [:h2 "Nested-sortable"]
   [:hr]
   [tree tree-data display]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page home-page))

;; -------------------------
;; Initialize app
(defn init! []
  (reagent/render-component [current-page] (.getElementById js/document "app")))

;; -------------------------
;; History
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))
;; need to run this after routes have been defined
(hook-browser-navigation!)

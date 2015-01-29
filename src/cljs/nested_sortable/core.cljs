(ns nested-sortable.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
<<<<<<< HEAD
              [nested-sortable.list :refer [sortable-list]]
              [nested-sortable.tree :refer [sortable-tree]])
=======
              [nested-sortable.tree :refer [tree sortable-list]])
>>>>>>> f57988d8b320f222cf2522d25800a404e222f72e
    (:import goog.History))

;; -------------------------
;; Views

<<<<<<< HEAD
(def tree-data (atom [{:name "Child 1"
                       :id 1
                       :children []}
                      {:name "Child 2"
                       :id 2
                       :children []}]))
=======
(def data {:name "Root"
            :id 0
            :children [{:name "Child 1"
                        :id 1
                        :children []}
                       {:name "Child 2"
                        :id 2
                        :children []}]})
>>>>>>> f57988d8b320f222cf2522d25800a404e222f72e

(def list-data (atom [{:name "One"
                       :id 1}
                      {:name "Two"
                       :id 2}
                      {:name "Three"
                       :id 3}
                      {:name "Four"
                       :id 4}]))

(defn node-component [node]
<<<<<<< HEAD
  [:div (:name @node)])

(defn home-page []
  [:div [:h2 "Nested-sortable"]
   ;[sortable-list node-component list-data]
   [:hr]
   [sortable-tree tree-data]
=======
  [:div (:name node)])

(defn home-page []
  [:div [:h2 "Nested-sortable"]
   ;; [tree data]
   [sortable-list node-component list-data]
>>>>>>> f57988d8b320f222cf2522d25800a404e222f72e
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About nested-sortable"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page home-page))

(secretary/defroute "/about" []
  (session/put! :current-page about-page))

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
<<<<<<< HEAD
=======

>>>>>>> f57988d8b320f222cf2522d25800a404e222f72e

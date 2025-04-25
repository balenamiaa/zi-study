(ns zi-study.frontend.core
  (:require [reagent.dom.client :as rdom]
            [reagent.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            ["lucide-react" :as lucide-icons]))

(defonce app-state (r/atom {:text "Hello, Reagent!"}))
(defonce router-match (r/atom nil))

(defn home-page []
  [:div
   [:h1 (:text @app-state)]
   [:button
    {:class "btn btn-secondary flex flex-row justify-center justify-end" :on-click #(swap! app-state assoc :text "Button clicked!")}
    [:> lucide-icons/WandSparkles {:color "red"}]
    [:span {:class "ml-2"} "Click me!"]]])


(defonce counter-count (r/atom 0))
(defn counter-page []
  [:div {:class "flex flex-col text-center justify-center"}
   [:h1 "Simple Counter"]
   [:p "Current count: " [:span {:class "text-blue-500"} @counter-count]]
   [:button
    {:class "btn btn-primary" :on-click #(swap! counter-count inc)}
    "Increment"] ;;
   ])

(defn demo-item-page [match]
  (let [{:keys [path query]} (:parameters match)
        {:keys [id]} path]
    [:div
     [:h1 (str "Item Id{" id "}, " "Query{" query "}")]
     [:p "This is a demo item page"]]))

(def routes
  [["/"
    {:name ::frontpage
     :view home-page}]

   ["/counter"
    {:name ::counter
     :view counter-page}]

   ["/item/:id"
    {:name ::item
     :view demo-item-page
     :parameters {:path {:id int?}
                  :query map?}}]])

(defn app []
  [:div
   [:ul
    [:li [:a {:href (rfe/href ::frontpage)} "Frontpage"]]
    [:li [:a {:href (rfe/href ::counter)} "Counter"]]
    [:li [:a {:href (rfe/href ::item {:id 1})} "Item 1"]]
    [:li [:a {:href (rfe/href ::item {:id 2} {:foo "bar"})} "Item 2"]]]
   (if @router-match
     (let [view (:view (:data @router-match))]
       [view @router-match])
     [:div "This should never be seen"])])

(defn init []
  (println "Initializing frontend...")
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m] (reset! router-match m))
   {:use-fragment false})
  (rdom/render (rdom/create-root (js/document.getElementById "app")) [app]))

(defn reload []
  (println "Reloading frontend...")
  (init))

(ns frontend.ui.card
  (:require
   [frontend.ui.core :as ui]
   [uix.core :as uix :refer [$ defui]]))

(defui Card
  "Card container with optional compact, hover and glass effects.
  Props: :compact? :hover? :glass? :class. Children are wrapped in .card-body"
  [{:keys [class compact? hover? glass? children] :as props}]
  (let [cls (ui/cx "card bg-base-200"
                   (when compact? "card-compact")
                   (when hover? "hover:shadow-xl")
                   (when glass? "glass-panel")
                   class)]
    ($ :div (-> props (dissoc :class :compact? :hover? :glass?) (assoc :class cls))
       ($ :div {:class "card-body"} children))))

(defui CardHeader
  "Card title area. Use inside Card."
  [{:keys [children]}]
  ($ :div {:class "card-title"} children))

(defui CardFooter
  "Card actions/footer area aligned to the right by default."
  [{:keys [children]}]
  ($ :div {:class "card-actions justify-end"} children))

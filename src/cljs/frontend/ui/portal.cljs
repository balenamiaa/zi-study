(ns frontend.ui.portal
  (:require
   ["react-dom" :refer [createPortal]]
   [uix.core :as uix :refer [$ defui]]))

(defui Portal
  "Create a detached DOM node and portal children into it.
  Props:
  - :target — optional mount element (defaults to document.body)
  - :children — rendered inside a z-layered container."
  [{:keys [target children]}]
  (let [[el set-el!] (uix/use-state nil)]
    (uix/use-effect
     (fn []
       (let [container (.createElement js/document "div")
             mount (or target (.-body js/document))]
         (.appendChild mount container)
         (set-el! container)
         (fn [] (.removeChild mount container))))
     [target])
    (when el
      (createPortal ($ :div {:class "z-[9999]"} children) el))))

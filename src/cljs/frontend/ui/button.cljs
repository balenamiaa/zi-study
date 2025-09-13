(ns frontend.ui.button
  (:require
   [frontend.ui.core :as ui]
   [uix.core :as uix :refer [$ defui]]))

(defui Button
  "Primary button component built on DaisyUI.
  Props:
  - :variant   — :primary (default) | :secondary | :accent | :ghost | :outline | :link | :neutral | :success | :warning | :error
  - :size      — :xs | :sm | :md (default) | :lg | :xl
  - :full?     — true for w-full
  - :icon      — optional lucide component to render left of label
  - :loading?  — shows loading state and disables button
  - :class     — extra classes
  Children: label/content inside the button."
  [{:keys [class variant size full? icon disabled loading? children] :as props}]
  (let [variant (or variant :primary)
        size (or size :md)
        cls (ui/cx "btn"
                   (ui/variants variant)
                   (ui/sizes size)
                   (when full? "w-full")
                   (when loading? "btn-loading")
                   class)
        props (-> props
                  (dissoc :class :variant :size :full? :icon :loading?)
                  (assoc :class cls
                         :disabled (or disabled loading?)))]
    ($ :button props
       (when icon ($ icon {:class "mr-2" :size 18}))
       children)))

(defui IconButton
  "Square icon button.
  Props:
  - :variant — :ghost (default) | any Button variant
  - :size    — :xs | :sm | :md (default) | :lg | :xl
  - :class   — extra classes
  Children: icon or content."
  [{:keys [class variant size disabled children] :as props}]
  (let [variant (or variant :ghost)
        size (or size :md)
        cls (ui/cx "btn btn-square"
                   (ui/variants variant)
                   (ui/sizes size)
                   class)]
    ($ :button (-> props (dissoc :class :variant :size) (assoc :class cls :disabled disabled))
       children)))

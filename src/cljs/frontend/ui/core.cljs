(ns frontend.ui.core
  (:require
   [clojure.string :as str]))

;; Utilities shared across components

(defn cx
  "Join class fragments into a single class string.
  - Accepts strings, keywords, nils, nested sequences.
  - Keywords are converted via name. Nils are ignored."
  [& xs]
  (->> xs
       flatten
       (remove nil?)
       (map #(if (keyword? %) (name %) %))
       (str/join " ")))

(def sizes
  "Button size class map."
  {:xs "btn-xs"
   :sm "btn-sm"
   :md ""
   :lg "btn-lg"
   :xl "btn-xl"})

(def variants
  "Button variant class map compatible with DaisyUI."
  {:primary "btn-primary"
   :secondary "btn-secondary"
   :accent "btn-accent"
   :ghost "btn-ghost"
   :outline "btn-outline"
   :link "btn-link"
   :neutral "btn-neutral"
   :success "btn-success"
   :warning "btn-warning"
   :error "btn-error"})

(def input-sizes
  "Input size class map."
  {:sm "input-sm"
   :md ""
   :lg "input-lg"})

(defn full-class
  "Return w-full when full? is truthy."
  [full?]
  (when full? "w-full"))

(def field-base "input input-bordered")
(def select-base "select select-bordered")
(def textarea-base "textarea textarea-bordered")

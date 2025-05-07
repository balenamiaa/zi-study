(ns zi-study.frontend.utilities
  (:require ["clsx" :as clsx]
            ["tailwind-merge" :as tw]))

(defn add-ripple-effect [e]
  (let [button (.-currentTarget e)
        rect (.getBoundingClientRect button)
        x (- (.-clientX e) (.-left rect))
        y (- (.-clientY e) (.-top rect))
        ripple (.createElement js/document "span")]

    ;; Set ripple style
    (set! (.. ripple -style -width) "300px")
    (set! (.. ripple -style -height) "300px")
    (set! (.. ripple -style -left) (str (- x 150) "px"))
    (set! (.. ripple -style -top) (str (- y 150) "px"))
    (set! (.. ripple -className) "ripple")

    ;; Add ripple to button
    (.appendChild button ripple)

    ;; Remove ripple after animation
    (js/setTimeout #(.remove ripple) 600)))

(defn cx
  "A ClojureScript wrapper for the clsx and tailwind-merge libraries that intelligently handles Clojure data structures.
   
   Arguments can be:
   - strings: included directly
   - keywords: converted to strings
   - maps: keys are included if values are truthy
   - collections: flattened and processed recursively
   - nil: ignored
   
   Examples:
   (cx \"btn\" \"btn-primary\") => \"btn btn-primary\"
   (cx :btn :btn-primary) => \"btn btn-primary\"
   (cx \"btn\" {:primary true :disabled false}) => \"btn primary\"
   (cx \"btn\" [:primary {:disabled false :active true}]) => \"btn primary active\"
   (cx nil \"btn\" false) => \"btn\"
   (cx \"text-red-500\" \"text-blue-500\") => \"text-blue-500\" (conflicting classes merged)
   
   Returns a string of space-separated class names with Tailwind conflicts resolved."
  [& args]
  (letfn [(process-arg [arg]
            (cond
              (nil? arg) nil
              (string? arg) arg
              (keyword? arg) (name arg)
              (map? arg) (clj->js (reduce-kv (fn [result k v]
                                               (if v
                                                 (assoc result (process-arg k) v)
                                                 result))
                                             {} arg))
              (coll? arg) (clj->js (map process-arg (remove nil? arg)))
              :else (str arg)))]
    (tw/twMerge (apply clsx (remove nil? (map process-arg args))))))


(prn (cx "btn" {:primary true :disabled false} "text-red-500" "text-blue-500"))
(ns frontend.ui.avatar
  (:require
   [clojure.string :as str]
   [frontend.ui.core :as ui]
   [uix.core :as uix :refer [$ defui]]))

(defn- str-hash [^string s]
  (reduce (fn [acc ch]
            (let [acc (bit-or 0 acc)
                  acc (+ (bit-shift-left acc 5) acc (int ch))]
              (bit-and acc 0x7fffffff)))
          0 s))

(defn- name->colors [name]
  (let [base (str-hash (or (not-empty name) "?"))
        h1 (mod base 360)
        h2 (mod (+ h1 47) 360)
        c1 (str "hsl(" h1 ", 70%, 55%)")
        c2 (str "hsl(" h2 ", 70%, 55%)")
        ring1 (str "hsl(" h1 ", 75%, 70%)")
        ring2 (str "hsl(" h2 ", 75%, 70%)")]
    {:c1 c1 :c2 c2 :ring1 ring1 :ring2 ring2}))

(defui Avatar
  "Avatar that renders either an image or a generated gradient + initials.
  Props:
  - :src    — optional image URL
  - :name   — user display name (used for initials + palette)
  - :email  — fallback to derive initials/palette
  - :size   — :xs|:sm|:md|:lg|:xl (controls dimension + font size)
  - :class  — outer wrapper classes"
  [{:keys [src name email size class]}]
  (let [size (or size :md)
        dim (case size :xs 24 :sm 32 :md 40 :lg 56 :xl 72)
        src-name (not-empty (some-> name str/trim))
        nm-tokens (when src-name (re-seq #"\p{L}+" src-name))
        from-email (when (not src-name)
                     (when-let [local (some-> email (str/split #"@")[0])]
                       (let [parts (re-seq #"[A-Za-z0-9]+" local)]
                         (when (seq parts)
                           (->> parts (map #(subs % 0 1)) (take 2) (apply str))))))
        initials (-> (or (when (seq nm-tokens)
                           (->> nm-tokens (map #(subs % 0 1)) (take 2) (apply str)))
                         from-email
                         (some-> src-name (subs 0 (min 1 (count src-name))))
                         (some-> email (subs 0 1))
                         "?")
                      str/upper-case)
        seed (or src-name email "?")
        {:keys [c1 c2 ring1 ring2]} (name->colors seed)]
    ($ :div {:class (ui/cx "avatar" class)}
       ($ :div {:class "rounded-full relative overflow-hidden shadow-md"
                :style {:width (str dim "px") :height (str dim "px")}}
          (if (seq src)
            ($ :img {:src src :alt (or name "Avatar")})
            ($ :div {:class "w-full h-full relative grid place-items-center text-white font-semibold"
                     :style {:background (str "radial-gradient( circle at 30% 20%, rgba(255,255,255,0.18), transparent 35%),"
                                              "conic-gradient(from 0deg, " c1 ", " c2 " 60%, " c1 " 100%)")}}
               ;; soft overlay for depth
               ($ :div {:class "absolute inset-0 mix-blend-soft-light"
                        :style {:background "radial-gradient( circle at 70% 30%, rgba(255,255,255,0.25), transparent 40%)"}})
               ;; subtle ring glow
               ($ :div {:class "absolute -inset-0.5 rounded-full blur-sm opacity-60 pointer-events-none"
                        :style {:background (str "conic-gradient(" ring1 ", " ring2 ")")}})
               ;; initials
               ($ :span {:class (str (case size :xs "text-[10px]" :sm "text-xs" :md "text-sm" :lg "text-base" :xl "text-lg")
                                     " drop-shadow-[0_1px_2px_rgba(0,0,0,0.45)] select-none")}
                  initials)))))))

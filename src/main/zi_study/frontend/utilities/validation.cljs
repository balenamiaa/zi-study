(ns zi-study.frontend.utilities.validation
  (:require [clojure.string :as str]))

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")
(def password-regex #"^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$")

(defn email-valid?
  "Validates if string is a proper email format"
  [email]
  (boolean (re-matches email-regex email)))

(defn password-valid?
  "Validates if password meets minimum requirements
   (at least 8 chars, 1 letter and 1 number)"
  [password]
  (boolean (re-matches password-regex password)))

(defn required?
  "Validates if value is not empty"
  [value]
  (if (string? value)
    (not (str/blank? value))
    (not (nil? value))))

(defn min-length?
  "Validates if string meets minimum length"
  [value min-length]
  (and (string? value)
       (>= (count value) min-length)))

(defn max-length?
  "Validates if string is within maximum length"
  [value max-length]
  (and (string? value)
       (<= (count value) max-length)))

(defn length-in-range?
  "Validates if string length is within range (inclusive)"
  [value min-length max-length]
  (and (min-length? value min-length)
       (max-length? value max-length)))

(defn matches?
  "Validates if two values match"
  [value1 value2]
  (= value1 value2))

(defn numeric?
  "Validates if string contains only numbers"
  [value]
  (boolean (re-matches #"^\d+$" (str value))))

(defn alpha?
  "Validates if string contains only letters"
  [value]
  (boolean (re-matches #"^[a-zA-Z]+$" (str value))))

(defn alphanumeric?
  "Validates if string contains only letters and numbers"
  [value]
  (boolean (re-matches #"^[a-zA-Z0-9]+$" (str value))))

(defn url?
  "Validates if string is a valid URL"
  [value]
  (try
    (let [url (js/URL. (str value))]
      (boolean (and (.-host url) (.-protocol url))))
    (catch :default _
      false)))

(defn validate-form
  "Validates a map of values against validation rules
   Returns a map with :valid? and :errors keys
   
   Example usage:
   (validate-form 
     {:email \"user@example.com\" :password \"abc123\"}
     {:email [[required? [] \"Email is required\"]
              [email-valid? [] \"Invalid email format\"]]
      :password [[required? [] \"Password is required\"]
                 [min-length? [8] \"Password must be at least 8 characters\"]]})"
  [values rules]
  (let [errors (reduce-kv (fn [acc field rule-set]
                            (let [value (get values field)
                                  field-errors (->> rule-set
                                                    (map (fn [[rule-fn rule-args error-msg]]
                                                           (if (apply rule-fn value rule-args)
                                                             nil
                                                             error-msg)))
                                                    (remove nil?))]
                              (if (seq field-errors)
                                (assoc acc field field-errors)
                                acc)))
                          {}
                          rules)]
    {:valid? (empty? errors)
     :errors errors}))

(defn calculate-password-strength
  "Calculate password strength on a scale of 0-4
   0: Very weak, 1: Weak, 2: Medium, 3: Strong, 4: Very strong"
  [password]
  (if (or (nil? password) (empty? password))
    0
    (let [has-lowercase (re-find #"[a-z]" password)
          has-uppercase (re-find #"[A-Z]" password)
          has-digit (re-find #"[0-9]" password)
          has-special (re-find #"[^a-zA-Z0-9]" password)
          is-long-enough (>= (count password) 8)

          score (+ (if has-lowercase 1 0)
                   (if has-uppercase 1 0)
                   (if has-digit 1 0)
                   (if has-special 1 0)
                   (if is-long-enough 1 0))]

      (cond
        (<= score 1) 0  ;; Very weak
        (= score 2) 1   ;; Weak
        (= score 3) 2   ;; Medium
        (= score 4) 3   ;; Strong
        :else 4))))    ;; Very strong
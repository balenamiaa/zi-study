(ns zi-study.shared.question-schemas)

;;;-----------------------------------------------------------------------------------------------------------------------
;;; Utility Schemas (used by both import and processed schemas)
;;;-----------------------------------------------------------------------------------------------------------------------

(def NonBlankString [:string {:min 1 :error/message "Must be a non-blank string"}])
(def Difficulty [:int {:min 1 :max 5 :error/message "Difficulty must be an integer between 1 and 5"}])
(def TempId [:or :string :keyword]) ; Used during import phase

(def QuestionTypeEnum
  [:enum "written" "mcq-single" "mcq-multi" "emq" "cloze" "true-false"])

;;;-----------------------------------------------------------------------------------------------------------------------
;;; Processed Question Content Schemas
;;; These represent the content part of a question after import and processing.
;;; This is what's typically stored in the DB (e.g., in a JSONB column) and used by the frontend.
;;;-----------------------------------------------------------------------------------------------------------------------

(def McqQuestionContent
  "Schema for the content of Multiple Choice Questions (single and multi-answer)."
  [:map
   [:question-text NonBlankString]
   [:options [:vector {:min 1 :error/message "Must provide at least one option"} NonBlankString]]
   [:correct-index {:optional true} [:int {:error/message "Correct index must be an integer for single-answer MCQs"}]]
   [:correct-indices {:optional true} [:vector {:min 1 :error/message "Must provide at least one correct index for multi-answer MCQs"} [:int {:error/message "Correct indices must be integers"}]]]
   [:explanation {:optional true} [:maybe :string]]])

(def WrittenQuestionContent
  "Schema for the content of Written Answer Questions."
  [:map
   [:question-text NonBlankString]
   [:correct-answer {:optional true} [:maybe NonBlankString]]
   [:explanation {:optional true} [:maybe :string]]])

(def TrueFalseQuestionContent
  "Schema for the content of True/False Questions."
  [:map
   [:question-text NonBlankString]
   [:is-correct-true [:boolean {:error/message "is-correct-true field must be a boolean"}]]
   [:explanation {:optional true} [:maybe :string]]])

(def ClozeQuestionContent
  "Schema for the content of Cloze (Fill in the Blanks) Questions."
  [:map
   [:cloze-text NonBlankString]
   [:answers [:vector {:min 1 :error/message "Must provide at least one answer for cloze"} NonBlankString]]
   [:explanation {:optional true} [:maybe :string]]])

(def EmqQuestionContent
  "Schema for the content of Extended Matching Questions."
  [:map
   [:instructions {:optional true} [:maybe NonBlankString]]
   [:premises [:vector {:min 1 :error/message "Must provide at least one premise for EMQ"} NonBlankString]]
   [:options [:vector {:min 1 :error/message "Must provide at least one option for EMQ"} NonBlankString]]
   [:matches {:optional true} [:vector {:min 1 :error/message "Must provide at least one match for EMQ"} [:tuple :int :int]]]
   [:explanation {:optional true} [:maybe :string]]])

;; Multi-schema for dispatching processed question content.
;; Expects :question-type to be a key within the content map itself.
;; If :question-type is a sibling, the consumer must select the schema directly.
(def ProcessedQuestionContent
  [:multi {:dispatch (fn [val _] (if (keyword? (:question-type val)) (:question-type val) (keyword (:question-type val))))}
   [:mcq-single McqQuestionContent]
   [:mcq-multi McqQuestionContent]
   [:written WrittenQuestionContent]
   [:true-false TrueFalseQuestionContent]
   [:cloze ClozeQuestionContent]
   [:emq EmqQuestionContent]])

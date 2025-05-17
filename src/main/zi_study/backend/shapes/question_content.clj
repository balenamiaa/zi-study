(ns zi-study.backend.shapes.question-content
  (:require [malli.core :as m]))

;; These schemas define the structure of the EDN data stored in the
;; `question_data` column in the `questions` table.
;; All keys are kebab-case.

(def McqQuestionContent
  "Schema for the content of Multiple Choice Questions (single and multi-answer)."
  [:map
   [:text :string {:error/message "Question text must be a string"}]
   [:options [:vector :string] {:error/message "Options must be a vector of strings"}]
   [:correct-index {:optional true} :int {:error/message "Correct index must be an integer for single-answer MCQs"}]
   [:correct-indices {:optional true} [:vector :int] {:error/message "Correct indices must be a vector of integers for multi-answer MCQs"}]
   [:explanation {:optional true} [:maybe :string]]])

(def WrittenQuestionContent
  "Schema for the content of Written Answer Questions."
  [:map
   [:text :string {:error/message "Question text must be a string"}]
   ;; The actual correct answer text might be part of the question data for reference,
   ;; or it might be omitted if it's only for manual review.
   [:correct-answer {:optional true} [:maybe :string]]
   [:explanation {:optional true} [:maybe :string]]])

(def TrueFalseQuestionContent
  "Schema for the content of True/False Questions."
  [:map
   [:text :string {:error/message "Question text must be a string"}]
   [:is-correct-true :boolean {:error/message "is-correct-true field must be a boolean"}]
   [:explanation {:optional true} [:maybe :string]]])

(def ClozeQuestionContent
  "Schema for the content of Cloze (Fill in the Blanks) Questions."
  [:map
   [:cloze-text :string {:error/message "Cloze text must be a string (e.g., \"Hello {{c1::World}}.\")"}]
   [:answers [:vector :string] {:error/message "Answers must be a vector of strings"}]
   [:explanation {:optional true} [:maybe :string]]])

(def EmqQuestionContent
  "Schema for the content of Extended Matching Questions."
  [:map
   [:instructions {:optional true} [:maybe :string]]
   [:premises [:vector :string] {:error/message "Premises must be a vector of strings"}]
   [:options [:vector :string] {:error/message "Options must be a vector of strings"}]
   ;; Matches are stored as [premise-index option-index]
   [:matches [:vector [:tuple :int :int]] {:error/message "Matches must be a vector of [premise-index, option-index] tuples"}]
   [:explanation {:optional true} [:maybe :string]]])

;; Note: A top-level dispatching schema for `QuestionContentData` (like the one in state.cljs)
;; could be defined here too if needed for backend validation at a higher level.
;; For now, these individual schemas document the structure for each type.

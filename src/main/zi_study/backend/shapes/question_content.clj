(ns zi-study.backend.shapes.question-content)

(def McqQuestionContent
  "Schema for the content of Multiple Choice Questions (single and multi-answer)."
  [:map
   [:question-text {:error/message "Question text must be a string"} :string]
   [:options {:error/message "Options must be a vector of strings"} [:vector :string]]
   [:correct-index {:error/message "Correct index must be an integer for single-answer MCQs" :optional true} :int]
   [:correct-indices {:error/message "Correct indices must be a vector of integers for multi-answer MCQs" :optional true} [:vector :int]]
   [:explanation {:optional true} [:maybe :string]]])

(def WrittenQuestionContent
  "Schema for the content of Written Answer Questions."
  [:map
   [:question-text {:error/message "Question text must be a string"} :string]
   [:correct-answer {:optional true} [:maybe :string]]
   [:explanation {:optional true} [:maybe :string]]])

(def TrueFalseQuestionContent
  "Schema for the content of True/False Questions."
  [:map
   [:question-text {:error/message "Question text must be a string"} :string]
   [:is-correct-true {:error/message "is-correct-true field must be a boolean"} :boolean]
   [:explanation {:optional true} [:maybe :string]]])

(def ClozeQuestionContent
  "Schema for the content of Cloze (Fill in the Blanks) Questions."
  [:map
   [:cloze-text {:error/message "Cloze text must be a string (e.g., \"Hello {{c1::World}}.\")"} :string]
   [:answers {:error/message "Answers must be a vector of strings"} [:vector :string]]
   [:explanation {:optional true} [:maybe :string]]])

(def EmqQuestionContent
  "Schema for the content of Extended Matching Questions."
  [:map
   [:instructions {:optional true} [:maybe :string]]
   [:premises {:error/message "Premises must be a vector of strings"} [:vector :string]]
   [:options {:error/message "Options must be a vector of strings"} [:vector :string]]
   ;; Matches are stored as [premise-index option-index]
   [:matches {:error/message "Matches must be a vector of [premise-index, option-index] tuples"} [:vector [:tuple :int :int]]]
   [:explanation {:optional true} [:maybe :string]]])

;; Note: A top-level dispatching schema for `QuestionContentData` (like the one in state.cljs)
;; could be defined here too if needed for backend validation at a higher level.
;; For now, these individual schemas document the structure for each type.

(ns zi-study.shared.question-import-schemas
  (:require [zi-study.shared.question-schemas :as shared]))


(def TempId [:or {:error/message "Temp ID must be a string or keyword"} :string :keyword]) ; Used during import phase

(def McqOptionImport
  [:map
   [:temp-id {:error/message "MCQ option temp ID is required"} TempId]
   [:text shared/NonBlankString]])

(def McqSingleQuestionImport
  [:map {:closed true :error/message "Invalid MCQ single-answer question structure"}
   [:temp-id {:error/message "Question temp ID is required"} TempId]
   [:type {:error/message "Question type is required"} shared/QuestionTypeEnum]
   [:difficulty {:optional true} shared/Difficulty]
   [:question-text shared/NonBlankString]
   [:retention-aid {:optional true :error/message "Retention aid must be a string if provided"} :string]
   [:options [:vector {:min 1 :error/message "At least one MCQ option is required"} McqOptionImport]]
   [:correct-option-temp-id {:error/message "Correct option temp ID is required for single-answer MCQ"} TempId]
   [:explanation {:optional true :error/message "Explanation must be a string if provided"} :string]])

(def McqMultiQuestionImport
  [:map {:closed true :error/message "Invalid MCQ multi-answer question structure"}
   [:temp-id {:error/message "Question temp ID is required"} TempId]
   [:type {:error/message "Question type is required"} shared/QuestionTypeEnum]
   [:difficulty {:optional true} shared/Difficulty]
   [:question-text shared/NonBlankString]
   [:retention-aid {:optional true :error/message "Retention aid must be a string if provided"} :string]
   [:options [:vector {:min 1 :error/message "At least one MCQ option is required"} McqOptionImport]]
   [:correct-option-temp-ids [:vector {:min 1 :error/message "At least one correct option temp ID is required for multi-answer MCQ"} TempId]]
   [:explanation {:optional true :error/message "Explanation must be a string if provided"} :string]])

(def WrittenQuestionImport
  [:map {:closed true :error/message "Invalid written question structure"}
   [:temp-id {:error/message "Question temp ID is required"} TempId]
   [:type {:error/message "Question type is required"} shared/QuestionTypeEnum]
   [:difficulty {:optional true} shared/Difficulty]
   [:question-text shared/NonBlankString]
   [:retention-aid {:optional true :error/message "Retention aid must be a string if provided"} :string]
   [:correct-answer-text shared/NonBlankString]
   [:explanation {:optional true :error/message "Explanation must be a string if provided"} :string]])

(def TrueFalseQuestionImport
  [:map {:closed true :error/message "Invalid true/false question structure"}
   [:temp-id {:error/message "Question temp ID is required"} TempId]
   [:type {:error/message "Question type is required"} shared/QuestionTypeEnum]
   [:difficulty {:optional true} shared/Difficulty]
   [:question-text shared/NonBlankString]
   [:retention-aid {:optional true :error/message "Retention aid must be a string if provided"} :string]
   [:is-correct-true [:boolean {:error/message "is-correct-true must be a boolean for true/false questions"}]]
   [:explanation {:optional true :error/message "Explanation must be a string if provided"} :string]])

(def ClozeQuestionImport
  [:map {:closed true :error/message "Invalid cloze question structure"}
   [:temp-id {:error/message "Question temp ID is required"} TempId]
   [:type {:error/message "Question type is required"} shared/QuestionTypeEnum]
   [:difficulty {:optional true} shared/Difficulty]
   [:cloze-text shared/NonBlankString]
   [:retention-aid {:optional true :error/message "Retention aid must be a string if provided"} :string]
   [:answers [:vector {:min 1 :error/message "At least one answer is required for cloze questions"} shared/NonBlankString]]
   [:explanation {:optional true :error/message "Explanation must be a string if provided"} :string]])

(def EmqPremiseImport
  [:map
   [:temp-id {:error/message "EMQ premise temp ID is required"} TempId]
   [:text shared/NonBlankString]])

(def EmqOptionImport
  [:map
   [:temp-id {:error/message "EMQ option temp ID is required"} TempId]
   [:text shared/NonBlankString]])

(def EmqMatchImport [:tuple {:error/message "EMQ match must be a pair of temp IDs"} TempId TempId])

(def EmqQuestionImport
  [:map {:closed true :error/message "Invalid EMQ question structure"}
   [:temp-id {:error/message "Question temp ID is required"} TempId]
   [:type {:error/message "Question type is required"} shared/QuestionTypeEnum]
   [:difficulty {:optional true} shared/Difficulty]
   [:instructions {:optional true :error/message "Instructions must be a string if provided"} shared/NonBlankString]
   [:retention-aid {:optional true :error/message "Retention aid must be a string if provided"} :string]
   [:premises [:vector {:min 1 :error/message "At least one EMQ premise is required"} EmqPremiseImport]]
   [:options [:vector {:min 1 :error/message "At least one EMQ option is required"} EmqOptionImport]]
   [:matches [:vector {:min 1 :error/message "At least one EMQ match is required"} EmqMatchImport]]
   [:explanation {:optional true :error/message "Explanation must be a string if provided"} :string]])

(def QuestionImport
  [:multi {:dispatch (fn [val] (let [type-val (:type val)] (if (keyword? type-val) type-val (keyword type-val))))
           :error/message "Invalid question type in import data"}
   [:mcq-single McqSingleQuestionImport]
   [:mcq-multi McqMultiQuestionImport]
   [:written WrittenQuestionImport]
   [:true-false TrueFalseQuestionImport]
   [:cloze ClozeQuestionImport]
   [:emq EmqQuestionImport]])

(def QuestionSetImport
  [:map {:closed true :error/message "Invalid question set structure for import"}
   [:title shared/NonBlankString]
   [:description {:optional true :error/message "Description must be a string if provided"} :string]
   [:tags {:optional true} [:vector {:error/message "Tags must be a list of non-blank strings"} shared/NonBlankString]]
   [:questions [:vector {:min 1 :error/message "A question set must contain at least one question"} QuestionImport]]])

(def ImportDataSchema [:vector {:min 1 :error/message "Import data must contain at least one question set"} QuestionSetImport]) 
(ns zi-study.backend.shapes.questions)


(def NonBlankString [:string {:min 1}])
(def OptionalString [:maybe :string])
(def Difficulty [:int {:min 1 :max 5}])
(def QuestionType [:enum "written" "mcq-single" "mcq-multi" "emq" "cloze" "true-false"])
(def TempId [:or :string :keyword])

(def McqOption
  [:map
   [:temp_id TempId]
   [:text NonBlankString]])

(def McqSingleQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :mcq-single "mcq-single"]] ; Allow keyword or string
   [:difficulty {:optional true} Difficulty]
   [:question_text NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:options [:vector {:min 1} McqOption]]
   [:correct_option_temp_id TempId]
   [:explanation {:optional true} OptionalString]]) ; Optional explanation field

(def McqMultiQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :mcq-multi "mcq-multi"]]
   [:difficulty {:optional true} Difficulty]
   [:question_text NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:options [:vector {:min 1} McqOption]]
   [:correct_option_temp_ids [:vector {:min 1} TempId]]
   [:explanation {:optional true} OptionalString]])

(def WrittenQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :written "written"]]
   [:difficulty {:optional true} Difficulty]
   [:question_text NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:correct_answer_text NonBlankString]
   [:explanation {:optional true} OptionalString]])

(def TrueFalseQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :true-false "true-false"]]
   [:difficulty {:optional true} Difficulty]
   [:question_text NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:is_correct_answer_true :boolean]
   [:explanation {:optional true} OptionalString]])

(def ClozeQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :cloze "cloze"]]
   [:difficulty {:optional true} Difficulty]
   [:cloze_text NonBlankString] ; Contains {{c1::hint}} or {{c1}} placeholders
   [:retention_aid {:optional true} OptionalString]
   [:answers [:vector {:min 1} NonBlankString]] ; Answers in order of placeholders
   [:explanation {:optional true} OptionalString]])

(def EmqPremise
  [:map
   [:temp_id TempId]
   [:text NonBlankString]])

(def EmqOption
  [:map
   [:temp_id TempId]
   [:text NonBlankString]])

(def EmqMatch [:tuple TempId TempId]) ; [premise_temp_id, option_temp_id]

(def EmqQuestion
  [:map {:closed true}
   [:temp_id TempId]
   [:type [:enum :emq "emq"]]
   [:difficulty {:optional true} Difficulty]
   [:instructions {:optional true} NonBlankString]
   [:retention_aid {:optional true} OptionalString]
   [:premises [:vector {:min 1} EmqPremise]]
   [:options [:vector {:min 1} EmqOption]]
   [:matches [:vector {:min 1} EmqMatch]]
   [:explanation {:optional true} OptionalString]])

(def Question
  [:multi {:dispatch :type}
   [:mcq-single McqSingleQuestion]
   ["mcq-single" McqSingleQuestion]
   [:mcq-multi McqMultiQuestion]
   ["mcq-multi" McqMultiQuestion]
   [:written WrittenQuestion]
   ["written" WrittenQuestion]
   [:true-false TrueFalseQuestion]
   ["true-false" TrueFalseQuestion]
   [:cloze ClozeQuestion]
   ["cloze" ClozeQuestion]
   [:emq EmqQuestion]
   ["emq" EmqQuestion]])

(def QuestionSet
  [:map {:closed true}
   [:title NonBlankString]
   [:description {:optional true} OptionalString]
   [:tags {:optional true} [:vector NonBlankString]]
   [:questions [:vector {:min 1} Question]]])

(def ImportData [:vector {:min 1} QuestionSet])
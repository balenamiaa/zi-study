(ns zi-study.backend.shapes.questions)


(def NonBlankString [:string {:min 1}])
(def Difficulty [:int {:min 1 :max 5}])
(def QuestionType [:enum "written" "mcq-single" "mcq-multi" "emq" "cloze" "true-false"])
(def TempId [:or :string :keyword])

(def McqOption
  [:map
   [:temp-id TempId]
   [:text NonBlankString]])

(def McqSingleQuestion
  [:map {:closed true}
   [:temp-id TempId]
   [:type [:enum :mcq-single "mcq-single"]] ; Allow keyword or string
   [:difficulty {:optional true} Difficulty]
   [:question-text NonBlankString]
   [:retention-aid {:optional true} :string]
   [:options [:vector {:min 1} McqOption]]
   [:correct-option-temp-id TempId]
   [:explanation {:optional true} :string]]) ; Optional explanation field

(def McqMultiQuestion
  [:map {:closed true}
   [:temp-id TempId]
   [:type [:enum :mcq-multi "mcq-multi"]]
   [:difficulty {:optional true} Difficulty]
   [:question-text NonBlankString]
   [:retention-aid {:optional true} :string]
   [:options [:vector {:min 1} McqOption]]
   [:correct-option-temp-ids [:vector {:min 1} TempId]]
   [:explanation {:optional true} :string]])

(def WrittenQuestion
  [:map {:closed true}
   [:temp-id TempId]
   [:type [:enum :written "written"]]
   [:difficulty {:optional true} Difficulty]
   [:question-text NonBlankString]
   [:retention-aid {:optional true} :string]
   [:correct-answer-text NonBlankString]
   [:explanation {:optional true} :string]])

(def TrueFalseQuestion
  [:map {:closed true}
   [:temp-id TempId]
   [:type [:enum :true-false "true-false"]]
   [:difficulty {:optional true} Difficulty]
   [:question-text NonBlankString]
   [:retention-aid {:optional true} :string]
   [:is-correct-true :boolean]
   [:explanation {:optional true} :string]])

(def ClozeQuestion
  [:map {:closed true}
   [:temp-id TempId]
   [:type [:enum :cloze "cloze"]]
   [:difficulty {:optional true} Difficulty]
   [:cloze-text NonBlankString] ; Contains {{c1::hint}} or {{c1}} placeholders
   [:retention-aid {:optional true} :string]
   [:answers [:vector {:min 1} NonBlankString]] ; Answers in order of placeholders
   [:explanation {:optional true} :string]])

(def EmqPremise
  [:map
   [:temp-id TempId]
   [:text NonBlankString]])

(def EmqOption
  [:map
   [:temp-id TempId]
   [:text NonBlankString]])

(def EmqMatch [:tuple TempId TempId]) ; [premise-tempid, option-tempid]

(def EmqQuestion
  [:map {:closed true}
   [:temp-id TempId]
   [:type [:enum :emq "emq"]]
   [:difficulty {:optional true} Difficulty]
   [:instructions {:optional true} NonBlankString]
   [:retention-aid {:optional true} :string]
   [:premises [:vector {:min 1} EmqPremise]]
   [:options [:vector {:min 1} EmqOption]]
   [:matches [:vector {:min 1} EmqMatch]]
   [:explanation {:optional true} :string]])

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
   [:description {:optional true} :string]
   [:tags {:optional true} [:vector NonBlankString]]
   [:questions [:vector {:min 1} Question]]])

(def ImportData [:vector {:min 1} QuestionSet])
 (ns common.schema-test
   (:require [clojure.test :refer [deftest is testing]]
             [common.schema :as schema]
             [malli.core :as m]))

 (deftest user-public-valid
   (testing "user-public accepts avatar-url and has-password?"
     (let [u {:id 1 :email "a@b.com" :name "A"
              :provider "google" :provider-id "sub"
              :avatar-url "/uploads/avatars/x.png"
              :has-password? false
              :created-at "2025-01-01T00:00:00Z"}]
       (is (true? (m/validate schema/user-public u))))))


(ns backend.api.auth-test
  (:require [backend.api.auth :as auth]
            [clojure.test :refer [deftest is testing]]))

(deftest me-includes-has-password
  (testing "me includes has-password? true when password-hash present"
    (let [req {:session {:user {:id nil :email "e@x" :password-hash "hash"}}}
          resp (auth/me req)]
      (is (= 200 (:status resp)))
      (is (= true (-> resp :body :has-password?)))))
  (testing "me includes has-password? false when no password"
    (let [req {:session {:user {:id nil :email "e@x"}}}
          resp (auth/me req)]
      (is (= 200 (:status resp)))
      (is (= false (-> resp :body :has-password?))))))

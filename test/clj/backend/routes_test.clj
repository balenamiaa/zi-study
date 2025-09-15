 (ns backend.routes-test
  (:require [backend.routes :as routes]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]))

(defn- handler []
  (routes/app {:db nil
               :env {:session {:secret (apply str (repeat 32 "a"))}}}))

(deftest csrf-token-endpoint
  (testing "/api/auth/csrf returns token"
    (let [h (handler)
          req (-> (mock/request :get "/api/auth/csrf")
                  (mock/header "accept" "application/json"))
          resp (h req)
          body (json/parse-string (slurp (:body resp)) true)]
      (is (= 200 (:status resp)))
      (is (string? (:token body)))
      (is (pos? (count (:token body)))))))


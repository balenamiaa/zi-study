 (ns backend.e2e.todos-e2e-test
  (:require [backend.e2e.support :as sup]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]))

(deftest todos-crud
  (let [ds (sup/h2-datasource)
        _ (sup/setup-schema! ds)
        h (sup/handler ds)]
    (testing "create todo"
      (let [req (-> (mock/request :post "/api/todo" (sup/json-body {:text "first"}))
                    (mock/header "content-type" "application/json")
                    (mock/header "accept" "application/json")
                    sup/with-csrf)
            resp (h req)]
        (is (= 200 (:status resp)))))

    (testing "list includes created"
      (let [resp (h (-> (mock/request :get "/api/todo")
                        (mock/header "accept" "application/json")))
            body (json/parse-string (slurp (:body resp)) true)]
        (is (= 200 (:status resp)))
        (is (seq body))
        (is (= "first" (-> body first :text)))))

    (testing "update status to resolved via Transit"
      (let [todos (-> (h (mock/request :get "/api/todo")) :body slurp (json/parse-string true))
            id (-> todos first :id)
            body (sup/transit-json {:status :resolved})
            req (-> (mock/request :put (str "/api/todo/" id) body)
                    (mock/header "content-type" "application/transit+json")
                    (mock/header "accept" "application/transit+json")
                    sup/with-csrf)
            resp (h req)]
        (is (= 200 (:status resp)))))

    (testing "status is resolved in listing"
      (let [resp (h (-> (mock/request :get "/api/todo")
                        (mock/header "accept" "application/json")))
            body (json/parse-string (slurp (:body resp)) true)]
        (is (= 200 (:status resp)))
        (is (= "resolved" (-> body first :status))))

      (testing "delete todo"
        (let [todos (-> (h (mock/request :get "/api/todo")) :body slurp (json/parse-string true))
              id (-> todos first :id)
              req (-> (mock/request :delete (str "/api/todo/" id))
                      sup/with-csrf)
              resp (h req)]
          (is (= 200 (:status resp))))
        (let [resp (h (mock/request :get "/api/todo"))
              body (json/parse-string (slurp (:body resp)) true)]
          (is (empty? body)))))))


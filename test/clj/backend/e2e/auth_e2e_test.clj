(ns backend.e2e.auth-e2e-test
  (:require [backend.e2e.support :as sup]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock])
  (:import (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io File)))

(deftest auth-flow
  (let [ds (sup/h2-datasource)
        _ (sup/setup-schema! ds)
        h (sup/handler ds)]
    (testing "unauthenticated me"
      (let [resp (h (mock/request :get "/api/auth/me"))]
        (is (= 401 (:status resp)))))

    (testing "register -> login -> me -> logout"
      ;; register
      (let [req (-> (mock/request :post "/api/auth/register" (sup/json-body {:email "a@b.com" :password "password1" :name "A"}))
                    (mock/header "content-type" "application/json")
                    (mock/header "accept" "application/json")
                    sup/with-csrf)
            resp (h req)
            body (json/parse-string (slurp (:body resp)) true)
            cookie (sup/cookie-from resp)]
        (is (= 200 (:status resp)))
        (is (= "a@b.com" (:email body)))

        ;; me with cookie
        (let [resp2 (h (-> (sup/request-with-cookie :get "/api/auth/me" cookie)
                           (mock/header "accept" "application/json")))
              body2 (json/parse-string (slurp (:body resp2)) true)]
          (is (= 200 (:status resp2)))
          (is (= "a@b.com" (:email body2))))

        ;; logout with cookie
        (let [resp3 (h (-> (sup/request-with-cookie :post "/api/auth/logout" cookie)
                           sup/with-csrf))]
          (is (= 200 (:status resp3))))

        ;; me after logout should be 401
        (let [resp4 (h (-> (sup/request-with-cookie :get "/api/auth/me" cookie)
                           (mock/header "accept" "application/json")))]
          (is (= 401 (:status resp4))))))

    (testing "password change and avatar upload"
      (let [ds (sup/h2-datasource)
            _ (sup/setup-schema! ds)
            h (sup/handler ds)
            reg (h (-> (mock/request :post "/api/auth/register" (sup/json-body {:email "z@z" :password "oldpass" :name "Z"}))
                       (mock/header "content-type" "application/json")
                       (mock/header "accept" "application/json")
                       sup/with-csrf))
            cookie (sup/cookie-from reg)]
        (is (= 200 (:status reg)))
        ;; change password
        (let [body (sup/transit-json {:current "oldpass" :new "newpass" :confirm "newpass"})
              resp (h (-> (sup/request-with-cookie :post "/api/user/password" cookie)
                          (mock/header "content-type" "application/transit+json")
                          (mock/header "accept" "application/transit+json")
                          sup/with-csrf
                          (assoc :body body)))]
          (is (= 200 (:status resp))))
        ;; logout
        (is (= 200 (:status (h (-> (sup/request-with-cookie :post "/api/auth/logout" cookie)
                                   sup/with-csrf)))))
        ;; login old fails
        (is (= 401 (:status (h (-> (mock/request :post "/api/auth/login" (sup/json-body {:email "z@z" :password "oldpass"}))
                                   (mock/header "content-type" "application/json")
                                   (mock/header "accept" "application/json")
                                   sup/with-csrf)))))
        ;; login new ok
        (let [login (h (-> (mock/request :post "/api/auth/login" (sup/json-body {:email "z@z" :password "newpass"}))
                           (mock/header "content-type" "application/json")
                           (mock/header "accept" "application/json")
                           sup/with-csrf))
              cookie2 (sup/cookie-from login)]
          (is (= 200 (:status login)))
          ;; avatar upload
          (let [tmp (File/createTempFile "e2e-avatar-" ".png")
                _ (ImageIO/write (BufferedImage. 32 32 BufferedImage/TYPE_INT_ARGB) "png" tmp)
                req (-> (sup/request-with-cookie :post "/api/user/avatar" cookie2)
                        sup/with-csrf
                        (assoc :multipart-params {"file" {:tempfile tmp :filename "a.png"}}))
                resp (h req)
                body (json/parse-string (slurp (:body resp)) true)
                url (:url body)]
            (is (= 200 (:status resp)))
            (is (string? url))
            (is (= 200 (:status (h (mock/request :get url)))))
            ;; remove avatar and verify cleared
            (let [resp-del (h (-> (sup/request-with-cookie :delete "/api/user/avatar" cookie2)
                                  sup/with-csrf))]
              (is (= 200 (:status resp-del))))
            (let [resp-me (h (-> (sup/request-with-cookie :get "/api/auth/me" cookie2)
                                 (mock/header "accept" "application/json")))
                  me (json/parse-string (slurp (:body resp-me)) true)]
              (is (= 200 (:status resp-me)))
              (is (nil? (:avatar-url me))))))))))

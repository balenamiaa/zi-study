 (ns backend.uploads-test
  (:require [backend.uploads :as uploads]
            [clojure.test :refer [deftest is testing]])
  (:import (java.io File)))

(defn- tmp-dir []
  (let [d (File/createTempFile "uploads-root-" "-dir")]
    (.delete d)
    (.mkdirs d)
    (.getAbsolutePath d)))

(defn- temp-file [suffix]
  (doto (File/createTempFile "payload-" suffix)
    (spit "data")))

(deftest save-upload-basic
  (testing "save-upload! writes file and returns url"
    (let [root (tmp-dir)
          req {:env {:uploads {:root root}}}
          f (temp-file ".png")
          rf {:tempfile f :filename "avatar.png"}
          {:keys [url path filename]} (uploads/save-upload! req "avatars" rf #{"png"} "user1")]
      (is (= (str "/uploads/avatars/" filename) url))
      (is (.exists (File. path))))))


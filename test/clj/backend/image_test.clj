 (ns backend.image-test
  (:require [backend.image :as image]
            [clojure.test :refer [deftest is testing]])
  (:import (java.io File)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)))

(defn- write-temp-png [w h]
  (let [img (BufferedImage. w h BufferedImage/TYPE_INT_ARGB)
        f (File/createTempFile "test-img-" ".png")]
    (ImageIO/write img "png" f)
    f))

(deftest process-avatar-produces-png
  (testing "process-avatar! crops, scales and writes PNG"
    (let [in (write-temp-png 200 100)
          out (image/process-avatar! in 64)]
      (is (.exists out))
      (is (.endsWith (.getName out) ".png"))
      (let [img (ImageIO/read out)]
        (is (= (.getWidth img) (.getHeight img)))
        (is (<= (.getWidth img) 64))))))


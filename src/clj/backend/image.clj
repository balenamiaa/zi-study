(ns backend.image
  (:import (java.awt Graphics2D RenderingHints)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io File)))

(defn- copy-image ^BufferedImage [^BufferedImage img]
  (let [w (.getWidth img)
        h (.getHeight img)
        out (BufferedImage. w h (.getType img))
        g (.createGraphics out)]
    (try
      (.drawImage g img 0 0 nil)
      out
      (finally (.dispose g)))))

(defn- square-crop ^BufferedImage [^BufferedImage img]
  (let [w (.getWidth img)
        h (.getHeight img)
        side (min w h)
        x (quot (- w side) 2)
        y (quot (- h side) 2)
        sub (.getSubimage img x y side side)]
    (copy-image sub)))

(defn- scale ^BufferedImage [^BufferedImage img target]
  (let [w (.getWidth img)
        h (.getHeight img)
        side (double (max w h))
        ;; only scale down if larger than target
        s (if (> side target) (/ target side) 1.0)
        nw (max 1 (int (Math/round (* w s))))
        nh (max 1 (int (Math/round (* h s))))
        out (BufferedImage. nw nh (if (= (.getType img) BufferedImage/TYPE_INT_ARGB)
                                    BufferedImage/TYPE_INT_ARGB
                                    BufferedImage/TYPE_INT_ARGB))
        g ^Graphics2D (.createGraphics out)]
    (try
      (.setRenderingHint g RenderingHints/KEY_INTERPOLATION RenderingHints/VALUE_INTERPOLATION_BICUBIC)
      (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
      (.drawImage g img 0 0 nw nh nil)
      out
      (finally (.dispose g)))))

(defn read-image ^BufferedImage [^File f]
  (let [img (ImageIO/read f)]
    (when (nil? img)
      (throw (ex-info "Unsupported image or corrupt file" {:error :invalid-image})))
    img))

(defn write-png! [^BufferedImage img ^File out]
  (when-not (ImageIO/write img "png" out)
    (throw (ex-info "PNG writer not available" {:error :write-failed})))
  out)

(defn process-avatar!
  "Validate, square-crop and scale avatar, return a temporary PNG file.
   - target-size: maximum edge in pixels (e.g., 512)
   Returns java.io.File of the new PNG."
  [^File infile target-size]
  (let [img (read-image infile)
        sq (square-crop img)
        scaled (scale sq target-size)
        tmp (File/createTempFile "avatar-" ".png")]
    (write-png! scaled tmp)))

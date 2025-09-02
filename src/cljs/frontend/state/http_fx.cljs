(ns frontend.state.http-fx
  (:require ["@js-joda/core" :refer [Instant]]
            [cognitect.transit :as transit]
            [re-frame.core :as rf]
            [superstructor.re-frame.fetch-fx :as fetch-fx]))

(def r (transit/reader :json
                       {:handlers {"Instant" (transit/read-handler #(.parse Instant %))}}))

(defn read-transit [s]
  (transit/read r s))

;; Fetch effect with default Transit/JSON handling
(rf/reg-fx ::fetch
           (fn [effect]
             (doseq [x (fetch-fx/->seq effect)]
               (fetch-fx/fetch (merge-with merge
                                           {:envelope? false
                                            :response-content-types {#"application/json" :json
                                                                     #"application/transit\+json" {:reader-kw :text
                                                                                                   :reader-fn read-transit}}
                                            :headers {"Accept" "application/transit+json"}}
                                           x)))))

;; HTTP request state events
(rf/reg-event-db :http/init
                 (fn [db [_ path]]
                   (update db :http update-in path update :status (fn [x]
                                                                    (case x
                                                                      :ready :loading
                                                                      :initial-loading)))))

(rf/reg-event-db :http/success
                 (fn [db [_ path resp]]
                   (update db :http update-in path (fn [x]
                                                     (-> x
                                                         (assoc :status :ready)
                                                         (assoc :resp resp))))))

(rf/reg-event-db :http/failure
                 (fn [db [_ path resp]]
                   (update db :http update-in path (fn [x]
                                                     (-> x
                                                         (assoc :status :error)
                                                         (assoc :error resp))))))

(rf/reg-sub :http/body
            (fn [db [_ path]]
              (:body (:resp (get-in (:http db) path)))))

;; Simple upload effect for multipart file uploads
(rf/reg-fx ::upload
           (fn [{:keys [url file on-success on-failure]}]
             (let [fd (js/FormData.)]
               (.append fd "file" file)
               (-> (js/fetch url #js {:method "POST" :body fd})
                   (.then (fn [resp]
                            (if (.-ok resp)
                              (.json resp)
                              (throw (js/Error. (str "Upload failed: " (.-status resp)))))))
                   (.then (fn [data]
                            (when on-success
                              (rf/dispatch (conj on-success data)))))
                   (.catch (fn [err]
                             (when on-failure
                               (rf/dispatch (conj on-failure {:message (.-message err)})))))))))

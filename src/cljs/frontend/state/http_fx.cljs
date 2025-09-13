(ns frontend.state.http-fx
  (:require
   ["@js-joda/core" :refer [Instant]]
   [clojure.string :as str]
   [cognitect.transit :as transit]
   [frontend.state.ui-fx :as ui-fx]
   [re-frame.core :as rf]
   [superstructor.re-frame.fetch-fx :as fetch-fx]))

(def r (transit/reader :json
                       {:handlers {"Instant" (transit/read-handler #(.parse Instant %))}}))

(def w (transit/writer :json))

(defn read-transit [s]
  (transit/read r s))

(defn write-transit [data]
  (transit/write w data))

;; CSRF token storage and helpers
(defonce csrf-token (atom nil))

(defn- csrf-header [method]
  (let [m (some-> method name str/lower-case keyword)]
    (when (and @csrf-token (contains? #{:post :put :patch :delete} (or m :get)))
      {"X-CSRF-Token" @csrf-token
       "x-csrf-token" @csrf-token
       "x-xsrf-token" @csrf-token})))

(rf/reg-event-fx ::set-csrf-token
                 (fn [_ [_ token]]
                   (reset! csrf-token token)
                   {}))

;; Generic fallback when a request fails and no on-failure was provided.
(rf/reg-event-fx :fetch-no-on-failure
                 (fn [_ [_ resp]]
                   (let [msg (or (some-> resp :message)
                                 (some-> resp :body :message)
                                 (some-> resp :body :humanized first)
                                 (when-let [st (:status resp)] (str "HTTP " st " error"))
                                 "Request failed")]
                     {::ui-fx/toast {:text msg :variant :error}})))

;; Fetch effect with default Transit/JSON handling (library) + CSRF + credentials
(rf/reg-fx ::fetch
           (fn [effect]
             (doseq [x (fetch-fx/->seq effect)]
               (let [x' (merge {:method :get} x)
                     req-ctype (or (:request-content-type x') :transit+json)
                     ;; Pre-encode body according to content-type
                     body' (let [b (:body x')]
                             (cond
                               (nil? b) nil
                               (= req-ctype :json) (if (string? b) b (js/JSON.stringify (clj->js b)))
                               :else (if (string? b) b (write-transit b))))
                     orig-failure (:on-failure x')
                     defaults {:envelope? false
                               :request-content-type req-ctype
                               :body body'
                               :response-content-types {#"application/json" :json
                                                        #"application/transit\+json" {:reader-kw :text
                                                                                      :reader-fn read-transit}}
                               :headers (merge {"Accept" "application/json, application/transit+json"
                                                "Content-Type" (if (= req-ctype :json)
                                                                 "application/json"
                                                                 "application/transit+json")}
                                               (csrf-header (:method x')))
                               :credentials "include"
                               ;; wrap failure to auto-refresh CSRF + retry once
                               :on-failure [::handle-failure x' orig-failure]}
                     opts (-> defaults
                              (update :headers merge (or (:headers x') {}))
                              (merge (dissoc x' :headers :on-failure :body)))]
                 (fetch-fx/fetch opts)))))

;; Auto-retry CSRF failures once: fetch CSRF token, then retry the original request.
(rf/reg-event-fx
 ::handle-failure
 (fn [_ [_ req orig-on-failure resp]]
   (let [err (or (:error resp)
                 (some-> resp :body :error)
                 (some-> resp :message)
                 (some-> resp :body :message)
                 (when (string? resp) resp))
         csrf? (or (= err :invalid-anti-forgery)
                   (= err "invalid-anti-forgery")
                   (= err "Invalid anti-forgery token"))
         retried? (true? (:_retried? req))]
     (cond
       (and csrf? (not retried?))
       {:fx [[:dispatch [:frontend.state.security/fetch-csrf-and
                         [::do-retry (assoc req :_retried? true)]]]]}

       orig-on-failure
       {:fx [[:dispatch (conj orig-on-failure resp)]]}

       :else
       {:fx [[:dispatch [:fetch-no-on-failure resp]]]}))))

(rf/reg-event-fx
 ::do-retry
 (fn [_ [_ req]]
   {::fetch req}))

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
               (let [hdrs (or (csrf-header :post) {})]
                 (-> (js/fetch url #js {:method "POST"
                                        :headers (clj->js hdrs)
                                        :credentials "include"
                                        :body fd})
                     (.then (fn [resp]
                              (if (.-ok resp)
                                (.json resp)
                                (if (= 403 (.-status resp))
                                  (-> (.json resp)
                                      (.then (fn [data]
                                               (if (or (= (aget data "error") "invalid-anti-forgery")
                                                       (= (get data :error) "invalid-anti-forgery"))
                                                 (rf/dispatch [:frontend.state.security/fetch-csrf-and
                                                               [::retry-upload {:url url :file file :on-success on-success :on-failure on-failure}]])
                                                 (throw (js/Error. (str "Upload failed: " (.-status resp))))))))
                                  (throw (js/Error. (str "Upload failed: " (.-status resp))))))))
                     (.then (fn [data]
                              (when on-success
                                (rf/dispatch (conj on-success data)))))
                     (.catch (fn [err]
                               (when on-failure
                                 (rf/dispatch (conj on-failure {:message (.-message err)}))))))))))

(rf/reg-event-fx
 ::retry-upload
 (fn [_ [_ {:keys [url file on-success on-failure]}]]
   {::upload {:url url :file file :on-success on-success :on-failure on-failure}}))

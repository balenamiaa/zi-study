(ns zi-study.frontend.components.pagination
  (:require ["lucide-react" :as lucide-icons]))

(defn pagination [{:keys [page total-pages total-items limit on-page-change item-name]}]
  [:div {:class "flex flex-col sm:flex-row justify-center items-center gap-6 mt-10"
         :key (str "pagination-" page)}

   ;; Pages navigation
   [:div {:class "flex justify-center items-center"}
    [:div {:class "inline-flex items-center rounded-md shadow-sm border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] overflow-hidden"}
     ;; Previous button
     [:button
      {:class (str "p-2.5 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] border-r border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] "
                   "hover:bg-[var(--color-light-bg)] dark:hover:bg-[var(--color-dark-bg)] transition-colors flex items-center justify-center "
                   (when (= page 1) "opacity-50 cursor-not-allowed"))
       :disabled (= page 1)
       :on-click #(on-page-change (dec page))}
      [:> lucide-icons/ChevronLeft {:size 18 :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]]

     ;; First page button (if needed)
     (when (and (> total-pages 5) (> page 3))
       [:button
        {:class "h-10 w-10 flex items-center justify-center bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] hover:bg-[var(--color-light-bg)] dark:hover:bg-[var(--color-dark-bg)] transition-colors border-r border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
         :on-click #(on-page-change 1)}
        "1"])

     ;; Ellipsis (if needed) after first page
     (when (and (> total-pages 5) (> page 3))
       [:span {:class "h-10 flex items-center justify-center px-2 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] border-r border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
        "..."])

     ;; Page buttons
     (for [p (cond
               (<= total-pages 5) (range 1 (inc total-pages))
               (< page 3) (range 1 6)
               (> page (- total-pages 2)) (range (- total-pages 4) (inc total-pages))
               :else (range (- page 2) (+ page 3)))]
       ^{:key p}
       [:button
        {:class (str "h-10 w-10 flex items-center justify-center border-r border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] "
                     (if (= p page)
                       "bg-[var(--color-primary)] text-white font-medium"
                       "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] hover:bg-[var(--color-light-bg)] dark:hover:bg-[var(--color-dark-bg)] text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)]")
                     " transition-colors")
         :disabled (= p page)
         :on-click #(when (not= p page) (on-page-change p))}
        p])

     ;; Ellipsis (if needed) before last page
     (when (and (> total-pages 5) (< page (- total-pages 2)))
       [:span {:class "h-10 flex items-center justify-center px-2 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] border-r border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
        "..."])

     ;; Last page button (if needed)
     (when (and (> total-pages 5) (< page (- total-pages 2)))
       [:button
        {:class "h-10 w-10 flex items-center justify-center bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] hover:bg-[var(--color-light-bg)] dark:hover:bg-[var(--color-dark-bg)] transition-colors border-r border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
         :on-click #(on-page-change total-pages)}
        total-pages])

     ;; Next button
     [:button
      {:class (str "p-2.5 bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] hover:bg-[var(--color-light-bg)] dark:hover:bg-[var(--color-dark-bg)] transition-colors "
                   "flex items-center justify-center "
                   (when (= page total-pages) "opacity-50 cursor-not-allowed"))
       :disabled (= page total-pages)
       :on-click #(on-page-change (inc page))}
      [:> lucide-icons/ChevronRight {:size 18 :className "text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)]"}]]]]

   ;; Display showing range / total
   [:div {:class "text-sm text-[var(--color-light-text-secondary)] dark:text-[var(--color-dark-text-secondary)] bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] py-2 px-4 rounded-full shadow-sm border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
    (let [start-item (if (or (zero? total-items) (= 1 page))
                       1
                       (inc (* (dec page) limit)))
          end-item (min (* page limit) total-items)]
      (if (zero? total-items)
        "No items to display"
        (str "Showing " start-item "–" end-item " of " total-items " " item-name)))]])
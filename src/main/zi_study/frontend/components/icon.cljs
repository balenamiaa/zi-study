(ns zi-study.frontend.components.icon
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]))

(def icons
  "A map of icon names to Lucide icon components. This allows for referring
   to icons by keywords in your application.
   
   Example: (icon :user {:size 24 :class \"text-primary\"})"
  {:activity lucide-icons/Activity
   :alert-circle lucide-icons/AlertCircle
   :alert-triangle lucide-icons/AlertTriangle
   :book lucide-icons/Book
   :book-open lucide-icons/BookOpen
   :bookmark lucide-icons/Bookmark
   :calendar lucide-icons/Calendar
   :camera lucide-icons/Camera
   :check lucide-icons/Check
   :check-circle lucide-icons/CheckCircle
   :chevron-down lucide-icons/ChevronDown
   :chevron-left lucide-icons/ChevronLeft
   :chevron-right lucide-icons/ChevronRight
   :chevron-up lucide-icons/ChevronUp
   :clipboard lucide-icons/Clipboard
   :cog lucide-icons/Cog
   :edit lucide-icons/Edit
   :email lucide-icons/Mail
   :eye lucide-icons/Eye
   :eye-off lucide-icons/EyeOff
   :file-text lucide-icons/FileText
   :heart lucide-icons/Heart
   :help-circle lucide-icons/HelpCircle
   :home lucide-icons/Home
   :house lucide-icons/Home
   :image lucide-icons/Image
   :info lucide-icons/Info
   :link lucide-icons/Link
   :log-in lucide-icons/LogIn
   :log-out lucide-icons/LogOut
   :logout lucide-icons/LogOut
   :login lucide-icons/LogIn
   :menu lucide-icons/Menu
   :message-square lucide-icons/MessageSquare
   :moon lucide-icons/Moon
   :plus lucide-icons/Plus
   :search lucide-icons/Search
   :settings lucide-icons/Settings
   :shield lucide-icons/Shield
   :sparkles lucide-icons/Sparkles
   :sun lucide-icons/Sun
   :trash lucide-icons/Trash
   :upload lucide-icons/Upload
   :user lucide-icons/User
   :user-plus lucide-icons/UserPlus
   :x lucide-icons/X
   :zap lucide-icons/Zap})

(defn icon
  "A component for rendering Lucide icons.
  
   Options:
   - icon-name: the keyword/name of the icon to display (must be in the icons map)
   - attrs: a map of props to pass to the icon component
     - size: number - size of the icon in pixels (default: 24)
     - class: string - additional CSS classes
     - stroke-width: number - width of the icon stroke
   
   Example:
   [icon :user {:size 24 :class \"text-primary\"}]
   [icon :check {:class \"text-green-500\" :size 16}]"
  [icon-name attrs]
  (let [icon-component (get icons icon-name)]
    (if icon-component
      [:> icon-component
       (merge
        {:size 24}
        (update attrs :class #(str "zi-icon " %)))]
      [:span.text-error (str "Icon '" (name icon-name) "' not found")])))
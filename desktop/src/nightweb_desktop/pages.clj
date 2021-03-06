(ns nightweb-desktop.pages
  (:use [hiccup.core :only [html]]
        [nightweb.constants :only [my-hash-bytes]]
        [nightweb-desktop.views :only [get-action-bar-view
                                       get-tab-view
                                       get-user-view
                                       get-post-view
                                       get-category-view]]
        [nightweb-desktop.dialogs :only [get-search-dialog
                                         get-new-post-dialog
                                         get-link-dialog
                                         get-export-dialog
                                         get-import-dialog]]))

(defmacro get-page
  [& body]
  `(html [:head
          [:title "Nightweb"]
          [:link {:rel "stylesheet" :href "foundation.min.css"}]
          [:link {:rel "stylesheet" :href "nw.css"}]
          [:link {:rel "stylesheet" :href "fonts/general_foundicons.css"}]]
         [:body {:class "dark-gradient"}
          ~@body
          (get-search-dialog)
          (get-new-post-dialog)
          (get-link-dialog)
          (get-export-dialog)
          (get-import-dialog)
          [:div {:id "lightbox"}]
          [:script {:src "zepto.js"}]
          [:script {:src "foundation.min.js"}]
          [:script {:src "custom.modernizr.js"}]
          [:script {:src "foundation/foundation.topbar.js"}]
          [:script {:src "spin.min.js"}]
          [:script {:src "nw.js"}]]))

(defn get-view
  [params]
  (case (:type params)
    :user (if (:userhash params)
            (get-user-view params)
            (get-category-view params))
    :post (if (:time params)
            (get-post-view params)
            (get-category-view params))
    :tag (get-category-view params)
    nil (get-user-view {:type :user :userhash @my-hash-bytes})
    [:h2 "There doesn't seem to be anything here."]))

(defn get-main-page
  [params]
  (get-page
    (get-action-bar-view (get-tab-view params true))
    (get-view params)))

(defn get-category-page
  [params]
  (get-page
    (get-action-bar-view (get-tab-view params false))))

(defn get-basic-page
  [params]
  (get-page
    (get-action-bar-view nil)
    (get-view params)))

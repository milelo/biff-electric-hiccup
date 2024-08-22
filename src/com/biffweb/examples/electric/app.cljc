(ns ^:dev/always com.biffweb.examples.electric.app
  (:require #?@(:clj [[com.biffweb :as biff :refer [q]]
                      [com.biffweb.examples.electric.middleware :as mid]
                      [com.biffweb.examples.electric.settings :as settings]
                      [com.biffweb.examples.electric.signals :as sig]
                      [com.biffweb.examples.electric.ui :as ui]])
            [electric-hiccup.reader]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]))

(e/def messages)
(e/def user)

#_(e/defn SignOut []
  (dom/div
   (dom/text "Signed in as " (e/server (:user/email user)) ". ")
   (dom/form
    (dom/props {:method "post"
                :action "/auth/signout"
                :class "inline"
                :style {:margin-bottom 0}})
    (dom/input
     (dom/props {:type "hidden"
                 :name "__anti-forgery-token"
                 :value (e/server (:anti-forgery-token e/*http-request*))}))
    (dom/button
     (dom/props {:class "text-blue-500 hover:text-blue-800"
                 :type "submit"})
     (dom/text "Sign out")))))

(e/defn SignOut []
  #electric-hiccup
   [:div
    (dom/text "Signed in as " (e/server (:user/email user)) ". ")
    [:form {:method "post"
            :action "/auth/signout"
            :class "inline"
            :style {:margin-bottom 0}} 
     [:input {:type "hidden"
              :name "__anti-forgery-token"
              :value (e/server (:anti-forgery-token e/*http-request*))}]
     [:button {:class "text-blue-500 hover:text-blue-800"
               :type :submit} "Sign out"]]])

(e/defn FooForm []
  #electric-hiccup
   [:form.inline {:method "post"
                  :action "/app/set-foo"
                  :style {:margin-bottom 0}}
    [:input {:type "hidden"
             :name "__anti-forgery-token"
             :value (e/server (:anti-forgery-token e/*http-request*))}]
    [:label {:for "foo" :class "block"} "Foo: "
     [:span.font-mono
      (dom/text (e/server (pr-str (:user/foo user))))]]
    [:div.h-1]
    [:div.flex
     [:input {:class "w-full"
              :id "foo"
              :type "text"
              :name "foo"
              :value (e/server (:user/foo user))}]
     [:div.w-3]
     [:button.btn {:type :Submit} "Update"]]
    [:div.h-1]
    [:div.text-sm.text-gray-600
     "This demonstrates updating a value with a plain old form."]])

(e/defn SetBar [text]
  (e/server
   (biff/submit-tx e/*http-request*
                   [{:db/doc-type :user
                     :xt/id (:xt/id user)
                     :db/op :update
                     :user/bar text}])))

(e/defn BarForm []
  #electric-hiccup
   [:div
    [:label.block {:for :bar} "Bar: "
     [:span.font-mono
      (dom/text (e/server (pr-str (:user/bar user))))]]
    [:div.h-1]
    (let [bar (e/server (:user/bar user))
          text (atom bar)]
      #electric-hiccup
       [:div.flex
        [:input#bar.w-full {:type :text :value bar}
         (dom/on "keyup" (e/fn [e]
                           (reset! text (-> e .-target .-value))))
         (dom/on "keydown" (e/fn [e]
                             (when (= "Enter" (.-key e))
                               (SetBar. (or @text "")))))]
        [:div.w-3]
        [:button.btn {:type :Submit} "Update"
         (dom/on "click" (e/fn [_e]
                           (SetBar. (or @text ""))))]])
    [:div.text-sm.text-gray-600
     "This demonstrates updating a value with Electric."]])

(e/defn Message [{:msg/keys [text sent-at]}]
  (dom/div
   (dom/props {:class "mt-3"})
   (dom/div
    (dom/props {:class "text-gray-600"})
    (dom/text sent-at))
   (dom/div (dom/text text))))

(e/defn Message [{:msg/keys [text sent-at]}]
  #electric-hiccup
   [:div.mt-3 
    [:div.text-gray-600 (dom/text sent-at)]
    [:div (dom/text text)]])

(e/defn Chat []
  (let [messages (e/server (vec (for [m messages]
                                  (update m :msg/sent-at biff/format-date "dd MMM yyyy HH:mm:ss"))))
        !text (atom "")
        text (e/watch !text)]
    #electric-hiccup
     [:div
      [:label.block  {:for :message} "Write a message"]
      [:div.h-1]
      [:textarea {:class "w-full"
                  :id "message"
                  :type "text"
                  :value text}
       (dom/on "keyup" (e/fn [e]
                         (reset! !text (-> e .-target .-value))))]
      [:div.h-1]
      [:div.text-sm.text-gray-600 "Sign in with an incognito window to have a conversation with yourself."]
      [:div.h-2]
      [:div
       [:button.btn "Send message"
        (dom/on "click" (e/fn [e]
                          (when (not-empty text)
                            (e/server
                             (biff/submit-tx e/*http-request*
                                             [{:db/doc-type :msg
                                               :msg/user (:xt/id user)
                                               :msg/text text
                                               :msg/sent-at :db/now}]))
                            (reset! !text ""))))]]
      [:div.h-6]
      [:div (dom/text
             (if (empty? messages)
               "No messages yet."
               "Messages sent in the past 10 minutes:"))]
      [:div (e/for-by :xt/id [msg messages]
                      (Message. msg))]]))

(e/defn App []
  (e/server
   (binding [messages (new (sig/recent-messages e/*http-request*))
             user (new (sig/user e/*http-request*))]
     (e/client
      [:div
       (SignOut.)
       [:div.h-6]
       (FooForm.)
       [:div.h-6]
       (BarForm.)
       [:div.h-6]
       (Chat.)]))))

#?(:cljs
   (do
     (def electric-main
       (e/boot
        (set! (.-innerHTML (.querySelector js/document "#loading")) "")
        (binding [dom/node (.querySelector js/document "#electric")]
          (App.))))

     (defonce reactor nil)

     (defn ^:dev/after-load ^:export start! []
       (assert (nil? reactor) "reactor already running")
       (set! reactor (electric-main
                      #(js/console.log "Reactor success:" %)
                      #(js/console.error "Reactor failure:" %))))

     (defn ^:dev/before-load stop! []
       (when reactor (reactor)) ; teardown
       (set! reactor nil))))

#?(:clj
   (do
     (defn app [_]
       (ui/page
        {:com.biffweb.examples.electric.ui/electric true}
        [:div#electric
         [:div#loading "Loading..."]]))

     (def about-page
       (ui/page
        {:base/title (str "About " settings/app-name)}
        [:p "This app was made with "
         [:a.link {:href "https://biffweb.com"} "Biff"] "."]))

     (defn set-foo [{:keys [session params] :as ctx}]
       (biff/submit-tx ctx
                       [{:db/op :update
                         :db/doc-type :user
                         :xt/id (:uid session)
                         :user/foo (:foo params)}])
       {:status 303
        :headers {"location" "/app"}})

     (defn echo [{:keys [params]}]
       {:status 200
        :headers {"content-type" "application/json"}
        :body params})

     (def plugin
       {:static {"/about/" about-page}
        :routes ["/app" {:middleware [mid/wrap-signed-in
                                      mid/wrap-electric]}
                 ["" {:get app}]
                 ["/set-foo" {:post set-foo}]]
        :api-routes [["/api/echo" {:post echo}]]})))

(ns ^:dev/always com.biffweb.examples.electric.app
  (:require #?@(:clj [[com.biffweb :as biff :refer [q]]
                      [com.biffweb.examples.electric.middleware :as mid]
                      [com.biffweb.examples.electric.settings :as settings]
                      [com.biffweb.examples.electric.signals :as sig]
                      [com.biffweb.examples.electric.ui :as ui]])
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-svg :as svg]
            [electric-hiccup.reader]))

(e/def messages)
(e/def user)

(e/defn SignOut []
  #electric-hiccup
   [:div
    (dom/text "Signed in as " (e/server (:user/email user)) ". ")
    [:form {:method :post
            :action "/auth/signout"
            :class :inline
            :style {:margin-bottom 0}}
     [:input {:type :hidden
              :name "__anti-forgery-token"
              :value (e/server (:anti-forgery-token e/*http-request*))}]
     [:button.text-blue-500.hover:text-blue-800 ;mid keyword ":" is ok
      {:type :submit} "Sign out"]]])

(e/defn FooForm []
  #electric-hiccup
   [:form.inline {:method :post
                  :action "/app/set-foo"
                  :style {:margin-bottom 0}}
    [:input {:type "hidden"
             :name "__anti-forgery-token"
             :value (e/server (:anti-forgery-token e/*http-request*))}]
    [:label.block {:for "foo"} "Foo: "
     [:span.font-mono
      (dom/text (e/server (pr-str (:user/foo user))))]]
    [:.h-1]
    [:.flex
     [:input {:class :w-full
              :id :foo
              :type :text
              :name :foo
              :value (e/server (:user/foo user))}]
     [:.w-3]
     [:button.btn {:type :Submit} "Update"]]
    [:.h-1]
    [:.text-sm.text-gray-600
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
    [:.h-1]
    (let [bar (e/server (:user/bar user))
          !text (atom bar)]
      #electric-hiccup
       [:.flex
        [:input#bar.w-full {:type :text :value bar}
         (dom/on "keyup" (e/fn [e]
                           (reset! !text (-> e .-target .-value))))
         (dom/on "keydown" (e/fn [e]
                             (when (= "Enter" (.-key e))
                               (SetBar. (or @!text "")))))]
        [:.w-3]
        [:button.btn {:type :Submit} "Update"
         (dom/on "click" (e/fn [_e]
                           (SetBar. (or @!text ""))))]])
    [:.text-sm.text-gray-600
     "This demonstrates updating a value with Electric."]])

(e/defn Message [{:msg/keys [text sent-at]}]
  #electric-hiccup
   [:.mt-3
    [:.text-gray-600 (dom/text sent-at)]
    [:div (dom/text text)]])

(e/defn Chat []
  (let [messages (e/server (vec (for [m messages]
                                  (update m :msg/sent-at biff/format-date "dd MMM yyyy HH:mm:ss"))))
        !text (atom "")
        text (e/watch !text)]
    #electric-hiccup
     [:div
      [:label.block {:for :message} "Write a message"]
      [:.h-1]
      [:textarea {:class :w-full
                  :id :message
                  :type :text
                  :value text}
       (dom/on "keyup" (e/fn [e]
                         (reset! !text (-> e .-target .-value))))]
      [:.h-1]
      [:.text-sm.text-gray-600
       "Sign in with an incognito window to have a conversation with yourself."]
      [:.h-2]
      [:div
       [:button.btn "Send message"
        (dom/on "click" (e/fn [_e]
                          (when (not-empty text)
                            (e/server
                             (biff/submit-tx e/*http-request*
                                             [{:db/doc-type :msg
                                               :msg/user (:xt/id user)
                                               :msg/text text
                                               :msg/sent-at :db/now}]))
                            (reset! !text ""))))]]
      [:.h-6]
      [:div (dom/text
             (if (empty? messages)
               "No messages yet."
               "Messages sent in the past 10 minutes:"))]
      [:div (e/for-by :xt/id [msg messages]
                      (Message. msg))]]))

(defn wave [time]
  (Math/cos (/ (* (mod (Math/round (/ time 10)) 360) Math/PI) 180)))

(e/defn SVG []
  (e/client
   (let [offset (* 3 (wave e/system-time-ms))] ; js animation clock
     #electric-hiccup
      [:div
       [:dom/div.text-gray-600
        "electric-hiccup supports other namespaces:"]
      ;to use other namespaces, 'require the namespace and prefix the tag with its alias
       [:svg/svg {:viewBox "0 0 300 100"}
        [:svg/circle {:cx 50 :cy 50 :r (+ 30 offset)
                      :style {:fill "#af7ac5 "}}]
        [:svg/g {:transform
                 (str "translate(105,20) rotate(" (* 3 offset) ")")}
         [:svg/polygon {:points "30,0 0,60 60,60"
                        :style {:fill "#5499c7"}}]]
        [:svg/rect {:x 200 :y 20 :width (+ 60 offset) :height (+ 60 offset)
                    :style {:fill "#45b39d"}}]]
       [:.text-sm.text-gray-600
        "SVG demo from: " [:span [:a {:href "https://github.com/hyperfiddle/electric-fiddle/blob/main/src/electric_tutorial/demo_svg.cljc"
                                     :target "_blank"} "electric-fiddle"]]]])))

(e/defn App []
  (e/server
   (binding [messages (new (sig/recent-messages e/*http-request*))
             user (new (sig/user e/*http-request*))]
     (e/client
      #electric-hiccup
       [:div
        (SignOut.)
        [:.h-6] ;tag defaults to div
        (FooForm.)
        [:.h-6]
        (BarForm.)
        [:.h-6]
        (Chat.)
        [:.h-6]
        (SVG.)]))))

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

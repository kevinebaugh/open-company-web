(ns oc.pages
  (:require [oc.terms :as terms]
            [oc.privacy :as privacy]))

(defn terms [options]
  (terms/terms options))

(defn privacy [options]
  (privacy/privacy options))

(defn features [options]
  [:div.container.main.features
    ; Hope page header
    [:h1.features "Features"]

    [:div.divider-line]


    [:div.illustrations.group

      [:div.illustration.illustration-1.group
        [:img {:src "/img/ML/features_il_1_608_504.svg"}]
        [:div.description.group
          [:div.title
            "Simplicity"]
          [:div.subtitle
            "Whether you’re adding a quick update about one topic, or writing a story that covers many, getting started is fast and simple."]]]

      [:div.illustration.illustration-2.group
        [:img {:src "/img/ML/features_il_2_395_424.svg"}]
        [:div.description.group
          [:div.title
            "Company timeline"]
          [:div.subtitle
            "It’s easy to catch up if you missed something or want more context. Great for getting new employees up to speed, too."]]]

      [:div.illustration.illustration-3.group
        [:img {:src "/img/ML/features_il_3_443_179.svg"}]
        [:div.description.group
          [:div.title
            "Feedback loops"]
          [:div.subtitle
            "Company updates are best when they trigger conversation. Comments and reactions keep everyone engaged and in sync - great for distributed teams."]]]

      [:div.illustration.illustration-4.group
        [:img {:src "/img/ML/features_il_4_288_233.svg"}]
        [:div.description.group
          [:div.title
            "Integrate with Slack"]
          [:div.subtitle
            "With Slack single sign-on and our Slack bot, updates are automatically shared to the right channels. Discussions about updates can happen within Slack or Carrot - everything is kept in sync."]]]

      [:div.illustration.illustration-5.group
        [:img {:src "/img/ML/features_il_5_320_312.svg"}]
        [:div.description.group
          [:div.title
            "Share the news more broadly"]
          [:div.subtitle
            "Share something beautiful via email or on the Web. Updates and stories are accessible by teams, but can also be made public or private."]]]]])

(defn pricing
  "Pricing page. This is a copy of oc.web.components.pricing and every change here should be reflected there and vice versa."
  [options]
   [:div.container.outer.sector.content
    [:div.row
     [:div.col-md-12.pricing-header
      [:h2 "Simple Pricing"]
      [:p "Transparent prices for any need."]]]
    [:div.row
     "<!-- Pricing Item -->"
     [:div.col-md-4
      [:div.pricing.hover-effect
       [:div.pricing-name
        [:h3 "Team"
         [:span "Internal Slack distrbution"]]]
       [:div.pricing-price [:h4 [:i "$"] "25" [:span "Per Month"]]]
       [:ul.pricing-content.list-unstyled
        [:li "Stakeholder dashboard"]
        [:li "Rich Slack integration"]]
       [:div.pricing-footer
        [:p "Perfect for keeping your team members informed."]
        [:p "Optionally involve the crowd with a public stakeholder dashboard."]]]]
     [:div.col-md-4
      [:div.pricing.hover-effect
       [:div.pricing-name
        [:h3 "Stakeholders"
         [:span "Periodic stakeholder updates"]]]
       [:div.pricing-price [:h4 [:i "$"] "50" [:span "Per Month"]]]
       [:ul.pricing-content.list-unstyled
        [:li "Stakeholder dashboard"]
        [:li "Rich Slack integration"]
        [:li [:b "Periodic stakeholder updates"]]]
       [:div.pricing-footer
        [:p "Keep your busy investors and advisors informed with periodic updates that follow best practices."]]]]
     [:div.col-md-4
      [:div.pricing.hover-effect
       [:div.pricing-name
        [:h3 "Concierge"
         [:span "Beautifully designed content"]]]
       [:div.pricing-price [:h4 [:i "$"] "250" [:span "Per Month"]]]
       [:ul.pricing-content.list-unstyled
        [:li "Stakeholder dashboard"]
        [:li "Rich Slack integration"]
        [:li "Periodic stakeholder updates"]
        [:li [:b "Concierge support to desgin custom stakeholder content *"]]]
       [:div.pricing-footer
        [:p "Impress your investors and advisors with beautful, concise and meaningful updates."]]]]
     ;; "<!--         <div class=\"col-md-4 sm-12\">\n          <h2>Team</h2>\n          <p>Internal distribution with Slack.</p>\n        </div>\n        <div class=\"col-md-4 sm-12\">\n          <h2>Stakeholders</h2>\n          <p>Periodic stakeholder updates distributed automatically.</p>\n        </div>\n        <div class=\"col-md-4 sm-12\">\n          <h2>Concierge</h2>\n          <p>Beautiful stakeholder updates, hand-crafted by content creation professionals.</p>\n        </div>\n -->"
     ]])

(defn about
  "About page. This is a copy of oc.web.components.about and every change here should be reflected there and vice versa."
  [options]
  [:div
    [:div.container.main.about

      [:h1.about "About"]

      [:div.divider-line]

      [:div.ovarls-container

        [:div.about-subline
          "Companies struggle to keep everyone on the same page. People are hyper-connected in the moment but still don’t know what’s happening across the company."]
        [:div.paragraphs-container.group
          [:div.mobile-only.happy-face.yellow-happy-face]
          [:div.mobile-only.happy-face.red-happy-face]
          [:div.mobile-paragraphs-container.group
            [:div.paragraph
              "The solution is surprisingly simple and effective - great company updates build transparency and alignment.  Updates need to be fun and engaging to trigger the conversations that keep everyone in sync. They need to be kept in one place so they’re easy to find, and don’t disappear as the chats scroll by."]
            [:div.paragraph
              "With that in mind we designed Carrot based on three principles:"]]
          [:div.mobile-only.happy-face.blue-happy-face]
          [:div.mobile-only.happy-face.purple-happy-face]
          [:div.mobile-only.happy-face.green-happy-face]]]

      [:div.principles.group
        [:div.principle.principle-1
          [:div.principle-oval-bg]
          [:div.principle-logo]
          [:div.principle-title "It has to be easy or no one will play."]
          [:div.principle-description "Alignment might be essential for success, but achieving it has never been easy or fun. We’re changing that. With a simple structure and beautiful writing experience, it can’t be easier. Just say what’s going on, we’ll take care of the rest."]]

        [:div.principle.principle-2
          [:div.principle-oval-bg]
          [:div.principle-logo]
          [:div.principle-title "The “big picture” should always be visible. "]
          [:div.principle-description "No one wants to drill into folders or documents to understand what’s going on, or search through corporate chat archives to find something. It should be easier to get an instant view of what’s happening across the company anytime."]]

        [:div.principle.principle-3
          [:div.principle-oval-bg]
          [:div.principle-logo]
          [:div.principle-title "Alignment is valuable beyond the team, too."]
          [:div.principle-description "Once you’ve created the big picture, you should be able to share it with anyone, inside and outside the company. Sharing beautiful updates with recruits, investors and customers is the surest way to keep them engaged and supportive."]]]

    ] ;<!-- main -->

    [:div.about-alignment
      [:div.quote]
      [:div.about-alignment-description "Company alignment requires real openness and transparency."]]

    [:div.about-team.group
      [:div.about-team-inner.group
        [:h1.team "Our team"]
        [:div.divider-line]

        [:div.group
          [:div.column-left.group
            [:div.team-card
              [:div.team-avatar]
              [:div.team-member
                [:div.team-name "Stuart Levinson"]
                [:div.team-description "Prior to founding OpenCompany, Stuart started and sold two venture-backed startups. Venetica (acquired by IBM) pioneered a new type of enterprise integration software, and TalkTo (acquired by Path) launched the first messaging app to local businesses powered by a human + AI backend."]
                [:div.team-media-links
                  [:a.linkedin {:href "https://linkedin.com/in/stuartlevinson"}]
                  [:a.twitter {:href "https://twitter.com/stuartlevinson"}]]]]
            [:div.team-card.new-member
              [:div.team-avatar]
                [:div.team-member
                  [:div.team-name "You?"]
                  [:div.team-description "We're always looking for talented individuals. Drop us a line if you share our mission."]]]]

          [:div.column-right.group
            [:div.team-card
              [:div.team-avatar]
              [:div.team-member
                [:div.team-name "Sean Johnson"]
                [:div.team-description "As a serial startup CTO and engineer, Sean has over 20 years experience building products and startup engineering teams."]
                [:div.team-media-links
                  [:a.linkedin {:href "https://linkedin.com/in/snootymonkey"}]
                  [:a.twitter {:href "http://twitter.com/belucid"}]
                  [:a.github {:href "http://github.com/belucid"} [:i.fa.fa-github]]]]]
            [:div.team-card
              [:div.team-avatar]
              [:div.team-member
                [:div.team-name "Iacopo Carraro"]
                [:div.team-description "Iacopo is a full-stack engineer with lots of remote team and startup experience."]
                [:div.team-media-links
                  [:a.linkedin {:href "https://www.linkedin.com/pub/iacopo-carraro/21/ba2/5ab"}]
                  [:a.twitter {:href "http://twitter.com/bago2k4"}]
                  [:a.github {:href "http://github.com/bago2k4"} [:i.fa.fa-github]]]]]]]]]

    [:div.about-footer.group

      [:div.block.join-us
        [:div.block-title
          "Join Us"]
        [:div.block-description
          "Want to join us? We are always looking for amazing people no matter where they live."]
        [:a.link
          {:a "mailto:hello@carrot.io"}
          "Say hello"]]

      [:div.block.open-source
        [:div.block-title
          "Open Source"]
        [:div.block-description
          "Have an idea you’d like to contribute? A new integration you’d like to see?"]
        [:a.link
          {:href "https://github.com/open-company"}
          "Build it with us on Github"]]]
    ])

(defn not-found [{contact-mail-to :contact-mail-to contact-email :contact-email}]
  [:div.container.outer.sector.not-found
   [:div.container.inner
    [:div.row
     [:div.col-md-12
      [:div.error-page
       [:h1 "404"]
       [:h2 "Hmm, this does not look right!!!"]
       [:p {:id "oc-404-disclaimer"} "You are accessing a page that doesn’t exist or requires authentication."]
       [:a.btn {:href "/"} "RETURN TO HOME"]
       [:a.btn.ml2 {:id "oc-signin-logout-btn" :href "/login"} "SIGN IN / SIGN UP"]
       [:script {:src "/js/set-path.js"}]]]]]])

(defn server-error [{contact-mail-to :contact-mail-to contact-email :contact-email}]
  [:div.container.outer.sector.server-error
   [:div.container.inner
    [:div.row
     [:div.col-md-12
      [:div.error-page
       [:h1 "500"]
       [:h2 "Hmm, this does not look right."]
       [:p
        "You seem to have come across an error."
        [:br]
        "Please try again or contact support: "
        [:a {:href contact-mail-to} contact-email]]
       [:a.btn {:href "/"} "Return To Home"]
       [:script {:src "/js/set-path.js"}]]]]]])

(def app-shell
  {:head [:head
          [:meta {:charset "utf-8"}]
          [:meta {:content "IE=edge", :http-equiv "X-UA-Compatible"}]
          [:meta {:content "width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no", :name "viewport"}]
          [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
          [:link {:rel "icon" :type "image/png" :href "/img/carrot_logo.png" :sizes "64x64"}]
          ;; The above 3 meta tags *must* come first in the head;
          ;; any other head content must come *after* these tags
          [:title "Carrot - Company updates and stories"]
          ;; Reset IE
          "<!--[if lt IE 9]><script src=\"//html5shim.googlecode.com/svn/trunk/html5.js\"></script><![endif]-->"
          ;; Bootstrap CSS //getbootstrap.com/
          [:link {:rel "stylesheet" :href "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" :crossorigin "anonymous"}]
          ;; Normalize.css //necolas.github.io/normalize.css/
          ;; TODO inline this into app.main.css
          [:link {:rel "stylesheet" :href "/css/normalize.css?oc_deploy_key"}]
          ;; Font Awesome icon fonts //fortawesome.github.io/Font-Awesome/cheatsheet/
          [:link {:rel "stylesheet" :href "//maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}]
          ;; OpenCompany CSS
          [:link {:type "text/css" :rel "stylesheet" :href "/css/app.main.css?oc_deploy_key"}]
          ;; jQuery UI CSS
          [:link {:rel "stylesheet" :href "//ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/themes/smoothness/jquery-ui.css"}]
          ;; Emoji One Autocomplete CSS
          [:link {:type "text/css" :rel "stylesheet" :href "/css/emojione/autocomplete.css?oc_deploy_key"}]
          ;; Google fonts Domine and OpenSans
          [:link {:type "text/css" :rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Open+Sans:400,700,600,300,800|Domine:400,700"}]
          ;; Google fonts Muli
          [:link {:href "https://fonts.googleapis.com/css?family=Muli" :rel "stylesheet"}]
          ;;  Medium Editor css
          [:link {:type "text/css" :rel "stylesheet" :href "/css/medium-editor/medium-editor.css?oc_deploy_key"}]
          [:link {:type "text/css" :rel "stylesheet" :href "/css/medium-editor/default.css?oc_deploy_key"}]
          ;; Emojione CSS
          [:link {:type "text/css" :rel "stylesheet" :href "/css/emojione.css?oc_deploy_key"}]
          ;; EmojionePicker css from cljsjs
          [:link {:type "text/css" :rel "stylesheet" :href "/css/emojione-picker.css?oc_deploy_key"}]
          ;; Emojone Sprites CSS
          [:link {:type "text/css" :rel "stylesheet" :href "/css/emojione.sprites.css?oc_deploy_key"}]
          ;; CarrotKit Font
          [:link {:type "text/css" :rel "stylesheet" :href "/css/fonts/CarrotKit.css?oc_deploy_key"}]
          ;; Filestack
          [:script {:type "text/javascript" :src "//static.filestackapi.com/v3/filestack-0.4.1.js"}]
          [:script {:type "text/javascript" :src "/lib/print_ascii.js"}]]
   :body [:body.small-footer
          [:div#app [:div.oc-loading.active [:div.oc-loading-inner [:div.oc-loading-heart] [:div.oc-loading-body]]]]
          [:div#oc-error-banner]
          [:div#oc-loading]
          ;; Custom Tooltips
          [:script {:type "text/javascript" :src "/lib/tooltip/tooltip.js"}]
          ;; jQuery needed by Bootstrap JavaScript
          [:script {:src "//ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js" :type "text/javascript"}]
          ;; Truncate html string
          [:script {:type "text/javascript" :src "/lib/truncate/jquery.truncate.js"}]
          ;; jQuery textcomplete needed by Emoji One autocomplete
          [:script {:src "//cdnjs.cloudflare.com/ajax/libs/jquery.textcomplete/1.7.3/jquery.textcomplete.min.js" :type "text/javascript"}]
          ;; WURFL used for mobile/tablet detection
          [:script {:type "text/javascript" :src "//wurfl.io/wurfl.js"}]
          ;; jQuery scrollTo plugin
          [:script {:src "/lib/scrollTo/scrollTo.min.js?oc_deploy_key" :type "text/javascript"}]
          ;; jQuery UI
          [:script {:src "//ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js" :type "text/javascript"}]
          ;; Resolve jQuery UI and Bootstrap tooltip conflict
          [:script "$.widget.bridge('uitooltip', $.ui.tooltip);"]
          ;; Bootstrap JavaScript //getf.com/
          [:script {:src "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" :type "text/javascript" :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" :crossorigin "anonymous"}]
          ;; Emoji One Autocomplete
          [:script {:src "/js/emojione/autocomplete.js?oc_deploy_key" :type "text/javascript"}]
          ;; ClojureScript generated JavaScript
          [:script {:src "/oc.js?oc_deploy_key" :type "text/javascript"}]
          ;; Utilities
          [:script {:type "text/javascript", :src "/lib/js-utils/svg-utils.js?oc_deploy_key"}]
          [:script {:type "text/javascript", :src "/lib/js-utils/pasteHtmlAtCaret.js?oc_deploy_key"}]
          ;; Clean HTML input
          [:script {:src "/lib/cleanHTML/cleanHTML.js?oc_deploy_key" :type "text/javascript"}]
          ;; MediumEditorAutolist
          [:script {:type "text/javascript" :src "/lib/MediumEditorAutolist/autolist.js"}]
          [:div.hidden [:img {:src "/img/emojione.sprites.png"}]]]})

(def prod-app-shell
  {:head [:head
          [:meta {:charset "utf-8"}]
          [:meta {:content "IE=edge", :http-equiv "X-UA-Compatible"}]
          [:meta {:content "width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no", :name "viewport"}]
          [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
          [:link {:rel "icon" :type "image/png" :href "/img/carrot_logo.png" :sizes "64x64"}]
          ;; The above 3 meta tags *must* come first in the head;
          ;; any other head content must come *after* these tags
          [:title "Carrot - Company updates and stories"]
          ;; Reset IE
          "<!--[if lt IE 9]><script src=\"//html5shim.googlecode.com/svn/trunk/html5.js\"></script><![endif]-->"
          ;; Bootstrap CSS //getbootstrap.com/
          [:link {:rel "stylesheet" :href "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" :crossorigin "anonymous"}]
          ;; Font Awesome icon fonts //fortawesome.github.io/Font-Awesome/cheatsheet/
          [:link {:rel "stylesheet" :href "//maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}]
          ;; jQuery UI CSS
          [:link {:rel "stylesheet" :href "//ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/themes/smoothness/jquery-ui.css"}]
          ;; Google fonts Domine and OpenSans
          [:link {:type "text/css" :rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Open+Sans:400,700,600,300,800|Domine:400,700"}]
          ;; App single CSS
          [:link {:type "text/css" :rel "stylesheet" :href "oc_web_cdn_url/oc_deploy_key/main.css"}]
          ;; Google fonts Muli
          [:link {:href "https://fonts.googleapis.com/css?family=Muli" :rel "stylesheet"}]
          ;; CarrotKit Font
          [:link {:type "text/css" :rel "stylesheet" :href "oc_web_cdn_url/fonts/CarrotKit.css"}]
          ;; Filestack
          [:script {:type "text/javascript" :src "//static.filestackapi.com/v3/filestack-0.1.10.js"}]
          ;; jQuery needed by Bootstrap JavaScript
          [:script {:src "//ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js" :type "text/javascript"}]]
   :body [:body.small-footer
          [:div#app [:div.oc-loading.active [:div.oc-loading-inner [:div.oc-loading-heart] [:div.oc-loading-body]]]]
          [:div#oc-error-banner]
          [:div#oc-loading]
          
          ;; jQuery textcomplete needed by Emoji One autocomplete
          [:script {:src "//cdnjs.cloudflare.com/ajax/libs/jquery.textcomplete/1.7.3/jquery.textcomplete.min.js" :type "text/javascript"}]
          ;; WURFL used for mobile/tablet detection
          [:script {:type "text/javascript" :src "//wurfl.io/wurfl.js"}]
          ;; jQuery UI
          [:script {:src "//ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js" :type "text/javascript"}]
          ;; Resolve jQuery UI and Bootstrap tooltip conflict
          [:script "$.widget.bridge('uitooltip', $.ui.tooltip);"]
          ;; Bootstrap JavaScript //getf.com/
          [:script {:src "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" :type "text/javascript" :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" :crossorigin "anonymous"}]
          ;; Compiled oc.min.js from our CDN
          [:script {:src "oc_web_cdn_url/oc_deploy_key/oc.js"}]
          ;; Compiled assents
          [:script {:src "oc_web_cdn_url/oc_deploy_key/oc_assets.js"}]
          [:div.hidden [:img {:src "oc_web_cdn_url/img/emojione.sprites.png"}]]]})
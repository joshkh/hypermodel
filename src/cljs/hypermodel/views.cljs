(enable-console-print!)

(ns hypermodel.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [hypermodel.radial :as radial]
            [hypermodel.transforms :as t]
            [json-html.core :as table]))

(def dimensions (reagent/atom {:width 500
                               :height 500}))

(def center-str
  (str "translate("
       (/ (:width @dimensions) 2)
       ","
       (/ (:height @dimensions) 2)
       ")"))

(defn half-of [val]
  (/ val 2))

(def plane-scale (-> js/d3 .-scale .linear
                     (.range #js [0 (half-of (- (:height @dimensions) 100))])
                     (.domain #js [0 5])))

(println "scales" (map plane-scale (range 1 4)))

(defn plane-guide []
  (fn [radius]
    ;(println "plane got r" radius)
    [:circle {:stroke-dasharray "3, 3"
              :r radius}]))

(defn plane-guides []
  (fn []
    [:g.planes {:transform center-str}
     (for [p (map plane-scale (range 0 5))]
       [plane-guide p])]))


(defn node []
  (fn [details]
    [:g.node
     [:circle {:r 40}]
     [:text {:text-anchor "middle"}
      (:node details)]]))

;(def dots ["A" "B" "C" "D" "E"])



(defn dot []
  (fn [{:keys [label tier index total] :as x}]
    ;(println "DOT GOT" x)
    [:g.node {:transform (str "rotate(" (* index (/ 360 total)) ")")}
     [:line {:x1 0 :y1 0 :x2 0 :y2 (* -1 (plane-scale tier))}]
     [:circle {:cx 0
               ;:fill "red"
               :cy (* -1 (plane-scale tier))
               ;:r (- 60 (* tier 20))
               ;:r (* 60 (/ 1 tier))
               :r (* 30 (/ 1 (inc tier)))
                ;:r 0
               }]
     [:text {:y (* -1 (plane-scale tier))
             :text-anchor "middle"} (str (name label))]]))



(defn some-plane [tier]
  (let [nodes-on-tier (re-frame/subscribe [:tiers (keyword (str tier))])]
    (fn []
      ;(println "nodes-on-tier" tier @nodes-on-tier)
      [:g.tier {:transform center-str}
       (loop [idx 0 total [:g]]
         (if (= idx (count @nodes-on-tier))
           total
           (recur (inc idx) (conj total [dot {:label (:node (get @nodes-on-tier idx))
                                              :tier tier
                                              :index (inc idx)
                                              :total (count @nodes-on-tier)}]))))])))


(defn new-plane [tier]
  (let [nodes-on-tier (re-frame/subscribe [:data-rows tier])]
    (fn []
      ;(println "nodes-on-tier" tier @nodes-on-tier)
      ;(println "GETTING" (nth @nodes-on-tier 0))
      ;(println "center-str" center-str)
      [:g.tier {:transform center-str}
       (loop [idx 0 total [:g]]
         (if (= idx (count @nodes-on-tier))
           total
           (recur (inc idx) (conj total [dot {:label (:self (nth @nodes-on-tier idx))
                                              :tier tier
                                              :index (inc idx)
                                              :total (count @nodes-on-tier)}]))))])))

(defn link []
  (let [data (re-frame/subscribe [:all-data])]
    (fn [tier]
      (let [prior-row (nth @data (dec tier))
            current-row (nth @data tier)]

        [:line.link {:x1 0 :y1 0 :x2 0 :y2 50}]))))

;links


(defn planes []
  [:g
   (for [tier (range 0 5)]
     [new-plane tier])])

(defn model []
  (let [model (re-frame/subscribe [:model])]
    [:div (str @model)]))

(defn dr []
  (let [model (re-frame/subscribe [:data-rows 0])]
    [:div (str "DR" @model)]))



(defn node-ring [n]
  (let [ring-data (re-frame/subscribe [:data-rows n])]
    (fn [n]
      [:div (str "N" n @ring-data)])))

(defn node-rings []
  (let [data (re-frame/subscribe [:all-data])]
    (fn []
      [:div
       (for [c (range (count @data))]
         [node-ring c])])))


(defn degree-ring [n]
  (let [ring-data (re-frame/subscribe [:data-rows n])]
    (fn []
      ;(println "degree ring has" @ring-data)
      [:g
       (for [n @ring-data]
         [:g.node
          [:circle {:cx (:cx n)
                    :cy (:cy n)
                    :r (:r n)}]
          [:text {:x (:cx n)
                  :y (:cy n)}
           (str (name (:self n)))]])])))

(defn degrees []
  (let [data (re-frame/subscribe [:all-data])]
    [:g
     (for [d (range (count @data))]
       [degree-ring d])]))

(defn svg-body-old []
  [:svg.hyperview {:shape-rendering "crispEdges"
                   :text-rendering "optimizeLegibility"
                   :width (:width @dimensions)
                   :height (:height @dimensions)
                   :id "canvas"}
   [plane-guides]
   [:g {:transform center-str}
    [degrees]]
   ;[:g {:transform center-str}
   ; (for [a (range 0 360 )]
   ;   (let [pos (t/radial 0 0 100 a)]
   ;     ;(println pos)
   ;     [:circle {:cx (:x pos)
   ;               :cy (:y pos)
   ;               :r 1}]))]
   ;[planes]
   ;[links]
   ;[dr]
   ;[model]
   ])


(defn recur-children [node total]
  (let [new-total (conj total [:g.node
                               {:on-click #(println (str node))}
                               [:circle {:cx (:cx node)
                                         :cy (:cy node)
                                         :r  10}]
                               [:text {:x (:cx node)
                                       :y (:cy node)}
                                (str (name (:name node)))]])]
    (if (not-empty (:children node))
      (doall (map (fn [x] (recur-children x new-total)) (:children node)))
      new-total)))


(defn recur-links [parent child total]
  ;(println "called with child" child)
  (if (empty? (:children child))
    (conj total (conj total [:line {:x1 (:cx parent)
                                    :y1 (:cy parent)
                                    :x2 (:cx child)
                                    :y2 (:cy child)}]))
    (for [c (:children child)]
      (recur-links child c total))))

(defn links []
  (let [data (re-frame/subscribe [:all-data])]
    [:line {:x1 0
            :y1 0
            :x2 100
            :y2 100}]))

(defn svg-body []
  (let [data (re-frame/subscribe [:all-data])]
    (fn []
      [:svg.hyperview {:shape-rendering "crispEdges"
                       :text-rendering "optimizeLegibility"
                       :width (:width @dimensions)
                       :height (:height @dimensions)
                       :id "canvas"}
       [plane-guides]
       [:g {:transform center-str}
        (recur-links nil @data [:g.links])
        ]
       [:g {:transform center-str}
        (recur-children @data [:g.nodes])]])))


(defn main-panel []
  (let [name (re-frame/subscribe [:name])
        data (re-frame/subscribe [:all-data])]
    (fn []
      [:div

       ;[node-rings]
       [svg-body]
       (table/edn->hiccup @data)
       ;[radial/main-panel]
       ])))

(enable-console-print!)

(ns hypermodel.radial
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

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

(defn links []
  [:g
   (for [tier (range 1 5)]
     [link tier])])

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

(defn svg-body []
  [:svg.hyperview {:shape-rendering "crispEdges"
                   :text-rendering "optimizeLegibility"
                   :width (:width @dimensions)
                   :height (:height @dimensions)
                   :id "canvas"}
   [plane-guides]
   ;[planes]
   [links]
   [dr]
   [model]])

(defn make-tree-layout []
  (-> js/d3
      .-layout
      .tree
      (.size #js [360 150])
      (.separation (fn [a b]
                     (if (= (.-parent a) (.-parent b))
                       1
                       2)))))

(defn make-diagonal []
  (-> js/d3
      .-svg
      .-diagonal
      .radial
      (.projection (fn [d]
                     (clj->js [(.-y d) (* (/ (.-x d) 180) (.-PI js/Math))])
                     ;(clj->js [1 1])
                     ))))

(defn make-svg []
  (-> js/d3
      (.select "body")
      (.append "svg")
      (.attr "width" 500)
      (.attr "height" 500)
      (.append "g")
      (.attr "transform" "translate(250,250)")))



(defn diag []
  (fn []
    (let [tree (make-tree-layout)
          diagonal (make-diagonal)
          svg (make-svg)]
      (-> js/d3
          (.json "http://localhost:9001/flare.json"
                 (fn [error root]
                   (let [nodes (.nodes tree root)
                         links (.links tree nodes)]
                     ;(println "nodes" nodes)
                     (-> svg
                         (.selectAll ".link")
                         (.data links)
                         .enter
                         (.append "path")
                         (.attr "class" "link")
                         (.attr "d" diagonal))
                     (let [node (-> svg
                                    (.selectAll ".node")
                                    (.data nodes)
                                    .enter
                                    (.append "g")
                                    (.attr "class" "node")
                                    (.attr "transform" (fn [d]

                                                         (str "rotate("
                                                              (- (.-x d) 90)
                                                              ")translate("
                                                              (.-y d)
                                                              ")")
                                                         (str "translate("
                                                              (.-y d)
                                                              ")"))))]
                       (-> node
                           (.append "circle")
                           (.attr "r" 4.5))
                       (-> node
                           (.append "text")
                           (.attr "dy" ".31em")
                           (.text (fn [d] (.-name d))))
                       )
                     ))))
      [:div "diag"])))

(defn main-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
    (println "got it" (* 180 (.-PI js/Math)))


      [:div
       [diag]
       ;[node-rings]
       ;[svg-body]
       ])))
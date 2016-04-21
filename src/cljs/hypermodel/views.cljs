(enable-console-print!)

(ns hypermodel.views
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


(def plane-scale (-> js/d3 .-scale .linear
                     (.range #js [0 (/ (- (:height @dimensions) 100) 2)])
                     (.domain #js [0 5])))

(println "scales" (map plane-scale (range 1 4)))

(defn plane-guide []
  (fn [radius]
    ;(println "plane got r" r)
    [:circle {:stroke-dasharray "3, 3"
              :r radius}]))

(defn plane-guides []
  (fn []
    [:g.planes {:transform center-str}
     (for [p (map plane-scale (range 1 5))]
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
    (println "DOT GOT" x)
    [:g.node {:transform (str "rotate(" (* index (/ 360 total)) ")")}
     [:line {:x1 0 :y1 0 :x2 0 :y2 (* -1 (plane-scale tier))}]
     [:circle {:cx 0
               ;:fill "red"
               :cy (* -1 (plane-scale tier))
               ;:r (- 60 (* tier 20))
               ;:r (* 60 (/ 1 tier))
               :r (* 60 (/ 1 (inc tier)))
              ;  :r 20
               }]
     [:text {:y (* -1 (plane-scale tier))
             :text-anchor "middle"} (str (name label))]]))



(defn some-plane [tier]
  (let [nodes-on-tier (re-frame/subscribe [:tiers (keyword (str tier))])]
    (fn []
      (println "nodes-on-tier" tier @nodes-on-tier)
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
      (println "nodes-on-tier" tier @nodes-on-tier)
      ;(println "GETTING" (nth @nodes-on-tier 0))
      (println "center-str" center-str)
      [:g.tier {:transform center-str}
       (loop [idx 0 total [:g]]
         (if (= idx (count @nodes-on-tier))
           total
           (recur (inc idx) (conj total [dot {:label (:self (nth @nodes-on-tier idx))
                                              :tier tier
                                              :index (inc idx)
                                              :total (count @nodes-on-tier)}]))))])))

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


(defn svg-body []
  [:svg.hyperview {:shape-rendering "crispEdges"
                   :text-rendering "optimizeLegibility"
                   :width (:width @dimensions)
                   :height (:height @dimensions)
                   :id "canvas"}
   [plane-guides]
   [planes]
   [dr]
   [model]])


(defn main-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div
       [svg-body]])))

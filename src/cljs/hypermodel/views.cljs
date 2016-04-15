(enable-console-print!)

(ns hypermodel.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(def dimensions (reagent/atom {:width 400
                               :height 400}))

(def center-str
  (str "translate("
       (/ (:width @dimensions) 2)
       ","
       (/ (:height @dimensions) 2)
       ")"))


(def plane-scale (-> js/d3 .-scale .linear
                     (.range #js [0 (/ (- (:height @dimensions) 100) 2)])
                     (.domain #js [1 3])))

(println "scales" (map plane-scale (range 1 4)))

(defn plane-guide []
  (fn [radius]
    ;(println "plane got r" r)
    [:circle {:stroke-dasharray "3, 3"
              :r radius}]))

(defn plane-guides []
  (fn []
    [:g.planes {:transform center-str}
     (for [p (map plane-scale (range 1 4))]
       [plane-guide p])]))


(defn node []
  (fn [details]
    [:g.node
     [:circle {:r 40}]
     [:text {:text-anchor "middle"}
      (:node details)]]))

(def dots ["A" "B" "C" "D" "E"])

(defn dot []
  (fn [{:keys [label tier index total]}]
    (println "creating dot for label" label)
    [:g.node {:transform (str "rotate(" (* index (/ 360 total)) ")")}
     [:line {:x1 0 :y1 0 :x2 0 :y2 (* -1 (plane-scale tier))}]
     [:circle {:on-click #(re-frame/dispatch [:click-node label tier])
               :cx 0
               :cy (* -1 (plane-scale tier))
               :r 20}]
     [:text {:y (* -1 (plane-scale tier))
             :text-anchor "middle"} (str label)]]))

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

(defn planes []
  [:g
   (for [tier (range 1 4)]
     [some-plane tier])])

(defn model []
  (let [model (re-frame/subscribe [:model])]
    [:div (str @model)]))


(defn svg-body []
  [:svg.hyperview {:shape-rendering "crispEdges"
                   :text-rendering "optimizeLegibility"
                   :width (:width @dimensions)
                   :height (:height @dimensions)
                   :id "canvas"}
   [plane-guides]
   [planes]
   [model]])

(defn data-viewer []
  (let [data (re-frame/subscribe [:selectable])]
    (fn []
      [:div
       [:h4 "Data"]
       (if-not @data
         [:span "(Nothing available.)"]
         [:span (str @data)])])))

(defn main-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.row
       [:div.col-xs-6
        [svg-body]]
       [:div.col-xs-6]
       [data-viewer]])))

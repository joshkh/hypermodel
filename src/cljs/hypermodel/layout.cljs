(ns hypermodel.layout
  (:require [clojure.zip :as zip]))

(enable-console-print!)

(def RADIUS 80)
(def THETA (.-PI js/Math))
(def PI (.-PI js/Math))

(def modelv ['({:self :A, :parent nil})
             '({:self :B, :parent :A}
                {:self :C, :parent :A}
                {:self :D, :parent :A}
                {:self :E, :parent :A})])

(defn place-on-edge [origin-x origin-y radius angle]
  {:x (+ origin-x (* radius (Math/cos (* angle (/ (.-PI js/Math) 180)))))
   :y (+ origin-y (* radius (Math/sin (* angle (/ (.-PI js/Math) 180)))))})





(defn containment-circle [tier]
  (let [m 2
        p1 (/ THETA 2)
        p2 (/ THETA m)
        p3 (/ THETA (* 2 m))
        result (- PI (+ p1 p2 p3))]
    (println "RESULT" result))
  )

;(defn evenly-space [tier]
;  ;(println "evenly spacing" tier)
;  (let [chunks (/ 360 (count tier))]
;    (map-indexed (fn [index node]
;                   (let [degrees (* chunks (inc index))
;                         {x :x y :y} (place-on-edge 0 0 RADIUS degrees)]
;                     (assoc node :r 5
;                                 :cx x
;                                 :cy y
;                                 :a degrees))) tier)))



;(defn evenly-space [tier]
;  ;(println "evenly spacing" tier)
;  (let [chunks (/ 360 (count tier))]
;    (map-indexed (fn [index node]
;                   (let [{x :x y :y} (point 0 0 RADIUS (/ (* 2 index PI) (count tier)))]
;                     (println "first x y" x y)
;                     (assoc node :r 5
;                                 :cx x
;                                 :cy y))) tier)))


(defn center-tier [tier]
  (map (fn [node]
         (assoc node :a 0
                     :cx 0
                     :cy 0
                     :r 5)) tier))

;(defn doit [parents tiers]
;  (println "doing it" tiers)
;  (let [grouped-parents (group-by :self parents)]
;    (println "parents" grouped-parents)
;    (map-indexed (fn [idx node]
;                   ;(println "parent has" (first ((:parent node) grouped-parents)))
;                   (let [parent (first ((:parent node) grouped-parents))
;                         m (count tiers)
;                         p1 (/ THETA 2)
;                         p2 (/ (* THETA idx) m)
;                         p3 (/ THETA (* 2 m))
;                         result (- PI (+ p1 p2 p3))
;                         r 60
;                         {x :x y :y} (point (:cx parent) (:cy parent) r result)]
;                     (println "working with parent" parent)
;                     (assoc node :cx x
;                                 :cy y
;                                 :r 5)))
;                 tiers)))

;(defn position [tiers]
;  (reduce (fn [total next-tier]
;            (cond
;              (= next-tier 0) (conj total (center-tier (nth tiers next-tier)))
;              (= next-tier 1) (conj total (evenly-space (nth tiers next-tier)))
;              :else (conj total (doit (nth total (dec next-tier))
;                                      (nth tiers next-tier)))))
;          [] (range 0 (count tiers))))

(defn layout [tiers]
  ;(println "POSITIONING")
  ;(position tiers)
  )

;(defn graph-zipper [m]
;  (zip/zipper
;    (fn [x] true)
;    (fn [x] (seq (:children x)))
;    (fn [x children]
;      (if (map? x)
;        (into {} children)
;        (assoc x 1 (into {} children))))
;    m))





(defn graph-zipper [m]
  (zip/zipper
    (fn [x] true)
    (fn [x] (seq (:children x)))
    (fn [x children]
      (assoc x :children children))
    m))

;(def z (graph-zipper data))

;(defn x [tier]
;  ;(println "evenly spacing" tier)
;  (let [chunks (/ 360 (count tier))]
;    (map-indexed (fn [index node]
;                   (let [{x :x y :y} (point 0 0 RADIUS (/ (* 2 index PI) (count tier)))]
;                     (println "first x y" x y)
;                     (assoc node :r 5
;                                 :cx x
;                                 :cy y))) tier)))

(defn center-root [loc]
  (-> loc
      (zip/edit #(assoc % :cx 0
                          :cy 0))))

(defn point [origin-x origin-y radius angle]
  {:x (+ origin-x (* radius (Math/cos angle)))
   :y (+ origin-y (* radius (Math/sin angle)))})

(defn place-on-circle [magnitude index node]
  (let [{x :x y :y} (point 0 0 RADIUS (* index (/ (* 2 PI) magnitude)))]
    (assoc node :r 5
                :cx x
                :cy y)))

(defn space-evenly
  "Recursively and evenly space children of the root of a tree
  around a circle. Expects a location in a zipper and the locations
  index within its siblings (defaults to 1).
  This is used to calculate its polar coordinates."
  [z & [index]]
  (let [index (if index index 0)
        magnitude (-> z zip/up zip/children count)
        updated (-> z (zip/edit (partial place-on-circle magnitude index)))]
    (if-let [more (zip/right updated)]
      (space-evenly more (inc index))
      (zip/leftmost updated))))

(def data {:name :A
           :children [
                      {:name :A1
                       :children [
                                  {:name :X
                                   :children []}
                                  {:name :Y
                                   :children []}
                                  ]}
                      {:name :A2
                       :children []}
                      ;{:name :D
                      ; :children [{:name :1111
                      ;             :children []}
                      ;            {:name :2222
                      ;             :children []}]}
                      {:name :A3
                       :children []}

                      ;{:name :A4
                      ; :children []}

                      ]})

(defn talk [loc]
  (println "talk called wiht loc" (zip/node loc))
  (loop [node loc]
    (let [updated (zip/edit node (fn [x] (assoc x :TEST 1)))]
      (if (nil? (zip/right updated))
        updated
        (recur (zip/right updated))))))

(defn ray-angle [grand-parent parent]
  ;(println "grand-parent" grand-parent)
  (.atan2 js/Math (- (:cy grand-parent) (:cy parent))
          (- (:cx grand-parent) (:cy parent))))

(defn place-kids [loc]
  ;(println "place kids got loc" (zip/node loc))
  (loop [loc loc
         idx 1]
    (let [parent (-> loc zip/up zip/node)
          grand-parent (-> loc zip/up zip/up zip/node)
          THETA (ray-angle grand-parent parent)
          m (-> loc zip/up zip/children count)
          p1 (/ THETA 2)
          p2 (/ (* THETA (inc idx)) m)
          p3 (/ THETA (* 2 m))
          result (- PI (+ p1 p2 p3))
          r 60
          {x :x y :y} (point (:cx parent) (:cy parent) r result)]
      ;(println "RAY ANGLE " THETA)
      (let [updated-loc (zip/edit loc (fn [n] (assoc n :cx x
                                                       :cy y
                                                       :r 5)) )]
        ;(println "updated-loc" updated-loc)
        (if (nil? (-> updated-loc zip/right))
          updated-loc
          (recur (-> updated-loc zip/right) (inc idx)))))))

(defn recur-children [loc]
  (let [parent (-> loc zip/up zip/node)]
    (loop [loc loc]
      (let [self (-> loc zip/node)]
        ;(println "ANGLE" (.atan2 js/Math (- (:cy self) (:cy parent)) (- (:cx self) (:cx parent))))
        (if (nil? (-> loc zip/right))
          loc
          (recur (-> loc zip/right)))))))

(defn space [data]
  (-> (graph-zipper data)
      center-root
      zip/down
      space-evenly
      recur-children
      zip/root))


(defn reduce-children [loc]
  (reduce (fn [z next]
            (println "has next zip" (-> z zip/node))
            z)
          loc [1 2 3])
  loc)

(defn test-space [data]
  (-> (graph-zipper data)
      center-root
      zip/down
      reduce-children))

;(println "test-space" (test-space data))

(def x {:self :A
        :cx 0
        :cy 0
        :children ({:self :C
                    :children ()
                    :banana 1}
                    {:self :B
                     :children ({:self :D
                                 :children ()
                                 :banana 1}
                                 {:self :D
                                  :children []
                                  :banana 1})
                     :banana 1}
                    {:self :B
                     :children [{:self :D
                                 :children []}]
                     :banana 1}
                    {:self :C, :children [], :banana 1})}

(def m {:self     :A
        :cx       0
        :cy       0
        :children [{:self     :B
                    :children [{:self     :D
                                :children []}]}
                   {:self     :C
                    :children []}]})


  )
(defn bfirst [m & siblings]
  (let [{:keys [self children]} m]
    (let [placed-children (map-indexed (fn [idx child]
                                         (assoc child :banana 1)) children)]
      (reduce (fn [new-m next]
                (update new-m :children conj (bfirst next)))
              (assoc m :children placed-children) placed-children)
      ;(reduce (fn [total next]
      ;          ()))
      )))

(println "res" (bfirst m))

;(println (seq m))


;(recur-map data)
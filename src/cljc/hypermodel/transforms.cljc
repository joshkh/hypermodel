(ns hypermodel.transforms)


(def test-model {:A {:C {:D {:F {}
                             :G {}}
                         :E {}}
                     :B {}}})


(def test-vec [:Z
               [:1
                [:12] [:2]]
               [:A
                [:AB
                 [:BC] [:BD]]]])

(def test-list '(:A (:B (:C) (:D))
                  (:W (:X) (:Y))))


(def flat-model
  {:A {:collections {:B {:referenceType "B"}
                     :X {:referenceType "X"}}}
   :B {:collections {:Y {:referenceType "Y"}
                     :C {:referenceType "C"}
                     :E {:referenceType "E"}}}
   :C {:collections {:D {:referenceType "D"}}}
   :D {:collections {:E {:referenceType "E"}}}
   :E {:collections {:A {:referenceType "A"}}}})



(defn children [m parent]
      (map (fn [c] {:self (keyword c)
                    :parent parent}) (map (fn [[_ t]] (:referenceType t)) (get-in m [parent :collections]))))

(defn tree [start degrees]
  (loop [stack [(list {:self start :parent nil})] x 1]
    (if (<= x degrees)
      (recur (conj stack (mapcat (fn [c] (children flat-model c)) (map :self (last stack)))) (inc x))
      stack)))





(defn children-old [m v]
      (map (fn [[id & children]] id) (seq (get-in m v))))

(defn siblings [m v]
      (dissoc (get-in m v) (last v)))

(defn submap [m v]
      (assoc {} (last v) (get-in m v)))



(defn pivot
      "Pivot a map around a nested key so that the key becomes the root element.
      Example:
      (pivot {:A{:B1 {:C1 {} :D1 {}} :B2 {:C2 {:D2 {}}}}} [:A :B2])
      => {:B2 {:C2 {:D2 {}}, :A {:B1 {:C1 {}, :D1 {}}}}}"
      [m ks]
      (loop [mp (submap m ks)
             idx (dec (dec (count ks)))]
            (if-let [adjusted (assoc-in mp
                                        (reverse (subvec ks idx (count ks)))
                                        (dissoc (get-in m (subvec ks 0 (inc idx))) (get ks (inc idx))))]
                 (if (< idx 1) adjusted (recur adjusted (dec idx)))
                 nil)))


(def model ['({:self :A, :parent nil})
            '({:self :B, :parent :A} {:self :X, :parent :A})
            '({:self :Y, :parent :B} {:self :C, :parent :B} {:self :E, :parent :B})
            '({:self :D, :parent :C} {:self :A, :parent :E})
            '({:self :E, :parent :D} {:self :B, :parent :A} {:self :X, :parent :A})])

(defn radial [origin-x origin-y radius angle]
  {:x (+ origin-x (* radius (Math/cos angle)))
   :y (+ origin-y (* radius (Math/sin angle)))})
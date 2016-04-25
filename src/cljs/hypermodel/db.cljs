(ns hypermodel.db)

(def default-db
  {:name  "hypermodel"

   :new {:name :A
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

                    ]}

   ;:new ['({:self :A, :parent nil})
   ;      '({:self :B, :parent :A}
   ;         {:self :C, :parent :A}
   ;         {:self :D, :parent :A}
   ;         {:self :E, :parent :A})
   ;      '({:self :F, :parent :B}
   ;         {:self :G, :parent :B})]

   ;:untested [{:name "A"
   ;            :children [{:name "B"
   ;                        :children [{:name "X"
   ;                                    :children [{:name "BOB"
   ;                                                :children []}]}]}
   ;                       {:name "C"
   ;                        :children [{:name "LAST"
   ;                                    :children []}]}]}]
   :untested {:name "A"
              :children [{:name "B"
                          :children []}
                         {:name "C"
                          :children [{:name "F"
                                      :children []}]}
                         {:name "D"
                          :children []}]}


   ;:new ['({:self :A, :parent nil})
   ;      '({:self :B, :parent :A} {:self :X, :parent :A})
   ;      '({:self :Y, :parent :B} {:self :C, :parent :B} {:self :E, :parent :B})
   ;      '({:self :D, :parent :C} {:self :A, :parent :E})
   ;      '({:self :E, :parent :D} {:self :B, :parent :A} {:self :X, :parent :A})]

   :tiers {:1 [{:node "CORE"}]
           :2 [{:node   "B2"
                :parent "A"}
               {:node   "C2"
                :parent "A"}
               {:node   "D2"
                :parent "A"}]
           :3 [{:node   "E3"
                :parent "A"}
               {:node   "F3"
                :parent "A"}
               {:node   "G3"
                :parent "A"}
               {:node   "H3"
                :parent "A"}
               {:node   "I3"
                :parent "A"}
               {:node   "J3"
                :parent "A"}]}

   :model-positioned [:A
                      [:B
                       [:C
                        [:D []]
                        [:E []]]
                       [:F
                        [:G []]
                        [:H []]]]
                      [:I
                       [:J []]]]


   :cursor [:A :B]

   :model-map {:A {:C {:D {:F {}
                           :G {}}
                       :E {}}
                   :B {}}}
   :model {}})

(defn children-at-path [m v]
  (map (fn [[id & children]] id) (seq (get-in m v))))


(defn pivot [m v]
  (loop [sorted (assoc {} (last v) (get-in m v))
         path v]
    ; [:A :C :X]
    (println "first map" sorted)
    (if-let [parent (last (butlast path))]
      (do
        (println "associng in" path)
        (recur
          (assoc-in sorted (conj (reverse path) parent) (dissoc (get-in m (butlast path)) (last path)))
          (butlast path)))
      sorted)))

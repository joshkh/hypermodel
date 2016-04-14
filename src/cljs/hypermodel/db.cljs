(ns hypermodel.db)

(def default-db
  {:name "hypermodel"
   :tiers {:1 [{:node "A1"}]
           :2 [{:node "B2"
                :parent "A"}
               {:node "C2"
                :parent "A"}
               {:node "D2"
                :parent "A"}]
           :3 [{:node "E3"
                :parent "A"}
               {:node "F3"
                :parent "A"}
               {:node "G3"
                :parent "A"}
               {:node "H3"
                :parent "A"}
               {:node "I3"
                :parent "A"}
               {:node "J3"
                :parent "A"}]}
   :model {:node "Gene"
           :children [{:node "B"}
                      {:node "C"}]}})



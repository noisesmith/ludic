(ns org.noisesmith.ludic-test
  (:require [clojure.test :as test :refer [is testing deftest]]
            [org.noisesmith.ludic.protocol :as l-]
            [org.noisesmith.ludic :as l]))

(defn null-rule
  []
  (reify l-/Rule
    (ready [this state] false)
    (run [this picked state] state)
    (execute [this picked state-a state-b])))

(deftest simple-rule-test
  (is (null-rule)))

(defn null-game
  []
  (reify
    l/GameBoard
    (activate [this] (assoc this :enabled nil))
    l-/GameBoard
    (enabled [this] ())
    (state [this] {})
    (transition [this message] this)
    (tick [this] this)
    (fire [this])
    (clock [this] 0)))

(deftest simple-game-test
  (is (null-game)))

(deftest game-test
  (let [log (volatile! [])
        rules [(reify
                 l/Rule
                 (reads [this] [])
                 (consumes [this] [:a])
                 (produces [this] [:a])
                 l-/Rule
                 (ready [this state]
                   (even? (:a state)))
                 (run [this _ state]
                   (update state :a inc))
                 (execute [this _ old-state new-state]
                   (vswap! log conj {:rule 1
                                     :action [(:a old-state)
                                              '->
                                              (:a new-state)]})))
               (reify
                 l/Rule
                 (reads [this] [])
                 (consumes [this] [:a])
                 (produces [this] [:a])
                 l-/Rule
                 (ready [this state]
                   (odd? (:a state)))
                 (run [this _ state]
                   (update state :a * 2))
                 (execute [this _ game new-state]
                   (vswap! log conj {:rule 2
                                     :action [(:a (:game-state game))
                                              '->
                                              (:a new-state)]})))]
        game (l/activate
              (l/map->Game
               {:game-state {:a 1}
                :rules rules
                :t 0}))
        boards (iterate (comp l/activate l-/tick) game)]
    (is (= (first (l-/enabled game))
           (second rules))
        "the second rule should apply")
    (is (= (first (l-/enabled (nth boards 1)))
           (first rules))
        "after an iteration, the first rule should apply")
    (l-/fire (nth boards 4))
    (is (= [{:rule  2 :action [7 '-> 14]}] @log)
        "the fifth iteration should run rule 2 7 -> 14")))

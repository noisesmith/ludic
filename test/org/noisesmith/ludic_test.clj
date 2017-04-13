(ns org.noisesmith.ludic-test
  (:require [clojure.test :as test :refer [is testing deftest]]
            [org.noisesmith.ludic :as l]))

(defn null-rule
  []
  (reify l/Rule
    (ready [this state] false)
    (run [this picked state] state)
    (execute [this picked state-a state-b])))

(deftest simple-rule-test
  (is (null-rule)))

(defn null-game
  []
  (reify l/GameBoard
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
        rules [(reify l/Rule
                 (ready [this state]
                   (even? (:a state)))
                 (run [this _ state]
                   (update state :a inc))
                 (execute [this _ old-state new-state]
                   (vswap! log conj {:rule 1
                                     :action [(:a old-state)
                                              '->
                                              (:a new-state)]})))
               (reify l/Rule
                 (ready [this state]
                   (odd? (:a state)))
                 (run [this _ state]
                   (update state :a * 2))
                 (execute [this _ old-state new-state]
                   (vswap! log conj {:rule 2
                                     :action [(:a old-state)
                                              '->
                                              (:a new-state)]})))]
        game (l/map->Game
              {:game-state {:a 1}
               :rules rules
               :t 0})
        boards (iterate l/tick game)]
    (is (= (l/enabled game)
           (drop 1 rules))
        "the second rule should apply")
    (is (= (l/enabled (nth boards 1))
           (take 1 rules))
        "after an iteration, the first rule should apply")
    (l/fire (nth boards 4))
    (is (= [{:rule  2 :action [7 '-> 14]}] @log)
        "the fifth iteration should run rule 2 7 -> 14")))

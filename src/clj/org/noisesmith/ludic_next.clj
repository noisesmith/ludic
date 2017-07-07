(ns org.noisesmith.ludic-next
  (:require [org.noisesmith.ludic :as ludic]))

(defprotocol GameBoard
  (activate [this]
    "marks rules as active in this context, and tracks their context data"))

(defprotocol Rule
  (consumes [this]
    "the parts of a game record it removes data from")
  (produces [this]
    "the parts of a game record it puts data into"))

(defrecord Game
           [game-state rules t]
  GameBoard
  (activate [this]
    (assoc this
           :enabled
           (first
            (sequence (comp
                       (map (juxt identity
                                  (fn check-ready
                                    [rule]
                                    (ludic/ready
                                     rule
                                     (select-keys game-state
                                                  (consumes rule))))))
                       (filter second))
                      rules))))
  ludic/GameBoard
  (enabled [this]
    (:enabled this))
  (state [this]
    game-state)
  (transition [this message x1]
    (message this x1))
  (tick [this]
    (if-let [[rule data] (:enabled this)]
      (-> this
          (update :game-state
                  (fn [s]
                    (merge s
                           (select-keys (ludic/run rule data s)
                                        (produces rule)))))
          (update :t inc))
      this))
  (fire [this]
    (let [upcoming (-> this (ludic/tick) (:game-state))]
      (when-let [[rule data] (:enabled this)]
        (ludic/execute rule data this upcoming))
      (assoc this :enabled nil)))
  (clock [this]
    t))

(defn one-step
  [game callback spy]
  (let [next-game (delay (-> game
                             (activate)
                             (ludic/tick)
                             (doto ludic/fire)
                             (spy ::cycle)))]
    (cond
      (not (callback game ::cycle))
      [::done game]
      (= (ludic/clock game)
         (ludic/clock (force next-game)))
      [::continue game]
      :else
      [::incomplete (force next-game)])))

(defn step-until-idle
  "iterates fire / tick until no progress is possible"
  [game spy callback]
  (let [[status next-game] (one-step game callback spy)]
    (case status
      ::done [::done next-game]
      ::incomplete (recur next-game spy callback)
      ::continue [::continue next-game])))

(defn main-pass
  [game get-command callback dispatch spy]
  (let [command (get-command game)]
    (if-not (callback game command)
      [::done game]
      [::continue (-> game
                      (spy ::pre-dispatch command)
                      (ludic/transition dispatch command)
                      (spy ::post-dispatch command))])))

(defn main-loop
  ([game get-command dispatch]
   (main-loop game get-command dispatch (constantly nil)))
  ([game get-command dispatch callback]
   (let [spy (fn spy [& context]
               (apply ludic/spy (:debug game) context))
         [status next-state] (main-pass game get-command callback dispatch spy)
         norm (delay (step-until-idle next-state spy callback))
         norm-status (delay (first (force norm)))
         normalized (delay (second (force norm)))]
     (cond
       (= status ::done)
       next-state
       (= (force norm-status) ::done)
       (force normalized)
       :else
       (recur (force normalized)
              get-command
              dispatch
              callback)))))

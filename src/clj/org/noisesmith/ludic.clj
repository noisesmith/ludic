(ns org.noisesmith.ludic
  (:require [org.noisesmith.ludic.protocol :as proto]
            [clojure.string :as string]))

(defprotocol GameBoard
  (activate [this]
    "marks rules as active in this context, and tracks their context data"))

(defprotocol Rule
  (reads [this]
    "the parts of a game record it reads from")
  (consumes [this]
    "the parts of a game record it removes data from")
  (produces [this]
    "the parts of a game record it puts data into"))

(defn viz-rules
  "produces a graphiz .dot file, suitable for viewing via eg.
   dot -Tpng foo.dot > foo.png"
  [rules]
  (let [places (into #{}
                     (comp (mapcat #(list (.consumes %) (.produces %)))
                           cat
                           (map name))
                     rules)
        rule-str #(second (re-matches #"<<\| Cluster Rule (.*) \|>>"
                                      (str %)))
        transitions (map rule-str rules)
        flows (concat (for [rule rules
                            input (.consumes rule)]
                        [(name input) (rule-str rule)])
                      (for [rule rules
                            output (.produces rule)]
                        [(rule-str rule) (name output)]))]
    (string/join \newline
                 (concat
                  ["digraph G {"
                   "\tsubgraph places {"
                   "\t\tgraph [shape=circle,color=gray];"
                   "\t\tnode [shape=circle,fixedsize=true,width=1.5];"]
                  (for [place places]
                    (str "\t\t\"" place "\";"))
                  ["\t}"
                   "\tsubgraph transitions {"
                   "\t\tnode [shape=rect,height=0.2,width=2];"]
                  (for [transition transitions]
                    (str "\t\t\"" transition "\";"))
                  ["\t}"]
                  (for [flow flows]
                    (str "\t\"" (first flow) "\" -> \"" (second flow) "\";"))
                  ["}"]))))

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
                                    (proto/ready
                                     rule
                                     (select-keys game-state
                                                  (into (reads rule)
                                                        (consumes rule)))))))
                       (filter second))
                      rules))))
  proto/GameBoard
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
                           (select-keys (proto/run rule data s)
                                        (into (consumes rule)
                                              (produces rule))))))
          (update :t inc))
      this))
  (fire [this]
    (let [upcoming (-> this (proto/tick) (:game-state))]
      (when-let [[rule data] (:enabled this)]
        (proto/execute rule data this upcoming))
      (assoc this :enabled nil)))
  (clock [this]
    t)
  proto/Tracked
  (track [this]
    (when-let [tracker (:tracker this)]
      (tracker this))
    this))

(defn one-step
  [game callback spy]
  (let [next-game (delay (-> game
                             (activate)
                             (proto/tick)
                             (doto proto/fire)
                             (spy ::cycle)))]
    (cond
      (not (callback game ::cycle))
      [::done game]
      (= (proto/clock game)
         (proto/clock (force next-game)))
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
                      (proto/transition dispatch command)
                      (proto/track)
                      (spy ::post-dispatch command))])))

(defn main-loop
  ([game get-command dispatch]
   (main-loop game get-command dispatch (constantly nil)))
  ([game get-command dispatch callback]
   (let [spy (fn spy [& context]
               (apply proto/spy (:debug game) context))
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

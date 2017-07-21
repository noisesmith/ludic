(ns org.noisesmith.ludic.protocol
  "\"showing spontaneous and undirected playfulness\"
   let the games begin!
   Ludic is a library describing the passage of logical time as a deterministic
   fold over a series of inputs.")

;; A GameBoard knows which rules currently apply to its state, and invokes
;; rules to achieve its next state.
(defprotocol GameBoard
  (enabled [this]
    "the rules that are currently accessible")
  (state [this]
    "the data collections implementing the full state of the current game")
  (transition
    [this message]
    [this message x1]
    [this message x1 x2]
    [this message x1 x2 x3]
    [this message x1 x2 x3 x4]
    "an arbitrary transform of the GameBoard, must return a GameBoard")
  (tick [this]
    "runs a currently enabled rule and updates the clock")
  (fire [this]
    "executes all side-effecting consequences of the next tick")
  (clock [this]
    "the comparable data representing \"now\""))

;; A rule can tell you if it applies to a given state, how it would change the
;; state if it was run, and which side effects to execute if it is run.
(defprotocol Rule
  (ready [this state]
    "pseudo-boolean, is this rule implied by the current state?")
  (run [this accepted state]
    "applies this rule to the state, returning a new state")
  (execute [this accepted old-state new-state]
    "runs all side effects implied by this rule, given the state"))

(defn spy
  [handler v & args]
  (when handler
    (apply handler (concat args [v])))
  v)

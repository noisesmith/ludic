# LUDIC

## showing spontaneous and undirected playfulness

Ludic is a library describing the passage of logical time as a deterministic
fold over a series of inputs.

![otter, hard at work](otter-stacking.jpg)

The GameBoard protocol allows iterating game states and introspecting on
which rules *would* execute, without executing the side effects attached to
the rules until you choose to fire the transition.

The methods are:
* enabled -> returning a list of rules that could be applied
* state -> data representing the game at a moment in time
* transition -> applies a message to a game, returning a new game (eg. it could modify rules)
* tick -> runs the first enabled rule, updates the clock and game state
* fire -> executes side effects of the next tick
* clock -> returns a comparable, the ordered position of this state in a game

The Rule protocol describes the rules that could be applied.
* ready -> returns true if this rule can act on the game
* run -> takes a game state, returns a game state
* execute -> effects some side effect of this rule on the external world, if any

While the protocol names reference games, they are applicable to any domain problem where you have a series of events, data describing the current state, and rules that describe transformations to apply based on state reached. Which could include but is not limited to inter-process coordination based on messages on a channel, simulations, or implementing agents with time travel.

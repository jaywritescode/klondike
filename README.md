# klondike

The classic solitaire game.

### what it is

Klondike is the Microsoft solitaire game that came with old versions of Windows. It's also a similar game that can be played with physical playing cards.

### how to build it

`gradle build` should do the trick.

### how to run it

Download the zip file, extract all its zippy goodness, then run `./bin/klondike`.

### how to use it (i.e., the worst user interface ever)

The UI is built with a curses-like substitute library after numerous failures with Swing, JavaFX, Apache Batik, etc., etc. So, I know it's ugly, you don't have to open an issue about it.

The `A` and `D` keys cycle the pointer along the deck, the waste, each tableau, and the foundations as a unit. `W` and `S` move the pointer up or down a tableau. 

`[Space]` is the action key. Pressing `[Space]` with the pointer pointing to...

+ ... __the deck__ will deal the next card(s) to the waste.
+ ... __the waste__ will "pick up" the current card if there's not one already picked up, or de-pick up the current card if there is one.
+ ... __a card in a tableau__ will "pick up" all that card and all the cards on top of it if there's nothing already picked up, or "drop" the current cards if that's a legal move.
+ ... __the foundations__ will "drop" the current card on the appropriate foundation, if it's a legal move.

There are also command line options to customize the game to your particular liking:

+ __--deal-one__ deals one card at a time instead of the standard three
+ __--passes=[num]__ restarts the deck at most `num` times before game over, assuming that `num` is either 1 or 3. Otherwise, you can pass through the deck infinitely!<sup>1</sup>

---

<sup>1</sup> Only 24 times, really. The game ends if you pass through the deck without moving any cards to a tableau or foundation.

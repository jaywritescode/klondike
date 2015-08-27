# klondike

The classic solitaire game.

### what it is

Klondike is the Microsoft solitaire game that came with old versions of Windows. It's also a similar game that can be played with physical playing cards.

### how to build it

`gradle build` should do the trick.

### how to run it

Try `gradle run`. Having [Gradle](http://www.gradle.org) installed makes this process a lot smoother. `brew install gradle`.

### how to use it (i.e., the worst user interface ever)

The UI is built with a curses-like substitute library after numerous failures with Swing, JavaFX, Apache Batik, etc., etc. So, I know it's ugly, you don't have to open an issue about it.

The `A` and `D` keys cycle the pointer along the deck, the waste, each tableau, and the foundations as a unit. `W` and `S` move the pointer up or down a tableau. 

`[Space]` is the action key. Pressing `[Space]` with the pointer pointing to...

+ ... __the deck__ will deal the next card(s) to the waste.
+ ... __the waste__ will "pick up" the current card if there's not one already picked up, or de-pick up the current card if there is one.
+ ... __a card in a tableau__ will "pick up" all that card and all the cards on top of it if there's nothing already picked up, or "drop" the current cards if that's a legal move.
+ ... __the foundations__ will "drop" the current card on the appropriate foundation, if it's a legal move.

package info.jayharris.klondike;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import info.jayharris.cardgames.*;

import java.util.*;

public class Klondike extends Observable {

    private Deck deck;
    private Waste waste;
    private final ArrayList<Tableau> tableaus;
    private final Map<Suit, Foundation> foundations;
    public final Rules rules;

    private int passes;
    private boolean didChange;          // keep track of whether we moved a card to a tableau
                                        // or to a foundation this round

    enum GameOver { GAME_OVER }

    public Klondike() {
        this(new Rules());
    }

    public Klondike(Rules rules) {
        deck = DeckUtils.createStandardDeck();

        tableaus = Lists.newArrayListWithCapacity(7);
        for (int i = 0; i < 7; ++i) {
            tableaus.add(new Tableau());
        }

        foundations = Maps.newHashMapWithExpectedSize(4);
        for (Suit suit : EnumSet.allOf(Suit.class)) {
            foundations.put(suit, new Foundation(suit));
        }

        waste = new Waste();
        this.rules = rules;

        passes = 0;
    }

    public void init() {
        deck.shuffle();

        System.err.println(deck);

        for (int i = 0; i < tableaus.size(); ++i) {
            for (int j = i; j < tableaus.size(); ++j) {
                tableaus.get(j).add(j == i ? deck.dealFaceUp() : deck.dealFaceDown());
            }
        }
    }

    public boolean isDeckEmpty() {
        return deck.isEmpty();
    }

    public boolean isGameOver() {
        if (!isDeckEmpty()) {
            return false;
        }
        if (!didChange || passes >= rules.getPasses() || Iterables.all(foundations.values(), pFoundationIsComplete)) {
            this.setChanged();
            this.notifyObservers(new GameOver());
            return true;
        }
        return false;
    }

    public boolean deal() {
        System.err.println("deal();");

        if (isDeckEmpty()) {
            return restartDeck();
        }
        else {
            for (int i = 0; i < rules.getDeal() && !isDeckEmpty(); ++i) {
                moveCardToWaste();
            }
            return true;
        }
    }

    /**
     * Move a card from the deck to the waste.
     *
     * @return {@code true}
     */
    public boolean moveCardToWaste() {
        Preconditions.checkState(!deck.isEmpty());
        return waste.add(deck.dealFaceUp());
    }

    /**
     * Move a card from the end of the waste to a tableau.
     *
     * @return {@code true} if the move is legal, {@code false} otherwise
     */
    public boolean moveFromWasteToTableau(Tableau tableau) {
        Card card = waste.peekLast();

        System.err.println("moveFromWasteToTableau(tableaus.get(" + whichTableau(tableau) + "));");

        if (tableau.isEmpty()) {
            if (card.getRank() == Rank.KING) {
                didChange = true;
                tableau.add(waste.removeLast());
                return true;
            }
            else {
                return false;
            }
        }
        else {
            Card target = tableau.peekLast();
            if (card.getRank() == target.getRank().lower() && card.getColor() == target.getColor().opposite()) {
                didChange = true;
                tableau.add(waste.removeLast());
                return true;
            }
            else {
                return false;
            }
        }
    }

    ///////////////////////////////////////
    // TEMPORARY
    ///////////////////////////////////////
    public int whichTableau(Tableau tableau) {
        for (int i = 0; i < tableaus.size(); ++i) {
            if (tableaus.get(i) == tableau) {
                return i;
            }
        }
        return -1;
    }
    ///////////////////////////////////////
    // TEMPORARY
    ///////////////////////////////////////

    /**
     * Move a card from the waste to the appropriate foundation.
     *
     * @return {@code true} if the move is legal, {@code false} otherwise
     */
    public boolean moveFromWasteToFoundation() {
        System.err.println("moveFromWasteToFoundation();");

        Card card = waste.peekLast();
        Foundation foundation = foundations.get(card.getSuit());

        if (foundation.isEmpty()) {
            if (card.getRank() == Rank.ACE) {
                didChange = true;
                foundation.add(waste.removeLast());
                return true;
            }
            else {
                return false;
            }
        }
        else {
            if (card.getRank().lower() == foundation.peekLast().getRank()) {
                didChange = true;
                foundation.add(waste.removeLast());
                return true;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Move a card from a tableau to the appropriate foundation.
     *
     * @param tableau the tableau
     * @return {@code true} if the move is legal, {@code false} otherwise
     */
    public boolean moveFromTableauToFoundation(Tableau tableau) {
        Preconditions.checkArgument(!tableau.isEmpty());

        System.err.println("moveFromTableauToFoundation(tableaus.get(" + whichTableau(tableau) + "));");

        Card card = tableau.peekLast();
        Foundation foundation = foundations.get(card.getSuit());

        if (foundation.accepts(card)) {
            didChange = true;
            foundation.add(tableau.removeLast());

            if (!tableau.isEmpty() && tableau.peekLast().isFacedown()) {
                tableau.peekLast().flip();
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Move one or more cards from one tableau to another.
     *
     * @param from the tableau that we're moving from
     * @param to the tableau that we're moving to
     * @param num the number of cards we're moving
     * @return {@code true} if the move is legal, {@code false} otherwise
     */
    public boolean moveFromTableauToTableau(Tableau from, Tableau to, int num) {
        Preconditions.checkArgument(!from.isEmpty());
        Preconditions.checkArgument(num > 0 && num <= from.countFaceup());

        System.err.println(String.format("moveFromTableauToTableaus(tableaus.get(%d), tableaus.get(%d), %d);", whichTableau(from), whichTableau(to), num));

        List<Card> moving = from.subList(from.size() - num, from.size());
        if (!to.accepts(moving.get(0))) {
            return false;
        }

        to.addAll(moving);
        for (int i = 0; i < num; ++i) {
            from.removeLast();
        }
        if (!from.isEmpty() && from.peekLast().isFacedown()) {
            from.peekLast().flip();
        }

        didChange = true;
        return true;
    }

    /**
     * Take the waste pile and turn it into the deck for the next round.
     *
     * @return {@code false} iff the game is over, otherwise {@code true}
     */
    public boolean restartDeck() {
        Preconditions.checkState(deck.isEmpty());

        ++passes;
        if (isGameOver()) {
            this.setChanged();
            this.notifyObservers(GameOver.GAME_OVER);
            return false;
        }

        deck.addAll(Collections2.transform(waste, new Function<Card, Card>() {
            public Card apply(Card input) {
                input.flip();
                return input;
            }
        }));

        waste.clear();
        didChange = false;
        return true;
    }

    protected Deck getDeck() {
        return deck;
    }

    protected Tableau getTableau(int index) {
        return tableaus.get(index);
    }

    protected Foundation getFoundation(Suit suit) {
        return foundations.get(suit);
    }

    protected Waste getWaste() {
        return waste;
    }

    protected Collection<Foundation> getFoundations() {
        return foundations.values();
    }

    static Predicate pTableauHasNoFacedown = new Predicate<Tableau>() {
        @Override
        public boolean apply(Tableau input) {
            return Iterables.all(input, input.pIsFaceDown);
        }
    };

    static Predicate pFoundationIsComplete = new Predicate<Foundation>() {
        @Override
        public boolean apply(Foundation input) {
            return input.isComplete();
        }
    };

    class Tableau extends LinkedList<Card> {
        Predicate pIsFaceDown = new Predicate<Card>() {
            @Override
            public boolean apply(Card input) {
                return input.isFacedown();
            }
        };

        /**
         * Is this a legal move?
         *
         * @param card the card that we're trying to add to the top of the tableau
         * @return {@code true} iff this is a legal move
         */
        public boolean accepts(Card card) {
            if (isEmpty()) {
                return card.getRank() == Rank.KING;
            }
            else {
                Card last = getLast();

                return card.getColor() == last.getColor().opposite() && card.getRank() == last.getRank().lower(Rank.SortType.ACE_LOW);
            }
        }

        /**
         * Count the number of face-up cards in the tableau.
         *
         * @return the number of face-up cards in the tableau.
         */
        public int countFaceup() {
            int count = 0;
            for (Iterator<Card> iter = descendingIterator(); iter.hasNext(); count++) {
                if (iter.next().isFacedown()) {
                    return count;
                }
            }
            return count;
        }

        /**
         * Are there any face-down cards in the tableau?
         *
         * @return {@code true} iff this tableau is empty or all cards in the tableau are face-up
         */
        public boolean hasNoFacedown() {
            return Iterables.all(this, Predicates.not(pIsFaceDown));
        }

        @Override
        public String toString() {
            if (isEmpty()) {
                return "[]";
            }

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0;;) {
                sb.append(get(i));
                if (++i == size()) {
                    return sb.append("]").toString();
                }
                sb.append(", ");
            }
        }
    }

    class Foundation extends LinkedList<Card> {
        final Suit suit;

        public Foundation(Suit suit) {
            this.suit = suit;
        }

        public boolean accepts(Card card) {
            Preconditions.checkArgument(!card.isFacedown());
            if (card.getSuit() == suit) {
                return card.getRank() == (isEmpty() ? Rank.ACE : peekLast().getRank().higher());
            }
            return false;
        }

        public boolean isComplete() {
            return size() == 13;
        }

        @Override
        public String toString() {
            return isEmpty() ? "[]" : peekLast().toString();
        }
    }

    /**
     * Waste.
     *
     * Cards are removed from the end of the waste, i.e. {@code waste[waste.length - 1]}.
     */
    class Waste extends LinkedList<Card> {
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("[]");
            if (this.isEmpty()) {
                return sb.toString();
            }

            for (int i = 1; i <= Math.min(this.size(), 6); ++i) {
                sb.insert(1, this.get(this.size() - i).toString());
                if (i >= this.size()) {
                    return sb.toString();
                }
                sb.insert(1, " ");
            }
            sb.insert(1, "...");
            return sb.toString();
        }
    }

    public static class Rules {
        enum Deal {
            DEAL_SINGLE(1), DEAL_THREE(3);

            public final int count;

            Deal(int count) {
                this.count = count;
            }
        }

        enum Passes {
            SINGLE(1), THREE(3), INFINITY(Integer.MAX_VALUE);

            public final int count;

            Passes(int count) {
                this.count = count;
            }
        }

        Deal deal;
        Passes passes;

        Rules() {
            this(Deal.DEAL_THREE, Passes.INFINITY);
        }

        Rules(Deal deal) { this(deal, Passes.INFINITY); }

        Rules(Passes passes) { this(Deal.DEAL_THREE, passes); }

        Rules(Deal deal, Passes passes) {
            this.deal = deal;
            this.passes = passes;
        }

        public int getDeal() {
            return deal.count;
        }

        public int getPasses() {
            return passes.count;
        }
    }
}

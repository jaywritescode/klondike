package info.jayharris.klondike;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import info.jayharris.cardgames.*;

import java.util.*;

public class Klondike {

    private Deck deck;
    private Waste waste;
    private final ArrayList<Tableau> tableaus;
    private final Map<Suit, Foundation> foundations;
    public final Rules rules;
    private boolean inProgress;

    private int passes;
    private boolean hasNoNextRound;

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
            foundations.put(suit, new Foundation());
        }

        waste = new Waste();
        this.rules = rules;
        inProgress = false;
    }

    public void init() {
        deck.shuffle();
        for (int i = 0; i < tableaus.size(); ++i) {
            for (int j = i; j < tableaus.size(); ++j) {
                tableaus.get(j).add(j == i ? deck.dealFaceUp() : deck.dealFaceDown());
            }
        }
    }

//    public boolean play() {
//        init();
//
//        passes = 0;
//        hasNoNextRound = true;
//        inProgress = true;
//
//        while (inProgress) {
//            if (app.getAction().doAction(this)) {
//                inProgress = !isGameOver();
//            }
//        }
//
//        // TODO: implement
//        return false;
//    }

    public boolean isDeckEmpty() {
        return deck.isEmpty();
    }

    public boolean isGameOver() {
        // TODO: implement
        return false;
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
     * Move a card from the waste to a tableau.
     *
     * @return {@code true} if the move is legal, {@code false} otherwise
     */
    public boolean moveFromWasteToTableau(Tableau tableau) {
        Card card = waste.peekLast();

        if (tableau.isEmpty()) {
            return card.getRank() == Rank.KING && tableau.add(waste.removeLast());
        }
        else {
            Card target = tableau.peekLast();
            return card.getRank() == target.getRank().lower() &&
                    card.getColor() == target.getColor().opposite() && tableau.add(waste.removeLast());
        }
    }

    /**
     * Move a card from the waste to the appropriate foundation.
     *
     * @return {@code true} if the move is legal, {@code false} otherwise
     */
    public boolean moveFromWasteToFoundation() {
        Card card = waste.peekLast();
        Foundation foundation = foundations.get(card.getSuit());

        if (foundation.isEmpty()) {
            return card.getRank() == Rank.ACE && foundation.add(waste.removeLast());
        }
        else {
            return card.getRank().lower() == foundation.peekLast().getRank() &&
                    foundation.add(waste.removeLast());
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
        Card card = tableau.peekLast();
        Foundation foundation = foundations.get(card.getSuit());

        return card.getRank().lower() == foundation.peekLast().getRank() &&
                foundation.add(tableau.removeLast());
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

        return true;
    }

    /**
     * Take the waste pile and turn it into the deck for the next round.
     *
     * @return true
     */
    public boolean restartDeck() {
        Preconditions.checkState(deck.isEmpty());

        deck.addAll(Collections2.transform(waste, new Function<Card, Card>() {
            public Card apply(Card input) {
                input.flip();
                return input;
            }
        }));
        waste.clear();
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

    class Tableau extends LinkedList<Card> {
        public boolean accepts(Card card) {
            if (isEmpty()) {
                return card.getRank() == Rank.KING;
            }
            else {
                Card last = getLast();

                return card.getColor() == last.getColor().opposite() && card.getRank() == last.getRank().lower(Rank.SortType.ACE_LOW);
            }
        }

        private int countFaceup() {
            int count = 0;
            for (Iterator<Card> iter = descendingIterator(); iter.hasNext(); count++) {
                if (iter.next().isFacedown()) {
                    return count;
                }
            }
            return count;
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
        @Override
        public String toString() {
            return isEmpty() ? "[]" : peekLast().toString();
        }
    }

    class Waste extends LinkedList<Card> {

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

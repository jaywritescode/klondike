package info.jayharris.klondike;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import info.jayharris.cardgames.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class KlondikeTest {

    Klondike klondike;

    @Before
    public void setUp() {
        klondike = new Klondike(/* mockActionSource */);
        klondike.init();
    }

    @Test
    public void testMoveCardToWaste() {
        ImmutableList<Card> originalDeck = ImmutableList.copyOf(klondike.getDeck());

        assertTrue(klondike.moveCardToWaste());
        assertEquals(
                originalDeck.subList(1, originalDeck.size()),
                klondike.getDeck());
        assertEquals(
                originalDeck.subList(0, 1),
                klondike.getWaste());

        assertTrue(klondike.moveCardToWaste());
        assertEquals(
                originalDeck.subList(2, originalDeck.size()),
                klondike.getDeck());
        assertEquals(
                originalDeck.subList(0, 2),
                klondike.getWaste());

        assertTrue(klondike.moveCardToWaste());
        assertEquals(
                originalDeck.subList(3, originalDeck.size()),
                klondike.getDeck());
        assertEquals(
                originalDeck.subList(0, 3),
                klondike.getWaste());

        // deal out the rest of the deck
        klondike.getDeck().clear();
        try {
            klondike.moveCardToWaste();
            fail("Expected IllegalStateException not thrown.");
        }
        catch(IllegalStateException e) {}
    }

    @Test
    public void testRestartDeck() {
        ImmutableList<Card> originalDeck = ImmutableList.copyOf(klondike.getDeck());

        // move all the cards to the waste
        while (!klondike.getDeck().isEmpty()) {
            klondike.moveCardToWaste();
        }

        assertTrue(klondike.restartDeck());
        assertTrue(klondike.getWaste().isEmpty());
        assertTrue(Iterables.all(klondike.getDeck(), new Predicate<Card>() {
            public boolean apply(Card input) {
                return input.isFacedown();
            }
        }));
        assertEquals(originalDeck, klondike.getDeck());
    }

    @Test
    public void testMoveFromWasteToTableau() {
        ImmutableList<Card> originalWaste, originalTableau;
        Card top, next, t;

        Klondike.Waste waste = klondike.getWaste();
        Klondike.Tableau empty = klondike.getTableau(0),
                tableau = klondike.getTableau(3);

        while ((t = tableau.peekLast()).getRank() == Rank.ACE ||
                t.getRank() == Rank.TWO || t.getRank() == Rank.THREE) {
            tableau.removeLast();
            tableau.add(Card.randomCard());
        }
        empty.clear();

        // seed the waste -- make sure we're moving cards off the correct end
        // of the list
        for (int j = 0; j < 3; j++) {
            klondike.moveCardToWaste();
        }
        originalWaste = ImmutableList.copyOf(waste);

        // the top card on the tableau
        top  = tableau.getLast();
        // a card that can go on top of the top card
        next = new Card(top.getRank().lower(), top.getColor() == Suit.Color.RED ? Suit.SPADES : Suit.DIAMONDS);

        waste.add(next);

        // valid move -- non-empty tableau, correct rank and color
        assertTrue(klondike.moveFromWasteToTableau(tableau));
        assertEquals(originalWaste, waste);
        assertEquals(next, tableau.peekLast());

        next = new Card(Rank.KING, Suit.CLUBS);
        waste.add(next);

        // valid move -- empty tableau, king
        assertTrue(klondike.moveFromWasteToTableau(empty));
        assertEquals(originalWaste, waste);
        assertEquals(ImmutableList.of(next), empty);

        empty.clear();
        while (next.getRank() == Rank.KING) {
            next = Card.randomCard();
        }
        waste.add(next);

        // invalid move -- empty tableau, non-king
        assertFalse(klondike.moveFromWasteToTableau(empty));
        assertEquals(originalWaste, waste.subList(0, 3));
        assertEquals(ImmutableList.of(next), waste.subList(3, 4));
        assertTrue(empty.isEmpty());
        waste.removeLast();

        originalTableau = ImmutableList.copyOf(tableau);
        top = tableau.peekLast();

        next = new Card(top.getRank().lower(), top.getColor() == Suit.Color.RED ? Suit.DIAMONDS : Suit.SPADES);
        waste.add(next);

        // invalid move -- non-empty tableau, correct rank but wrong color
        assertFalse(klondike.moveFromWasteToTableau(tableau));
        assertEquals(originalWaste, waste.subList(0, 3));
        assertEquals(ImmutableList.of(next), waste.subList(3, 4));
        assertEquals(originalTableau, tableau);
        waste.removeLast();

        while (next.getRank() == top.getRank().lower() || next.getColor() == top.getColor()) {
            next = Card.randomCard();
        }
        waste.add(next);

        // invalid move -- non-empty tableau, correct color but wrong rank
        assertFalse(klondike.moveFromWasteToTableau(tableau));
        assertEquals(originalWaste, waste.subList(0, 3));
        assertEquals(ImmutableList.of(next), waste.subList(3, 4));
        assertEquals(originalTableau, tableau);
    }

    @Test
    public void testMoveFromWasteToFoundation() {
        ImmutableList<Card> originalWaste, originalFoundation;
        Card top, next;

        Klondike.Waste waste = klondike.getWaste();
        Klondike.Foundation foundation = klondike.getFoundation(Suit.HEARTS),
                empty = klondike.getFoundation(Suit.DIAMONDS);

        // seed a foundation
        int s = (int) ((Math.random() * 10) + 1);
        foundation.add(new Card(Rank.ACE, Suit.HEARTS));
        for (int i = 1; i < s; ++i) {
            top = foundation.peekLast();
            foundation.add(new Card(top.getRank().higher(), Suit.HEARTS));
        }

        // seed the waste
        for (int j = 0; j < 3; j++) {
            klondike.moveCardToWaste();
        }
        originalWaste = ImmutableList.copyOf(waste);

        next = new Card(Rank.ACE, Suit.DIAMONDS);
        waste.add(next);

        // valid move -- empty foundation, ace
        assertTrue(klondike.moveFromWasteToFoundation());
        assertEquals(originalWaste, waste);
        assertEquals(ImmutableList.of(next), empty);
        empty.clear();

        while (next.getRank() == Rank.ACE || next.getSuit() == Suit.HEARTS) {
            next = Card.randomCard();
        }
        waste.add(next);

        // invalid move -- empty foundation, non-ace
        assertFalse(klondike.moveFromWasteToFoundation());
        assertEquals(originalWaste, waste.subList(0, 3));
        assertEquals(ImmutableList.of(next), waste.subList(3, 4));
        assertTrue(klondike.getFoundation(next.getSuit()).isEmpty());
        waste.removeLast();

        next = new Card(foundation.peekLast().getRank().higher(), Suit.HEARTS);
        waste.add(next);

        // valid move -- non-empty foundation, correct rank
        assertTrue(klondike.moveFromWasteToFoundation());
        assertEquals(originalWaste, waste);
        assertEquals(next, foundation.getLast());

        originalFoundation = ImmutableList.copyOf(foundation);
        while (next.getRank() == foundation.getLast().getRank().higher() || next.getSuit() != Suit.HEARTS) {
            next = Card.randomCard();
        }
        waste.add(next);

        // invalid move -- wrong rank
        assertFalse(klondike.moveFromWasteToFoundation());
        assertEquals(originalWaste, waste.subList(0, 3));
        assertEquals(ImmutableList.of(next), waste.subList(3, 4));
        assertEquals(originalFoundation, foundation);
    }

    @Test
    public void testMoveFromTableauToTableau() {
        int count;
        Card card, card2;
        List<Card> faceup;
        ImmutableList<Card> original1, original2;
        Klondike.Tableau empty = klondike.new Tableau(),
                tableau1 = klondike.new Tableau(),
                tableau2 = klondike.new Tableau();
        Deck d = DeckUtils.createStandardDeck();
        d.shuffle();

        count = (int) (Math.random() * 6) + 1;
        for (int i = 0; i < count; ++i) {
            tableau1.add(d.dealFaceDown());
        }

        // valid move -- king and other cards to empty tableau
        tableau1.add(Card.fromString("K" + "CDHS".charAt((int) (Math.random() * 4))));
        count = (int) (Math.random() * 13);
        buildTableau(tableau1, count);
        original1 = ImmutableList.copyOf(tableau1);

        assertTrue(klondike.moveFromTableauToTableau(tableau1, empty, count + 1));
        assertEquals(original1.subList(0, original1.size() - count - 1), tableau1);
        assertEquals(original1.subList(original1.size() - count - 1, original1.size()), empty);
        assertFalse(tableau1.peekLast().isFacedown());

        // invalid move -- non-king and other cards to empty tableau
        empty.clear();
        tableau1.removeLast();
        while ((card = Card.randomCard()).getRank() == Rank.KING || card.getRank() == Rank.ACE);
        tableau1.add(card);
        count = (int) (Math.random() * (card.getRank().value - 2));
        buildTableau(tableau1, count);
        original1 = ImmutableList.copyOf(tableau1);

        assertFalse(klondike.moveFromTableauToTableau(tableau1, empty, count));
        assertTrue(empty.isEmpty());
        assertEquals(original1, tableau1);
        assertEquals(Collections2.filter(original1, isFaceDownPredicate).size(),
                Collections2.filter(tableau1, isFaceDownPredicate).size());

        // valid move -- non-king and other cards to correct rank tableau
        empty.clear();
        tableau1.removeLast();
        while ((card = Card.randomCard()).getRank() == Rank.TWO || card.getRank() == Rank.ACE);
        tableau1.add(card);
        count = (int) (Math.random() * (card.getRank().value - 2));
        buildTableau(tableau1, count);
        original1 = ImmutableList.copyOf(tableau1);
        faceup = Lists.newLinkedList(Collections2.filter(tableau1, Predicates.not(isFaceDownPredicate)));
        while ((card = faceup.get((int) (Math.random() * faceup.size()))).getRank() != Rank.KING);
        card = new Card(card.getRank(), otherSuitOfSameColor(card.getSuit()));
        tableau2.removeLast();
        tableau2.add(card);
        original2 = ImmutableList.copyOf(tableau2);

        assertTrue(klondike.moveFromTableauToTableau(tableau1, tableau2, count));
        assertEquals(original1.subList(0, original1.size() - count), tableau1);
        assertEquals(original2, tableau2.subList(0, original2.size()));
        assertEquals(original2, tableau2.subList(original2.size(), tableau2.size()));
        assertFalse(tableau1.peekLast().isFacedown());
    }

    /**
     * Add {@code numCards} cards face up to {@code tableau}.
     *
     * @param tableau
     * @param numCards
     */
    private void buildTableau(Klondike.Tableau tableau, int numCards) {
        Rank nextRank;
        Suit nextSuit;
        Card last = tableau.peekLast();

        for (int i = 0; i < numCards; ++i) {
            if (last.getRank() == Rank.TWO) {
                return;
            }
            nextRank = last.getRank().lower();
            nextSuit = last.getColor().opposite() == Suit.Color.RED ?
                    (Math.random() < 0.5 ? Suit.DIAMONDS : Suit.HEARTS) :
                    (Math.random() < 0.5 ? Suit.CLUBS : Suit.SPADES);

            last = new Card(nextRank, nextSuit);
            tableau.add(last);
        }
    }

    private Suit otherSuitOfSameColor(Suit suit) {
        switch (suit) {
            case SPADES:   return Suit.CLUBS;
            case HEARTS:   return Suit.DIAMONDS;
            case CLUBS:    return Suit.SPADES;
            case DIAMONDS: return Suit.HEARTS;
        }
        return null;
    }

    private final Predicate<Card> isFaceDownPredicate = new Predicate<Card>() {
        @Override
        public boolean apply(Card input) {
            return input.isFacedown();
        }
    };
}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;
// import static org.mockito.Mockito.mock;

public class KlondikeTest {

    Klondike klondike;

    final Logger logger = LoggerFactory.getLogger(KlondikeTest.class);

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
        Card card;
        Suit suit;
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
        count = (int) (Math.random() * tableau1.size() == 7 ? 4 : 5) + 1;
        for (int i = 0; i < count; ++i) {
            tableau2.add(d.dealFaceDown());
        }

        /******************************************************
         * valid move -- king and other cards to empty tableau
         ******************************************************/
        // get a random king
        tableau1.add(Card.fromString("K" + "CDHS".charAt((int) (Math.random() * 4))));
        // choose a random number of additional cards to add to the tableau
        count = (int) (Math.random() * 12);
        buildTableau(tableau1, count);
        original1 = ImmutableList.copyOf(tableau1);

        assertTrue(klondike.moveFromTableauToTableau(tableau1, empty, count + 1));
        assertEquals(original1.subList(0, original1.size() - count - 1), tableau1);
        assertEquals(original1.subList(original1.size() - count - 1, original1.size()), empty);
        assertFalse(tableau1.peekLast().isFacedown());

        /************************************************************
         * invalid move -- non-king and other cards to empty tableau
         ************************************************************/
        empty.clear();
        // the tableau has one face-up card after moving the king, remove it
        tableau1.removeLast();
        // and replace it with a random card that's not a king
        while ((card = Card.randomCard()).getRank() == Rank.KING || card.getRank() == Rank.ACE);
        tableau1.add(card);
        // choose a random number of additional cards to add to the tableau
        // count + 1 is the number of face-up cards in the tableau
        count = (int) (Math.random() * (card.getRank().value - 2));
        buildTableau(tableau1, count);
        original1 = ImmutableList.copyOf(tableau1);

        assertFalse(klondike.moveFromTableauToTableau(tableau1, empty, count + 1));
        assertTrue(empty.isEmpty());
        assertEquals(original1, tableau1);
        assertEquals(Collections2.filter(original1, Predicates.not(isFaceDownPredicate)).size(),
                count + 1);

        /*****************************************
         * valid move -- non-king and other cards
         *****************************************/
        empty.clear();
        // get rid of all the face-up cards in tableau1
        tableau1.retainAll(Collections2.filter(tableau1, isFaceDownPredicate));
        // start with a random card that's not a king
        while ((card = Card.randomCard()).getRank() == Rank.KING || card.getRank() == Rank.ACE);
        tableau1.add(card);
        // choose a random number of additional cards to add to the tableau
        count = (int) (Math.random() * (card.getRank().value - 2));
        buildTableau(tableau1, count);
        original1 = ImmutableList.copyOf(tableau1);
        // choose a random face-up card to move to another tableau
        faceup = Lists.newLinkedList(Collections2.filter(tableau1, Predicates.not(isFaceDownPredicate)));
        card = faceup.get(count = (int) (Math.random() * faceup.size()));
        // put a card on tableau2 so we can move our test card there
        if (count != 0) {
            // get the previous card in the tableau, then get the other suit that's the same color
            suit = otherSuitOfSameColor(faceup.get(count - 1).getSuit());
        }
        else {
            // get a random suit of the appropriate color
            if (card.getColor() == Suit.Color.RED) {
                suit = Math.random() < 0.5 ? Suit.CLUBS : Suit.SPADES;
            }
            else {
                suit = Math.random() < 0.5 ? Suit.DIAMONDS : Suit.HEARTS;
            }
        }
        tableau2.add(new Card(card.getRank().higher(), suit));
        original2 = ImmutableList.copyOf(tableau2);

        assertTrue(klondike.moveFromTableauToTableau(tableau1, tableau2, faceup.size() - count));
        assertEquals(original1.subList(0, original1.size() - (faceup.size() - count)), tableau1);
        assertEquals(original2, tableau2.subList(0, original2.size()));
        assertEquals(original1.subList(original1.size() - (faceup.size() - count), original1.size()),
                tableau2.subList(original2.size(), tableau2.size()));
        if (!tableau1.isEmpty()) {
            assertFalse(tableau1.peekLast().isFacedown());
        }

        /****************************
         * invalid move -- wrong rank
         ****************************/
        // get rid of all the face-up cards in both tableaus
        tableau1.retainAll(Collections2.filter(tableau1, isFaceDownPredicate));
        tableau2.retainAll(Collections2.filter(tableau2, isFaceDownPredicate));
        // get a random card for tableau1
        while ((card = Card.randomCard()).getRank() == Rank.ACE || card.getRank().value <= 4);
        tableau1.add(card);
        // choose a random number of additional cards (at least three) to add to the tableau
        count = (int) (Math.random() * (card.getRank().value - 3) + 3);
        buildTableau(tableau1, count);
        original1 = ImmutableList.copyOf(tableau1);
        // get a random card for tableau2
        while ((card = Card.randomCard()).getRank() == Rank.ACE);
        tableau2.add(card);
        // choose a random number of additional cards to add to the tableau
        // but don't complete tableau2
        count = (int) (Math.random() * (card.getRank().value - 3));
        buildTableau(tableau2, count);
        original2 = ImmutableList.copyOf(tableau2);
        // choose a random face-up card from tableau1 that's the opposite color
        // from the top card on tableau2 and that is not one rank lower than
        // that card
        faceup = Lists.newLinkedList(Collections2.filter(tableau1, Predicates.not(isFaceDownPredicate)));
        while ((card = faceup.get(count = (int) (Math.random() * faceup.size()))).getColor() == tableau2.getLast().getColor() ||
                card.getRank() == tableau2.getLast().getRank().lower());

        assertFalse(klondike.moveFromTableauToTableau(tableau1, tableau2, faceup.size() - count));
        assertEquals(original1, tableau1);
        assertEquals(original2, tableau2);

        /******************************
         * invalid move -- wrong color
         ******************************/
        // get rid of all the face-up cards in tableaus
        tableau1.retainAll(Collections2.filter(tableau1, isFaceDownPredicate));
        tableau2.retainAll(Collections2.filter(tableau2, isFaceDownPredicate));
        // start with a random card that's not a king
        while ((card = Card.randomCard()).getRank() == Rank.KING || card.getRank() == Rank.ACE);
        tableau1.add(card);
        // choose a random number of additional cards to add to the tableau
        count = (int) (Math.random() * (card.getRank().value - 3) + 1);
        buildTableau(tableau1, count);
        original1 = ImmutableList.copyOf(tableau1);
        // choose a random face-up card to try and move to another tableau
        faceup = Lists.newLinkedList(Collections2.filter(tableau1, Predicates.not(isFaceDownPredicate)));
        card = faceup.get(count = (int) (Math.random() * faceup.size()));
        // put a card on tableau2 so we can move our test card there
        // get a random suit of the wrong color
        if (card.getColor() == Suit.Color.RED) {
            suit = Math.random() < 0.5 ? Suit.DIAMONDS : Suit.HEARTS;
        }
        else {
            suit = Math.random() < 0.5 ? Suit.CLUBS : Suit.SPADES;
        }
        tableau2.add(new Card(card.getRank().higher(), suit));
        original2 = ImmutableList.copyOf(tableau2);

        assertFalse(klondike.moveFromTableauToTableau(tableau1, tableau2, faceup.size() - count));
        assertEquals(original1, tableau1);
        assertEquals(original2, tableau2);
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

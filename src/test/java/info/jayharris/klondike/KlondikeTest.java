package info.jayharris.klondike;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import info.jayharris.cardgames.Card;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class KlondikeTest {

    Klondike klondike;

    @Before
    public void setUp() throws Exception {
        klondike = new Klondike(/* mockActionSource */);
        klondike.init();
    }

    @Test
    public void testMoveCardToWaste() throws Exception {
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
    public void testRestartDeck() throws Exception {
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
//
//    @Test
//    public void testMoveWasteToTableau() throws Exception {
//        Klondike.Waste waste = getWaste(klondike);
//        Klondike.Tableau tableau = getTableau(klondike, 1),
//                empty = getTableau(klondike, 0);
//        empty.clear();
//
//        tableau.add(Card.fromString("9H"));
//
//        waste.add(Card.fromString("7S"));
//        waste.add(Card.fromString("KH"));
//        waste.add(Card.fromString("8C"));
//
//        // can't put a non-king on an empty tableau
//        assertFalse(klondike.moveFromWasteToTableau(empty));
//        assertEquals(ImmutableList.of(Card.fromString("7S"), Card.fromString("KH"), Card.fromString("8C")), waste);
//        assertTrue(empty.isEmpty());
//
//        // can put a valid card on a non-empty tableau
//        assertTrue(klondike.moveFromWasteToTableau(tableau));
//        assertEquals(ImmutableList.of(Card.fromString("7S"), Card.fromString("KH")), waste);
//        assertEquals(Card.fromString("8C"), tableau.peekLast());
//
//        // can't put a wrong-rank card on a non-empty tableau
//        assertFalse(klondike.moveFromWasteToTableau(tableau));
//        assertEquals(ImmutableList.of(Card.fromString("7S"), Card.fromString("KH")), waste);
//        assertEquals(Card.fromString("8C"), tableau.peekLast());
//
//        // can put a king on an empty tableau
//        assertTrue(klondike.moveFromWasteToTableau(empty));
//        assertEquals(ImmutableList.of(Card.fromString("7S")), waste);
//        assertEquals(ImmutableList.of(Card.fromString("KH")), empty);
//
//        // can't put a wrong-suit card on a non-empty tableau
//        assertFalse(klondike.moveFromWasteToTableau(tableau));
//        assertEquals(ImmutableList.of(Card.fromString("7S")), waste);
//        assertEquals(Card.fromString("8C"), tableau.peekLast());
//    }
}

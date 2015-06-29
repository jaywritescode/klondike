package info.jayharris.klondike;

import com.google.common.base.Strings;
import com.googlecode.blacken.colors.ColorNames;
import com.googlecode.blacken.colors.ColorPalette;
import com.googlecode.blacken.swing.SwingTerminal;
import com.googlecode.blacken.terminal.BlackenKeys;
import com.googlecode.blacken.terminal.CursesLikeAPI;
import com.googlecode.blacken.terminal.TerminalInterface;
import info.jayharris.cardgames.Deck;
import info.jayharris.cardgames.Suit;
import org.apache.commons.collections4.iterators.LoopingListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TerminalUI implements KlondikeUI {

    private Klondike klondike;

    private boolean quit;
    private ColorPalette palette;
    private CursesLikeAPI term = null;

    private TerminalUIComponent<?> pointingTo;
    private List<TerminalUIComponent<?>> components;
    private LoopingListIterator<TerminalUIComponent<?>> componentOrder;
    private boolean lastDirectionRight = true;      // direction of last move --
                                                    // if componentOrder.next() => X, then an immediate call to
                                                    // componentOrder.previous() will also => X.

    public final int START_ROW = 0,
            LEFT_COL = 5,
            SPACE_BETWEEN = 3,
            TABLEAU_ROW = 2,
            WASTE_START_COL = LEFT_COL + "[24 cards]".length() + SPACE_BETWEEN,
            WASTE_MAX_WIDTH = "[... XX XX XX XX XX XX]".length(),
            FOUNDATION_START_COL = WASTE_START_COL + WASTE_MAX_WIDTH + SPACE_BETWEEN;

    final Logger logger = LoggerFactory.getLogger(TerminalUI.class);

    public TerminalUI(Klondike klondike) {
        this.klondike = klondike;
    }

    protected boolean loop() {
        int key = BlackenKeys.NO_KEY;
        if (palette.containsKey("White")) {
            term.setCurBackground("White");
        }
        if (palette.containsKey("Black")) {
            term.setCurForeground("Black");
        }
        this.term.clear();

        while (!this.quit) {
            for (TerminalUIComponent<?> component : components) {
                component.writeToTerminal();
            }
            key = term.getch();
            // getch automatically does a refresh
            onKeyPress(key);
        }

        term.refresh();
        return this.quit;
    }

    public void init(TerminalInterface term, ColorPalette palette) {
        if (term == null) {
            this.term = new CursesLikeAPI(new SwingTerminal());
            this.term.init("Klondike", 25, 80);
        } else {
            this.term = new CursesLikeAPI(term);
        }

        if (palette == null) {
            palette = new ColorPalette();
            palette.addAll(ColorNames.XTERM_256_COLORS, false);
            palette.putMapping(ColorNames.SVG_COLORS);
        }
        this.palette = palette;
        this.term.setPalette(palette);

        // set up all of the deck, waste, foundation, tableau visual components
        components = new ArrayList<TerminalUIComponent<?>>() {{
            int col;

            this.add(new TerminalUIComponent<Deck>(klondike.getDeck(), START_ROW, LEFT_COL) {
                @Override
                public void writeToTerminal() {
                    super.writeToTerminal("[" +
                            Strings.padStart(Integer.toString(klondike.getDeck().size()), 2, ' ') + " cards]");
                }

                @Override
                public void doAction() {
                    System.err.println("deck");
                }
            });

            this.add(new TerminalUIComponent<Klondike.Waste>(klondike.getWaste(), START_ROW, WASTE_START_COL) {
                @Override
                public void doAction() {
                    System.err.println("waste");
                }
            });

            col = LEFT_COL;
            for (int i = 0; i < 7; ++i) {
                this.add(new TableauUIComponent(klondike.getTableau(i), col));
                col += "XX".length() + SPACE_BETWEEN;
            }

            col = FOUNDATION_START_COL;
            for (Suit suit : EnumSet.allOf(Suit.class)) {
                this.add(new FoundationUIComponent(klondike.getFoundation(suit), col));
                col += "XX".length() + SPACE_BETWEEN;
            }
        }};
        componentOrder = new LoopingListIterator(components);
        pointingTo = componentOrder.next();

        start();
    }

    private void onKeyPress(int codepoint) {
        switch (codepoint) {
            case 'z':
            case 'Z':
                pointingTo.doAction();
                break;
            case 'a':
            case 'A':
                movePointerLeft();
                break;
            case 'd':
            case 'D':
                movePointerRight();
                break;
            case 's':
            case 'S':
                break;
            case 'w':
            case 'W':
                break;
            default:
                break;
        }
    }

    private void movePointerRight() {
        pointingTo = componentOrder.next();
        if (!lastDirectionRight) {
            pointingTo = componentOrder.next();
            lastDirectionRight = true;
        }
    }

    private void movePointerLeft() {
        pointingTo = componentOrder.previous();
        if (lastDirectionRight) {
            pointingTo = componentOrder.previous();
            lastDirectionRight = false;
        }
    }

    private void start() {
        klondike.init();
    }

    public void quit() {
        this.quit = true;
        term.quit();
    }

    public abstract class TerminalUIComponent<T> {
        T payload;
        int row, column;

        public TerminalUIComponent(T payload, int row, int column) {
            this.payload = payload;
            this.row = row;
            this.column = column;
        }

        public abstract void doAction();

        public boolean canAcceptCard() {
            return false;
        }

        public void writeToTerminal() {
            this.writeToTerminal(payload.toString());
        }

        public void writeToTerminal(String str) {
            term.mvputs(row, column, str);
        }

        
    }

    public class FoundationUIComponent extends TerminalUIComponent<Klondike.Foundation> {
        public FoundationUIComponent(Klondike.Foundation payload, int column) {
            super(payload, START_ROW, column);
        }

        @Override
        public boolean canAcceptCard() {
            return true;
        }

        @Override
        public void doAction() {
            System.err.println("foundation: " + payload.toString());
        }
    }

    public class TableauUIComponent extends TerminalUIComponent<Klondike.Tableau> {
        public TableauUIComponent(Klondike.Tableau payload, int column) {
            super(payload, TABLEAU_ROW, column);
        }

        public void writeToTerminal() {
            if (payload.isEmpty()) {
                term.mvputs(column, row, "  ");
            } else {
                for (int i = 0; i < payload.size(); ++i) {
                    term.mvputs(row + i, column, payload.get(i).toString());
                }
            }
        }

        public void doAction() {
            System.err.println("tableau: " + payload.toString());
        }
    }

    public static void main(String[] args) {
        TerminalUI ui = new TerminalUI(new Klondike());
        ui.init(null, null);
        ui.loop();
        ui.quit();
    }
}

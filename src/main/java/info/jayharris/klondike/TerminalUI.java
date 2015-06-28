package info.jayharris.klondike;

import com.google.common.base.Strings;
import com.googlecode.blacken.colors.ColorNames;
import com.googlecode.blacken.colors.ColorPalette;
import com.googlecode.blacken.swing.SwingTerminal;
import com.googlecode.blacken.terminal.BlackenKeys;
import com.googlecode.blacken.terminal.CursesLikeAPI;
import com.googlecode.blacken.terminal.TerminalInterface;
import info.jayharris.cardgames.Card;
import info.jayharris.cardgames.Deck;
import info.jayharris.cardgames.Suit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;
import java.util.*;

public class TerminalUI implements KlondikeUI {

    private Klondike klondike;

    private boolean quit;
    private ColorPalette palette;
    private CursesLikeAPI term = null;
    private Card current = null;

    private TerminalUIComponent<?> pointingTo;
    private List<TerminalUIComponent<?>> componentOrder;

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
            for (TerminalUIComponent<?> component : componentOrder) {
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
        componentOrder = new ArrayList<TerminalUIComponent<?>>() {{
            int col;

            this.add(new TerminalUIComponent<Deck>(klondike.getDeck(), LEFT_COL, START_ROW) {
                @Override
                public void writeToTerminal() {
                    super.writeToTerminal("[" +
                            Strings.padStart(Integer.toString(klondike.getDeck().size()), 2, ' ') + " cards]");
                }

                @Override
                public void doAction() {
                    klondike.deal();
                }
            });

            this.add(new TerminalUIComponent<Klondike.Waste>(klondike.getWaste(), WASTE_START_COL, START_ROW) {
                @Override
                public void doAction() {
                    current = (current == null ? payload.getLast() : null);
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
        pointingTo = componentOrder.get(0);

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

    }

    private void movePointerLeft() {

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

        public void writeToTerminal() {
            this.writeToTerminal(payload.toString());
        }

        public void writeToTerminal(String str) {
            term.mvputs(column, row, str);
        }
    }

    public class FoundationUIComponent extends TerminalUIComponent<Klondike.Foundation> {
        public FoundationUIComponent(Klondike.Foundation payload, int column) {
            super(payload, column, START_ROW);
        }

        @Override
        public void doAction() {
            System.out.println("foundation: " + payload.suit.toString());
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

        public void doAction() {}
    }

    public static void main(String[] args) {
        TerminalUI ui = new TerminalUI(new Klondike());
        ui.init(null, null);
        ui.loop();
        ui.quit();
    }
}

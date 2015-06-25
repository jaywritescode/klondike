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

import java.util.*;

public class TerminalUI implements KlondikeUI {

    private Klondike klondike;

    private boolean quit;
    private ColorPalette palette;
    private CursesLikeAPI term = null;

    private TerminalUIComponent<?> deckUIComponent,
            wasteUIComponent, pointingTo = null;
    private Map<Suit, TerminalUIComponent<Klondike.Foundation>> foundationUIComponents;
    private List<TableauUIComponent> tableauUIComponents;

    public final int START_ROW = 0,
            LEFT_COL = 5,
            SPACE_BETWEEN = 3,
            TABLEAU_ROW = 2,
            WASTE_START_COL = LEFT_COL + "[24 cards]".length() + SPACE_BETWEEN,
            WASTE_MAX_WIDTH = "[... XX XX XX XX XX XX]".length(),
            FOUNDATION_START_COL = WASTE_START_COL + WASTE_MAX_WIDTH + SPACE_BETWEEN;

    public TerminalUI(Klondike klondike) {
        this.klondike = klondike;
    }

    protected boolean loop() {
        int ch = BlackenKeys.KEY_NO_KEY;
        if (palette.containsKey("White")) {
            term.setCurBackground("White");
        }
        if (palette.containsKey("Black")) {
            term.setCurForeground("Black");
        }
        this.term.clear();

        while (!this.quit) {
            deckUIComponent.writeToTerminal();
            wasteUIComponent.writeToTerminal();
            for (TerminalUIComponent<Klondike.Foundation> foundation : foundationUIComponents.values()) {
                foundation.writeToTerminal();
            }
            for (TableauUIComponent tableau : tableauUIComponents) {
                tableau.writeToTerminal();
            }

            // getch automatically does a refresh
            ch = term.getch();
            if (ch != BlackenKeys.NO_KEY) {
                onKeyPress(ch);
            }
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

        this.deckUIComponent = new TerminalUIComponent<Deck>(klondike.getDeck(), LEFT_COL, START_ROW) {
            @Override
            public void writeToTerminal() {
                super.writeToTerminal("[" + Strings.padStart(Integer.toString(klondike.getDeck().size()), 2, ' ') + " cards]");
            }

            @Override
            public void doAction() {
                klondike.deal();
            }
        };
        this.wasteUIComponent = new TerminalUIComponent<Klondike.Waste>(klondike.getWaste(), WASTE_START_COL, START_ROW) {
            @Override
            public void doAction() {
                System.out.println("waste");
            }
        };
        this.foundationUIComponents = new HashMap<Suit, TerminalUIComponent<Klondike.Foundation>>() {{
            int col = FOUNDATION_START_COL;
            for (Suit suit : EnumSet.allOf(Suit.class)) {
                this.put(suit, new FoundationUIComponent(klondike.getFoundation(suit), col));
                col += "XX".length() + SPACE_BETWEEN;
            }
        }};
        this.tableauUIComponents = new ArrayList<TableauUIComponent>() {{
            int col = LEFT_COL;
            for (int i = 0; i < 7; ++i) {
                this.add(new TableauUIComponent(klondike.getTableau(i), col));
                col += "XX".length() + SPACE_BETWEEN;
            }
        }};

        start();
    }

    private void onKeyPress(int ch) {
        switch (ch) {
            case 'd':
            case 'D':
                klondike.deal();
                break;
            case 'h':
            case 'H':
            case BlackenKeys.KEY_LEFT:
                movePointerLeft();
                break;
            case 'l':
            case 'L':
            case BlackenKeys.KEY_RIGHT:
                movePointerRight();
                break;
            case BlackenKeys.KEY_ENTER:

            default:
        }
    }

    private void movePointerRight() {
        int tmp;
        if (pointingTo == deckUIComponent) {
            pointingTo = wasteUIComponent;
        }
        else if (pointingTo == wasteUIComponent) {
            pointingTo = tableauUIComponents.get(0);
        }
        else if ((tmp = tableauUIComponents.indexOf(pointingTo)) > -1) {
            if (tmp + 1 < tableauUIComponents.size()) {
                pointingTo = tableauUIComponents.get(tmp + 1);
            }
            else if (false) {
                // move to the correct foundation if we're holding a card
            }
            else {
                pointingTo = deckUIComponent;
            }
        }
        else {
            // we're pointing at a foundation
            pointingTo = deckUIComponent;
        }
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

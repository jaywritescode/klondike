package info.jayharris.klondike;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.googlecode.blacken.colors.ColorNames;
import com.googlecode.blacken.colors.ColorPalette;
import com.googlecode.blacken.swing.SwingTerminal;
import com.googlecode.blacken.terminal.*;
import info.jayharris.cardgames.Deck;
import org.apache.commons.collections4.iterators.LoopingListIterator;

import java.util.*;

public class TerminalUI implements KlondikeUI, Observer {

    private Klondike klondike;

    private boolean quit;
    private ColorPalette palette;
    private CursesLikeAPI term = null;

    private TerminalUIComponent<?> pointingTo, movingFrom = null;
    private List<TerminalUIComponent<?>> components;
    private LoopingListIterator<TerminalUIComponent<?>> componentOrder;
    private boolean lastDirectionRight = true;      // direction of last move --
                                                    // if componentOrder.next() => X, then an immediate call to
                                                    // componentOrder.previous() will also => X.

    // layout parameters
    public final int START_ROW = 0,
            LEFT_COL = 5,
            SPACE_BETWEEN = 5,
            TABLEAU_ROW = 2,
            WASTE_CARDS_SHOWN = 6,
            WASTE_START_COL = LEFT_COL + "[24 cards]".length() + SPACE_BETWEEN,
            WASTE_MAX_WIDTH = "... XX XX XX XX XX XX".length(),
            FOUNDATION_START_COL = WASTE_START_COL + WASTE_MAX_WIDTH + SPACE_BETWEEN;

    /**
     * Create a new curses-style UI.
     *
     * @param klondike the game
     */
    public TerminalUI(Klondike klondike) {
        setKlondike(klondike);
    }

    protected void init(TerminalInterface term, ColorPalette palette) {
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

        setupUIComponents();
        start();
    }

    /**
     * Wire up the {@code Klondike} instance to this observer.
     *
     * @param klondike the game
     */
    private void setKlondike(Klondike klondike) {
        this.klondike = klondike;
        this.klondike.addObserver(this);
    }

    /**
     * Wire up the {@code Klondike} instance to the various layout components.
     */
    private void setupUIComponents() {
        components = new ArrayList<TerminalUIComponent<?>>() {{
            int col;

            /******************************************************************
             * Deck UI component
             ******************************************************************/
            this.add(new TerminalUIComponent<Deck>(klondike.getDeck(), START_ROW, LEFT_COL) {
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

            /******************************************************************
             * Waste UI component
             ******************************************************************/
            this.add(new TerminalUIComponent<Klondike.Waste>(klondike.getWaste(), START_ROW, WASTE_START_COL) {
                @Override
                public void doAction() {
                    // pick up the next card in the waste
                    if (movingFrom == null && !payload.isEmpty()) {
                        movingFrom = this;
                    }
                    // return the card to whence it came
                    else {
                        movingFrom = null;
                    }
                }

                @Override
                public void writeToTerminal() {
                    int sz = payload.size();

                    int index = Math.max(0, sz - WASTE_CARDS_SHOWN), strlen = 0;
                    if (sz > WASTE_CARDS_SHOWN) {
                        writeToTerminal("... ");
                        strlen += "... ".length();
                    }
                    while (index < sz) {
                        if (index == sz - 1) {
                            if (movingFrom == this) {
                                setCurBackground("Yellow");
                            }
                            writeToTerminal(0, strlen, payload.get(index).toString());
                            strlen += 2;
                        }
                        else {
                            writeToTerminal(0, strlen, payload.get(index).toString() + " ");
                            strlen += 3;
                        }
                        setCurBackground("White");
                        ++index;
                    }
                    while (strlen < WASTE_MAX_WIDTH) {
                        writeToTerminal(0, strlen, " ");
                        ++strlen;
                    }
                }
            });

            /******************************************************************
             * Tableau UI components
             ******************************************************************/
            col = LEFT_COL;
            for (int i = 0; i < 7; ++i) {
                this.add(new TableauUIComponent(klondike.getTableau(i), col));
                col += "XX".length() + SPACE_BETWEEN;
            }

            /******************************************************************
             * Foundation UI component
             ******************************************************************/
            col = FOUNDATION_START_COL;
            this.add(new TerminalUIComponent<Collection<Klondike.Foundation>>(
                    klondike.getFoundations(), START_ROW, col) {

                Joiner joiner = Joiner.on(Strings.repeat(" ", SPACE_BETWEEN));

                @Override
                public void writeToTerminal() {
                    writeToTerminal(joiner.join(payload));
                }

                @Override
                public void doAction() {
                    boolean legal = false;
                    if (movingFrom != null) {
                        if (movingFrom.getClass() == TableauUIComponent.class) {
                            legal = klondike.moveFromTableauToFoundation((Klondike.Tableau) movingFrom.payload);
                        }
                        else {
                            legal = klondike.moveFromWasteToFoundation();
                        }
                    }
                    if (legal) {
                        movingFrom.writeToTerminal();
                        this.writeToTerminal();
                        movingFrom = null;
                    }
                }
            });
        }};
        componentOrder = new LoopingListIterator<>(components);
        pointingTo = componentOrder.next();
    }

    private void start() {
        klondike.init();
    }

    private void onKeyPress(int codepoint) {
        switch (codepoint) {
            case ' ':
                pointingTo.doAction();
                break;
            case 'a':
            case 'A':
                movePointerAndRedraw(true);
                break;
            case 'd':
            case 'D':
                movePointerAndRedraw(false);
                break;
            case 'r':
            case 'R':
                if (klondike.isGameOver()) {
                    setKlondike(new Klondike(klondike.rules));
                    term.clear();
                    setupUIComponents();
                    start();
                }
                break;
            default:
                break;
        }
        pointingTo.receiveKeyPress(codepoint);
    }

    /* ************************************************************************
     * Methods for moving the pointer across the list of components.
     * ************************************************************************/
    private void movePointerAndRedraw(boolean left) {
        pointingTo.drawPointer(true);
        if (left) {
            movePointerLeft();
        }
        else {
            movePointerRight();
        }
        pointingTo.receiveFocus();
        pointingTo.drawPointer(false);
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

    /* ************************************************************************
     * Methods for running the game
     * ************************************************************************/

    protected boolean loop() {
        int key;
        if (palette.containsKey("White")) {
            term.setCurBackground("White");
        }
        if (palette.containsKey("Black")) {
            term.setCurForeground("Black");
        }
        term.clear();

        while (!this.quit) {
            for (TerminalUIComponent<?> component : components) {
                component.writeToTerminal();
            }
            pointingTo.drawPointer(false);
            key = term.getch();
            // getch automatically does a refresh
            onKeyPress(key);
        }

        term.refresh();
        return this.quit;
    }

    public void quit() {
        this.quit = true;
        term.quit();
    }

    /* ************************************************************************
     * Helper methods for inner class references
     * ************************************************************************/
    private void setCurBackground(String c) {
        term.setCurBackground(c);
    }

    /* ************************************************************************
     * Observer/observable API
     * ************************************************************************/
    @Override
    public void update(Observable o, Object arg) {
        String msg, color;

        if (o == klondike && arg instanceof Klondike.GameOver) {
            if (klondike.won()) {
                msg = " YOU WIN! ";
                color = "Green";
            }
            else {
                msg = "GAME OVER!";
                color = "Magenta";
            }

            String s = "Press 'R' to restart";
            term.setCurForeground(color);
            term.mvputs(10, term.getWidth() / 2 - 10, Strings.repeat("#", 20));
            term.mvputs(11, term.getWidth() / 2 - 10, "#" + Strings.repeat(" ", 18) + "#");
            term.mvputs(12, term.getWidth() / 2 - 10, "#" + String.format("    %s    ", msg) + "#");
            term.mvputs(13, term.getWidth() / 2 - 10, "#" + Strings.repeat(" ", 18) + "#");
            term.mvputs(14, term.getWidth() / 2 - 10, Strings.repeat("#", 20));
            term.mvputs(term.getHeight() - 1, term.getWidth() - s.length() - 2, s);
        }

        term.setCurForeground("Black");
    }

    public abstract class TerminalUIComponent<T> {
        T payload;
        int startRow, startColumn;

        public TerminalUIComponent(T payload, int startRow, int startColumn) {
            this.payload = payload;
            this.startRow = startRow;
            this.startColumn = startColumn;
        }

        public abstract void doAction();

        public void receiveFocus() {}

        public void receiveKeyPress(int codepoint) {}

        public void writeToTerminal() {
            this.writeToTerminal(payload.toString());
        }

        public void writeToTerminal(String str) {
            term.mvputs(startRow, startColumn, str);
        }

        public void writeToTerminal(int rowOffset, int columnOffset, String str) {
            term.mvputs(startRow + rowOffset, startColumn + columnOffset, str);
        }

        /**
         * Draw or undraw the pointer indicating the current element.
         *
         * @param remove if {@code true} then remove the pointer, otherwise draw it
         */
        public void drawPointer(boolean remove) {
            term.mvputs(startRow, startColumn - 3, remove ? "   " : "-> ");
        }
    }

    public class TableauUIComponent extends TerminalUIComponent<Klondike.Tableau> {
        int pointerIndex;
        int lengthToClean;

        public TableauUIComponent(Klondike.Tableau payload, int column) {
            super(payload, TABLEAU_ROW, column);
            pointerIndex = payload.size() - 1;
            lengthToClean = payload.size();
        }

        public void writeToTerminal() {
            for (int i = 0; i < Math.max(payload.size(), lengthToClean); ++i) {
                if (movingFrom == this && pointerIndex == i) {
                    setCurBackground("Yellow");
                }
                if (i == payload.size()) {
                    setCurBackground("White");
                }
                term.mvputs(startRow + i, startColumn, i < payload.size() ? payload.get(i).toString() : "  ");
            }
            setCurBackground("White");
            lengthToClean = payload.size();
        }

        public void doAction() {
            boolean legal = false;
            if (movingFrom == this) {
                movingFrom = null;
            }
            else if (movingFrom == null) {
                if (!payload.isEmpty()) {
                    movingFrom = this;
                }
            }
            else if (movingFrom.getClass() == TableauUIComponent.class) {
                TableauUIComponent _movingFrom = (TableauUIComponent) movingFrom;
                int numCards = _movingFrom.payload.size() - _movingFrom.pointerIndex;
                legal = klondike.moveFromTableauToTableau(_movingFrom.payload, this.payload, numCards);
            }
            else {
                legal = klondike.moveFromWasteToTableau(this.payload);
            }
            if (legal) {
                movingFrom.writeToTerminal();
                writeToTerminal();
                drawPointer(true);
                this.pointerIndex = payload.size() - 1;
                drawPointer(false);
                movingFrom = null;
            }
        }

        public void receiveFocus() {
            if (movingFrom != this) {
                pointerIndex = Math.max(payload.size() - 1, 0);
            }
        }

        public void receiveKeyPress(int codepoint) {
            switch (codepoint) {
                case 'w':
                case 'W':
                    movePointerUp();
                    break;
                case 's':
                case 'S':
                    movePointerDown();
                    break;
                default:
                    break;
            }
        }

        public void movePointerUp() {
            if (pointerIndex > payload.size() - payload.countFaceup()) {
                drawPointer(true);
                --pointerIndex;
                drawPointer(false);
            }
        }

        public void movePointerDown() {
            if (pointerIndex < payload.size() - 1) {
                drawPointer(true);
                ++pointerIndex;
                drawPointer(false);
            }
        }

        public void drawPointer(boolean remove) {
            term.mvputs(startRow + pointerIndex, startColumn - 3, remove ? "   " : "-> ");
        }
    }

    static class CommandLineParams {
        @Parameter(names = "--deal-one", description = "Deal one card at a time (instead of three).")
        private boolean dealOne = false;

        @Parameter(names = "--passes", description = "Maximum number of times through the deck.",
                converter = PassesConverter.class)
        private Klondike.Rules.Passes passes = Klondike.Rules.Passes.INFINITY;

        public class PassesConverter implements IStringConverter<Klondike.Rules.Passes> {
            @Override
            public Klondike.Rules.Passes convert(String value) {
                switch(value) {
                    case "1":
                        return Klondike.Rules.Passes.SINGLE;
                    case "3":
                        return Klondike.Rules.Passes.THREE;
                    default:
                        return Klondike.Rules.Passes.INFINITY;
                }
            }
        }
    }

    public static void main(String... args) {
        CommandLineParams params = new TerminalUI.CommandLineParams();
        new JCommander(params, args);

        Klondike.Rules rules = new Klondike.Rules(
                params.dealOne ? Klondike.Rules.Deal.DEAL_SINGLE : Klondike.Rules.Deal.DEAL_THREE, params.passes);

        TerminalUI ui = new TerminalUI(new Klondike(rules));
        ui.init(null, null);
        ui.loop();
        ui.quit();
    }
}

package info.jayharris.klondike;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.googlecode.blacken.colors.ColorNames;
import com.googlecode.blacken.colors.ColorPalette;
import com.googlecode.blacken.swing.SwingTerminal;
import com.googlecode.blacken.terminal.BlackenKeys;
import com.googlecode.blacken.terminal.CursesLikeAPI;
import com.googlecode.blacken.terminal.TerminalInterface;
import info.jayharris.cardgames.Deck;
import org.apache.commons.collections4.iterators.LoopingListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TerminalUI implements KlondikeUI {

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
            pointingTo.drawPointer(false);
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
                        System.err.println("picking up card: " + payload.peekLast().toString());
                    }
                    // return the card to whence it came
                    else {
                        movingFrom = null;
                        System.err.println("dropping card");
                    }
                }

                @Override
                public void writeToTerminal() {
                    writeToTerminal(Strings.padEnd(payload.toString(), WASTE_MAX_WIDTH, ' '));
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
                movePointerAndRedraw(true);
                break;
            case 'd':
            case 'D':
                movePointerAndRedraw(false);
                break;
            default:
                break;
        }
        pointingTo.receiveKeyPress(codepoint);
    }

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

        public void receiveFocus() {}

        public void receiveKeyPress(int codepoint) {}

        public void writeToTerminal() {
            this.writeToTerminal(payload.toString());
        }

        public void writeToTerminal(String str) {
            term.mvputs(row, column, str);
        }

        /**
         * Draw or undraw the pointer indicating the current element.
         *
         * @param remove if {@code true} then remove the pointer, otherwise draw it
         */
        public void drawPointer(boolean remove) {
            term.mvputs(row, column - 3, remove ? "   " : "-> ");
        }
    }

    public class TableauUIComponent extends TerminalUIComponent<Klondike.Tableau> {
        int pointerIndex = 1;       // pointerIndex = 1 means the top-most card in the tableau
        int lengthToClean;

        static final String blank = "  ";

        public TableauUIComponent(Klondike.Tableau payload, int column) {
            super(payload, TABLEAU_ROW, column);
            lengthToClean = payload.size();
        }

        public void writeToTerminal() {
            int newLengthToClean = 0;
            String s;

            for (int i = 0; i < Math.max(payload.size(), lengthToClean); ++i) {
                s = "  ";
                if (i < payload.size()) {
                    s = payload.get(i).toString();
                    ++newLengthToClean;
                }
                term.mvputs(row + i, column, s);
            }
            lengthToClean = newLengthToClean;
        }

        public void doAction() {
            boolean legal = false;
            if (movingFrom == null && !payload.isEmpty()) {
                movingFrom = this;
            }
            else if (movingFrom.getClass() == TableauUIComponent.class) {
                TableauUIComponent _movingFrom = (TableauUIComponent) movingFrom;
                legal = klondike.moveFromTableauToTableau(_movingFrom.payload, this.payload, _movingFrom.pointerIndex);

                // since we added more cards to the end of this tableau, we need to update its pointerIndex
                // to reflect its new length
                this.pointerIndex += _movingFrom.pointerIndex;
            }
            else {
                legal = klondike.moveFromWasteToTableau(this.payload);
                this.pointerIndex += 1;
            }
            if (legal) {
                movingFrom.writeToTerminal();
                writeToTerminal();
                drawPointer(true);
                this.pointerIndex = 1;
                drawPointer(false);
                movingFrom = null;
            }
        }

        public void receiveFocus() {
            if (movingFrom != this) {
                pointerIndex = 1;
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
            if (pointerIndex < payload.countFaceup()) {
                drawPointer(true);
                ++pointerIndex;
                drawPointer(false);
            }
        }

        public void movePointerDown() {
            if (pointerIndex > 0) {
                drawPointer(true);
                --pointerIndex;
                drawPointer(false);
            }
        }

        public void drawPointer(boolean remove) {
            term.mvputs(row + Math.max(payload.size() - pointerIndex, 0), column - 3, remove ? "   " : "-> ");
        }
    }

    public static void main(String[] args) {
        TerminalUI ui = new TerminalUI(new Klondike());
        ui.init(null, null);
        ui.loop();
        ui.quit();
    }
}

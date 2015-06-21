package info.jayharris.klondike;

import com.google.common.base.Strings;
import com.googlecode.blacken.colors.ColorNames;
import com.googlecode.blacken.colors.ColorPalette;
import com.googlecode.blacken.swing.SwingTerminal;
import com.googlecode.blacken.terminal.BlackenKeys;
import com.googlecode.blacken.terminal.CursesLikeAPI;
import com.googlecode.blacken.terminal.TerminalInterface;
import info.jayharris.cardgames.Suit;

import java.util.EnumSet;

public class TerminalUI implements KlondikeUI {

    private Klondike klondike;

    private boolean quit;
    private ColorPalette palette;
    private CursesLikeAPI term = null;

    public final int START_ROW = 0,
            LEFT_COL = 5,
            SPACE_BETWEEN = 3,
            TABLEAU_ROW = 2,
            TABLEAUS_WIDTH = LEFT_COL + (2 * 7) + (SPACE_BETWEEN * 6),
            WASTE_MAX_WIDTH = "[... XX XX XX XX XX XX]".length(),
            FOUNDATION_START_COL = Math.max(
                    TABLEAUS_WIDTH - (2 * 4) - (SPACE_BETWEEN * 3),
                    LEFT_COL + 2 + SPACE_BETWEEN + WASTE_MAX_WIDTH + SPACE_BETWEEN);

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
        term.puts("Terminal Interface\n");
        term.puts("Press F10 to quit.\n");
        this.term.clear();

        while (!this.quit) {
            // getch automatically does a refresh
            ch = term.getch();
            term.mvputs(START_ROW, LEFT_COL, deckToString());
            term.mvputs(START_ROW, LEFT_COL + 2 + SPACE_BETWEEN, wasteToString());
            term.mvputs(START_ROW, FOUNDATION_START_COL, foundationsToString());

            Klondike.Tableau tableau;
            int x = LEFT_COL;
            for (int i = 0; i < 7; ++i) {
                tableau = klondike.getTableau(i);
                if (tableau.isEmpty()) {
                    term.mvputs(TABLEAU_ROW, x, "  ");
                }
                else {
                    for (int j = 0; j < tableau.size(); ++j) {
                        term.mvputs(TABLEAU_ROW + j, x, tableau.get(j).toString());
                    }
                }
                x += 2 + SPACE_BETWEEN;
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
    }

    private String deckToString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        sb.append(Strings.padStart(Integer.toString(klondike.getDeck().size()), 2, ' '));
        sb.append("]");
        return sb.toString();
    }

    private String wasteToString() {
        return klondike.getWaste().toString();
    }

    private String foundationsToString() {
        Klondike.Foundation foundation;
        StringBuffer sb = new StringBuffer();

        for (Suit suit : EnumSet.allOf(Suit.class)) {
            foundation = klondike.getFoundation(suit);
            if (foundation.isEmpty()) {
                sb.insert(0, Strings.repeat(suit.toString(), 2));
            }
            else {
                sb.insert(0, foundation.peekLast().toString());
            }
            sb.insert(0, Strings.repeat(" ", SPACE_BETWEEN));
        }
        return sb.delete(0, SPACE_BETWEEN).toString();
    }

    public void quit() {
        this.quit = true;
        term.quit();
    }

    public static void main(String[] args) {
        TerminalUI ui = new TerminalUI(new Klondike());
        ui.init(null, null);
        ui.loop();
        ui.quit();
    }
}

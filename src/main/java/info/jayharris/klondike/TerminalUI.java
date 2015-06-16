package info.jayharris.klondike;

import com.googlecode.blacken.colors.ColorNames;
import com.googlecode.blacken.colors.ColorPalette;
import com.googlecode.blacken.swing.SwingTerminal;
import com.googlecode.blacken.terminal.BlackenKeys;
import com.googlecode.blacken.terminal.CursesLikeAPI;
import com.googlecode.blacken.terminal.TerminalInterface;

public class TerminalUI implements KlondikeUI {

    private Klondike klondike;

    private boolean quit;
    private ColorPalette palette;
    private CursesLikeAPI term = null;

    public TerminalUI(Klondike klondike) {
        this.klondike = klondike;
    }

    protected boolean loop() {
        int ch = BlackenKeys.KEY_NO_KEY;
        if (palette.containsKey("Black")) {
            term.setCurBackground("Black");
        }
        if (palette.containsKey("White")) {
            term.setCurForeground("White");
        }
        term.puts("Terminal Interface\n");
        term.puts("Press F10 to quit.\n");
        while (!this.quit) {
            term.puts(">");
            // getch automatically does a refresh
            ch = term.getch();
            term.puts(BlackenKeys.toString(ch));
            if (ch == BlackenKeys.KEY_F10) {
                this.quit = true;
            }
            term.puts("\n");
        }
        term.refresh();
        return this.quit;
    }

    public void init(TerminalInterface term, ColorPalette palette) {
        if (term == null) {
            this.term = new CursesLikeAPI(new SwingTerminal());
            this.term.init("Klondike", 25, 80);
        }
        else {
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

    public void quit() {
        this.quit = true;
        term.quit();
    }

    public static void main(String[] args) {
        TerminalUI ui= new TerminalUI(new Klondike());
        ui.init(null, null);
        ui.loop();
        ui.quit();
    }
}

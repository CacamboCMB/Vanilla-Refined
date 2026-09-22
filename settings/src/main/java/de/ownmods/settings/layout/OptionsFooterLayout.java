package de.ownmods.settings.layout;

/** Horizontal footer geometry in Minecraft GUI units, not physical pixels. */
public final class OptionsFooterLayout {
    public static final int GAP = 8;
    public static final int MARGIN = 12;
    private final int preferredWidth;

    /** Capture the original full-width Done button once, never its shrunken width. */
    public OptionsFooterLayout(int preferredWidth) {
        if (preferredWidth < 2) throw new IllegalArgumentException("Footer needs at least two units");
        this.preferredWidth = preferredWidth;
    }

    public record Row(int doneX, int doneWidth, int modsX, int modsWidth, int gap) {
        public int totalWidth() { return doneWidth + gap + modsWidth; }
        public int right() { return modsX + modsWidth; }
    }

    public Row atWidth(int screenWidth) {
        if (screenWidth < 2) throw new IllegalArgumentException("Screen needs at least two units");
        // Normal Minecraft screens are much wider. The fallback also keeps tiny test
        // viewports inside their bounds instead of producing negative button widths.
        int available = screenWidth >= 2 * MARGIN + 2 ? screenWidth - 2 * MARGIN : screenWidth;
        int span = Math.min(preferredWidth, available);
        int gap = Math.min(GAP, span - 2);
        int doneWidth = (span - gap) / 2;
        int modsWidth = span - gap - doneWidth;
        int x = (screenWidth - span) / 2;
        return new Row(x, doneWidth, x + doneWidth + gap, modsWidth, gap);
    }
}

package ddstweak.feature.tabgroups;

/** Pure row geometry: grouped entries retain vanilla row height and the 80-player cap. */
public final class TabGroupLayout {
    public static final int GAP_PIXELS = 3;
    private static final int MAX_ROWS_PER_COLUMN = 20;

    private TabGroupLayout() {}

    /** Non-spectator normal entries, spectator normal entries, then known fakes. */
    public static int group(boolean knownFake, boolean spectator) {
        return knownFake ? 2 : spectator ? 1 : 0;
    }

    /** Add spacing only for group transitions inside the same vanilla Tab column. */
    public static int[] offsets(int[] groups) {
        int[] offsets = new int[groups.length];
        if (groups.length < 2) return offsets;
        int columns = 1;
        int rows = groups.length;
        while (rows > MAX_ROWS_PER_COLUMN) {
            columns++;
            rows = (groups.length + columns - 1) / columns;
        }
        int offset = 0;
        for (int i = 1; i < groups.length; i++) {
            if (i % rows == 0) {
                offset = 0;
            } else if (groups[i] != groups[i - 1]) {
                offset += GAP_PIXELS;
            }
            offsets[i] = offset;
        }
        return offsets;
    }

    /** Keep the full vanilla Tab, including the footer, inside the scaled GUI height. */
    public static boolean fitsVertically(int listBottom, int footerLines, int screenHeight, int extra) {
        int footerHeight = footerLines == 0 ? 0 : 1 + footerLines * 9;
        return listBottom + footerHeight + extra + 1 <= screenHeight;
    }

    public static int maximumOffset(int[] offsets) {
        int maximum = 0;
        for (int offset : offsets) maximum = Math.max(maximum, offset);
        return maximum;
    }
}

package ddstweak.feature.tabgroups;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

final class TabGroupLayoutTest {
    @Test void knownFakesAlwaysFollowNormalSpectators() {
        assertEquals(0, TabGroupLayout.group(false, false));
        assertEquals(1, TabGroupLayout.group(false, true));
        assertEquals(2, TabGroupLayout.group(true, false));
        assertEquals(2, TabGroupLayout.group(true, true));
    }

    @Test void eachActualTransitionCreatesThreePixels() {
        int[] offsets = TabGroupLayout.offsets(new int[]{0, 0, 1, 1, 2, 2});
        assertArrayEquals(new int[]{0, 0, 3, 3, 6, 6}, offsets);
        assertEquals(6, TabGroupLayout.maximumOffset(offsets));
        assertArrayEquals(new int[]{0, 0}, TabGroupLayout.offsets(new int[]{1, 1}));
        assertArrayEquals(new int[0], TabGroupLayout.offsets(new int[0]));
    }

    @Test void aBoundaryAtTheNextColumnDoesNotInsertBlankRows() {
        int[] groups = new int[21];
        Arrays.fill(groups, 11, 21, 2); // 21 entries: two columns, 11 rows
        assertEquals(0, TabGroupLayout.maximumOffset(TabGroupLayout.offsets(groups)));
    }

    @Test void keepsSpacingOnlyWhenTheWholeTabIncludingFooterFits() {
        assertTrue(TabGroupLayout.fitsVertically(180, 0, 190, 6));
        assertFalse(TabGroupLayout.fitsVertically(180, 0, 186, 6));
        assertTrue(TabGroupLayout.fitsVertically(180, 2, 210, 6));
        assertFalse(TabGroupLayout.fitsVertically(180, 2, 205, 6));
        assertTrue(TabGroupLayout.fitsVertically(180, 2, 205, 0));
    }

    @Test void columnOffsetsResetAndEightyEntriesRemainEighty() {
        int[] groups = new int[80];
        Arrays.fill(groups, 6, 13, 1);
        Arrays.fill(groups, 13, 20, 2);
        Arrays.fill(groups, 20, 40, 0);
        Arrays.fill(groups, 40, 60, 1);
        Arrays.fill(groups, 60, 80, 2);
        int[] offsets = TabGroupLayout.offsets(groups);
        assertEquals(6, offsets[19]);
        assertEquals(0, offsets[20]);
        assertEquals(0, offsets[40]);
        assertEquals(0, offsets[60]);
        assertEquals(80, offsets.length);
        assertEquals(6, TabGroupLayout.maximumOffset(offsets));
    }
}

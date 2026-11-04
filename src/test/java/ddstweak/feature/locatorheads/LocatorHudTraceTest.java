package ddstweak.feature.locatorheads;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LocatorHudTraceTest {
    @Test void intervalSnapshotResetsWithoutModifyingPreviousSample(){
        var c=new LocatorHudTrace.Counters();
        c.add(LocatorHudTrace.Kind.HEAD);
        c.add(LocatorHudTrace.Kind.HEAD);
        c.add(LocatorHudTrace.Kind.ARROW_HIDDEN);
        long[] first=c.drain();
        assertEquals(2,first[LocatorHudTrace.Kind.HEAD.ordinal()]);
        assertEquals(1,first[LocatorHudTrace.Kind.ARROW_HIDDEN.ordinal()]);
        for(long value:c.drain())assertEquals(0,value);
        assertEquals(2,first[LocatorHudTrace.Kind.HEAD.ordinal()]);
    }
}

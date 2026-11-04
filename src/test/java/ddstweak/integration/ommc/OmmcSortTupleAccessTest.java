package ddstweak.integration.ommc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OmmcSortTupleAccessTest {
    public static final class Pair {
        private final Object a,b;
        public Pair(Object a,Object b){this.a=a;this.b=b;}
        public Object getA(){return a;}
        public Object getB(){return b;}
    }
    @Test void constructsAndInspectsOmmcStylePairs()throws Exception{
        Object instance=OmmcSortTupleAccess.create(Pair.class.getName(),18,44);
        assertArrayEquals(new int[]{18,44},OmmcSortTupleAccess.indices(instance));
        assertArrayEquals(new int[]{-1,999},OmmcSortTupleAccess.indices(new Pair(-1,999)));
    }
    @Test void malformedPairsCannotBeUsedForClicks(){
        assertNull(OmmcSortTupleAccess.indices(null));
        assertNull(OmmcSortTupleAccess.indices(new Object()));
        assertNull(OmmcSortTupleAccess.indices(new Pair("18",44)));
        assertNull(OmmcSortTupleAccess.indices(new Pair(18,null)));
    }
}

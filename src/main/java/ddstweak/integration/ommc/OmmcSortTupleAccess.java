package ddstweak.integration.ommc;

/** Strict, optional bridge for OMMC v0.8's own public Tuple API. */
public final class OmmcSortTupleAccess {
    private OmmcSortTupleAccess(){}

    public static Object create(String type,int start,int end)
            throws ReflectiveOperationException {
        return Class.forName(type).getConstructor(Object.class,Object.class)
                .newInstance(start,end);
    }

    /** Reject malformed or incompatible pairs before a pickup click is emitted. */
    public static int[] indices(Object pair){
        if(pair==null)return null;
        try{
            Object a=pair.getClass().getMethod("getA").invoke(pair);
            Object b=pair.getClass().getMethod("getB").invoke(pair);
            if(a instanceof Integer&&b instanceof Integer)
                return new int[]{(Integer)a,(Integer)b};
        }catch(ReflectiveOperationException|LinkageError|SecurityException ignored){}
        return null;
    }
}

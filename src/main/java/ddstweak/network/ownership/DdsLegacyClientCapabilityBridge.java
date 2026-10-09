package ddstweak.network.ownership;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.lang.reflect.Method;
import java.util.*;

/** Temporary C0 bridge that preserves capabilities advertised by an unmarked legacy DDS client. */
public final class DdsLegacyClientCapabilityBridge {
    private static final Logger LOGGER=LogManager.getLogger("DDS Tweak");
    private static final String REGISTRY_CLASS="carpetddsaddition.network.DdsPacketRegistry";
    private static volatile Snapshot snapshot;
    private static volatile boolean failed;
    private DdsLegacyClientCapabilityBridge(){}

    public static Snapshot snapshot(){
        Snapshot s=snapshot;if(s!=null||failed)return s==null?Snapshot.EMPTY:s;
        synchronized(DdsLegacyClientCapabilityBridge.class){
            if(snapshot!=null||failed)return snapshot==null?Snapshot.EMPTY:snapshot;
            try{
                Class<?> c=Class.forName(REGISTRY_CLASS,false,DdsLegacyClientCapabilityBridge.class.getClassLoader());
                Method c2s=c.getMethod("clientC2SPackets"),s2c=c.getMethod("clientS2CPackets");
                snapshot=new Snapshot(copy(c2s.invoke(null)),copy(s2c.invoke(null)));
            }catch(ReflectiveOperationException|ClassCastException ex){
                failed=true;LOGGER.error("Unable to read legacy DDS client capability registry; legacy co-install enhancement will fail closed",ex);
                return Snapshot.EMPTY;
            }
            return snapshot;
        }
    }

    private static Set<String> copy(Object value){
        if(!(value instanceof Collection<?>))throw new ClassCastException("DDS capability registry is not a Collection");
        Collection<?> values=(Collection<?>)value;
        LinkedHashSet<String> out=new LinkedHashSet<String>();
        for(Object v:values)if(v instanceof String&&!((String)v).trim().isEmpty())out.add((String)v);
        return Collections.unmodifiableSet(out);
    }

    public static final class Snapshot{
        public static final Snapshot EMPTY=new Snapshot(Collections.<String>emptySet(),Collections.<String>emptySet());
        private final Set<String> c2s,s2c;
        public Snapshot(Set<String> c2s,Set<String> s2c){
            this.c2s=Collections.unmodifiableSet(new LinkedHashSet<String>(c2s));
            this.s2c=Collections.unmodifiableSet(new LinkedHashSet<String>(s2c));
        }
        public Set<String> c2s(){return c2s;}public Set<String> s2c(){return s2c;}
        public boolean available(){return !c2s.isEmpty()||!s2c.isEmpty();}
        @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof Snapshot))return false;Snapshot other=(Snapshot)o;return c2s.equals(other.c2s)&&s2c.equals(other.s2c);}
        @Override public int hashCode(){return Objects.hash(c2s,s2c);}
    }
}
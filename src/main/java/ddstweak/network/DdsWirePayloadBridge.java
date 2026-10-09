package ddstweak.network;

import ddstweak.network.ownership.*;
import net.minecraft.nbt.CompoundTag;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.lang.reflect.*;

/** Optional same-JVM bridge between DDS Tweak Payload and a handoff DDS Payload class. */
public final class DdsWirePayloadBridge {
    private static final Logger LOGGER=LogManager.getLogger("DDS Tweak");
    private static final String DDS_PAYLOAD_CLASS="carpetddsaddition.network.DdsProtocol$Payload";
    private static volatile Access access;private static volatile boolean failed;
    private DdsWirePayloadBridge(){}
    public static Object outbound(String id,CompoundTag data){return outbound(DdsLocalOwnershipResolver.current(),id,data);}
    public static DdsProtocol.Payload inbound(Object payload){return inbound(DdsLocalOwnershipResolver.current(),payload);}
    static Object outbound(DdsLocalOwnershipDecision decision,String id,CompoundTag data){if(!usesDdsPayload(decision))return new DdsProtocol.Payload(id,data);Access a=access();if(a==null)return null;try{return a.ctor.newInstance(id,data);}catch(ReflectiveOperationException ex){fail(ex);return null;}}
    static DdsProtocol.Payload inbound(DdsLocalOwnershipDecision decision,Object payload){if(!usesDdsPayload(decision))return payload instanceof DdsProtocol.Payload?(DdsProtocol.Payload)payload:null;if(payload==null||!DDS_PAYLOAD_CLASS.equals(payload.getClass().getName()))return null;Access a=access();if(a==null)return null;try{return new DdsProtocol.Payload((String)a.packetId.invoke(payload),(CompoundTag)a.data.invoke(payload));}catch(ReflectiveOperationException|ClassCastException ex){fail(ex);return null;}}
    private static boolean usesDdsPayload(DdsLocalOwnershipDecision d){return d==DdsLocalOwnershipDecision.LEGACY_COMPAT||d==DdsLocalOwnershipDecision.HANDOFF_DDS_PAYLOAD_CODEC||d==DdsLocalOwnershipDecision.HANDOFF_DDS_PAYLOAD_READER_MAP;}
    private static Access access(){Access a=access;if(a!=null||failed)return a;synchronized(DdsWirePayloadBridge.class){if(access!=null||failed)return access;try{Class<?> c=Class.forName(DDS_PAYLOAD_CLASS,false,DdsWirePayloadBridge.class.getClassLoader());Constructor<?> ctor=c.getConstructor(String.class,CompoundTag.class);Method packetId=c.getMethod("packetId"),data=c.getMethod("data");access=new Access(ctor,packetId,data);}catch(ReflectiveOperationException ex){fail(ex);}return access;}}
    private static void fail(Throwable ex){if(failed)return;failed=true;LOGGER.error("DDS payload bridge unavailable; DDS integration cannot use local handoff payload",ex);}
    private static final class Access{
        private final Constructor<?> ctor;private final Method packetId,data;
        private Access(Constructor<?> ctor,Method packetId,Method data){this.ctor=ctor;this.packetId=packetId;this.data=data;}
    }
}

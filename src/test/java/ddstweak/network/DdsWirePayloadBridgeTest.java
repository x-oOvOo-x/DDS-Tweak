package ddstweak.network;

import ddstweak.network.ownership.DdsLocalOwnershipDecision;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DdsWirePayloadBridgeTest {
    @Test void standaloneUsesTweakPayload(){CompoundTag data=new CompoundTag();data.putInt("x",1);Object raw=DdsWirePayloadBridge.outbound(DdsLocalOwnershipDecision.TWEAK_STANDALONE,"test",data);assertInstanceOf(DdsProtocol.Payload.class,raw);DdsProtocol.Payload decoded=DdsWirePayloadBridge.inbound(DdsLocalOwnershipDecision.TWEAK_STANDALONE,raw);assertNotNull(decoded);assertEquals("test",decoded.packetId());assertEquals(1,decoded.data().getIntOr("x",0));}
    @Test void standaloneIgnoresForeignPayloadEnvelope(){assertNull(DdsWirePayloadBridge.inbound(DdsLocalOwnershipDecision.TWEAK_STANDALONE,new Object()));}
    @Test void legacyCompatRequiresLocalDdsPayloadClass(){assertNull(DdsWirePayloadBridge.outbound(DdsLocalOwnershipDecision.LEGACY_COMPAT,"legacy",new CompoundTag()));}
    @Test void legacyHandoffUsesTweakPayloadWithoutDdsReflection(){CompoundTag data=new CompoundTag();Object raw=DdsWirePayloadBridge.outbound(DdsLocalOwnershipDecision.HANDOFF_LEGACY_CUSTOM_PAYLOAD,"legacy_handoff",data);assertInstanceOf(DdsProtocol.Payload.class,raw);assertNotNull(DdsWirePayloadBridge.inbound(DdsLocalOwnershipDecision.HANDOFF_LEGACY_CUSTOM_PAYLOAD,raw));}
}

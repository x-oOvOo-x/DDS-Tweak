package ddstweak.network;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class DdsClientCapabilityRegistryTest {
    @Test void registersAndSnapshotsCapabilities(){DdsClientCapabilityRegistry r=new DdsClientCapabilityRegistry();r.registerC2S("a");r.registerOrderedS2C("b",(c,t)->{});r.registerS2C("c",t->{});assertEquals(Set.of("a"),r.snapshotC2S());assertEquals(Set.of("b","c"),r.snapshotS2C());assertTrue(r.hasC2S("a"));assertNotNull(r.orderedS2C("b"));assertNotNull(r.asyncS2C("c"));}
    @Test void rejectsDuplicateC2S(){DdsClientCapabilityRegistry r=new DdsClientCapabilityRegistry();r.registerC2S("a");assertThrows(IllegalStateException.class,()->r.registerC2S("a"));}
    @Test void rejectsHandlerModeCollision(){DdsClientCapabilityRegistry r=new DdsClientCapabilityRegistry();r.registerS2C("x",(CompoundTag t)->{});assertThrows(IllegalStateException.class,()->r.registerOrderedS2C("x",(c,t)->{}));}
    @Test void snapshotsAreImmutable(){DdsClientCapabilityRegistry r=new DdsClientCapabilityRegistry();r.registerC2S("a");Set<String> s=r.snapshotC2S();assertThrows(UnsupportedOperationException.class,()->s.add("b"));}
}

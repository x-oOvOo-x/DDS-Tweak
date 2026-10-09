package ddstweak.integration.dds.fakeplayer;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class DdsFakePlayerMenuOpenStateTest {
    @Test void parsesLegacyAndSchemaOneMetadata(){
        DdsFakePlayerOpenContext legacy=DdsFakePlayerOpenContext.parse(tag(7,1));assertNotNull(legacy);assertEquals(0,legacy.menuSchema());assertTrue(legacy.mirrorCompatible());assertNull(legacy.targetUuid());assertEquals("",legacy.targetName());assertFalse(legacy.hasExplicitSupportedActions());assertTrue(legacy.supportsAction(DdsFakePlayerAction.DROP_ALL));
        UUID id=UUID.randomUUID();CompoundTag full=tag(8,3);full.putInt("menu_schema",1);full.putString("target_uuid",id.toString());full.putString("target_name","bot");full.putString("supported_actions","ender_drop_all,disconnect");DdsFakePlayerOpenContext current=DdsFakePlayerOpenContext.parse(full);assertNotNull(current);assertEquals(1,current.menuSchema());assertTrue(current.mirrorCompatible());assertEquals(id,current.targetUuid());assertEquals("bot",current.targetName());assertTrue(current.hasExplicitSupportedActions());assertTrue(current.supportsAction(DdsFakePlayerAction.DISCONNECT));assertFalse(current.supportsAction(DdsFakePlayerAction.DROP_ALL));
    }
    @Test void unknownSchemaAndLayoutAreRetainedForSafeReject(){
        CompoundTag schema=tag(9,1);schema.putInt("menu_schema",2);DdsFakePlayerOpenContext a=DdsFakePlayerOpenContext.parse(schema);assertNotNull(a);assertFalse(a.mirrorCompatible());assertFalse(a.supportsAction(DdsFakePlayerAction.DROP_ALL));
        DdsFakePlayerOpenContext b=DdsFakePlayerOpenContext.parse(tag(10,99));assertNotNull(b);assertFalse(b.mirrorCompatible());
    }
    @Test void parserBoundsUntrustedIdentityAndActions(){
        CompoundTag t=tag(11,1);t.putInt("menu_schema",1);t.putString("target_uuid","x".repeat(100));t.putString("target_name","n".repeat(100));t.putString("supported_actions","drop_all,"+"x".repeat(600));DdsFakePlayerOpenContext c=DdsFakePlayerOpenContext.parse(t);assertNotNull(c);assertNull(c.targetUuid());assertEquals("",c.targetName());assertTrue(c.hasExplicitSupportedActions());assertTrue(c.supportedActions().isEmpty());
    }
    @Test void peekValidateCommitIsTwoPhaseAndOneShot(){
        DdsFakePlayerMenuOpenState<Object> state=new DdsFakePlayerMenuOpenState<>(100L);Object connection=new Object();DdsFakePlayerOpenContext ctx=DdsFakePlayerOpenContext.parse(tag(7,1));state.expect(connection,ctx,10L);assertSame(ctx,state.peek(connection,7,20L));assertSame(ctx,state.peek(connection,7,21L));assertTrue(state.commit(connection,7,ctx));assertNull(state.peek(connection,7,22L));assertFalse(state.commit(connection,7,ctx));
    }
    @Test void staleMarkersAreDiscardedUntilMatchingContainer(){
        DdsFakePlayerMenuOpenState<Object> state=new DdsFakePlayerMenuOpenState<>(100L);Object connection=new Object();DdsFakePlayerOpenContext first=DdsFakePlayerOpenContext.parse(tag(7,1)),second=DdsFakePlayerOpenContext.parse(tag(8,3));state.expect(connection,first,10L);state.expect(connection,second,11L);assertSame(second,state.peek(connection,8,20L));assertTrue(state.commit(connection,8,second));assertNull(state.peek(connection,7,21L));
    }
    @Test void connectionChangeExpiryAndClearFailClosed(){
        DdsFakePlayerMenuOpenState<Object> state=new DdsFakePlayerMenuOpenState<>(10L);Object a=new Object(),b=new Object();DdsFakePlayerOpenContext first=DdsFakePlayerOpenContext.parse(tag(7,1)),second=DdsFakePlayerOpenContext.parse(tag(8,6));state.expect(a,first,100L);assertNull(state.peek(a,7,111L));state.expect(a,first,120L);state.expect(b,second,121L);assertNull(state.peek(a,7,122L));assertSame(second,state.peek(b,8,122L));state.clear();assertNull(state.peek(b,8,123L));
    }
    private static CompoundTag tag(int id,int layout){CompoundTag t=new CompoundTag();t.putInt("container_id",id);t.putInt("layout",layout);return t;}
}

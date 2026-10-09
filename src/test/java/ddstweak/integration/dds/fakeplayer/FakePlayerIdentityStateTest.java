package ddstweak.integration.dds.fakeplayer;

import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class FakePlayerIdentityStateTest {
    private static final UUID FAKE=UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REAL=UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test void absenceIsUnknownUntilCompleteSnapshot(){
        FakePlayerIdentityState state=new FakePlayerIdentityState();
        state.applyChange(FAKE,true);
        assertEquals(PlayerInventoryTargetType.UNKNOWN,state.resolve(REAL));
        assertTrue(state.acceptSnapshot(FAKE.toString()));
        assertEquals(PlayerInventoryTargetType.FAKE,state.resolve(FAKE));
        assertEquals(PlayerInventoryTargetType.REAL,state.resolve(REAL));
    }
    @Test void logoutRemovesFakeAndDisconnectRevokesSnapshot(){
        FakePlayerIdentityState state=new FakePlayerIdentityState();
        state.acceptSnapshot(FAKE.toString());
        state.applyChange(FAKE,false);
        assertEquals(PlayerInventoryTargetType.REAL,state.resolve(FAKE));
        state.clear();
        assertEquals(PlayerInventoryTargetType.UNKNOWN,state.resolve(FAKE));
    }
    @Test void malformedSnapshotCannotLeaveAnOldAuthoritativeSet(){
        FakePlayerIdentityState state=new FakePlayerIdentityState();
        state.acceptSnapshot(FAKE.toString());
        assertFalse(state.acceptSnapshot("bad-uuid"));
        assertEquals(PlayerInventoryTargetType.UNKNOWN,state.resolve(FAKE));
    }
}

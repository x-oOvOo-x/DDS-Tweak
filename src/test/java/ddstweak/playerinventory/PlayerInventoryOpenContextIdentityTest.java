package ddstweak.playerinventory;

import ddstweak.integration.dds.playerinventory.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Fake Ender UI requires proof tied to the opened menu, not a generic Ender title. */
final class PlayerInventoryOpenContextIdentityTest {
    @Test void genericEnderMenuCannotInheritAnUnboundFakeOwner() {
        PlayerInventoryContext fake=context("dds_qca",PlayerInventoryTargetType.FAKE,1);
        assertTrue(PlayerInventoryOpenContext.isUnverifiedFakeEnder(-1,fake));
        assertFalse(PlayerInventoryOpenContext.isUnverifiedFakeEnder(23,fake));
    }

    @Test void providerFakeHintsAndStrictSessionsAreNotProofForAnUnboundNewMenu() {
        assertTrue(PlayerInventoryOpenContext.isUnverifiedFakeEnder(-1,
                context("gca",PlayerInventoryTargetType.FAKE,1)));
        assertTrue(PlayerInventoryOpenContext.isUnverifiedFakeEnder(-1,
                context("org",PlayerInventoryTargetType.FAKE,3)));
    }

    @Test void realAndUnknownEnderContextsDoNotGainFakeAuthority() {
        assertFalse(PlayerInventoryOpenContext.isUnverifiedFakeEnder(-1,
                context("dds_qca",PlayerInventoryTargetType.REAL,1)));
        assertFalse(PlayerInventoryOpenContext.isUnverifiedFakeEnder(-1,
                context("dds_qca",PlayerInventoryTargetType.UNKNOWN,1)));
        assertFalse(PlayerInventoryOpenContext.isUnverifiedFakeEnder(-1,null));
    }

    private static PlayerInventoryContext context(String source,PlayerInventoryTargetType type,int schema) {
        return new PlayerInventoryContext(23,schema,source,UUID.randomUUID(),"target",type,
                PlayerInventoryTargetState.ONLINE,PlayerInventoryKind.ENDER_CHEST,
                PlayerInventoryLayouts.ENDER_3_V1,1,"test",
                Collections.<PlayerInventoryAction>emptySet(),
                Collections.<PlayerInventoryInteractionCapability>emptySet(),
                PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
    }
}

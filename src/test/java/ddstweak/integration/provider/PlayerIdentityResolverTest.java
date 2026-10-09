package ddstweak.integration.provider;

import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.org.playerinventory.OrgFakePlayerIdentityClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerIdentityResolverTest {
    @AfterEach void clear(){OrgFakePlayerIdentityClient.clear();}

    @Test void orgPositiveIdentityIsFakeButRemovalReturnsUnknown(){
        UUID uuid=UUID.randomUUID();
        PlayerInventoryContext stale=context("org",PlayerInventoryTargetType.FAKE,uuid);
        assertEquals(PlayerInventoryTargetType.UNKNOWN,PlayerIdentityResolver.resolve(uuid));
        assertEquals(PlayerInventoryTargetType.UNKNOWN,PlayerIdentityResolver.currentType(stale));
        OrgFakePlayerIdentityClient.receive(uuid,true);
        assertEquals(PlayerInventoryTargetType.FAKE,PlayerIdentityResolver.resolve(uuid));
        assertEquals(PlayerInventoryTargetType.FAKE,PlayerIdentityResolver.currentType(stale));
        OrgFakePlayerIdentityClient.receive(uuid,false);
        assertEquals(PlayerInventoryTargetType.UNKNOWN,PlayerIdentityResolver.resolve(uuid));
        assertEquals(PlayerInventoryTargetType.UNKNOWN,PlayerIdentityResolver.currentType(stale));
        assertFalse(PlayerIdentityResolver.isAuthoritativeFake(stale));
    }

    @Test void ddsOwnedFakeContextIsAuthoritative(){
        assertTrue(PlayerIdentityResolver.isAuthoritativeFake(context("dds",PlayerInventoryTargetType.FAKE,UUID.randomUUID())));
        assertTrue(PlayerIdentityResolver.isAuthoritativeFake(context("dds_qca",PlayerInventoryTargetType.FAKE,UUID.randomUUID())));
    }

    @Test void providerHeuristicCannotUnlockFakeOnlyRpc(){
        assertFalse(PlayerIdentityResolver.isAuthoritativeFake(context("gca",PlayerInventoryTargetType.FAKE,UUID.randomUUID())));
        assertFalse(PlayerIdentityResolver.isAuthoritativeFake(context("org",PlayerInventoryTargetType.UNKNOWN,UUID.randomUUID())));
    }

    @Test void realOrOfflineTargetsAlwaysFailClosed(){
        assertFalse(PlayerIdentityResolver.isAuthoritativeFake(context("dds",PlayerInventoryTargetType.REAL,UUID.randomUUID())));
        PlayerInventoryContext offline=new PlayerInventoryContext(1,1,"dds",UUID.randomUUID(),"bot",
                PlayerInventoryTargetType.FAKE,PlayerInventoryTargetState.OFFLINE,PlayerInventoryKind.ENDER_CHEST,
                PlayerInventoryLayouts.ENDER_3_V1,1,"test",Collections.<PlayerInventoryAction>emptySet(),
                Collections.<PlayerInventoryInteractionCapability>emptySet(),PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
        assertFalse(PlayerIdentityResolver.isAuthoritativeFake(offline));
    }

    private static PlayerInventoryContext context(String source,PlayerInventoryTargetType type,UUID uuid){
        return new PlayerInventoryContext(1,1,source,uuid,"bot",type,PlayerInventoryTargetState.ONLINE,
                PlayerInventoryKind.ENDER_CHEST,PlayerInventoryLayouts.ENDER_3_V1,1,"test",
                Collections.<PlayerInventoryAction>emptySet(),Collections.<PlayerInventoryInteractionCapability>emptySet(),
                PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
    }
}

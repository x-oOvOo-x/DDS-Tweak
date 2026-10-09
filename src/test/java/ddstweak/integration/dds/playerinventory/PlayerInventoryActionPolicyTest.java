package ddstweak.integration.dds.playerinventory;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryActionPolicyTest {
    private static final Path JAVA=Path.of("").toAbsolutePath().resolve("src/main/java");

    @Test void actionKindsHaveOneCentralPolicy(){
        EnumSet<PlayerInventoryAction> inventory=EnumSet.of(
                PlayerInventoryAction.SELECT_HOTBAR,
                PlayerInventoryAction.ATTACK_ONCE,PlayerInventoryAction.ATTACK_INTERVAL,PlayerInventoryAction.ATTACK_CONTINUOUS,PlayerInventoryAction.STOP_ATTACK,
                PlayerInventoryAction.USE_ONCE,PlayerInventoryAction.USE_INTERVAL,PlayerInventoryAction.USE_CONTINUOUS,PlayerInventoryAction.STOP_USE,
                PlayerInventoryAction.STOP_ALL,PlayerInventoryAction.DROP_ALL,PlayerInventoryAction.DISCONNECT);
        EnumSet<PlayerInventoryAction> ender=EnumSet.of(
                PlayerInventoryAction.ENDER_DROP_ALL,PlayerInventoryAction.ENDER_STORE_ALL,PlayerInventoryAction.ENDER_TAKE_ALL,
                PlayerInventoryAction.TOGGLE_SNEAK,PlayerInventoryAction.DISCONNECT);

        for(PlayerInventoryAction action:PlayerInventoryAction.values()){
            assertEquals(inventory.contains(action),action.allowedIn(PlayerInventoryKind.INVENTORY),action.name());
            assertEquals(inventory.contains(action),action.allowedIn(PlayerInventoryKind.CONTROLLER),action.name());
            assertEquals(ender.contains(action),action.allowedIn(PlayerInventoryKind.ENDER_CHEST),action.name());
            assertFalse(action.allowedIn(null),action.name());
        }
    }

    @Test void contextsDiscardActionsFromTheWrongMenuKind(){
        PlayerInventoryContext inventory=context(PlayerInventoryKind.INVENTORY);
        PlayerInventoryContext ender=context(PlayerInventoryKind.ENDER_CHEST);

        assertTrue(inventory.supports(PlayerInventoryAction.ATTACK_CONTINUOUS));
        assertTrue(inventory.supports(PlayerInventoryAction.DROP_ALL));
        assertFalse(inventory.supports(PlayerInventoryAction.ENDER_TAKE_ALL));
        assertFalse(inventory.supports(PlayerInventoryAction.TOGGLE_SNEAK));

        assertTrue(ender.supports(PlayerInventoryAction.ENDER_TAKE_ALL));
        assertTrue(ender.supports(PlayerInventoryAction.TOGGLE_SNEAK));
        assertFalse(ender.supports(PlayerInventoryAction.ATTACK_CONTINUOUS));
        assertFalse(ender.supports(PlayerInventoryAction.DROP_ALL));
    }

    @Test void everyPublicActionBindingRechecksTheSamePolicy() throws IOException {
        for(String path:new String[]{
                "ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java",
                "ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java",
                "ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionActionBinding.java"}){
            String source=Files.readString(JAVA.resolve(path));
            assertTrue(source.contains("action.allowedIn(menu.kind())"),path);
            assertTrue(source.contains("menu.supports(action)"),path);
        }
        String network=Files.readString(JAVA.resolve("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java"));
        assertTrue(network.contains("action.allowedIn(context.inventoryKind())"));
    }

    private static PlayerInventoryContext context(PlayerInventoryKind kind){
        return new PlayerInventoryContext(1,1,"test",null,"target",PlayerInventoryTargetType.FAKE,
                PlayerInventoryTargetState.ONLINE,kind,
                kind==PlayerInventoryKind.ENDER_CHEST?PlayerInventoryLayouts.ENDER_3_V1:PlayerInventoryLayouts.ORG_INVENTORY_V1,
                1,"test",EnumSet.allOf(PlayerInventoryAction.class),
                EnumSet.noneOf(PlayerInventoryInteractionCapability.class),PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
    }
}

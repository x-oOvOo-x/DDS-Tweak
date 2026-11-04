package ddstweak.integration.gca.playerinventory;

import ddstweak.integration.dds.playerinventory.PlayerInventoryKind;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class GcaPlayerInteractionIntentTest {
    @Test void serverDisplayMismatchDoesNotInvalidateTheClickedProfile(){
        String displayed="[Log head] 假的Log";
        for(PlayerInventoryKind kind:PlayerInventoryKind.values()){
            if(kind!=PlayerInventoryKind.INVENTORY&&kind!=PlayerInventoryKind.ENDER_CHEST
                    &&kind!=PlayerInventoryKind.CONTROLLER)continue;
            String key=kind==PlayerInventoryKind.INVENTORY?GcaPlayerInventoryAdapter.TITLE_INVENTORY
                    :kind==PlayerInventoryKind.ENDER_CHEST?GcaPlayerInventoryAdapter.TITLE_ENDER
                    :GcaPlayerInventoryAdapter.TITLE_CONTROLLER;
            Component title=Component.translatable(key,Component.literal(displayed));
            assertTrue(GcaPlayerInteractionIntent.matchesTitle(title,displayed));
            assertFalse(GcaPlayerInteractionIntent.matchesTitle(title,"Log"));
        }
    }

    @Test void menuRecognitionRequiresGcaTypeAndRejectsUnrelatedMenu()throws IOException{
        // MenuType.GENERIC_* initializes Minecraft's registries, unavailable in
        // a plain unit-test JVM. Check the production guard without bootstrapping
        // client registries; real menu binding is covered by in-game tests.
        Path path=Path.of("").toAbsolutePath().resolve(
                "src/main/java/ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        String adapter=Files.readString(path);
        assertTrue(adapter.contains("return title!=null&&resolve(type,title)!=null"));
        assertTrue(adapter.contains("TITLE_INVENTORY.equals(key)&&type==MenuType.GENERIC_9x6"));
        assertTrue(adapter.contains("TITLE_CONTROLLER.equals(key)&&type==MenuType.GENERIC_9x3"));
        assertTrue(adapter.contains("TITLE_ENDER.equals(key)&&type==MenuType.GENERIC_9x6"));
        assertTrue(adapter.contains("if(TITLE_INVENTORY.equals(key)&&type==MenuType.GENERIC_9x6"));
        assertTrue(adapter.contains("if(type==MenuType.GENERIC_9x6&&isRenderedGcaInventory(visible,target))"));
    }

    @Test void expiredOrInvalidatedInteractionCannotAuthorizeAnotherMenu(){
        assertTrue(GcaPlayerInteractionIntent.fresh(100L,100L));
        assertTrue(GcaPlayerInteractionIntent.fresh(100L,3_000_000_100L));
        assertFalse(GcaPlayerInteractionIntent.fresh(100L,3_000_000_101L));
        assertFalse(GcaPlayerInteractionIntent.fresh(100L,99L));
    }

    @Test void clientCaptureIsBoundToConnectionAndNeverGrantsFakeAuthority()throws IOException{
        Path root=Path.of("").toAbsolutePath().resolve("src/main");
        String mixin=Files.readString(root.resolve("java/ddstweak/mixin/playerinventory/GcaPlayerInteractionMixin.java"));
        String intent=Files.readString(root.resolve("java/ddstweak/integration/gca/playerinventory/GcaPlayerInteractionIntent.java"));
        String adapter=Files.readString(root.resolve("java/ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java"));
        String config=Files.readString(root.resolve("resources/dds-tweak.mixins.json"));
        assertTrue(config.contains("playerinventory.GcaPlayerInteractionMixin"));
        assertTrue(mixin.contains("@Inject(method=\"startUseItem\",at=@At(\"HEAD\"))"));
        assertTrue(intent.contains("minecraft.getConnection()!=previous.connection"));
        assertTrue(intent.contains("pending=null;"));
        assertTrue(intent.contains("previous.containerId!=containerId"));
        assertTrue(intent.contains("previous.containerId=containerId"));
        assertTrue(intent.contains("GcaPlayerInventoryAdapter.isGcaMenu(type,title)"));
        assertFalse(intent.contains("||!matchesTitle(title,kind,previous.displayName)"));
        assertTrue(intent.contains("registerConnectionResetHook"));
        assertTrue(adapter.contains("GcaPlayerInteractionIntent.consume(containerId,title,kind)"));
        String menuMixin=Files.readString(root.resolve("java/ddstweak/mixin/fakeplayer/MenuScreensMixin.java"));
        int bind=menuMixin.indexOf("GcaPlayerInteractionIntent.observeMenu(type,containerId,title)");
        int adapterCall=menuMixin.indexOf("GcaPlayerInventoryAdapter.tryContext(type,containerId,title)");
        assertTrue(bind>=0&&adapterCall>bind,"bind click before opening GCA adapter");
        assertTrue(menuMixin.contains("GcaPlayerInventoryAdapter.tryContext(type,containerId,title)"));
        assertTrue(adapter.contains("PlayerIdentityResolver.resolve(clicked.uuid)"));
        assertFalse(intent.contains("getLatency()"));
        assertFalse(intent.contains("PlayerInventoryTargetType.FAKE"));
    }
}

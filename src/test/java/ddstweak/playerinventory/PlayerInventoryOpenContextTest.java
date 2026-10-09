package ddstweak.playerinventory;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryOpenContextTest {
    private static final Path JAVA=Path.of("").toAbsolutePath().resolve("src/main/java");

    @Test void gcaShulkerReturnCarriesItsInventoryContextIntoBothScreenCreationPaths() throws IOException {
        String navigation=read("ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String upgrade=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        assertTrue(navigation.contains("nestedInventoryParent!=null&&nestedInventoryChild==menu.containerId"));
        assertTrue(navigation.contains("PlayerIdentityResolver.navigationFallback(nestedInventoryParent)"));
        assertTrue(navigation.contains("PlayerInventoryOpenContext.expectInventory(parent)"));
        assertTrue(dispatch.contains("PlayerInventoryOpenContext.consumeInventory(type,containerId,title)"));
        assertTrue(upgrade.contains("PlayerInventoryOpenContext.consumeInventory(type,containerId,title)"));
    }

    @Test void gcaReopenRequiresTheSameTargetAndLayoutWithoutClearingTheEnderIntent() throws IOException {
        String context=read("ddstweak/playerinventory/PlayerInventoryOpenContext.java");
        assertTrue(context.contains("PlayerInventoryLayouts.GCA_INVENTORY_V1.equals(source.layoutId())"));
        assertTrue(context.contains("type!=MenuType.GENERIC_9x6"));
        assertTrue(context.contains("matchesInventoryTitle(title,source)"));
        assertTrue(context.contains("matchesEnderTitle(title,source)"));
        assertTrue(context.contains("gca.player.inventory"));
        assertTrue(context.contains("carpet.commands.player.inventory.offline.display_name"));
        assertTrue(context.contains("carpet-org-addition.operation.offline_player_name"));
        assertTrue(context.contains("new PlayerInventoryContext(containerId,source.contextSchema()"));
        assertTrue(context.contains("source.layoutId(),source.layoutSchema()"));
        assertTrue(context.contains("if(pendingEnder==null)return null;"));
        assertTrue(context.contains("clearEnder();"));
        assertTrue(context.contains("clearInventory();"));
    }

    @Test void nestedReturnKeepsRealAndOfflineEnderParentsInsteadOfFakeOnlyFiltering() throws IOException {
        String navigation=read("ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        assertTrue(navigation.contains("mirror.context().targetUuid()!=null&&canUseNavigateBack()"));
        assertFalse(navigation.contains("mirror.context().targetType()!=PlayerInventoryTargetType.REAL"));
        assertTrue(navigation.contains("PlayerInventoryOpenContext.expectEnder(PlayerIdentityResolver.navigationFallback(nestedEnderParent))"));
    }

    private static String read(String path) throws IOException { return Files.readString(JAVA.resolve(path)); }
}

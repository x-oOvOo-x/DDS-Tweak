package ddstweak.integration.dds.fakeplayer;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class DdsFakePlayerResourceContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath(),SRC=ROOT.resolve("src/main/resources"),RES=Path.of(System.getProperty("dds.testBuildDir")).resolve("resources/main");
    private static final List<String> UI=List.of(
            "textures/gui/container/gca_fake_player_inventory.png","textures/gui/container/gca_fake_player_ender_chest_3.png","textures/gui/container/gca_fake_player_ender_chest_6.png",
            "textures/gui/gca/action_button_normal.png","textures/gui/gca/action_button_hover.png","textures/gui/gca/action_button_active.png","textures/gui/gca/action_icons.png","textures/gui/gca/hotbar_number_buttons.png","textures/gui/gca/hotbar_selected.png","textures/gui/gca/interval_arrows.png","textures/gui/gca/interval_input.png","textures/gui/gca/interval_input_focused.png");

    @Test void fakePlayerResourcesAreSourceOwned(){Path assets=SRC.resolve("assets/dds-tweak"),pack=SRC.resolve("resourcepacks/dds_fake_player_ui");UI.forEach(path->{assertTrue(Files.isRegularFile(assets.resolve(path)),"source default: "+path);assertTrue(Files.isRegularFile(pack.resolve("assets/dds-tweak").resolve(path)),"source cozy: "+path);});for(String name:List.of("pack.mcmeta","NOTICE.txt","LICENSE-CozyUI-Plus.txt"))assertTrue(Files.isRegularFile(pack.resolve(name)),name);}
    @Test void buildNoLongerDownloadsFakePlayerUi() throws IOException {String build=Files.readString(ROOT.resolve("build.gradle"));for(String token:List.of("prepareFakePlayerUiResources","generated/fake-player-ui","raw.githubusercontent.com/x-oOvOo-x/Carpet-DDS-Addition","fakePlayerUiSourceCommit","fakePlayerUiFiles"))assertFalse(build.contains(token),token);}
    @Test void defaultFakePlayerUiBelongsToDdsTweak(){Path assets=RES.resolve("assets/dds-tweak");UI.forEach(path->assertTrue(Files.isRegularFile(assets.resolve(path)),path));}
    @Test void cozyUiIsOptionalBuiltinPackWithAttribution(){Path pack=RES.resolve("resourcepacks/dds_fake_player_ui");assertTrue(Files.isRegularFile(pack.resolve("pack.mcmeta")));assertTrue(Files.isRegularFile(pack.resolve("NOTICE.txt")));assertTrue(Files.isRegularFile(pack.resolve("LICENSE-CozyUI-Plus.txt")));Path assets=pack.resolve("assets/dds-tweak");UI.forEach(path->assertTrue(Files.isRegularFile(assets.resolve(path)),path));}
    @Test void builtinPackMetadataIsGeneratedPerMinecraftTarget() throws IOException {
        String build=Files.readString(ROOT.resolve("common.gradle"));
        for(String token:List.of("resourcePackFormat","resourcePackMinFormat","packMeta.pack.pack_format","packMeta.pack.min_format","packMeta.pack.max_format","supported_formats"))assertTrue(build.contains(token),token);
        String built=Files.readString(RES.resolve("resourcepacks/dds_fake_player_ui/pack.mcmeta"));
        assertTrue(built.contains("\"min_format\": 75"),built);
        assertTrue(built.contains("\"max_format\": 75"),built);
        assertFalse(built.contains("\"pack_format\""),built);
    }
    @Test void legacyIntervalTextKeepsCenteredLayout() throws IOException {String source=Files.readString(ROOT.resolve("src/main/java/ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java"));for(String token:List.of("LegacyCenteredEditBox extends EditBox","centerOffset()","font.width(getValue())","setX(x+offset)","mouseX-centerOffset()"))assertTrue(source.contains(token),token);}
    @Test void screensUseTweakResourceNamespace() throws IOException {String source=Files.readString(ROOT.resolve("src/main/java/ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java"));assertTrue(source.contains("DDSTweak.MOD_ID"));assertFalse(source.contains("Identifier.fromNamespaceAndPath(\"carpet-dds-addition\""));}
    @Test void actionClicksUseTheScreenOwnedMirrorMenu() throws IOException {
        String network=Files.readString(ROOT.resolve("src/main/java/ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java")),screen=Files.readString(ROOT.resolve("src/main/java/ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java"));
        assertTrue(network.contains("PlayerInventoryActionBinding actionBinding(PlayerInventoryContext context)"));
        assertTrue(network.contains("DdsFakePlayerLegacyContextAdapter.legacyAction(action)"));
        assertTrue(screen.contains("menu.send(action,value)"));
        assertFalse(screen.contains("DdsFakePlayerClientNetwork"));
    }
    @Test void equipmentSlotsKeepVanillaEmptyIcons() throws IOException {String source=Files.readString(ROOT.resolve("src/main/java/ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java"));for(String token:List.of("InventoryMenu.EMPTY_ARMOR_SLOT_HELMET","InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE","InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS","InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS","InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD","getNoItemIcon()"))assertTrue(source.contains(token),token);}
}

package ddstweak.integration.dds.playerinventory;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryAutomationContractTest{
    private static final Path ROOT=Path.of("").toAbsolutePath(),JAVA=ROOT.resolve("src/main/java");

    @Test void ipnStorageAreaSupportsNonContiguousProviderRanges()throws IOException{
        String compat=read("ddstweak/integration/dds/fakeplayer/client/ipn/DdsIpnCompat.java");
        assertTrue(compat.contains("storageIndices"));
        assertTrue(compat.contains("appendRange(indices,menu.targetMainStart(),menu.targetMainEnd())"));
        assertTrue(compat.contains("appendRange(indices,menu.targetHotbarStart(),menu.targetHotbarEnd())"));
        assertTrue(compat.contains("indices=sortable?sortIndices(menu):storageIndices(menu)"));
    }

    @Test void itemScrollerUsesSemanticHoveredRangesAndCapabilityGate()throws IOException{
        String mixin=read("ddstweak/mixin/integration/itemscroller/InventoryUtilsMixin.java");
        assertTrue(mixin.contains("menu.slotRole(slot)"));
        assertTrue(mixin.contains("case MAIN:case ENDER_STORAGE:return menu.targetMainStart()"));
        assertTrue(mixin.contains("case HOTBAR:return menu.targetHotbarStart()"));
        assertTrue(mixin.contains("PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP"));
    }

    @Test void enderIpnButtonsShareTheDefaultHorizontalAnchor()throws IOException{
        String hints=read("ddstweak/mixin/integration/inventoryprofilesnext/UnifiedPlayerEnderChestScreenIpnHintsMixin.java");
        assertTrue(hints.contains("IPNButton.MOVE_TO_CONTAINER,bottom=8"));
        assertFalse(hints.contains("horizontalOffset=24"));
    }

    @Test void enderToolbarOnlyExposesSneakAndDisconnect()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        int start=screen.indexOf("private void initEnder()"),end=screen.indexOf("private EditBox createIntervalBox",start);
        assertTrue(start>=0&&end>start);
        String ender=screen.substring(start,end);
        assertTrue(screen.contains("ENDER_SNEAK_X=7,ENDER_DISCONNECT_X=29"));
        assertTrue(ender.contains("PlayerInventoryAction.TOGGLE_SNEAK"));
        assertTrue(ender.contains("ENDER_DISCONNECT_X"));
        assertTrue(ender.contains("PlayerInventoryAction.DISCONNECT"));
        assertFalse(ender.contains("PlayerInventoryAction.ENDER_DROP_ALL"));
        assertFalse(ender.contains("PlayerInventoryAction.ENDER_STORE_ALL"));
        assertFalse(ender.contains("PlayerInventoryAction.ENDER_TAKE_ALL"));
    }

    @Test void continuousButtonsRestoreRightClickOnceAndStableIntervalEditing()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        String action=read("ddstweak/integration/dds/playerinventory/PlayerInventoryAction.java");
        assertTrue(action.contains("ATTACK_ONCE"));
        assertTrue(action.contains("USE_ONCE"));
        assertTrue(screen.contains("pressActionButton(e.x(),e.y(),button)"));
        assertTrue(screen.contains("ICON_ATTACK_CONTINUOUS?PlayerInventoryAction.ATTACK_ONCE"));
        assertTrue(screen.contains("ICON_USE_CONTINUOUS?PlayerInventoryAction.USE_ONCE"));
        assertTrue(screen.contains("lastAttackInterval=v"));
        assertTrue(screen.contains("lastUseInterval=v"));
        assertTrue(screen.contains("(long)readInterval(box)+delta"));
        assertTrue(screen.contains("ONCE_FEEDBACK_NANOS=220_000_000L"));
        assertTrue(screen.contains("onceFeedback(b)"));
        assertTrue(screen.contains("attackOnceFeedbackUntil=until"));
        assertTrue(screen.contains("useOnceFeedbackUntil=until"));
        assertTrue(screen.contains("menu.predictAttackOnce()"));
        assertTrue(screen.contains("menu.predictUseOnce()"));
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("predictAttackOnce(){if(supports(PlayerInventoryAction.ATTACK_ONCE)"));
        assertTrue(menu.contains("predictUseOnce(){if(supports(PlayerInventoryAction.USE_ONCE)"));
    }

    @Test void continuousTooltipShowsPrimaryCommandThenOnceCommand()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        assertTrue(screen.contains("lines.add(button.tooltip())"));
        assertTrue(screen.contains("PlayerInventoryAction.ATTACK_CONTINUOUS:PlayerInventoryAction.USE_CONTINUOUS"));
        assertTrue(screen.contains("dds-tweak.fakeplayer.action.once_hint"));
        assertTrue(screen.contains("dds-tweak.fakeplayer.action.attack_once"));
        assertTrue(screen.contains("dds-tweak.fakeplayer.action.use_once"));
        assertTrue(screen.contains("appendCommandTooltip(lines,once,0)"));
        assertTrue(screen.contains("MinecraftCompat.grayLiteral(\"/\"+command)"));
        assertFalse(screen.contains("dds-tweak.fakeplayer.action.once_replaces"));
    }

    @Test void unsupportedActionsUseTwoTooltipLinesWithGrayStatus()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        String compat=read("ddstweak/platform/minecraft/MinecraftCompat.java");
        assertTrue(screen.contains("Arrays.asList(b.tooltip(),MinecraftCompat.grayTranslatable(\"dds-tweak.fakeplayer.action.unavailable\"))"));
        assertFalse(screen.contains("tr(\"dds-tweak.fakeplayer.action.unavailable\",b.tooltip())"));
        assertTrue(compat.contains("grayTranslatable"));
        assertTrue(compat.contains("withStyle(ChatFormatting.GRAY)"));
    }

    private static String read(String path)throws IOException{return Files.readString(JAVA.resolve(path));}
}

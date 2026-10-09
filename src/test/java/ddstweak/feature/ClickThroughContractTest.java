package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class ClickThroughContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();

    private static String read(String p) throws Exception {
        Path f=ROOT.resolve(p);
        return Files.exists(f)?Files.readString(f):"";
    }

    @Test void itemFramesExposeSmartAndAlwaysModes() throws Exception {
        String resolver=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughTargetResolver.java");
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        String mode=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughMode.java");
        assertTrue(resolver.contains("clickThroughMode()==ClickThroughMode.ALWAYS"));
        assertTrue(resolver.contains("ClickThroughSupportPolicy.canClickThrough(level,player,redirected)"));
        assertTrue(configs.contains("CLICK_THROUGH_MODE"));
        assertTrue(screen.contains("c.add(Configs.Tweaks.CLICK_THROUGH_MODE)"));
        assertTrue(configs.contains("ClickThroughMode.SMART"));
        assertTrue(mode.contains("SMART(\"smart\")"));
        assertTrue(mode.contains("ALWAYS(\"always\")"));
    }

    @Test void smartPolicyCoversPredictableVanillaInteractions() throws Exception {
        String policy=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughSupportPolicy.java");
        assertTrue(policy.contains("state.getMenuProvider(level,pos)!=null"));
        assertTrue(policy.contains("isHandOpenableDoor"));
        assertTrue(policy.contains("isHandOpenableTrapDoor"));
        assertTrue(policy.contains("type==FenceGateBlock.class||type==BedBlock.class"));
        assertTrue(policy.contains("type==EnderChestBlock.class"));
        assertTrue(policy.contains("type==LecternBlock.class"));
        assertTrue(policy.contains("type==CakeBlock.class"));
        assertTrue(policy.contains("type==CandleCakeBlock.class"));
        assertTrue(policy.contains("canUseJukebox"));
        assertTrue(policy.contains("canUseComposter"));
        assertTrue(policy.contains("canUseCampfire"));
        assertTrue(policy.contains("canRingBell"));
        assertTrue(policy.contains("canUseBeehive"));
        assertTrue(policy.contains("canUseRespawnAnchor"));
        assertTrue(policy.contains("canUseShelf"));
        assertTrue(policy.contains("type==RepeaterBlock.class||type==ComparatorBlock.class"));
        assertTrue(policy.contains("player.canUseGameMasterBlocks()"));
    }

    @Test void uncertainInteractionsAreNotGuessedInSmartMode() throws Exception {
        String policy=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughSupportPolicy.java");
        assertFalse(policy.contains("CauldronInteraction"));
        assertFalse(policy.contains("VaultBlock"));
        assertFalse(policy.contains("CopperGolemStatueBlock"));
        assertTrue(policy.contains("intentionally left to ALWAYS mode"));
    }

    @Test void versionSpecificApisStayInsideCompatLayer() throws Exception {
        String policy=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughSupportPolicy.java");
        String signPolicy=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughSignPolicy.java");
        String compat=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughCompat.java");
        assertFalse(policy.contains("RecipePropertySet"));
        assertFalse(policy.contains("DataComponents.COMPOSTABLE"));
        assertFalse(signPolicy.contains("SignTextSlot"));
        assertFalse(signPolicy.contains("SignApplicator"));
        assertTrue(compat.contains("RecipePropertySet.CAMPFIRE_INPUT"));
        assertTrue(compat.contains("stack.has(DataComponents.COMPOSTABLE)"));
        assertTrue(compat.contains("ItemTags.DOUSES_CAMPFIRES"));
        assertTrue(compat.contains("SignTextSlot"));
        assertTrue(compat.contains("//#if MC >= 260100"));
        assertTrue(compat.contains("stack.get(DataComponents.DYE)"));
        assertTrue(compat.contains("//#if MC >= 260300"));
        assertTrue(policy.contains("//#if MC >= 12004"));
        assertTrue(policy.contains("//#if MC >= 12111"));
        assertFalse(policy.contains("//$ "));
        assertFalse(compat.contains("//$ "));
    }

    @Test void wallSignsKeepOnlyRealForegroundActionsAndCurrentHand() throws Exception {
        String resolver=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughTargetResolver.java");
        String signPolicy=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughSignPolicy.java");
        String compat=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughCompat.java");

        assertTrue(resolver.contains("ClickThroughSignPolicy.shouldKeepTarget(level,player,pos,player.getMainHandItem())"));
        assertTrue(signPolicy.contains("ItemStack stack"));
        assertTrue(signPolicy.contains("ClickThroughCompat.hasSignClickCommand"));
        assertTrue(signPolicy.contains("ClickThroughCompat.canApplyToSign"));
        assertTrue(compat.contains("stack.getItem()==Items.GLOW_INK_SAC"));
        assertTrue(compat.contains("stack.getItem()==Items.INK_SAC"));
        assertTrue(compat.contains("stack.getItem() instanceof DyeItem"));
        assertTrue(compat.contains("text.getColor()!=dyeColor"));
        assertTrue(compat.contains("sign.isWaxed()"));
        assertFalse(resolver.contains("Items.HONEYCOMB"));
        assertFalse(resolver.contains("Items.GLOW_INK_SAC"));
        assertFalse(resolver.contains("Items.INK_SAC"));
    }

    @Test void blockHitRoutingOccursBeforeVanillaInteraction() throws Exception {
        String resolver=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughTargetResolver.java");
        String mixin=read("src/main/java/ddstweak/mixin/feature/clickthrough/MinecraftUseItemMixin.java");
        assertTrue(mixin.contains("value=\"FIELD\""));
        assertFalse(mixin.contains("MultiPlayerGameMode;useItemOn"));
        assertTrue(resolver.contains("HitResult.Type.BLOCK"));
        assertTrue(resolver.contains("ClickThroughSignPolicy.shouldKeepTarget"));
        assertTrue(resolver.contains("pos.relative(side.getOpposite())"));
        assertFalse(mixin.contains("RELEASE_SHIFT_KEY"));
        assertFalse(mixin.contains("PRESS_SHIFT_KEY"));
    }

    @Test void existingMarkerAndSneakSemanticsRemainRegistered() throws Exception {
        String resolver=read("src/main/java/ddstweak/feature/clickthrough/ClickThroughTargetResolver.java");
        String mixin=read("src/main/java/ddstweak/mixin/feature/clickthrough/MinecraftUseItemMixin.java");
        String json=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(resolver.contains("player.isShiftKeyDown()"));
        assertTrue(resolver.contains("block instanceof WallSignBlock"));
        assertTrue(resolver.contains("block instanceof WallBannerBlock"));
        assertTrue(mixin.contains("ClickThroughTargetResolver.resolve"));
        assertTrue(json.contains("feature.clickthrough.MinecraftUseItemMixin"));
    }
}

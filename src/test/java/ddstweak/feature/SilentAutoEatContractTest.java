package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class SilentAutoEatContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();

    private static String read(String path) throws Exception {
        Path file=ROOT.resolve(path);
        return Files.exists(file)?Files.readString(file):"";
    }

    @Test void configIsRegisteredAndVisible() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        assertTrue(configs.contains("AUTO_EAT=MalilibCompat.bool(\"autoEat\",false"));
        assertTrue(configs.contains("AUTO_EAT_HUNGER_THRESHOLD"));
        assertTrue(configs.contains("AUTO_EAT_FOOD_BLACKLIST"));
        assertTrue(configs.contains("\"minecraft:pufferfish\""));
        assertTrue(configs.contains("\"minecraft:golden_apple\""));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT)"));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT_HUNGER_THRESHOLD)"));
        assertTrue(screen.contains("c.add(Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST)"));
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String pickerMixin=read("src/main/java/ddstweak/mixin/feature/dropconfirmation/DropConfirmStringListButtonMixin.java");
        assertTrue(picker.contains("Kind.FOOD"));
        assertTrue(picker.contains("SilentAutoEatFoodRules.isEdible(item)"));
        assertTrue(pickerMixin.contains("\"autoEatFoodBlacklist\""));
        assertTrue(pickerMixin.contains("DropConfirmationItemScreen.Kind.FOOD"));
    }

    @Test void initializationAndMixinsAreRegistered() throws Exception {
        String init=read("src/main/java/ddstweak/DDSTweakInitHandler.java");
        String mixins=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(init.contains("SilentAutoEat.initialize()"));
        assertTrue(mixins.contains("feature.silentautoeat.MinecraftInputMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.ContainerScreenInputMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.ContainerScreenRenderMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.ItemInHandRendererMixin"));
        assertTrue(mixins.contains("feature.silentautoeat.GuiMixin"));
    }

    @Test void stateMachineUsesVanillaInventoryAndUsePaths() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEat.java");
        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        String compat=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatCompat.java");
        assertTrue(feature.contains("owner=minecraft.player;"));
        assertTrue(feature.contains("ownerLevel=minecraft.level;"));
        assertTrue(feature.indexOf("owner=minecraft.player;")<feature.indexOf("state=State.PREPARE;"));
        assertTrue(feature.contains("SWAP_TO_OFFHAND"));
        assertTrue(feature.contains("START_USE"));
        assertTrue(feature.contains("USING"));
        assertTrue(feature.contains("RESTORE_OFFHAND"));
        assertTrue(feature.contains("swapWithOffhand"));
        assertTrue(compat.contains("ContainerInput.SWAP"));
        assertTrue(compat.contains("ClickType.SWAP"));
        assertTrue(compat.contains("handleContainerInput"));
        assertTrue(compat.contains("handleInventoryMouseClick"));
        assertTrue(compat.contains("AbstractContainerScreen"));
        assertTrue(compat.contains("SlotContainerIndexAccessor"));
        assertTrue(compat.contains("menuSlot(LocalPlayer player,int inventoryIndex)"));
        assertTrue(compat.contains("inventoryIndex(LocalPlayer player,Slot slot)"));
        assertFalse(feature.contains("visualContainerSlot"));
        assertTrue(presentation.contains("projectContainerStack"));
        assertTrue(presentation.contains("ItemStack upstream"));
        String containerRender=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ContainerScreenRenderMixin.java");
        assertTrue(containerRender.contains("method={\"renderSlot\",\"extractSlot\"}"));
        assertTrue(containerRender.contains("@ModifyVariable("));
        assertTrue(containerRender.contains("at=@At(value=\"STORE\",ordinal=0)"));
        assertTrue(containerRender.contains("method={\"renderTooltip\",\"extractTooltip\"}"));
        assertFalse(containerRender.contains("method={\"renderSlot\",\"renderTooltip\",\"extractSlot\",\"extractTooltip\"}"));
        assertTrue(feature.contains("START_CLIENT_TICK"));
        assertTrue(feature.contains("keyUse.isDown()"));
        assertTrue(feature.contains("keyAttack.isDown()"));
        assertTrue(feature.contains("keySwapOffhand.isDown()"));
        assertTrue(feature.contains("SilentAutoEatCompat.isSafeAutomationContext"));
        assertTrue(feature.contains("transactionMenu"));
        assertTrue(feature.contains("sourceMenuSlot"));
        assertTrue(feature.contains("completionObserved"));
        assertTrue(feature.contains("!completionObserved&&!offhandStillOwnedByAutoEat()"));
        assertTrue(feature.contains("releaseUsingItem"));
        assertTrue(compat.contains("gameMode.useItem"));
        assertFalse(feature.contains("setFoodLevel"));
        assertFalse(feature.contains("setSaturation"));
        assertFalse(feature.contains("ServerboundSetCreativeModeSlotPacket"));
    }

    @Test void renderLayerHidesOnlyLocalPresentation() throws Exception {
        String input=read("src/main/java/ddstweak/mixin/feature/silentautoeat/MinecraftInputMixin.java");
        String hand=read("src/main/java/ddstweak/mixin/feature/silentautoeat/ItemInHandRendererMixin.java");
        String gui=read("src/main/java/ddstweak/mixin/feature/silentautoeat/GuiMixin.java");
        String presentation=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatPresentation.java");
        assertTrue(input.contains("method=\"handleKeybinds\""));
        assertTrue(input.contains("SilentAutoEat.ownsUse"));
        assertTrue(hand.contains("SilentAutoEatPresentation.projectOffhand"));
        assertTrue(hand.contains("SilentAutoEat.isPresentationActive"));
        assertTrue(hand.contains("renderArmWithItem"));
        assertTrue(hand.contains("FirstPersonHandsAndItems"));
        assertTrue(hand.contains("FirstPersonHandsAndItemsRenderState"));
        assertTrue(hand.contains("extractRenderState"));
        assertTrue(hand.contains("state.useItemRemainingTicks=0"));
        assertTrue(hand.contains("//#if MC >= 260300"));
        assertTrue(gui.contains("renderHotbar"));
        assertTrue(gui.contains("renderItemHotbar"));
        assertTrue(gui.contains("extractItemHotbar"));
        assertTrue(gui.contains("MC >= 260200"));
        assertTrue(gui.contains("SilentAutoEatPresentation.projectOffhand"));
        assertTrue(gui.contains("SilentAutoEatPresentation.projectInventoryStack"));
        assertTrue(presentation.contains("projectOffhand"));
        assertTrue(presentation.contains("projectInventoryStack"));
        assertTrue(presentation.contains("projectContainerStack"));
    }

    @Test void versionDifferencesStayInCompat() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/silentautoeat/SilentAutoEatCompat.java");
        assertTrue(compat.contains("//#if MC >= 12105"));
        assertTrue(compat.contains("//#if MC >= 12102"));
        assertTrue(compat.contains("DataComponents.CONSUMABLE"));
        assertTrue(compat.contains("//#elseif MC >= 12005"));
        assertTrue(compat.contains("//#if MC >= 11903"));
        assertTrue(compat.contains("//#if MC >= 11900"));
        assertTrue(compat.contains("//#if MC >= 260100"));
        assertFalse(compat.contains("//$ "));
    }
}

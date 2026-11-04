package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryMainScreen;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.spongepowered.asm.mixin.Mixin;

// The fake-inventory layout uses the same IPN anchor across all supported versions.
// UNKNOWN target identity does not change the visual layout or these annotations.
@IPNGuiHint(button=IPNButton.MOVE_TO_PLAYER,top=44)
@IPNGuiHint(button=IPNButton.SORT,top=44)
@IPNGuiHint(button=IPNButton.SORT_COLUMNS,top=44)
@IPNGuiHint(button=IPNButton.SORT_ROWS,top=44)
@Mixin(UnifiedPlayerInventoryMainScreen.class)
public abstract class UnifiedPlayerInventoryScreenIpnHintsMixin {}

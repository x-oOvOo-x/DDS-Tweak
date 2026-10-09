package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryRealScreen;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.spongepowered.asm.mixin.Mixin;

@IPNGuiHint(button=IPNButton.MOVE_TO_PLAYER,top=66)
@IPNGuiHint(button=IPNButton.SORT,top=66)
@IPNGuiHint(button=IPNButton.SORT_COLUMNS,top=66)
@IPNGuiHint(button=IPNButton.SORT_ROWS,top=66)
@Mixin(UnifiedPlayerInventoryRealScreen.class)
public abstract class UnifiedPlayerInventoryRealScreenIpnHintsMixin {}

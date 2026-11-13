package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerEnderChestScreen;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Positions IPN controls on the unified Ender Chest screen.
 * 为统一末影箱界面指定 IPN 按钮位置。
 */
@IPNGuiHint(button=IPNButton.MOVE_TO_PLAYER)
@IPNGuiHint(button=IPNButton.SORT)
@IPNGuiHint(button=IPNButton.SORT_COLUMNS)
@IPNGuiHint(button=IPNButton.SORT_ROWS)
@IPNGuiHint(button=IPNButton.MOVE_TO_CONTAINER,bottom=8)
@Mixin(UnifiedPlayerEnderChestScreen.class)
public abstract class UnifiedPlayerEnderChestScreenIpnHintsMixin {}

package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.fakeplayer.screen.DdsFakePlayerEnderChestScreen;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Positions IPN controls on the DDS fake-player Ender Chest screen.
 * 为 DDS 假人末影箱界面指定 IPN 按钮位置。
 */
@IPNGuiHint(button=IPNButton.MOVE_TO_PLAYER,horizontalOffset=24)
@IPNGuiHint(button=IPNButton.SORT,horizontalOffset=24)
@IPNGuiHint(button=IPNButton.SORT_COLUMNS,horizontalOffset=24)
@IPNGuiHint(button=IPNButton.SORT_ROWS,horizontalOffset=24)
@IPNGuiHint(button=IPNButton.MOVE_TO_CONTAINER,bottom=8)
@Mixin(DdsFakePlayerEnderChestScreen.class)
public abstract class DdsFakePlayerEnderChestScreenIpnHintsMixin {}

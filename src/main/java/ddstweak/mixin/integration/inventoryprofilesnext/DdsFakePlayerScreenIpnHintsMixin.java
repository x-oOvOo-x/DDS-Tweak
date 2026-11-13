package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.fakeplayer.screen.DdsFakePlayerScreen;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Positions IPN controls on the DDS fake-player inventory screen.
 * 为 DDS 假人背包界面指定 IPN 按钮位置。
 */
@IPNGuiHint(button=IPNButton.MOVE_TO_PLAYER,top=44)
@IPNGuiHint(button=IPNButton.SORT,top=44)
@IPNGuiHint(button=IPNButton.SORT_COLUMNS,top=44)
@IPNGuiHint(button=IPNButton.SORT_ROWS,top=44)
@Mixin(DdsFakePlayerScreen.class)
public abstract class DdsFakePlayerScreenIpnHintsMixin {}

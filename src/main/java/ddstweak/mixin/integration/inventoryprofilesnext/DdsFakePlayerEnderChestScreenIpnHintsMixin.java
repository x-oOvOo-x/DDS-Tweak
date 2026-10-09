package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.fakeplayer.screen.DdsFakePlayerEnderChestScreen;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.spongepowered.asm.mixin.Mixin;

@IPNGuiHint(button=IPNButton.MOVE_TO_PLAYER,horizontalOffset=24)
@IPNGuiHint(button=IPNButton.SORT,horizontalOffset=24)
@IPNGuiHint(button=IPNButton.SORT_COLUMNS,horizontalOffset=24)
@IPNGuiHint(button=IPNButton.SORT_ROWS,horizontalOffset=24)
@IPNGuiHint(button=IPNButton.MOVE_TO_CONTAINER,bottom=8)
@Mixin(DdsFakePlayerEnderChestScreen.class)
public abstract class DdsFakePlayerEnderChestScreenIpnHintsMixin {}

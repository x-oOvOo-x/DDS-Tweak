package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.fakeplayer.screen.DdsFakePlayerScreen;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.spongepowered.asm.mixin.Mixin;

@IPNGuiHint(button=IPNButton.MOVE_TO_PLAYER,top=44)
@IPNGuiHint(button=IPNButton.SORT,top=44)
@IPNGuiHint(button=IPNButton.SORT_COLUMNS,top=44)
@IPNGuiHint(button=IPNButton.SORT_ROWS,top=44)
@Mixin(DdsFakePlayerScreen.class)
public abstract class DdsFakePlayerScreenIpnHintsMixin {}

package ddstweak.mixin.feature.locatorheads;

//#if MC >= 12108
import ddstweak.feature.locatorheads.LocatorHeadsFeature;
import ddstweak.feature.locatorheads.LocatorHudTrace;
import net.minecraft.client.Minecraft;
//#if MC >= 260200
//$$ import net.minecraft.client.gui.Hud;
//#else
import net.minecraft.client.gui.Gui;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keep the XP bar while the separate locator markers are overlaid by LocatorExperienceMixin.
 * 定位标记单独叠加时保留经验条显示优先级。
 */
//#if MC >= 260200
//$$ @Mixin(Hud.class)
//#else
@Mixin(Gui.class)
//#endif
public abstract class LocatorPriorityMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method="willPrioritizeExperienceInfo",at=@At("RETURN"),cancellable=true,require=0)
    private void ddstweak$alwaysExperience(CallbackInfoReturnable<Boolean> cir){
        boolean force=LocatorHeadsFeature.alwaysXp()&&minecraft.level!=null&&minecraft.player!=null&&
                minecraft.getCameraEntity()!=null&&!minecraft.player.isPassenger();
        LocatorHudTrace.priority(force);
        if(force)cir.setReturnValue(true);
    }
}
//#else
//$$ public abstract class LocatorPriorityMixin {}
//#endif

package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Passive wheel intent, before vanilla updates the selected hotbar slot.
 * 原版切换快捷栏前记录玩家滚轮意图。
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerScrollMixin {
    @Inject(method="onScroll(JDD)V",at=@At("HEAD"))
    private void ddstweak$rememberAutoEatWheel(long window,double horizontal,double vertical,CallbackInfo ci){
        SilentAutoEat.onManualHotbarScroll(Minecraft.getInstance(),vertical);
    }
}

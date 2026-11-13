package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Older container screens inherit mouseScrolled instead of declaring it.
 * Listen at the native scroll entrypoint, then cancel only for a container GUI.
 * 捕获旧版容器滚轮输入，不影响其他界面。
 */
@Mixin(MouseHandler.class)
public abstract class LegacyContainerScrollMixin {
    //#if MC < 12102
    @Inject(method="onScroll",at=@At("HEAD"))
    private void ddstweak$silentAutoEatLegacyScroll(long window,double horizontal,double vertical,CallbackInfo ci){
        Minecraft minecraft=Minecraft.getInstance();
        // The GLFW callback need not run on the render/game thread.
        minecraft.execute(()->{
            if(MinecraftCompat.screen(minecraft) instanceof AbstractContainerScreen){
                SilentAutoEat.onExplicitPlayerAction(minecraft);
            }
        });
    }
    //#endif
}

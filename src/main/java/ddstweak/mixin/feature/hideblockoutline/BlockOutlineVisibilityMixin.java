package ddstweak.mixin.feature.hideblockoutline;

import ddstweak.config.Configs;
import ddstweak.feature.helpfulhitboxes.HelpfulHitboxesFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class BlockOutlineVisibilityMixin {
    //#if MC >= 260200
    //$$ @Inject(method="submitBlockOutline",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$hideBlockOutline(CallbackInfo ci){if(shouldHide())ci.cancel();}
    //#elseif MC >= 12102
    @Inject(method="renderBlockOutline",at=@At("HEAD"),cancellable=true)
    private void ddstweak$hideBlockOutline(CallbackInfo ci){if(shouldHide())ci.cancel();}
    //#else
    //$$ @Inject(method="renderHitOutline",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$hideBlockOutline(CallbackInfo ci){if(shouldHide())ci.cancel();}
    //#endif

    private static boolean shouldHide(){
        Minecraft minecraft=Minecraft.getInstance();
        return Configs.Visual.HIDE_BLOCK_OUTLINE.getBooleanValue()
                &&minecraft.player!=null
                &&!minecraft.player.isShiftKeyDown()
                &&!HelpfulHitboxesFeature.shouldRevealOutline();
    }
}

package ddstweak.mixin.feature.locatorheads;

//#if MC >= 12108
import ddstweak.feature.locatorheads.LocatorHeadsFeature;
import ddstweak.feature.locatorheads.LocatorHudTrace;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
//#if MC >= 260200
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//$$ import net.minecraft.client.gui.contextualbar.ExperienceBar;
//$$ import net.minecraft.client.gui.contextualbar.LocatorBar;
//#elseif MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//$$ import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
//$$ import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
//#else
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Render locator markers, not their background, when XP is preferred. */
//#if MC >= 260200
//$$ @Mixin(ExperienceBar.class)
//#else
@Mixin(ExperienceBarRenderer.class)
//#endif
public abstract class LocatorExperienceMixin {
    @Shadow @Final private Minecraft minecraft;
    //#if MC >= 260100
    //$$ @Inject(method="extractBackground",at=@At("HEAD"),require=1)
    //$$ private void ddstweak$countBackground(GuiGraphicsExtractor graphics,DeltaTracker ticks,CallbackInfo ci){
    //#else
    @Inject(method="renderBackground",at=@At("HEAD"),require=1)
    private void ddstweak$countBackground(GuiGraphics graphics,DeltaTracker ticks,CallbackInfo ci){
    //#endif
        LocatorHudTrace.background(true);
    }

    //#if MC >= 260100
    //$$ @Inject(method="extractRenderState",at=@At("RETURN"),require=0)
    //$$ private void ddstweak$overlay(GuiGraphicsExtractor graphics,DeltaTracker ticks,CallbackInfo ci){
    //#else
    @Inject(method="render",at=@At("RETURN"),require=0)
    private void ddstweak$overlay(GuiGraphics graphics,DeltaTracker ticks,CallbackInfo ci){
    //#endif
        boolean overlay=LocatorHeadsFeature.alwaysXp()&&minecraft.level!=null&&minecraft.getCameraEntity()!=null;
        LocatorHudTrace.xpReturn(overlay);
        if(!overlay)return;
        //#if MC >= 260200
        //$$ new LocatorBar(minecraft).extractRenderState(graphics,ticks);
        //#else
        //#if MC >= 260100
        //$$ new LocatorBarRenderer(minecraft).extractRenderState(graphics,ticks);
        //#else
        new LocatorBarRenderer(minecraft).render(graphics,ticks);
        //#endif
        //#endif
    }
}
//#else
//$$ public abstract class LocatorExperienceMixin {}
//#endif

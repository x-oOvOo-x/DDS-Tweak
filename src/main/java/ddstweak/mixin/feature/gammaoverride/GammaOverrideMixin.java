package ddstweak.mixin.feature.gammaoverride;

import ddstweak.feature.gammaoverride.GammaOverrideFeature;
//#if MC >= 11904
import net.minecraft.client.OptionInstance;
//#else
//$$ import net.minecraft.client.Options;
//#endif
//#if MC >= 260102
//$$ import net.minecraft.client.renderer.LightmapRenderStateExtractor;
//#else
import net.minecraft.client.renderer.LightTexture;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC >= 260102
//$$ @Mixin(LightmapRenderStateExtractor.class)
//$$ public abstract class GammaOverrideMixin {
//$$     @Redirect(method="extract",at=@At(value="INVOKE",target="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",ordinal=1))
//$$     private Object ddstweak$runtimeGamma(OptionInstance<?> option){return GammaOverrideFeature.enabled()?GammaOverrideFeature.value():option.get();}
//$$ }
//#else
@Mixin(LightTexture.class)
public abstract class GammaOverrideMixin {
    //#if MC >= 12109
    @Redirect(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",ordinal=2))
    private Object ddstweak$runtimeGamma(OptionInstance<?> option){return GammaOverrideFeature.enabled()?GammaOverrideFeature.value():option.get();}
    //#elseif MC >= 11904
    //$$ @Redirect(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",ordinal=1))
    //$$ private Object ddstweak$runtimeGamma(OptionInstance<?> option){return GammaOverrideFeature.enabled()?GammaOverrideFeature.value():option.get();}
    //#else
    //$$ @Redirect(method="updateLightTexture",at=@At(value="FIELD",target="Lnet/minecraft/client/Options;gamma:D"))
    //$$ private double ddstweak$runtimeGamma(Options options){return GammaOverrideFeature.resolve(options.gamma);}
    //#endif
}
//#endif

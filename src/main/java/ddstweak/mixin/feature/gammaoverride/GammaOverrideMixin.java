package ddstweak.mixin.feature.gammaoverride;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//#if MC >= 11904
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
//#endif
import ddstweak.feature.gammaoverride.GammaOverrideFeature;
//#if MC >= 260102
//$$ import net.minecraft.client.renderer.LightmapRenderStateExtractor;
//#else
import net.minecraft.client.renderer.LightTexture;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC >= 260102
//$$ // Overrides rendered gamma without rewriting the saved options value.
//$$ // 只覆盖渲染亮度，不修改保存的设置值。
//$$ @Mixin(LightmapRenderStateExtractor.class)
//$$ public abstract class GammaOverrideMixin {
//$$     @Definition(id="gamma",method="Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;")
//$$     @Definition(id="get",method="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;")
//$$     @Expression("?.gamma().get()")
//$$     @ModifyExpressionValue(method="extract",at=@At("MIXINEXTRAS:EXPRESSION"),allow=1)
//$$     private Object ddstweak$runtimeGamma(Object original){return GammaOverrideFeature.resolve(original);}
//$$ }
//#else
@Mixin(LightTexture.class)
public abstract class GammaOverrideMixin {
    //#if MC >= 11904
    @Definition(id="gamma",method="Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;")
    @Definition(id="get",method="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;")
    @Expression("?.gamma().get()")
    @ModifyExpressionValue(method="updateLightTexture",at=@At("MIXINEXTRAS:EXPRESSION"),allow=1)
    private Object ddstweak$runtimeGamma(Object original){return GammaOverrideFeature.resolve(original);}
    //#else
    //$$ @ModifyExpressionValue(method="updateLightTexture",at=@At(value="FIELD",target="Lnet/minecraft/client/Options;gamma:D"),allow=1)
    //$$ private double ddstweak$runtimeGamma(double original){return GammaOverrideFeature.resolve(original);}
    //#endif
}
//#endif

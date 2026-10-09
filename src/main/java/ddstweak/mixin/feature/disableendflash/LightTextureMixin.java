package ddstweak.mixin.feature.disableendflash;

//#if MC >= 12109
import ddstweak.feature.disableendflash.DisableEndFlashFeature;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.EndFlashState;
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
//$$ public abstract class LightTextureMixin {
//$$     @Redirect(method="extract",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;endFlashState()Lnet/minecraft/client/renderer/EndFlashState;"),require=0)
//$$     private EndFlashState ddstweak$ignoreEndFlashLightEffect(ClientLevel level){return DisableEndFlashFeature.enabled()?null:level.endFlashState();}
//$$ }
//#else
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Redirect(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;endFlashState()Lnet/minecraft/client/renderer/EndFlashState;"))
    private EndFlashState ddstweak$ignoreEndFlashLightEffect(ClientLevel level){return DisableEndFlashFeature.enabled()?null:level.endFlashState();}
}
//#endif
//#else
//$$ public abstract class LightTextureMixin {}
//#endif

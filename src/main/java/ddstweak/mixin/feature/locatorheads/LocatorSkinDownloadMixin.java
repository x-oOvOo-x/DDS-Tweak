package ddstweak.mixin.feature.locatorheads;

//#if MC >= 12108
import com.mojang.blaze3d.platform.NativeImage;
import ddstweak.feature.locatorheads.LocatorContourCache;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
//#if MC >= 12109
import net.minecraft.core.ClientAsset;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the same processed NativeImage the vanilla skin downloader registers. */
@Mixin(SkinTextureDownloader.class)
public abstract class LocatorSkinDownloadMixin {
    //#if MC >= 12109
    @Inject(method="registerTextureInManager",at=@At("HEAD"),require=0)
    private void ddstweak$captureAlpha(ClientAsset.Texture texture,NativeImage image,
            CallbackInfoReturnable<CompletableFuture<ClientAsset.Texture>> cir){
        LocatorContourCache.accept(texture.texturePath(),image);
    }
    //#else
    //$$ @Inject(method="registerTextureInManager",at=@At("HEAD"),require=0)
    //$$ private static void ddstweak$captureAlpha(ResourceLocation texture,NativeImage image,
    //$$         CallbackInfoReturnable<CompletableFuture<ResourceLocation>> cir){
    //$$     LocatorContourCache.accept(texture,image);
    //$$ }
    //#endif
}
//#else
//$$ public abstract class LocatorSkinDownloadMixin {}
//#endif

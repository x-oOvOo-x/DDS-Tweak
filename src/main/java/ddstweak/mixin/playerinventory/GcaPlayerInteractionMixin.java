package ddstweak.mixin.playerinventory;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
//#if MC >= 12111
import ddstweak.integration.gca.playerinventory.GcaPlayerInteractionIntent;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

/** Captures the actual right-clicked player, without relying on its display name. */
@Mixin(Minecraft.class)
public abstract class GcaPlayerInteractionMixin {
    //#if MC >= 12111
    @Inject(method="startUseItem",at=@At("HEAD"))
    private void ddstweak$rememberGcaTarget(CallbackInfo ci){
        GcaPlayerInteractionIntent.observeUse((Minecraft)(Object)this);
    }
    //#endif
}

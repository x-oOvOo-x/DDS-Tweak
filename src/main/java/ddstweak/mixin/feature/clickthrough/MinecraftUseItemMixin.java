package ddstweak.mixin.feature.clickthrough;

import ddstweak.feature.clickthrough.ClickThroughTargetResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftUseItemMixin {
    @Redirect(
            method="startUseItem",
            at=@At(
                    value="FIELD",
                    target="Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;",
                    opcode=Opcodes.GETFIELD
            )
    )
    private HitResult ddstweak$clickThroughTarget(Minecraft minecraft){
        return ClickThroughTargetResolver.resolve(minecraft.hitResult,minecraft.player,minecraft.level);
    }
}

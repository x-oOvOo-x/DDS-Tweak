package ddstweak.mixin.feature.helpfulhitboxes;

import ddstweak.feature.helpfulhitboxes.HelpfulHitboxesFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
//#if MC >= 11600
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow public abstract Block getBlock();

    @Inject(method="getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",at=@At("RETURN"),cancellable=true)
    private void ddstweak$helpfulHitboxes(BlockGetter world,BlockPos pos,CollisionContext context,CallbackInfoReturnable<VoxelShape> cir){
        VoxelShape original=cir.getReturnValue();
        VoxelShape adjusted=HelpfulHitboxesFeature.adjust(getBlock(),context,original);
        if(adjusted!=original)cir.setReturnValue(adjusted);
    }
}
//#else
//$$ public final class BlockStateBaseMixin {private BlockStateBaseMixin(){}}
//#endif

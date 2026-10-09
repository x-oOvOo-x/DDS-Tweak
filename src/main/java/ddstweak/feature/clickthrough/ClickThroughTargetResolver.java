package ddstweak.feature.clickthrough;

import ddstweak.config.Configs;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ClickThroughTargetResolver {
    private ClickThroughTargetResolver(){}

    public static HitResult resolve(HitResult original,LocalPlayer player,ClientLevel level){
        if(!enabled(original,player,level)||player.isShiftKeyDown())return original;

        if(original.getType()==HitResult.Type.ENTITY&&original instanceof EntityHitResult){
            EntityHitResult entityHit=(EntityHitResult)original;
            if(entityHit.getEntity() instanceof ItemFrame){
                ItemFrame frame=(ItemFrame)entityHit.getEntity();
                Direction side=frame.getDirection();
                //#if MC >= 11600
                BlockPos support=frame.blockPosition().relative(side.getOpposite());
                //#else
                //$$ BlockPos support=frame.getCommandSenderBlockPosition().relative(side.getOpposite());
                //#endif
                BlockHitResult redirected=blockHit(entityHit.getLocation(),support,side);
                if(clickThroughMode()==ClickThroughMode.ALWAYS
                        ||ClickThroughSupportPolicy.canClickThrough(level,player,redirected))return redirected;
            }
        }

        if(original.getType()!=HitResult.Type.BLOCK||!(original instanceof BlockHitResult))return original;

        BlockHitResult hit=(BlockHitResult)original;
        BlockPos pos=hit.getBlockPos();
        BlockState state=level.getBlockState(pos);
        Block block=state.getBlock();
        Direction side=null;

        if(block instanceof WallSignBlock){
            if(ClickThroughSignPolicy.shouldKeepTarget(level,player,pos,player.getMainHandItem()))return original;
            side=state.getValue(WallSignBlock.FACING);
        }else if(block instanceof WallBannerBlock){
            side=state.getValue(WallBannerBlock.FACING);
        }

        return side==null?original:blockHit(hit.getLocation(),pos.relative(side.getOpposite()),side);
    }

    private static boolean enabled(HitResult original,LocalPlayer player,ClientLevel level){
        return Configs.Tweaks.CLICK_THROUGH.getBooleanValue()&&original!=null&&player!=null&&level!=null;
    }

    private static ClickThroughMode clickThroughMode(){
        IConfigOptionListEntry value=Configs.Tweaks.CLICK_THROUGH_MODE.getOptionListValue();
        return value instanceof ClickThroughMode?(ClickThroughMode)value:ClickThroughMode.SMART;
    }

    private static BlockHitResult blockHit(Vec3 location,BlockPos pos,Direction side){
        double x=location.x,y=location.y,z=location.z;
        if(side==Direction.EAST)x=pos.getX()+1.0D;
        else if(side==Direction.WEST)x=pos.getX();
        else if(side==Direction.UP)y=pos.getY()+1.0D;
        else if(side==Direction.DOWN)y=pos.getY();
        else if(side==Direction.SOUTH)z=pos.getZ()+1.0D;
        else if(side==Direction.NORTH)z=pos.getZ();
        return new BlockHitResult(new Vec3(x,y,z),side,pos,false);
    }
}

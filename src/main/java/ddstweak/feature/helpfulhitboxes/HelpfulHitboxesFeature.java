package ddstweak.feature.helpfulhitboxes;

import ddstweak.config.Configs;
import net.minecraft.client.Minecraft;
//#if MC >= 11900
import net.minecraft.world.level.block.BambooStalkBlock;
//#else
//$$ import net.minecraft.world.level.block.BambooBlock;
//#endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
//#if MC >= 11700
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
//#endif
//#if MC >= 11600
import net.minecraft.world.level.block.ChainBlock;
//#endif
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HelpfulHitboxesFeature {
    private static final double EXPAND=2.0D/16.0D;

    public static VoxelShape adjust(Block block,CollisionContext context,VoxelShape original){
        if(!Configs.Tweaks.HELPFUL_HITBOXES.getBooleanValue()||block==null||context==null||original==null||isSneaking()||!isHoldingMatchingItem(block,context))return original;
        if(block instanceof FenceBlock||block instanceof IronBarsBlock)return Shapes.block();
        if(isExpanded(block))return expand(original);
        return original;
    }

    public static boolean shouldRevealOutline(){
        Minecraft minecraft=Minecraft.getInstance();
        if(!Configs.Tweaks.HELPFUL_HITBOXES.getBooleanValue()
                ||minecraft.player==null||minecraft.level==null||minecraft.hitResult==null
                ||minecraft.hitResult.getType()!=HitResult.Type.BLOCK)return false;
        Block block=minecraft.level.getBlockState(((BlockHitResult)minecraft.hitResult).getBlockPos()).getBlock();
        return isHelpfulBlock(block)&&isHoldingMatchingItem(block);
    }

    private static boolean isHoldingMatchingItem(Block block,CollisionContext context){
        return context.isHoldingItem(block.asItem())||isOffhandHolding(block);
    }

    private static boolean isHoldingMatchingItem(Block block){
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player==null)return false;
        return minecraft.player.getMainHandItem().getItem()==block.asItem()||isOffhandHolding(block);
    }

    private static boolean isOffhandHolding(Block block){
        Minecraft minecraft=Minecraft.getInstance();
        return minecraft.player!=null&&minecraft.player.getOffhandItem().getItem()==block.asItem();
    }

    private static boolean isSneaking(){
        Minecraft minecraft=Minecraft.getInstance();
        return minecraft.player!=null&&minecraft.player.isShiftKeyDown();
    }

    private static boolean isHelpfulBlock(Block block){
        return block instanceof FenceBlock||block instanceof IronBarsBlock||isExpanded(block);
    }

    private static boolean isExpanded(Block block){
        //#if MC >= 11900
        if(block instanceof EndRodBlock||block instanceof BambooStalkBlock)return true;
        //#else
        //$$ if(block instanceof EndRodBlock||block instanceof BambooBlock)return true;
        //#endif
        //#if MC >= 11700
        if(block instanceof LightningRodBlock||block instanceof PointedDripstoneBlock)return true;
        //#endif
        //#if MC >= 11600
        return block instanceof ChainBlock;
        //#else
        //$$ return false;
        //#endif
    }

    private static VoxelShape expand(VoxelShape shape){
        if(shape.isEmpty())return shape;
        AABB b=shape.bounds();
        return Shapes.box(
                Math.max(0.0D,b.minX-EXPAND),Math.max(0.0D,b.minY-EXPAND),Math.max(0.0D,b.minZ-EXPAND),
                Math.min(1.0D,b.maxX+EXPAND),Math.min(1.0D,b.maxY+EXPAND),Math.min(1.0D,b.maxZ+EXPAND)
        );
    }

    private HelpfulHitboxesFeature(){}
}

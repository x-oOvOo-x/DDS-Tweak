package ddstweak.feature.clickthrough;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Resolves hit geometry when interaction passes through foreground blocks.
 * 计算穿过前景方块后的有效射线命中位置。
 */
final class ClickThroughGeometry {
    private static final double FACE_EPSILON=1.0E-4D;

    private ClickThroughGeometry(){}

    /** A hit on a frame's edge is not a click through its front face. */
    static boolean isFrontFace(AABB bounds,Vec3 hit,Direction facing){
        switch(facing){
            case EAST: return near(hit.x,bounds.maxX);
            case WEST: return near(hit.x,bounds.minX);
            case UP: return near(hit.y,bounds.maxY);
            case DOWN: return near(hit.y,bounds.minY);
            case SOUTH: return near(hit.z,bounds.maxZ);
            case NORTH: return near(hit.z,bounds.minZ);
            default: return false;
        }
    }

    static BlockHitResult supportHit(ClientLevel level,LocalPlayer player,Vec3 foreground,
                                     BlockPos support,Direction facing){
        VoxelShape shape=level.getBlockState(support).getShape(level,support);
        return traceSupport(shape,player.getEyePosition(1.0F),foreground,support,facing);
    }

    /** Only a real outline-shape intersection behind the foreground, on the attached face, counts. */
    static BlockHitResult traceSupport(VoxelShape shape,Vec3 eye,Vec3 foreground,
                                       BlockPos support,Direction facing){
        Vec3 ray=foreground.subtract(eye);
        if(ray.lengthSqr()<1.0E-8D)return null;
        Vec3 end=foreground.add(ray.normalize().scale(2.0D));
        BlockHitResult hit=shape.clip(eye,end,support);
        if(hit==null||hit.getDirection()!=facing
                ||hit.getLocation().distanceToSqr(eye)<=foreground.distanceToSqr(eye))return null;
        return hit;
    }

    private static boolean near(double a,double b){
        return Math.abs(a-b)<=FACE_EPSILON;
    }
}

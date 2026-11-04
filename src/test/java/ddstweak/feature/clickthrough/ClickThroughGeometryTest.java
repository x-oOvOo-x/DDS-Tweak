package ddstweak.feature.clickthrough;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ClickThroughGeometryTest {
    private static final BlockPos SUPPORT=new BlockPos(0,0,0);

    @Test void frontFaceAndEdgeHitsAreDifferent(){
        AABB frame=new AABB(0.125,0.125,1.0,0.875,0.875,1.0625);
        assertTrue(ClickThroughGeometry.isFrontFace(frame,new Vec3(0.5,0.5,1.0625),Direction.SOUTH));
        assertFalse(ClickThroughGeometry.isFrontFace(frame,new Vec3(0.125,0.5,1.03),Direction.SOUTH));
        assertFalse(ClickThroughGeometry.isFrontFace(frame,new Vec3(0.5,0.125,1.03),Direction.SOUTH));
        assertFalse(ClickThroughGeometry.isFrontFace(frame,new Vec3(0.5,0.5,1.0),Direction.SOUTH));
        assertTrue(ClickThroughGeometry.isFrontFace(frame,new Vec3(0.875,0.5,1.03),Direction.EAST));
        assertTrue(ClickThroughGeometry.isFrontFace(frame,new Vec3(0.5,0.875,1.03),Direction.UP));
    }

    @Test void backingRayMustIntersectRealOutlineFromTheAttachedFace(){
        VoxelShape chest=Shapes.box(0.0625,0.0,0.0625,0.9375,0.875,0.9375);
        Vec3 eye=new Vec3(0.5,0.5,2.5);
        BlockHitResult hit=ClickThroughGeometry.traceSupport(
                chest,eye,new Vec3(0.5,0.5,1.1),SUPPORT,Direction.SOUTH);
        assertNotNull(hit);
        assertEquals(Direction.SOUTH,hit.getDirection());
        assertEquals(SUPPORT,hit.getBlockPos());
        assertEquals(0.9375,hit.getLocation().z,1.0E-6D);

        assertNull(ClickThroughGeometry.traceSupport(chest,
                new Vec3(0.98,0.5,2.5),new Vec3(0.98,0.5,1.1),SUPPORT,Direction.SOUTH));
        assertNull(ClickThroughGeometry.traceSupport(chest,
                new Vec3(0.5,0.95,2.5),new Vec3(0.5,0.95,1.1),SUPPORT,Direction.SOUTH));
        assertNull(ClickThroughGeometry.traceSupport(chest,
                eye,new Vec3(0.5,0.5,1.1),SUPPORT,Direction.NORTH));
        assertNull(ClickThroughGeometry.traceSupport(Shapes.empty(),
                eye,new Vec3(0.5,0.5,1.1),SUPPORT,Direction.SOUTH));
    }

    @Test void supportHitMustBeBehindForegroundNotAheadOfIt(){
        VoxelShape cube=Shapes.box(0,0,0,1,1,1);
        Vec3 eye=new Vec3(0.5,0.5,2.5);
        assertNull(ClickThroughGeometry.traceSupport(
                cube,eye,new Vec3(0.5,0.5,0.9),SUPPORT,Direction.SOUTH));
        assertNull(ClickThroughGeometry.traceSupport(
                cube,eye,eye,SUPPORT,Direction.SOUTH));
    }
}

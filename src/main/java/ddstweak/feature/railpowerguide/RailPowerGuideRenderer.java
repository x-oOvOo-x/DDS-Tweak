package ddstweak.feature.railpowerguide;

import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
//#if MC >= 12105
import fi.dy.masa.malilib.util.data.Color4f;
//#else
//$$ import fi.dy.masa.malilib.util.Color4f;
//#endif
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//#if MC >= 11903
import org.joml.Matrix4f;
//#elseif MC >= 11700
//$$ import com.mojang.math.Matrix4f;
//#endif
//#if 11500 <= MC && MC < 12005
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
//#if MC >= 260100
//$$ import com.mojang.blaze3d.pipeline.RenderTarget;
//$$ import net.minecraft.client.renderer.RenderBuffers;
//$$ import net.minecraft.client.renderer.culling.Frustum;
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//$$ import net.minecraft.util.profiling.ProfilerFiller;
//$$ import org.joml.Vector4f;
//#if MC >= 260300
//$$ import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
//#else
//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice;
//$$ import org.joml.Matrix4fc;
//#endif
//#endif

/** Renders all nearby placement markers with a stronger outline and a faint depth-aware fill. */
public final class RailPowerGuideRenderer implements IRenderer {
    public static final RailPowerGuideRenderer INSTANCE=new RailPowerGuideRenderer();
    private static final Color4f OUTLINE=new Color4f(1.0F,0.78F,0.08F,0.90F);
    private static final Color4f FILL=new Color4f(1.0F,0.75F,0.10F,0.14F);
    private static final Color4f LEGACY_FACE=new Color4f(1.0F,0.75F,0.10F,0.24F);

    private RailPowerGuideRenderer(){}

    //#if MC >= 260300
    //$$ @Override public void onRenderWorldLast(RenderTarget fb,CameraRenderState cameraState,Frustum culling,
    //$$         RenderBuffers buffers,GpuBufferSlice terrainFog,Vector4f fogColor,ProfilerFiller profiler){render();}
    //#elseif MC >= 260100
    //$$ @Override public void onRenderWorldLast(RenderTarget fb,Matrix4fc modelViewMatrix,CameraRenderState cameraState,Frustum culling,
    //$$         RenderBuffers buffers,GpuBufferSlice terrainFog,Vector4f fogColor,ProfilerFiller profiler){render();}
    //#elseif MC >= 12105
    @Override public void onRenderWorldLast(Matrix4f posMatrix,Matrix4f projMatrix){render(posMatrix);}
    //#elseif MC >= 12101
    //$$ @Override public void onRenderWorldLast(Matrix4f posMatrix,Matrix4f projMatrix){render(posMatrix);}
    //#elseif MC >= 12005
    //$$ @Override public void onRenderWorldLast(Matrix4f posMatrix,Matrix4f projMatrix){render(posMatrix);}
    //#elseif MC >= 11700
    //$$ @Override public void onRenderWorldLast(PoseStack matrixStack,Matrix4f projMatrix){render(matrixStack);}
    //#elseif MC >= 11500
    //$$ @Override public void onRenderWorldLast(float partialTicks,PoseStack matrixStack){render(matrixStack);}
    //#else
    //$$ @Override public void onRenderWorldLast(float partialTicks){render();}
    //#endif

    //#if MC >= 260100
    //$$ private static void render(){
    //$$     for(BlockPos target:RailPowerPlacementGuide.getTargets()){
    //$$         RenderUtils.renderAreaSides(target,target,FILL,false);
    //$$         RenderUtils.renderBlockOutline(target,0.003F,4.0F,OUTLINE,false);
    //$$     }
    //$$ }
    //#elseif MC >= 12105
    private static void render(Matrix4f matrix){
        for(BlockPos target:RailPowerPlacementGuide.getTargets()){
            RenderUtils.renderAreaSides(target,target,FILL,matrix,false);
            RenderUtils.renderBlockOutline(target,0.003F,4.0F,OUTLINE,false);
        }
    }
    //#elseif MC >= 12101
    //$$ private static void render(Matrix4f matrix){
    //$$     Minecraft mc=Minecraft.getInstance();
    //$$     for(BlockPos target:RailPowerPlacementGuide.getTargets()){
    //$$         RenderUtils.renderAreaSides(target,target,FILL,matrix,mc);
    //$$         RenderUtils.renderBlockOutline(target,0.003F,4.0F,OUTLINE,mc);
    //$$     }
    //$$ }
    //#elseif MC >= 12005
    //$$ private static void render(Matrix4f matrix){
    //$$     Minecraft mc=Minecraft.getInstance();
    //$$     if(mc.player==null)return;
    //$$     for(BlockPos target:RailPowerPlacementGuide.getTargets())
    //$$         RenderUtils.renderBlockTargetingOverlaySimple(mc.player,target,Direction.UP,LEGACY_FACE,matrix,mc);
    //$$ }
    //#elseif MC >= 11700
    //$$ private static void render(PoseStack matrix){
    //$$     Minecraft mc=Minecraft.getInstance();
    //$$     if(mc.player==null)return;
    //$$     for(BlockPos target:RailPowerPlacementGuide.getTargets())
    //$$         RenderUtils.renderBlockTargetingOverlaySimple(mc.player,target,Direction.UP,LEGACY_FACE,matrix,mc);
    //$$ }
    //#elseif MC >= 11500
    //$$ private static void render(PoseStack matrix){
    //$$     Minecraft mc=Minecraft.getInstance();
    //$$     if(mc.player==null)return;
    //$$     for(BlockPos target:RailPowerPlacementGuide.getTargets())
    //$$         RenderUtils.renderBlockTargetingOverlaySimple(mc.player,target,Direction.UP,LEGACY_FACE,matrix,mc);
    //$$ }
    //#else
    //$$ private static void render(){
    //$$     Minecraft mc=Minecraft.getInstance();
    //$$     if(mc.player==null)return;
    //$$     for(BlockPos target:RailPowerPlacementGuide.getTargets())
    //$$         RenderUtils.renderBlockTargetingOverlaySimple(mc.player,target,Direction.UP,LEGACY_FACE,mc);
    //$$ }
    //#endif
}

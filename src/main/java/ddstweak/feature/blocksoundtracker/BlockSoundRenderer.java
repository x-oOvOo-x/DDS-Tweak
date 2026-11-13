package ddstweak.feature.blocksoundtracker;

//#if MC >= 12111
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Identical through-wall purple outlines and translucent faces for every sound location.
 * 在已记录的声音位置绘制轮廓提示。
 */
public final class BlockSoundRenderer implements IRenderer {
    public static final BlockSoundRenderer INSTANCE=new BlockSoundRenderer();
    private static final Logger LOGGER=LoggerFactory.getLogger(BlockSoundRenderer.class);
    private static final Color4f OUTLINE=new Color4f(0.659F,0.333F,0.969F,0.90F);
    private static final Color4f FILL=new Color4f(0.659F,0.333F,0.969F,0.14F);
    private static boolean reportedRenderError;

    private BlockSoundRenderer(){}

    //#if MC >= 260300
    //$$ @Override public void onRenderWorldLast(RenderTarget fb,CameraRenderState cameraState,Frustum culling,
    //$$         RenderBuffers buffers,GpuBufferSlice terrainFog,Vector4f fogColor,ProfilerFiller profiler){render();}
    //#elseif MC >= 260100
    //$$ @Override public void onRenderWorldLast(RenderTarget fb,Matrix4fc modelViewMatrix,CameraRenderState cameraState,Frustum culling,
    //$$         RenderBuffers buffers,GpuBufferSlice terrainFog,Vector4f fogColor,ProfilerFiller profiler){render();}
    //#else
    @Override public void onRenderWorldLast(Matrix4f posMatrix,Matrix4f projMatrix){render();}
    //#endif

    private static void render(){
        for(BlockSoundTracker.Marker marker:BlockSoundTracker.visible()){
            BlockPos pos=marker.pos();
            renderThroughFill(pos);
            RenderUtils.renderBlockOutline(pos,0.003F,4.0F,OUTLINE,true);
        }
    }

    /**
     * RenderUtils.renderAreaSides() always uses a depth-tested pipeline; its last boolean
     * controls translucent sorting, NOT through-wall rendering. Select a no-depth
     * translucent pipeline explicitly instead, keeping the MaLiLib cube geometry.
     */
    private static void renderThroughFill(BlockPos pos){
        //#if MC >= 260200
        //$$ try(RenderContext ctx=new RenderContext(()->"ddstweak:blockSoundFill",
        //$$         MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL,0)){
        //#else
        try(RenderContext ctx=new RenderContext(()->"ddstweak:blockSoundFill",
                MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL)){
        //#endif
            RenderUtils.renderAreaSidesBatched(pos,pos,FILL,0.002,ctx.getBuilder());
            ctx.draw(false);
        }catch(Exception e){
            if(!reportedRenderError){
                reportedRenderError=true;
                LOGGER.error("Could not render through-wall block sound fill",e);
            }
        }
    }
}
//#endif

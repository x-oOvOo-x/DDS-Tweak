package ddstweak.mixin.feature.chunkcenterindicator;

import ddstweak.feature.chunkcenterindicator.ChunkCenterIndicatorFeature;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 12111
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#if MC >= 11904
//$$ import com.mojang.blaze3d.vertex.VertexConsumer;
//$$ import net.minecraft.client.renderer.RenderType;
//$$ import org.joml.Matrix4f;
//#else
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import com.mojang.blaze3d.vertex.BufferBuilder;
//$$ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//$$ import com.mojang.blaze3d.vertex.Tesselator;
//#if MC >= 11700
//$$ import com.mojang.blaze3d.vertex.VertexFormat;
//#endif
//#endif
//#else
//#if MC >= 11500
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//#else
//$$ import com.mojang.blaze3d.platform.GlStateManager;
//#endif
//$$ import com.mojang.blaze3d.vertex.BufferBuilder;
//$$ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//$$ import com.mojang.blaze3d.vertex.Tesselator;
//#endif

@Mixin(ChunkBorderRenderer.class)
public abstract class ChunkBorderRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    //#if MC >= 12111
    @Inject(method="emitGizmos",at=@At("TAIL"))
    private void ddstweak$chunkCenter(double camX,double camY,double camZ,DebugValueAccess debugValues,Frustum frustum,float partialTicks,CallbackInfo ci){
        if(!ChunkCenterIndicatorFeature.enabled())return;
        Entity entity;
        //#if MC >= 260100
        //$$ entity=minecraft.getCameraEntity();
        //#else
        entity=minecraft.gameRenderer.getMainCamera().entity();
        //#endif
        if(entity==null)return;
        double x=ChunkCenterIndicatorFeature.chunkCenter(entity.getX());
        double y=ChunkCenterIndicatorFeature.markerY(camY);
        double z=ChunkCenterIndicatorFeature.chunkCenter(entity.getZ());
        double a=ChunkCenterIndicatorFeature.ARM,v=ChunkCenterIndicatorFeature.VERTICAL;
        Gizmos.line(new Vec3(x-a,y,z),new Vec3(x+a,y,z),ChunkCenterIndicatorFeature.COLOR_ARGB,3.0F);
        Gizmos.line(new Vec3(x,y,z-a),new Vec3(x,y,z+a),ChunkCenterIndicatorFeature.COLOR_ARGB,3.0F);
        Gizmos.line(new Vec3(x,y-v,z),new Vec3(x,y+v,z),ChunkCenterIndicatorFeature.COLOR_ARGB,3.0F);
    }
    //#elseif MC >= 11600
    //$$ @Inject(method="render",at=@At("TAIL"))
    //$$ private void ddstweak$chunkCenter(PoseStack pose,MultiBufferSource buffers,double camX,double camY,double camZ,CallbackInfo ci){
    //$$     if(!ChunkCenterIndicatorFeature.enabled())return;
    //$$     Entity entity=minecraft.gameRenderer.getMainCamera().getEntity();
    //$$     if(entity==null)return;
    //$$     double x=ChunkCenterIndicatorFeature.chunkCenter(entity.getX())-camX;
    //$$     double y=ChunkCenterIndicatorFeature.markerY(camY)-camY;
    //$$     double z=ChunkCenterIndicatorFeature.chunkCenter(entity.getZ())-camZ;
    //$$     drawLegacy(pose,buffers,x,y,z);
    //$$ }
    //#else
    //$$ @Inject(method="render",at=@At("TAIL"))
    //$$ private void ddstweak$chunkCenter(long finishTimeNano,CallbackInfo ci){
    //$$     if(!ChunkCenterIndicatorFeature.enabled())return;
    //$$     Camera camera=minecraft.gameRenderer.getMainCamera();
    //$$     Entity entity=camera.getEntity();
    //$$     if(entity==null)return;
    //$$     Vec3 cameraPos=camera.getPosition();
    //$$     double x=ChunkCenterIndicatorFeature.chunkCenter(cameraPos.x)-cameraPos.x;
    //$$     double y=ChunkCenterIndicatorFeature.markerY(cameraPos.y)-cameraPos.y;
    //$$     double z=ChunkCenterIndicatorFeature.chunkCenter(cameraPos.z)-cameraPos.z;
    //$$     drawLegacy(x,y,z);
    //$$ }
    //#endif

    //#if MC >= 11904 && MC < 12111
    //$$ private static void drawLegacy(PoseStack pose,MultiBufferSource buffers,double x,double y,double z){
    //$$     double a=ChunkCenterIndicatorFeature.ARM,v=ChunkCenterIndicatorFeature.VERTICAL;
    //$$     VertexConsumer out=buffers.getBuffer(RenderType.debugLineStrip(3.0D));
    //$$     Matrix4f matrix=pose.last().pose();
    //$$     vertex(out,matrix,x-a,y,z);vertex(out,matrix,x+a,y,z);vertex(out,matrix,x,y,z);
    //$$     vertex(out,matrix,x,y,z-a);vertex(out,matrix,x,y,z+a);vertex(out,matrix,x,y,z);
    //$$     vertex(out,matrix,x,y-v,z);vertex(out,matrix,x,y+v,z);
    //$$ }
    //$$ private static void vertex(VertexConsumer out,Matrix4f matrix,double x,double y,double z){
    //#if MC >= 12101
    //$$     out.addVertex(matrix,(float)x,(float)y,(float)z).setColor(ChunkCenterIndicatorFeature.COLOR_ARGB);
    //#else
    //$$     out.vertex(matrix,(float)x,(float)y,(float)z).color(ChunkCenterIndicatorFeature.COLOR_R,ChunkCenterIndicatorFeature.COLOR_G,ChunkCenterIndicatorFeature.COLOR_B,ChunkCenterIndicatorFeature.COLOR_A).endVertex();
    //#endif
    //$$ }
    //#elseif MC >= 11600 && MC < 11904
    //$$ private static void drawLegacy(PoseStack pose,MultiBufferSource buffers,double x,double y,double z){
    //$$     RenderSystem.disableTexture();RenderSystem.disableBlend();RenderSystem.lineWidth(3.0F);
    //$$     Tesselator t=Tesselator.getInstance();BufferBuilder b=t.getBuilder();
    //#if MC >= 11700
    //$$     b.begin(VertexFormat.Mode.DEBUG_LINE_STRIP,DefaultVertexFormat.POSITION_COLOR);
    //#else
    //$$     b.begin(3,DefaultVertexFormat.POSITION_COLOR);
    //#endif
    //$$     legacyVertices(b,x,y,z);t.end();
    //$$     RenderSystem.lineWidth(1.0F);RenderSystem.enableBlend();RenderSystem.enableTexture();
    //$$ }
    //$$ private static void legacyVertices(BufferBuilder b,double x,double y,double z){
    //$$     double a=ChunkCenterIndicatorFeature.ARM,v=ChunkCenterIndicatorFeature.VERTICAL;
    //$$     legacyVertex(b,x-a,y,z);legacyVertex(b,x+a,y,z);legacyVertex(b,x,y,z);
    //$$     legacyVertex(b,x,y,z-a);legacyVertex(b,x,y,z+a);legacyVertex(b,x,y,z);
    //$$     legacyVertex(b,x,y-v,z);legacyVertex(b,x,y+v,z);
    //$$ }
    //$$ private static void legacyVertex(BufferBuilder b,double x,double y,double z){b.vertex(x,y,z).color(ChunkCenterIndicatorFeature.COLOR_R,ChunkCenterIndicatorFeature.COLOR_G,ChunkCenterIndicatorFeature.COLOR_B,ChunkCenterIndicatorFeature.COLOR_A).endVertex();}
    //#elseif MC < 11600
    //$$ private static void drawLegacy(double x,double y,double z){
    //#if MC >= 11500
    //$$     RenderSystem.disableTexture();RenderSystem.disableBlend();RenderSystem.lineWidth(3.0F);
    //#else
    //$$     GlStateManager.disableTexture();GlStateManager.disableBlend();GlStateManager.lineWidth(3.0F);
    //#endif
    //$$     Tesselator t=Tesselator.getInstance();BufferBuilder b=t.getBuilder();b.begin(3,DefaultVertexFormat.POSITION_COLOR);
    //$$     double a=ChunkCenterIndicatorFeature.ARM,v=ChunkCenterIndicatorFeature.VERTICAL;
    //$$     legacyVertex(b,x-a,y,z);legacyVertex(b,x+a,y,z);legacyVertex(b,x,y,z);
    //$$     legacyVertex(b,x,y,z-a);legacyVertex(b,x,y,z+a);legacyVertex(b,x,y,z);
    //$$     legacyVertex(b,x,y-v,z);legacyVertex(b,x,y+v,z);t.end();
    //#if MC >= 11500
    //$$     RenderSystem.lineWidth(1.0F);RenderSystem.enableBlend();RenderSystem.enableTexture();
    //#else
    //$$     GlStateManager.lineWidth(1.0F);GlStateManager.enableBlend();GlStateManager.enableTexture();
    //#endif
    //$$ }
    //$$ private static void legacyVertex(BufferBuilder b,double x,double y,double z){b.vertex(x,y,z).color(ChunkCenterIndicatorFeature.COLOR_R,ChunkCenterIndicatorFeature.COLOR_G,ChunkCenterIndicatorFeature.COLOR_B,ChunkCenterIndicatorFeature.COLOR_A).endVertex();}
    //#endif
}

package ddstweak.mixin.feature.stackcountscale;

import ddstweak.feature.stackcountscale.StackCountScaleFeature;
import net.minecraft.client.gui.Font;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import net.minecraft.client.renderer.entity.ItemRenderer;
//#if MC >= 11500
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#if MC >= 11900
//$$ import org.joml.Matrix4f;
//#else
//$$ import com.mojang.math.Matrix4f;
//#endif
//#else
//$$ import com.mojang.blaze3d.platform.GlStateManager;
//#endif
//#endif
import net.minecraft.world.item.ItemStack;
//#if MC >= 12106
import org.joml.Matrix3x2fStack;
//#elseif MC >= 12000
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 260100
//$$ @Mixin(GuiGraphicsExtractor.class)
//#elseif MC >= 12000
@Mixin(GuiGraphics.class)
//#else
//$$ @Mixin(ItemRenderer.class)
//#endif
public abstract class GuiGraphicsMixin {
    //#if MC >= 260100
    //$$ @Inject(method="itemCount",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$scaleStackCount(Font font,ItemStack itemStack,int x,int y,String countText,CallbackInfo ci){
    //$$     if(!StackCountScaleFeature.enabled()||(itemStack.getCount()==1&&countText==null))return;
    //$$     float scale=StackCountScaleFeature.scale();if(scale==1.0F)return;
    //$$     String amount=countText==null?String.valueOf(itemStack.getCount()):countText;
    //$$     GuiGraphicsExtractor g=(GuiGraphicsExtractor)(Object)this;
    //$$     Matrix3x2fStack pose=g.pose();pose.pushMatrix();pose.translate(x+17,y+18);pose.scale(scale,scale);g.text(font,amount,-font.width(amount),-font.lineHeight,-1,true);pose.popMatrix();
    //$$     ci.cancel();
    //$$ }
    //#elseif MC >= 12102
    @Inject(method="renderItemCount",at=@At("HEAD"),cancellable=true)
    private void ddstweak$scaleStackCount(Font font,ItemStack itemStack,int x,int y,String countText,CallbackInfo ci){
        if(!StackCountScaleFeature.enabled()||(itemStack.getCount()==1&&countText==null))return;
        float scale=StackCountScaleFeature.scale();if(scale==1.0F)return;
        String amount=countText==null?String.valueOf(itemStack.getCount()):countText;
        GuiGraphics g=(GuiGraphics)(Object)this;
        //#if MC >= 12106
        Matrix3x2fStack pose=g.pose();pose.pushMatrix();pose.translate(x+17,y+18);pose.scale(scale,scale);g.drawString(font,amount,-font.width(amount),-font.lineHeight,-1,true);pose.popMatrix();
        //#else
        //$$ PoseStack pose=g.pose();pose.pushPose();pose.translate(x+17,y+18,0);pose.scale(scale,scale,1);g.drawString(font,amount,-font.width(amount),-font.lineHeight,-1,true);pose.popPose();
        //#endif
        ci.cancel();
    }
    //#elseif MC >= 12000
    //$$ @Redirect(method="renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"))
    //$$ private int ddstweak$scaleLegacyStackCount(GuiGraphics g,Font font,String amount,int x,int y,int color,boolean shadow){
    //$$     if(!StackCountScaleFeature.enabled())return g.drawString(font,amount,x,y,color,shadow);
    //$$     float scale=StackCountScaleFeature.scale();if(scale==1.0F)return g.drawString(font,amount,x,y,color,shadow);
    //$$     int width=font.width(amount);PoseStack pose=g.pose();pose.pushPose();pose.translate(x+width,y+font.lineHeight,0);pose.scale(scale,scale,1);
    //$$     int result=g.drawString(font,amount,-width,-font.lineHeight,color,shadow);pose.popPose();return result;
    //$$ }
    //#elseif MC >= 11900
    //$$ @Redirect(
    //$$     method="renderGuiItemDecorations(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
    //$$     at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I")
    //$$ )
    //$$ private int ddstweak$scalePoseStackCount(Font font,String amount,float x,float y,int color,boolean shadow,Matrix4f matrix,MultiBufferSource buffers,Font.DisplayMode mode,int background,int packedLight){
    //$$     if(!StackCountScaleFeature.enabled())return font.drawInBatch(amount,x,y,color,shadow,matrix,buffers,mode,background,packedLight);
    //$$     float scale=StackCountScaleFeature.scale();if(scale==1.0F)return font.drawInBatch(amount,x,y,color,shadow,matrix,buffers,mode,background,packedLight);
    //$$     int width=font.width(amount);
    //$$     Matrix4f scaled=new Matrix4f(matrix).translate(x+width,y+font.lineHeight,0).scale(scale,scale,1);
    //$$     return font.drawInBatch(amount,-width,-font.lineHeight,color,shadow,scaled,buffers,mode,background,packedLight);
    //$$ }
    //#elseif MC >= 11500
    //$$ @Redirect(
    //$$     method="renderGuiItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
    //$$     at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;drawInBatch(Ljava/lang/String;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I")
    //$$ )
    //$$ private int ddstweak$scaleLegacyMatrixCount(Font font,String amount,float x,float y,int color,boolean shadow,Matrix4f matrix,MultiBufferSource buffers,boolean seeThrough,int background,int packedLight){
    //$$     if(!StackCountScaleFeature.enabled())return font.drawInBatch(amount,x,y,color,shadow,matrix,buffers,seeThrough,background,packedLight);
    //$$     float scale=StackCountScaleFeature.scale();if(scale==1.0F)return font.drawInBatch(amount,x,y,color,shadow,matrix,buffers,seeThrough,background,packedLight);
    //$$     int width=font.width(amount);Matrix4f scaled=matrix.copy();
    //$$     scaled.multiply(Matrix4f.createTranslateMatrix(x+width,y+font.lineHeight,0));scaled.multiply(Matrix4f.createScaleMatrix(scale,scale,1));
    //$$     return font.drawInBatch(amount,-width,-font.lineHeight,color,shadow,scaled,buffers,seeThrough,background,packedLight);
    //$$ }
    //#else
    //$$ @Redirect(
    //$$     method="renderGuiItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
    //$$     at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;drawShadow(Ljava/lang/String;FFI)I")
    //$$ )
    //$$ private int ddstweak$scaleFixedFunctionCount(Font font,String amount,float x,float y,int color){
    //$$     if(!StackCountScaleFeature.enabled())return font.drawShadow(amount,x,y,color);
    //$$     float scale=StackCountScaleFeature.scale();if(scale==1.0F)return font.drawShadow(amount,x,y,color);
    //$$     int width=font.width(amount);
    //$$     GlStateManager.pushMatrix();
    //$$     GlStateManager.translatef(x+width,y+9.0F,0.0F);
    //$$     GlStateManager.scalef(scale,scale,1.0F);
    //$$     int result=font.drawShadow(amount,-width,-9.0F,color);
    //$$     GlStateManager.popMatrix();
    //$$     return result;
    //$$ }
    //#endif
}

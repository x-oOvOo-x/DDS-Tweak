package ddstweak.platform.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
//#if MC < 11600
//$$ import org.lwjgl.opengl.GL11;
//#endif
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.gui.GuiComponent;
//#elseif MC >= 11500
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import net.minecraft.client.gui.GuiComponent;
//#else
//$$ import com.mojang.blaze3d.platform.GlStateManager;
//$$ import net.minecraft.client.gui.GuiComponent;
//#endif
//#if MC >= 12106
import net.minecraft.client.renderer.RenderPipelines;
//#elseif MC >= 12102
//$$ import net.minecraft.client.renderer.RenderType;
//#endif
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.item.ItemStack;

/** Small rendering facade for extractor / GuiGraphics / PoseStack / pre-PoseStack boundaries. */
public final class ScreenRenderCompat {
    private ScreenRenderCompat(){}

    public static void fill(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x1,int y1,int x2,int y2,int color){
        //#if MC >= 260100
        //$$ graphics.fill(x1,y1,x2,y2,color);
        //#elseif MC >= 12000
        graphics.fill(x1,y1,x2,y2,color);
        //#elseif MC >= 11600
        //$$ GuiComponent.fill(graphics,x1,y1,x2,y2,color);
        //#else
        //$$ GuiComponent.fill(x1,y1,x2,y2,color);
        //#endif
    }

    public static void text(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            Font font,String text,int x,int y,int color,boolean shadow){
        //#if MC >= 260100
        //$$ graphics.text(font,text,x,y,color,shadow);
        //#elseif MC >= 12000
        graphics.drawString(font,text,x,y,color,shadow);
        //#elseif MC >= 11600
        //$$ if(shadow)font.drawShadow(graphics,text,(float)x,(float)y,color);else font.draw(graphics,text,(float)x,(float)y,color);
        //#else
        //$$ if(shadow)font.drawShadow(text,(float)x,(float)y,color);else font.draw(text,(float)x,(float)y,color);
        //#endif
    }

    public static void item(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            ItemStack stack,int x,int y){
        //#if MC >= 260100
        //$$ graphics.item(stack,x,y);
        //#elseif MC >= 12000
        graphics.renderItem(stack,x,y);
        //#elseif MC >= 11904
        //$$ Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(graphics,stack,x,y);
        //#else
        //$$ Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack,x,y);
        //#endif
    }

    public static void crossButton(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x,int y,boolean highlighted,boolean enabled){
        //#if MC >= 12002
        String path="textures/gui/sprites/widget/cross_button"+(highlighted?"_highlighted":"")+".png";
        //#if MC >= 12111
        Identifier texture=Identifier.withDefaultNamespace(path);
        //#elseif MC >= 12100
        //$$ ResourceLocation texture=ResourceLocation.withDefaultNamespace(path);
        //#else
        //$$ ResourceLocation texture=new ResourceLocation("minecraft",path);
        //#endif
        blit(graphics,texture,x,y,0,0,14,14,14,14);
        //#else
        //$$ int color=highlighted?0xFFFFFFFF:0xFFB0B0B0;
        //$$ fill(graphics,x+3,y+2,x+5,y+4,color);fill(graphics,x+9,y+2,x+11,y+4,color);
        //$$ fill(graphics,x+4,y+3,x+6,y+5,color);fill(graphics,x+8,y+3,x+10,y+5,color);
        //$$ fill(graphics,x+5,y+4,x+9,y+10,color);
        //$$ fill(graphics,x+4,y+9,x+6,y+11,color);fill(graphics,x+8,y+9,x+10,y+11,color);
        //$$ fill(graphics,x+3,y+10,x+5,y+12,color);fill(graphics,x+9,y+10,x+11,y+12,color);
        //#endif
    }

    public static void playerHead(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            PlayerInfo info,int x,int y,int size){
        if(info==null)return;
        //#if MC >= 12111
        Identifier texture=info.getSkin().body().texturePath();
        //#elseif MC >= 12109
        //$$ ResourceLocation texture=info.getSkin().body().texturePath();
        //#elseif MC >= 12002
        //$$ ResourceLocation texture=info.getSkin().texture();
        //#else
        //$$ ResourceLocation texture=info.getSkinLocation();
        //#endif
        if(texture==null)return;
        drawSkinLayer(graphics,texture,x,y,size,8,8);
        int outer=size+2;
        drawSkinLayer(graphics,texture,x-1,y-1,outer,40,8);
    }

    private static void drawSkinLayer(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            //#if MC >= 12111
            Identifier texture,
            //#else
            //$$ ResourceLocation texture,
            //#endif
            int x,int y,int size,int u,int v){
        float scale=size/8.0F;
        //#if MC >= 260100
        //$$ graphics.pose().pushMatrix();graphics.pose().translate((float)x,(float)y);graphics.pose().scale(scale,scale);
        //$$ blit(graphics,texture,0,0,u,v,8,8,64,64);graphics.pose().popMatrix();
        //#elseif MC >= 12106
        graphics.pose().pushMatrix();graphics.pose().translate((float)x,(float)y);graphics.pose().scale(scale,scale);
        blit(graphics,texture,0,0,u,v,8,8,64,64);graphics.pose().popMatrix();
        //#elseif MC >= 12000
        //$$ graphics.pose().pushPose();graphics.pose().translate(x,y,0);graphics.pose().scale(scale,scale,1.0F);
        //$$ blit(graphics,texture,0,0,u,v,8,8,64,64);graphics.pose().popPose();
        //#elseif MC >= 11600
        //$$ graphics.pushPose();graphics.translate(x,y,0);graphics.scale(scale,scale,1.0F);
        //$$ blit(graphics,texture,0,0,u,v,8,8,64,64);graphics.popPose();
        //#elseif MC >= 11500
        //$$ RenderSystem.pushMatrix();RenderSystem.translated((double)x,(double)y,0.0D);RenderSystem.scaled((double)scale,(double)scale,1.0D);
        //$$ blit(graphics,texture,0,0,u,v,8,8,64,64);RenderSystem.popMatrix();
        //#else
        //$$ GlStateManager.pushMatrix();GlStateManager.translatef((float)x,(float)y,0.0F);GlStateManager.scalef(scale,scale,1.0F);
        //$$ blit(graphics,texture,0,0,u,v,8,8,64,64);GlStateManager.popMatrix();
        //#endif
    }

    public static boolean beginScissor(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x1,int y1,int x2,int y2){
        //#if MC >= 260100
        //$$ graphics.enableScissor(x1,y1,x2,y2);return true;
        //#elseif MC >= 12000
        graphics.enableScissor(x1,y1,x2,y2);return true;
        //#elseif MC >= 11904
        //$$ GuiComponent.enableScissor(x1,y1,x2,y2);return true;
        //#else
        //$$ double scale=MinecraftCompat.guiScale(Minecraft.getInstance());
        //$$ int windowHeight=MinecraftCompat.windowHeight(Minecraft.getInstance());
        //$$ int sx=(int)(x1*scale),sy=(int)(windowHeight-y2*scale);
        //$$ int sw=Math.max(0,(int)((x2-x1)*scale)),sh=Math.max(0,(int)((y2-y1)*scale));
        //#if MC >= 11600
        //$$ RenderSystem.enableScissor(sx,sy,sw,sh);
        //#else
        //$$ GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(sx,sy,sw,sh);
        //#endif
        //$$ return true;
        //#endif
    }

    public static void endScissor(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics
            //#elseif MC >= 12000
            GuiGraphics graphics
            //#elseif MC >= 11600
            //$$ PoseStack graphics
            //#else
            //$$ Object graphics
            //#endif
    ){
        //#if MC >= 260100
        //$$ graphics.disableScissor();
        //#elseif MC >= 12000
        graphics.disableScissor();
        //#elseif MC >= 11904
        //$$ GuiComponent.disableScissor();
        //#elseif MC >= 11600
        //$$ RenderSystem.disableScissor();
        //#else
        //$$ GL11.glDisable(GL11.GL_SCISSOR_TEST);
        //#endif
    }

    public static void blit(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            //#if MC >= 12111
            Identifier texture,
            //#else
            //$$ ResourceLocation texture,
            //#endif
            int x,int y,int u,int v,int width,int height,int textureWidth,int textureHeight){
        //#if MC >= 12106
        graphics.blit(RenderPipelines.GUI_TEXTURED,texture,x,y,(float)u,(float)v,width,height,textureWidth,textureHeight);
        //#elseif MC >= 12102
        //$$ graphics.blit(RenderType::guiTextured,texture,x,y,(float)u,(float)v,width,height,textureWidth,textureHeight);
        //#elseif MC >= 12000
        //$$ graphics.blit(texture,x,y,(float)u,(float)v,width,height,textureWidth,textureHeight);
        //#elseif MC >= 11700
        //$$ RenderSystem.setShaderTexture(0,texture);
        //$$ GuiComponent.blit(graphics,x,y,(float)u,(float)v,width,height,textureWidth,textureHeight);
        //#elseif MC >= 11600
        //$$ Minecraft.getInstance().getTextureManager().bind(texture);
        //$$ GuiComponent.blit(graphics,x,y,(float)u,(float)v,width,height,textureWidth,textureHeight);
        //#else
        //$$ Minecraft.getInstance().getTextureManager().bind(texture);
        //$$ GuiComponent.blit(x,y,(float)u,(float)v,width,height,textureWidth,textureHeight);
        //#endif
    }
}

package ddstweak.mixin.feature.compactstatuseffects;

//#if MC >= 12102
import ddstweak.feature.compactstatuseffects.CompactStatusEffectBounds;
import ddstweak.feature.compactstatuseffects.CompactStatusEffectFormat;
import ddstweak.feature.compactstatuseffects.CompactStatusEffectLayout;
import ddstweak.feature.compactstatuseffects.CompactStatusEffectsFeature;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 260200
//$$ import net.minecraft.client.gui.Hud;
//#else
import net.minecraft.client.gui.Gui;
//#endif
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
//#if MC >= 12106
import net.minecraft.client.renderer.RenderPipelines;
//#else
//$$ import net.minecraft.client.renderer.RenderType;
//$$ import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//#endif
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
//#if MC >= 260300
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#else
import org.lwjgl.glfw.GLFW;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value=EffectsInInventory.class,priority=1100)
public abstract class EffectsInInventoryMixin {
    @Shadow @Final private AbstractContainerScreen<?> screen;
    @Shadow @Final private Minecraft minecraft;

    @Unique private boolean ddstweak$expanded;
    @Unique private int ddstweak$effectCount;
    @Unique private CompactStatusEffectBounds ddstweak$area;
    @Unique private boolean ddstweak$translated;
    @Unique private int ddstweak$hoveredCompact=-1;

    //#if MC >= 260100
    //$$ @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true,require=0)
    //#elseif MC >= 12111
    @Inject(method="render",at=@At("HEAD"),cancellable=true,require=0)
    //#else
    //$$ @Inject(method="renderEffects",at=@At("HEAD"),cancellable=true,require=0)
    //#endif
    private void ddstweak$renderCompact(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            int mouseX,int mouseY,CallbackInfo ci){
        if(!CompactStatusEffectsFeature.enabled()){
            CompactStatusEffectsFeature.setBounds(screen,null);
            return;
        }
        if(minecraft.player==null)return;

        LocalPlayer player=minecraft.player;
        List<MobEffectInstance> effects=player.getActiveEffects().stream().sorted().toList();
        ddstweak$effectCount=effects.size();
        ddstweak$expanded=ddstweak$isTabDown();
        ddstweak$updateArea();
        if(ddstweak$area==null){
            CompactStatusEffectsFeature.setBounds(screen,null);
            ci.cancel();
            return;
        }

        if(effects.isEmpty()){
            ddstweak$hoveredCompact=-1;
            CompactStatusEffectsFeature.setBounds(screen,null);
            return;
        }
        if(ddstweak$expanded){
            ddstweak$hoveredCompact=-1;
            //#if MC >= 12111
            ddstweak$area=ddstweak$fullExpandedBounds(effects);
            CompactStatusEffectsFeature.setBounds(screen,ddstweak$area);
            ddstweak$drawFullExpandedEffects(graphics,effects,mouseX,mouseY);
            ci.cancel();
            //#else
            //$$ CompactStatusEffectsFeature.setBounds(screen,ddstweak$area);
            //$$ ddstweak$beginExpandedTranslation(graphics);
            //#endif
            return;
        }

        ddstweak$hoveredCompact=ddstweak$findHoveredEffect(effects,mouseX,mouseY);
        int expandedWidth=ddstweak$hoveredCompact>=0?ddstweak$expandedWidth(effects.get(ddstweak$hoveredCompact)):CompactStatusEffectLayout.FRAME_SIZE;
        CompactStatusEffectsFeature.setBounds(screen,ddstweak$visibleBounds(effects.size(),ddstweak$hoveredCompact,expandedWidth));
        ddstweak$drawCompactEffects(graphics,effects,ddstweak$hoveredCompact,expandedWidth,mouseX,mouseY);
        ci.cancel();
    }

    @Unique
    private void ddstweak$drawCompactEffects(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            List<MobEffectInstance> effects,int hoveredIndex,int expandedWidth,int mouseX,int mouseY){
        CompactStatusEffectBounds hoveredFrame=null;
        for(int i=0;i<effects.size();i++){
            CompactStatusEffectBounds frame=ddstweak$renderFrameBounds(i,effects.size(),hoveredIndex,expandedWidth);
            if(i==hoveredIndex){
                hoveredFrame=frame;
                continue;
            }
            MobEffectInstance effect=effects.get(i);
            ddstweak$drawBackground(graphics,frame.x(),frame.y(),CompactStatusEffectLayout.FRAME_SIZE);
            ddstweak$drawEffectIcon(graphics,effect,frame.x()+CompactStatusEffectLayout.ICON_OFFSET,frame.y()+CompactStatusEffectLayout.ICON_OFFSET,CompactStatusEffectLayout.ICON_SIZE,CompactStatusEffectLayout.ICON_SIZE);
            ddstweak$drawLevelBadge(graphics,effect,frame);
        }
        if(hoveredIndex>=0&&hoveredFrame!=null)
            ddstweak$drawExpandedEffect(graphics,effects.get(hoveredIndex),hoveredFrame,expandedWidth,mouseX,mouseY);
    }

    @Unique
    private int ddstweak$findHoveredEffect(List<MobEffectInstance> effects,int mouseX,int mouseY){
        int count=effects.size();
        if(ddstweak$hoveredCompact>=0&&ddstweak$hoveredCompact<count){
            int width=ddstweak$expandedWidth(effects.get(ddstweak$hoveredCompact));
            CompactStatusEffectBounds expanded=ddstweak$expandedBounds(ddstweak$hoveredCompact,count,width);
            if(expanded.contains(mouseX,mouseY))return ddstweak$hoveredCompact;
            for(int i=0;i<count;i++){
                if(i==ddstweak$hoveredCompact)continue;
                if(ddstweak$renderFrameBounds(i,count,ddstweak$hoveredCompact,width).contains(mouseX,mouseY))return i;
            }
        }
        for(int i=0;i<count;i++)if(ddstweak$baseFrameBounds(i,count).contains(mouseX,mouseY))return i;
        return -1;
    }

    @Unique
    private CompactStatusEffectBounds ddstweak$baseFrameBounds(int index,int count){
        int x=ddstweak$area.x()+CompactStatusEffectLayout.columnOffset(index,count,CompactStatusEffectsFeature.isLeftSide());
        int y=ddstweak$area.y()+CompactStatusEffectLayout.row(index)*(CompactStatusEffectLayout.FRAME_SIZE+CompactStatusEffectLayout.GAP);
        return new CompactStatusEffectBounds(x,y,CompactStatusEffectLayout.FRAME_SIZE,CompactStatusEffectLayout.FRAME_SIZE);
    }

    @Unique
    private CompactStatusEffectBounds ddstweak$renderFrameBounds(int index,int count,int hoveredIndex,int expandedWidth){
        CompactStatusEffectBounds base=ddstweak$baseFrameBounds(index,count);
        int x=base.x()+CompactStatusEffectLayout.hoverColumnShift(index,hoveredIndex,expandedWidth,CompactStatusEffectsFeature.isLeftSide());
        int y=base.y()+CompactStatusEffectLayout.hoverRowShift(index,hoveredIndex);
        return new CompactStatusEffectBounds(x,y,base.width(),base.height());
    }

    @Unique
    private CompactStatusEffectBounds ddstweak$expandedBounds(int index,int count,int expandedWidth){
        CompactStatusEffectBounds frame=ddstweak$renderFrameBounds(index,count,index,expandedWidth);
        int x=CompactStatusEffectsFeature.isLeftSide()?frame.x()+frame.width()-expandedWidth:frame.x();
        return new CompactStatusEffectBounds(x,frame.y(),expandedWidth,CompactStatusEffectLayout.EXPANDED_HEIGHT);
    }

    @Unique
    private CompactStatusEffectBounds ddstweak$visibleBounds(int count,int hoveredIndex,int expandedWidth){
        if(hoveredIndex<0)return ddstweak$area;
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE;
        CompactStatusEffectBounds expanded=ddstweak$expandedBounds(hoveredIndex,count,expandedWidth);
        minX=Math.min(minX,expanded.x());minY=Math.min(minY,expanded.y());
        maxX=Math.max(maxX,expanded.x()+expanded.width());maxY=Math.max(maxY,expanded.y()+expanded.height());
        for(int i=0;i<count;i++){
            if(i==hoveredIndex)continue;
            CompactStatusEffectBounds frame=ddstweak$renderFrameBounds(i,count,hoveredIndex,expandedWidth);
            minX=Math.min(minX,frame.x());minY=Math.min(minY,frame.y());
            maxX=Math.max(maxX,frame.x()+frame.width());maxY=Math.max(maxY,frame.y()+frame.height());
        }
        return new CompactStatusEffectBounds(minX,minY,maxX-minX,maxY-minY);
    }

    @Unique
    private int ddstweak$desiredExpandedWidth(MobEffectInstance effect){
        int textWidth=Math.max(minecraft.font.width(CompactStatusEffectFormat.displayName(effect)),minecraft.font.width(CompactStatusEffectFormat.durationText(effect)));
        //#if MC >= 12111
        return Math.max(80,Math.min(160,textWidth+40));
        //#else
        //$$ return 120;
        //#endif
    }

    @Unique
    private int ddstweak$expandedWidth(MobEffectInstance effect){
        CompactStatusEffectBounds frame=ddstweak$baseFrameBounds(Math.max(0,ddstweak$hoveredCompact),Math.max(1,ddstweak$effectCount));
        int available=CompactStatusEffectsFeature.isLeftSide()?frame.x()+frame.width()-2:MinecraftCompat.guiScaledWidth(minecraft)-frame.x()-2;
        return Math.max(CompactStatusEffectLayout.FRAME_SIZE,Math.min(ddstweak$desiredExpandedWidth(effect),available));
    }

    @Unique
    private int ddstweak$fullExpandedWidth(MobEffectInstance effect){
        int guiLeft=ddstweak$screenAccess().ddstweak$getLeftPos();
        int anchor=CompactStatusEffectsFeature.isLeftSide()?guiLeft-2:guiLeft+ddstweak$screenAccess().ddstweak$getImageWidth()+2;
        int available=CompactStatusEffectsFeature.isLeftSide()?anchor-2:MinecraftCompat.guiScaledWidth(minecraft)-anchor-2;
        return Math.max(CompactStatusEffectLayout.FRAME_SIZE,Math.min(ddstweak$desiredExpandedWidth(effect),available));
    }

    @Unique
    private CompactStatusEffectBounds ddstweak$fullExpandedBounds(List<MobEffectInstance> effects){
        int guiLeft=ddstweak$screenAccess().ddstweak$getLeftPos();
        int anchor=CompactStatusEffectsFeature.isLeftSide()?guiLeft-2:guiLeft+ddstweak$screenAccess().ddstweak$getImageWidth()+2;
        int maxWidth=CompactStatusEffectLayout.FRAME_SIZE;
        for(MobEffectInstance effect:effects)maxWidth=Math.max(maxWidth,ddstweak$fullExpandedWidth(effect));
        int x=CompactStatusEffectLayout.expandedRowX(anchor,maxWidth,CompactStatusEffectsFeature.isLeftSide());
        return new CompactStatusEffectBounds(x,ddstweak$screenAccess().ddstweak$getTopPos(),maxWidth,CompactStatusEffectLayout.expandedHeight(effects.size()));
    }

    @Unique
    private void ddstweak$drawFullExpandedEffects(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            List<MobEffectInstance> effects,int mouseX,int mouseY){
        boolean leftSide=CompactStatusEffectsFeature.isLeftSide();
        int guiLeft=ddstweak$screenAccess().ddstweak$getLeftPos();
        int anchor=leftSide?guiLeft-2:guiLeft+ddstweak$screenAccess().ddstweak$getImageWidth()+2;
        int top=ddstweak$screenAccess().ddstweak$getTopPos();
        int step=CompactStatusEffectLayout.expandedStep(effects.size());
        for(int i=0;i<effects.size();i++){
            MobEffectInstance effect=effects.get(i);
            int rowWidth=ddstweak$fullExpandedWidth(effect);
            int rowX=CompactStatusEffectLayout.expandedRowX(anchor,rowWidth,leftSide);
            int dy=i*step;
            //#if MC >= 12111
            //#if MC >= 260100
            //$$ graphics.pose().pushMatrix();graphics.pose().translate(0.0F,(float)dy);
            //$$ ddstweak$invokeVanillaEffect(graphics,List.of(effect),rowX,CompactStatusEffectLayout.EXPANDED_HEIGHT,mouseX,mouseY-dy,rowWidth);
            //$$ graphics.pose().popMatrix();
            //#else
            //$$ graphics.pose().pushMatrix();graphics.pose().translate(0.0F,(float)dy);
            //$$ ddstweak$invokeVanillaEffect(graphics,List.of(effect),rowX,CompactStatusEffectLayout.EXPANDED_HEIGHT,mouseX,mouseY-dy,rowWidth);
            //$$ graphics.pose().popMatrix();
            //#endif
            //#else
            //$$ ddstweak$drawLegacyExpandedEffect(graphics,effect,new CompactStatusEffectBounds(rowX,top+dy,rowWidth,CompactStatusEffectLayout.EXPANDED_HEIGHT));
            //#endif
        }
    }

    @Unique
    private void ddstweak$drawExpandedEffect(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            MobEffectInstance effect,CompactStatusEffectBounds frame,int expandedWidth,int mouseX,int mouseY){
        CompactStatusEffectBounds expanded=ddstweak$expandedBounds(ddstweak$hoveredCompact,ddstweak$effectCount,expandedWidth);
        //#if MC >= 12111
        int vanillaX=expanded.x();
        int dy=expanded.y()-ddstweak$screenAccess().ddstweak$getTopPos();
        //#if MC >= 260100
        //$$ graphics.pose().pushMatrix();graphics.pose().translate(0.0F,(float)dy);
        //$$ ddstweak$invokeVanillaEffect(graphics,List.of(effect),vanillaX,CompactStatusEffectLayout.EXPANDED_HEIGHT,mouseX,mouseY-dy,expandedWidth);
        //$$ graphics.pose().popMatrix();
        //#else
        graphics.pose().pushMatrix();graphics.pose().translate(0.0F,(float)dy);
        ddstweak$invokeVanillaEffect(graphics,List.of(effect),vanillaX,CompactStatusEffectLayout.EXPANDED_HEIGHT,mouseX,mouseY-dy,expandedWidth);
        graphics.pose().popMatrix();
        //#endif
        //#else
        //$$ ddstweak$drawLegacyExpandedEffect(graphics,effect,expanded);
        //#endif
    }

    //#if MC >= 12111
    //#if MC >= 260100
    //$$ @Invoker("extractEffects")
    //$$ protected abstract void ddstweak$invokeVanillaEffect(GuiGraphicsExtractor graphics,Collection<MobEffectInstance> effects,int x0,int yStep,int mouseX,int mouseY,int maxWidth);
    //#else
    @Invoker("renderEffects")
    protected abstract void ddstweak$invokeVanillaEffect(GuiGraphics graphics,Collection<MobEffectInstance> effects,int x0,int yStep,int mouseX,int mouseY,int maxWidth);
    //#endif
    //#else
    //$$ @Unique
    //$$ private void ddstweak$drawLegacyExpandedEffect(GuiGraphics graphics,MobEffectInstance effect,CompactStatusEffectBounds bounds){
    //#if MC >= 12106
    //$$     graphics.blitSprite(RenderPipelines.GUI_TEXTURED,MinecraftCompat.id("minecraft","container/inventory/effect_background_large"),bounds.x(),bounds.y(),bounds.width(),bounds.height());
    //#else
    //$$     graphics.blitSprite(RenderType::guiTextured,MinecraftCompat.id("minecraft","container/inventory/effect_background_large"),bounds.x(),bounds.y(),bounds.width(),bounds.height());
    //#endif
    //$$     ddstweak$drawEffectIcon(graphics,effect,bounds.x()+7,bounds.y()+7,18,18);
    //$$     ScreenRenderCompat.text(graphics,minecraft.font,CompactStatusEffectFormat.displayName(effect),bounds.x()+32,bounds.y()+6,0xFFFFFFFF,false);
    //$$     ScreenRenderCompat.text(graphics,minecraft.font,CompactStatusEffectFormat.durationText(effect),bounds.x()+32,bounds.y()+16,0xFF7F7F7F,false);
    //$$ }
    //#endif

    @Unique
    private void ddstweak$drawLevelBadge(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            MobEffectInstance effect,CompactStatusEffectBounds frame){
        String badge=CompactStatusEffectFormat.levelBadge(effect);
        if(badge.isEmpty())return;
        int x=frame.x()+frame.width()-minecraft.font.width(badge)-2;
        int y=frame.y()+frame.height()-9;
        ScreenRenderCompat.text(graphics,minecraft.font,badge,x,y,0xFFFFFFFF,true);
    }

    @Unique
    private void ddstweak$drawEffectIcon(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            MobEffectInstance effect,int x,int y,int width,int height){
        //#if MC >= 260200
        //$$ graphics.blitSprite(RenderPipelines.GUI_TEXTURED,Hud.getMobEffectSprite(effect.getEffect()),x,y,width,height);
        //#elseif MC >= 12106
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,Gui.getMobEffectSprite(effect.getEffect()),x,y,width,height);
        //#else
        //$$ TextureAtlasSprite sprite=minecraft.getMobEffectTextures().get(effect.getEffect());
        //$$ graphics.blitSprite(RenderType::guiTextured,sprite,x,y,width,height);
        //#endif
    }

    @Unique
    private static void ddstweak$drawBackground(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            int x,int y,int size){
        //#if MC >= 12106
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,MinecraftCompat.id("minecraft","hud/effect_background"),x,y,size,size);
        //#else
        //$$ graphics.blitSprite(RenderType::guiTextured,MinecraftCompat.id("minecraft","hud/effect_background"),x,y,size,size);
        //#endif
    }

    @Unique
    private void ddstweak$updateArea(){
        if(!canSeeEffects()){
            ddstweak$area=null;
            return;
        }
        int guiLeft=ddstweak$screenAccess().ddstweak$getLeftPos();
        int top=ddstweak$screenAccess().ddstweak$getTopPos();
        if(ddstweak$expanded){
            boolean leftSide=CompactStatusEffectsFeature.isLeftSide();
            int space=leftSide?guiLeft-2:MinecraftCompat.guiScaledWidth(minecraft)-(guiLeft+ddstweak$screenAccess().ddstweak$getImageWidth()+2);
            boolean fullWidth=space>=120;
            int left=leftSide?(fullWidth?guiLeft-124:guiLeft-36):guiLeft+ddstweak$screenAccess().ddstweak$getImageWidth()+2;
            int height=CompactStatusEffectLayout.expandedHeight(ddstweak$effectCount);
            ddstweak$area=new CompactStatusEffectBounds(left,top,fullWidth?120:32,height);
            return;
        }

        int compactWidth=CompactStatusEffectLayout.width(ddstweak$effectCount);
        int compactHeight=CompactStatusEffectLayout.height(ddstweak$effectCount);
        int left=CompactStatusEffectsFeature.isLeftSide()?guiLeft-2-compactWidth:guiLeft+ddstweak$screenAccess().ddstweak$getImageWidth()+2;
        ddstweak$area=new CompactStatusEffectBounds(left,top,compactWidth,compactHeight);
    }

    @Unique
    private boolean ddstweak$isTabDown(){
        if("key.keyboard.tab".equals(minecraft.options.keyInventory.saveString()))return false;
        //#if MC >= 260300
        //$$ return InputConstants.isKeyDown(InputConstants.KEY_TAB);
        //#else
        return GLFW.glfwGetKey(MinecraftCompat.windowHandle(minecraft),GLFW.GLFW_KEY_TAB)==GLFW.GLFW_PRESS;
        //#endif
    }

    @Unique
    private void ddstweak$beginExpandedTranslation(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics
            //#else
            GuiGraphics graphics
            //#endif
    ){
        if(!CompactStatusEffectsFeature.isLeftSide()||ddstweak$translated)return;
        int right=ddstweak$screenAccess().ddstweak$getLeftPos()+ddstweak$screenAccess().ddstweak$getImageWidth()+2;
        int dx=ddstweak$area.x()-right;
        //#if MC >= 260100
        //$$ graphics.pose().pushMatrix();graphics.pose().translate((float)dx,0.0F);
        //#elseif MC >= 12106
        graphics.pose().pushMatrix();graphics.pose().translate((float)dx,0.0F);
        //#else
        //$$ graphics.pose().pushPose();graphics.pose().translate((double)dx,0.0D,0.0D);
        //#endif
        ddstweak$translated=true;
    }

    //#if MC >= 260100
    //$$ @Inject(method="extractRenderState",at=@At("RETURN"),require=0)
    //#elseif MC >= 12111
    @Inject(method="render",at=@At("RETURN"),require=0)
    //#else
    //$$ @Inject(method="renderEffects",at=@At("RETURN"),require=0)
    //#endif
    private void ddstweak$restoreExpandedTranslation(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            int mouseX,int mouseY,CallbackInfo ci){
        if(!ddstweak$translated)return;
        //#if MC >= 260100
        //$$ graphics.pose().popMatrix();
        //#elseif MC >= 12106
        graphics.pose().popMatrix();
        //#else
        //$$ graphics.pose().popPose();
        //#endif
        ddstweak$translated=false;
    }

    @Unique
    private CompactAbstractContainerScreenAccessor ddstweak$screenAccess(){
        return (CompactAbstractContainerScreenAccessor)(Object)screen;
    }

    @Shadow public abstract boolean canSeeEffects();

    @Inject(method="canSeeEffects",at=@At("HEAD"),cancellable=true,require=0)
    private void ddstweak$canSeeCompactEffects(CallbackInfoReturnable<Boolean> ci){
        if(!CompactStatusEffectsFeature.enabled())return;
        if(CompactStatusEffectsFeature.requiresHoldingTab()&&!ddstweak$isTabDown()){
            ci.setReturnValue(false);
            return;
        }
        int guiLeft=ddstweak$screenAccess().ddstweak$getLeftPos();
        int requiredWidth=ddstweak$isTabDown()?32:CompactStatusEffectLayout.width(Math.max(1,ddstweak$effectCount));
        if(CompactStatusEffectsFeature.isLeftSide())ci.setReturnValue(guiLeft-2>=requiredWidth);
        else ci.setReturnValue(MinecraftCompat.guiScaledWidth(minecraft)-(guiLeft+ddstweak$screenAccess().ddstweak$getImageWidth()+2)>=requiredWidth);
    }

    //#if MC >= 260100
    //$$ @ModifyVariable(method="extractRenderState",at=@At(value="STORE"),ordinal=1,require=0)
    //#elseif MC >= 12111
    @ModifyVariable(method="render",at=@At(value="STORE"),ordinal=1,require=0)
    //#else
    //$$ @ModifyVariable(method="renderEffects",at=@At(value="STORE"),ordinal=1,require=0)
    //#endif
    private int ddstweak$leftEffectsSpace(int value){
        return CompactStatusEffectsFeature.enabled()&&CompactStatusEffectsFeature.isLeftSide()?ddstweak$screenAccess().ddstweak$getLeftPos()-2:value;
    }

    //#if MC >= 260100
    //$$ @ModifyVariable(method="extractRenderState",at=@At(value="STORE"),ordinal=0,require=0)
    //#elseif MC >= 12111
    @ModifyVariable(method="render",at=@At(value="STORE"),ordinal=0,require=0)
    //#else
    //$$ @ModifyVariable(method="renderEffects",at=@At(value="STORE"),ordinal=0,require=0)
    //#endif
    private boolean ddstweak$leftEffectsFullWidth(boolean value){
        if(!CompactStatusEffectsFeature.enabled())return value;
        int space=CompactStatusEffectsFeature.isLeftSide()
                ?ddstweak$screenAccess().ddstweak$getLeftPos()-2
                :MinecraftCompat.guiScaledWidth(minecraft)-(ddstweak$screenAccess().ddstweak$getLeftPos()+ddstweak$screenAccess().ddstweak$getImageWidth()+2);
        return space>=120;
    }
}
//#else
//$$ public final class EffectsInInventoryMixin {}
//#endif

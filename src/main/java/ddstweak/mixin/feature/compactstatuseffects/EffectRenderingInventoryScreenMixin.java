package ddstweak.mixin.feature.compactstatuseffects;

//#if MC >= 12101 && MC < 12102
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectBounds;
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectFormat;
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectLayout;
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectsAccess;
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectsFeature;
//$$ import ddstweak.platform.minecraft.MinecraftCompat;
//$$ import ddstweak.platform.minecraft.ScreenRenderCompat;
//$$ import java.util.List;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.GuiGraphics;
//$$ import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//$$ import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
//$$ import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
//$$ import net.minecraft.client.player.LocalPlayer;
//$$ import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//$$ import net.minecraft.network.chat.Component;
//$$ import net.minecraft.world.effect.MobEffectInstance;
//$$ import net.minecraft.world.entity.player.Inventory;
//$$ import net.minecraft.world.inventory.AbstractContainerMenu;
//$$ import org.lwjgl.glfw.GLFW;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ @Mixin(value=EffectRenderingInventoryScreen.class,priority=1100)
//$$ public abstract class EffectRenderingInventoryScreenMixin<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements CompactStatusEffectsAccess {
//$$     @Unique private boolean ddstweak$effectsExpanded;
//$$     @Unique private int ddstweak$effectCount;
//$$     @Unique private CompactStatusEffectBounds ddstweak$effectArea;
//$$     @Unique private boolean ddstweak$translated;
//$$     @Unique private int ddstweak$hoveredCompact=-1;
//$$
//$$     protected EffectRenderingInventoryScreenMixin(T menu,Inventory inventory,Component title){super(menu,inventory,title);}
//$$
//$$     @Inject(method="renderEffects",at=@At("HEAD"),cancellable=true,require=0)
//$$     private void ddstweak$renderCompactEffects(GuiGraphics graphics,int mouseX,int mouseY,CallbackInfo ci){
//$$         if(!CompactStatusEffectsFeature.enabled()){CompactStatusEffectsFeature.setBounds(this,null);return;}
//$$         if(minecraft==null||minecraft.player==null)return;
//$$
//$$         LocalPlayer player=minecraft.player;
//$$         List<MobEffectInstance> effects=player.getActiveEffects().stream().sorted().toList();
//$$         ddstweak$effectCount=effects.size();
//$$         ddstweak$effectsExpanded=ddstweak$isTabDown();
//$$         ddstweak$updateArea();
//$$         if(ddstweak$effectArea==null){CompactStatusEffectsFeature.setBounds(this,null);ci.cancel();return;}
//$$
//$$         if(effects.isEmpty()){
//$$             ddstweak$hoveredCompact=-1;
//$$             CompactStatusEffectsFeature.setBounds(this,null);
//$$             return;
//$$         }
//$$         if(ddstweak$effectsExpanded){
//$$             ddstweak$hoveredCompact=-1;
//$$             CompactStatusEffectsFeature.setBounds(this,ddstweak$effectArea);
//$$             ddstweak$beginExpandedTranslation(graphics);
//$$             return;
//$$         }
//$$
//$$         ddstweak$hoveredCompact=ddstweak$findHoveredEffect(effects,mouseX,mouseY);
//$$         int expandedWidth=ddstweak$hoveredCompact>=0?ddstweak$expandedWidth(effects.get(ddstweak$hoveredCompact)):CompactStatusEffectLayout.FRAME_SIZE;
//$$         CompactStatusEffectsFeature.setBounds(this,ddstweak$visibleBounds(effects.size(),ddstweak$hoveredCompact,expandedWidth));
//$$         ddstweak$drawCompactEffects(graphics,effects,ddstweak$hoveredCompact,expandedWidth);
//$$         ci.cancel();
//$$     }
//$$
//$$     @Unique
//$$     private void ddstweak$drawCompactEffects(GuiGraphics graphics,List<MobEffectInstance> effects,int hoveredIndex,int expandedWidth){
//$$         CompactStatusEffectBounds hoveredFrame=null;
//$$         for(int i=0;i<effects.size();i++){
//$$             CompactStatusEffectBounds frame=ddstweak$renderFrameBounds(i,effects.size(),hoveredIndex,expandedWidth);
//$$             if(i==hoveredIndex){hoveredFrame=frame;continue;}
//$$             MobEffectInstance effect=effects.get(i);
//$$             ddstweak$drawBackground(graphics,frame.x(),frame.y(),CompactStatusEffectLayout.FRAME_SIZE);
//$$             ddstweak$drawEffectIcon(graphics,minecraft.getMobEffectTextures().get(effect.getEffect()),frame.x()+CompactStatusEffectLayout.ICON_OFFSET,frame.y()+CompactStatusEffectLayout.ICON_OFFSET,CompactStatusEffectLayout.ICON_SIZE,CompactStatusEffectLayout.ICON_SIZE);
//$$             ddstweak$drawLevelBadge(graphics,effect,frame);
//$$         }
//$$         if(hoveredIndex>=0&&hoveredFrame!=null)ddstweak$drawExpandedEffect(graphics,effects.get(hoveredIndex),hoveredFrame,expandedWidth);
//$$     }
//$$
//$$     @Unique
//$$     private int ddstweak$findHoveredEffect(List<MobEffectInstance> effects,int mouseX,int mouseY){
//$$         int count=effects.size();
//$$         if(ddstweak$hoveredCompact>=0&&ddstweak$hoveredCompact<count){
//$$             int width=ddstweak$expandedWidth(effects.get(ddstweak$hoveredCompact));
//$$             CompactStatusEffectBounds expanded=ddstweak$expandedBounds(ddstweak$hoveredCompact,count,width);
//$$             if(expanded.contains(mouseX,mouseY))return ddstweak$hoveredCompact;
//$$             for(int i=0;i<count;i++){
//$$                 if(i==ddstweak$hoveredCompact)continue;
//$$                 if(ddstweak$renderFrameBounds(i,count,ddstweak$hoveredCompact,width).contains(mouseX,mouseY))return i;
//$$             }
//$$         }
//$$         for(int i=0;i<count;i++)if(ddstweak$baseFrameBounds(i,count).contains(mouseX,mouseY))return i;
//$$         return -1;
//$$     }
//$$
//$$     @Unique
//$$     private CompactStatusEffectBounds ddstweak$baseFrameBounds(int index,int count){
//$$         int x=ddstweak$effectArea.x()+CompactStatusEffectLayout.columnOffset(index,count,CompactStatusEffectsFeature.isLeftSide());
//$$         int y=ddstweak$effectArea.y()+CompactStatusEffectLayout.row(index)*(CompactStatusEffectLayout.FRAME_SIZE+CompactStatusEffectLayout.GAP);
//$$         return new CompactStatusEffectBounds(x,y,CompactStatusEffectLayout.FRAME_SIZE,CompactStatusEffectLayout.FRAME_SIZE);
//$$     }
//$$
//$$     @Unique
//$$     private CompactStatusEffectBounds ddstweak$renderFrameBounds(int index,int count,int hoveredIndex,int expandedWidth){
//$$         CompactStatusEffectBounds base=ddstweak$baseFrameBounds(index,count);
//$$         int x=base.x()+CompactStatusEffectLayout.hoverColumnShift(index,hoveredIndex,expandedWidth,CompactStatusEffectsFeature.isLeftSide());
//$$         int y=base.y()+CompactStatusEffectLayout.hoverRowShift(index,hoveredIndex);
//$$         return new CompactStatusEffectBounds(x,y,base.width(),base.height());
//$$     }
//$$
//$$     @Unique
//$$     private CompactStatusEffectBounds ddstweak$expandedBounds(int index,int count,int expandedWidth){
//$$         CompactStatusEffectBounds frame=ddstweak$renderFrameBounds(index,count,index,expandedWidth);
//$$         int x=CompactStatusEffectsFeature.isLeftSide()?frame.x()+frame.width()-expandedWidth:frame.x();
//$$         return new CompactStatusEffectBounds(x,frame.y(),expandedWidth,CompactStatusEffectLayout.EXPANDED_HEIGHT);
//$$     }
//$$
//$$     @Unique
//$$     private CompactStatusEffectBounds ddstweak$visibleBounds(int count,int hoveredIndex,int expandedWidth){
//$$         if(hoveredIndex<0)return ddstweak$effectArea;
//$$         int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE;
//$$         CompactStatusEffectBounds expanded=ddstweak$expandedBounds(hoveredIndex,count,expandedWidth);
//$$         minX=Math.min(minX,expanded.x());minY=Math.min(minY,expanded.y());
//$$         maxX=Math.max(maxX,expanded.x()+expanded.width());maxY=Math.max(maxY,expanded.y()+expanded.height());
//$$         for(int i=0;i<count;i++){
//$$             if(i==hoveredIndex)continue;
//$$             CompactStatusEffectBounds frame=ddstweak$renderFrameBounds(i,count,hoveredIndex,expandedWidth);
//$$             minX=Math.min(minX,frame.x());minY=Math.min(minY,frame.y());
//$$             maxX=Math.max(maxX,frame.x()+frame.width());maxY=Math.max(maxY,frame.y()+frame.height());
//$$         }
//$$         return new CompactStatusEffectBounds(minX,minY,maxX-minX,maxY-minY);
//$$     }
//$$
//$$     @Unique
//$$     private int ddstweak$expandedWidth(MobEffectInstance effect){
//$$         int textWidth=Math.max(minecraft.font.width(CompactStatusEffectFormat.displayName(effect)),minecraft.font.width(CompactStatusEffectFormat.durationText(effect)));
//$$         int desired=Math.max(120,Math.min(160,textWidth+40));
//$$         CompactStatusEffectBounds frame=ddstweak$baseFrameBounds(Math.max(0,ddstweak$hoveredCompact),Math.max(1,ddstweak$effectCount));
//$$         int available=CompactStatusEffectsFeature.isLeftSide()?frame.x()+frame.width()-2:width-frame.x()-2;
//$$         return Math.max(CompactStatusEffectLayout.FRAME_SIZE,Math.min(desired,available));
//$$     }
//$$
//$$     @Unique
//$$     private void ddstweak$drawExpandedEffect(GuiGraphics graphics,MobEffectInstance effect,CompactStatusEffectBounds frame,int expandedWidth){
//$$         CompactStatusEffectBounds bounds=ddstweak$expandedBounds(ddstweak$hoveredCompact,ddstweak$effectCount,expandedWidth);
//$$         graphics.blitSprite(MinecraftCompat.id("minecraft","container/inventory/effect_background_large"),bounds.x(),bounds.y(),bounds.width(),bounds.height());
//$$         ddstweak$drawEffectIcon(graphics,minecraft.getMobEffectTextures().get(effect.getEffect()),bounds.x()+7,bounds.y()+7,18,18);
//$$         ScreenRenderCompat.text(graphics,minecraft.font,CompactStatusEffectFormat.displayName(effect),bounds.x()+32,bounds.y()+6,0xFFFFFF,false);
//$$         ScreenRenderCompat.text(graphics,minecraft.font,CompactStatusEffectFormat.durationText(effect),bounds.x()+32,bounds.y()+16,0x7F7F7F,false);
//$$     }
//$$
//$$     @Unique
//$$     private void ddstweak$drawLevelBadge(GuiGraphics graphics,MobEffectInstance effect,CompactStatusEffectBounds frame){
//$$         String badge=CompactStatusEffectFormat.levelBadge(effect);
//$$         if(badge.isEmpty())return;
//$$         int x=frame.x()+frame.width()-minecraft.font.width(badge)-2;
//$$         int y=frame.y()+frame.height()-9;
//$$         ScreenRenderCompat.text(graphics,minecraft.font,badge,x,y,0xFFFFFFFF,true);
//$$     }
//$$
//$$     @Unique
//$$     private static void ddstweak$drawEffectIcon(GuiGraphics graphics,TextureAtlasSprite sprite,int x,int y,int width,int height){
//$$         graphics.blit(x,y,0,width,height,sprite);
//$$     }
//$$
//$$     @Unique
//$$     private static void ddstweak$drawBackground(GuiGraphics graphics,int x,int y,int size){
//$$         graphics.blitSprite(GuiAccessor.ddstweak$getEffectBackgroundSprite(),x,y,size,size);
//$$     }
//$$
//$$     @Unique
//$$     private void ddstweak$updateArea(){
//$$         if(!canSeeEffects()){ddstweak$effectArea=null;return;}
//$$         if(ddstweak$effectsExpanded){
//$$             boolean leftSide=CompactStatusEffectsFeature.isLeftSide();
//$$             int space=leftSide?leftPos-2:width-(leftPos+imageWidth+2);
//$$             boolean fullWidth=space>=120;
//$$             int left=leftSide?(fullWidth?leftPos-124:leftPos-36):leftPos+imageWidth+2;
//$$             int height=ddstweak$effectCount>5?165:33*ddstweak$effectCount;
//$$             ddstweak$effectArea=new CompactStatusEffectBounds(left,topPos,fullWidth?120:32,height);
//$$             return;
//$$         }
//$$         int compactWidth=CompactStatusEffectLayout.width(ddstweak$effectCount);
//$$         int compactHeight=CompactStatusEffectLayout.height(ddstweak$effectCount);
//$$         int left=CompactStatusEffectsFeature.isLeftSide()?leftPos-2-compactWidth:leftPos+imageWidth+2;
//$$         ddstweak$effectArea=new CompactStatusEffectBounds(left,topPos,compactWidth,compactHeight);
//$$     }
//$$
//$$     @Unique
//$$     private boolean ddstweak$isTabDown(){
//$$         if("key.keyboard.tab".equals(minecraft.options.keyInventory.saveString()))return false;
//$$         return GLFW.glfwGetKey(MinecraftCompat.windowHandle(minecraft),GLFW.GLFW_KEY_TAB)==GLFW.GLFW_PRESS;
//$$     }
//$$
//$$     @Unique
//$$     private void ddstweak$beginExpandedTranslation(GuiGraphics graphics){
//$$         if(!CompactStatusEffectsFeature.isLeftSide()||ddstweak$translated)return;
//$$         int right=leftPos+imageWidth+2;
//$$         int dx=ddstweak$effectArea.x()-right;
//$$         graphics.pose().pushPose();graphics.pose().translate((double)dx,0.0D,0.0D);
//$$         ddstweak$translated=true;
//$$     }
//$$
//$$     @Inject(method="renderEffects",at=@At("RETURN"),require=0)
//$$     private void ddstweak$restoreExpandedTranslation(GuiGraphics graphics,int mouseX,int mouseY,CallbackInfo ci){
//$$         if(!ddstweak$translated)return;
//$$         graphics.pose().popPose();
//$$         ddstweak$translated=false;
//$$     }
//$$
//$$     @Override public CompactStatusEffectBounds ddstweak$getCompactStatusEffectBounds(){
//$$         return CompactStatusEffectsFeature.enabled()?CompactStatusEffectsFeature.bounds(this):null;
//$$     }
//$$
//$$     @Inject(method="canSeeEffects",at=@At("HEAD"),cancellable=true,require=0)
//$$     private void ddstweak$canSeeCompactEffects(CallbackInfoReturnable<Boolean> ci){
//$$         if(!CompactStatusEffectsFeature.enabled())return;
//$$         if(CompactStatusEffectsFeature.requiresHoldingTab()&&!ddstweak$isTabDown()){
//$$             ci.setReturnValue(false);
//$$             return;
//$$         }
//$$         if(this instanceof RecipeUpdateListener listener&&listener.getRecipeBookComponent().isVisible()){
//$$             ci.setReturnValue(false);
//$$             return;
//$$         }
//$$         int requiredWidth=ddstweak$isTabDown()?32:CompactStatusEffectLayout.width(Math.max(1,ddstweak$effectCount));
//$$         if(CompactStatusEffectsFeature.isLeftSide())ci.setReturnValue(leftPos-2>=requiredWidth);
//$$         else ci.setReturnValue(width-(leftPos+imageWidth+2)>=requiredWidth);
//$$     }
//$$
//$$     @ModifyVariable(method="renderEffects",at=@At(value="STORE",ordinal=0),index=5,require=0)
//$$     private int ddstweak$leftEffectsSpace(int value){
//$$         return CompactStatusEffectsFeature.enabled()&&CompactStatusEffectsFeature.isLeftSide()?leftPos-2:value;
//$$     }
//$$
//$$     @ModifyVariable(method="renderEffects",at=@At(value="STORE",ordinal=0),index=7,require=0)
//$$     private boolean ddstweak$fullWidthEffects(boolean value){
//$$         if(!CompactStatusEffectsFeature.enabled())return value;
//$$         int space=CompactStatusEffectsFeature.isLeftSide()?leftPos-2:width-(leftPos+imageWidth+2);
//$$         return space>=120;
//$$     }
//$$
//$$     @Shadow public abstract boolean canSeeEffects();
//$$ }
//#else
public final class EffectRenderingInventoryScreenMixin {}
//#endif

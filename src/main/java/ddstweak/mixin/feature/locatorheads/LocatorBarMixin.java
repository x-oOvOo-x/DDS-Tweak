package ddstweak.mixin.feature.locatorheads;

//#if MC >= 12108
//#if MC >= 260300
//$$ import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//#else
import com.mojang.blaze3d.pipeline.RenderPipeline;
//#endif
import ddstweak.feature.locatorheads.LocatorHeadsFeature;
import ddstweak.feature.locatorheads.LocatorHudTrace;
import ddstweak.feature.locatorheads.LocatorContourCache;
import ddstweak.feature.locatorheads.LocatorContourMask;
import ddstweak.feature.locatorheads.LocatorNameFades;
import ddstweak.feature.locatorheads.LocatorOptions;
import ddstweak.feature.locatorheads.LocatorSpriteDecision;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
//#if MC >= 260300
//$$ import net.minecraft.client.gui.contextualbar.LocatorBar;
//#elseif MC >= 260200
//$$ import net.minecraft.client.gui.contextualbar.LocatorBar;
//#else
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
//#endif
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
//#if MC >= 12109
import net.minecraft.world.waypoints.PartialTickSupplier;
//#endif
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;
import java.util.UUID;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only the original waypoint sprite at the vanilla rendering position.
 * The server retains complete authority over waypoint transmission/visibility.
 * 只替换原版定位图标绘制，不改变服务端可见性。
 */
//#if MC >= 260200
//$$ @Mixin(LocatorBar.class)
//#else
@Mixin(LocatorBarRenderer.class)
//#endif
public abstract class LocatorBarMixin {
    @Shadow @Final private Minecraft minecraft;
    //#if MC >= 260100
    //$$ @Inject(method="extractBackground",at=@At("HEAD"),require=1)
    //$$ private void ddstweak$countBackground(GuiGraphicsExtractor graphics,DeltaTracker ticks,CallbackInfo ci){
    //#else
    @Inject(method="renderBackground",at=@At("HEAD"),require=1)
    private void ddstweak$countBackground(GuiGraphics graphics,DeltaTracker ticks,CallbackInfo ci){
    //#endif
        LocatorHudTrace.background(false);
    }

    //#if MC >= 260100
    //$$ @Inject(method="extractRenderState",at=@At("HEAD"),require=1)
    //$$ private void ddstweak$countForeground(GuiGraphicsExtractor graphics,DeltaTracker ticks,CallbackInfo ci){
    //#else
    @Inject(method="render",at=@At("HEAD"),require=1)
    private void ddstweak$countForeground(GuiGraphics graphics,DeltaTracker ticks,CallbackInfo ci){
    //#endif
        LocatorHudTrace.foreground();
    }

    //#if MC >= 12111
    @Unique private Identifier ddstweak$texture;
    //#else
    //$$ @Unique private ResourceLocation ddstweak$texture;
    //#endif
    @Unique private TrackedWaypoint ddstweak$waypoint;
    @Unique private UUID ddstweak$uuid;
    @Unique private String ddstweak$name;
    @Unique private Integer ddstweak$teamColor;
    @Unique private boolean ddstweak$filtered;
    // The original pitch arrow is rendered after the marker blit: preserve its waypoint outcome.
    @Unique private LocatorSpriteDecision.Outcome ddstweak$pendingArrowOutcome=LocatorSpriteDecision.Outcome.ORIGINAL;
    //#if MC >= 260300
    //$$ @Inject(method="lambda$extractRenderState$1",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",ordinal=0,shift=At.Shift.BEFORE),require=1)
    //$$ private void ddstweak$capture(Entity viewer,Level world,PartialTickSupplier ticks,GuiGraphicsExtractor graphics,int index,TrackedWaypoint waypoint,CallbackInfo ci){
    //#elseif MC >= 260100
    //$$ @Inject(method="lambda$extractRenderState$1",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$capture(Entity viewer,Level world,PartialTickSupplier ticks,GuiGraphicsExtractor graphics,int index,TrackedWaypoint waypoint,CallbackInfo ci){
    //#elseif MC >= 12111
    @Inject(method="method_70870",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",ordinal=0),require=1)
    private void ddstweak$capture(Entity viewer,Level world,PartialTickSupplier ticks,GuiGraphics graphics,int index,TrackedWaypoint waypoint,CallbackInfo ci){
    //#elseif MC >= 12109
    //$$ @Inject(method="method_70870",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$capture(Entity viewer,Level world,PartialTickSupplier ticks,GuiGraphics graphics,int index,TrackedWaypoint waypoint,CallbackInfo ci){
    //#else
    //$$ @Inject(method="method_70870",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$capture(Level world,GuiGraphics graphics,int index,TrackedWaypoint waypoint,CallbackInfo ci){
    //#endif
        //#if MC < 12109
        //$$ Entity viewer=minecraft.getCameraEntity();
        //#endif
        LocatorHudTrace.capture();
        ddstweak$waypoint=null;ddstweak$texture=null;ddstweak$uuid=null;ddstweak$name=null;
        ddstweak$teamColor=null;ddstweak$filtered=false;
        ddstweak$pendingArrowOutcome=LocatorSpriteDecision.Outcome.ORIGINAL;
        if(!LocatorHeadsFeature.enabled()||world==null||viewer==null||minecraft.getConnection()==null)return;
        if(waypoint.id().left().isEmpty())return;
        UUID uuid=waypoint.id().left().get();
        var info=minecraft.getConnection().getPlayerInfo(uuid);
        if(info==null)return;
        String name=info.getProfile().name();
        if(!LocatorHeadsFeature.allowsPlayer(name)||
                (LocatorHeadsFeature.nearbyOnly()&&world.getPlayerByUUID(uuid)==null)){
            ddstweak$filtered=true;return;
        }
        ddstweak$waypoint=waypoint;ddstweak$uuid=uuid;ddstweak$name=name;
        //#if MC >= 12109
        ddstweak$texture=info.getSkin().body().texturePath();
        //#else
        //$$ ddstweak$texture=info.getSkin().texture();
        //#endif
        PlayerTeam team=world.getScoreboard().getPlayersTeam(name);
        if(team!=null){
            //#if MC >= 260200
            //$$ if(team.getColor().isPresent())ddstweak$teamColor=team.getColor().get().rgb();
            //#else
            ddstweak$teamColor=team.getColor().getColor();
            //#endif
        }
    }

    //#if MC >= 260300
    //$$ @Redirect(method="lambda$extractRenderState$1",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$replace(GuiGraphicsExtractor graphics,RenderPipeline pipeline,Identifier sprite,int x,int y,int width,int height,int depth){
    //#elseif MC >= 260100
    //$$ @WrapOperation(method="lambda$extractRenderState$1",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$replace(GuiGraphicsExtractor graphics,RenderPipeline pipeline,Identifier sprite,int x,int y,int width,int height,int depth,Operation<Void> original){
    //#elseif MC >= 12111
    @WrapOperation(method="method_70870",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",ordinal=0),require=1)
    private void ddstweak$replace(GuiGraphics graphics,RenderPipeline pipeline,Identifier sprite,int x,int y,int width,int height,int depth,Operation<Void> original){
    //#else
    //$$ @WrapOperation(method="method_70870",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$replace(GuiGraphics graphics,RenderPipeline pipeline,ResourceLocation sprite,int x,int y,int width,int height,int depth,Operation<Void> original){
    //#endif
        try{
            boolean enabled=LocatorHeadsFeature.enabled();
            // Never render a hidden or disabled marker; preserve fallback to vanilla if skin rendering fails.
            boolean drewHead=enabled&&!ddstweak$filtered&&ddstweak$renderHead(graphics,x,y,width,height);
            ddstweak$pendingArrowOutcome=LocatorSpriteDecision.decide(enabled,ddstweak$filtered,drewHead);
            LocatorHudTrace.outcome(ddstweak$pendingArrowOutcome);
            if(ddstweak$pendingArrowOutcome==LocatorSpriteDecision.Outcome.ORIGINAL){
                //#if MC >= 260300
                //$$ graphics.blitSprite(pipeline,sprite,x,y,width,height,depth);
                //#else
                original.call(graphics,pipeline,sprite,x,y,width,height,depth);
                //#endif
            }
        }finally{
            ddstweak$waypoint=null;ddstweak$texture=null;ddstweak$uuid=null;ddstweak$name=null;
            ddstweak$filtered=false;ddstweak$teamColor=null;
        }
    }

    // Separate from the colored dot: vanilla submits the optional up/down indicator afterwards.
    //#if MC >= 260300
    //$$ @Redirect(method="lambda$extractRenderState$1",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$filterPitchArrow(GuiGraphicsExtractor graphics,RenderPipeline pipeline,Identifier arrow,int x,int y,int width,int height){
    //#elseif MC >= 260100
    //$$ @WrapOperation(method="lambda$extractRenderState$1",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$filterPitchArrow(GuiGraphicsExtractor graphics,RenderPipeline pipeline,Identifier arrow,int x,int y,int width,int height,Operation<Void> original){
    //#elseif MC >= 12111
    @WrapOperation(method="method_70870",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",ordinal=0),require=1)
    private void ddstweak$filterPitchArrow(GuiGraphics graphics,RenderPipeline pipeline,Identifier arrow,int x,int y,int width,int height,Operation<Void> original){
    //#else
    //$$ @WrapOperation(method="method_70870",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V",ordinal=0),require=1)
    //$$ private void ddstweak$filterPitchArrow(GuiGraphics graphics,RenderPipeline pipeline,ResourceLocation arrow,int x,int y,int width,int height,Operation<Void> original){
    //#endif
        try{
            LocatorHudTrace.arrow(ddstweak$pendingArrowOutcome==LocatorSpriteDecision.Outcome.HIDDEN);
            if(ddstweak$pendingArrowOutcome!=LocatorSpriteDecision.Outcome.HIDDEN){
                //#if MC >= 260300
                //$$ graphics.blitSprite(pipeline,arrow,x,y,width,height);
                //#else
                original.call(graphics,pipeline,arrow,x,y,width,height);
                //#endif
            }
        }finally{
            ddstweak$pendingArrowOutcome=LocatorSpriteDecision.Outcome.ORIGINAL;
        }
    }

    //#if MC >= 260100
    //$$ @Unique private boolean ddstweak$renderHead(GuiGraphicsExtractor graphics,int x,int y,int width,int height){
    //#else
    @Unique private boolean ddstweak$renderHead(GuiGraphics graphics,int x,int y,int width,int height){
    //#endif
        if(!LocatorHeadsFeature.enabled()||ddstweak$waypoint==null||ddstweak$texture==null)return false;
        Entity camera=minecraft.getCameraEntity();
        if(camera==null)return false;
        float distance=Mth.sqrt((float)ddstweak$waypoint.distanceSquared(camera));
        Waypoint.Icon icon=ddstweak$waypoint.icon();
        //#if MC >= 260200
        //$$ WaypointStyle style=minecraft.gui.hud.getWaypointStyles().get(icon.style);
        //#else
        WaypointStyle style=minecraft.getWaypointStyles().get(icon.style);
        //#endif
        if(style==null)return false;
        float span=style.farDistance()-style.nearDistance();
        float progress=span<=0?0:1-Mth.clamp((distance-style.nearDistance())/span,0,1);
        float scale=LocatorHeadsFeature.headScale();
        int w=Math.round(Mth.lerp(progress,5,width)*scale*100f);
        int h=Math.round(Mth.lerp(progress,5,height)*scale*100f);
        int left=x*100+width*50-w/2,top=y*100+height*50-h/2;
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.01f);
        // The second skin layer has its own switch and renders independently of the outline cache.
        // A 1/8-width overhang makes transparent hair/hat edges visible at HUD scale.
        int overhang=Math.max(1,Math.round(Math.min(w,h)/8f));
        if(LocatorHeadsFeature.borderEnabled()){
            // Unassigned teams have no team RGB: use the configured static color instead.
            int color=LocatorHeadsFeature.borderStyle()==LocatorOptions.BorderStyle.TEAM&&ddstweak$teamColor!=null?
                    0xFF000000|(ddstweak$teamColor&0xFFFFFF):LocatorHeadsFeature.fixedBorderArgb();
            var mask=LocatorContourCache.get(ddstweak$texture,LocatorHeadsFeature.outerLayer(),
                    LocatorHeadsFeature.borderThickness(),Math.min(w,h)/100f);
            if(mask!=null){
                int padX=w*LocatorContourMask.FACE_START/LocatorContourMask.FACE_SIZE;
                int padY=h*LocatorContourMask.FACE_START/LocatorContourMask.FACE_SIZE;
                int maskWidth=w*LocatorContourMask.SIZE/LocatorContourMask.FACE_SIZE;
                int maskHeight=h*LocatorContourMask.SIZE/LocatorContourMask.FACE_SIZE;
                graphics.blit(RenderPipelines.GUI_TEXTURED,mask,left-padX,top-padY,
                        0f,0f,maskWidth,maskHeight,LocatorContourMask.SIZE,
                        LocatorContourMask.SIZE,LocatorContourMask.SIZE,LocatorContourMask.SIZE,color);
            }
        }
        // Every supported GUI backend blits the base and optional outer layer independently.
        // The 26.2 PlayerFaceExtractor draws the hat at base size, hiding the intended overhang.
        graphics.blit(ddstweak$texture,left,top,left+w,top+h,1f/8,2f/8,1f/8,2f/8);
        if(LocatorHeadsFeature.outerLayer()){
            graphics.blit(ddstweak$texture,left-overhang,top-overhang,left+w+overhang,top+h+overhang,
                    5f/8,6f/8,1f/8,2f/8);
        }
        graphics.pose().popMatrix();
        ddstweak$renderName(graphics,x+width/2,y,scale);
        return true;
    }

    //#if MC >= 260100
    //$$ @Unique private void ddstweak$renderName(GuiGraphicsExtractor graphics,int centerX,int y,float scale){
    //#else
    @Unique private void ddstweak$renderName(GuiGraphics graphics,int centerX,int y,float scale){
    //#endif
        if(ddstweak$name==null||ddstweak$uuid==null)return;
        LocatorOptions.NameMode mode=LocatorHeadsFeature.nameMode();
        boolean visible=switch(mode){
            case OFF->false;
            case ALWAYS->true;
            case PLAYER_LIST->minecraft.options.keyPlayerList.isDown();
            case LOOKING_AT->ddstweak$lookingAtPlayer();
        };
        float ease=LocatorNameFades.ease(ddstweak$uuid,visible,System.nanoTime());
        int alpha=Math.round(191*ease);
        if(alpha==0)return;
        int color=LocatorHeadsFeature.borderEnabled()&&ddstweak$teamColor!=null&&
                LocatorHeadsFeature.borderStyle()==LocatorOptions.BorderStyle.TEAM?ddstweak$teamColor:
                LocatorHeadsFeature.borderEnabled()&&LocatorHeadsFeature.borderStyle()==LocatorOptions.BorderStyle.STATIC?
                    LocatorHeadsFeature.fixedBorderColor():0xFFFFFF;
        color|=alpha<<24;
        int textX=centerX-minecraft.font.width(ddstweak$name)/2;
        int textY=y-Math.round(12*scale)+Math.round(20*(1-ease));
        //#if MC >= 260100
        //$$ graphics.text(minecraft.font,ddstweak$name,textX,textY,color,true);
        //#else
        graphics.drawString(minecraft.font,ddstweak$name,textX,textY,color,true);
        //#endif
    }

    @Unique private boolean ddstweak$lookingAtPlayer(){
        if(ddstweak$uuid==null||minecraft.level==null||minecraft.getCameraEntity()==null)return false;
        var player=minecraft.level.getPlayerByUUID(ddstweak$uuid);
        if(player==null)return false;
        var delta=player.position().subtract(minecraft.getCameraEntity().position());
        return delta.lengthSqr()>0&&minecraft.getCameraEntity().getViewVector(1f).dot(delta.normalize())>=Math.cos(Math.toRadians(20));
    }
}
//#else
//$$ public abstract class LocatorBarMixin {}
//#endif

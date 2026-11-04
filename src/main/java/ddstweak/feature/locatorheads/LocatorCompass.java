package ddstweak.feature.locatorheads;

//#if MC >= 12108
import ddstweak.DDSTweakReference;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
//#if MC >= 260100
//$$ import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//$$ import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//$$ import net.minecraft.resources.Identifier;
//#else
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Optional cardinal-direction ribbon aligned to the vanilla locator/experience bar. */
public final class LocatorCompass {
    private LocatorCompass(){}
    public static void initialize(){
        //#if MC >= 260100
        //$$ HudElementRegistry.attachElementAfter(VanillaHudElements.INFO_BAR,
        //$$         Identifier.fromNamespaceAndPath(DDSTweakReference.MOD_ID,"locator_compass"),LocatorCompass::render);
        //#else
        HudRenderCallback.EVENT.register(LocatorCompass::render);
        //#endif
    }
    //#if MC >= 260100
    //$$ private static void render(GuiGraphicsExtractor graphics,DeltaTracker ticks){
    //#else
    private static void render(GuiGraphics graphics,DeltaTracker ticks){
    //#endif
        Minecraft client=Minecraft.getInstance();
        if(!LocatorHeadsFeature.compassEnabled()||client.level==null||client.getCameraEntity()==null)return;
        //#if MC >= 260200
        //$$ if(client.gui.hud.isHidden())return;
        //#else
        if(client.options.hideGui)return;
        //#endif
        var screen=MinecraftCompat.screen(client);
        if(screen!=null&&!(screen instanceof ChatScreen)&&!(screen instanceof AbstractContainerScreen))return;
        if(FabricLoader.getInstance().isModLoaded("flashback")&&isFlashbackReplay())return;
        int center=client.getWindow().getGuiScaledWidth()/2,y=client.getWindow().getGuiScaledHeight()-31;
        float facing=client.getCameraEntity().getYRot();
        String[] labels={"S","W","N","E"};
        for(int i=0;i<4;i++)draw(graphics,client,center,y,facing,labels[i],i*90f);
    }
    //#if MC >= 260100
    //$$ private static void draw(GuiGraphicsExtractor graphics,Minecraft client,int center,int y,float facing,String label,float direction){
    //#else
    private static void draw(GuiGraphics graphics,Minecraft client,int center,int y,float facing,String label,float direction){
    //#endif
        float offset=LocatorHeadsFeature.relativeAngle(facing,direction);
        if(Math.abs(offset)>60f)return;
        int x=center+Math.round(offset/60f*91f)-client.font.width(label)/2;
        int configured=LocatorHeadsFeature.compassArgb();
        int alpha=Math.round((1f-Math.abs(offset)/60f*0.5f)*(configured>>>24));
        if(alpha<=0)return;
        int color=(alpha<<24)|(configured&0xFFFFFF);
        if(LocatorHeadsFeature.compassShadow()){
            int shadow=alpha<<24;
            text(graphics,client,label,x-1,y,shadow);
            text(graphics,client,label,x+1,y,shadow);
            text(graphics,client,label,x,y-1,shadow);
            text(graphics,client,label,x,y+1,shadow);
        }
        text(graphics,client,label,x,y,color);
    }
    //#if MC >= 260100
    //$$ private static void text(GuiGraphicsExtractor g,Minecraft client,String value,int x,int y,int color){
    //$$     g.text(client.font,value,x,y,color,false);
    //$$ }
    //#else
    private static void text(GuiGraphics g,Minecraft client,String value,int x,int y,int color){
        g.drawString(client.font,value,x,y,color,false);
    }
    //#endif
    private static boolean isFlashbackReplay(){
        try{
            Class<?> type=Class.forName("com.moulberry.flashback.Flashback");
            return Boolean.TRUE.equals(type.getMethod("isInReplay").invoke(null));
        }catch(ReflectiveOperationException|LinkageError ignored){return false;}
    }
}
//#else
//$$ public final class LocatorCompass { private LocatorCompass(){} public static void initialize(){} }
//#endif

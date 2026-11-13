package ddstweak.feature.happyghastspeed;

//#if MC >= 12106
import ddstweak.config.Configs;
import net.minecraft.client.Minecraft;
//#if MC < 12109
//$$ import net.minecraft.client.gui.screens.Screen;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Scales client riding input for an eligible Happy Ghast.
 * 仅对符合条件的快乐恶魂调整客户端骑乘输入。
 */
public final class HappyGhastRidingSpeedFeature {
    private static final double MIN=0.5D,MAX=2.0D,STEP=0.1D;
    private static final long SCROLL_RESET_NANOS=500_000_000L;
    private static double scrollProgress;
    private static long lastScrollNanos;

    private HappyGhastRidingSpeedFeature(){}

    public static Vec3 scaleRiddenInput(Player controller,Vec3 input){
        Minecraft minecraft=Minecraft.getInstance();
        return controller==minecraft.player?input.scale(Configs.Tweaks.HAPPY_GHAST_RIDING_SPEED_MULTIPLIER.getDoubleValue()):input;
    }

    public static boolean handleScroll(double vertical){
        Minecraft minecraft=Minecraft.getInstance();
        if(vertical==0.0D||ddstweak$hasScreen(minecraft)||minecraft.player==null||!ddstweak$altDown()||!ddstweak$isControllingHappyGhast(minecraft.player)){
            scrollProgress=0.0D;
            return false;
        }

        long now=System.nanoTime();
        if(now-lastScrollNanos>SCROLL_RESET_NANOS)scrollProgress=0.0D;
        lastScrollNanos=now;
        scrollProgress+=vertical;

        int steps=scrollProgress>=1.0D?(int)Math.floor(scrollProgress):scrollProgress<=-1.0D?(int)Math.ceil(scrollProgress):0;
        if(steps==0)return true;

        scrollProgress-=steps;
        double current=Configs.Tweaks.HAPPY_GHAST_RIDING_SPEED_MULTIPLIER.getDoubleValue();
        double value=ddstweak$roundTenth(Math.max(MIN,Math.min(MAX,current+steps*STEP)));
        Configs.Tweaks.HAPPY_GHAST_RIDING_SPEED_MULTIPLIER.setDoubleValue(value);
        ddstweak$showSpeed(minecraft.player,value);
        return true;
    }

    private static boolean ddstweak$isControllingHappyGhast(Player player){
        Entity vehicle=player.getVehicle();
        return vehicle instanceof HappyGhastRidingSpeedTarget&&vehicle.getControllingPassenger()==player;
    }

    private static double ddstweak$roundTenth(double value){
        return Math.round(value*10.0D)/10.0D;
    }

    private static void ddstweak$showSpeed(Player player,double value){
        Component message=Component.translatable("dds-tweak.happy_ghast.riding_speed",String.format(Locale.ROOT,"%.1f",value));
        //#if MC >= 260000
        //$$ player.sendOverlayMessage(message);
        //#else
        player.displayClientMessage(message,true);
        //#endif
    }

    private static boolean ddstweak$hasScreen(Minecraft minecraft){
        //#if MC >= 260200
        //$$ return minecraft.gui.screen()!=null;
        //#else
        return minecraft.screen!=null;
        //#endif
    }
    private static boolean ddstweak$altDown(){
        //#if MC >= 12109
        return Minecraft.getInstance().hasAltDown();
        //#else
        //$$ return Screen.hasAltDown();
        //#endif
    }
}
//#else
//$$ public final class HappyGhastRidingSpeedFeature {
//$$     private HappyGhastRidingSpeedFeature(){}
//$$ }
//#endif

package ddstweak.feature.locatorheads;

//#if MC >= 12108
import ddstweak.config.Configs;
import net.fabricmc.loader.api.FabricLoader;
import java.util.List;
import java.util.Locale;

/**
 * Reads locator HUD options and resolves feature availability.
 * 读取定位栏配置并判断功能是否可用。
 */
public final class LocatorHeadsFeature {
    private LocatorHeadsFeature(){}

    public static boolean enabled(){return Configs.Visual.LOCATOR_HEADS.getBooleanValue()&&!FabricLoader.getInstance().isModLoaded("locator-heads");}
    public static boolean outerLayer(){return Configs.Visual.LOCATOR_OUTER_LAYER.getBooleanValue();}
    public static boolean borderEnabled(){return Configs.Visual.LOCATOR_BORDER.getBooleanValue();}
    public static boolean compassEnabled(){return enabled()&&Configs.Visual.LOCATOR_COMPASS.getBooleanValue();}
    public static boolean alwaysXp(){return enabled()&&Configs.Visual.LOCATOR_ALWAYS_XP.getBooleanValue();}
    public static float headScale(){return headScale(Configs.Visual.LOCATOR_HEAD_SIZE.getIntegerValue());}
    public static float headScale(int percent){return Math.max(50,Math.min(150,percent))*0.9f/100f;}
    public static LocatorOptions.NameMode nameMode(){return (LocatorOptions.NameMode)Configs.Visual.LOCATOR_NAME_MODE.getOptionListValue();}
    public static LocatorOptions.BorderStyle borderStyle(){return (LocatorOptions.BorderStyle)Configs.Visual.LOCATOR_BORDER_STYLE.getOptionListValue();}
    public static float borderThickness(){return ((LocatorOptions.BorderThickness)Configs.Visual.LOCATOR_BORDER_THICKNESS.getOptionListValue()).pixels();}
    public static int fixedBorderArgb(){return Configs.Visual.LOCATOR_BORDER_COLOR.getIntegerValue();}
    public static int compassArgb(){return Configs.Visual.LOCATOR_COMPASS_COLOR.getIntegerValue();}
    public static int fixedBorderColor(){return fixedBorderArgb()&0xFFFFFF;}
    public static int compassColor(){return compassArgb()&0xFFFFFF;}
    public static boolean compassShadow(){return Configs.Visual.LOCATOR_COMPASS_SHADOW.getBooleanValue();}
    public static boolean nearbyOnly(){return Configs.Visual.LOCATOR_NEARBY_ONLY.getBooleanValue();}

    public static boolean allowsPlayer(String name){
        if(name==null||name.isEmpty())return false;
        LocatorOptions.PlayerFilter filter=(LocatorOptions.PlayerFilter)Configs.Visual.LOCATOR_PLAYER_FILTER.getOptionListValue();
        if(filter==LocatorOptions.PlayerFilter.ALL)return true;
        List<String> names=filter==LocatorOptions.PlayerFilter.INCLUDE?
                Configs.Visual.LOCATOR_INCLUDE.getStrings():Configs.Visual.LOCATOR_EXCLUDE.getStrings();
        boolean matched=matches(names,name);
        return filter==LocatorOptions.PlayerFilter.INCLUDE?matched:!matched;
    }

    /** Lists allow one name per row or comma/semicolon/colon/period separated names, like Locator Heads. */
    public static boolean matches(List<String> names,String playerName){
        if(names==null||playerName==null)return false;
        String target=playerName.trim().toLowerCase(Locale.ROOT);
        for(String entry:names){
            if(entry==null)continue;
            for(String token:entry.split("[,;:.]"))if(token.trim().toLowerCase(Locale.ROOT).equals(target))return true;
        }
        return false;
    }

    /** Matches the reference compass's 120-degree horizontal field of view. */
    public static float relativeAngle(float facing,float direction){
        float delta=(direction-facing)%360f;
        if(delta>180f)delta-=360f;
        if(delta<-180f)delta+=360f;
        return delta;
    }
}
//#else
//$$ public final class LocatorHeadsFeature {
//$$     private LocatorHeadsFeature(){}
//$$     public static boolean enabled(){return false;}
//$$ }
//#endif

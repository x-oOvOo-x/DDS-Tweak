package ddstweak.feature.locatorheads;

//#if MC >= 12108
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;

/** Pure, idempotent migration of preview Locator Heads settings before MaLiLib loads them. */
public final class LocatorConfigMigration {
    private LocatorConfigMigration(){}

    public static void migrate(JsonObject root){
        JsonElement element=root.get("Visual");
        if(element==null||!element.isJsonObject())return;
        JsonObject visual=element.getAsJsonObject();
        JsonElement size=visual.get("locatorHeadSize");
        if(size!=null&&size.isJsonPrimitive()&&size.getAsJsonPrimitive().isNumber()){
            int value=size.getAsInt();
            if(value>=1&&value<=9){
                int percent=value==9?150:value>=5?50+value*10:Math.max(50,40+value*10);
                visual.addProperty("locatorHeadSize",percent);
            }
        }
        migrateColor(visual,"locatorCompassColor");
        migrateColor(visual,"locatorBorderColor");
    }

    private static void migrateColor(JsonObject visual,String key){
        JsonElement element=visual.get(key);
        if(element!=null&&element.isJsonPrimitive()&&element.getAsJsonPrimitive().isNumber())
            visual.addProperty(key,String.format(Locale.ROOT,"#%08X",0xFF000000|(element.getAsInt()&0xFFFFFF)));
    }
}
//#else
//$$ public final class LocatorConfigMigration { private LocatorConfigMigration(){} }
//#endif

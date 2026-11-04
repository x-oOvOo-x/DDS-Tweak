package ddstweak.platform.malilib;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.util.FileUtils;
//#if MC >= 260200
//$$ import fi.dy.masa.malilib.util.data.json.JsonUtils;
//#else
import fi.dy.masa.malilib.util.JsonUtils;
//#endif
import fi.dy.masa.malilib.util.StringUtils;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;

public final class MalilibCompat {
    private MalilibCompat(){}

    private static String clean(String name){return name.toLowerCase(Locale.ROOT);}
    private static String key(String prefix,String part,String name){return prefix+"."+part+"."+clean(name);}

    public static ConfigBoolean bool(String name,boolean value,String translationPrefix){
        //#if MC > 12006
        return new ConfigBoolean(name,value).apply(translationPrefix);
        //#else
        //$$ return new LegacyTranslatedBoolean(name,value,translationPrefix);
        //#endif
    }

    public static ConfigBooleanHotkeyed boolHotkeyed(String name,boolean value,String hotkey,String translationPrefix){
        //#if MC > 12006
        return new ConfigBooleanHotkeyed(name,value,hotkey).apply(translationPrefix);
        //#else
        //$$ return new LegacyTranslatedBooleanHotkeyed(name,value,hotkey,translationPrefix);
        //#endif
    }

    //#if MC >= 12108
    public static ConfigColor color(String name,String value,String translationPrefix){
        return new ConfigColor(name,value).apply(translationPrefix);
    }
    //#endif

    public static ConfigInteger integer(String name,int value,int min,int max,boolean slider,String translationPrefix){
        //#if MC > 12006
        return new ConfigInteger(name,value,min,max,slider).apply(translationPrefix);
        //#else
        //$$ return new LegacyTranslatedInteger(name,value,min,max,slider,translationPrefix);
        //#endif
    }

    public static ConfigOptionList optionList(String name,IConfigOptionListEntry value,String translationPrefix){
        //#if MC > 12006
        return new ConfigOptionList(name,value).apply(translationPrefix);
        //#else
        //$$ return new LegacyTranslatedOptionList(name,value,translationPrefix);
        //#endif
    }

    public static ConfigStringList stringList(String name,List<String> value,String translationPrefix){
        //#if MC > 12006
        return new ConfigStringList(name,ImmutableList.copyOf(value)).apply(translationPrefix);
        //#else
        //$$ return new LegacyTranslatedStringList(name,ImmutableList.copyOf(value),translationPrefix);
        //#endif
    }

    public static ConfigDouble decimal(String name,double value,double min,double max,boolean slider,String translationPrefix){
        //#if MC > 12006
        return new ConfigDouble(name,value,min,max,slider).apply(translationPrefix);
        //#else
        //$$ return new LegacyTranslatedDouble(name,value,min,max,slider,translationPrefix);
        //#endif
    }

    public static ConfigHotkey hotkey(String name,String value,String translationPrefix){
        //#if MC > 12006
        return new ConfigHotkey(name,value).apply(translationPrefix);
        //#else
        //$$ return new LegacyTranslatedHotkey(name,value,translationPrefix);
        //#endif
    }

    public static Path configDirectory(){
        //#if MC >= 260200
        //$$ return FileUtils.getConfigDirectory();
        //#elseif MC > 12006
        return FileUtils.getConfigDirectoryAsPath();
        //#else
        //$$ return FileUtils.getConfigDirectory().toPath();
        //#endif
    }

    public static JsonElement readJson(Path path){
        //#if MC >= 260200
        //$$ return JsonUtils.parseJsonFile(path);
        //#elseif MC > 12006
        return JsonUtils.parseJsonFileAsPath(path);
        //#else
        //$$ return JsonUtils.parseJsonFile(path.toFile());
        //#endif
    }

    public static void ensureDirectory(Path path){
        try{Files.createDirectories(path);}catch(IOException ignored){}
    }

    //#if MC <= 12006
    //$$ /** Legacy MaLiLib config GUIs call getConfigGuiDisplayName(), not getPrettyName(). */
    //$$ private interface LegacyGuiName extends IConfigBase {
    //$$     @Override default String getConfigGuiDisplayName(){return getPrettyName();}
    //$$ }
    //$$ private static final class LegacyTranslatedBoolean extends ConfigBoolean implements LegacyGuiName {
    //$$     LegacyTranslatedBoolean(String name,boolean value,String prefix){
    //$$         super(name,value,key(prefix,"comment",name),key(prefix,"prettyName",name));
    //$$     }
    //$$ }
    //$$ private static final class LegacyTranslatedBooleanHotkeyed extends ConfigBooleanHotkeyed implements LegacyGuiName {
    //$$     LegacyTranslatedBooleanHotkeyed(String name,boolean value,String hotkey,String prefix){
    //$$         super(name,value,hotkey,key(prefix,"comment",name),key(prefix,"prettyName",name));
    //$$     }
    //$$ }
    //$$ private static final class LegacyTranslatedOptionList extends ConfigOptionList implements LegacyGuiName {
    //$$     LegacyTranslatedOptionList(String name,IConfigOptionListEntry value,String prefix){
    //$$         super(name,value,key(prefix,"comment",name),key(prefix,"prettyName",name));
    //$$     }
    //$$ }
    //$$ private static final class LegacyTranslatedHotkey extends ConfigHotkey implements LegacyGuiName {
    //$$     LegacyTranslatedHotkey(String name,String value,String prefix){
    //$$         super(name,value,key(prefix,"comment",name),key(prefix,"prettyName",name));
    //$$     }
    //$$ }
    //$$ private static final class LegacyTranslatedInteger extends ConfigInteger implements LegacyGuiName {
    //$$     private final String displayKey;
    //$$     LegacyTranslatedInteger(String name,int value,int min,int max,boolean slider,String prefix){
    //$$         super(name,value,min,max,slider,key(prefix,"comment",name));
    //$$         this.displayKey=key(prefix,"prettyName",name);
    //$$     }
    //$$     @Override public String getPrettyName(){return StringUtils.translate(displayKey);}
    //$$ }
    //$$ private static final class LegacyTranslatedDouble extends ConfigDouble implements LegacyGuiName {
    //$$     private final String displayKey;
    //$$     LegacyTranslatedDouble(String name,double value,double min,double max,boolean slider,String prefix){
    //$$         super(name,value,min,max,slider,key(prefix,"comment",name));
    //$$         this.displayKey=key(prefix,"prettyName",name);
    //$$     }
    //$$     @Override public String getPrettyName(){return StringUtils.translate(displayKey);}
    //$$ }
    //$$ private static final class LegacyTranslatedStringList extends ConfigStringList implements LegacyGuiName {
    //$$     private final String displayKey;
    //$$     LegacyTranslatedStringList(String name,ImmutableList<String> value,String prefix){
    //$$         super(name,value,key(prefix,"comment",name));
    //$$         this.displayKey=key(prefix,"prettyName",name);
    //$$     }
    //$$     @Override public String getPrettyName(){return StringUtils.translate(displayKey);}
    //$$ }
    //#endif

    public static void writeJson(JsonObject root,Path path){
        //#if MC >= 260200
        //$$ JsonUtils.writeJsonToFile(root,path);
        //#elseif MC > 12006
        JsonUtils.writeJsonToFileAsPath(root,path);
        //#else
        //$$ JsonUtils.writeJsonToFile(root,path.toFile());
        //#endif
    }
}

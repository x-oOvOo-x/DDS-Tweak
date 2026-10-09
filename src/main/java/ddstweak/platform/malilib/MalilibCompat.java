package ddstweak.platform.malilib;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
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
    private static String tr(String prefix,String part,String name){return StringUtils.translate(prefix+"."+part+"."+clean(name));}

    public static ConfigBoolean bool(String name,boolean value,String translationPrefix){
        //#if MC > 12006
        return new ConfigBoolean(name,value).apply(translationPrefix);
        //#else
        //$$ return new ConfigBoolean(name,value,tr(translationPrefix,"comment",name),tr(translationPrefix,"prettyName",name));
        //#endif
    }

    public static ConfigBooleanHotkeyed boolHotkeyed(String name,boolean value,String hotkey,String translationPrefix){
        //#if MC > 12006
        return new ConfigBooleanHotkeyed(name,value,hotkey).apply(translationPrefix);
        //#else
        //$$ return new ConfigBooleanHotkeyed(name,value,hotkey,tr(translationPrefix,"comment",name),tr(translationPrefix,"prettyName",name));
        //#endif
    }

    public static ConfigInteger integer(String name,int value,int min,int max,boolean slider,String translationPrefix){
        //#if MC > 12006
        return new ConfigInteger(name,value,min,max,slider).apply(translationPrefix);
        //#else
        //$$ return new ConfigInteger(name,value,min,max,slider,tr(translationPrefix,"comment",name));
        //#endif
    }

    public static ConfigOptionList optionList(String name,IConfigOptionListEntry value,String translationPrefix){
        //#if MC > 12006
        return new ConfigOptionList(name,value).apply(translationPrefix);
        //#else
        //$$ return new ConfigOptionList(name,value,tr(translationPrefix,"comment",name),tr(translationPrefix,"prettyName",name));
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
        //$$ return new ConfigDouble(name,value,min,max,slider,tr(translationPrefix,"comment",name));
        //#endif
    }

    public static ConfigHotkey hotkey(String name,String value,String translationPrefix){
        //#if MC > 12006
        return new ConfigHotkey(name,value).apply(translationPrefix);
        //#else
        //$$ return new ConfigHotkey(name,value,tr(translationPrefix,"comment",name),tr(translationPrefix,"prettyName",name));
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
    //$$ private static final class LegacyTranslatedStringList extends ConfigStringList {
    //$$     private final String prettyName;
    //$$     LegacyTranslatedStringList(String name,ImmutableList<String> value,String prefix){
    //$$         super(name,value,tr(prefix,"comment",name));
    //$$         this.prettyName=tr(prefix,"prettyName",name);
    //$$     }
    //$$     @Override public String getPrettyName(){return prettyName;}
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

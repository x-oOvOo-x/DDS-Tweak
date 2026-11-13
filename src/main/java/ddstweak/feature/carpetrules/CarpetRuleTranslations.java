package ddstweak.feature.carpetrules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** UI translations are advisory and may not match a remote server's mod version. */
final class CarpetRuleTranslations {
    private CarpetRuleTranslations() {}

    static String language(){
        //#if MC >= 11600 && MC < 11700
        // Old Carpet's /carpet language is independent of the client language.
        // Prefer that effective locale for Carpet and CDA rule translations.
        //$$ try{
        //$$     Object configured=Class.forName("carpet.CarpetSettings")
        //$$             .getField("language").get(null);
        //$$     if(configured instanceof String){
        //$$         String code=(String)configured;
        //$$         if(code.matches("[a-z]{2}_[a-z]{2}"))return code;
        //$$     }
        //$$ }catch(ReflectiveOperationException|LinkageError ignored){}
        //$$ return validLanguage(net.minecraft.client.Minecraft.getInstance().options.languageCode);
        //#else
        try{
            // The return type changed from a language object to String across MC versions.
            // Object accepts both APIs, so the preprocess root can compile without both branches.
            Object selected=net.minecraft.client.Minecraft.getInstance()
                    .getLanguageManager().getSelected();
            return languageCode(selected);
        }catch(RuntimeException e){
            return "en_us";
        }
        //#endif
    }

    static String languageCode(Object selected){
        if(selected instanceof String)return validLanguage((String)selected);
        if(selected==null)return "en_us";
        // Avoid reflecting by a mapped method name: production remapping does not rewrite strings.
        for(Method method:selected.getClass().getMethods()){
            if(method.getParameterCount()!=0||method.getReturnType()!=String.class)continue;
            try{
                Object value=method.invoke(selected);
                if(value instanceof String){
                    String code=validLanguage((String)value);
                    if(!"en_us".equals(code)||"en_us".equals(value))return code;
                }
            }catch(ReflectiveOperationException|RuntimeException ignored){}
        }
        return "en_us";
    }

    private static String validLanguage(String value){
        return value!=null&&value.matches("[a-z]{2}_[a-z]{2}")?value:"en_us";
    }

    static Map<String,String> load(String language, Iterable<?> extensions){
        Map<String,String> all=new HashMap<>();
        String safe=language.matches("[a-z]{2}_[a-z]{2}")?language:"en_us";
        try(InputStream file=openCarpetLanguage("assets/carpet/lang/"+safe+".json")){
            if(file!=null){
                JsonObject data=new GsonBuilder().setLenient().create()
                        .fromJson(new InputStreamReader(file,StandardCharsets.UTF_8),JsonObject.class);
                if(data!=null)for(Map.Entry<String,JsonElement> e:data.entrySet())
                    if(e.getValue().isJsonPrimitive()&&e.getValue().getAsJsonPrimitive().isString())
                        all.put(qualifiedKey(e.getKey(),"carpet"),e.getValue().getAsString());
            }
        }catch(Exception ignored){}
        if(extensions!=null)for(Object ext:extensions){
            try {
                // Invoke the public Carpet API, not a potentially non-public extension implementation.
                Class<?> api=Class.forName("carpet.CarpetExtension");
                Object data=api.getMethod("canHasTranslations",String.class).invoke(ext,safe);
                if(!(data instanceof Map))continue;
                String manager=extensionManagerId(api,ext);
                for(Map.Entry<?,?> e:((Map<?,?>)data).entrySet()){
                    if(!(e.getKey() instanceof String)||!(e.getValue() instanceof String))continue;
                    String key=(String)e.getKey(),value=(String)e.getValue();
                    key=qualifiedKey(key,manager);
                    if(key!=null)all.putIfAbsent(key,value);
                }
            }catch(ReflectiveOperationException|LinkageError|RuntimeException ignored){}
        }
        return all;
    }

    /** Legacy 1.15–1.18 Carpet maps are unqualified; keep addon keys scoped. */
    static String qualifiedKey(String key,String manager){
        if(key.startsWith("rule.")||key.startsWith("category."))
            return manager==null?null:manager+"."+key;
        return key;
    }

    private static String extensionManagerId(Class<?> api,Object extension){
        for(String accessor:new String[]{"extensionSettingsManager","customSettingsManager"}){
            try{
                Object manager=api.getMethod(accessor).invoke(extension);
                if(manager==null)continue;
                for(String method:new String[]{"identifier","getIdentifier"}){
                    try{
                        Object id=manager.getClass().getMethod(method).invoke(manager);
                        if(id instanceof String&&!((String)id).isEmpty())return (String)id;
                    }catch(NoSuchMethodException ignored){}
                }
            }catch(ReflectiveOperationException|RuntimeException ignored){}
        }
        return null;
    }

    /**
     * Fabric's ModContainer lookup is authoritative for another mod's resources.
     * Keep the classloader fallback for older Fabric Loader implementations.
     */
    private static InputStream openCarpetLanguage(String resource){
        try{
            Object container=FabricLoader.getInstance().getModContainer("carpet").orElse(null);
            if(container!=null){
                Class<?> api=Class.forName("net.fabricmc.loader.api.ModContainer");
                Object found=api.getMethod("findPath",String.class).invoke(container,resource);
                if(found instanceof Optional){
                    Object path=((Optional<?>)found).orElse(null);
                    if(path instanceof Path)return Files.newInputStream((Path)path);
                }
            }
        }catch(Exception ignored){}
        return CarpetRuleTranslations.class.getClassLoader().getResourceAsStream(resource);
    }

    static String name(Map<String,String> map,String manager,String rule){
        return map.getOrDefault(manager+".rule."+rule+".name",rule);
    }

    static String description(Map<String,String> map,String manager,String rule){
        return map.getOrDefault(manager+".rule."+rule+".desc","");
    }
}

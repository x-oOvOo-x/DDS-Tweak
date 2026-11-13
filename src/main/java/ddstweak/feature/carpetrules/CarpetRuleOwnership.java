package ddstweak.feature.carpetrules;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Conservative integrated-server-only attribution. Legacy Carpet ParsedRule
 * exposes its defining Field; a unique matching Fabric mod jar can identify
 * the owner. Manager IDs and client-installed mods never establish remote
 * server ownership.
 */
final class CarpetRuleOwnership {
    private static final Map<Class<?>,String> CACHE=new HashMap<>();
    private CarpetRuleOwnership(){}

    static String owner(Object rule){
        try {
            Class<?> definition=null;
            try{
                Object value=rule.getClass().getField("field").get(rule);
                if(value instanceof Field)definition=((Field)value).getDeclaringClass();
            }catch(ReflectiveOperationException ignored){}
            if(definition==null){
                // A custom CarpetRule implementation may belong to an addon
                // without exposing ParsedRule.field. Verify its concrete class.
                definition=rule.getClass();
                if(definition.getName().startsWith("carpet."))return null;
            }
            String cached=CACHE.get(definition);
            if(cached!=null)return cached.isEmpty()?null:cached;
            String found=findUniqueOwner(definition);
            CACHE.put(definition,found==null?"":found);
            return found;
        }catch(SecurityException|LinkageError ex){
            return null;
        }
    }

    /** Read public Carpet API declarations even when the rule implementation is not public. */
    static Object invokeRule(Object rule,String method) throws ReflectiveOperationException {
        for(Class<?> type=rule.getClass();type!=null;type=type.getSuperclass()){
            for(Class<?> api:type.getInterfaces()){
                if(!Modifier.isPublic(api.getModifiers()))continue;
                try{return api.getMethod(method).invoke(rule);}
                catch(NoSuchMethodException ignored){}
            }
            if(Modifier.isPublic(type.getModifiers())){
                try{return type.getMethod(method).invoke(rule);}
                catch(NoSuchMethodException ignored){}
            }
        }
        throw new NoSuchMethodException(method);
    }

    private static String findUniqueOwner(Class<?> definition){
        Path source=null;
        try {
            CodeSource code=definition.getProtectionDomain().getCodeSource();
            URL location=code==null?null:code.getLocation();
            if(location!=null&&"file".equals(location.getProtocol()))
                source=Paths.get(location.toURI()).toAbsolutePath().normalize();
        }catch(Exception ignored){}
        String owner=null;
        for(ModContainer mod:FabricLoader.getInstance().getAllMods()){
            if(!belongsTo(definition,source,mod))continue;
            if(owner!=null)return null; // ambiguous jar/class ownership
            owner=mod.getMetadata().getId();
        }
        return owner;
    }

    private static boolean belongsTo(Class<?> definition,Path source,ModContainer mod){
        if(source!=null){
            try {
                Method getOrigin=mod.getClass().getMethod("getOrigin");
                Object origin=getOrigin.invoke(mod);
                Object roots=origin.getClass().getMethod("getPaths").invoke(origin);
                if(roots instanceof Collection)for(Object root:(Collection<?>)roots)
                    if(root instanceof Path&&source.equals(((Path)root).toAbsolutePath().normalize()))
                        return true;
            }catch(ReflectiveOperationException|RuntimeException ignored){}
        }
        // A unique mod resource is a fallback when the classloader hides CodeSource.
        try {
            Method find=mod.getClass().getMethod("findPath",String.class);
            Object resource=find.invoke(mod,definition.getName().replace('.','/')+".class");
            return resource instanceof Optional&&((Optional<?>)resource).isPresent();
        }catch(ReflectiveOperationException|RuntimeException ignored){
            return false;
        }
    }
}

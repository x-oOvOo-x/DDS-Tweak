package ddstweak.feature.carpetrules;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
//#if MC >= 11601
import net.minecraft.world.level.storage.LevelResource;
//#endif
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only integrated-world persisted Carpet rules. Every registered manager
 * uses <manager>.conf; when missing, Carpet can use config/carpet/default_<manager>.conf.
 * Remote servers never expose their files here; unreadable managers remain unknown.
 */
final class CarpetRuleDefaultConfig {
    private static final int MAX_BYTES=256*1024;
    private CarpetRuleDefaultConfig(){}

    static final class ReadResult {
        private final Map<CarpetRuleCatalog.RuleKey,String> values;
        private final Set<CarpetRuleCatalog.RuleKey> rules;
        private final Set<String> knownManagers;
        private final boolean known;

        ReadResult(Map<CarpetRuleCatalog.RuleKey,String> values,
                   Set<String> knownManagers,boolean known){
            this.values=Collections.unmodifiableMap(new HashMap<>(values));
            this.rules=Collections.unmodifiableSet(new HashSet<>(values.keySet()));
            this.knownManagers=Collections.unmodifiableSet(new HashSet<>(knownManagers));
            this.known=known;
        }
        Map<CarpetRuleCatalog.RuleKey,String> values(){return values;}
        Set<CarpetRuleCatalog.RuleKey> rules(){return rules;}
        Set<String> knownManagers(){return knownManagers;}
        boolean known(){return known;}
    }

    static ReadResult readWorld(MinecraftServer server,
                                List<CarpetRuleCatalog.RemoteRule> registered){
        try{
            Path world=null;
            // Use the modern mapped path, or ask legacy Carpet where it reads its config.
            // 旧版通过 Carpet 自身的配置文件定位逻辑获取存档目录，避免映射名称差异。
            //#if MC >= 11601
            world=server.getWorldPath(LevelResource.ROOT);
            //#else
            //$$ world=legacyWorldDirectory();
            //#endif
            if(world==null)return unknown();
            Path defaults=FabricLoader.getInstance().getConfigDir().resolve("carpet");
            return readManagers(world,defaults,registered);
        }catch(RuntimeException ex){
            return unknown();
        }
    }


    /**
     * Resolve the exact save directory used by Carpet 1.14/1.15 itself.
     * 通过旧版 Carpet 的 SettingsManager.getFile() 定位配置文件，不依赖旧版 MinecraftServer 映射。
     */
    private static Path legacyWorldDirectory(){
        try{
            Class<?> carpet=Class.forName("carpet.CarpetServer");
            return managerConfigDirectory(carpet.getField("settingsManager").get(null));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ignored){
            return null;
        }
    }

    /** Also tested with a manager exposing a private getFile() method. */
    static Path managerConfigDirectory(Object manager){
        if(manager==null)return null;
        for(Class<?> type=manager.getClass();type!=null;type=type.getSuperclass()){
            try{
                Method method=type.getDeclaredMethod("getFile");
                method.setAccessible(true);
                Object file=method.invoke(manager);
                return file instanceof File?((File)file).toPath().getParent():null;
            }catch(NoSuchMethodException ignored){
                // Look for the method in a parent implementation.
            }catch(ReflectiveOperationException|RuntimeException ignored){
                return null;
            }
        }
        return null;
    }

    /** Scan only managers actually registered by Carpet on the integrated server. */
    static ReadResult readManagers(Path world,Path fallbackDir,
                                   List<CarpetRuleCatalog.RemoteRule> registered){
        Set<String> managers=new HashSet<>();
        for(CarpetRuleCatalog.RemoteRule rule:registered)managers.add(rule.key().manager());
        Set<String> known=new HashSet<>();
        Map<CarpetRuleCatalog.RuleKey,String> values=new HashMap<>();
        for(String manager:managers){
            // Identifiers are file basenames, not arbitrary filesystem paths.
            if(!manager.matches("[A-Za-z0-9_.-]{1,128}"))continue;
            Path local=world.resolve(manager+".conf");
            Path fallback=fallbackDir.resolve("default_"+manager+".conf");
            try{
                // Files.exists() alone also returns false on unknown access state.
                if(!Files.exists(local)&&!Files.notExists(local))continue;
                Path source=Files.exists(local)?local:fallback;
                ReadResult result=readManager(source,registered,manager);
                if(result.known()){
                    known.add(manager);
                    values.putAll(result.values());
                }
            }catch(SecurityException ignored){
                // Unknown is safer than asserting that this manager has no defaults.
            }
        }
        return new ReadResult(values,known,known.containsAll(managers));
    }

    /** Kept for callers and tests covering the core Carpet config. */
    static ReadResult read(Path config,List<CarpetRuleCatalog.RemoteRule> registered){
        return readManager(config,registered,"carpet");
    }

    private static ReadResult readManager(Path config,
                    List<CarpetRuleCatalog.RemoteRule> registered,String manager){
        try{
            if(Files.notExists(config))
                return new ReadResult(Collections.emptyMap(),Collections.singleton(manager),true);
            if(!Files.isRegularFile(config)||Files.size(config)>MAX_BYTES)return unknown();
            return parseManager(Files.readAllLines(config,StandardCharsets.UTF_8),
                    registered,manager);
        }catch(IOException|SecurityException ex){
            return unknown();
        }
    }

    static ReadResult parse(List<String> lines,List<CarpetRuleCatalog.RemoteRule> registered){
        return parseManager(lines,registered,"carpet");
    }

    /** Parse with the same split/comment/duplicate semantics as Carpet's config reader. */
    private static ReadResult parseManager(List<String> lines,
                    List<CarpetRuleCatalog.RemoteRule> registered,String manager){
        Set<CarpetRuleCatalog.RuleKey> available=new HashSet<>();
        for(CarpetRuleCatalog.RemoteRule rule:registered)
            if(manager.equals(rule.key().manager()))available.add(rule.key());
        Map<CarpetRuleCatalog.RuleKey,String> values=new HashMap<>();
        for(String raw:lines){
            String line=raw.replaceAll("[\\r\\n]","");
            if("locked".equalsIgnoreCase(line))continue;
            String[] fields=line.split("\\s+",2);
            if(fields.length<2||(values.isEmpty()&&fields[0].startsWith("#"))
                    ||fields[1].startsWith("#"))continue;
            CarpetRuleCatalog.RuleKey key;
            try{key=new CarpetRuleCatalog.RuleKey(manager,fields[0]);}
            catch(IllegalArgumentException ex){continue;}
            if(available.contains(key))values.put(key,fields[1]);
        }
        return new ReadResult(values,Collections.singleton(manager),true);
    }

    private static ReadResult unknown(){
        return new ReadResult(Collections.emptyMap(),Collections.emptySet(),false);
    }
}

package ddstweak.feature.carpetrules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ddstweak.config.DdsTweakConfigPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline presets and server snapshots. Saved values never become live server
 * facts and may only be applied after matching the connected server's catalog.
 */
public final class CarpetRulePresetStore {
    private static final int MAX_PRESETS=128, MAX_RULES=4096, MAX_BYTES=4*1024*1024;
    private final Map<String, Preset> saved=new LinkedHashMap<>();

    public Collection<Preset> presets(){
        return Collections.unmodifiableCollection(new ArrayList<>(saved.values()));
    }

    /** User-editable plans only; historical snapshots stay intact in storage. */
    public Collection<Preset> editablePlans(){
        List<Preset> plans=new ArrayList<>();
        for(Preset preset:saved.values())if(!preset.snapshot)plans.add(preset);
        return Collections.unmodifiableList(plans);
    }

    public Preset get(String name){return saved.get(name);}

    public boolean delete(String name){return saved.remove(name)!=null;}
    public boolean rename(String oldName,String name){
        Preset old=saved.get(oldName);
        if(old==null||!validName(name)||saved.containsKey(name))return false;
        Map<String,Preset> renamed=new LinkedHashMap<>();
        for(Preset preset:saved.values())
            renamed.put(preset.name.equals(oldName)?name:preset.name,
                    preset.name.equals(oldName)?new Preset(name,preset.source,
                            preset.createdAt,preset.snapshot,preset.applyMode,
                            new LinkedHashMap<>(preset.values)):preset);
        saved.clear();
        saved.putAll(renamed);
        return true;
    }
    public boolean setApplyMode(String name,CarpetRuleCommands.Mode mode){
        Preset preset=saved.get(name);
        if(preset==null||preset.snapshot||!supportedMode(mode))return false;
        saved.put(name,new Preset(preset.name,preset.source,preset.createdAt,
                false,mode,new LinkedHashMap<>(preset.values)));
        return true;
    }
    private static boolean supportedMode(CarpetRuleCommands.Mode mode){
        return mode==CarpetRuleCommands.Mode.TEMPORARY
                ||mode==CarpetRuleCommands.Mode.SET_DEFAULT;
    }

    public boolean setTarget(String name, CarpetRuleCatalog.RuleKey key, String value) {
        Preset preset=saved.get(name);
        if(preset==null||preset.snapshot||key==null||value==null||value.length()>2048)return false;
        try {
            CarpetRuleCommands.build(key,CarpetRuleCommands.Mode.TEMPORARY,value);
        }catch(IllegalArgumentException ex){return false;}
        Map<CarpetRuleCatalog.RuleKey,String> changed=new LinkedHashMap<>(preset.values);
        if(!changed.containsKey(key)&&changed.size()>=MAX_RULES)return false;
        changed.put(key,value);
        saved.put(name,new Preset(preset.name,preset.source,preset.createdAt,false,preset.applyMode,changed));
        return true;
    }

    public boolean removeTarget(String name,CarpetRuleCatalog.RuleKey key) {
        Preset preset=saved.get(name);
        if(preset==null||preset.snapshot||!preset.values.containsKey(key))return false;
        Map<CarpetRuleCatalog.RuleKey,String> changed=new LinkedHashMap<>(preset.values);
        changed.remove(key);
        saved.put(name,new Preset(preset.name,preset.source,preset.createdAt,false,preset.applyMode,changed));
        return true;
    }

    public boolean createSnapshot(String name,String source,CarpetRuleCatalog catalog,long createdAt){
        if(!catalog.isReady())return false;
        Map<CarpetRuleCatalog.RuleKey,String> values=new LinkedHashMap<>();
        for(CarpetRuleCatalog.RemoteRule rule:catalog.snapshot())
            values.put(rule.key(),rule.value());
        return put(name,source,createdAt,true,values);
    }

    public boolean put(String name,String source,long createdAt,boolean snapshot,
                       Map<CarpetRuleCatalog.RuleKey,String> values){
        return put(name,source,createdAt,snapshot,CarpetRuleCommands.Mode.TEMPORARY,values);
    }
    public boolean put(String name,String source,long createdAt,boolean snapshot,
                       CarpetRuleCommands.Mode mode,
                       Map<CarpetRuleCatalog.RuleKey,String> values){
        if(!supportedMode(mode)||!validName(name)||source==null||source.length()>256||values==null||
                values.size()>MAX_RULES||saved.containsKey(name)||saved.size()>=MAX_PRESETS)return false;
        Map<CarpetRuleCatalog.RuleKey,String> copy=new LinkedHashMap<>();
        for(Map.Entry<CarpetRuleCatalog.RuleKey,String> e:values.entrySet()){
            if(e.getKey()==null||e.getValue()==null||e.getValue().length()>2048)return false;
            copy.put(e.getKey(),e.getValue());
        }
        saved.put(name,new Preset(name,source,createdAt,snapshot,mode,copy));
        return true;
    }

    public List<Diff> compare(CarpetRuleCatalog catalog,String name){
        Preset preset=saved.get(name);
        if(preset==null||!catalog.isReady())return Collections.emptyList();
        List<Diff> result=new ArrayList<>();
        for(Map.Entry<CarpetRuleCatalog.RuleKey,String> e:preset.values.entrySet()){
            CarpetRuleCatalog.RemoteRule current=catalog.get(e.getKey());
            DiffStatus status=current==null?DiffStatus.MISSING:
                    current.value().equals(e.getValue())?DiffStatus.MATCHED:DiffStatus.DIFFERENT;
            result.add(new Diff(e.getKey(),current==null?null:current.value(),e.getValue(),status));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Atomic import: malformed entries, duplicate names, collisions, or
     * exhausted storage reject the entire transfer without mutating presets.
     * Returns the number imported; -1 means failure.
     */
    public int importJson(String json) {
        if (json==null || json.length()>MAX_BYTES) return -1;
        try {
            JsonElement parsed=new JsonParser().parse(json);
            if(!parsed.isJsonObject())return -1;
            JsonObject root=parsed.getAsJsonObject();
            if(!root.has("version") || (root.get("version").getAsInt()!=1&&root.get("version").getAsInt()!=2)
                    ||!root.has("presets") ||!root.get("presets").isJsonArray())return -1;
            int count=root.getAsJsonArray("presets").size();
            CarpetRulePresetStore imported=parse(json);
            if(imported.saved.size()!=count || saved.size()+count>MAX_PRESETS)return -1;
            for(String name:imported.saved.keySet())
                if(saved.containsKey(name))return -1;
            saved.putAll(imported.saved);
            return count;
        }catch(RuntimeException ignored){
            return -1;
        }
    }

    /** Export only one editable non-empty plan; never leak snapshots or other plans. */
    public String exportJson(String name){
        Preset preset=saved.get(name);
        if(preset==null||preset.snapshot||preset.values.isEmpty())return null;
        return toJson(Collections.singletonList(preset));
    }

    public String toJson(){return toJson(saved.values());}

    private static String toJson(Collection<Preset> selection){
        JsonObject root=new JsonObject();
        root.addProperty("version",2);
        JsonArray presets=new JsonArray();
        for(Preset p:selection){
            JsonObject entry=new JsonObject();
            entry.addProperty("name",p.name);
            entry.addProperty("source",p.source);
            entry.addProperty("createdAt",p.createdAt);
            entry.addProperty("snapshot",p.snapshot);
            entry.addProperty("applyMode",p.applyMode.name());
            JsonArray rules=new JsonArray();
            for(Map.Entry<CarpetRuleCatalog.RuleKey,String> r:p.values.entrySet()){
                JsonObject rule=new JsonObject();
                rule.addProperty("manager",r.getKey().manager());
                rule.addProperty("rule",r.getKey().name());
                rule.addProperty("value",r.getValue());
                rules.add(rule);
            }
            entry.add("rules",rules);
            presets.add(entry);
        }
        root.add("presets",presets);
        return root.toString();
    }

    public static CarpetRulePresetStore parse(String json){
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        if(json==null||json.length()>MAX_BYTES)return store;
        try{
            JsonElement parsed=new JsonParser().parse(json);
            if(!parsed.isJsonObject())return store;
            JsonObject root=parsed.getAsJsonObject();
            if(!root.has("version")||(root.get("version").getAsInt()!=1
                &&root.get("version").getAsInt()!=2))return store;
            JsonElement entries=root.get("presets");
            if(entries==null||!entries.isJsonArray())return store;
            for(JsonElement entry:entries.getAsJsonArray()){
                if(store.saved.size()>=MAX_PRESETS)break;
                try{
                    if(!entry.isJsonObject())continue;
                    JsonObject p=entry.getAsJsonObject();
                    String name=p.get("name").getAsString(),source=p.get("source").getAsString();
                    long created=p.get("createdAt").getAsLong();
                    boolean snapshot=p.get("snapshot").getAsBoolean();
                    CarpetRuleCommands.Mode mode=CarpetRuleCommands.Mode.TEMPORARY;
                    if(p.has("applyMode")){
                        mode=CarpetRuleCommands.Mode.valueOf(p.get("applyMode").getAsString());
                        if(!supportedMode(mode))continue;
                    }
                    JsonElement items=p.get("rules");
                    if(items==null||!items.isJsonArray()||items.getAsJsonArray().size()>MAX_RULES)continue;
                    Map<CarpetRuleCatalog.RuleKey,String> values=new LinkedHashMap<>();
                    boolean invalid=false;
                    for(JsonElement item:items.getAsJsonArray()){
                        try{
                            JsonObject rule=item.getAsJsonObject();
                            String manager=rule.get("manager").getAsString(),key=rule.get("rule").getAsString();
                            String value=rule.get("value").getAsString();
                            if(!validId(manager)||!validId(key)||value.length()>2048){invalid=true;break;}
                            CarpetRuleCatalog.RuleKey identity=new CarpetRuleCatalog.RuleKey(manager,key);
                            if(values.put(identity,value)!=null){invalid=true;break;}
                        }catch(RuntimeException ex){invalid=true;break;}
                    }
                    if(!invalid)store.put(name,source,created,snapshot,mode,values);
                }catch(RuntimeException ignored){
                    // ignore a damaged preset without deleting other intact entries
                }
            }
        }catch(RuntimeException ignored){
            return new CarpetRulePresetStore();
        }
        return store;
    }

    private static boolean validName(String name){
        if(name==null||name.length()==0||name.length()>64||!name.trim().equals(name))return false;
        for(int i=0;i<name.length();i++)if(Character.isISOControl(name.charAt(i)))return false;
        return true;
    }

    private static boolean validId(String value){
        if(value==null||value.isEmpty()||value.length()>128)return false;
        for(int i=0;i<value.length();i++)
            if(Character.isISOControl(value.charAt(i))||Character.isWhitespace(value.charAt(i)))return false;
        return true;
    }

    public static CarpetRulePresetStore load(){
        try{
            Path path=DdsTweakConfigPaths.carpetRulePresets();
            if(!Files.isRegularFile(path)||Files.size(path)>MAX_BYTES)return new CarpetRulePresetStore();
            return parse(new String(Files.readAllBytes(path),StandardCharsets.UTF_8));
        }catch(IOException|RuntimeException ignored){return new CarpetRulePresetStore();}
    }

    public boolean save(){
        try{
            Path path=DdsTweakConfigPaths.carpetRulePresets();
            Path temp=path.resolveSibling(path.getFileName()+".tmp");
            Files.createDirectories(path.getParent());
            byte[] bytes=toJson().getBytes(StandardCharsets.UTF_8);
            if(bytes.length>MAX_BYTES)return false;
            Files.write(temp,bytes);
            try{
                Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            }catch(java.nio.file.AtomicMoveNotSupportedException ignored){
                Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        }catch(IOException|RuntimeException ignored){return false;}
    }

    public enum DiffStatus { MISSING, MATCHED, DIFFERENT }

    public static final class Diff{
        private final CarpetRuleCatalog.RuleKey key;
        private final String current,target;
        private final DiffStatus status;
        private Diff(CarpetRuleCatalog.RuleKey key,String current,String target,DiffStatus status){
            this.key=key;this.current=current;this.target=target;this.status=status;
        }
        public CarpetRuleCatalog.RuleKey key(){return key;}
        public String current(){return current;}
        public String target(){return target;}
        public DiffStatus status(){return status;}
    }

    public static final class Preset{
        private final String name,source;
        private final long createdAt;
        private final boolean snapshot;
        private final CarpetRuleCommands.Mode applyMode;
        private final Map<CarpetRuleCatalog.RuleKey,String> values;
        private Preset(String name,String source,long createdAt,boolean snapshot,
                       CarpetRuleCommands.Mode mode,Map<CarpetRuleCatalog.RuleKey,String> values){
            this.name=name;this.source=source;this.createdAt=createdAt;
            this.snapshot=snapshot;this.applyMode=mode;
            this.values=Collections.unmodifiableMap(values);
        }
        public String name(){return name;}
        public String source(){return source;}
        public long createdAt(){return createdAt;}
        public boolean isSnapshot(){return snapshot;}
        public CarpetRuleCommands.Mode applyMode(){return applyMode;}
        public Map<CarpetRuleCatalog.RuleKey,String> values(){return values;}
    }
}

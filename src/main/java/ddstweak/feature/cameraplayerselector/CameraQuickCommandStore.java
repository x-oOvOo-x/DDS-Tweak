package ddstweak.feature.cameraplayerselector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ddstweak.config.DdsTweakConfigPaths;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Global quick commands plus independently stored current-server/world commands. */
final class CameraQuickCommandStore {
    private static final String PRESETS_INITIALIZED="presetsInitialized";
    enum Scope { GLOBAL, LOCAL }

    private CameraQuickCommandStore(){}

    static final class Entry {
        final String id,name,command;
        final Scope scope;

        Entry(String id,String name,String command){
            this(id,name,command,Scope.GLOBAL);
        }

        Entry(String id,String name,String command,Scope scope){
            this.id=id==null||id.trim().isEmpty()?UUID.randomUUID().toString():id.trim();
            this.name=name==null?"":name.trim();
            this.command=command==null?"":command;
            this.scope=scope==null?Scope.GLOBAL:scope;
        }
    }

    static String validate(String name,String command){
        if(name==null||name.trim().isEmpty()||name.length()>64)return "name";
        if(command==null||command.trim().isEmpty()||command.length()>256)return "command";
        for(int i=0;i<command.length();i++)if(Character.isISOControl(command.charAt(i)))return "command";
        String value=command.trim();
        if(value.startsWith("/"))value=value.substring(1);
        if(value.isEmpty()||value.charAt(0)=='/'||value.indexOf(';')>=0)return "command";
        return CameraCommandSyntax.validRoot(command.trim())?null:"command";
    }

    static String commandToSend(Entry entry){
        if(entry==null||validate(entry.name,entry.command)!=null)return null;
        String value=entry.command.trim();
        return value.startsWith("/")?value.substring(1):value;
    }

    static boolean hasLocalScope(Minecraft minecraft){
        return !CameraCommandProfileStore.serverKey(minecraft).isEmpty();
    }

    /** Read both scopes, retaining each entry's origin for precise edit and delete. */
    static synchronized List<Entry> load(Minecraft minecraft){
        JsonObject root=read();
        if(ensurePresets(root))write(root);
        return Collections.unmodifiableList(merge(root,CameraCommandProfileStore.serverKey(minecraft)));
    }

    /** Edit an entry, optionally moving it between scopes without rewriting other servers. */
    static synchronized void upsert(Minecraft minecraft,Entry entry,Scope previousScope){
        if(entry==null||validate(entry.name,entry.command)!=null)return;
        String key=CameraCommandProfileStore.serverKey(minecraft);
        Scope original=previousScope==null?entry.scope:previousScope;
        if(entry.scope==Scope.LOCAL||original==Scope.LOCAL)
            CameraCommandProfileStore.requireServerIdentity(key,key);
        JsonObject root=read();
        ensurePresets(root);
        upsertIn(root,key,entry,original);
        write(root);
    }

    static synchronized void upsert(Minecraft minecraft,Entry entry){
        upsert(minecraft,entry,entry==null?Scope.GLOBAL:entry.scope);
    }

    static synchronized void delete(Minecraft minecraft,String id,Scope scope){
        if(id==null||id.isEmpty()||scope==null)return;
        String key=CameraCommandProfileStore.serverKey(minecraft);
        if(scope==Scope.LOCAL)CameraCommandProfileStore.requireServerIdentity(key,key);
        JsonObject root=read();
        ensurePresets(root);
        if(deleteFrom(root,key,id,scope))write(root);
    }

    static List<Entry> merge(JsonObject root,String key){
        List<Entry> result=new ArrayList<>(decode(array(root,"global"),Scope.GLOBAL));
        if(key!=null&&!key.isEmpty())
            result.addAll(decode(array(object(root,"servers"),key),Scope.LOCAL));
        return result;
    }

    static void upsertIn(JsonObject root,String key,Entry entry,Scope previousScope){
        List<Entry> original=readScope(root,key,previousScope);
        original.removeIf(value->value.id.equals(entry.id));
        if(previousScope!=entry.scope)putScope(root,key,previousScope,original);
        List<Entry> target=previousScope==entry.scope?original:readScope(root,key,entry.scope);
        target.removeIf(value->value.id.equals(entry.id));
        target.add(entry);
        putScope(root,key,entry.scope,target);
    }

    static boolean deleteFrom(JsonObject root,String key,String id,Scope scope){
        List<Entry> entries=readScope(root,key,scope);
        if(!entries.removeIf(value->value.id.equals(id)))return false;
        putScope(root,key,scope,entries);
        return true;
    }

    private static List<Entry> readScope(JsonObject root,String key,Scope scope){
        return new ArrayList<>(decode(scope==Scope.GLOBAL
                ?array(root,"global"):array(object(root,"servers"),key),scope));
    }

    private static void putScope(JsonObject root,String key,Scope scope,List<Entry> entries){
        JsonArray array=new JsonArray();
        for(Entry entry:entries){
            if(validate(entry.name,entry.command)!=null)continue;
            JsonObject json=new JsonObject();
            json.addProperty("id",entry.id);
            json.addProperty("name",entry.name);
            json.addProperty("command",entry.command);
            array.add(json);
        }
        if(scope==Scope.GLOBAL)root.add("global",array);
        else{
            JsonObject servers=object(root,"servers");
            if(servers==null)servers=new JsonObject();
            servers.add(key,array);
            root.add("servers",servers);
        }
    }

    /** One-time migration: never reseed presets after the user deletes or edits them. */
    private static boolean ensurePresets(JsonObject root){
        String[] labels=new String[3];
        String[] servers={"survival","creative","mirror"};
        for(int i=0;i<servers.length;i++)
            labels[i]=MinecraftCompat.translatable("dds-tweak.camera.quick.preset."+servers[i]).getString();
        return ensurePresets(root,labels);
    }

    static boolean ensurePresets(JsonObject root,String[] labels){
        JsonElement initialized=root.get(PRESETS_INITIALIZED);
        if(initialized!=null&&initialized.isJsonPrimitive()){
            try{if(initialized.getAsBoolean())return false;}
            catch(RuntimeException ignored){}
        }
        List<Entry> global=readScope(root,"",Scope.GLOBAL);
        String[] servers={"survival","creative","mirror"};
        for(int i=0;i<servers.length;i++){
            String id=servers[i],command="/server "+id;
            boolean exists=false;
            for(Entry entry:global)
                if(command.equalsIgnoreCase(entry.command.trim())||("preset-"+id).equals(entry.id)){
                    exists=true;break;
                }
            if(!exists)global.add(new Entry("preset-"+id,labels[i],command,Scope.GLOBAL));
        }
        putScope(root,"",Scope.GLOBAL,global);
        root.addProperty(PRESETS_INITIALIZED,true);
        return true;
    }

    private static List<Entry> decode(JsonArray values,Scope scope){
        if(values==null)return Collections.emptyList();
        List<Entry> result=new ArrayList<>();
        for(JsonElement element:values){
            if(!element.isJsonObject())
                throw new IllegalStateException("Invalid quick command entry; original JSON kept");
            JsonObject obj=element.getAsJsonObject();
            String id=string(obj,"id"),name=string(obj,"name"),command=string(obj,"command");
            if(id.isEmpty()||validate(name,command)!=null)
                throw new IllegalStateException("Invalid quick command entry; original JSON kept");
            result.add(new Entry(id,name,command,scope));
        }
        return result;
    }

    private static String string(JsonObject obj,String key){
        JsonElement element=obj.get(key);
        return element!=null&&element.isJsonPrimitive()?element.getAsString():"";
    }

    private static JsonArray array(JsonObject obj,String key){
        if(obj==null)return null;
        JsonElement value=obj.get(key);
        if(value!=null&&!value.isJsonArray())
            throw new IllegalStateException("Invalid quick command array: "+key);
        return value==null?null:value.getAsJsonArray();
    }

    private static JsonObject read(){
        JsonObject root=CameraConfigFiles.read(DdsTweakConfigPaths.cameraQuickCommands());
        decode(array(root,"global"),Scope.GLOBAL);
        JsonObject servers=object(root,"servers");
        if(servers!=null)
            for(java.util.Map.Entry<String,JsonElement> server:servers.entrySet())
                decode(array(servers,server.getKey()),Scope.LOCAL);
        return root;
    }

    private static JsonObject object(JsonObject obj,String key){
        if(obj==null)return null;
        JsonElement value=obj.get(key);
        if(value!=null&&!value.isJsonObject())
            throw new IllegalStateException("Invalid quick command group: "+key);
        return value==null?null:value.getAsJsonObject();
    }

    private static void write(JsonObject root){
        root.addProperty("schema",2);
        Path path=DdsTweakConfigPaths.cameraQuickCommands();
        CameraConfigFiles.write(path,root);
    }
}

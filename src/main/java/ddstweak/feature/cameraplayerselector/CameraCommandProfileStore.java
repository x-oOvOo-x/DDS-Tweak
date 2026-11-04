package ddstweak.feature.cameraplayerselector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ddstweak.config.DdsTweakConfigPaths;
import net.minecraft.client.Minecraft;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Global defaults plus independently persisted server/world overrides. */
final class CameraCommandProfileStore {
    private CameraCommandProfileStore(){}

    static String serverKey(Minecraft minecraft) {
        if (minecraft==null||minecraft.getConnection()==null) return "";
        if (minecraft.getSingleplayerServer()!=null) {
            //#if MC >= 11600
            return worldKey(minecraft.getSingleplayerServer()
                    .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
            //#else
            //$$ return "world-folder:"+minecraft.getSingleplayerServer().getLevelIdName();
            //#endif
        }
        if (minecraft.getCurrentServer()!=null) {
            String ip=minecraft.getCurrentServer().ip;
            return ip==null?"":"server:"+ip.trim().toLowerCase(Locale.ROOT);
        }
        return ""; // no stable identity: global only
    }

    static String worldKey(Path root){
        if(root==null)return "";
        Path folder=root.toAbsolutePath().normalize().getFileName();
        return folder==null?"":"world-folder:"+folder;
    }

    /** Refuse to save a local draft to a missing or different server/world. */
    static void requireServerIdentity(String opened,String current){
        if(opened==null||opened.isEmpty()||!opened.equals(current))
            throw new IllegalStateException("Camera server/world identity changed or unavailable");
    }

    static synchronized CameraCommandProfile load(Minecraft minecraft) {
        JsonObject root=read();
        return merge(object(root,"global"),object(object(root,"servers"),serverKey(minecraft)));
    }

    static synchronized CameraCommandProfile loadGlobal(){
        return parse(object(read(),"global"),new CameraCommandProfile());
    }

    static synchronized void save(Minecraft minecraft,boolean global,CameraCommandProfile profile) {
        JsonObject root=read();
        if (global||serverKey(minecraft).isEmpty()) {
            root.add("global",serialize(profile));
        } else {
            JsonObject servers=object(root,"servers");
            if (servers==null) servers=new JsonObject();
            servers.add(serverKey(minecraft),serializeOverrides(profile));
            root.add("servers",servers);
        }
        root.addProperty("schema",2);
        Path path=path();
        CameraConfigFiles.write(path,root);
    }

    /** Every key in a legacy full snapshot is intentionally retained as an explicit override. */
    static CameraCommandProfile merge(JsonObject defaults,JsonObject server){
        CameraCommandProfile profile=parse(defaults,new CameraCommandProfile());
        profile.clearAllOverrides();
        return parse(server,profile);
    }

    private static CameraCommandProfile parse(JsonObject json,CameraCommandProfile inherited) {
        CameraCommandProfile profile=inherited.copy();
        if (json==null) return profile;
        JsonElement mode=json.get("spectatorControlMode");
        if(mode!=null&&mode.isJsonPrimitive()) {
            try{profile.setMode(CameraCommandProfile.Mode.valueOf(mode.getAsString()));}
            catch(IllegalArgumentException ignored){}
        }
        for(CameraCommandProfile.Action action:CameraCommandProfile.Action.values()) {
            JsonElement value=json.get(action.key);
            if(value!=null&&value.isJsonPrimitive()) {
                String template=value.getAsString();
                if(CameraCommandProfile.validate(action,template)==null) profile.set(action,template);
            }
        }
        return profile;
    }

    static synchronized void saveBoth(Minecraft minecraft,CameraCommandProfile global,boolean saveGlobal,
            CameraCommandProfile server,boolean saveServer){
        if(!saveGlobal&&!saveServer)return;
        JsonObject root=read();
        if(saveGlobal)root.add("global",serialize(global));
        String key=serverKey(minecraft);
        if(saveServer){
            if(key.isEmpty())throw new IllegalStateException("Missing server/world identity");
            JsonObject servers=object(root,"servers");
            if(servers==null)servers=new JsonObject();
            servers.add(key,serializeOverrides(server));
            root.add("servers",servers);
        }
        root.addProperty("schema",2);
        CameraConfigFiles.write(path(),root);
    }

    private static JsonObject serialize(CameraCommandProfile profile) {
        JsonObject result=new JsonObject();
        result.addProperty("spectatorControlMode",profile.mode.name());
        for(CameraCommandProfile.Action action:CameraCommandProfile.Action.values())
            result.addProperty(action.key,profile.get(action));
        return result;
    }

    /** An empty string is a deliberate disable, not an absent override. */
    static JsonObject serializeOverrides(CameraCommandProfile profile){
        JsonObject result=new JsonObject();
        if(profile.overridesMode())result.addProperty("spectatorControlMode",profile.mode.name());
        for(CameraCommandProfile.Action action:CameraCommandProfile.Action.values())
            if(profile.overrides(action))result.addProperty(action.key,profile.get(action));
        return result;
    }

    private static JsonObject read() {return CameraConfigFiles.read(path());}
    private static JsonObject object(JsonObject parent,String key){
        if(parent==null)return null;
        JsonElement value=parent.get(key);
        return value!=null&&value.isJsonObject()?value.getAsJsonObject():null;
    }
    private static Path path(){return DdsTweakConfigPaths.cameraCommandProfiles();}
}

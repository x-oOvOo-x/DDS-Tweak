package ddstweak.feature.cameraplayerselector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ddstweak.config.DdsTweakConfigPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Loads and persists the configured camera destinations.
 * 读取并保存已配置的相机目的地。
 */
final class CameraDestinationStore {
    private static final String KEY="destinations";
    private CameraDestinationStore(){}

    static synchronized List<CameraDestination> load(){
        Path path=path();
        JsonObject root=CameraConfigFiles.read(path);
        JsonElement element=root.get(KEY);
        if(element==null)return Collections.emptyList();
        if(!element.isJsonArray())throw new IllegalStateException("Invalid destination list; original JSON kept");
        List<CameraDestination> result=new ArrayList<>();
        for(JsonElement entry:element.getAsJsonArray()){
            CameraDestination destination=read(entry);
            if(destination==null||!destination.isValid())
                throw new IllegalStateException("Invalid destination entry; original JSON kept");
            result.add(destination);
        }
        return result;
    }

    static synchronized void upsert(CameraDestination destination){
        if(destination==null||!destination.isValid())return;
        List<CameraDestination> next=new ArrayList<>(load());
        boolean replaced=false;
        for(int i=0;i<next.size();i++){
            if(next.get(i).id().equals(destination.id())){
                next.set(i,destination);
                replaced=true;
                break;
            }
        }
        if(!replaced)next.add(destination);
        save(next);
    }

    static synchronized void delete(String id){
        if(id==null||id.isEmpty())return;
        List<CameraDestination> next=new ArrayList<>(load());
        if(next.removeIf(destination->id.equals(destination.id())))save(next);
    }

    private static void save(Collection<CameraDestination> destinations){
        JsonObject root=new JsonObject();
        root.addProperty("schema",1);
        JsonArray array=new JsonArray();
        for(CameraDestination destination:destinations){
            if(destination==null||!destination.isValid())continue;
            JsonObject entry=new JsonObject();
            entry.addProperty("id",destination.id());
            entry.addProperty("name",destination.name());
            entry.addProperty("dimension",destination.dimension());
            entry.addProperty("x",destination.x());
            entry.addProperty("y",destination.y());
            entry.addProperty("z",destination.z());
            entry.addProperty("icon",destination.iconId());
            array.add(entry);
        }
        root.add(KEY,array);
        Path path=path();
        CameraConfigFiles.write(path,root);
    }

    private static CameraDestination read(JsonElement element){
        if(element==null||!element.isJsonObject())return null;
        JsonObject entry=element.getAsJsonObject();
        try{
            return new CameraDestination(
                    string(entry,"id",""),
                    string(entry,"name",""),
                    string(entry,"dimension",""),
                    number(entry,"x"),
                    number(entry,"y"),
                    number(entry,"z"),
                    string(entry,"icon",CameraDestination.DEFAULT_ICON_ID)
            );
        }catch(RuntimeException ignored){
            return null;
        }
    }

    private static String string(JsonObject object,String key,String fallback){
        JsonElement value=object.get(key);
        return value==null||value.isJsonNull()?fallback:value.getAsString();
    }

    private static double number(JsonObject object,String key){
        JsonElement value=object.get(key);
        if(value==null||value.isJsonNull())throw new IllegalArgumentException(key);
        return value.getAsDouble();
    }

    private static Path path(){
        return DdsTweakConfigPaths.cameraDestinations();
    }
}

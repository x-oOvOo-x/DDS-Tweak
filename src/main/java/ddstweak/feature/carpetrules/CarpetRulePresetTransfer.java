package ddstweak.feature.carpetrules;

import ddstweak.config.DdsTweakConfigPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Explicit local JSON exchange. The GUI defaults to a fixed config-directory
 * path and permits a local file path entered by the user, never by a remote server.
 */
public final class CarpetRulePresetTransfer {
    private static final int MAX_BYTES=4*1024*1024;
    private CarpetRulePresetTransfer(){}

    public static Path exchangeFile(){
        return DdsTweakConfigPaths.carpetRuleExchange();
    }

    /** Each selected plan has its own safely named JSON file; legacy exchange imports remain valid. */
    public static Path exportDirectory(){
        return DdsTweakConfigPaths.carpetRuleExports();
    }

    public static Path exportFile(String planName){
        return fileForPlan(exportDirectory(),planName);
    }

    static Path fileForPlan(Path directory,String planName){
        if(directory==null||planName==null||planName.trim().isEmpty())
            throw new IllegalArgumentException("Missing export name or directory");
        String safe=planName.replaceAll("[^\\p{L}\\p{N}._ -]","_")
                .replaceAll("^[. ]+|[. ]+$","");
        if(safe.length()>48)safe=safe.substring(0,48).replaceAll("[. ]+$","");
        if(safe.isEmpty())safe="plan";
        String identity=UUID.nameUUIDFromBytes(planName.getBytes(StandardCharsets.UTF_8))
                .toString().substring(0,8);
        return directory.resolve(safe+"-"+identity+".json");
    }

    public static boolean exportTo(Path destination,CarpetRulePresetStore store){
        return writeJson(destination,store.toJson());
    }

    /** UI export must never emit empty plans, snapshots or unrelated plans. */
    public static boolean exportSelectedTo(Path destination,CarpetRulePresetStore store,String name){
        String json=store.exportJson(name);
        return json!=null&&writeJson(destination,json);
    }

    private static boolean writeJson(Path destination,String json){
        if(destination==null||json==null)return false;
        Path tmp=destination.resolveSibling(destination.getFileName()+".tmp");
        try{
            byte[] data=json.getBytes(StandardCharsets.UTF_8);
            if(data.length>MAX_BYTES)return false;
            Files.createDirectories(destination.getParent());
            Files.write(tmp,data);
            try{
                Files.move(tmp,destination,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            }catch(java.nio.file.AtomicMoveNotSupportedException ignored){
                Files.move(tmp,destination,StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        }catch(IOException|RuntimeException ex){
            return false;
        }
    }

    /**
     * Returns imported count, or -1 if invalid/unreadable. A failed disk save
     * is reported rather than falsely claiming persistent import success.
     */
    public static int importFrom(Path source,CarpetRulePresetStore store){
        try{
            if(!Files.isRegularFile(source)||Files.size(source)>MAX_BYTES)return -1;
            String data=new String(Files.readAllBytes(source),StandardCharsets.UTF_8);
            // The store import is atomic for validation, but disk I/O can still
            // fail; the GUI reports that separately.
            return store.importJson(data);
        }catch(IOException|RuntimeException ex){
            return -1;
        }
    }
}

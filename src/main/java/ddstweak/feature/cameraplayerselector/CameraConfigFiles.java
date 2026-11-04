package ddstweak.feature.cameraplayerselector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Protect Camera JSON from corruption, partial writes and accidental reset. */
final class CameraConfigFiles {
    private CameraConfigFiles(){}

    static JsonObject read(Path path){
        if(!Files.exists(path))return new JsonObject();
        if(!Files.isRegularFile(path)||!Files.isReadable(path))
            throw new IllegalStateException("Cannot read Camera config: "+path);
        // Pure Gson parsing keeps file validation independent of Minecraft/MaLiLib initialization.
        try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
            JsonElement value=new JsonParser().parse(reader);
            if(value==null||!value.isJsonObject())
                throw new IllegalStateException("Invalid Camera JSON object: "+path);
            return value.getAsJsonObject();
        }catch(IOException | RuntimeException failure){
            throw new IllegalStateException("Cannot parse Camera config, keeping original: "+path,failure);
        }
    }

    static void write(Path path,JsonObject root){
        // Refuse to overwrite a broken existing document with new defaults.
        read(path);
        Path temp=path.resolveSibling(path.getFileName()+".tmp");
        Path backup=path.resolveSibling(path.getFileName()+".bak");
        try{
            Files.createDirectories(path.getParent());
            if(Files.exists(path))Files.copy(path,backup,StandardCopyOption.REPLACE_EXISTING);
            Files.write(temp,root.toString().getBytes(StandardCharsets.UTF_8));
            try{
                Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            }catch(AtomicMoveNotSupportedException ignored){
                Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(IOException failure){
            throw new IllegalStateException("Cannot save Camera config: "+path,failure);
        }finally{
            try{Files.deleteIfExists(temp);}catch(IOException ignored){}
        }
    }
}

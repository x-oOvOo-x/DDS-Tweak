package ddstweak.feature.windowplayerhead;

import java.io.*;
import java.nio.file.*;

final class PlayerSkinCache {
    private static final byte[] PNG={(byte)137,80,78,71,13,10,26,10};
    static Path path(Path gameDir,String key){return gameDir.resolve("cache").resolve("dds-tweak").resolve("player-skin").resolve(key+".png");}
    static boolean png(byte[] data){if(data==null||data.length<PNG.length)return false;for(int i=0;i<PNG.length;i++)if(data[i]!=PNG[i])return false;return true;}
    static boolean usable(Path path){
        try{if(!Files.isRegularFile(path)||Files.size(path)<PNG.length)return false;byte[] h=new byte[PNG.length];try(InputStream in=Files.newInputStream(path)){int offset=0;while(offset<h.length){int read=in.read(h,offset,h.length-offset);if(read<0)break;offset+=read;}return offset==h.length&&png(h);}}
        catch(IOException e){return false;}
    }
    static Path store(Path path,byte[] data) throws IOException {
        if(!png(data))throw new IOException("Not a PNG image");Files.createDirectories(path.getParent());Path tmp=Files.createTempFile(path.getParent(),"skin-",".tmp");
        try{Files.write(tmp,data);try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}return path;}
        finally{Files.deleteIfExists(tmp);}
    }
    private PlayerSkinCache(){}
}

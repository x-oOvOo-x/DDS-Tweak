package ddstweak.feature.autologcleanup;

import ddstweak.config.Configs;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AutoLogCleanup {
    private static final Pattern HISTORICAL_LOG=Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]+\\.log(?:\\.gz)?");
    private static boolean cleaned;

    private AutoLogCleanup(){}

    public static void initialize(){
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> cleanOnce());
    }

    static void cleanOnce(){
        if(cleaned)return;
        cleaned=true;
        if(!Configs.Tweaks.AUTO_LOG_CLEANUP.getBooleanValue())return;

        Path logs=FabricLoader.getInstance().getGameDir().resolve("logs");
        if(!Files.isDirectory(logs))return;

        List<Path> files;
        try(Stream<Path> stream=Files.list(logs)){
            files=stream.filter(Files::isRegularFile)
                    .filter(AutoLogCleanup::isHistoricalLog)
                    .collect(Collectors.toList());
        }catch(IOException e){
            warn("Failed to scan log directory "+logs,e);
            return;
        }

        IConfigOptionListEntry value=Configs.Tweaks.AUTO_LOG_CLEANUP_MODE.getOptionListValue();
        AutoLogCleanupMode mode=value instanceof AutoLogCleanupMode?(AutoLogCleanupMode)value:AutoLogCleanupMode.DAYS;
        if(mode==AutoLogCleanupMode.LIMIT)cleanByLimit(files,Configs.Tweaks.AUTO_LOG_KEEP_LIMIT.getIntegerValue());
        else cleanByDays(files,Configs.Tweaks.AUTO_LOG_KEEP_DAYS.getIntegerValue());
    }

    static boolean isHistoricalLog(Path path){
        Path name=path.getFileName();
        return name!=null&&HISTORICAL_LOG.matcher(name.toString()).matches();
    }

    private static void cleanByDays(List<Path> files,int days){
        long cutoff=System.currentTimeMillis()-days*24L*60L*60L*1000L;
        for(Path file:files){
            long modified=lastModifiedOrMax(file);
            if(modified<cutoff)delete(file);
        }
    }

    private static void cleanByLimit(List<Path> files,int limit){
        files.sort(Comparator.comparingLong(AutoLogCleanup::lastModifiedOrMax).reversed());
        for(int i=Math.max(0,limit);i<files.size();i++)delete(files.get(i));
    }

    private static long lastModifiedOrMax(Path file){
        try{
            return Files.getLastModifiedTime(file).toMillis();
        }catch(IOException e){
            warn("Failed to read log timestamp "+file,e);
            return Long.MAX_VALUE;
        }
    }

    private static void delete(Path file){
        try{
            Files.deleteIfExists(file);
        }catch(IOException|SecurityException e){
            warn("Failed to delete old log "+file,e);
        }
    }

    private static void warn(String message,Exception e){
        System.err.println("[DDS Tweak/AutoLogCleanup] "+message+": "+e.getMessage());
    }
}

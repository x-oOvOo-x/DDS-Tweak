package ddstweak.feature.windowplayerhead;

import com.google.gson.*;
import org.apache.logging.log4j.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

final class PlayerSkinDownloader {
    private static final Logger LOGGER=LogManager.getLogger("DDS Tweak/PlayerHeadWindowIcon");
    static boolean successful(int status){return status>=200&&status<300;}

    static CompletableFuture<Path> resolve(final Path gameDir,final UUID id){
        return CompletableFuture.supplyAsync(new java.util.function.Supplier<Path>(){
            @Override public Path get(){
                try{
                    String uuid=id.toString().replace("-","");
                    Response profile=request(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/"+uuid+"?unsigned=false"),15000);
                    if(!successful(profile.status)){LOGGER.warn("Player profile request failed: HTTP {}",profile.status);return null;}
                    URI skin=skinUri(profile.body);String key=textureKey(skin);
                    if(key==null){LOGGER.warn("Player profile did not contain a valid Minecraft skin texture");return null;}
                    Path cached=PlayerSkinCache.path(gameDir,key);if(PlayerSkinCache.usable(cached))return cached;
                    Response texture=request(skin,20000);
                    if(!successful(texture.status)||!PlayerSkinCache.png(texture.body)){LOGGER.warn("Player skin request failed: HTTP {} or invalid PNG",texture.status);return null;}
                    try{return PlayerSkinCache.store(cached,texture.body);}catch(Exception e){LOGGER.warn("Failed to cache player skin at {}",cached,e);return null;}
                }catch(Exception e){LOGGER.warn("Failed to resolve player skin",e);return null;}
            }
        });
    }

    private static Response request(URI uri,int timeoutMillis) throws IOException{
        HttpURLConnection connection=(HttpURLConnection)uri.toURL().openConnection();
        connection.setConnectTimeout(Math.min(timeoutMillis,10000));
        connection.setReadTimeout(timeoutMillis);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent","DDS-Tweak");
        try{
            int status=connection.getResponseCode();
            InputStream in=status>=400?connection.getErrorStream():connection.getInputStream();
            return new Response(status,readAll(in));
        }finally{connection.disconnect();}
    }

    private static byte[] readAll(InputStream input) throws IOException{
        if(input==null)return new byte[0];
        try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];
            for(int read;(read=in.read(buffer))>=0;)if(read>0)out.write(buffer,0,read);
            return out.toByteArray();
        }
    }

    static URI skinUri(byte[] profile){
        try{
            JsonObject root=new JsonParser().parse(new String(profile,StandardCharsets.UTF_8)).getAsJsonObject();JsonArray properties=root.getAsJsonArray("properties");if(properties==null)return null;
            for(JsonElement e:properties){JsonObject p=e.getAsJsonObject();if(!"textures".equals(p.get("name").getAsString()))continue;String value=p.get("value").getAsString();JsonObject payload=new JsonParser().parse(new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8)).getAsJsonObject();JsonObject textures=payload.getAsJsonObject("textures");if(textures==null||!textures.has("SKIN"))return null;URI raw=URI.create(textures.getAsJsonObject("SKIN").get("url").getAsString());String key=textureKey(raw);return key==null?null:URI.create("https://textures.minecraft.net/texture/"+key);}
        }catch(Exception e){return null;}
        return null;
    }
    static String textureKey(URI uri){
        if(uri==null||!"textures.minecraft.net".equalsIgnoreCase(uri.getHost()))return null;String path=uri.getPath();if(path==null)return null;int slash=path.lastIndexOf('/');String key=slash>=0?path.substring(slash+1):path;return key.matches("[0-9a-fA-F]{16,128}")?key.toLowerCase(Locale.ROOT):null;
    }
    private static final class Response{
        private final int status;private final byte[] body;
        private Response(int status,byte[] body){this.status=status;this.body=body;}
    }
    private PlayerSkinDownloader(){}
}

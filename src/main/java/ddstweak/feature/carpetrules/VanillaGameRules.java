package ddstweak.feature.carpetrules;

import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
//#if MC < 260000
import net.minecraft.server.MinecraftServer;
//#endif
//#if MC < 12111
//$$ import net.minecraft.nbt.CompoundTag;
//#endif
//#if MC >= 12111 && MC < 260000
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.core.registries.BuiltInRegistries;
//#endif
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
//#if MC >= 260000
//$$ import net.minecraft.commands.Commands;
//$$ import net.minecraft.core.registries.BuiltInRegistries;
//$$ import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
//$$ import net.minecraft.resources.ResourceKey;
//$$ import net.minecraft.world.level.gamerules.GameRule;
//$$ import net.minecraft.world.level.gamerules.GameRuleType;
//$$ import net.minecraft.network.chat.Component;
//#endif

/**
 * Server-synchronized vanilla gamerules, independent of Carpet's rule catalog.
 * Unknown current values are never reconstructed from registry defaults.
 */
public final class VanillaGameRules {
    private static ClientPacketListener connection;
    private static Map<String,Entry> entries=Collections.emptyMap();
    private static boolean received;
    private static int readFailures;
    private static long lastRequest;
    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(VanillaGameRules.class.getName());
    private static volatile boolean warnedRegistryFailure;
    private VanillaGameRules(){}

    public static final class Entry {
        public final String id,title,description,category,value,defaultValue;
        public final boolean booleanType;
        private final Object rule;
        Entry(String id,String title,String description,String category,
              String value,String defaultValue,boolean booleanType,Object rule){
            this.id=id;this.title=title;this.description=description;
            this.category=category;this.value=value;this.defaultValue=defaultValue;
            this.booleanType=booleanType;this.rule=rule;
        }
    }

    public static synchronized void reset(){
        connection=null;entries=Collections.emptyMap();
        received=false;lastRequest=0;readFailures=0;warnedRegistryFailure=false;
        //#if MC < 260000
        VanillaGameRuleRemoteSync.reset();
        //#endif
    }
    public static synchronized boolean received(){return received;}
    /** A failed local read is distinguishable from a request still in flight. */
    public static synchronized boolean unavailable(){return !received&&readFailures>=3;}
    public static synchronized Entry get(CarpetRuleCatalog.RuleKey key){
        return key!=null&&"minecraft".equals(key.manager())?entries.get(key.name()):null;
    }
    public static synchronized List<CarpetRuleCatalog.RemoteRule> list(String query){
        String needle=query.trim().toLowerCase(Locale.ROOT);
        List<CarpetRuleCatalog.RemoteRule> out=new ArrayList<>();
        if(!received)return out;
        for(Entry item:entries.values()){
            if(!needle.isEmpty()&&!item.id.toLowerCase(Locale.ROOT).contains(needle)
                    &&!item.title.toLowerCase(Locale.ROOT).contains(needle)
                    &&!item.description.toLowerCase(Locale.ROOT).contains(needle)
                    &&!item.category.toLowerCase(Locale.ROOT).contains(needle))continue;
            out.add(new CarpetRuleCatalog.RemoteRule(
                    new CarpetRuleCatalog.RuleKey("minecraft",item.id),item.value,true));
        }
        out.sort(Comparator.comparing(rule->rule.key().name()));
        return out;
    }
    public static boolean supported(){
        //#if MC >= 260000
        //$$ return true;
        //#else
        return Minecraft.getInstance().getSingleplayerServer()!=null
                ||VanillaGameRuleRemoteSync.available();
        //#endif
    }
    public static boolean permitted(Minecraft client){
        //#if MC >= 260000
        //$$ return client!=null&&client.player!=null&&client.getConnection()!=null
        //$$         &&Commands.LEVEL_GAMEMASTERS.check(client.player.permissions());
        //#else
        return client!=null&&client.getConnection()!=null
                &&client.getConnection().getCommands().getRoot()
                        .getChild("gamerule")!=null;
        //#endif
    }
    public static synchronized void request(Minecraft client){
        if(client==null||client.getConnection()==null)return;
        ClientPacketListener current=client.getConnection();
        if(connection!=current)reset();
        connection=current;
        // Reading a local world's gamerules does not require command permission.
        // Only writes through /gamerule remain permission-gated.
        //#if MC >= 260000
        //$$ if(!permitted(client))return;
        //#else
        if(client.getSingleplayerServer()==null
                &&!VanillaGameRuleRemoteSync.available())return;
        //#endif
        long now=System.currentTimeMillis();
        if(lastRequest>0&&now-lastRequest<4000L)return;
        lastRequest=now;
        //#if MC >= 260000
        //$$ current.send(new ServerboundClientCommandPacket(
        //$$         ServerboundClientCommandPacket.Action.REQUEST_GAMERULE_VALUES));
        //#else
        MinecraftServer server=client.getSingleplayerServer();
        if(server!=null)server.execute(()->{
            Map<String,Entry> snapshot=readIntegrated(server);
            client.execute(()->acceptIntegrated(client,current,server,snapshot));
        });
        else VanillaGameRuleRemoteSync.request(current);
        //#endif
    }
    public static synchronized void poll(Minecraft client){
        if(client==null||client.getConnection()==null)return;
        if(connection!=client.getConnection())reset();
        //#if MC >= 260000
        //$$ long interval=20000L;
        //#else
        if(client.getSingleplayerServer()==null&&!VanillaGameRuleRemoteSync.available()){
            entries=Collections.emptyMap();
            received=false;
            return;
        }
        long interval=4000L;
        //#endif
        if(lastRequest==0||System.currentTimeMillis()-lastRequest>=interval)
            request(client);
    }

    //#if MC >= 260000
    //$$ public static synchronized void accept(ClientPacketListener source,
    //$$         Map<ResourceKey<GameRule<?>>,String> values){
    //$$     Minecraft client=Minecraft.getInstance();
    //$$     if(source!=client.getConnection()||values==null||values.size()>512)return;
    //$$     Map<String,Entry> incoming=new HashMap<>();
    //$$     for(Map.Entry<ResourceKey<GameRule<?>>,String> item:values.entrySet()){
    //$$         GameRule<?> rule=BuiltInRegistries.GAME_RULE.getValue(item.getKey());
    //$$         if(rule==null||item.getValue()==null||item.getValue().length()>256)return;
    //$$         String id=item.getKey().identifier().toString();
    //$$         String title=Component.translatableWithFallback(rule.getDescriptionId(),id).getString();
    //$$         String description=Component.translatableWithFallback(
    //$$                 rule.getDescriptionId()+".description","").getString();
    //$$         String category=rule.category().id().getPath();
    //$$         Entry entry=new Entry(id,title,description,category,item.getValue(),
    //$$                 serializeDefault(rule),rule.gameRuleType()==GameRuleType.BOOL,rule);
    //$$         if(incoming.putIfAbsent(id,entry)!=null)return;
    //$$     }
    //$$     connection=source;
    //$$     entries=Collections.unmodifiableMap(incoming);
    //$$     received=true;
    //$$ }
    //$$ private static <T> String serializeDefault(GameRule<T> rule){
    //$$     return rule.serialize(rule.defaultValue());
    //$$ }
    //#endif

    //#if MC < 12111
    //$$ /**
    //$$  * Older Minecraft versions have no request/response gamerule packet.
    //$$  * Read only the running singleplayer server's actual values, on its thread.
    //$$  * No remote server state is inferred or fabricated.
    //$$  */
    //$$ private static Map<String,Entry> readIntegrated(MinecraftServer server){
    //$$     try{
    //$$         CompoundTag values=server.getGameRules().createTag();
    //$$         if(values==null)return null;
    //#if MC >= 12105
    //$$         java.util.Set<String> keys=values.keySet();
    //#else
    //$$         java.util.Set<String> keys=values.getAllKeys();
    //#endif
    //$$         if(keys.isEmpty()||keys.size()>512)return null;
    //$$         Map<String,Entry> snapshot=new HashMap<>();
    //$$         for(String id:keys){
    //$$             if(!id.matches("[A-Za-z0-9_:.\\-]{1,128}"))return null;
    //$$             String value=MinecraftCompat.stringOr(values,id,"");
    //$$             if(value.isEmpty()||value.length()>256)return null;
    //$$             boolean bool="true".equals(value)||"false".equals(value);
    //$$             if(!bool)try{Integer.parseInt(value);}
    //$$                 catch(NumberFormatException ex){return null;}
    //$$             snapshot.put(id,new Entry(id,id,"","minecraft",value,null,bool,null));
    //$$         }
    //$$         return snapshot;
    //$$     }catch(RuntimeException ex){return null;}
    //$$ }
    //#elseif MC < 260000
    /** Registered gamerules (1.21.11) have no legacy NBT tag API. */
    private static Map<String,Entry> readIntegrated(MinecraftServer server){
        try{
            GameRules rules=server.overworld().getGameRules();
            Map<String,Entry> snapshot=new HashMap<>();
            int skipped=0;
            for(GameRule<?> rule:rules.availableRules().toArray(GameRule[]::new)){
                try{
                    Entry entry=registeredEntry(rules,rule);
                    if(entry==null||snapshot.putIfAbsent(entry.id,entry)!=null)skipped++;
                    if(snapshot.size()>512)return null;
                }catch(RuntimeException ex){
                    skipped++; // one modded/broken rule must not hide all vanilla rules
                }
            }
            // The 1.21.11 gamerule registry was newly introduced. If the
            // instance enumeration is empty, fall back to the global registry
            // but still query values from this world's authoritative rules.
            if(snapshot.isEmpty()){
                for(GameRule<?> rule:BuiltInRegistries.GAME_RULE){
                    try{
                        Entry entry=registeredEntry(rules,rule);
                        if(entry!=null)snapshot.putIfAbsent(entry.id,entry);
                        if(snapshot.size()>512)return null;
                    }catch(RuntimeException ex){skipped++;}
                }
            }
            if(skipped>0&&!warnedRegistryFailure){
                warnedRegistryFailure=true;
                LOGGER.warning("Skipped "+skipped+" unreadable 1.21.11 game rules");
            }
            if(snapshot.isEmpty()&&!warnedRegistryFailure){
                warnedRegistryFailure=true;
                LOGGER.warning("No 1.21.11 game rules were available from the integrated server");
            }
            return snapshot.isEmpty()?null:snapshot;
        }catch(RuntimeException ex){
            if(!warnedRegistryFailure){
                warnedRegistryFailure=true;
                LOGGER.log(java.util.logging.Level.WARNING,
                        "Unable to collect integrated 1.21.11 game rules",ex);
            }
            return null;
        }
    }
    private static <T> Entry registeredEntry(GameRules rules,GameRule<T> rule){
        String id=rule.getIdentifier().toString();
        if(id.length()>128||!id.matches("[A-Za-z0-9_:.\\-]+"))return null;
        String value=rule.serialize(rules.get(rule));
        String standard=rule.serialize(rule.defaultValue());
        if(value==null||standard==null||value.length()>256
                ||standard.length()>256)return null;
        return new Entry(id,rule.getDescriptionId(),"",rule.category().id().getPath(),
                value,standard,rule.valueClass()==Boolean.class,rule);
    }
    //#endif

    //#if MC < 260000
    private static synchronized void acceptIntegrated(Minecraft client,
            ClientPacketListener source,MinecraftServer server,Map<String,Entry> snapshot){
        if(client.getConnection()!=source
                ||client.getSingleplayerServer()!=server)return;
        if(snapshot==null){
            readFailures++;
            return;
        }
        publish(source,snapshot);
    }
    static synchronized void remoteReadFailed(ClientPacketListener source){
        if(source==connection&&Minecraft.getInstance().getSingleplayerServer()==null){
            if(++readFailures>=3){
                // Never keep displaying a stale remote snapshot indefinitely.
                entries=Collections.emptyMap();
                received=false;
            }
        }
    }
    static synchronized void acceptRemote(ClientPacketListener source,
            Map<String,Entry> snapshot){
        Minecraft client=Minecraft.getInstance();
        if(source!=connection||client.getConnection()!=source
                ||client.getSingleplayerServer()!=null||snapshot==null)return;
        publish(source,snapshot);
    }
    private static void publish(ClientPacketListener source,Map<String,Entry> snapshot){
        readFailures=0;
        Map<String,Entry> localized=new HashMap<>();
        for(Entry entry:snapshot.values()){
            String nameKey=entry.title.startsWith("gamerule.")
                    ?entry.title:"gamerule."+entry.id;
            String descKey=nameKey+".description";
            String title=MinecraftCompat.translatable(nameKey).getString();
            String description=MinecraftCompat.translatable(descKey).getString();
            localized.put(entry.id,new Entry(entry.id,
                    nameKey.equals(title)?entry.id:title,
                    descKey.equals(description)?"":description,
                    entry.category,entry.value,entry.defaultValue,entry.booleanType,entry.rule));
        }
        connection=source;
        entries=Collections.unmodifiableMap(localized);
        received=true;
    }
    //#endif

    public static synchronized boolean valid(Entry entry,String value){
        if(entry==null||value==null||value.isEmpty()||value.length()>256
                ||value.indexOf('\n')>=0||value.indexOf('\r')>=0
                ||value.indexOf('\0')>=0)return false;
        //#if MC >= 260000
        //$$ if(entry.booleanType&&!("true".equals(value)||"false".equals(value)))return false;
        //$$ return ((GameRule<?>)entry.rule).deserialize(value).result().isPresent();
        //#elseif MC >= 12111
        if(entry.rule instanceof GameRule)
            return ((GameRule<?>)entry.rule).deserialize(value).result().isPresent();
        if(entry.booleanType)return "true".equals(value)||"false".equals(value);
        try{Integer.parseInt(value);return true;}
        catch(NumberFormatException ex){return false;}
        //#else
        //$$ if(entry.booleanType)return "true".equals(value)||"false".equals(value);
        //$$ try {Integer.parseInt(value);return true;}
        //$$ catch(NumberFormatException ex){return false;}
        //#endif
    }

    /** Command sent does not prove success: wait for a fresh server snapshot. */
    public static synchronized boolean submit(Minecraft client,Entry entry,
                                               String expected,String target){
        if(!permitted(client)||!received||entry==null||expected==null
                ||!valid(entry,target))return false;
        Entry live=entries.get(entry.id);
        if(live==null||!live.value.equals(expected)
                ||connection!=client.getConnection())return false;
        boolean sent=MinecraftCompat.sendCommandWithoutHistory(client,
                "gamerule "+entry.id+" "+target);
        if(sent)lastRequest=0;
        return sent;
    }
}

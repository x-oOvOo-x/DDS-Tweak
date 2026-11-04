package ddstweak.feature.blocksoundtracker;

//#if MC >= 12111
import ddstweak.config.Configs;
import fi.dy.masa.malilib.event.RenderEventHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-only, bounded and world-scoped sound location cache. */
public final class BlockSoundTracker {
    private static final int MAX_RECORDS=256;
    private static final Map<Key,Record> RECORDS=new LinkedHashMap<>();
    private static ClientLevel lastLevel;
    private static long clock;
    private static boolean initialized;

    private BlockSoundTracker(){}

    public static void initialize(){
        if(initialized)return;
        initialized=true;
        Minecraft.getInstance().getSoundManager().addListener((sound,ignored,range)->onSound(sound));
        ClientTickEvents.END_CLIENT_TICK.register(BlockSoundTracker::tick);
        RenderEventHandler.getInstance().registerWorldLastRenderer(BlockSoundRenderer.INSTANCE);
        Configs.Visual.BLOCK_SOUND_TRACKER.getKeybind().setCallback((action,key)->{
            Configs.Visual.BLOCK_SOUND_TRACKER.toggleBooleanValue();
            if(!Configs.Visual.BLOCK_SOUND_TRACKER.getBooleanValue())clear();
            return true;
        });
    }

    public static void onWorldEvent(int type,BlockPos pos){
        if(!BlockSoundPolicy.isDispenserWorldEvent(type)||pos==null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!enabled())return;
        if(!mc.isSameThread()){
            BlockPos immutable=pos.immutable();
            ClientLevel originalLevel=mc.level;
            mc.execute(()->{
                if(mc.level==originalLevel)onWorldEvent(type,immutable);
            });
            return;
        }
        String sourceBlockId=null;
        if(mc.level.hasChunkAt(pos)){
            BlockState state=mc.level.getBlockState(pos);
            if(state.is(Blocks.DISPENSER)||state.is(Blocks.DROPPER))
                sourceBlockId=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        }
        record(mc,"event:"+type,pos,sourceBlockId);
    }

    private static void onSound(SoundInstance sound){
        if(sound==null||!enabled())return;
        String id=sound.getIdentifier().toString();
        if(!BlockSoundPolicy.isBlockSound(id,sound.getSource()==SoundSource.BLOCKS,sound.isRelative())||
                BlockSoundPolicy.isWorldEventSoundDuplicate(id))return;
        double x=sound.getX(),y=sound.getY(),z=sound.getZ();
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))return;
        Minecraft mc=Minecraft.getInstance();
        ClientLevel originalLevel=mc.level;
        if(originalLevel==null)return;
        BlockPos pos=BlockPos.containing(x,y,z);
        mc.execute(()->{
            if(mc.level!=originalLevel)return;
            String sourceBlockId=null;
            if(originalLevel.hasChunkAt(pos)&&
                    BlockSoundSourceResolver.atBlockCenter(x,y,z,pos.getX(),pos.getY(),pos.getZ())){
                BlockState state=originalLevel.getBlockState(pos);
                String candidate=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                if(BlockSoundSourceResolver.matches(id,candidate))sourceBlockId=candidate;
            }
            record(mc,id,pos,sourceBlockId);
        });
    }

    private static boolean enabled(){
        return Configs.Visual.BLOCK_SOUND_TRACKER.getBooleanValue();
    }

    private static BlockSoundFilterMode filterMode(){
        return Configs.Visual.BLOCK_SOUND_FILTER_MODE.getOptionListValue() instanceof BlockSoundFilterMode
                ?(BlockSoundFilterMode)Configs.Visual.BLOCK_SOUND_FILTER_MODE.getOptionListValue()
                :BlockSoundFilterMode.WHITELIST;
    }

    private static boolean accepted(String blockId){
        return BlockSoundPolicy.accepted(blockId,filterMode(),
                Configs.Visual.BLOCK_SOUND_BLOCK_BLACKLIST.getStrings(),
                Configs.Visual.BLOCK_SOUND_BLOCK_WHITELIST.getStrings());
    }

    private static void record(Minecraft mc,String id,BlockPos pos,String sourceBlockId){
        if(!enabled()||mc.level==null||mc.player==null||!accepted(sourceBlockId))return;
        if(lastLevel!=mc.level){clear();lastLevel=mc.level;}
        int range=Configs.Visual.BLOCK_SOUND_RANGE.getIntegerValue();
        double dx=pos.getX()+0.5-mc.player.getX();
        double dy=pos.getY()+0.5-mc.player.getY();
        double dz=pos.getZ()+0.5-mc.player.getZ();
        if(dx*dx+dy*dy+dz*dz>(double)range*range)return;

        Key key=new Key(BlockSoundPolicy.markerSourceKey(id,sourceBlockId),pos.immutable());
        Record entry=RECORDS.get(key);
        long expires=clock+Configs.Visual.BLOCK_SOUND_HIGHLIGHT_DURATION.getIntegerValue();
        if(entry==null){
            if(RECORDS.size()>=MAX_RECORDS){
                Iterator<Key> iterator=RECORDS.keySet().iterator();
                if(iterator.hasNext()){iterator.next();iterator.remove();}
            }
            RECORDS.put(key,new Record(sourceBlockId,expires,clock));
        }else{
            entry.sourceBlockId=sourceBlockId;
            entry.expiresAt=expires;
            entry.lastTriggered=clock;
            entry.count++;
        }
    }

    private static void tick(Minecraft mc){
        clock++;
        if(!enabled()||mc.level==null||mc.player==null||mc.level!=lastLevel){
            clear();
            lastLevel=mc.level;
            return;
        }
        RECORDS.entrySet().removeIf(entry->entry.getValue().expiresAt<=clock);
    }

    public static List<Marker> visible(){
        Minecraft mc=Minecraft.getInstance();
        if(!enabled()||mc.level==null||mc.player==null||mc.level!=lastLevel)return java.util.Collections.emptyList();
        int range=Configs.Visual.BLOCK_SOUND_RANGE.getIntegerValue(),limit=Configs.Visual.BLOCK_SOUND_MAX_HIGHLIGHTS.getIntegerValue();
        List<Marker> list=new ArrayList<>();
        for(Map.Entry<Key,Record> e:RECORDS.entrySet()){
            Key key=e.getKey();
            Record record=e.getValue();
            if(Configs.Visual.BLOCK_SOUND_REPEATED_ONLY.getBooleanValue()&&record.count<2)continue;
            if(!accepted(record.sourceBlockId))continue;
            double dx=key.pos.getX()+0.5-mc.player.getX();
            double dy=key.pos.getY()+0.5-mc.player.getY();
            double dz=key.pos.getZ()+0.5-mc.player.getZ();
            double distanceSq=dx*dx+dy*dy+dz*dz;
            if(distanceSq>(double)range*range)continue;
            list.add(new Marker(key.pos,record.sourceBlockId!=null,record.count,record.lastTriggered,distanceSq));
        }
        list.sort(Comparator.comparingLong(Marker::lastTriggered).reversed()
                .thenComparing(Comparator.comparingInt(Marker::count).reversed())
                .thenComparingDouble(Marker::distanceSquared));
        return list.subList(0,Math.min(limit,list.size()));
    }

    public static void clear(){RECORDS.clear();}

    private static final class Key {
        private final String id;
        private final BlockPos pos;
        private Key(String id,BlockPos pos){this.id=id;this.pos=pos;}
        @Override public boolean equals(Object value){
            if(!(value instanceof Key))return false;
            Key other=(Key)value;
            return id.equals(other.id)&&pos.equals(other.pos);
        }
        @Override public int hashCode(){return 31*id.hashCode()+pos.hashCode();}
    }

    private static final class Record {
        private String sourceBlockId;
        private long expiresAt;
        private long lastTriggered;
        private int count=1;
        private Record(String sourceBlockId,long expiresAt,long lastTriggered){
            this.sourceBlockId=sourceBlockId;this.expiresAt=expiresAt;this.lastTriggered=lastTriggered;
        }
    }

    public static final class Marker {
        private final BlockPos pos;
        private final boolean confirmed;
        private final int count;
        private final long lastTriggered;
        private final double distanceSquared;
        private Marker(BlockPos pos,boolean confirmed,int count,long time,double distance){
            this.pos=pos;this.confirmed=confirmed;this.count=count;
            this.lastTriggered=time;this.distanceSquared=distance;
        }
        public BlockPos pos(){return pos;}
        public boolean confirmed(){return confirmed;}
        public int count(){return count;}
        public long lastTriggered(){return lastTriggered;}
        public double distanceSquared(){return distanceSquared;}
    }
}
//#endif

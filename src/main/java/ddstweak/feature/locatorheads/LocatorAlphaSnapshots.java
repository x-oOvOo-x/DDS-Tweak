package ddstweak.feature.locatorheads;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-safe, bounded LRU of CPU skin alpha snapshots (no GPU access).
 * 以有限容量 LRU 缓存皮肤透明度数据，不操作 GPU。
 */
final class LocatorAlphaSnapshots {
    private final int capacity;
    private final Map<String,LocatorContourMask.SkinAlpha> entries=new LinkedHashMap<>(32,0.75f,true);

    LocatorAlphaSnapshots(int capacity){
        if(capacity<1)throw new IllegalArgumentException("capacity must be positive");
        this.capacity=capacity;
    }

    synchronized LocatorContourMask.SkinAlpha get(String key){
        return entries.get(key);
    }

    synchronized void replace(String key,LocatorContourMask.SkinAlpha alpha){
        entries.put(key,alpha);
        trim();
    }

    /** Resource packs can replace bundled skins, but downloaded skins remain cached. */
    synchronized void invalidateBundled(){
        entries.keySet().removeIf(key->
                key.substring(key.indexOf(':')+1).startsWith("textures/entity/player/"));
    }

    synchronized void clear(){
        entries.clear();
    }

    synchronized int size(){
        return entries.size();
    }

    private void trim(){
        while(entries.size()>capacity){
            String oldest=entries.keySet().iterator().next();
            entries.remove(oldest);
        }
    }
}

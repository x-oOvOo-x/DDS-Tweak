package ddstweak.feature.locatorheads;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Tests the production CPU cache without requiring a Minecraft client or GPU. */
final class LocatorAlphaSnapshotsTest {
    private static LocatorContourMask.SkinAlpha alpha(boolean opaque){
        boolean[] face=new boolean[64];
        face[0]=opaque;
        return new LocatorContourMask.SkinAlpha(face,new boolean[64]);
    }

    @Test void evictionIsBoundedAndLeastRecentlyUsed(){
        var cache=new LocatorAlphaSnapshots(2);
        var original=alpha(true);
        cache.replace("a",original);
        cache.replace("b",alpha(false));
        assertSame(original,cache.get("a")); // a is more recently used than b
        cache.replace("c",alpha(true));
        assertNull(cache.get("b"));
        assertSame(original,cache.get("a"));
        assertEquals(2,cache.size());

        var updated=alpha(false);
        cache.replace("a",updated);
        assertSame(updated,cache.get("a"));
        cache.clear();
        assertEquals(0,cache.size());
    }

    @Test void reloadKeepsDownloadedAlphaButInvalidatesBundledResources(){
        var cache=new LocatorAlphaSnapshots(4);
        var downloaded=alpha(true);
        cache.replace("minecraft:skins/face-hash",downloaded);
        cache.replace("minecraft:textures/entity/player/wide/steve.png",alpha(false));
        cache.replace("custom:textures/entity/player/custom.png",alpha(true));
        cache.invalidateBundled();
        assertSame(downloaded,cache.get("minecraft:skins/face-hash"));
        assertNull(cache.get("minecraft:textures/entity/player/wide/steve.png"));
        assertNull(cache.get("custom:textures/entity/player/custom.png"));
        assertEquals(1,cache.size());
    }

    @Test void concurrentSkinSnapshotsNeverExceedCapacity() throws Exception {
        var cache=new LocatorAlphaSnapshots(32);
        var pool=Executors.newFixedThreadPool(4);
        var start=new CountDownLatch(1);
        var jobs=new ArrayList<Future<?>>();
        try{
            for(int worker=0;worker<4;worker++){
                final int partition=worker;
                jobs.add(pool.submit(()->{
                    start.await();
                    for(int i=0;i<600;i++){
                        String key=partition+"-"+i;
                        cache.replace(key,alpha(i%2==0));
                        cache.get(key);
                        if(cache.size()>32)throw new AssertionError("Alpha snapshots exceeded capacity");
                    }
                    return null;
                }));
            }
            start.countDown();
            for(Future<?> job:jobs)job.get(20,TimeUnit.SECONDS);
            assertTrue(cache.size()<=32);
        }finally{
            pool.shutdownNow();
        }
    }
}

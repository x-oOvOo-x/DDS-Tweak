package ddstweak.feature.locatorheads;

//#if MC >= 12108
import com.mojang.blaze3d.platform.NativeImage;
import ddstweak.DDSTweakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures the alpha of downloaded skins once, and uploads small white contour
 * textures only when that skin + outer-layer + thickness combination is drawn.
 * The HUD only needs one tinted texture blit per player (not 8-16 skin blits).
 */
public final class LocatorContourCache {
    private static final int MAX_TEXTURES=128;
    private static final LocatorAlphaSnapshots ALPHAS=new LocatorAlphaSnapshots(512);
    private static final LinkedHashMap<Key,Entry> MASKS=new LinkedHashMap<>(32,0.75f,true);
    private static Object connection;
    private static Object resourceManager;
    private static final AtomicInteger reloadGeneration=new AtomicInteger();
    private static volatile int appliedReloadGeneration;
    private static long sequence;
    // Enable with -Dddstweak.locatorHeads.trace=true when collecting a debug log.
    private static final boolean TRACE=Boolean.getBoolean("ddstweak.locatorHeads.trace");
    private static final Logger LOGGER=LoggerFactory.getLogger(LocatorContourCache.class);
    private static final AtomicLong CAPTURES=new AtomicLong();
    private static long recovered,bundled,missing,uploads,releases,traceTicks;
    private LocatorContourCache(){}

    /** Releases session-bound textures even when Locator Heads is switched off. */
    public static void initialize(){
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            refresh(client);
            LocatorHudTrace.tick(client.getConnection()!=null);
            if(TRACE&&++traceTicks%200==0&&client.getConnection()!=null)
                LOGGER.info("LocatorHeads alpha: captured={}, recovered={}, bundled={}, misses={}, cpuEntries={}; masks: uploaded={}, released={}, live={}",
                        CAPTURES.get(),recovered,bundled,missing,ALPHAS.size(),uploads,releases,MASKS.size());
        });
        // ReloadableResourceManager may retain its identity after F3+T.
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            //#if MC >= 12111
            @Override public Identifier getFabricId(){
                return Identifier.fromNamespaceAndPath(DDSTweakReference.MOD_ID,"locator_contour_reload");
            }
            //#else
            //$$ @Override public ResourceLocation getFabricId(){
            //$$     return ResourceLocation.fromNamespaceAndPath(DDSTweakReference.MOD_ID,"locator_contour_reload");
            //$$ }
            //#endif
            @Override public void onResourceManagerReload(ResourceManager manager){
                clear();
            }
        });
    }

    private static void refresh(Minecraft client){
        // Do not discard skins downloaded after reload invalidation but before
        // its queued GPU cleanup has updated the resource-manager identity.
        if(reloadGeneration.get()==appliedReloadGeneration&&
                (connection!=client.getConnection()||resourceManager!=client.getResourceManager()))reset(client);
    }

    private static void reset(Minecraft client){
        boolean hadSession=connection!=null;
        boolean sessionChanged=connection!=client.getConnection();
        int previousMasks=MASKS.size();
        clearTextures(client);
        ALPHAS.clear();
        // Keep the current identity so a skin uploaded after F3+T is not erased
        // again on the next tick or contour draw.
        connection=client.getConnection();
        resourceManager=client.getResourceManager();
        // This event is also emitted after disconnect, when periodic stats stop.
        if(TRACE&&(hadSession||previousMasks>0))
            LOGGER.info("LocatorHeads reset: cause={}, previousMasks={}, cpuEntries={}, live={}, uploads={}, releases={}",
                    sessionChanged?"session":"resources",previousMasks,ALPHAS.size(),MASKS.size(),uploads,releases);
    }

    private record Key(String texture,boolean outer,int radius){}
    //#if MC >= 12111
    private record Entry(Identifier texture,LocatorContourMask.SkinAlpha source){}
    //#else
    //$$ private record Entry(ResourceLocation texture,LocatorContourMask.SkinAlpha source){}
    //#endif

    /** Called by the vanilla skin downloader while the CPU image is still available. */
    //#if MC >= 12111
    public static void accept(Identifier id,NativeImage pixels){
    //#else
    //$$ public static void accept(ResourceLocation id,NativeImage pixels){
    //#endif
        if(id==null||pixels==null||pixels.getWidth()<48||pixels.getHeight()<16)return;
        boolean[] base=new boolean[64],outer=new boolean[64];
        for(int y=0;y<8;y++)for(int x=0;x<8;x++){
            base[y*8+x]=(pixels.getPixel(x+8,y+8)>>>24)!=0;
            outer[y*8+x]=(pixels.getPixel(x+40,y+8)>>>24)!=0;
        }
        ALPHAS.replace(id.toString(),new LocatorContourMask.SkinAlpha(base,outer));
        if(TRACE)CAPTURES.incrementAndGet();
    }

    /**
     * Only called on the client render/extraction thread.
     * Connection changes evict uploaded textures to avoid retaining old worlds.
     */
    //#if MC >= 12111
    public static Identifier get(Identifier skin,boolean outer,float thickness,float faceWidth){
    //#else
    //$$ public static ResourceLocation get(ResourceLocation skin,boolean outer,float thickness,float faceWidth){
    //#endif
        Minecraft client=Minecraft.getInstance();
        refresh(client);
        if(skin==null||connection==null)return null;
        String name=skin.toString();
        LocatorContourMask.SkinAlpha alpha=ALPHAS.get(name);
        if(alpha==null)alpha=readBundledSkin(client,skin);
        // Vanilla may have registered the skin before our observer began
        // tracking it (including textures loaded from its disk cache).
        // DynamicTexture retains CPU NativeImage pixels; sampling them here
        // costs no GPU readback and is only needed once per missing skin.
        if(alpha==null)alpha=readRegisteredSkin(client,skin);
        if(alpha==null){
            if(TRACE)missing++;
            return null;
        }
        int radius=LocatorContourMask.radius(thickness,faceWidth);
        Key key=new Key(name,outer,radius);
        Entry old=MASKS.get(key);
        if(old!=null&&old.source()==alpha)return old.texture();
        if(old!=null)release(client,old);
        byte[] contour=LocatorContourMask.create(alpha.face(),alpha.hat(),outer,radius);
        if(!LocatorContourMask.hasAlpha(contour)){
            MASKS.remove(key);
            return null;
        }
        NativeImage pixels=new NativeImage(LocatorContourMask.SIZE,LocatorContourMask.SIZE,true);
        for(int y=0;y<LocatorContourMask.SIZE;y++)for(int x=0;x<LocatorContourMask.SIZE;x++)
            pixels.setPixel(x,y,contour[y*LocatorContourMask.SIZE+x]!=0?0xFFFFFFFF:0);
        //#if MC >= 12111
        Identifier id=Identifier.fromNamespaceAndPath(DDSTweakReference.MOD_ID,"locator_contour/"+Long.toHexString(++sequence));
        //#else
        //$$ ResourceLocation id=ResourceLocation.fromNamespaceAndPath(DDSTweakReference.MOD_ID,"locator_contour/"+Long.toHexString(++sequence));
        //#endif
        DynamicTexture texture=new DynamicTexture(()->"DDS Locator Heads outline",pixels);
        client.getTextureManager().register(id,texture);
        texture.upload();
        if(TRACE)uploads++;
        MASKS.put(key,new Entry(id,alpha));
        while(MASKS.size()>MAX_TEXTURES){
            Key eldest=MASKS.keySet().iterator().next();
            Entry removed=MASKS.remove(eldest);
            if(removed!=null)release(client,removed);
        }
        return id;
    }

    /** Recover cached player skins without downloading or reading GPU memory. */
    //#if MC >= 12111
    private static LocatorContourMask.SkinAlpha readRegisteredSkin(Minecraft client,Identifier skin){
    //#else
    //$$ private static LocatorContourMask.SkinAlpha readRegisteredSkin(Minecraft client,ResourceLocation skin){
    //#endif
        if(!(client.getTextureManager().getTexture(skin) instanceof DynamicTexture texture))return null;
        NativeImage pixels=texture.getPixels();
        if(pixels==null)return null;
        // The same pixels may be owned by the texture manager; never close them.
        try{
            accept(skin,pixels);
            var alpha=ALPHAS.get(skin.toString());
            if(TRACE&&alpha!=null)recovered++;
            return alpha;
        }catch(IllegalStateException ignored){
            // A texture can be replaced while it is being unloaded.
            return null;
        }
    }

    /**
     * Vanilla default skins live in resources instead of the download cache.
     * Never read a remote URL or GPU image here.
     */
    //#if MC >= 12111
    private static LocatorContourMask.SkinAlpha readBundledSkin(Minecraft client,Identifier skin){
    //#else
    //$$ private static LocatorContourMask.SkinAlpha readBundledSkin(Minecraft client,ResourceLocation skin){
    //#endif
        if(!skin.getPath().startsWith("textures/entity/player/"))return null;
        var resource=client.getResourceManager().getResource(skin);
        if(resource.isEmpty())return null;
        try(InputStream stream=resource.get().open();NativeImage image=NativeImage.read(stream)){
            accept(skin,image);
            var alpha=ALPHAS.get(skin.toString());
            if(TRACE&&alpha!=null)bundled++;
            return alpha;
        }catch(IOException|RuntimeException ignored){return null;}
    }

    public static void clear(){
        Minecraft client=Minecraft.getInstance();
        // Discard old CPU alpha immediately: a fresh download may arrive before
        // a resource-reload callback's client-thread cleanup is executed.
        int generation=reloadGeneration.incrementAndGet();
        // Built-in player skins may change with resource packs. Downloaded
        // skins can remain in the vanilla skin cache without being reloaded.
        ALPHAS.invalidateBundled();
        // Dynamic textures belong to the client thread. Do not clear ALPHAS
        // again in the delayed task, or freshly downloaded masks are lost.
        if(client.isSameThread())clearReloadedTextures(client,generation);
        else client.execute(()->clearReloadedTextures(client,generation));
    }
    private static void clearReloadedTextures(Minecraft client,int generation){
        // A newer resource reload supersedes any cleanup still in the queue.
        if(generation!=reloadGeneration.get())return;
        int previousMasks=MASKS.size();
        clearTextures(client);
        connection=client.getConnection();
        resourceManager=client.getResourceManager();
        appliedReloadGeneration=generation;
        if(TRACE)
            LOGGER.info("LocatorHeads reload: generation={}, previousMasks={}, cpuEntries={}, live={}, uploads={}, releases={}",
                    generation,previousMasks,ALPHAS.size(),MASKS.size(),uploads,releases);
    }
    private static void clearTextures(Minecraft client){
        for(Entry entry:MASKS.values())release(client,entry);
        MASKS.clear();
    }
    private static void release(Minecraft client,Entry entry){
        client.getTextureManager().release(entry.texture());
        if(TRACE)releases++;
    }
}
//#else
//$$ public final class LocatorContourCache { private LocatorContourCache(){} }
//#endif

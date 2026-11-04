package ddstweak.feature.locatorheads;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Short-lived HUD name transitions, separate from the waypoint renderer. */
public final class LocatorNameFades {
    private static final long DURATION_NANOS=150_000_000L;
    private static final int MAX_ENTRIES=512;
    private static final Map<UUID,Fade> FADES=new HashMap<>();
    private static Object connection;
    private static Object level;

    private LocatorNameFades(){}

    private static final class Fade {
        long start;
        boolean showing;
        Fade(long start,boolean showing){this.start=start;this.showing=showing;}
    }

    public static void initialize(){
        ClientTickEvents.END_CLIENT_TICK.register(LocatorNameFades::refresh);
    }

    private static void refresh(Minecraft client){
        if(connection!=client.getConnection()||level!=client.level||
                !LocatorHeadsFeature.enabled()&&!FADES.isEmpty()){
            FADES.clear();
            connection=client.getConnection();
            level=client.level;
        }
    }

    /** Eased visibility in [0,1], preserving progress when the target reverses. */
    public static float ease(UUID uuid,boolean visible,long now){
        Fade fade=FADES.get(uuid);
        if(fade==null){
            if(!visible)return 0f;
            if(FADES.size()>=MAX_ENTRIES)FADES.clear();
            fade=new Fade(now,true);
            FADES.put(uuid,fade);
        }else if(fade.showing!=visible){
            float fraction=fraction(now-fade.start);
            fade.start=now-(long)((1f-fraction)*DURATION_NANOS);
            fade.showing=visible;
        }
        float fraction=fraction(now-fade.start);
        if(!fade.showing&&fraction>=1f){
            FADES.remove(uuid);
            return 0f;
        }
        float progress=fade.showing?fraction:1f-fraction;
        return 1f-(1f-progress)*(1f-progress)*(1f-progress);
    }

    private static float fraction(long elapsed){
        return Math.min(1f,Math.max(0f,(float)elapsed/DURATION_NANOS));
    }

    public static void clear(){
        FADES.clear();
    }
}

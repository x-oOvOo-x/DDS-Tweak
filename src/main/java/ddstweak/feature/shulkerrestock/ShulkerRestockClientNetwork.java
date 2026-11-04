package ddstweak.feature.shulkerrestock;

import ddstweak.config.Configs;
import ddstweak.network.DdsClientNetwork;
import ddstweak.network.DdsClientSessionHooks;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.nbt.CompoundTag;
import java.util.List;

/**
 * Local preferences are not server authority. Only the current DDS connection's
 * validated status may report effective enablement.
 */
public final class ShulkerRestockClientNetwork {
    public static final String PREFERENCES_C2S = "shulker_restock_preferences_c2s";
    public static final String STATUS_S2C = "shulker_restock_status_s2c";
    private static final long SEND_COOLDOWN_NANOS = 400_000_000L;
    private static boolean initialized, effective, accepted;
    private static String serverMode = "unknown", lastSent = "";
    private static int serial, pendingSerial;
    private static long lastSentAt;

    private ShulkerRestockClientNetwork(){}

    public static synchronized void initialize(){
        if(initialized)return;
        initialized=true;
        DdsClientNetwork.registerC2S(PREFERENCES_C2S);
        DdsClientNetwork.registerS2CHandler(STATUS_S2C, ShulkerRestockClientNetwork::onStatus);
        DdsClientSessionHooks.registerResetHook(ShulkerRestockClientNetwork::reset);
    }

    private static void reset(){
        effective=false;accepted=false;serverMode="unknown";
        lastSent="";lastSentAt=0L;pendingSerial=0;
    }

    public static void onClientTick(){
        if(!DdsClientNetwork.canSend(PREFERENCES_C2S)){
            // Do not pretend an old or disconnected server honored an opt-in.
            effective=false;accepted=false;serverMode="unknown";
            return;
        }
        String current=configurationFingerprint();
        long now=System.nanoTime();
        if(current.equals(lastSent) || lastSentAt!=0 && now-lastSentAt<SEND_COOLDOWN_NANOS)return;
        CompoundTag payload=new CompoundTag();
        ShulkerRestockFilterMode mode=(ShulkerRestockFilterMode)Configs.Tweaks.SHULKER_RESTOCK_MODE.getOptionListValue();
        List<String> ids=selectedIds(mode);
        boolean valid=RestockPreferencePolicy.valid(ids);
        payload.putInt("request_id",++serial);
        payload.putInt("enabled",valid&&Configs.Tweaks.SHULKER_RESTOCK_ENABLED.getBooleanValue()?1:0);
        payload.putString("filter",mode.name());
        payload.putInt("threshold",Configs.Tweaks.SHULKER_RESTOCK_THRESHOLD.getIntegerValue());
        payload.putString("ids",valid?String.join(",",ids):"");
        if(!DdsClientNetwork.send(PREFERENCES_C2S,payload))return;
        lastSent=current;pendingSerial=serial;lastSentAt=now;
        accepted=false;effective=false;
    }

    private static String configurationFingerprint(){
        ShulkerRestockFilterMode mode=(ShulkerRestockFilterMode)Configs.Tweaks.SHULKER_RESTOCK_MODE.getOptionListValue();
        List<String> ids=selectedIds(mode);
        return Configs.Tweaks.SHULKER_RESTOCK_ENABLED.getBooleanValue()+"|"+mode.name()+"|"
                +Configs.Tweaks.SHULKER_RESTOCK_THRESHOLD.getIntegerValue()+"|"+String.join(",",ids);
    }

    private static List<String> selectedIds(ShulkerRestockFilterMode mode){
        List<String> selected=mode==ShulkerRestockFilterMode.BLACKLIST
                ? Configs.Tweaks.SHULKER_RESTOCK_BLACKLIST.getStrings()
                : Configs.Tweaks.SHULKER_RESTOCK_WHITELIST.getStrings();
        return RestockPreferencePolicy.effectiveIds(mode.name(),selected);
    }

    public static boolean hasInvalidList(){
        ShulkerRestockFilterMode mode=(ShulkerRestockFilterMode)Configs.Tweaks.SHULKER_RESTOCK_MODE.getOptionListValue();
        return !RestockPreferencePolicy.valid(selectedIds(mode));
    }

    private static void onStatus(CompoundTag data){
        int requestId=MinecraftCompat.intOr(data,"request_id",-1);
        if(requestId!=pendingSerial && requestId!=0)return;
        String mode=MinecraftCompat.stringOr(data,"server_mode","unknown");
        if(!"off".equals(mode)&&!"always".equals(mode)&&!"client".equals(mode))return;
        String previousMode=serverMode;
        serverMode=mode;
        accepted=MinecraftCompat.intOr(data,"accepted",0)!=0;
        effective=MinecraftCompat.intOr(data,"effective",0)!=0 && ("always".equals(mode) || accepted);
        // Do not resend unchanged rejected preferences forever.
        // Server mode transitions to CLIENT, config changes and connection resets reapply them.
        if(RestockPreferencePolicy.retryAfterModeChange(requestId,previousMode,mode))lastSent="";
    }

    public static String serverMode(){return serverMode;}
    public static boolean isEffective(){return effective && ("always".equals(serverMode) || !hasInvalidList())
            && DdsClientNetwork.canSend(PREFERENCES_C2S);}
    public static boolean isAccepted(){return accepted && DdsClientNetwork.canSend(PREFERENCES_C2S);}
    public static boolean isServerManaged(){return "off".equals(serverMode)||"always".equals(serverMode);}
}

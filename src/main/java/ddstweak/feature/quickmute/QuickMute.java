package ddstweak.feature.quickmute;

import ddstweak.config.Configs;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class QuickMute {
    private static final QuickMuteVolumeState VOLUME=new QuickMuteVolumeState();
    private static boolean appliedMuted;
    private static boolean initialized;

    private QuickMute(){}

    /** Internal recovery metadata, not a separate editable volume setting. */
    public static void loadState(JsonObject root){
        JsonElement section=root.get("QuickMute");
        if(section==null||!section.isJsonObject())return;
        JsonElement volume=section.getAsJsonObject().get("restoreVolume");
        if(volume==null||!volume.isJsonPrimitive()||!volume.getAsJsonPrimitive().isNumber())return;
        try{VOLUME.load(volume.getAsDouble());}catch(NumberFormatException ignored){}
    }

    public static void saveState(JsonObject root){
        if(!VOLUME.known())return;
        JsonObject section=new JsonObject();
        section.addProperty("restoreVolume",VOLUME.saved());
        root.add("QuickMute",section);
    }

    public static void initialize(){
        initialized=true;
        Configs.Tweaks.QUICK_MUTE.setValueChangeCallback(config -> {
            if(!Configs.isLoading())apply(config.getBooleanValue(),true);
        });
        Configs.Tweaks.QUICK_MUTE.getKeybind().setCallback((action,key) -> {
            Configs.Tweaks.QUICK_MUTE.toggleBooleanValue();
            return true;
        });
        if(Configs.Tweaks.QUICK_MUTE.getBooleanValue())apply(true,false);
    }

    public static void onConfigLoaded(){
        if(initialized)apply(Configs.Tweaks.QUICK_MUTE.getBooleanValue(),false);
    }

    private static void apply(boolean muted,boolean feedback){
        if(muted==appliedMuted)return;

        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft==null)return;

        if(muted){
            // Startup migration must also persist a newly recovered positive volume.
            VOLUME.prepareMute(MinecraftCompat.masterVolume(minecraft),!feedback,()->new Configs().save());
            MinecraftCompat.setMasterVolume(minecraft,0.0F);
            appliedMuted=true;
            if(feedback)MinecraftCompat.overlayMessage(minecraft,MinecraftCompat.translatable("dds-tweak.quick_mute.muted"));
            return;
        }

        float current=MinecraftCompat.masterVolume(minecraft);
        if(current<=0.0F)MinecraftCompat.setMasterVolume(minecraft,VOLUME.restore(current));
        else VOLUME.restore(current);
        appliedMuted=false;
        if(feedback&&!Configs.isLoading())new Configs().save();
        if(feedback)MinecraftCompat.overlayMessage(minecraft,MinecraftCompat.translatable("dds-tweak.quick_mute.restored",Math.round(MinecraftCompat.masterVolume(minecraft)*100.0F)+"%"));
    }
}

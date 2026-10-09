package ddstweak.feature.quickmute;

import ddstweak.config.Configs;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;

public final class QuickMute {
    private static float restoreVolume=1.0F;
    private static boolean appliedMuted;

    private QuickMute(){}

    public static void initialize(){
        Configs.Tweaks.QUICK_MUTE.setValueChangeCallback(config -> apply(config.getBooleanValue(),true));
        Configs.Tweaks.QUICK_MUTE.getKeybind().setCallback((action,key) -> {
            Configs.Tweaks.QUICK_MUTE.toggleBooleanValue();
            return true;
        });
        if(Configs.Tweaks.QUICK_MUTE.getBooleanValue())apply(true,false);
    }

    private static void apply(boolean muted,boolean feedback){
        if(muted==appliedMuted)return;

        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft==null)return;

        if(muted){
            restoreVolume=MinecraftCompat.masterVolume(minecraft);
            MinecraftCompat.setMasterVolume(minecraft,0.0F);
            appliedMuted=true;
            if(feedback)MinecraftCompat.overlayMessage(minecraft,MinecraftCompat.translatable("dds-tweak.quick_mute.muted"));
            return;
        }

        float current=MinecraftCompat.masterVolume(minecraft);
        if(current<=0.0F)MinecraftCompat.setMasterVolume(minecraft,restoreVolume);
        else restoreVolume=current;
        appliedMuted=false;
        if(feedback)MinecraftCompat.overlayMessage(minecraft,MinecraftCompat.translatable("dds-tweak.quick_mute.restored",Math.round(MinecraftCompat.masterVolume(minecraft)*100.0F)+"%"));
    }
}

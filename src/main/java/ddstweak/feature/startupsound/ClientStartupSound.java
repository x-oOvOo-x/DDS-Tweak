package ddstweak.feature.startupsound;

import ddstweak.config.Configs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class ClientStartupSound {
    private static boolean pending=true;

    private ClientStartupSound(){}

    public static void initialize(){
        ClientTickEvents.END_CLIENT_TICK.register(ClientStartupSound::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft){
        if(!pending)return;
        if(!Configs.Tweaks.CLIENT_STARTUP_SOUND.getBooleanValue()){
            pending=false;
            return;
        }
        if(minecraft.getSoundManager().getAvailableSounds().isEmpty())return;

        pending=false;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP,1.0F));
    }
}

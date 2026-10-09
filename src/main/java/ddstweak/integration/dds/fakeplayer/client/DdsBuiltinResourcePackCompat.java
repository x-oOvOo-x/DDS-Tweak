package ddstweak.integration.dds.fakeplayer.client;

import ddstweak.DDSTweak;
import ddstweak.platform.minecraft.MinecraftCompat;
//#if MC >= 260100
//$$ import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
//$$ import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
//#elseif MC >= 11600
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
//#endif
import net.fabricmc.loader.api.FabricLoader;

public final class DdsBuiltinResourcePackCompat {
    private DdsBuiltinResourcePackCompat(){}
    @SuppressWarnings("deprecation")
    public static void register(){
        //#if MC >= 260100
        //$$ ResourceLoader.registerBuiltinPack(
        //$$         MinecraftCompat.id(DDSTweak.MOD_ID,"dds_fake_player_ui"),
        //$$         FabricLoader.getInstance().getModContainer(DDSTweak.MOD_ID).orElseThrow(() -> new IllegalStateException("DDS Tweak mod container is unavailable")),
        //$$         PackActivationType.NORMAL);
        //#elseif MC >= 11600
        ResourceManagerHelper.registerBuiltinResourcePack(
                MinecraftCompat.id(DDSTweak.MOD_ID,"dds_fake_player_ui"),
                FabricLoader.getInstance().getModContainer(DDSTweak.MOD_ID).orElseThrow(() -> new IllegalStateException("DDS Tweak mod container is unavailable")),
                ResourcePackActivationType.NORMAL);
        //#else
        // UI assets are flattened into the mod's root assets by common.gradle.
        //#endif
    }
}

package ddstweak;

//#if MC >= 12101
import ddstweak.gui.DDSTweakConfigScreen;
import fi.dy.masa.malilib.util.data.ModInfo;
//#endif
import net.fabricmc.loader.api.FabricLoader;

public final class DDSTweakReference {
    public static final String MOD_ID="dds-tweak",MOD_NAME="DDS Tweak";
    //#if MC >= 12101
    public static final ModInfo MOD_INFO=new ModInfo(MOD_ID,MOD_NAME,DDSTweakConfigScreen::new);
    //#endif
    public static final String MOD_VERSION=FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(() -> new IllegalStateException("DDS Tweak mod container is unavailable")).getMetadata().getVersion().getFriendlyString();
    private DDSTweakReference(){}
}

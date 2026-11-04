package ddstweak.integration.modmenu;

import ddstweak.DDSTweakReference;
import ddstweak.gui.DDSTweakConfigScreen;
import net.minecraft.client.gui.screens.Screen;

//#if MC >= 11605
import com.terraformersmc.modmenu.api.ModMenuApi;
//#else
//$$ import io.github.prospector.modmenu.api.ModMenuApi;
//#endif
//#if MC >= 11502
//#if MC >= 11605
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
//#else
//$$ import io.github.prospector.modmenu.api.ConfigScreenFactory;
//#endif
//#else
//$$ import java.util.function.Function;
//#endif

/** Optional Mod Menu adapter; the existing MaLiLib config screen remains the owner. */
public final class DDSTweakModMenuIntegration implements ModMenuApi {
    //#if MC >= 11502
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DDSTweakModMenuIntegration::createConfigScreen;
    }
    //#else
    //$$ @Override
    //$$ public Function<Screen, ? extends Screen> getConfigScreenFactory() {
    //$$     return DDSTweakModMenuIntegration::createConfigScreen;
    //$$ }
    //$$ @Override
    //$$ public String getModId() {
    //$$     return DDSTweakReference.MOD_ID;
    //$$ }
    //#endif

    private static DDSTweakConfigScreen createConfigScreen(Screen parent) {
        DDSTweakConfigScreen screen = new DDSTweakConfigScreen();
        screen.setParent(parent);
        return screen;
    }
}

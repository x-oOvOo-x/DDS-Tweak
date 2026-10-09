package ddstweak.config;

import com.google.common.collect.ImmutableList;
import ddstweak.DDSTweakReference;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.hotkeys.*;

public final class DDSTweakHotkeyProvider implements IKeybindProvider {
    public static final DDSTweakHotkeyProvider INSTANCE=new DDSTweakHotkeyProvider();
    private DDSTweakHotkeyProvider(){}

    private static ImmutableList<IHotkey> allHotkeys(){
        ImmutableList.Builder<IHotkey> out=ImmutableList.builder();
        for(IConfigBase config:Configs.ALL)if(config instanceof IHotkey)out.add((IHotkey)config);
        return out.build();
    }

    @Override public void addKeysToMap(IKeybindManager m){
        for(IHotkey hotkey:allHotkeys())m.addKeybindToMap(hotkey.getKeybind());
    }

    @Override public void addHotkeys(IKeybindManager m){
        m.addHotkeysForCategory(DDSTweakReference.MOD_NAME,"dds-tweak.hotkeys.category.general",allHotkeys());
    }
}

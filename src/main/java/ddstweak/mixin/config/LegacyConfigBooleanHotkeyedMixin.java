package ddstweak.mixin.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Backports the fixed ConfigBooleanHotkeyed persistence/reset contract from MaLiLib 0.11.4.
 * The mixin is filtered out at runtime on Minecraft 1.18+.
 */
@Mixin(value=ConfigBooleanHotkeyed.class,remap=false)
public abstract class LegacyConfigBooleanHotkeyedMixin extends ConfigBoolean implements IHotkeyTogglable {
    @Shadow @Final protected IKeybind keybind;

    protected LegacyConfigBooleanHotkeyedMixin(String name,boolean defaultValue,String comment){
        super(name,defaultValue,comment);
    }

    @Override public boolean isModified(){
        return super.isModified()||keybind.isModified();
    }

    @Override public void resetToDefault(){
        super.resetToDefault();
        keybind.resetToDefault();
    }

    @Override public void setValueFromJsonElement(JsonElement element){
        if(!element.isJsonObject()){
            super.setValueFromJsonElement(element);
            return;
        }

        JsonObject object=element.getAsJsonObject();
        JsonElement enabled=object.get("enabled");
        if(enabled!=null&&enabled.isJsonPrimitive())super.setValueFromJsonElement(enabled);

        JsonElement hotkey=object.get("hotkey");
        if(hotkey!=null&&hotkey.isJsonObject()){
            JsonObject value=hotkey.getAsJsonObject();
            JsonElement keys=value.get("keys");
            if(keys!=null&&keys.isJsonPrimitive())keybind.setValueFromString(keys.getAsString());
            JsonElement settings=value.get("settings");
            if(settings!=null&&settings.isJsonObject())keybind.setSettings(KeybindSettings.fromJson(settings.getAsJsonObject()));
        }
    }

    @Override public JsonElement getAsJsonElement(){
        JsonObject object=new JsonObject();
        object.add("enabled",super.getAsJsonElement());

        JsonObject hotkey=new JsonObject();
        hotkey.add("keys",new JsonPrimitive(keybind.getStringValue()));
        if(keybind.areSettingsModified())hotkey.add("settings",keybind.getSettings().toJson());
        object.add("hotkey",hotkey);
        return object;
    }
}

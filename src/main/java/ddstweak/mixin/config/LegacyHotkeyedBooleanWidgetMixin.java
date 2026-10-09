package ddstweak.mixin.config;

import ddstweak.gui.DDSTweakConfigScreen;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.gui.ConfigOptionChangeListenerButton;
import fi.dy.masa.malilib.config.gui.ConfigOptionChangeListenerKeybind;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonBoolean;
import fi.dy.masa.malilib.gui.button.ConfigButtonKeybind;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOptionBase;
import fi.dy.masa.malilib.gui.widgets.WidgetKeybindSettings;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import java.util.Objects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backports MaLiLib 0.11.4's compact ConfigBooleanHotkeyed row to legacy MaLiLib.
 * The mixin is filtered out at runtime on Minecraft 1.18+.
 */
@Mixin(value=WidgetConfigOption.class,remap=false)
public abstract class LegacyHotkeyedBooleanWidgetMixin extends WidgetConfigOptionBase<GuiConfigsBase.ConfigOptionWrapper> {
    @Shadow @Final protected IKeybindConfigGui host;
    @Shadow @Final protected GuiConfigsBase.ConfigOptionWrapper wrapper;
    @Shadow @Mutable @Final protected KeybindSettings initialKeybindSettings;
    @Unique private boolean ddstweak$initialBoolean;

    protected LegacyHotkeyedBooleanWidgetMixin(int x,int y,int width,int height,
            WidgetListConfigOptionsBase<?,?> parent,GuiConfigsBase.ConfigOptionWrapper entry,int listIndex){
        super(x,y,width,height,parent,entry,listIndex);
    }

    @Unique private boolean ddstweak$isOwnConfigScreen(){
        return host instanceof DDSTweakConfigScreen;
    }

    @Inject(method="<init>",at=@At("TAIL"))
    private void ddstweak$captureHotkeyedBoolean(CallbackInfo ci){
        if(!ddstweak$isOwnConfigScreen()||wrapper.getType()!=GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG)return;
        IConfigBase config=wrapper.getConfig();
        if(config instanceof ConfigBooleanHotkeyed){
            ConfigBooleanHotkeyed option=(ConfigBooleanHotkeyed)config;
            ddstweak$initialBoolean=option.getBooleanValue();
            initialStringValue=option.getKeybind().getStringValue();
            initialKeybindSettings=option.getKeybind().getSettings();
        }
    }

    @Inject(
            method="addConfigOption",
            at=@At(value="FIELD",target="Lfi/dy/masa/malilib/config/ConfigType;BOOLEAN:Lfi/dy/masa/malilib/config/ConfigType;"),
            cancellable=true
    )
    private void ddstweak$renderHotkeyedBoolean(int x,int y,float zLevel,int labelWidth,int configWidth,
            IConfigBase config,CallbackInfo ci){
        if(!ddstweak$isOwnConfigScreen()||!(config instanceof ConfigBooleanHotkeyed))return;
        ConfigBooleanHotkeyed option=(ConfigBooleanHotkeyed)config;
        ddstweak$addBooleanAndHotkeyWidgets(x,y,configWidth,option);
        ci.cancel();
    }

    @Unique private void ddstweak$addBooleanAndHotkeyWidgets(int x,int y,int configWidth,IHotkeyTogglable config){
        IKeybind keybind=config.getKeybind();
        int booleanWidth=(configWidth-24)/2;
        ConfigButtonBoolean booleanButton=new ConfigButtonBoolean(x,y,booleanWidth,20,config);
        x+=booleanWidth+2;
        configWidth-=booleanWidth+24;

        ConfigButtonKeybind hotkeyButton=new ConfigButtonKeybind(x,y,configWidth,20,keybind,host);
        x+=configWidth+2;
        addWidget(new WidgetKeybindSettings(x,y,20,20,keybind,config.getName(),parent,host.getDialogHandler()));
        //#if MC >= 11800
        x+=22;
        //#else
        //$$ x+=24;
        //#endif

        ButtonGeneric resetButton=createResetButton(x,y,config);
        ConfigOptionChangeListenerButton booleanListener=new ConfigOptionChangeListenerButton(config,resetButton,null);
        HotkeyedResetListener resetListener=new HotkeyedResetListener(config,booleanButton,hotkeyButton,resetButton,host);
        //#if MC >= 11800
        host.addKeybindChangeListener(resetListener::updateButtons);
        //#else
        //$$ host.addKeybindChangeListener(resetListener);
        //#endif

        addButton(booleanButton,booleanListener);
        addButton(hotkeyButton,host.getButtonPressListener());
        addButton(resetButton,resetListener);
    }

    @Inject(
            method="wasConfigModified",
            at=@At(value="INVOKE",target="Lfi/dy/masa/malilib/gui/GuiConfigsBase$ConfigOptionWrapper;getConfig()Lfi/dy/masa/malilib/config/IConfigBase;",ordinal=0),
            cancellable=true
    )
    private void ddstweak$hotkeyedBooleanModified(CallbackInfoReturnable<Boolean> cir){
        if(!ddstweak$isOwnConfigScreen()||wrapper.getType()!=GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG)return;
        IConfigBase config=wrapper.getConfig();
        if(config instanceof ConfigBooleanHotkeyed){
            ConfigBooleanHotkeyed option=(ConfigBooleanHotkeyed)config;
            IKeybind keybind=option.getKeybind();
            cir.setReturnValue(
                    ddstweak$initialBoolean!=option.getBooleanValue()||
                    !Objects.equals(initialStringValue,keybind.getStringValue())||
                    !Objects.equals(initialKeybindSettings,keybind.getSettings())
            );
        }
    }

    @Unique
    private static final class HotkeyedResetListener extends ConfigOptionChangeListenerKeybind {
        private final IHotkeyTogglable config;
        private final ButtonGeneric booleanButton;
        private final ConfigButtonKeybind hotkeyButton;
        private final ButtonGeneric resetButton;
        private final IKeybindConfigGui host;

        HotkeyedResetListener(IHotkeyTogglable config,ButtonGeneric booleanButton,
                ConfigButtonKeybind hotkeyButton,ButtonGeneric resetButton,IKeybindConfigGui host){
            super(config.getKeybind(),hotkeyButton,booleanButton,host);
            this.config=config;
            this.booleanButton=booleanButton;
            this.hotkeyButton=hotkeyButton;
            this.resetButton=resetButton;
            this.host=host;
        }

        @Override public void actionPerformedWithButton(ButtonBase button,int mouseButton){
            config.resetToDefault();
            host.getButtonPressListener().actionPerformedWithButton(button,mouseButton);
            updateButtons();
        }

        @Override public void updateButtons(){
            booleanButton.updateDisplayString();
            hotkeyButton.updateDisplayString();
            resetButton.setEnabled(config.isModified());
        }
    }
}

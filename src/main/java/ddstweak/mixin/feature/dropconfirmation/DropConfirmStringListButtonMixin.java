package ddstweak.mixin.feature.dropconfirmation;

import ddstweak.gui.itempicker.ItemPickerConfigBindings;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.button.ConfigButtonStringList;
import fi.dy.masa.malilib.util.GuiUtils;
//#if MC >= 12109
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Opens the item picker for drop-confirmation list settings.
// 为丢弃确认名单配置打开物品选择器。
@Mixin(value=ConfigButtonStringList.class,remap=false)
public abstract class DropConfirmStringListButtonMixin {
    @Shadow @Final private IConfigStringList config;

    //#if MC >= 12109
    @Inject(method="onMouseClickedImpl",at=@At("HEAD"),cancellable=true,remap=false)
    private void ddstweak$openItemPicker(MouseButtonEvent click,boolean doubleClick,CallbackInfoReturnable<Boolean> cir){openPicker(cir);}
    //#else
    //$$ @Inject(method="onMouseClickedImpl",at=@At("HEAD"),cancellable=true,remap=false)
    //$$ private void ddstweak$openItemPicker(int mouseX,int mouseY,int mouseButton,CallbackInfoReturnable<Boolean> cir){openPicker(cir);}
    //#endif

    private void openPicker(CallbackInfoReturnable<Boolean> cir){
        if(ItemPickerConfigBindings.open(GuiUtils.getCurrentScreen(),config))cir.setReturnValue(true);
    }
}

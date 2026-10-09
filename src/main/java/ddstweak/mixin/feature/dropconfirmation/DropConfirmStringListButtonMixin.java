package ddstweak.mixin.feature.dropconfirmation;

import ddstweak.feature.dropconfirmation.DropConfirmationItemScreen;
import ddstweak.platform.minecraft.MinecraftCompat;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.button.ConfigButtonStringList;
import fi.dy.masa.malilib.util.GuiUtils;
import net.minecraft.client.Minecraft;
//#if MC >= 12109
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        String name=config.getName(),title;
        DropConfirmationItemScreen.Kind kind;
        if("autoEatFoodBlacklist".equals(name)){
            title="dds-tweak.config.tweaks.prettyName.autoEatFoodBlacklist";kind=DropConfirmationItemScreen.Kind.FOOD;
        }else if("dropConfirmBlacklist".equals(name)){
            title="dds-tweak.config.tweaks.prettyName.dropConfirmBlacklist";kind=DropConfirmationItemScreen.Kind.ITEM;
        }else if("dropConfirmWhitelist".equals(name)){
            title="dds-tweak.config.tweaks.prettyName.dropConfirmWhitelist";kind=DropConfirmationItemScreen.Kind.ITEM;
        //#if MC >= 12111
        }else if("betterSnowCoverageBlacklist".equals(name)){
            title="dds-tweak.config.visual.prettyName.betterSnowCoverageBlacklist";kind=DropConfirmationItemScreen.Kind.BLOCK;
        }else if("betterSnowCoverageWhitelist".equals(name)){
            title="dds-tweak.config.visual.prettyName.betterSnowCoverageWhitelist";kind=DropConfirmationItemScreen.Kind.BLOCK;
        //#endif
        }else return;
        MinecraftCompat.setScreen(Minecraft.getInstance(),new DropConfirmationItemScreen(GuiUtils.getCurrentScreen(),config,kind,title));
        cir.setReturnValue(true);
    }
}

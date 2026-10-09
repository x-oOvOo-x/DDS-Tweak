package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=AbstractContainerScreen.class,priority=1100)
public abstract class ContainerScreenInputMixin {
    //#if MC >= 12109
    @Inject(method="mouseClicked",at=@At("HEAD"))
    private void ddstweak$silentAutoEatContainerClick(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir){
        ddstweak$cancelAutoEat();
    }

    @Inject(method="keyPressed",at=@At("HEAD"))
    private void ddstweak$silentAutoEatContainerKey(KeyEvent event,CallbackInfoReturnable<Boolean> cir){
        ddstweak$cancelAutoEat();
    }
    //#else
    //$$ @Inject(method="mouseClicked",at=@At("HEAD"))
    //$$ private void ddstweak$silentAutoEatContainerClick(double x,double y,int button,CallbackInfoReturnable<Boolean> cir){
    //$$     ddstweak$cancelAutoEat();
    //$$ }
    //$$
    //$$ @Inject(method="keyPressed",at=@At("HEAD"))
    //$$ private void ddstweak$silentAutoEatContainerKey(int key,int scan,int modifiers,CallbackInfoReturnable<Boolean> cir){
    //$$     ddstweak$cancelAutoEat();
    //$$ }
    //#endif

    //#if MC >= 12002
    @Inject(method="mouseScrolled",at=@At("HEAD"))
    private void ddstweak$silentAutoEatContainerScroll(double x,double y,double horizontal,double vertical,CallbackInfoReturnable<Boolean> cir){
        ddstweak$cancelAutoEat();
    }
    //#else
    //$$ @Inject(method="mouseScrolled",at=@At("HEAD"))
    //$$ private void ddstweak$silentAutoEatContainerScroll(double x,double y,double amount,CallbackInfoReturnable<Boolean> cir){
    //$$     ddstweak$cancelAutoEat();
    //$$ }
    //#endif

    private static void ddstweak$cancelAutoEat(){
        SilentAutoEat.onExplicitPlayerAction(Minecraft.getInstance());
    }
}

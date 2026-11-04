package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=AbstractContainerScreen.class,priority=1100)
public abstract class ContainerScreenInputMixin {
    @Shadow protected int leftPos,topPos;
    //#if MC >= 12109
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void ddstweak$silentAutoEatContainerClick(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir){
        boolean guard=ddstweak$guardSourceClick(event.x(),event.y());
        ddstweak$cancelAutoEat();
        if(guard)cir.setReturnValue(true);
    }

    @Inject(method="keyPressed",at=@At("HEAD"))
    private void ddstweak$silentAutoEatContainerKey(KeyEvent event,CallbackInfoReturnable<Boolean> cir){
        ddstweak$cancelAutoEat();
    }
    //#else
    //$$ @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$silentAutoEatContainerClick(double x,double y,int button,CallbackInfoReturnable<Boolean> cir){
    //$$     boolean guard=ddstweak$guardSourceClick(x,y);
    //$$     ddstweak$cancelAutoEat();
    //$$     if(guard)cir.setReturnValue(true);
    //$$ }
    //$$
    //$$ @Inject(method="keyPressed",at=@At("HEAD"))
    //$$ private void ddstweak$silentAutoEatContainerKey(int key,int scan,int modifiers,CallbackInfoReturnable<Boolean> cir){
    //$$     ddstweak$cancelAutoEat();
    //$$ }
    //#endif

    // AbstractContainerScreen declares mouseScrolled itself only from MC 1.21.2;
    // earlier versions inherit the interface default, which Mixin cannot inject here.
    //#if MC >= 12102
    @Inject(method="mouseScrolled",at=@At("HEAD"))
    private void ddstweak$silentAutoEatContainerScroll(double x,double y,double horizontal,double vertical,CallbackInfoReturnable<Boolean> cir){
        ddstweak$cancelAutoEat();
    }
    //#endif

    /**
     * Resolve the clicked menu slot before the input can touch its real stack.
     * QCA/custom menus are not automation contexts; only the player's own
     * inventory's temporary SWAP slots can be temporarily guarded.
     */
    private boolean ddstweak$guardSourceClick(double mouseX,double mouseY){
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player==null||minecraft.player.containerMenu==null)return false;
        double x=mouseX-leftPos,y=mouseY-topPos;
        for(Slot slot:minecraft.player.containerMenu.slots)
            if(x>=slot.x&&x<slot.x+16&&y>=slot.y&&y<slot.y+16)
                return SilentAutoEat.shouldGuardSlotClick(minecraft,slot);
        return false;
    }

    private static void ddstweak$cancelAutoEat(){
        SilentAutoEat.onExplicitPlayerAction(Minecraft.getInstance());
    }
}

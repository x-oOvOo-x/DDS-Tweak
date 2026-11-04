package ddstweak.mixin.integration.dds.qca;

import ddstweak.integration.dds.qca.*;
import ddstweak.integration.dds.qca.DdsQcaMouseInputBridge;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.playerinventory.PlayerInventoryOpenContext;
import ddstweak.platform.minecraft.MinecraftCompat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
//#if MC >= 12109
import net.minecraft.client.input.*;
//#endif
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenInputMixin implements DdsQcaMouseInputBridge {
    @Shadow @Final protected AbstractContainerMenu menu;@Shadow protected int leftPos;@Shadow protected int topPos;
    @Unique private boolean ddsTweak$storageRightDown,ddsTweak$creativeStorageGesture;@Unique private DdsQcaClient.StorageDragMode ddsTweak$storageDragMode;@Unique private final Set<Integer> ddsTweak$visitedStorageSlots=new HashSet<>();
    @Unique private static final boolean ddsTweak$TRACE=Boolean.getBoolean("dds.qca.drag.trace");
    @Unique private static final org.apache.logging.log4j.Logger ddsTweak$LOG=
            org.apache.logging.log4j.LogManager.getLogger("dds.qca.drag");
    @Unique private int ddsTweak$dragCallbacks,ddsTweak$dragSent,ddsTweak$lastTraceSlot=Integer.MIN_VALUE;
    @Unique private void ddsTweak$trace(String stage,int slot,String detail){
        if(ddsTweak$TRACE)ddsTweak$LOG.info("qca_drag stage={} screen={} menu={} id={} slot={} mode={} callbacks={} sends={} {}",
                stage,getClass().getSimpleName(),menu.getClass().getSimpleName(),menu.containerId,slot,
                ddsTweak$storageDragMode,ddsTweak$dragCallbacks,ddsTweak$dragSent,detail);
    }

    //#if MC >= 11602
    @Inject(method="onClose",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$navigateBackOnClose(CallbackInfo ci){if(DdsQcaClientNetwork.sendNavigateBack(menu))ci.cancel();}
    //#endif
    //#if MC >= 12109
    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$navigateBackOnEscape(KeyEvent event,CallbackInfoReturnable<Boolean> cir){if(event.key()==InputConstants.KEY_ESCAPE&&DdsQcaClientNetwork.sendNavigateBack(menu))cir.setReturnValue(true);}
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$mouseClicked(MouseButtonEvent event,boolean doubled,CallbackInfoReturnable<Boolean> cir){if(ddsTweak$handleMouseClicked(event.x(),event.y(),event.button()))cir.setReturnValue(true);}
    @Inject(method="mouseDragged",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$mouseDragged(MouseButtonEvent event,double dragX,double dragY,CallbackInfoReturnable<Boolean> cir){if(ddsTweak$handleMouseDragged(event.x(),event.y(),event.button()))cir.setReturnValue(true);}
    @Inject(method="mouseReleased",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$mouseReleased(MouseButtonEvent event,CallbackInfoReturnable<Boolean> cir){if(ddsTweak$handleMouseReleased(event.button()))cir.setReturnValue(true);}
    //#else
    //$$ @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    //$$ private void ddsTweak$navigateBackOnEscape(int key,int scan,int modifiers,CallbackInfoReturnable<Boolean> cir){
    //#if MC >= 11700
    //$$     if(key==InputConstants.KEY_ESCAPE&&DdsQcaClientNetwork.sendNavigateBack(menu))cir.setReturnValue(true);
    //#else
    //$$     if(key==256&&DdsQcaClientNetwork.sendNavigateBack(menu))cir.setReturnValue(true);
    //#endif
    //$$ }
    //$$ @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    //$$ private void ddsTweak$mouseClicked(double x,double y,int button,CallbackInfoReturnable<Boolean> cir){if(ddsTweak$handleMouseClicked(x,y,button))cir.setReturnValue(true);}
    //$$ @Inject(method="mouseDragged",at=@At("HEAD"),cancellable=true)
    //$$ private void ddsTweak$mouseDragged(double x,double y,int button,double dragX,double dragY,CallbackInfoReturnable<Boolean> cir){if(ddsTweak$handleMouseDragged(x,y,button))cir.setReturnValue(true);}
    //$$ @Inject(method="mouseReleased",at=@At("HEAD"),cancellable=true)
    //$$ private void ddsTweak$mouseReleased(double x,double y,int button,CallbackInfoReturnable<Boolean> cir){if(ddsTweak$handleMouseReleased(button))cir.setReturnValue(true);}
    //#endif

    @Override public boolean ddstweak$qcaMouseClicked(double x,double y,int button){return ddsTweak$handleMouseClicked(x,y,button);}
    @Override public boolean ddstweak$qcaMouseDragged(double x,double y,int button){return ddsTweak$handleMouseDragged(x,y,button);}
    @Override public boolean ddstweak$qcaMouseReleased(int button){return ddsTweak$handleMouseReleased(button);}

    @Unique private boolean ddsTweak$handleMouseClicked(double mouseX,double mouseY,int button){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return false;boolean creative=(Object)this instanceof CreativeModeInventoryScreen;
        if(creative&&DdsQcaClientNetwork.creativeStoragePending(menu))return true;if(ddsTweak$isRightMouseButton(button))ddsTweak$resetGesture();if(!ddsTweak$isRightMouseButton(button))return false;
        ItemStack carried=MinecraftCompat.carried(menu,mc.player);Slot slot=ddsTweak$getSlot(mouseX,mouseY);if(creative&&DdsQcaClient.isCreativePickerScreen())return false;
        if(carried.isEmpty()){if(creative){int i=DdsQcaClient.creativeInventoryOpenSlot(slot,mc.player);if(i>=0&&DdsQcaClientNetwork.sendOpenInventorySlot(i))return true;}else if(DdsQcaClient.canUseDedicatedOpenSlot(menu,slot,mc.player)){int id=menu.slots.indexOf(slot);if(id>=0&&DdsQcaClientNetwork.sendOpenSlot(menu,id)){if(menu instanceof UnifiedPlayerInventoryMirrorMenu&&DdsQcaItems.kindOf(slot.getItem())==DdsQcaItems.MenuKind.ENDER_CHEST)PlayerInventoryOpenContext.expectEnder(((UnifiedPlayerInventoryMirrorMenu)menu).context());return true;}}}
        if(creative){int i=DdsQcaClient.creativeInventoryStorageSlot(slot,mc.player,carried);if(i<0)return false;DdsQcaClientNetwork.beginCreativeStorageGesture();if(!DdsQcaClientNetwork.sendCreativeStorageSlot(i,mc.player,menu,carried))return false;if(DdsQcaItems.isStorageHostCandidate(carried)){ddsTweak$creativeStorageGesture=ddsTweak$storageRightDown=true;ddsTweak$storageDragMode=slot.getItem().isEmpty()?DdsQcaClient.StorageDragMode.EXTRACT:DdsQcaClient.StorageDragMode.ABSORB;ddsTweak$visitedStorageSlots.add(i);}return true;}
        if(carried.isEmpty()&&slot!=null&&DdsQcaItems.kindOf(slot.getItem())==DdsQcaItems.MenuKind.SHULKER)
            DdsQcaClientNetwork.rememberNativeNestedEnder(menu);
        if(!DdsQcaClientNetwork.canUseStorageClick()||!DdsQcaClient.canUseDedicatedStorageInteraction(menu,slot,mc.player,carried))return false;int id=menu.slots.indexOf(slot);if(id<0||!DdsQcaClientNetwork.sendStorageClick(menu.containerId,id))return false;ddsTweak$storageRightDown=true;ddsTweak$storageDragMode=slot.getItem().isEmpty()?DdsQcaClient.StorageDragMode.EXTRACT:DdsQcaClient.StorageDragMode.ABSORB;ddsTweak$visitedStorageSlots.add(id);ddsTweak$trace("click",id,"storage_click=sent");return true;
    }
    @Unique private boolean ddsTweak$handleMouseDragged(double mouseX,double mouseY,int button){
        if(ddsTweak$storageRightDown){
            ddsTweak$dragCallbacks++;
            if(ddsTweak$TRACE&&ddsTweak$dragCallbacks==1)
                ddsTweak$trace("first_drag",-1,"button="+button+" right="+ddsTweak$isRightMouseButton(button));
        }
        if(!ddsTweak$storageRightDown||!ddsTweak$isRightMouseButton(button))return false;if(ddsTweak$storageDragMode==null)return true;Minecraft mc=Minecraft.getInstance();if(mc.player==null){ddsTweak$resetGesture();return false;}ItemStack carried=MinecraftCompat.carried(menu,mc.player);Slot slot=ddsTweak$getSlot(mouseX,mouseY);
        if(ddsTweak$creativeStorageGesture){if(!DdsQcaClientNetwork.canUseCreativeStorage()||!DdsQcaClient.isCreativeSurvivalScreen()){ddsTweak$resetGesture();return false;}int i=DdsQcaClient.creativeInventoryStorageSlot(slot,mc.player,carried);if(i<0||ddsTweak$visitedStorageSlots.contains(i))return true;boolean empty=slot.getItem().isEmpty();if(ddsTweak$storageDragMode==DdsQcaClient.StorageDragMode.ABSORB?empty:!empty)return true;if(DdsQcaClientNetwork.sendCreativeStorageSlot(i,mc.player,menu,carried))ddsTweak$visitedStorageSlots.add(i);return true;}
        if(!DdsQcaClientNetwork.canUseStorageClick()){ddsTweak$resetGesture();return false;}boolean eligible=DdsQcaClient.canUseDedicatedStorageInteraction(menu,slot,mc.player,carried);int id=menu.slots.indexOf(slot);if(ddsTweak$TRACE&&ddsTweak$lastTraceSlot!=id){ddsTweak$lastTraceSlot=id;ddsTweak$trace("drag_slot",id,"eligible="+eligible+" empty="+(slot==null?"n/a":slot.getItem().isEmpty())+" visited="+ddsTweak$visitedStorageSlots.contains(id));}if(!eligible)return true;boolean empty=slot.getItem().isEmpty();if(ddsTweak$storageDragMode==DdsQcaClient.StorageDragMode.ABSORB?empty:!empty)return true;if(id<0||ddsTweak$visitedStorageSlots.contains(id))return true;if(DdsQcaClientNetwork.sendStorageClick(menu.containerId,id)){ddsTweak$visitedStorageSlots.add(id);ddsTweak$dragSent++;ddsTweak$trace("drag_sent",id,"storage_click=sent");}else ddsTweak$trace("drag_rejected",id,"storage_click=not_sent");return true;
    }
    @Unique private boolean ddsTweak$handleMouseReleased(int button){if(!ddsTweak$storageRightDown||!ddsTweak$isRightMouseButton(button))return false;ddsTweak$trace("release",-1,"button="+button+" visited="+ddsTweak$visitedStorageSlots.size());ddsTweak$resetGesture();return true;}
    @Unique private void ddsTweak$resetGesture(){ddsTweak$storageRightDown=ddsTweak$creativeStorageGesture=false;ddsTweak$storageDragMode=null;ddsTweak$visitedStorageSlots.clear();ddsTweak$dragCallbacks=ddsTweak$dragSent=0;ddsTweak$lastTraceSlot=Integer.MIN_VALUE;}
    /** SDL 26.3+ uses mouse button 1 for left and 3 for right; older versions use GLFW's 0/1. */
    @Unique private static boolean ddsTweak$isRightMouseButton(int button) {
        //#if MC >= 260300
        //$$ return button == InputConstants.MOUSE_BUTTON_RIGHT;
        //#else
        return button == 1;
        //#endif
    }
    @Unique private Slot ddsTweak$getSlot(double mouseX,double mouseY){double x=mouseX-leftPos,y=mouseY-topPos;for(Slot slot:menu.slots)if(x>=slot.x&&x<slot.x+16&&y>=slot.y&&y<slot.y+16)return slot;return null;}
}

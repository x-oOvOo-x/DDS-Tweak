package ddstweak.integration.ommc;

import ddstweak.integration.dds.playerinventory.PlayerInventorySortRanges;
import ddstweak.integration.dds.playerinventory.PlayerInventorySlotPolicy;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.mixin.fakeplayer.AbstractContainerScreenAccessor;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
//#if MC < 260200
import net.minecraft.util.Tuple;
//#endif
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared fail-closed semantics for both OMMC package layouts; never performs item clicks. */
public final class OmmcSortCompatibility {
    private static final org.apache.logging.log4j.Logger LOG=
            org.apache.logging.log4j.LogManager.getLogger("ddstweak.ommc");
    private static final AtomicBoolean WARNED_UNSUPPORTED=new AtomicBoolean();
    private OmmcSortCompatibility(){}

    public static void unsupported(CallbackInfoReturnable<Runnable> cir){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        Object current=MinecraftCompat.screen(mc);
        if((current instanceof AbstractContainerScreen<?>
                &&((AbstractContainerScreen<?>)current).getMenu() instanceof UnifiedPlayerInventoryMirrorMenu)
                ||mc.player.containerMenu instanceof UnifiedPlayerInventoryMirrorMenu){
            if(WARNED_UNSUPPORTED.compareAndSet(false,true))
                LOG.warn("OMMC sorting disabled for unsupported sorter signature in DT mirror menu");
            cir.setReturnValue(null);
        }
    }

    //#if MC < 260200
    public static void range(AbstractContainerMenu handler,Slot hovered,
                             CallbackInfoReturnable<Tuple<Integer,Integer>> cir){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        Object current=MinecraftCompat.screen(mc);
        if(!(current instanceof AbstractContainerScreen<?>))return;
        AbstractContainerScreen<?> screen=(AbstractContainerScreen<?>)current;
        if(!(screen.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu)){
            if(mc.player.containerMenu instanceof UnifiedPlayerInventoryMirrorMenu)cir.setReturnValue(null);
            return;
        }
        UnifiedPlayerInventoryMirrorMenu menu=(UnifiedPlayerInventoryMirrorMenu)screen.getMenu();
        if(handler!=menu||mc.player.containerMenu!=menu){
            cir.setReturnValue(null);
            return;
        }
        int hoveredId=menu.slots.indexOf(hovered);
        int[] range=PlayerInventorySlotPolicy.sortableRange(menu,hoveredId);
        LOG.info("OMMC sort range hook: sync={}, hovered={}, role={}, range={}",
                menu.containerId,hoveredId,menu.slotRole(hoveredId),Arrays.toString(range));
        cir.setReturnValue(range==null?null:new Tuple<>(range[0],range[1]));
    }

    /** Reject the entire OMMC queue before it can send a single PICKUP packet. */
    public static void clicks(Player player,int syncId,MultiPlayerGameMode gameMode,
                              List<Integer> merges,List<Tuple<Integer,Integer>> swaps,CallbackInfo ci){
        Minecraft mc=Minecraft.getInstance();
        Object current=MinecraftCompat.screen(mc);
        AbstractContainerScreen<?> screen=current instanceof AbstractContainerScreen<?>
                ?(AbstractContainerScreen<?>)current:null;
        boolean screenMirror=screen!=null&&screen.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu;
        boolean activeMirror=mc.player!=null&&mc.player.containerMenu instanceof UnifiedPlayerInventoryMirrorMenu;
        if(!screenMirror&&!activeMirror)return;

        UnifiedPlayerInventoryMirrorMenu menu=activeMirror
                ?(UnifiedPlayerInventoryMirrorMenu)mc.player.containerMenu:null;
        int[] range=null;
        if(screenMirror&&menu!=null&&screen.getMenu()==menu&&player==mc.player
                &&syncId==menu.containerId&&gameMode==mc.gameMode){
            Slot hovered=((AbstractContainerScreenAccessor)screen).ddsTweak$getHoveredSlot();
            range=PlayerInventorySlotPolicy.sortableRange(menu,menu.slots.indexOf(hovered));
        }
        boolean valid=range!=null&&merges!=null&&swaps!=null;
        if(valid)for(Integer click:merges){
            if(!PlayerInventorySortRanges.allowsMergeClick(range,click)){valid=false;break;}
        }
        if(valid)for(Tuple<Integer,Integer> swap:swaps){
            if(swap==null||swap.getA()==null||swap.getB()==null
                    ||!PlayerInventorySortRanges.contains(range,swap.getA())
                    ||!PlayerInventorySortRanges.contains(range,swap.getB())){
                valid=false;
                break;
            }
        }
        if(!valid){
            LOG.warn("OMMC sort click batch blocked: sync={}, range={}, merges={}, swaps={}",
                    syncId,Arrays.toString(range),merges==null?-1:merges.size(),
                    swaps==null?-1:swaps.size());
            ci.cancel();
        }
    }
    //#endif

    /**
     * OMMC v0.8 owns its Tuple type; construct its public (Object,Object)
     * pair reflectively so DT can compile even when vanilla Tuple is removed.
     */
    public static void rangeTupleAdapter(AbstractContainerMenu handler,Slot hovered,
                                          CallbackInfoReturnable<Object> cir){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        Object current=MinecraftCompat.screen(mc);
        if(!(current instanceof AbstractContainerScreen<?>))return;
        AbstractContainerScreen<?> screen=(AbstractContainerScreen<?>)current;
        if(!(screen.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu)){
            if(mc.player.containerMenu instanceof UnifiedPlayerInventoryMirrorMenu)cir.setReturnValue(null);
            return;
        }
        UnifiedPlayerInventoryMirrorMenu menu=(UnifiedPlayerInventoryMirrorMenu)screen.getMenu();
        if(handler!=menu||mc.player.containerMenu!=menu){cir.setReturnValue(null);return;}
        int hoveredId=menu.slots.indexOf(hovered);
        int[] range=PlayerInventorySlotPolicy.sortableRange(menu,hoveredId);
        LOG.info("OMMC sort range hook: sync={}, hovered={}, role={}, range={}",
                menu.containerId,hoveredId,menu.slotRole(hoveredId),Arrays.toString(range));
        if(range==null){cir.setReturnValue(null);return;}
        try{
            cir.setReturnValue(OmmcSortTupleAccess.create(
                    "com.plusls.ommc.util.Tuple",range[0],range[1]));
        }catch(ReflectiveOperationException|LinkageError exception){
            LOG.warn("OMMC sorter tuple adapter unavailable; blocking DT mirror sorting",exception);
            cir.setReturnValue(null);
        }
    }

    /** Validate all swaps in OMMC's mod-owned Tuple queue before any click. */
    public static void clicksTupleAdapter(Player player,int syncId,MultiPlayerGameMode gameMode,
                                          List<Integer> merges,List<?> swaps,CallbackInfo ci){
        Minecraft mc=Minecraft.getInstance();
        Object current=MinecraftCompat.screen(mc);
        AbstractContainerScreen<?> screen=current instanceof AbstractContainerScreen<?>
                ?(AbstractContainerScreen<?>)current:null;
        boolean screenMirror=screen!=null&&screen.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu;
        boolean activeMirror=mc.player!=null&&mc.player.containerMenu instanceof UnifiedPlayerInventoryMirrorMenu;
        if(!screenMirror&&!activeMirror)return;
        UnifiedPlayerInventoryMirrorMenu menu=activeMirror
                ?(UnifiedPlayerInventoryMirrorMenu)mc.player.containerMenu:null;
        int[] range=null;
        if(screenMirror&&menu!=null&&screen.getMenu()==menu&&player==mc.player
                &&syncId==menu.containerId&&gameMode==mc.gameMode){
            Slot hovered=((AbstractContainerScreenAccessor)screen).ddsTweak$getHoveredSlot();
            range=PlayerInventorySlotPolicy.sortableRange(menu,menu.slots.indexOf(hovered));
        }
        boolean valid=range!=null&&merges!=null&&swaps!=null;
        if(valid)for(Integer click:merges){
            if(!PlayerInventorySortRanges.allowsMergeClick(range,click)){valid=false;break;}
        }
        if(valid)for(Object swap:swaps){
            int[] pair=OmmcSortTupleAccess.indices(swap);
            if(pair==null||!PlayerInventorySortRanges.contains(range,pair[0])
                    ||!PlayerInventorySortRanges.contains(range,pair[1])){
                valid=false;
                break;
            }
        }
        if(!valid){
            LOG.warn("OMMC sort click batch blocked: sync={}, range={}, merges={}, swaps={}",
                    syncId,Arrays.toString(range),merges==null?-1:merges.size(),
                    swaps==null?-1:swaps.size());
            ci.cancel();
        }
    }

}

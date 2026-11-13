package ddstweak.integration.dds.playerinventory;

import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Semantic menu-slot policy shared by optional inventory automation adapters.
 * 为背包自动化适配器统一槽位语义与资格判断。
 */
public final class PlayerInventorySlotPolicy {
    private PlayerInventorySlotPolicy(){}

    public static boolean isAutomaticStorageRole(PlayerInventorySlotRole role){
        return role==PlayerInventorySlotRole.MAIN||role==PlayerInventorySlotRole.HOTBAR
                ||role==PlayerInventorySlotRole.ENDER_STORAGE
                ||role==PlayerInventorySlotRole.VIEWER_MAIN
                ||role==PlayerInventorySlotRole.VIEWER_HOTBAR;
    }

    public static boolean isTargetStorageRole(PlayerInventoryKind kind,PlayerInventorySlotRole role){
        if(kind==PlayerInventoryKind.INVENTORY)
            return role==PlayerInventorySlotRole.MAIN||role==PlayerInventorySlotRole.HOTBAR;
        return kind==PlayerInventoryKind.ENDER_CHEST&&role==PlayerInventorySlotRole.ENDER_STORAGE;
    }

    /** IPN's Sort buttons only operate on the target inventory, never viewer slots. */
    public static int[] targetSortableRange(UnifiedPlayerInventoryMirrorMenu menu,int hoveredId){
        if(menu==null||hoveredId<0||hoveredId>=menu.slots.size()
                ||!isTargetStorageRole(menu.kind(),menu.slotRole(hoveredId)))return null;
        return sortableRange(menu,hoveredId);
    }

    public static boolean allowsAutomaticClick(UnifiedPlayerInventoryMirrorMenu menu,int slotId){
        return menu!=null&&slotId>=0&&slotId<menu.slots.size()
                &&isAutomaticStorageRole(menu.slotRole(slotId));
    }

    public static boolean isViewerToTarget(UnifiedPlayerInventoryMirrorMenu menu,int slotId){
        if(menu==null||menu.kind()!=PlayerInventoryKind.INVENTORY
                ||slotId<0||slotId>=menu.slots.size())return false;
        PlayerInventorySlotRole role=menu.slotRole(slotId);
        return role==PlayerInventorySlotRole.VIEWER_MAIN
                ||role==PlayerInventorySlotRole.VIEWER_HOTBAR;
    }

    public static int[] sortableRange(UnifiedPlayerInventoryMirrorMenu menu,int hoveredId){
        if(!allowsAutomaticClick(menu,hoveredId))return null;
        return PlayerInventorySortRanges.range(menu.slotRole(hoveredId),hoveredId,
                menu.targetMainStart(),menu.targetMainEnd(),
                menu.targetHotbarStart(),menu.targetHotbarEnd(),
                menu.viewerMainStart(),menu.viewerHotbarStart(),menu.viewerEnd());
    }

    /** Explicit destinations only: no equipment, offhand, control or hidden slots. */
    public static List<Integer> directMoveDestinations(UnifiedPlayerInventoryMirrorMenu menu,int sourceId){
        if(!allowsAutomaticClick(menu,sourceId)||menu.kind()!=PlayerInventoryKind.INVENTORY)
            return java.util.Collections.emptyList();
        if(isViewerToTarget(menu,sourceId))return targetStorageSlots(menu);
        if(!isTargetStorageRole(menu.kind(),menu.slotRole(sourceId)))
            return java.util.Collections.emptyList();
        List<Integer> destinations=new ArrayList<>();
        for(int id=menu.viewerMainStart();id<menu.viewerEnd();id++){
            PlayerInventorySlotRole role=menu.slotRole(id);
            if(role==PlayerInventorySlotRole.VIEWER_MAIN||role==PlayerInventorySlotRole.VIEWER_HOTBAR)
                destinations.add(id);
        }
        return destinations;
    }

    /** Keep IPN's original main-before-hotbar order, including the Org layout. */
    public static List<Integer> targetStorageSlots(UnifiedPlayerInventoryMirrorMenu menu){
        return targetStorageSlots(menu.targetMainStart(),menu.targetMainEnd(),
                menu.targetHotbarStart(),menu.targetHotbarEnd(),
                menu.kind(),menu::slotRole);
    }

    public static List<Integer> targetStorageSlots(int mainStart,int mainEnd,
                                                    int hotbarStart,int hotbarEnd,
                                                    PlayerInventoryKind kind,
                                                    IntFunction<PlayerInventorySlotRole> roleAt){
        List<Integer> ids=new ArrayList<>();
        if(kind!=PlayerInventoryKind.INVENTORY&&kind!=PlayerInventoryKind.ENDER_CHEST)return ids;
        append(ids,mainStart,mainEnd,kind,roleAt);
        if(kind==PlayerInventoryKind.INVENTORY)append(ids,hotbarStart,hotbarEnd,kind,roleAt);
        return ids;
    }

    private static void append(List<Integer> ids,int start,int end,PlayerInventoryKind kind,
                               IntFunction<PlayerInventorySlotRole> roleAt){
        for(int id=Math.max(0,start);id<end;id++)
            if(isTargetStorageRole(kind,roleAt.apply(id))&&!ids.contains(id))ids.add(id);
    }

}

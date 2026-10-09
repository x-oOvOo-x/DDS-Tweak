package ddstweak.integration.dds.playerinventory;

import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import net.minecraft.world.entity.player.Player;
//#if MC >= 260102
//$$ import net.minecraft.world.inventory.ContainerInput;
//#else
import net.minecraft.world.inventory.ClickType;
//#endif

public interface PlayerInventoryActionBinding{
    boolean canSend();
    default boolean supports(PlayerInventoryAction action){return action!=null&&canSend();}
    boolean send(PlayerInventoryAction action,int value);
    default int dataSlotCount(PlayerInventoryContext context){return 0;}
    default void attach(UnifiedPlayerInventoryMirrorMenu menu){}
    //#if MC >= 260102
    //$$ default boolean handleControlClick(int slotId,int button,ContainerInput type,Player player){return false;}
    //#else
    default boolean handleControlClick(int slotId,int button,ClickType type,Player player){return false;}
    //#endif
    default int selectedTargetHotbarSlot(){return -1;}
    default int attackMode(){return PlayerInventoryBehaviorV1.MODE_NONE;}
    default int attackInterval(){return PlayerInventoryBehaviorV1.DEFAULT_INTERVAL;}
    default int useMode(){return PlayerInventoryBehaviorV1.MODE_NONE;}
    default int useInterval(){return PlayerInventoryBehaviorV1.DEFAULT_INTERVAL;}
    default boolean sneaking(){return false;}
    default PlayerInventoryStateConfidence selectedHotbarConfidence(){return PlayerInventoryStateConfidence.UNKNOWN;}
    default PlayerInventoryStateConfidence attackModeConfidence(){return PlayerInventoryStateConfidence.UNKNOWN;}
    default PlayerInventoryStateConfidence attackIntervalConfidence(){return PlayerInventoryStateConfidence.UNKNOWN;}
    default PlayerInventoryStateConfidence useModeConfidence(){return PlayerInventoryStateConfidence.UNKNOWN;}
    default PlayerInventoryStateConfidence useIntervalConfidence(){return PlayerInventoryStateConfidence.UNKNOWN;}
    default PlayerInventoryStateConfidence sneakingConfidence(){return PlayerInventoryStateConfidence.UNKNOWN;}
}

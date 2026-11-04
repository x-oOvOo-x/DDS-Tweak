package ddstweak.feature.silentautoeat;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

/**
 * Passive S2C slot evidence for one staged SWAP attempt.
 *
 * A matching server update is NOT a positive click acknowledgement. Vanilla
 * may omit updates when local prediction matches, and no packet identifies the
 * Auto Eat click. The observer never authorizes item movement.
 *
 * Kept independent of Minecraft registries so the comparison state machine
 * can be tested in ordinary JUnit without bootstrapping ItemStack/Items.
 */
final class StagingServerSlotEvidence<T> {
    private final BiPredicate<T,T> same;
    private final UnaryOperator<T> snapshot;
    private int menuId=-1,sourceSlot=-1,scratchSlot=-1;
    private T expectedSource,expectedScratch;
    private boolean sourceSeen,scratchSeen,conflicting;

    StagingServerSlotEvidence(BiPredicate<T,T> same,UnaryOperator<T> snapshot){
        this.same=java.util.Objects.requireNonNull(same);
        this.snapshot=java.util.Objects.requireNonNull(snapshot);
    }

    void arm(int menuId,int sourceSlot,int scratchSlot,T source,T scratch){
        clear();
        if(menuId<0||sourceSlot<0||scratchSlot<0||sourceSlot==scratchSlot
                ||source==null||scratch==null)return;
        this.menuId=menuId;
        this.sourceSlot=sourceSlot;
        this.scratchSlot=scratchSlot;
        expectedSource=snapshot.apply(source);
        expectedScratch=snapshot.apply(scratch);
    }

    void observe(int packetMenuId,int packetSlot,T serverStack){
        if(menuId<0||packetMenuId!=menuId||serverStack==null)return;
        if(packetSlot==sourceSlot){
            sourceSeen=true;
            if(!same.test(serverStack,expectedSource))conflicting=true;
        }else if(packetSlot==scratchSlot){
            scratchSeen=true;
            if(!same.test(serverStack,expectedScratch))conflicting=true;
        }
    }

    /** Full server content is evidence for both slots, but not a click ACK. */
    void observeSnapshot(int packetMenuId,List<T> slots){
        if(menuId<0||packetMenuId!=menuId||slots==null||sourceSlot<0
                ||sourceSlot>=slots.size()||scratchSlot>=slots.size())return;
        observe(packetMenuId,sourceSlot,slots.get(sourceSlot));
        if(scratchSlot>=0)observe(packetMenuId,scratchSlot,slots.get(scratchSlot));
    }

    /**
     * Once mainhand consumption starts the food slot can legitimately lose
     * items. Continue watching the unchanged stored scratch item in the
     * original inventory source slot until the reverse SWAP is prepared.
     */
    void retainSourceDuringConsumption(){
        scratchSlot=-1;
        expectedScratch=null;
        scratchSeen=false;
    }

    boolean pairedMatchingUpdatesSeen(){
        return menuId>=0&&scratchSlot>=0&&sourceSeen&&scratchSeen&&!conflicting;
    }

    boolean hasConflict(){return conflicting;}

    void clear(){
        menuId=-1;
        sourceSlot=-1;
        scratchSlot=-1;
        expectedSource=null;
        expectedScratch=null;
        sourceSeen=false;
        scratchSeen=false;
        conflicting=false;
    }
}

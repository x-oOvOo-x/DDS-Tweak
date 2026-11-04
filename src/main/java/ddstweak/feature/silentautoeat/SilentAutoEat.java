package ddstweak.feature.silentautoeat;

import ddstweak.config.Configs;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SilentAutoEat {
    private static State state=State.IDLE;
    private static LocalPlayer owner;
    private static ClientLevel ownerLevel;
    private static AbstractContainerMenu transactionMenu;
    private static int transactionContainerId=-1;
    private static int sourceIndex=-1;
    private static int sourceMenuSlot=-1;
    private static boolean selectedMainhandSource;
    private static ItemStack originalOffhand=ItemStack.EMPTY;
    private static ItemStack selectedFood=ItemStack.EMPTY;
    private static boolean swapped;
    private static int mainhandOriginalSelected=-1,mainhandAutoSelected=-1;
    private static boolean mainhandSelectedByAuto;
    private static int scratchIndex=-1;
    private static final StagingServerSlotEvidence<ItemStack> serverSlotEvidence=
            new StagingServerSlotEvidence<>(ItemStack::matches,ItemStack::copy);
    private static final org.apache.logging.log4j.Logger STAGING_EVIDENCE_LOG=
            org.apache.logging.log4j.LogManager.getLogger("DDS Tweak Silent Auto Eat");
    private static ItemStack originalScratch=ItemStack.EMPTY;
    // The local reverse SWAP can look restored before a late server correction.
    // Observe only server-authored updates for a bounded period; never re-swap.
    private static final StagingServerSlotEvidence<ItemStack> settledStageEvidence=
            new StagingServerSlotEvidence<>(ItemStack::matches,ItemStack::copy);
    private static LocalPlayer settledStageOwner;
    private static ClientLevel settledStageLevel;
    private static int settledStageSource=-1,settledStageScratch=-1,settledStageMenuId=-1;
    private static ItemStack settledStageOriginal=ItemStack.EMPTY,settledStageFood=ItemStack.EMPTY;
    private static boolean settledStageCompleted;
    private static long settledStageDeadline;
    // A short render-only settle window after a confirmed mainhand restore.
    private static LocalPlayer stagedVisualOwner;
    private static ClientLevel stagedVisualLevel;
    private static int stagedVisualSlot=-1;
    private static ItemStack stagedVisualItem=ItemStack.EMPTY;
    private static long stagedVisualDeadline;
    private static boolean stagingSwapSent,stagingConfirmed,stagingRestoreSent;
    private static long stagingAckDeadline,stagingRestoreDeadline;
    private static LocalPlayer failedStagingOwner;
    private static ClientLevel failedStagingLevel;
    private static int failedStagingSource=-1,failedStagingScratch=-1;
    private static ItemStack failedStagingOriginalScratch=ItemStack.EMPTY;
    private static ItemStack failedStagingFood=ItemStack.EMPTY;
    private static boolean failedStagingCompleted;
    private static long failedStagingStableSince=-1;
    private static boolean completionObserved,restoreRequestSent;
    private static long clientTicks, restoreDeadlineTick, overrideDeadlineTick, swapDeadlineTick, restoreReadyTick;
    private static LocalPlayer overrideOwner, failedRestoreOwner;
    private static int failedRestoreSourceIndex=-1;
    private static ItemStack failedRestoreOffhand=ItemStack.EMPTY, failedRestoreFood=ItemStack.EMPTY;
    private static ClientLevel overrideLevel, failedRestoreLevel;
    private static long failedRestoreStableSince=-1;
    private static final int USER_OVERRIDE_TICKS=20;
    // Mouse scroll is an event, not a held key. Capture its intent even if
    // vanilla has already changed the selected slot before the next tick.
    private static final java.util.concurrent.atomic.AtomicInteger pendingHotbarScrolls=
            new java.util.concurrent.atomic.AtomicInteger();
    private static final int SWAP_ACK_TICKS=20;
    private static final int RESTORE_GRACE_TICKS=40;
    private static final int FAILURE_RECOVERY_TICKS=20;
    private static final int HAND_CACHE_SETTLE_TICKS=10;
    private static final int STAGED_SERVER_SETTLE_TICKS=40;
    // Local inventory convergence after a vanilla SWAP may be only client prediction.
    // Do not enable staged item movement before a cross-version authoritative
    // acknowledgement path has been verified. Hotbar-only mainhand remains active.
    private static final boolean STAGED_SERVER_ACK_READY=false;
    // Opt-in development canary for Minecraft 26.3 only. Vanilla's client
    // prediction is not an ACK; never enable this by default or in a release.
    private static boolean experimental26StagingEnabled(){
        //#if MC == 260300
        //$$ return Boolean.getBoolean("ddstweak.experimental.silentAutoEatStaging26");
        //#else
        return false;
        //#endif
    }
    private static LocalPlayer restoredVisualOwner;
    private static ClientLevel restoredVisualLevel;
    private static int restoredVisualSource=-1;
    private static long restoredVisualUntilTick;
    private static ItemStack restoredVisualFood=ItemStack.EMPTY;
    private static ItemStack restoredVisualOriginalOffhand=ItemStack.EMPTY;
    private static boolean restoredVisualCompleted;
    // Only HUD presentation: the reverse offhand SWAP may set vanilla popTime
    // on the hotbar source or offhand item. Never modify real ItemStack state.
    private static LocalPlayer hotbarPopOwner;
    private static ClientLevel hotbarPopLevel;
    private static int hotbarPopSource=-1;
    private static long hotbarPopUntilTick;
    private static ItemStack hotbarPopFood=ItemStack.EMPTY;
    private static ItemStack hotbarPopOriginalOffhand=ItemStack.EMPTY;
    private static boolean hotbarPopCompleted;
    //#if MC == 260300
    //$$ private static final org.apache.logging.log4j.Logger RECOVERY_LOG=
    //$$         org.apache.logging.log4j.LogManager.getLogger("DDS Tweak Silent Auto Eat");
    //$$ private static String recoveryDiagnostic;
    //#endif
    //#if MC == 11404
    //$$ private static final org.apache.logging.log4j.Logger LEGACY_LOG=
    //$$         org.apache.logging.log4j.LogManager.getLogger("DDS Tweak Silent Auto Eat");
    //$$ private static LocalPlayer legacy11404DiagnosticPlayer;
    //$$ private static String legacy11404DiagnosticSnapshot;
    //#endif

    private SilentAutoEat(){}

    public static void initialize(){
        ClientTickEvents.START_CLIENT_TICK.register(SilentAutoEat::beforeTick);
        ClientTickEvents.END_CLIENT_TICK.register(SilentAutoEat::tick);
        //#if MC == 11404
        //$$ LEGACY_LOG.debug("[1.14.4] client tick callbacks registered");
        //#endif
    }

    public static boolean isPresentationActive(LocalPlayer player){
        if(owner!=player||state==State.IDLE||!swapped)return false;
        if(sourceIndex<0)
            return SilentAutoEatCompat.isSafeAutomationContext(Minecraft.getInstance(),player)
                    &&SilentAutoEatFoodRules.sameStackIgnoringCount(player.getOffhandItem(),selectedFood);
        if(state==State.RESTORE_OFFHAND){
            // Rendering must follow the owned pending transaction, not whether
            // a foreign/QCA menu currently allows sending the reverse SWAP.
            // Never mask a foreign offhand stack after ownership is lost.
            return SilentAutoEatRules.shouldMaskPendingRestore(
                    sourceStillOwnsOriginal(),canRestoreOwnedOffhand());
        }
        return SilentAutoEatCompat.isSafeAutomationContext(Minecraft.getInstance(),player)
                &&hasConfirmedSwap();
    }

    static int presentationSourceIndex(){
        return sourceIndex;
    }

    static boolean presentationUsesSelectedMainhand(LocalPlayer player){
        // Rendering follows the currently selected slot, not its selection at
        // transaction start. Selecting the source during eating still cancels
        // the transaction; until restore, the source physically holds the offhand.
        int selected=SilentAutoEatCompat.selectedSlot(SilentAutoEatCompat.inventory(player));
        return SilentAutoEatRules.isOwnedSourceIndex(selected,sourceIndex);
    }

    static ItemStack presentationOriginalOffhand(){
        return originalOffhand;
    }

    /** 26.3 staging uses the selected mainhand slot, not another hotbar key. */
    public static boolean isStagedMainhandVisualActive(LocalPlayer player){
        if(player==null)return false;
        int selected=SilentAutoEatCompat.selectedSlot(SilentAutoEatCompat.inventory(player));
        if(owner==player&&ownerLevel==Minecraft.getInstance().level
                &&stagingSwapSent&&state!=State.IDLE&&selected==scratchIndex
                &&!serverSlotEvidence.hasConflict()
                &&ItemStack.matches(player.getOffhandItem(),originalOffhand)){
            boolean finishing=state==State.MAIN_HAND_USING
                    &&(!player.isUsingItem()||player.getUsedItemHand()!=InteractionHand.MAIN_HAND);
            return stagedSourceOwned()&&isRestoredFoodOrRemainder(
                    player.getMainHandItem(),selectedFood,completionObserved||finishing)
                    ||stagingRestoreSent&&hasConfirmedStageRestore();
        }
        return state==State.IDLE&&stagedVisualOwner==player
                &&stagedVisualLevel==Minecraft.getInstance().level
                &&clientTicks<=stagedVisualDeadline&&selected==stagedVisualSlot
                &&ItemStack.matches(player.getMainHandItem(),stagedVisualItem);
    }

    static ItemStack stagedMainhandVisual(LocalPlayer player){
        return owner==player&&stagingSwapSent?originalScratch:stagedVisualItem;
    }

    static int stagedMainhandSlot(LocalPlayer player){
        return owner==player&&stagingSwapSent?scratchIndex:stagedVisualSlot;
    }

    private static void clearStagedMainhandVisual(){
        stagedVisualOwner=null;
        stagedVisualLevel=null;
        stagedVisualSlot=-1;
        stagedVisualItem=ItemStack.EMPTY;
        stagedVisualDeadline=0;
    }

    private static void rememberStagedMainhandVisual(){
        if(owner==null||scratchIndex!=mainhandOriginalSelected||!hasConfirmedStageRestore()
                ||SilentAutoEatCompat.selectedSlot(SilentAutoEatCompat.inventory(owner))!=scratchIndex)
            return;
        stagedVisualOwner=owner;
        stagedVisualLevel=ownerLevel;
        stagedVisualSlot=scratchIndex;
        stagedVisualItem=originalScratch.copy();
        stagedVisualDeadline=clientTicks+HAND_CACHE_SETTLE_TICKS;
    }

    private static boolean isRestoredFoodOrRemainder(ItemStack actual,ItemStack food,boolean completed){
        return SilentAutoEatFoodRules.sameStackIgnoringCount(actual,food)
                ||completed&&(actual.isEmpty()||actual.getItem()==Items.BOWL
                ||actual.getItem()==Items.GLASS_BOTTLE);
    }

    /** Suppress only the HUD bounce caused by our own offhand SWAP. */
    public static boolean shouldSuppressHotbarPop(LocalPlayer player,int index,ItemStack rendered){
        if(player==null||player!=Minecraft.getInstance().player||rendered==null
                ||rendered.isEmpty()||(index!=40&&(index<0||index>=9)))return false;
        boolean active=owner==player&&ownerLevel==Minecraft.getInstance().level
                &&swapped&&sourceIndex>=0&&sourceIndex<9&&!originalOffhand.isEmpty()
                &&state!=State.IDLE;
        if(active){
            boolean restored=state==State.RESTORE_OFFHAND&&restoreRequestSent
                    &&hasConfirmedRestore();
            if(index==40)return ItemStack.matches(rendered,originalOffhand)
                    &&(isPresentationActive(player)||restored);
            return index==sourceIndex
                    &&isRestoredFoodOrRemainder(rendered,selectedFood,completionObserved)
                    &&(isPresentationActive(player)||restored);
        }
        if(state!=State.IDLE||hotbarPopOwner!=player
                ||hotbarPopLevel!=Minecraft.getInstance().level
                ||clientTicks>hotbarPopUntilTick)return false;
        if(index==40)return ItemStack.matches(rendered,hotbarPopOriginalOffhand);
        return index==hotbarPopSource
                &&isRestoredFoodOrRemainder(rendered,hotbarPopFood,hotbarPopCompleted);
    }

    //#if MC >= 260300
    //$$ /**
    //$$  * The two real slots must still form one of Auto Eat's known swap
    //$$  * layouts. Do not rely on the rendered ItemStack or on a later tick
    //$$  * of state=START_USE: 26.3 extracts the HUD between transitions.
    //$$  */
    //$$ public static boolean isOwned26HudSwap(LocalPlayer player){
    //$$     if(player==null||owner!=player||ownerLevel!=Minecraft.getInstance().level
    //$$             ||!swapped||state==State.IDLE||sourceIndex<0||sourceIndex>=36)
    //$$         return false;
    //$$     ItemStack source=SilentAutoEatCompat.inventory(player).getItem(sourceIndex);
    //$$     ItemStack offhand=player.getOffhandItem();
    //$$     // The last food can disappear before watchUse observes completion
    //$$     // at END_CLIENT_TICK. Do not expose the temporary offhand in that frame.
    //$$     boolean finishingUse=state==State.USING
    //$$             &&(!player.isUsingItem()
    //$$             ||player.getUsedItemHand()!=InteractionHand.OFF_HAND);
    //$$     boolean completed=completionObserved||finishingUse;
    //$$     return ItemStack.matches(source,originalOffhand)
    //$$             &&isRestoredFoodOrRemainder(offhand,selectedFood,completed)
    //$$             ||ItemStack.matches(offhand,originalOffhand)
    //$$             &&isRestoredFoodOrRemainder(source,selectedFood,completed);
    //$$ }
    //$$
    //$$ /**
    //$$  * The transaction can complete between extraction and the next HUD
    //$$  * frame. Keep original visibility only while real slots are restored.
    //$$  */
    //$$ public static boolean isOwned26HudRestore(LocalPlayer player,int slot){
    //$$     if(player==null||hotbarPopOwner!=player
    //$$             ||hotbarPopLevel!=Minecraft.getInstance().level
    //$$             ||state!=State.IDLE||clientTicks>hotbarPopUntilTick
    //$$             ||hotbarPopSource<0||hotbarPopSource>=9
    //$$             ||slot!=40&&slot!=hotbarPopSource)return false;
    //$$     return ItemStack.matches(player.getOffhandItem(),hotbarPopOriginalOffhand)
    //$$             &&isRestoredFoodOrRemainder(
    //$$                     SilentAutoEatCompat.inventory(player).getItem(hotbarPopSource),
    //$$                     hotbarPopFood,hotbarPopCompleted);
    //$$ }
    //$$
    //$$ static ItemStack hudFood26(LocalPlayer player){
    //$$     ItemStack source=SilentAutoEatCompat.inventory(player).getItem(sourceIndex);
    //$$     return ItemStack.matches(source,originalOffhand)
    //$$             ?player.getOffhandItem():source;
    //$$ }
    //$$
    //$$ /**
    //$$  * 26.3 extracts HUD item render states before drawing. In this era an
    //$$  * upstream projected stack may differ from our source snapshots, so use
    //$$  * the verified real transaction slots (not the rendered copy's identity).
    //$$  * Never touch the real stack's popTime.
    //$$  */
    //$$ public static boolean shouldSuppressExtractedHotbarPop(LocalPlayer player,int index){
    //$$     if(player==null||player!=Minecraft.getInstance().player
    //$$             ||(index!=40&&(index<0||index>=9)))return false;
    //$$     if(isOwned26HudSwap(player)){
    //$$         return index==40||sourceIndex<9&&index==sourceIndex;
    //$$     }
    //$$     return isOwned26HudRestore(player,index);
    //$$ }
    //#endif

    /** The real slots are restored, but vanilla's hand render cache may still hold the old offhand. */
    public static boolean isRestoredMainhandVisualActive(LocalPlayer player){
        if(player==null)return false;
        int selected=SilentAutoEatCompat.selectedSlot(SilentAutoEatCompat.inventory(player));
        // The client may have applied the reverse click before our next END_CLIENT_TICK.
        // Cover that frame only after both real slots already prove restoration.
        if(owner==player&&state==State.RESTORE_OFFHAND&&restoreRequestSent
                &&sourceIndex>=0&&sourceIndex<9&&selected==sourceIndex)
            return hasConfirmedRestore();
        if(restoredVisualOwner!=player||restoredVisualLevel!=Minecraft.getInstance().level)return false;
        ItemStack actual=player.getMainHandItem();
        boolean sourceRestored=SilentAutoEatFoodRules.sameStackIgnoringCount(actual,restoredVisualFood)
                ||restoredVisualCompleted&&(actual.isEmpty()||actual.getItem()==Items.BOWL
                ||actual.getItem()==Items.GLASS_BOTTLE);
        return SilentAutoEatRules.keepRestoredMainhandVisual(
                state==State.IDLE,true,clientTicks<=restoredVisualUntilTick,
                selected==restoredVisualSource,sourceRestored);
    }

    static boolean isStaleRestoredMainhandStack(LocalPlayer player,ItemStack upstream){
        if(!isRestoredMainhandVisualActive(player))return false;
        ItemStack oldOffhand=owner==player&&state==State.RESTORE_OFFHAND
                ?originalOffhand:restoredVisualOriginalOffhand;
        return ItemStack.matches(upstream,oldOffhand)
                &&!ItemStack.matches(upstream,player.getMainHandItem());
    }

    /**
     * A projected food source is not the real stack in that slot.
     * Guard only the two player-owned transaction slots, including when a QCA
     * nested menu exposes them, until the reverse SWAP is acknowledged.
     * Unrelated QCA/container slots remain fully interactive.
     */
    public static boolean shouldGuardSlotClick(Minecraft minecraft,net.minecraft.world.inventory.Slot slot){
        if(minecraft==null||owner!=minecraft.player||owner==null||sourceIndex<0
                ||(!swapped&&!stagingSwapSent)||state==State.IDLE||owner.containerMenu==null
                ||!owner.containerMenu.slots.contains(slot))return false;
        int index=SilentAutoEatCompat.inventoryIndex(owner,slot);
        return stagingSwapSent?index==sourceIndex||index==scratchIndex
                :SilentAutoEatRules.isTransactionSwapSlot(index,sourceIndex);
    }

    /**
     * Called only after vanilla applies an S2C slot or content packet.
     * These observations cannot independently authorize a staged SWAP.
     */
    public static void observeServerSlot(int menuId,int menuSlot,ItemStack stack){
        if(settledStageOwner!=null){
            if(isSettledStageContextValid()){
                settledStageEvidence.observe(menuId,menuSlot,stack);
                if(settledStageEvidence.hasConflict())rejectSettledStage();
            }else clearSettledStage();
        }
        if(owner==null||!stagingSwapSent||state==State.IDLE)return;
        boolean paired=serverSlotEvidence.pairedMatchingUpdatesSeen();
        boolean conflict=serverSlotEvidence.hasConflict();
        serverSlotEvidence.observe(menuId,menuSlot,stack);
        diagnoseServerSlotEvidence(paired,conflict);
    }

    /** The direct player-inventory packet uses inventory indexes, not menu slots. */
    public static void observeServerInventorySlot(int inventoryIndex,ItemStack stack){
        LocalPlayer observed=owner!=null&&stagingSwapSent&&state!=State.IDLE
                ?owner:settledStageOwner;
        if(observed==null)return;
        int source=observed==owner?sourceIndex:settledStageSource;
        int scratch=observed==owner?scratchIndex:settledStageScratch;
        if(inventoryIndex!=source&&inventoryIndex!=scratch)return;
        int slot=SilentAutoEatCompat.menuSlot(observed,inventoryIndex);
        if(slot>=0)observeServerSlot(observed==owner
                ?transactionContainerId:settledStageMenuId,slot,stack);
    }

    public static void observeServerSnapshot(int menuId,java.util.List<ItemStack> stacks){
        if(settledStageOwner!=null){
            if(isSettledStageContextValid()){
                settledStageEvidence.observeSnapshot(menuId,stacks);
                if(settledStageEvidence.hasConflict())rejectSettledStage();
            }else clearSettledStage();
        }
        if(owner==null||!stagingSwapSent||state==State.IDLE)return;
        boolean paired=serverSlotEvidence.pairedMatchingUpdatesSeen();
        boolean conflict=serverSlotEvidence.hasConflict();
        serverSlotEvidence.observeSnapshot(menuId,stacks);
        diagnoseServerSlotEvidence(paired,conflict);
    }

    private static void diagnoseServerSlotEvidence(boolean paired,boolean conflict){
        if(!STAGING_EVIDENCE_LOG.isDebugEnabled())return;
        if(!conflict&&serverSlotEvidence.hasConflict())
            STAGING_EVIDENCE_LOG.debug("Auto Eat staging: server slot data conflicts with the observed layout");
        else if(!paired&&serverSlotEvidence.pairedMatchingUpdatesSeen())
            STAGING_EVIDENCE_LOG.debug("Auto Eat staging: both slots appeared in S2C updates (not a click ACK)");
    }

    /** Cancel a manual use, then defer it until our temporary swap is acknowledged as restored. */
    public static boolean onExplicitUse(Minecraft minecraft){
        boolean defer=minecraft!=null&&owner!=null&&owner==minecraft.player
                &&ownerLevel==minecraft.level
                &&(SilentAutoEatRules.shouldDeferExplicitUse(state!=State.IDLE,swapped,sourceIndex)
                ||state!=State.IDLE&&stagingSwapSent);
        onExplicitPlayerAction(minecraft);
        return defer;
    }

    /** Read-only wheel callback: leave inventory and use state on the client tick thread. */
    public static void onManualHotbarScroll(Minecraft minecraft,double vertical){
        if(vertical==0.0D||minecraft==null||minecraft.player==null
                ||minecraft.level==null
                ||ddstweak.platform.minecraft.MinecraftCompat.screen(minecraft)!=null)return;
        pendingHotbarScrolls.incrementAndGet();
    }

    private static boolean consumeManualHotbarScroll(Minecraft minecraft){
        if(pendingHotbarScrolls.getAndSet(0)==0||minecraft.player==null
                ||minecraft.level==null||!Configs.Tweaks.AUTO_EAT.getBooleanValue())return false;
        // The player's wheel movement wins even when the vanilla input and
        // a previous automatic selection occur between two client ticks.
        if(mainhandPath()||stagingSwapSent)mainhandSelectedByAuto=false;
        onExplicitPlayerAction(minecraft);
        return true;
    }

    public static void onExplicitPlayerAction(Minecraft minecraft){
        clearSettledStage();
        clearHotbarPopVisual();
        clearStagedMainhandVisual();
        if(minecraft.player==null)return;
        overrideOwner=minecraft.player;
        overrideLevel=minecraft.level;
        overrideDeadlineTick=clientTicks+USER_OVERRIDE_TICKS;
        if(state!=State.IDLE)cancelAndRestore(minecraft);
    }

    /**
     * The food is truly used in MAIN_HAND, but the first-person visual may
     * continue showing the original selected hotbar item. This belongs only
     * to our selected slot, never to a player-selected replacement.
     */
    public static boolean isMainhandVisualSourceActive(LocalPlayer player){
        if(player==null||owner!=player||mainhandOriginalSelected<0||mainhandOriginalSelected>=9
                ||mainhandAutoSelected<0||mainhandAutoSelected>=9)return false;
        if(state!=State.MAIN_HAND_START_USE&&state!=State.MAIN_HAND_USING)return false;
        return SilentAutoEatCompat.selectedSlot(SilentAutoEatCompat.inventory(player))==mainhandAutoSelected;
    }

    public static ItemStack mainhandOriginalVisual(LocalPlayer player){
        return SilentAutoEatCompat.inventory(player).getItem(mainhandOriginalSelected);
    }

    /** Real mainhand food use stays untouched; only its first-person pose is hidden. */
    public static boolean isAutomatedMainhandUse(LocalPlayer player){
        return player!=null&&owner==player&&state==State.MAIN_HAND_USING
                &&player.isUsingItem()&&player.getUsedItemHand()==InteractionHand.MAIN_HAND;
    }

    public static boolean shouldHideUseAnimation(LocalPlayer player){
        return isPresentationActive(player)||isAutomatedMainhandUse(player);
    }

    public static boolean ownsUse(LocalPlayer player){
        if(owner!=player||!player.isUsingItem())return false;
        return (state==State.USING&&player.getUsedItemHand()==InteractionHand.OFF_HAND)
                ||(state==State.MAIN_HAND_USING&&player.getUsedItemHand()==InteractionHand.MAIN_HAND);
    }

    /** Only a vanilla offhand use owned by Auto Eat can bypass local slowdown. */
    public static boolean bypassMovementSlowdown(LocalPlayer player){
        return player!=null&&SilentAutoEatRules.shouldBypassSlowdown(
                Configs.Tweaks.AUTO_EAT_NO_SLOWDOWN.getBooleanValue(),ownsUse(player));
    }

    static State stateForTest(){return state;}

    private static void beforeTick(Minecraft minecraft){
        clientTicks++;
        if(settledStageOwner!=null&&(!isSettledStageContextValid()
                ||clientTicks>settledStageDeadline))clearSettledStage();
        if(stagedVisualOwner!=null&&(clientTicks>stagedVisualDeadline
                ||minecraft.player!=stagedVisualOwner||minecraft.level!=stagedVisualLevel))
            clearStagedMainhandVisual();
        if(restoredVisualOwner!=null&&(clientTicks>restoredVisualUntilTick
                ||minecraft.player!=restoredVisualOwner||minecraft.level!=restoredVisualLevel))
            clearRestoredMainhandVisual();
        if(hotbarPopOwner!=null&&(clientTicks>hotbarPopUntilTick
                ||minecraft.player!=hotbarPopOwner||minecraft.level!=hotbarPopLevel))
            clearHotbarPopVisual();
        if(consumeManualHotbarScroll(minecraft))return;
        if(state==State.IDLE||minecraft.player!=owner)return;
        if(relinquishOnPlayerHotbarIntent(minecraft))return;
        if(hasExplicitConflict(minecraft))onExplicitPlayerAction(minecraft);
    }

    private static void tick(Minecraft minecraft){
        //#if MC == 11404
        //$$ if(clientTicks%40==0&&LEGACY_LOG.isDebugEnabled())diagnoseLegacy11404(minecraft);
        //#endif
        if(consumeManualHotbarScroll(minecraft))return;
        if(state==State.IDLE){
            if(shouldStart(minecraft)){
                owner=minecraft.player;
                ownerLevel=minecraft.level;
                state=State.PREPARE;
            }
            return;
        }

        if(minecraft.player!=owner||minecraft.level!=ownerLevel){
            clear();
            return;
        }

        if(relinquishOnPlayerHotbarIntent(minecraft))return;
        if(!Configs.Tweaks.AUTO_EAT.getBooleanValue()){
            cancelAndRestore(minecraft);
            return;
        }

        switch(state){
            case STAGED_SWAP:stageToHotbar(minecraft);break;
            case STAGED_WAIT_ACK:waitForStagingAck(minecraft);break;
            case STAGED_RESTORE:restoreStagedHotbar(minecraft);break;
            case MAIN_HAND_SELECT:selectMainhand(minecraft);break;
            case MAIN_HAND_START_USE:startMainhandUse(minecraft);break;
            case MAIN_HAND_USING:watchMainhandUse(minecraft);break;
            case PREPARE:prepare(minecraft);break;
            case SWAP_TO_OFFHAND:swapToOffhand(minecraft);break;
            case WAIT_SWAP:waitForSwap(minecraft);break;
            case START_USE:startUse(minecraft);break;
            case USING:watchUse(minecraft);break;
            case RESTORE_OFFHAND:restoreAndClear(minecraft);break;
            default:clear();
        }
    }

    //#if MC == 11404
    //$$ /** Logs only when the 1.14.4 idle-gate snapshot changes, never per tick. */
    //$$ private static void diagnoseLegacy11404(Minecraft minecraft){
    //$$     LocalPlayer player=minecraft.player;
    //$$     if(player==null){
    //$$         legacy11404DiagnosticPlayer=null;
    //$$         legacy11404DiagnosticSnapshot=null;
    //$$         return;
    //$$     }
    //$$     if(legacy11404DiagnosticPlayer!=player){
    //$$         legacy11404DiagnosticPlayer=player;
    //$$         legacy11404DiagnosticSnapshot=null;
    //$$     }
    //$$     boolean enabled=Configs.Tweaks.AUTO_EAT.getBooleanValue();
    //$$     int hunger=player.getFoodData().getFoodLevel();
    //$$     int threshold=Configs.Tweaks.AUTO_EAT_HUNGER_THRESHOLD.getIntegerValue();
    //$$     boolean canEat=player.canEat(false);
    //$$     boolean safe=SilentAutoEatCompat.isSafeAutomationContext(minecraft,player);
    //$$     boolean blockedOffhand=isProtectedOffhand(player.getOffhandItem());
    //$$     boolean conflictingInput=hasExplicitConflict(minecraft);
    //$$     String screen=ddstweak.platform.minecraft.MinecraftCompat.screen(minecraft)==null?
    //$$             "none":ddstweak.platform.minecraft.MinecraftCompat.screen(minecraft).getClass().getSimpleName();
    //$$     boolean personalMenu=player.containerMenu==player.inventoryMenu;
    //$$     int foodCandidate=Integer.MIN_VALUE;
    //$$     if(enabled&&safe&&canEat&&!blockedOffhand&&hunger<=threshold)
    //$$         foodCandidate=findFood(player,SilentAutoEatCompat.inventory(player),player.getOffhandItem(),false);
    //$$     String snapshot="state="+state+" enabled="+enabled+" hunger="+hunger+
    //$$             " threshold="+threshold+" canEat="+canEat+" safe="+safe+
    //$$             " menuIsInventory="+personalMenu+" screen="+screen+
    //$$             " offhandProtected="+blockedOffhand+" inputBusy="+conflictingInput+
    //$$             " eligibleSource="+foodCandidate;
    //$$     if(!snapshot.equals(legacy11404DiagnosticSnapshot)){
    //$$         legacy11404DiagnosticSnapshot=snapshot;
    //$$         LEGACY_LOG.debug("[1.14.4] {}",snapshot);
    //$$     }
    //$$ }
    //#endif
    private static boolean shouldStart(Minecraft minecraft){
        LocalPlayer player=minecraft.player;
        // Do not start another staged transaction while the previous reverse
        // SWAP can still receive a server-authored correction.
        if(settledStageOwner!=null){
            if(!isSettledStageContextValid()||clientTicks>settledStageDeadline)
                clearSettledStage();
            else return false;
        }
        if(player==null||minecraft.level==null||minecraft.gameMode==null
                ||hasExplicitConflict(minecraft)||hasExplicitHotbarIntent(minecraft))return false;
        if(player==failedRestoreOwner&&minecraft.level==failedRestoreLevel){
            if(!hasConfirmedFailedRestore(minecraft,player)){
                failedRestoreStableSince=-1;
                //#if MC == 260300
                //$$ diagnoseFailedRestore(minecraft,player);
                //#endif
                return false;
            }
            if(failedRestoreStableSince<0){
                failedRestoreStableSince=clientTicks;
                return false;
            }
            if(clientTicks-failedRestoreStableSince<FAILURE_RECOVERY_TICKS)return false;
            clearFailedRestore();
        }
        if(player==failedStagingOwner&&minecraft.level==failedStagingLevel){
            if(!hasConfirmedStagingRecovery(player)){
                failedStagingStableSince=-1;
                return false;
            }
            if(failedStagingStableSince<0){
                failedStagingStableSince=clientTicks;
                return false;
            }
            if(clientTicks-failedStagingStableSince<FAILURE_RECOVERY_TICKS)return false;
            clearFailedStaging();
        }
        if(player==overrideOwner&&minecraft.level==overrideLevel
                &&SilentAutoEatRules.isWithinUserOverride(clientTicks,overrideDeadlineTick))return false;
        return SilentAutoEatRules.shouldTrigger(
                Configs.Tweaks.AUTO_EAT.getBooleanValue(),
                SilentAutoEatCompat.isSafeAutomationContext(minecraft,player),
                player.isUsingItem(),
                player.isSpectator(),
                player.canEat(false),
                player.getFoodData().getFoodLevel(),
                Configs.Tweaks.AUTO_EAT_HUNGER_THRESHOLD.getIntegerValue()
        );
    }

    private static void prepare(Minecraft minecraft){
        LocalPlayer player=minecraft.player;
        if(player==null||!SilentAutoEatCompat.isSafeAutomationContext(minecraft,player)
                ||hasExplicitHotbarIntent(minecraft)){
            clear();return;
        }

        Inventory inventory=SilentAutoEatCompat.inventory(player);
        ItemStack offhand=player.getOffhandItem();
        AutoEatOffhandPolicy policy=(AutoEatOffhandPolicy)
                Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY.getOptionListValue();
        boolean preferHotbar=preferSwapFreeHotbar(policy);
        int candidate=findFood(player,inventory,offhand,preferHotbar);
        if(candidate==Integer.MIN_VALUE){clear();return;}
        AutoEatExecutionPlanner.Path path=AutoEatExecutionPlanner.plan(
                policy,isEligibleFood(offhand),offhand.isEmpty(),
                isProtectedOffhand(offhand),candidate,SilentAutoEatCompat.selectedSlot(inventory),
                preferHotbar);
        // 26.3 prefers an existing hotbar source over a real offhand SWAP.
        // Inventory-only staging still requires authoritative ACK protection.
        if(path==AutoEatExecutionPlanner.Path.NONE
                ||path==AutoEatExecutionPlanner.Path.MAIN_HAND_STAGED
                &&!STAGED_SERVER_ACK_READY&&!experimental26StagingEnabled()){
            clear();return;
        }

        owner=player;
        ownerLevel=minecraft.level;
        transactionMenu=player.containerMenu;
        transactionContainerId=transactionMenu.containerId;
        originalOffhand=offhand.copy();

        if(candidate==-1){
            sourceIndex=-1;
            sourceMenuSlot=-1;
            selectedFood=offhand.copy();
            swapped=true;
            state=State.START_USE;
            return;
        }

        sourceIndex=candidate;
        selectedMainhandSource=candidate==SilentAutoEatCompat.selectedSlot(inventory);
        sourceMenuSlot=SilentAutoEatCompat.menuSlot(player,candidate);
        if(sourceMenuSlot<0){clear();return;}
        selectedFood=inventory.getItem(candidate).copy();
        if(path==AutoEatExecutionPlanner.Path.MAIN_HAND_STAGED){
            int selected=SilentAutoEatCompat.selectedSlot(inventory);
            scratchIndex=experimental26StagingEnabled()
                    ?AutoEatScratchSelection.selected(selected,
                            index->!SilentAutoEatFoodRules.sameStackIgnoringCount(
                                    inventory.getItem(index),selectedFood))
                    :AutoEatScratchSelection.choose(selected,
                            index->inventory.getItem(index).isEmpty(),
                            index->!SilentAutoEatFoodRules.sameStackIgnoringCount(
                                    inventory.getItem(index),selectedFood));
            if(scratchIndex<0){clear();return;}
            originalScratch=inventory.getItem(scratchIndex).copy();
            // The 26.3 development canary explicitly holds the displaced
            // selected item in the original food slot until reverse SWAP.
            if(!STAGED_SERVER_ACK_READY&&!experimental26StagingEnabled()
                    &&!originalScratch.isEmpty()){
                clear();return;
            }
            mainhandOriginalSelected=selected;
            mainhandAutoSelected=scratchIndex;
            state=State.STAGED_SWAP;
            return;
        }
        if(path==AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR){
            mainhandOriginalSelected=SilentAutoEatCompat.selectedSlot(inventory);
            mainhandAutoSelected=candidate;
            state=State.MAIN_HAND_SELECT;
            return;
        }
        state=State.SWAP_TO_OFFHAND;
    }

    private static boolean preferSwapFreeHotbar(AutoEatOffhandPolicy policy){
        //#if MC >= 260300
        //$$ // ALLOW_REPLACE explicitly opts into the existing offhand SWAP.
        //$$ return policy!=AutoEatOffhandPolicy.ALLOW_REPLACE;
        //#else
        return false;
        //#endif
    }

    private static int findFood(LocalPlayer player,Inventory inventory,ItemStack offhand,
                                boolean preferHotbar){
        int selected=SilentAutoEatCompat.selectedSlot(inventory);
        if(selected>=0&&selected<9
                &&SilentAutoEatCompat.menuSlot(player,selected)>=0
                &&isEligibleFood(inventory.getItem(selected)))return selected;
        if(isEligibleFood(offhand))return -1;

        return AutoEatExecutionPlanner.selectFoodSource(
                (AutoEatOffhandPolicy)Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY.getOptionListValue(),
                offhand.isEmpty(),isProtectedOffhand(offhand),selected,
                index->SilentAutoEatCompat.menuSlot(player,index)>=0&&isEligibleFood(inventory.getItem(index)),
                preferHotbar,STAGED_SERVER_ACK_READY||experimental26StagingEnabled()
        );
    }

    /** Critical offhand items retain their real server-owned slot throughout fallback. */
    private static boolean isProtectedOffhand(ItemStack stack){
        return !stack.isEmpty()
                &&(stack.getItem()==Items.TOTEM_OF_UNDYING||stack.getItem()==Items.SHIELD);
    }

    private static boolean isEligibleFood(ItemStack stack){
        return SilentAutoEatFoodRules.isAllowed(stack,Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST.getStrings());
    }

    /**
     * Stage inventory food into a non-selected hotbar slot. The scratch item,
     * including nonempty stacks, is held in the source inventory slot until
     * the reverse SWAP has been acknowledged.
     */
    private static void stageToHotbar(Minecraft minecraft){
        if(!canContinueTransaction(minecraft)
                ||!AutoEatScratchSelection.isOwnedStageSource(sourceIndex,scratchIndex)){
            clear();return;
        }
        if(!ItemStack.matches(owner.getOffhandItem(),originalOffhand)){
            clear();return;
        }
        Inventory inv=SilentAutoEatCompat.inventory(owner);
        if(SilentAutoEatCompat.selectedSlot(inv)!=mainhandOriginalSelected
                ||!ItemStack.matches(inv.getItem(sourceIndex),selectedFood)
                ||!ItemStack.matches(inv.getItem(scratchIndex),originalScratch)){
            clear();return;
        }
        if(!SilentAutoEatCompat.swapWithHotbar(
                minecraft,owner,transactionMenu,sourceMenuSlot,scratchIndex)){
            clear();return;
        }
        stagingSwapSent=true;
        serverSlotEvidence.arm(transactionContainerId,sourceMenuSlot,
                SilentAutoEatCompat.menuSlot(owner,scratchIndex),originalScratch,selectedFood);
        stagingAckDeadline=clientTicks+SWAP_ACK_TICKS;
        stagingRestoreDeadline=clientTicks+RESTORE_GRACE_TICKS;
        state=State.STAGED_WAIT_ACK;
    }

    private static boolean stagedSourceOwned(){
        return sourceIndex>=9&&sourceIndex<36&&scratchIndex>=0&&scratchIndex<9
                &&ItemStack.matches(SilentAutoEatCompat.inventory(owner).getItem(sourceIndex),originalScratch);
    }

    private static boolean hasStagingAck(){
        return stagedSourceOwned()
                &&SilentAutoEatFoodRules.sameStackIgnoringCount(
                        SilentAutoEatCompat.inventory(owner).getItem(scratchIndex),selectedFood);
    }

    private static boolean stagedScratchRecoverable(){
        if(!stagedSourceOwned())return false;
        ItemStack current=SilentAutoEatCompat.inventory(owner).getItem(scratchIndex);
        return SilentAutoEatFoodRules.sameStackIgnoringCount(current,selectedFood)
                ||completionObserved&&(current.isEmpty()||current.getItem()==Items.BOWL
                ||current.getItem()==Items.GLASS_BOTTLE);
    }

    private static boolean hasConfirmedStageRestore(){
        if(sourceIndex<9||sourceIndex>=36||scratchIndex<0||scratchIndex>=9)return false;
        Inventory inv=SilentAutoEatCompat.inventory(owner);
        ItemStack source=inv.getItem(sourceIndex);
        return ItemStack.matches(inv.getItem(scratchIndex),originalScratch)
                &&(SilentAutoEatFoodRules.sameStackIgnoringCount(source,selectedFood)
                ||completionObserved&&(source.isEmpty()||source.getItem()==Items.BOWL
                ||source.getItem()==Items.GLASS_BOTTLE));
    }

    private static void waitForStagingAck(Minecraft minecraft){
        if(!canContinueTransaction(minecraft)
                ||SilentAutoEatCompat.selectedSlot(SilentAutoEatCompat.inventory(owner))!=mainhandOriginalSelected){
            requestStageRestore(minecraft,true);
            return;
        }
        if(hasStagingAck()){
            stagingConfirmed=true;
            // Consumption may change the food count, but the saved scratch
            // item in the source must still match server-side corrections.
            serverSlotEvidence.retainSourceDuringConsumption();
            state=State.MAIN_HAND_SELECT;
        }else if(clientTicks>=stagingAckDeadline){
            requestStageRestore(minecraft,true);
        }
    }

    /** Never restore selection on top of a player's new hotbar choice. */
    private static void requestStageRestore(Minecraft minecraft,boolean interrupted){
        if(!stagingSwapSent){clear();return;}
        // Wait for vanilla to finalize a consumed stack/remainder before
        // returning the food slot. Repeated cancels must not push this back.
        if(state!=State.STAGED_RESTORE)
            restoreReadyTick=Math.max(restoreReadyTick,clientTicks+1);
        if(owner!=null&&owner==minecraft.player){
            if(interrupted&&mainhandSelectionOwned()&&owner.isUsingItem()
                    &&owner.getUsedItemHand()==InteractionHand.MAIN_HAND
                    &&minecraft.gameMode!=null){
                minecraft.gameMode.releaseUsingItem(owner);
                restoreReadyTick=clientTicks+1;
            }
            if(mainhandSelectedByAuto&&mainhandSelectionOwned())
                SilentAutoEatCompat.selectHotbar(SilentAutoEatCompat.inventory(owner),
                        mainhandOriginalSelected);
        }
        state=State.STAGED_RESTORE;
        restoreStagedHotbar(minecraft);
    }

    private static void restoreStagedHotbar(Minecraft minecraft){
        if(owner==null||owner!=minecraft.player||minecraft.gameMode==null){
            clear();return;
        }
        // A server correction invalidates the assumed layout: never issue a
        // speculative reverse SWAP against slots the server disagrees with.
        if(serverSlotEvidence.hasConflict()){
            expireStagedTransaction();return;
        }
        if(stagingRestoreSent){
            if(hasConfirmedStageRestore()){
                rememberStagedMainhandVisual();
                observeSettledStage();
                clear();
            }else expireStagedTransaction();
            return;
        }
        // If the first SWAP was never observed, do not send a reverse SWAP:
        // it could swap the wrong items or arrive before the pending first click.
        if(!stagingConfirmed){
            if(hasStagingAck())stagingConfirmed=true;
            else{expireStagedTransaction();return;}
        }
        if(!stagedScratchRecoverable()){
            expireStagedTransaction();
            return;
        }
        if(!SilentAutoEatCompat.isSafeAutomationContext(minecraft,owner)){
            // Only an unsent reverse click with proven slot ownership can wait.
            stagingRestoreDeadline=clientTicks+RESTORE_GRACE_TICKS;
            return;
        }
        if(clientTicks<restoreReadyTick
                ||owner.isUsingItem()&&owner.getUsedItemHand()==InteractionHand.MAIN_HAND){
            expireStagedTransaction();return;
        }
        int menuSlot=SilentAutoEatCompat.menuSlot(owner,sourceIndex);
        if(menuSlot<0){expireStagedTransaction();return;}
        // Capture the restored source before the locally predicted click:
        // afterward the scratch slot can already appear to hold originalScratch.
        ItemStack expectedSource=SilentAutoEatCompat.inventory(owner).getItem(scratchIndex).copy();
        if(!SilentAutoEatCompat.swapWithHotbar(minecraft,owner,
                owner.containerMenu,menuSlot,scratchIndex)){
            expireStagedTransaction();return;
        }
        stagingRestoreSent=true;
        serverSlotEvidence.arm(transactionContainerId,menuSlot,
                SilentAutoEatCompat.menuSlot(owner,scratchIndex),
                expectedSource,originalScratch);
        stagingRestoreDeadline=clientTicks+RESTORE_GRACE_TICKS;
    }

    /** Only observed server corrections, not local optimistic state, can revoke settlement. */
    private static boolean isSettledStageContextValid(){
        Minecraft minecraft=Minecraft.getInstance();
        return settledStageOwner!=null&&clientTicks<=settledStageDeadline
                &&minecraft.player==settledStageOwner
                &&minecraft.level==settledStageLevel
                &&settledStageOwner.containerMenu==settledStageOwner.inventoryMenu
                &&settledStageOwner.containerMenu.containerId==settledStageMenuId
                &&MinecraftCompat.screen(minecraft)==null;
    }

    private static void observeSettledStage(){
        if(owner==null||!hasConfirmedStageRestore())return;
        Inventory inv=SilentAutoEatCompat.inventory(owner);
        settledStageOwner=owner;
        settledStageLevel=ownerLevel;
        settledStageSource=sourceIndex;
        settledStageScratch=scratchIndex;
        settledStageMenuId=transactionContainerId;
        settledStageOriginal=originalScratch.copy();
        settledStageFood=selectedFood.copy();
        settledStageCompleted=completionObserved;
        settledStageDeadline=clientTicks+STAGED_SERVER_SETTLE_TICKS;
        settledStageEvidence.arm(settledStageMenuId,
                SilentAutoEatCompat.menuSlot(owner,sourceIndex),
                SilentAutoEatCompat.menuSlot(owner,scratchIndex),
                inv.getItem(sourceIndex),inv.getItem(scratchIndex));
    }

    private static void rejectSettledStage(){
        // Never issue a speculative third SWAP. Require a stable, recovered
        // inventory before Auto Eat can start any new transaction.
        failedStagingOwner=settledStageOwner;
        failedStagingLevel=settledStageLevel;
        failedStagingSource=settledStageSource;
        failedStagingScratch=settledStageScratch;
        failedStagingOriginalScratch=settledStageOriginal.copy();
        failedStagingFood=settledStageFood.copy();
        failedStagingCompleted=settledStageCompleted;
        failedStagingStableSince=-1;
        STAGING_EVIDENCE_LOG.warn("Auto Eat: late server correction after staged restore; automatic transactions paused");
        clearSettledStage();
    }

    private static void clearSettledStage(){
        settledStageOwner=null;
        settledStageLevel=null;
        settledStageSource=-1;
        settledStageScratch=-1;
        settledStageMenuId=-1;
        settledStageOriginal=ItemStack.EMPTY;
        settledStageFood=ItemStack.EMPTY;
        settledStageCompleted=false;
        settledStageDeadline=0;
        settledStageEvidence.clear();
    }

    /** Fail closed instead of guessing whether a sent SWAP succeeded. */
    private static void expireStagedTransaction(){
        if(clientTicks<stagingRestoreDeadline)return;
        failedStagingOwner=owner;
        failedStagingLevel=ownerLevel;
        failedStagingSource=sourceIndex;
        failedStagingScratch=scratchIndex;
        failedStagingOriginalScratch=originalScratch.copy();
        failedStagingFood=selectedFood.copy();
        failedStagingCompleted=completionObserved;
        failedStagingStableSince=-1;
        clear();
    }

    private static boolean hasConfirmedStagingRecovery(LocalPlayer player){
        if(!SilentAutoEatCompat.isSafeAutomationContext(Minecraft.getInstance(),player)
                ||player.isUsingItem()||failedStagingSource<9||failedStagingSource>=36
                ||failedStagingScratch<0||failedStagingScratch>=9)return false;
        Inventory inv=SilentAutoEatCompat.inventory(player);
        ItemStack source=inv.getItem(failedStagingSource);
        return ItemStack.matches(inv.getItem(failedStagingScratch),failedStagingOriginalScratch)
                &&(SilentAutoEatFoodRules.sameStackIgnoringCount(source,failedStagingFood)
                ||failedStagingCompleted&&(source.isEmpty()||source.getItem()==Items.BOWL
                ||source.getItem()==Items.GLASS_BOTTLE));
    }

    /** A protected offhand stays in place: only the selected hotbar index changes. */
    private static void selectMainhand(Minecraft minecraft){
        if(!canContinueTransaction(minecraft)||mainhandAutoSelected<0
                ||mainhandAutoSelected>=9
                ||stagingSwapSent&&(!stagingConfirmed||!stagedSourceOwned())){
            if(stagingSwapSent)requestStageRestore(minecraft,true);
            else clear();
            return;
        }
        Inventory inventory=SilentAutoEatCompat.inventory(owner);
        if(SilentAutoEatCompat.selectedSlot(inventory)!=mainhandOriginalSelected
                ||!ItemStack.matches(inventory.getItem(mainhandAutoSelected),selectedFood)){
            if(stagingSwapSent)requestStageRestore(minecraft,true);
            else clear();
            return;
        }
        mainhandSelectedByAuto=mainhandAutoSelected!=mainhandOriginalSelected;
        if(mainhandSelectedByAuto)
            SilentAutoEatCompat.selectHotbar(inventory,mainhandAutoSelected);
        state=State.MAIN_HAND_START_USE;
    }

    private static boolean mainhandSelectionOwned(){
        return owner!=null&&SilentAutoEatCompat.selectedSlot(
                SilentAutoEatCompat.inventory(owner))==mainhandAutoSelected;
    }

    private static void startMainhandUse(Minecraft minecraft){
        if(!canContinueTransaction(minecraft)||!mainhandSelectionOwned()
                ||!ItemStack.matches(owner.getMainHandItem(),selectedFood)){
            finishMainhand(minecraft,false);return;
        }
        // Vanilla synchronizes the carried slot through its normal use path.
        SilentAutoEatCompat.useMainhand(minecraft,owner);
        if(owner.isUsingItem()&&owner.getUsedItemHand()==InteractionHand.MAIN_HAND)
            state=State.MAIN_HAND_USING;
        else finishMainhand(minecraft,false);
    }

    private static void watchMainhandUse(Minecraft minecraft){
        boolean completed=!owner.isUsingItem()
                ||owner.getUsedItemHand()!=InteractionHand.MAIN_HAND;
        if(stagingSwapSent&&completed)completionObserved=true;
        if(!canContinueTransaction(minecraft)||!mainhandSelectionOwned()
                ||stagingSwapSent&&(!stagedSourceOwned()||!stagedScratchRecoverable())){
            finishMainhand(minecraft,true);return;
        }
        if(completed)finishMainhand(minecraft,false);
    }

    /** Preserve a new player-selected slot; never move any real item stack. */
    private static void finishMainhand(Minecraft minecraft,boolean interrupted){
        if(stagingSwapSent){
            requestStageRestore(minecraft,interrupted);
            return;
        }
        if(owner!=null&&owner==minecraft.player){
            if(interrupted&&state==State.MAIN_HAND_USING&&owner.isUsingItem()
                    &&owner.getUsedItemHand()==InteractionHand.MAIN_HAND
                    &&minecraft.gameMode!=null)
                minecraft.gameMode.releaseUsingItem(owner);
            if(mainhandSelectedByAuto&&mainhandSelectionOwned())
                SilentAutoEatCompat.selectHotbar(SilentAutoEatCompat.inventory(owner),
                        mainhandOriginalSelected);
        }
        clear();
    }

    private static boolean mainhandPath(){
        return state==State.MAIN_HAND_SELECT||state==State.MAIN_HAND_START_USE
                ||state==State.MAIN_HAND_USING;
    }

    private static void swapToOffhand(Minecraft minecraft){
        if(sourceIndex<0){clear();return;}
        if(!canContinueTransaction(minecraft)){clear();return;}

        Inventory inventory=SilentAutoEatCompat.inventory(owner);
        if(sourceSelectionConflicts(inventory)||!ItemStack.matches(inventory.getItem(sourceIndex),selectedFood)){clear();return;}

        SilentAutoEatCompat.swapWithOffhand(minecraft,owner,transactionMenu,sourceMenuSlot);
        swapped=true;
        swapDeadlineTick=clientTicks+SWAP_ACK_TICKS;
        state=State.WAIT_SWAP;
    }

    private static void waitForSwap(Minecraft minecraft){
        boolean valid=canContinueTransaction(minecraft);
        boolean selectionChanged=!valid||sourceSelectionConflicts(SilentAutoEatCompat.inventory(owner));
        if(!valid||selectionChanged){
            //#if MC == 11404
            //$$ ddstweak$traceLegacy11404Swap("transaction-aborted");
            //#endif
            queueRestore();
            return;
        }
        if(hasConfirmedSwap())state=State.START_USE;
        else if(clientTicks>=swapDeadlineTick){
            //#if MC == 11404
            //$$ ddstweak$traceLegacy11404Swap("ack-timeout");
            //#endif
            queueRestore();
        }
    }

    //#if MC == 11404
    //$$ private static void ddstweak$traceLegacy11404Swap(String reason){
    //$$     if(!LEGACY_LOG.isDebugEnabled()||owner==null||sourceIndex<0)return;
    //$$     Inventory inv=SilentAutoEatCompat.inventory(owner);
    //$$     ItemStack currentSource=inv.getItem(sourceIndex);
    //$$     ItemStack currentOffhand=owner.getOffhandItem();
    //$$     LEGACY_LOG.debug("[1.14.4] swap {} source={} menuSlot={} menuId={} " +
    //$$             "sourceMatchesOriginal={} offhandMatchesFood={} actualSource={} actualOffhand={} " +
    //$$             "expectedOriginal={} expectedFood={} selected={} mainhandSource={}",
    //$$             reason,sourceIndex,sourceMenuSlot,transactionContainerId,
    //$$             ItemStack.matches(currentSource,originalOffhand),
    //$$             SilentAutoEatFoodRules.sameStackIgnoringCount(currentOffhand,selectedFood),
    //$$             ddstweak$legacy11404Stack(currentSource),ddstweak$legacy11404Stack(currentOffhand),
    //$$             ddstweak$legacy11404Stack(originalOffhand),ddstweak$legacy11404Stack(selectedFood),
    //$$             SilentAutoEatCompat.selectedSlot(inv),selectedMainhandSource);
    //$$ }
    //$$ private static String ddstweak$legacy11404Stack(ItemStack stack){
    //$$     return stack.isEmpty()?"empty":SilentAutoEatCompat.itemId(stack)+" x"+stack.getCount();
    //$$ }
    //#endif
    private static boolean hasConfirmedSwap(){
        return sourceIndex>=0&&SilentAutoEatRules.hasConfirmedSwap(
                sourceStillOwnsOriginal(),
                SilentAutoEatFoodRules.sameStackIgnoringCount(owner.getOffhandItem(),selectedFood));
    }

    private static void startUse(Minecraft minecraft){
        if(!canContinueTransaction(minecraft)){cancelAndRestore(minecraft);return;}
        Inventory inventory=SilentAutoEatCompat.inventory(owner);
        if(sourceSelectionConflicts(inventory)||sourceIndex>=0&&!hasConfirmedSwap()){cancelAndRestore(minecraft);return;}
        if(!isEligibleFood(owner.getOffhandItem())){queueRestore();return;}

        SilentAutoEatCompat.useOffhand(minecraft,owner);
        if(owner.isUsingItem()&&owner.getUsedItemHand()==InteractionHand.OFF_HAND)state=State.USING;
        else queueRestore();
    }

    private static void watchUse(Minecraft minecraft){
        if(!canContinueTransaction(minecraft)){cancelAndRestore(minecraft);return;}
        Inventory inventory=SilentAutoEatCompat.inventory(owner);
        if(sourceSelectionConflicts(inventory)||sourceIndex>=0&&!sourceStillOwnsOriginal()){cancelAndRestore(minecraft);return;}

        if(!owner.isUsingItem()||owner.getUsedItemHand()!=InteractionHand.OFF_HAND){
            completionObserved=true;
            queueRestore();
        }
    }

    private static void queueRestore(){
        state=State.RESTORE_OFFHAND;
        restoreDeadlineTick=clientTicks+RESTORE_GRACE_TICKS;
    }

    private static void cancelAndRestore(Minecraft minecraft){
        if(stagingSwapSent||state==State.STAGED_SWAP){
            requestStageRestore(minecraft,true);
            return;
        }
        if(mainhandPath()){
            finishMainhand(minecraft,true);
            return;
        }
        boolean releasingUse=owner==minecraft.player&&owner!=null&&owner.isUsingItem()
                &&owner.getUsedItemHand()==InteractionHand.OFF_HAND&&minecraft.gameMode!=null;
        if(releasingUse){
            minecraft.gameMode.releaseUsingItem(owner);
            // Do not race a reverse SWAP against the vanilla release/use packet.
            restoreReadyTick=clientTicks+1;
        }
        if(state!=State.RESTORE_OFFHAND)queueRestore();
        if(!releasingUse)restoreAndClear(minecraft);
    }

    private static void restoreAndClear(Minecraft minecraft){
        if(sourceIndex<0||!swapped){clear();return;}
        if(owner==null||owner!=minecraft.player||minecraft.gameMode==null){clear();return;}
        if(restoreRequestSent){
            if(hasConfirmedRestore()){
                rememberRestoredMainhandVisual();
                rememberHotbarPopVisual();
                clear();
            }
            else expireUnrestorableTransaction();
            return;
        }
        if(!sourceStillOwnsOriginal()||!canRestoreOwnedOffhand()){
            expireUnrestorableTransaction();
            return;
        }
        if(!SilentAutoEatCompat.isSafeAutomationContext(minecraft,owner)){
            // When both real slots are still owned and no reverse SWAP was
            // sent, a QCA/foreign menu must pause the deadline even if the
            // original OFF_HAND use has not finished releasing yet.
            restoreDeadlineTick=clientTicks+RESTORE_GRACE_TICKS;
            return;
        }
        if(!SilentAutoEatRules.canSendRestoreSwap(clientTicks,restoreReadyTick,
                owner.isUsingItem()&&owner.getUsedItemHand()==InteractionHand.OFF_HAND)){
            expireUnrestorableTransaction();
            return;
        }
        AbstractContainerMenu menu=owner.containerMenu;
        int menuSlot=SilentAutoEatCompat.menuSlot(owner,sourceIndex);
        if(menuSlot<0){
            expireUnrestorableTransaction();
            return;
        }

        restoreRequestSent=true;
        SilentAutoEatCompat.swapWithOffhand(minecraft,owner,menu,menuSlot);
    }

    private static void clearHotbarPopVisual(){
        hotbarPopOwner=null;
        hotbarPopLevel=null;
        hotbarPopSource=-1;
        hotbarPopUntilTick=0;
        hotbarPopFood=ItemStack.EMPTY;
        hotbarPopOriginalOffhand=ItemStack.EMPTY;
        hotbarPopCompleted=false;
    }

    private static void rememberHotbarPopVisual(){
        //#if MC >= 260300
        //$$ // Preserve the earlier 26.3 inventory-only rendering: do not
        //$$ // extend hotbar pop/cache ownership into slots 9..35.
        //$$ if(owner==null||sourceIndex<0||sourceIndex>=9)return;
        //#else
        if(owner==null||sourceIndex<0||sourceIndex>=9||originalOffhand.isEmpty())return;
        //#endif
        hotbarPopOwner=owner;
        hotbarPopLevel=ownerLevel;
        hotbarPopSource=sourceIndex;
        //#if MC >= 260300
        //$$ // 26.3's extracted HUD render states may be drawn after slot update.
        //$$ hotbarPopUntilTick=clientTicks+12;
        //#else
        hotbarPopUntilTick=clientTicks+7;
        //#endif
        hotbarPopFood=selectedFood.copy();
        hotbarPopOriginalOffhand=originalOffhand.copy();
        hotbarPopCompleted=completionObserved;
    }

    private static void clearRestoredMainhandVisual(){
        restoredVisualOwner=null;
        restoredVisualLevel=null;
        restoredVisualSource=-1;
        restoredVisualUntilTick=0;
        restoredVisualFood=ItemStack.EMPTY;
        restoredVisualOriginalOffhand=ItemStack.EMPTY;
        restoredVisualCompleted=false;
    }

    private static void rememberRestoredMainhandVisual(){
        if(sourceIndex<0||sourceIndex>=9
                ||SilentAutoEatCompat.selectedSlot(SilentAutoEatCompat.inventory(owner))!=sourceIndex)return;
        restoredVisualOwner=owner;
        restoredVisualLevel=ownerLevel;
        restoredVisualSource=sourceIndex;
        restoredVisualUntilTick=clientTicks+HAND_CACHE_SETTLE_TICKS;
        restoredVisualFood=selectedFood.copy();
        restoredVisualOriginalOffhand=originalOffhand.copy();
        restoredVisualCompleted=completionObserved;
    }

    private static boolean hasConfirmedRestore(){
        if(!ItemStack.matches(owner.getOffhandItem(),originalOffhand))return false;
        ItemStack source=SilentAutoEatCompat.inventory(owner).getItem(sourceIndex);
        return SilentAutoEatFoodRules.sameStackIgnoringCount(source,selectedFood)
                ||completionObserved&&(source.isEmpty()||source.getItem()==Items.BOWL
                ||source.getItem()==Items.GLASS_BOTTLE);
    }

    private static boolean canRestoreOwnedOffhand(){
        boolean foodStillOwned=offhandStillOwnedByAutoEat();
        ItemStack remainder=owner.getOffhandItem();
        // Only a completed vanilla consume may replace our food with an empty slot or known remainder.
        boolean vanillaRemainder=remainder.isEmpty()||remainder.getItem()==Items.BOWL
                ||remainder.getItem()==Items.GLASS_BOTTLE;
        return SilentAutoEatRules.canRestoreOffhand(foodStillOwned,completionObserved,vanillaRemainder);
    }

    private static void expireUnrestorableTransaction(){
        if(clientTicks>=restoreDeadlineTick){
            // A failed restoration must not immediately start another inventory transaction.
            failedRestoreOwner=owner;
            failedRestoreLevel=ownerLevel;
            failedRestoreSourceIndex=sourceIndex;
            failedRestoreOffhand=originalOffhand.copy();
            failedRestoreFood=selectedFood.copy();
            failedRestoreStableSince=-1;
            //#if MC == 260300
            //$$ RECOVERY_LOG.warn("[26.3] auto-eat restore expired: requestSent={} sourceOwned={} offhandOwned={}",
            //$$         restoreRequestSent,sourceStillOwnsOriginal(),canRestoreOwnedOffhand());
            //#endif
            clear();
        }
    }

    private static boolean hasConfirmedFailedRestore(Minecraft minecraft,LocalPlayer player){
        if(!SilentAutoEatCompat.isSafeAutomationContext(minecraft,player))return false;
        // The player may have moved the food after the transaction ended.
        // Only the offhand's safe return matters for rearming; food selection
        // must use the player's *current* inventory, not an obsolete slot.
        return failedRestoreSourceIndex>=0&&failedRestoreSourceIndex<36
                &&!player.isUsingItem()
                &&ItemStack.matches(player.getOffhandItem(),failedRestoreOffhand);
    }

    //#if MC == 260300
    //$$ /** Only log a changed blocked-recovery snapshot, not every game tick. */
    //$$ private static void diagnoseFailedRestore(Minecraft minecraft,LocalPlayer player){
    //$$     ItemStack actualOffhand=player.getOffhandItem();
    //$$     boolean safe=SilentAutoEatCompat.isSafeAutomationContext(minecraft,player);
    //$$     String snapshot="safe="+safe+" originalRestored="+
    //$$             ItemStack.matches(actualOffhand,failedRestoreOffhand)+
    //$$             " offhandFood="+SilentAutoEatFoodRules.sameStackIgnoringCount(actualOffhand,failedRestoreFood)+
    //$$             " source="+failedRestoreSourceIndex+
    //$$             " currentOffhand="+SilentAutoEatCompat.itemId(actualOffhand)+
    //$$             " expectedOffhand="+SilentAutoEatCompat.itemId(failedRestoreOffhand);
    //$$     if(!snapshot.equals(recoveryDiagnostic)){
    //$$         recoveryDiagnostic=snapshot;
    //$$         RECOVERY_LOG.warn("[26.3] blocked auto-eat restart after interrupted restore: {}",snapshot);
    //$$     }
    //$$ }
    //#endif
    private static void clearFailedStaging(){
        failedStagingOwner=null;
        failedStagingLevel=null;
        failedStagingSource=-1;
        failedStagingScratch=-1;
        failedStagingOriginalScratch=ItemStack.EMPTY;
        failedStagingFood=ItemStack.EMPTY;
        failedStagingCompleted=false;
        failedStagingStableSince=-1;
    }

    private static void clearFailedRestore(){
        //#if MC == 260300
        //$$ recoveryDiagnostic=null;
        //#endif
        failedRestoreOwner=null;
        failedRestoreLevel=null;
        failedRestoreSourceIndex=-1;
        failedRestoreOffhand=ItemStack.EMPTY;
        failedRestoreFood=ItemStack.EMPTY;
        failedRestoreStableSince=-1;
    }

    private static boolean canContinueTransaction(Minecraft minecraft){
        if(owner==null||minecraft.player!=owner||minecraft.gameMode==null||transactionMenu==null)return false;
        if(owner.containerMenu!=transactionMenu||transactionMenu.containerId!=transactionContainerId)return false;
        if(stagingSwapSent&&serverSlotEvidence.hasConflict())return false;
        if(!SilentAutoEatCompat.isSafeAutomationContext(minecraft,owner))return false;
        return sourceIndex<0||SilentAutoEatCompat.menuSlot(owner,sourceIndex)==sourceMenuSlot;
    }

    private static boolean sourceStillOwnsOriginal(){
        return sourceIndex>=0&&ItemStack.matches(
                SilentAutoEatCompat.inventory(owner).getItem(sourceIndex),originalOffhand
        );
    }

    private static boolean offhandStillOwnedByAutoEat(){
        ItemStack current=owner.getOffhandItem();
        return current.isEmpty()||SilentAutoEatFoodRules.sameStackIgnoringCount(current,selectedFood);
    }

    private static boolean sourceSelectionConflicts(Inventory inventory){
        return SilentAutoEatRules.sourceSelectionConflicts(
                sourceIndex,selectedMainhandSource,SilentAutoEatCompat.selectedSlot(inventory));
    }

    private static boolean relinquishOnPlayerHotbarIntent(Minecraft minecraft){
        if(!(mainhandPath()||stagingSwapSent)||!hasExplicitHotbarIntent(minecraft))
            return false;
        // A same-slot number key or pick-block does not change the selected
        // index, but the intent is still the player's, not Auto Eat's.
        mainhandSelectedByAuto=false;
        onExplicitPlayerAction(minecraft);
        return true;
    }

    private static boolean hasExplicitHotbarIntent(Minecraft minecraft){
        if(minecraft.options.keyPickItem.isDown())return true;
        for(int i=0;i<minecraft.options.keyHotbarSlots.length;i++)
            if(minecraft.options.keyHotbarSlots[i].isDown())return true;
        return false;
    }

    private static boolean hasExplicitConflict(Minecraft minecraft){
        return minecraft.options.keyUse.isDown()
                ||minecraft.options.keyAttack.isDown()
                ||minecraft.options.keySwapOffhand.isDown();
    }

    private static void clear(){
        state=State.IDLE;
        owner=null;
        ownerLevel=null;
        transactionMenu=null;
        transactionContainerId=-1;
        sourceIndex=-1;
        sourceMenuSlot=-1;
        selectedMainhandSource=false;
        mainhandOriginalSelected=-1;
        mainhandAutoSelected=-1;
        mainhandSelectedByAuto=false;
        scratchIndex=-1;
        serverSlotEvidence.clear();
        originalScratch=ItemStack.EMPTY;
        stagingSwapSent=false;
        stagingConfirmed=false;
        stagingRestoreSent=false;
        stagingAckDeadline=0;
        stagingRestoreDeadline=0;
        originalOffhand=ItemStack.EMPTY;
        selectedFood=ItemStack.EMPTY;
        swapped=false;
        completionObserved=false;
        restoreRequestSent=false;
        restoreDeadlineTick=0;
        swapDeadlineTick=0;
        restoreReadyTick=0;
    }

    enum State {
        IDLE,
        PREPARE,
        SWAP_TO_OFFHAND,
        WAIT_SWAP,
        START_USE,
        USING,
        RESTORE_OFFHAND,
        MAIN_HAND_SELECT,
        MAIN_HAND_START_USE,
        MAIN_HAND_USING,
        STAGED_SWAP,
        STAGED_WAIT_ACK,
        STAGED_RESTORE
    }
}

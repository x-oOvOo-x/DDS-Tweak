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

public final class SilentAutoEat {
    private static State state=State.IDLE;
    private static LocalPlayer owner;
    private static ClientLevel ownerLevel;
    private static AbstractContainerMenu transactionMenu;
    private static int transactionContainerId=-1;
    private static int sourceIndex=-1;
    private static int sourceMenuSlot=-1;
    private static ItemStack originalOffhand=ItemStack.EMPTY;
    private static ItemStack selectedFood=ItemStack.EMPTY;
    private static boolean swapped;
    private static boolean completionObserved;

    private SilentAutoEat(){}

    public static void initialize(){
        ClientTickEvents.START_CLIENT_TICK.register(SilentAutoEat::beforeTick);
        ClientTickEvents.END_CLIENT_TICK.register(SilentAutoEat::tick);
    }

    public static boolean isPresentationActive(LocalPlayer player){
        return owner==player&&state!=State.IDLE&&swapped;
    }

    static int presentationSourceIndex(){
        return sourceIndex;
    }

    static ItemStack presentationOriginalOffhand(){
        return originalOffhand;
    }

    public static void onExplicitPlayerAction(Minecraft minecraft){
        if(state!=State.IDLE)cancelAndRestore(minecraft);
    }

    public static boolean ownsUse(LocalPlayer player){
        return owner==player&&state==State.USING&&player.isUsingItem()
                &&player.getUsedItemHand()==InteractionHand.OFF_HAND;
    }

    static State stateForTest(){return state;}

    private static void beforeTick(Minecraft minecraft){
        if(state==State.IDLE||minecraft.player!=owner)return;
        if(hasExplicitConflict(minecraft))cancelAndRestore(minecraft);
    }

    private static void tick(Minecraft minecraft){
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

        if(!Configs.Tweaks.AUTO_EAT.getBooleanValue()){
            cancelAndRestore(minecraft);
            return;
        }

        switch(state){
            case PREPARE:prepare(minecraft);break;
            case SWAP_TO_OFFHAND:swapToOffhand(minecraft);break;
            case START_USE:startUse(minecraft);break;
            case USING:watchUse(minecraft);break;
            case RESTORE_OFFHAND:restoreAndClear(minecraft);break;
            default:clear();
        }
    }

    private static boolean shouldStart(Minecraft minecraft){
        LocalPlayer player=minecraft.player;
        if(player==null||minecraft.level==null||minecraft.gameMode==null||hasExplicitConflict(minecraft))return false;
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
        if(player==null||!SilentAutoEatCompat.isSafeAutomationContext(minecraft,player)){clear();return;}

        Inventory inventory=SilentAutoEatCompat.inventory(player);
        ItemStack offhand=player.getOffhandItem();
        int candidate=findFood(player,inventory,offhand);
        if(candidate==Integer.MIN_VALUE){clear();return;}

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
        sourceMenuSlot=SilentAutoEatCompat.menuSlot(player,candidate);
        if(sourceMenuSlot<0){clear();return;}
        selectedFood=inventory.getItem(candidate).copy();
        state=State.SWAP_TO_OFFHAND;
    }

    private static int findFood(LocalPlayer player,Inventory inventory,ItemStack offhand){
        if(isEligibleFood(offhand))return -1;

        int selected=SilentAutoEatCompat.selectedSlot(inventory);
        for(int i=9;i<36;i++)
            if(SilentAutoEatCompat.menuSlot(player,i)>=0&&isEligibleFood(inventory.getItem(i)))return i;
        for(int i=0;i<9;i++)
            if(i!=selected&&SilentAutoEatCompat.menuSlot(player,i)>=0&&isEligibleFood(inventory.getItem(i)))return i;
        return Integer.MIN_VALUE;
    }

    private static boolean isEligibleFood(ItemStack stack){
        return SilentAutoEatFoodRules.isAllowed(stack,Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST.getStrings());
    }

    private static void swapToOffhand(Minecraft minecraft){
        if(sourceIndex<0){clear();return;}
        if(!canContinueTransaction(minecraft)){clear();return;}

        Inventory inventory=SilentAutoEatCompat.inventory(owner);
        if(sourceIsSelectedHotbar(inventory)||!ItemStack.matches(inventory.getItem(sourceIndex),selectedFood)){clear();return;}

        SilentAutoEatCompat.swapWithOffhand(minecraft,owner,transactionMenu,sourceMenuSlot);
        swapped=true;
        state=State.START_USE;
    }

    private static void startUse(Minecraft minecraft){
        if(!canContinueTransaction(minecraft)){cancelAndRestore(minecraft);return;}
        Inventory inventory=SilentAutoEatCompat.inventory(owner);
        if(sourceIsSelectedHotbar(inventory)||sourceIndex>=0&&!sourceStillOwnsOriginal()){cancelAndRestore(minecraft);return;}
        if(!isEligibleFood(owner.getOffhandItem())){state=State.RESTORE_OFFHAND;return;}

        SilentAutoEatCompat.useOffhand(minecraft,owner);
        if(owner.isUsingItem()&&owner.getUsedItemHand()==InteractionHand.OFF_HAND)state=State.USING;
        else state=State.RESTORE_OFFHAND;
    }

    private static void watchUse(Minecraft minecraft){
        if(!canContinueTransaction(minecraft)){cancelAndRestore(minecraft);return;}
        Inventory inventory=SilentAutoEatCompat.inventory(owner);
        if(sourceIsSelectedHotbar(inventory)||sourceIndex>=0&&!sourceStillOwnsOriginal()){cancelAndRestore(minecraft);return;}

        if(!owner.isUsingItem()||owner.getUsedItemHand()!=InteractionHand.OFF_HAND){
            completionObserved=true;
            state=State.RESTORE_OFFHAND;
        }
    }

    private static void cancelAndRestore(Minecraft minecraft){
        if(owner==minecraft.player&&owner!=null&&owner.isUsingItem()
                &&owner.getUsedItemHand()==InteractionHand.OFF_HAND&&minecraft.gameMode!=null)
            minecraft.gameMode.releaseUsingItem(owner);
        state=State.RESTORE_OFFHAND;
        restoreAndClear(minecraft);
    }

    private static void restoreAndClear(Minecraft minecraft){
        if(sourceIndex<0||!swapped){clear();return;}
        if(owner==null||owner!=minecraft.player||minecraft.gameMode==null){clear();return;}
        if(!sourceStillOwnsOriginal()){clear();return;}
        if(!completionObserved&&!offhandStillOwnedByAutoEat()){clear();return;}

        AbstractContainerMenu menu=owner.containerMenu;
        if(menu==null||!MinecraftCompat.carried(menu,owner).isEmpty())return;
        int menuSlot=SilentAutoEatCompat.menuSlot(owner,sourceIndex);
        if(menuSlot<0)return;

        SilentAutoEatCompat.swapWithOffhand(minecraft,owner,menu,menuSlot);
        clear();
    }

    private static boolean canContinueTransaction(Minecraft minecraft){
        if(owner==null||minecraft.player!=owner||minecraft.gameMode==null||transactionMenu==null)return false;
        if(owner.containerMenu!=transactionMenu||transactionMenu.containerId!=transactionContainerId)return false;
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

    private static boolean sourceIsSelectedHotbar(Inventory inventory){
        return sourceIndex>=0&&sourceIndex<9&&SilentAutoEatCompat.selectedSlot(inventory)==sourceIndex;
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
        originalOffhand=ItemStack.EMPTY;
        selectedFood=ItemStack.EMPTY;
        swapped=false;
        completionObserved=false;
    }

    enum State {
        IDLE,
        PREPARE,
        SWAP_TO_OFFHAND,
        START_USE,
        USING,
        RESTORE_OFFHAND
    }
}

package ddstweak.integration.dds.qca;

import ddstweak.network.*;
import ddstweak.integration.dds.playerinventory.PlayerInventoryAction;
import ddstweak.integration.dds.playerinventory.PlayerInventoryContext;
import ddstweak.integration.dds.playerinventory.PlayerInventoryKind;
import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.playerinventory.PlayerInventoryOpenContext;
import ddstweak.integration.provider.ProviderPlayerInventoryScreenCoordinator;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.integration.org.playerinventory.OrgPlayerInventoryOpenIntent;
import ddstweak.integration.org.playerinventory.OrgPlayerInventoryAdapter;
import ddstweak.integration.gca.playerinventory.GcaPlayerInventoryAdapter;
import ddstweak.platform.minecraft.MinecraftCompat;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Client network/session slice for DDS quickContainerAccess. */
public final class DdsQcaClientNetwork {
    private static final long CREATIVE_TIMEOUT_NANOS=5_000_000_000L,OWNER_ENDER_TIMEOUT_NANOS=3_000_000_000L;
    private static final Queue<Integer> CREATIVE_QUEUE=new ArrayDeque<>();
    private static final Map<String,PlayerInventoryContext> PROVIDER_CONTEXTS=new LinkedHashMap<String,PlayerInventoryContext>(){
        @Override protected boolean removeEldestEntry(Map.Entry<String,PlayerInventoryContext> eldest){return size()>32;}
    };
    private static int nextTransaction=1,pendingTransaction=-1,navigationContainer=-1,pendingBack=-1;
    private static long pendingSince;private static AbstractContainerMenu pendingMenu;private static PendingOwnerEnder pendingOwnerEnder;private static Screen playerInventoryParent;private static PlayerInventoryContext nestedEnderParent,nestedInventoryParent;private static int nestedEnderSource=-1,nestedEnderChild=-1,nestedInventorySource=-1,nestedInventoryChild=-1;private static boolean nestedInventoryViaEnder,initialized;
    private DdsQcaClientNetwork(){}
    public static synchronized void initialize(){if(initialized)return;for(String id:new String[]{DdsQcaPackets.OPEN_SLOT_C2S,DdsQcaPackets.OPEN_INVENTORY_SLOT_C2S,DdsQcaPackets.STORAGE_CLICK_C2S,DdsQcaPackets.CREATIVE_STORAGE_C2S,DdsQcaPackets.NAVIGATE_BACK_C2S,DdsQcaPackets.OPEN_OWNER_ENDER_C2S,DdsQcaPackets.OWNER_ENDER_ACTION_C2S})DdsClientNetwork.registerC2S(id);DdsClientNetwork.registerS2CHandler(DdsQcaPackets.CREATIVE_STORAGE_RESULT_S2C,DdsQcaClientNetwork::handleCreativeStorageResult);DdsClientNetwork.registerS2CHandler(DdsQcaPackets.NAVIGATION_STATE_S2C,DdsQcaClientNetwork::handleNavigationState);DdsClientSessionHooks.registerResetHook(DdsQcaClientNetwork::resetSession);initialized=true;}
    /** Retain provider metadata for an Ender menu reopened later by QCA. */
    public static void rememberProviderContext(PlayerInventoryContext context){
        if(context==null||context.targetName().trim().isEmpty()
                ||!GcaPlayerInventoryAdapter.SOURCE.equals(context.source())
                &&!OrgPlayerInventoryAdapter.SOURCE.equals(context.source()))return;
        PROVIDER_CONTEXTS.put(context.targetUuid()==null?"name:"+context.targetName():"uuid:"+context.targetUuid(),context);
    }
    private static PlayerInventoryContext providerContext(UUID ownerUuid,String ownerName){
        PlayerInventoryContext source=PROVIDER_CONTEXTS.get("uuid:"+ownerUuid);
        if(source==null)source=PROVIDER_CONTEXTS.get("name:"+ownerName);
        return source!=null&&(ownerUuid.equals(source.targetUuid())
                ||source.targetUuid()==null&&ownerName.equals(source.targetName()))?source:null;
    }
    public static void revokeProviderExtensionAuthority(UUID targetUuid){
        if(targetUuid==null)return;
        for(Map.Entry<String,PlayerInventoryContext> entry:PROVIDER_CONTEXTS.entrySet())
            if(targetUuid.equals(entry.getValue().targetUuid()))
                entry.setValue(PlayerIdentityResolver.withoutStrictSession(entry.getValue()));
        if(nestedEnderParent!=null&&targetUuid.equals(nestedEnderParent.targetUuid()))
            nestedEnderParent=PlayerIdentityResolver.withoutStrictSession(nestedEnderParent);
        if(nestedInventoryParent!=null&&targetUuid.equals(nestedInventoryParent.targetUuid()))
            nestedInventoryParent=PlayerIdentityResolver.withoutStrictSession(nestedInventoryParent);
        if(pendingOwnerEnder!=null&&targetUuid.equals(pendingOwnerEnder.targetUuid))
            pendingOwnerEnder.context=PlayerIdentityResolver.withoutStrictSession(pendingOwnerEnder.context);
        PlayerInventoryOpenContext.revokeStrictAuthority(targetUuid);
    }

    public static boolean canUseOpenSlot(){return DdsClientNetwork.canTakeOverInput(DdsQcaPackets.OPEN_SLOT_C2S);}
    public static boolean canUseOpenInventorySlot(){return DdsClientNetwork.canTakeOverInput(DdsQcaPackets.OPEN_INVENTORY_SLOT_C2S);}
    public static boolean canUseStorageClick(){return DdsClientNetwork.canTakeOverInput(DdsQcaPackets.STORAGE_CLICK_C2S);}
    public static boolean canUseCreativeStorage(){return DdsClientNetwork.canTakeOverInput(DdsQcaPackets.CREATIVE_STORAGE_C2S);}
    public static boolean canUseNavigateBack(){return DdsClientNetwork.canTakeOverInput(DdsQcaPackets.NAVIGATE_BACK_C2S);}
    public static boolean canUseOwnerEnderOpen(){return DdsClientNetwork.canTakeOverInput(DdsQcaPackets.OPEN_OWNER_ENDER_C2S);}
    public static boolean canUseOwnerEnderAction(){return DdsClientNetwork.canTakeOverInput(DdsQcaPackets.OWNER_ENDER_ACTION_C2S);}
    public static boolean creativeStoragePending(AbstractContainerMenu menu){expireCreativePending();if(pendingTransaction<0)return false;if(pendingMenu!=menu||!canUseCreativeStorage()){resetCreativeStorage();return false;}return true;}
    public static boolean sendOpenSlot(AbstractContainerMenu menu,int slotId){
        if(menu==null||!canUseOpenSlot())return false;
        rememberPlayerInventoryParent(menu);
        if(!send(DdsQcaPackets.OPEN_SLOT_C2S,menu.containerId,slotId))return false;
        if(menu instanceof UnifiedPlayerInventoryMirrorMenu){
            UnifiedPlayerInventoryMirrorMenu mirror=(UnifiedPlayerInventoryMirrorMenu)menu;
            if(mirror.kind()==PlayerInventoryKind.ENDER_CHEST){
                boolean fromInventory=nestedInventoryParent!=null&&nestedInventoryChild==menu.containerId
                        &&nestedInventoryParent.targetUuid()!=null
                        &&Objects.equals(nestedInventoryParent.targetUuid(),mirror.context().targetUuid());
                nestedEnderParent=mirror.context();nestedEnderSource=menu.containerId;nestedEnderChild=-1;
                if(fromInventory)nestedInventoryViaEnder=true;
                else clearNestedInventory();
            }else if(mirror.kind()==PlayerInventoryKind.INVENTORY){
                nestedInventoryParent=mirror.context();nestedInventorySource=menu.containerId;nestedInventoryChild=-1;
                nestedInventoryViaEnder=false;
                clearNestedEnder();
            }else {clearNestedEnder();clearNestedInventory();}
        }else {clearNestedEnder();clearNestedInventory();}
        return true;
    }
    /** The remote Ender rule can open nested storage via vanilla clicks when general QCA is disabled. */
    public static void rememberNativeNestedEnder(AbstractContainerMenu menu) {
        if(menu instanceof UnifiedPlayerInventoryMirrorMenu){
            UnifiedPlayerInventoryMirrorMenu mirror=(UnifiedPlayerInventoryMirrorMenu)menu;
            if(mirror.kind()==PlayerInventoryKind.ENDER_CHEST
                    &&mirror.context().targetUuid()!=null&&canUseNavigateBack()){
                nestedEnderParent=mirror.context();nestedEnderSource=menu.containerId;nestedEnderChild=-1;
            }
        }
    }
    public static boolean sendOpenInventorySlot(int inventorySlot){if(!canUseOpenInventorySlot())return false;rememberPlayerInventoryParent(null);CompoundTag d=new CompoundTag();d.putInt("inventory_slot",inventorySlot);return DdsClientNetwork.send(DdsQcaPackets.OPEN_INVENTORY_SLOT_C2S,d);}
    public static boolean sendStorageClick(int containerId,int slotId){return canUseStorageClick()&&send(DdsQcaPackets.STORAGE_CLICK_C2S,containerId,slotId);}
    public static boolean sendOpenOwnerEnder(AbstractContainerMenu menu,PlayerInventoryContext context){return menu!=null&&sendOpenOwnerEnder(menu.containerId,context,menu);}
    public static boolean sendOpenOwnerEnder(int containerId,PlayerInventoryContext context){return sendOpenOwnerEnder(containerId,context,null);}
    private static boolean sendOpenOwnerEnder(int containerId,PlayerInventoryContext context,AbstractContainerMenu sourceMenu){
        if(containerId<0||context==null||context.targetUuid()==null||!PlayerIdentityResolver.isAuthoritativeFake(context)||!canUseOwnerEnderOpen())return false;
        long now=System.nanoTime();UUID target=context.targetUuid();
        if(pendingOwnerEnder!=null&&pendingOwnerEnder.matches(containerId,target,sourceMenu,now))return true;
        CompoundTag d=new CompoundTag();d.putInt("container_id",containerId);
        if(!DdsClientNetwork.send(DdsQcaPackets.OPEN_OWNER_ENDER_C2S,d))return false;
        pendingOwnerEnder=new PendingOwnerEnder(containerId,target,context,sourceMenu,now);return true;
    }
    public static void stageOwnerEnderFallback(AbstractContainerMenu staging,PlayerInventoryContext context,Component title){
        PendingOwnerEnder pending=pendingOwnerEnder;
        if(staging==null||context==null||context.targetUuid()==null||pending==null
                ||pending.sourceContainerId!=staging.containerId||!pending.targetUuid.equals(context.targetUuid())
                ||pending.sourceMenu!=null&&pending.sourceMenu!=staging)return;
        pending.stagingMenu=staging;pending.context=context;pending.title=title;
    }
    public static void onClientTick(){expireCreativePending();expireOwnerEnderPending();}
    private static void expireOwnerEnderPending(){
        PendingOwnerEnder pending=pendingOwnerEnder;if(pending==null)return;
        Minecraft mc=Minecraft.getInstance();
        AbstractContainerMenu watched=pending.stagingMenu!=null?pending.stagingMenu:pending.sourceMenu;
        if(watched!=null&&mc.player!=null&&mc.player.containerMenu!=watched){pendingOwnerEnder=null;return;}
        if(canUseOwnerEnderOpen()&&System.nanoTime()-pending.startedNanos<OWNER_ENDER_TIMEOUT_NANOS)return;
        restoreOwnerEnderFallback();
    }
    private static boolean restoreOwnerEnderFallback(){
        PendingOwnerEnder pending=pendingOwnerEnder;pendingOwnerEnder=null;
        if(pending==null||pending.stagingMenu==null)return false;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.player.containerMenu!=pending.stagingMenu||MinecraftCompat.screen(mc)!=null)return false;
        PlayerInventoryOpenContext.clear();
        return ProviderPlayerInventoryScreenCoordinator.restoreProviderEnder(pending.stagingMenu,pending.context,pending.title);
    }
    public static boolean sendOwnerEnderAction(AbstractContainerMenu menu,PlayerInventoryAction action){
        if(!(menu instanceof UnifiedPlayerInventoryMirrorMenu)||!canUseOwnerEnderAction()||action==null)return false;
        UnifiedPlayerInventoryMirrorMenu mirror=(UnifiedPlayerInventoryMirrorMenu)menu;
        if(mirror.kind()!=PlayerInventoryKind.ENDER_CHEST
                ||!PlayerIdentityResolver.isAuthoritativeFake(mirror.context()))return false;
        int expectedSize=mirror.targetEnd();if(expectedSize!=27&&expectedSize!=54)return false;
        String wire;
        switch(action){
            case ENDER_DROP_ALL:wire="drop_all";break;
            case ENDER_STORE_ALL:wire="store_all";break;
            case ENDER_TAKE_ALL:wire="take_all";break;
            default:wire=null;
        }
        if(wire==null)return false;
        CompoundTag d=new CompoundTag();d.putInt("container_id",menu.containerId);d.putString("action",wire);d.putInt("expected_size",expectedSize);
        return DdsClientNetwork.send(DdsQcaPackets.OWNER_ENDER_ACTION_C2S,d);
    }
    public static boolean sendNavigateBack(AbstractContainerMenu menu){if(!canUseNavigateBack()){clearNavigation();return false;}if(menu==null||menu.containerId!=navigationContainer)return false;if(pendingBack==menu.containerId)return true;CompoundTag d=new CompoundTag();d.putInt("container_id",menu.containerId);if(!DdsClientNetwork.send(DdsQcaPackets.NAVIGATE_BACK_C2S,d))return false;pendingBack=menu.containerId;
        if(nestedEnderParent!=null&&nestedEnderChild==menu.containerId){
            PlayerInventoryOpenContext.expectEnder(PlayerIdentityResolver.navigationFallback(nestedEnderParent));
            clearNestedEnder();
        }
        if(nestedInventoryParent!=null&&nestedInventoryChild==menu.containerId){
            PlayerInventoryContext parent=PlayerIdentityResolver.navigationFallback(nestedInventoryParent);
            if(OrgPlayerInventoryAdapter.SOURCE.equals(parent.source()))
                OrgPlayerInventoryOpenIntent.expectReopenInventory(parent);
            else if(GcaPlayerInventoryAdapter.SOURCE.equals(parent.source()))
                PlayerInventoryOpenContext.expectInventory(parent);
            clearNestedInventory();
        }
        return true;}
    public static boolean shouldSuppressTransitionClose(int containerId){if(!canUseNavigateBack()){clearNavigation();return false;}if(containerId==pendingBack)return true;if(containerId==navigationContainer){navigationContainer=-1;playerInventoryParent=null;}return false;}
    public static void beginCreativeStorageGesture(){CREATIVE_QUEUE.clear();expireCreativePending();}
    public static boolean sendCreativeStorageSlot(int inventorySlot,Player player,AbstractContainerMenu menu,ItemStack carried){if(!canUseCreativeStorage()||player==null||menu==null||carried==null||carried.isEmpty()||!DdsQcaClient.isCreativeSurvivalScreen()){resetCreativeStorage();return false;}expireCreativePending();if(pendingTransaction>=0&&pendingMenu!=menu)resetCreativeStorage();if(pendingTransaction>=0){CREATIVE_QUEUE.offer(inventorySlot);return true;}return sendCreativeNow(inventorySlot,player,menu,carried);}
    private static boolean sendCreativeNow(int inventorySlot,Player player,AbstractContainerMenu menu,ItemStack carried){if(carried==null||carried.isEmpty())return false;int tx=nextTransaction;nextTransaction=nextTransaction==Integer.MAX_VALUE?1:nextTransaction+1;CompoundTag d=new CompoundTag();d.putInt("transaction",tx);d.putInt("inventory_slot",inventorySlot);DdsQcaStackCodec.put(d,"carried",player,carried);if(!DdsClientNetwork.send(DdsQcaPackets.CREATIVE_STORAGE_C2S,d))return false;pendingTransaction=tx;pendingSince=System.nanoTime();pendingMenu=menu;return true;}
    private static void handleCreativeStorageResult(CompoundTag d){Minecraft mc=Minecraft.getInstance();if(mc.player==null||pendingMenu==null||!DdsQcaClient.isCreativeSurvivalScreen()){resetCreativeStorage();return;}int tx=MinecraftCompat.intOr(d,"transaction",-1);if(tx!=pendingTransaction)return;if(MinecraftCompat.intOr(d,"handled",-1)!=1){resetCreativeStorage();return;}ItemStack carried=DdsQcaStackCodec.get(d,"carried",mc.player);AbstractContainerMenu menu=pendingMenu;DdsQcaClient.applyCreativeCarried(menu,mc.player,carried);pendingTransaction=-1;pendingSince=0L;while(!CREATIVE_QUEUE.isEmpty()){int slot=CREATIVE_QUEUE.poll();if(!DdsQcaClient.isCreativeSurvivalScreen()){resetCreativeStorage();return;}if(sendCreativeNow(slot,mc.player,menu,carried))return;}pendingMenu=null;}
    private static void handleNavigationState(CompoundTag d){
        boolean rejected=MinecraftCompat.intOr(d,"rejected",-1)==1;
        boolean ownerEnderRejected=MinecraftCompat.intOr(d,"owner_ender_rejected",-1)==1;
        boolean openInventory=MinecraftCompat.intOr(d,"open_player_inventory",-1)==1;
        int id=MinecraftCompat.intOr(d,"container_id",-1);
        int requestId=MinecraftCompat.intOr(d,"request_container_id",-1);
        if(ownerEnderRejected&&pendingOwnerEnder!=null&&pendingOwnerEnder.sourceContainerId==requestId){
            restoreOwnerEnderFallback();
            return;
        }
        boolean enderSourceState=nestedEnderParent!=null&&id==nestedEnderSource;
        boolean inventorySourceState=nestedInventoryParent!=null&&id==nestedInventorySource;
        Minecraft mc=Minecraft.getInstance();
        String ownerText=MinecraftCompat.stringOr(d,"fake_ender_owner_uuid","");
        String ownerName=MinecraftCompat.stringOr(d,"fake_ender_owner_name","");
        boolean ownerEnderMatchesInventory=false;
        if(!rejected&&!openInventory&&id>=0&&!ownerText.trim().isEmpty()&&!ownerName.trim().isEmpty()){
            try{
                UUID ownerUuid=UUID.fromString(ownerText);
                ownerEnderMatchesInventory=nestedInventoryViaEnder&&nestedInventoryParent!=null
                        &&ownerUuid.equals(nestedInventoryParent.targetUuid())
                        &&mc.player!=null&&mc.player.containerMenu.containerId==id;
                PlayerInventoryOpenContext.expectVerifiedEnder(id,ownerUuid,ownerName,
                        providerContext(ownerUuid,ownerName));
                if(mc.player!=null&&mc.player.containerMenu.containerId==id)
                    ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent();
            }catch(IllegalArgumentException ignored){}
        }
        // A source menu's delayed no-back packet must not replace the child's navigation state.
        if(!rejected&&!openInventory&&(enderSourceState||inventorySourceState||nestedInventoryViaEnder)
                &&mc.player!=null&&mc.player.containerMenu.containerId!=id)return;

        navigationContainer=MinecraftCompat.intOr(d,"can_back",-1)==1?id:-1;
        if(nestedEnderParent!=null&&navigationContainer>=0&&id!=nestedEnderSource)nestedEnderChild=id;
        if(nestedInventoryParent!=null&&navigationContainer>=0&&id!=nestedInventorySource
                &&(!nestedInventoryViaEnder||ownerEnderMatchesInventory))nestedInventoryChild=id;
        if(rejected){
            if(pendingOwnerEnder!=null&&restoreOwnerEnderFallback())return;
            PlayerInventoryOpenContext.clear();
            clearNestedEnder();clearNestedInventory();pendingBack=-1;playerInventoryParent=null;
            Screen screen=MinecraftCompat.screen(mc);
            if(screen!=null)screen.onClose();
            return;
        }
        pendingBack=-1;
        if(openInventory){
            Screen parent=playerInventoryParent;playerInventoryParent=null;
            if(mc.player!=null){
                mc.player.containerMenu=mc.player.inventoryMenu;
                MinecraftCompat.setScreen(mc,parent!=null?parent:new InventoryScreen(mc.player));
            }
        }else if(navigationContainer<0){
            playerInventoryParent=null;
            if(!enderSourceState)clearNestedEnder();
            if(!inventorySourceState)clearNestedInventory();
        }
    }
    private static void rememberPlayerInventoryParent(AbstractContainerMenu menu){Minecraft mc=Minecraft.getInstance();Screen s=MinecraftCompat.screen(mc);if(mc.player!=null&&s!=null&&(menu==mc.player.inventoryMenu||DdsQcaClient.isCreativeSurvivalScreen()))playerInventoryParent=s;}
    private static void expireCreativePending(){if(pendingTransaction>=0&&System.nanoTime()-pendingSince>=CREATIVE_TIMEOUT_NANOS)resetCreativeStorage();}
    public static void resetSession(){restoreOwnerEnderFallback();resetCreativeStorage();clearNavigation();clearNestedEnder();clearNestedInventory();pendingOwnerEnder=null;playerInventoryParent=null;PROVIDER_CONTEXTS.clear();PlayerInventoryOpenContext.clear();}
    private static void resetCreativeStorage(){pendingTransaction=-1;pendingSince=0L;pendingMenu=null;CREATIVE_QUEUE.clear();}
    private static void clearNavigation(){navigationContainer=-1;pendingBack=-1;playerInventoryParent=null;clearNestedEnder();clearNestedInventory();}
    private static void clearNestedEnder(){nestedEnderParent=null;nestedEnderSource=-1;nestedEnderChild=-1;}
    private static void clearNestedInventory(){nestedInventoryParent=null;nestedInventorySource=-1;nestedInventoryChild=-1;nestedInventoryViaEnder=false;}
    private static boolean send(String packet,int containerId,int slotId){if(!DdsClientNetwork.canTakeOverInput(packet))return false;CompoundTag d=new CompoundTag();d.putInt("container_id",containerId);d.putInt("slot_id",slotId);return DdsClientNetwork.send(packet,d);}
    private static final class PendingOwnerEnder{
        private final int sourceContainerId;private final UUID targetUuid;private final AbstractContainerMenu sourceMenu;private final long startedNanos;
        private PlayerInventoryContext context;private AbstractContainerMenu stagingMenu;private Component title;
        private PendingOwnerEnder(int sourceContainerId,UUID targetUuid,PlayerInventoryContext context,AbstractContainerMenu sourceMenu,long startedNanos){this.sourceContainerId=sourceContainerId;this.targetUuid=targetUuid;this.context=context;this.sourceMenu=sourceMenu;this.startedNanos=startedNanos;}
        private boolean matches(int containerId,UUID target,AbstractContainerMenu source,long now){return sourceMenu==source&&sourceContainerId==containerId&&targetUuid.equals(target)&&now-startedNanos<OWNER_ENDER_TIMEOUT_NANOS;}
    }
}

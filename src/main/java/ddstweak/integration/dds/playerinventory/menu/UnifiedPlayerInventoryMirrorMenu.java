package ddstweak.integration.dds.playerinventory.menu;

import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.qca.DdsQcaSlotRoleProvider;
import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.platform.minecraft.MinecraftCompat;
//#if MC < 12104
//$$ import com.mojang.datafixers.util.Pair;
//#endif
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** Source-neutral client mirror. Vanilla container packets remain authoritative for slot and data state. */
public final class UnifiedPlayerInventoryMirrorMenu extends AbstractContainerMenu implements DdsQcaSlotRoleProvider{
    public static final int INVENTORY_EQUIPMENT_END=5,INVENTORY_MAIN_END=32,INVENTORY_HOTBAR_START=32,INVENTORY_TARGET_END=41,CARRIED_SCRATCH_BUTTON=9;
    public static final int GCA_MAIN_START=18,GCA_MAIN_END=45,GCA_HOTBAR_START=45,GCA_TARGET_END=54,GCA_CONTROLLER_END=27,GCA_ENDER_STORAGE_START=27,GCA_ENDER_TARGET_END=54;
    public static final int ORG_EQUIPMENT_END=41,ORG_DISABLED_START=43,ORG_TARGET_END=54;
    private static final int[] MAIN_MAPPING=createMapping(9,27),HOTBAR_MAPPING=createMapping(0,9),EQUIPMENT_MAPPING={39,38,37,36,40},
            GCA_CONTROL_MAPPING={0,5,6,8,9,10,11,12,13,14,15,16,17},GCA_EQUIPMENT_MAPPING={1,2,3,4,7},
            GCA_MAIN_MAPPING=createMapping(18,27),GCA_HOTBAR_MAPPING=createMapping(45,9),GCA_ENDER_STORAGE_MAPPING=createMapping(27,27),
            ORG_HOTBAR_MAPPING=createMapping(0,9),ORG_MAIN_MAPPING=createMapping(9,27),
            ORG_EQUIPMENT_MAPPING=createMapping(36,7),ORG_DISABLED_MAPPING=createMapping(43,11);
    private PlayerInventoryContext context;private final PlayerInventoryActionBinding actions;private final ContainerData state;
    private final int rows,targetEnd,targetMainStart,targetMainEnd,targetHotbarStart,targetHotbarEnd,viewerMainStart,viewerHotbarStart,viewerEnd;

    public static UnifiedPlayerInventoryMirrorMenu client(int id,Inventory viewer,PlayerInventoryContext context,PlayerInventoryActionBinding actions){return new UnifiedPlayerInventoryMirrorMenu(id,viewer,context,actions);}
    private UnifiedPlayerInventoryMirrorMenu(int id,Inventory viewer,PlayerInventoryContext context,PlayerInventoryActionBinding actions){
        super(menuType(context),id);this.context=Objects.requireNonNull(context);this.actions=Objects.requireNonNull(actions);if(context.containerId()!=id)throw new IllegalArgumentException("containerId");
        rows=PlayerInventoryLayouts.enderRows(context);int dataCount=actions.dataSlotCount(context);if(dataCount<0)throw new IllegalArgumentException("dataSlotCount");state=new SimpleContainerData(dataCount);
        if(PlayerInventoryLayouts.isGcaController(context)){
            SimpleContainer target=new SimpleContainer(GCA_CONTROLLER_END);addGcaController(target,viewer);targetEnd=GCA_CONTROLLER_END;targetMainStart=GCA_CONTROLLER_END;targetMainEnd=GCA_CONTROLLER_END;targetHotbarStart=9;targetHotbarEnd=18;viewerMainStart=GCA_CONTROLLER_END;viewerHotbarStart=viewerMainStart+27;viewerEnd=viewerHotbarStart+9;
        }else if(PlayerInventoryLayouts.isControlledInventory(context)){
            SimpleContainer target=new SimpleContainer(GCA_TARGET_END);addGcaInventory(target,viewer);targetEnd=GCA_TARGET_END;targetMainStart=GCA_MAIN_START;targetMainEnd=GCA_MAIN_END;targetHotbarStart=GCA_HOTBAR_START;targetHotbarEnd=GCA_TARGET_END;viewerMainStart=GCA_TARGET_END;viewerHotbarStart=viewerMainStart+27;viewerEnd=viewerHotbarStart+9;
        }else if(PlayerInventoryLayouts.isOrgInventory(context)){
            SimpleContainer target=new SimpleContainer(ORG_TARGET_END);addOrgInventory(target,viewer);targetEnd=ORG_TARGET_END;targetMainStart=9;targetMainEnd=36;targetHotbarStart=0;targetHotbarEnd=9;viewerMainStart=ORG_TARGET_END;viewerHotbarStart=viewerMainStart+27;viewerEnd=viewerHotbarStart+9;
        }else if(PlayerInventoryLayouts.isInventory(context)){
            SimpleContainer target=new SimpleContainer(INVENTORY_TARGET_END);addEquipment(new MappedContainer(target,EQUIPMENT_MAPPING));addInventory(new MappedContainer(target,MAIN_MAPPING),new MappedContainer(target,HOTBAR_MAPPING),viewer);targetEnd=INVENTORY_TARGET_END;targetMainStart=INVENTORY_EQUIPMENT_END;targetMainEnd=INVENTORY_MAIN_END;targetHotbarStart=INVENTORY_HOTBAR_START;targetHotbarEnd=INVENTORY_TARGET_END;viewerMainStart=INVENTORY_TARGET_END;viewerHotbarStart=viewerMainStart+27;viewerEnd=viewerHotbarStart+9;
        }else if(PlayerInventoryLayouts.isGcaEnder(context)){
            SimpleContainer target=new SimpleContainer(GCA_ENDER_TARGET_END);addGcaEnder(target,viewer);targetEnd=GCA_ENDER_TARGET_END;targetMainStart=GCA_ENDER_STORAGE_START;targetMainEnd=GCA_ENDER_TARGET_END;targetHotbarStart=GCA_ENDER_TARGET_END;targetHotbarEnd=GCA_ENDER_TARGET_END;viewerMainStart=GCA_ENDER_TARGET_END;viewerHotbarStart=viewerMainStart+27;viewerEnd=viewerHotbarStart+9;
        }else{
            SimpleContainer target=new SimpleContainer(rows*9);addEnder(target,viewer);targetEnd=rows*9;targetMainStart=0;targetMainEnd=targetEnd;targetHotbarStart=targetEnd;targetHotbarEnd=targetEnd;viewerMainStart=targetEnd;viewerHotbarStart=targetEnd+27;viewerEnd=targetEnd+36;
        }
        addDataSlots(state);actions.attach(this);
    }
    //#if MC >= 11500
    private static Identifier helmetIcon(){return InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;}
    private static Identifier chestplateIcon(){return InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;}
    private static Identifier leggingsIcon(){return InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;}
    private static Identifier bootsIcon(){return InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;}
    private static Identifier shieldIcon(){return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;}
    //#else
    //$$ private static String helmetIcon(){return "item/empty_armor_slot_helmet";}
    //$$ private static String chestplateIcon(){return "item/empty_armor_slot_chestplate";}
    //$$ private static String leggingsIcon(){return "item/empty_armor_slot_leggings";}
    //$$ private static String bootsIcon(){return "item/empty_armor_slot_boots";}
    //$$ private static String shieldIcon(){return "item/empty_armor_slot_shield";}
    //#endif
    private static MenuType<?> menuType(PlayerInventoryContext context){if(PlayerInventoryLayouts.isGcaController(context))return MenuType.GENERIC_9x3;if(PlayerInventoryLayouts.isInventory(context)||PlayerInventoryLayouts.isGcaEnder(context))return MenuType.GENERIC_9x6;int rows=PlayerInventoryLayouts.enderRows(context);if(rows==3)return MenuType.GENERIC_9x3;if(rows==6)return MenuType.GENERIC_9x6;throw new IllegalArgumentException("Unsupported layout");}
    private void addEquipment(Container c){addSlot(new EquipmentClientSlot(c,0,8,8,true,helmetIcon()));addSlot(new EquipmentClientSlot(c,1,8,26,true,chestplateIcon()));addSlot(new EquipmentClientSlot(c,2,8,44,true,leggingsIcon()));addSlot(new EquipmentClientSlot(c,3,8,62,true,bootsIcon()));addSlot(new EquipmentClientSlot(c,4,77,62,false,shieldIcon()));}
    private void addInventory(Container main,Container hotbar,Inventory viewer){for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(main,c+r*9,8+c*18,84+r*18));for(int c=0;c<9;c++)addSlot(new Slot(hotbar,c,8+c*18,142));addViewer(viewer,184,242);}
    private void addGcaInventory(SimpleContainer target,Inventory viewer){
        Container controls=new MappedContainer(target,GCA_CONTROL_MAPPING),equipment=new MappedContainer(target,GCA_EQUIPMENT_MAPPING),
                main=new MappedContainer(target,GCA_MAIN_MAPPING),hotbar=new MappedContainer(target,GCA_HOTBAR_MAPPING);
        addSlot(hidden(controls,0));
        addSlot(new EquipmentClientSlot(equipment,0,8,8,false,helmetIcon()));
        addSlot(new EquipmentClientSlot(equipment,1,8,26,false,chestplateIcon()));
        addSlot(new EquipmentClientSlot(equipment,2,8,44,false,leggingsIcon()));
        addSlot(new EquipmentClientSlot(equipment,3,8,62,false,bootsIcon()));
        addSlot(hidden(controls,1));addSlot(hidden(controls,2));addSlot(new EquipmentClientSlot(equipment,4,77,62,false,shieldIcon()));
        for(int i=3;i<GCA_CONTROL_MAPPING.length;i++)addSlot(hidden(controls,i));
        // Keep fake main and fake hotbar as distinct Container identities. ItemScroller
        // discovers an inventory range by Slot.container identity; sharing one 36-slot
        // wrapper makes it treat both regions as a single sortable inventory.
        for(int i=0;i<27;i++){int r=i/9,col=i%9;addSlot(new Slot(main,i,8+col*18,84+r*18));}
        for(int i=0;i<9;i++)addSlot(new Slot(hotbar,i,8+i*18,142));
        addViewer(viewer,184,242);
    }
    private void addGcaController(Container target,Inventory viewer){for(int i=0;i<GCA_CONTROLLER_END;i++){int r=i/9,c=i%9;addSlot(new Slot(target,i,8+c*18,84+r*18));}addViewer(viewer,184,242);}
    private void addOrgInventory(SimpleContainer target,Inventory viewer){
        Container hotbar=new MappedContainer(target,ORG_HOTBAR_MAPPING),main=new MappedContainer(target,ORG_MAIN_MAPPING),
                equipment=new MappedContainer(target,ORG_EQUIPMENT_MAPPING),disabled=new MappedContainer(target,ORG_DISABLED_MAPPING);
        // ItemScroller derives its initial sort domain from Slot.container identity. Keep
        // Org main/hotbar distinct just like the fixed GCA layout, while retaining menu ids.
        for(int i=0;i<9;i++)addSlot(new Slot(hotbar,i,8+i*18,142));
        for(int i=0;i<27;i++){int r=i/9,col=i%9;addSlot(new Slot(main,i,8+col*18,84+r*18));}
        addSlot(new EquipmentClientSlot(equipment,0,8,8,false,helmetIcon()));
        addSlot(new EquipmentClientSlot(equipment,1,8,26,false,chestplateIcon()));
        addSlot(new EquipmentClientSlot(equipment,2,8,44,false,leggingsIcon()));
        addSlot(new EquipmentClientSlot(equipment,3,8,62,false,bootsIcon()));
        addSlot(new EquipmentClientSlot(equipment,4,77,62,false,shieldIcon()));
        addSlot(hidden(equipment,5));addSlot(hidden(equipment,6));
        for(int i=0;i<ORG_DISABLED_MAPPING.length;i++)addSlot(hidden(disabled,i));
        addViewer(viewer,184,242);
    }
    private static Slot hidden(Container target,int index){return new Slot(target,index,-1000,-1000);}
    private void addViewer(Inventory viewer,int mainY,int hotbarY){for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(viewer,c+(r+1)*9,8+c*18,mainY+r*18));for(int c=0;c<9;c++)addSlot(new Slot(viewer,c,8+c*18,hotbarY));}
    private void addGcaEnder(SimpleContainer target,Inventory viewer){Container storage=new MappedContainer(target,GCA_ENDER_STORAGE_MAPPING);for(int i=0;i<GCA_ENDER_STORAGE_START;i++)addSlot(hidden(target,i));for(int i=0;i<27;i++){int r=i/9,c=i%9;addSlot(new Slot(storage,i,8+c*18,18+r*18));}addViewer(viewer,85,143);}
    private void addEnder(Container target,Inventory viewer){for(int r=0;r<rows;r++)for(int c=0;c<9;c++)addSlot(new Slot(target,c+r*9,8+c*18,18+r*18));int offset=(rows-4)*18;addViewer(viewer,103+offset,161+offset);}

    //#if MC >= 260102
    //$$ @Override public void clicked(int slotId,int button,ContainerInput type,Player player){
    //$$     if(slotRole(slotId)==PlayerInventorySlotRole.CONTROL&&actions.handleControlClick(slotId,button,type,player))return;
    //$$     if(type==ContainerInput.SWAP&&button==CARRIED_SCRATCH_BUTTON){if(context.allows(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP))swapWithCarried(slotId,player);return;}
    //$$     super.clicked(slotId,button,type,player);
    //$$ }
    //#elseif MC >= 11700
    @Override public void clicked(int slotId,int button,ClickType type,Player player){
        if(slotRole(slotId)==PlayerInventorySlotRole.CONTROL&&actions.handleControlClick(slotId,button,type,player))return;
        if(type==ClickType.SWAP&&button==CARRIED_SCRATCH_BUTTON){if(context.allows(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP))swapWithCarried(slotId,player);return;}
        super.clicked(slotId,button,type,player);
    }
    //#else
    //$$ @Override public ItemStack clicked(int slotId,int button,ClickType type,Player player){
    //$$     if(slotRole(slotId)==PlayerInventorySlotRole.CONTROL&&actions.handleControlClick(slotId,button,type,player))return ItemStack.EMPTY;
    //$$     if(type==ClickType.SWAP&&button==CARRIED_SCRATCH_BUTTON){if(context.allows(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP))swapWithCarried(slotId,player);return ItemStack.EMPTY;}
    //$$     return super.clicked(slotId,button,type,player);
    //$$ }
    //#endif
    private void swapWithCarried(int slotId,Player player){PlayerInventorySlotRole role=slotRole(slotId);if(role!=PlayerInventorySlotRole.MAIN&&role!=PlayerInventorySlotRole.HOTBAR&&role!=PlayerInventorySlotRole.ENDER_STORAGE)return;Slot slot=getSlot(slotId);ItemStack slotStack=slot.getItem(),carried=MinecraftCompat.carried(this,player);if(!slot.mayPickup(player)||!carried.isEmpty()&&!slot.mayPlace(carried))return;slot.set(carried);MinecraftCompat.setCarried(this,player,slotStack);}
    @Override public boolean stillValid(Player player){return true;}
    @Override public ItemStack quickMoveStack(Player player,int slotIndex){if(!context.allows(PlayerInventoryInteractionCapability.QUICK_MOVE_PREDICTION)||context.inventoryKind()!=PlayerInventoryKind.ENDER_CHEST||slotIndex<0||slotIndex>=slots.size())return ItemStack.EMPTY;Slot slot=slots.get(slotIndex);if(!slot.hasItem())return ItemStack.EMPTY;ItemStack stack=slot.getItem(),copy=stack.copy();if(slotIndex<targetEnd){if(!moveItemStackTo(stack,targetEnd,viewerEnd,true))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,0,targetEnd,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return copy;}

    public PlayerInventoryContext context(){return context;}
    /** Recheck live authority so a late snapshot can immediately disable controls on real players. */
    public PlayerInventoryTargetType targetType(){return PlayerIdentityResolver.currentType(context);}
    public boolean updateContext(PlayerInventoryContext updated){
        if(updated==null||updated.containerId()!=containerId||updated.inventoryKind()!=context.inventoryKind()||!updated.layoutId().equals(context.layoutId()))return false;
        context=updated;return true;
    }
    public PlayerInventoryKind kind(){return context.inventoryKind();}private boolean isActionInventory(){return kind()==PlayerInventoryKind.INVENTORY||kind()==PlayerInventoryKind.CONTROLLER;}public int rows(){return rows;}public int visualRows(){return rows;}public int targetEnd(){return targetEnd;}public int targetMainStart(){return targetMainStart;}public int targetMainEnd(){return targetMainEnd;}public int targetHotbarStart(){return targetHotbarStart;}public int targetHotbarEnd(){return targetHotbarEnd;}public int viewerMainStart(){return viewerMainStart;}public int viewerHotbarStart(){return viewerHotbarStart;}public int viewerEnd(){return viewerEnd;}
    public PlayerInventorySlotRole slotRole(int slot){
        if(slot<0||slot>=viewerEnd)return PlayerInventorySlotRole.UNKNOWN;
        if(kind()==PlayerInventoryKind.ENDER_CHEST){if(PlayerInventoryLayouts.isGcaEnder(context)){if(slot<GCA_ENDER_STORAGE_START)return PlayerInventorySlotRole.CONTROL;if(slot<GCA_ENDER_TARGET_END)return PlayerInventorySlotRole.ENDER_STORAGE;}else if(slot<targetEnd)return PlayerInventorySlotRole.ENDER_STORAGE;return slot<viewerHotbarStart?PlayerInventorySlotRole.VIEWER_MAIN:PlayerInventorySlotRole.VIEWER_HOTBAR;}
        if(PlayerInventoryLayouts.isGcaController(context)){if(slot<GCA_CONTROLLER_END)return PlayerInventorySlotRole.CONTROL;return slot<viewerHotbarStart?PlayerInventorySlotRole.VIEWER_MAIN:PlayerInventorySlotRole.VIEWER_HOTBAR;}
        if(PlayerInventoryLayouts.isControlledInventory(context)){
            if(slot>=viewerMainStart)return slot<viewerHotbarStart?PlayerInventorySlotRole.VIEWER_MAIN:PlayerInventorySlotRole.VIEWER_HOTBAR;
            if(slot>=GCA_MAIN_START&&slot<GCA_MAIN_END)return PlayerInventorySlotRole.MAIN;if(slot>=GCA_HOTBAR_START&&slot<GCA_TARGET_END)return PlayerInventorySlotRole.HOTBAR;if((slot>=1&&slot<=4)||slot==7)return PlayerInventorySlotRole.EQUIPMENT;return PlayerInventorySlotRole.CONTROL;
        }
        if(PlayerInventoryLayouts.isOrgInventory(context)){
            if(slot>=viewerMainStart)return slot<viewerHotbarStart?PlayerInventorySlotRole.VIEWER_MAIN:PlayerInventorySlotRole.VIEWER_HOTBAR;
            if(slot<9)return PlayerInventorySlotRole.HOTBAR;if(slot<36)return PlayerInventorySlotRole.MAIN;if(slot<ORG_EQUIPMENT_END)return PlayerInventorySlotRole.EQUIPMENT;if(slot<ORG_TARGET_END)return PlayerInventorySlotRole.DISABLED;
        }
        if(slot<INVENTORY_EQUIPMENT_END)return PlayerInventorySlotRole.EQUIPMENT;if(slot<INVENTORY_MAIN_END)return PlayerInventorySlotRole.MAIN;if(slot<INVENTORY_TARGET_END)return PlayerInventorySlotRole.HOTBAR;if(slot<viewerHotbarStart)return PlayerInventorySlotRole.VIEWER_MAIN;return PlayerInventorySlotRole.VIEWER_HOTBAR;
    }
    @Override public Role ddsTweak$qcaRole(int slot){switch(slotRole(slot)){case EQUIPMENT:return Role.TARGET_EQUIPMENT;case MAIN:case HOTBAR:return Role.TARGET_STORAGE;case ENDER_STORAGE:return Role.ENDER_STORAGE;case VIEWER_MAIN:case VIEWER_HOTBAR:return Role.VIEWER_INVENTORY;case CONTROL:return Role.CONTROL;case DISABLED:return Role.DISABLED;default:return Role.UNKNOWN;}}

    public boolean supports(PlayerInventoryAction action){
        if(action==null||!action.allowedIn(kind())||targetType()==PlayerInventoryTargetType.REAL)return false;
        if(action==PlayerInventoryAction.ENDER_DROP_ALL||action==PlayerInventoryAction.ENDER_STORE_ALL||action==PlayerInventoryAction.ENDER_TAKE_ALL){
            boolean canonical=PlayerInventoryLayouts.ENDER_3_V1.equals(context.layoutId())||PlayerInventoryLayouts.ENDER_6_V1.equals(context.layoutId());
            return canonical&&PlayerIdentityResolver.isAuthoritativeFake(context)&&DdsQcaClientNetwork.canUseOwnerEnderAction()
                    &&context.supports(action)&&actions.supports(action);
        }
        if((action==PlayerInventoryAction.DROP_ALL||action==PlayerInventoryAction.DISCONNECT||action==PlayerInventoryAction.TOGGLE_SNEAK)
                &&!PlayerIdentityResolver.isAuthoritativeFake(context))return false;
        return context.supports(action)&&actions.supports(action);
    }
    public boolean canSendActions(){return actions.canSend();}
    public boolean send(PlayerInventoryAction action,int value){return supports(action)&&actions.canSend()&&actions.send(action,value);}
    private boolean hasData(int index){return index>=0&&index<state.getCount();}
    public int selectedTargetHotbarSlot(){return !isActionInventory()?-1:hasData(0)?state.get(0):actions.selectedTargetHotbarSlot();}
    public int attackMode(){return hasData(1)?state.get(1):actions.attackMode();}
    public int attackInterval(){return hasData(2)?state.get(2):actions.attackInterval();}
    public int useMode(){return hasData(3)?state.get(3):actions.useMode();}
    public int useInterval(){return hasData(4)?state.get(4):actions.useInterval();}
    public boolean isSneakingActive(){return kind()==PlayerInventoryKind.ENDER_CHEST&&(hasData(0)?state.get(0)!=0:actions.sneaking());}
    public PlayerInventoryStateConfidence selectedHotbarConfidence(){return !isActionInventory()?PlayerInventoryStateConfidence.UNKNOWN:hasData(0)?PlayerInventoryStateConfidence.AUTHORITATIVE:actions.selectedHotbarConfidence();}
    public PlayerInventoryStateConfidence attackModeConfidence(){return hasData(1)?PlayerInventoryStateConfidence.AUTHORITATIVE:actions.attackModeConfidence();}
    public PlayerInventoryStateConfidence attackIntervalConfidence(){return hasData(2)?PlayerInventoryStateConfidence.AUTHORITATIVE:actions.attackIntervalConfidence();}
    public PlayerInventoryStateConfidence useModeConfidence(){return hasData(3)?PlayerInventoryStateConfidence.AUTHORITATIVE:actions.useModeConfidence();}
    public PlayerInventoryStateConfidence useIntervalConfidence(){return hasData(4)?PlayerInventoryStateConfidence.AUTHORITATIVE:actions.useIntervalConfidence();}
    public PlayerInventoryStateConfidence sneakingConfidence(){return kind()!=PlayerInventoryKind.ENDER_CHEST?PlayerInventoryStateConfidence.UNKNOWN:hasData(0)?PlayerInventoryStateConfidence.AUTHORITATIVE:actions.sneakingConfidence();}
    public boolean isAttackIntervalActive(){return attackMode()==PlayerInventoryBehaviorV1.MODE_INTERVAL;}public boolean isAttackContinuousActive(){return attackMode()==PlayerInventoryBehaviorV1.MODE_CONTINUOUS;}public boolean isUseIntervalActive(){return useMode()==PlayerInventoryBehaviorV1.MODE_INTERVAL;}public boolean isUseContinuousActive(){return useMode()==PlayerInventoryBehaviorV1.MODE_CONTINUOUS;}
    public void predictAttackInterval(int v){if(supports(PlayerInventoryAction.ATTACK_INTERVAL)&&hasData(2))predictInterval(1,2,v);}public void predictAttackContinuous(){if(supports(PlayerInventoryAction.ATTACK_CONTINUOUS)&&hasData(1))predictMode(1,PlayerInventoryBehaviorV1.MODE_CONTINUOUS);}public void predictAttackOnce(){if(supports(PlayerInventoryAction.ATTACK_ONCE)&&hasData(1))predictMode(1,PlayerInventoryBehaviorV1.MODE_NONE);}public void predictStopAttack(){if(supports(PlayerInventoryAction.STOP_ATTACK)&&hasData(1))predictMode(1,PlayerInventoryBehaviorV1.MODE_NONE);}public void predictUseInterval(int v){if(supports(PlayerInventoryAction.USE_INTERVAL)&&hasData(4))predictInterval(3,4,v);}public void predictUseContinuous(){if(supports(PlayerInventoryAction.USE_CONTINUOUS)&&hasData(3))predictMode(3,PlayerInventoryBehaviorV1.MODE_CONTINUOUS);}public void predictUseOnce(){if(supports(PlayerInventoryAction.USE_ONCE)&&hasData(3))predictMode(3,PlayerInventoryBehaviorV1.MODE_NONE);}public void predictStopUse(){if(supports(PlayerInventoryAction.STOP_USE)&&hasData(3))predictMode(3,PlayerInventoryBehaviorV1.MODE_NONE);}public void predictStopAll(){if(supports(PlayerInventoryAction.STOP_ALL)&&isActionInventory()){if(hasData(1))state.set(1,PlayerInventoryBehaviorV1.MODE_NONE);if(hasData(3))state.set(3,PlayerInventoryBehaviorV1.MODE_NONE);}}public void predictSelectedHotbar(int slot){if(supports(PlayerInventoryAction.SELECT_HOTBAR)&&isActionInventory()&&hasData(0)&&slot>=0&&slot<=8)state.set(0,slot);}public void predictSneaking(boolean value){if(supports(PlayerInventoryAction.TOGGLE_SNEAK)&&kind()==PlayerInventoryKind.ENDER_CHEST&&hasData(0))state.set(0,value?1:0);}
    private void predictInterval(int mode,int value,int v){state.set(mode,PlayerInventoryBehaviorV1.MODE_INTERVAL);state.set(value,PlayerInventoryBehaviorV1.clampInterval(v));}private void predictMode(int index,int value){state.set(index,value);}
    private static int[] createMapping(int start,int size){int[] r=new int[size];for(int i=0;i<size;i++)r[i]=start+i;return r;}

    private static final class MappedContainer implements Container{private final SimpleContainer delegate;private final int[] mapping;MappedContainer(SimpleContainer delegate,int[] mapping){this.delegate=delegate;this.mapping=mapping;}@Override public int getContainerSize(){return mapping.length;}@Override public boolean isEmpty(){for(int i:mapping)if(!delegate.getItem(i).isEmpty())return false;return true;}@Override public ItemStack getItem(int slot){return delegate.getItem(mapping[slot]);}@Override public ItemStack removeItem(int slot,int amount){return delegate.removeItem(mapping[slot],amount);}@Override public ItemStack removeItemNoUpdate(int slot){return delegate.removeItemNoUpdate(mapping[slot]);}@Override public void setItem(int slot,ItemStack stack){delegate.setItem(mapping[slot],stack);}@Override public int getMaxStackSize(){return delegate.getMaxStackSize();}
        //#if MC >= 12006
        @Override public int getMaxStackSize(ItemStack stack){return delegate.getMaxStackSize(stack);}
        //#endif
        @Override public void setChanged(){delegate.setChanged();}@Override public boolean stillValid(Player player){return true;}@Override public boolean canPlaceItem(int slot,ItemStack stack){return delegate.canPlaceItem(mapping[slot],stack);}@Override public void clearContent(){for(int i:mapping)delegate.setItem(i,ItemStack.EMPTY);delegate.setChanged();}}
    private static final class EquipmentClientSlot extends Slot{
        private final boolean limitOne;
        //#if MC >= 11500
        private final Identifier emptyIcon;
        EquipmentClientSlot(Container c,int index,int x,int y,boolean limitOne,Identifier emptyIcon){super(c,index,x,y);this.limitOne=limitOne;this.emptyIcon=emptyIcon;}
        //#else
        //$$ private final String emptyIcon;
        //$$ EquipmentClientSlot(Container c,int index,int x,int y,boolean limitOne,String emptyIcon){super(c,index,x,y);this.limitOne=limitOne;this.emptyIcon=emptyIcon;}
        //#endif
        @Override public int getMaxStackSize(){return limitOne?1:super.getMaxStackSize();}
        //#if MC >= 12104
        @Override public Identifier getNoItemIcon(){return emptyIcon;}
        //#elseif MC >= 11500
        //$$ @Override public Pair<net.minecraft.resources.ResourceLocation,net.minecraft.resources.ResourceLocation> getNoItemIcon(){return Pair.of(InventoryMenu.BLOCK_ATLAS,emptyIcon);}
        //#else
        //$$ @Override public String getNoItemIcon(){return emptyIcon;}
        //#endif
    }
}

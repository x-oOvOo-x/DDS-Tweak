package ddstweak.integration.provider;

import ddstweak.config.Configs;
import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.dds.playerinventory.screen.*;
import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import ddstweak.playerinventory.PlayerInventoryOpenContext;
import ddstweak.integration.gca.playerinventory.*;
import ddstweak.integration.org.playerinventory.*;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Performs an in-place client screen upgrade after an authoritative provider marker becomes available. */
public final class ProviderPlayerInventoryScreenCoordinator{
    private static final boolean TRACE=Boolean.getBoolean("dds.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger TRACE_LOG=
            org.apache.logging.log4j.LogManager.getLogger("dds.playerinventory.dt");
    @FunctionalInterface
    public interface AuthoritativeContextResolver{PlayerInventoryContext resolve(MenuType<?> type,int containerId);}
    @FunctionalInterface
    public interface StructuralReopenResolver{boolean tryReopen(AbstractContainerMenu menu,PlayerInventoryContext context);}
    @FunctionalInterface
    public interface LegacyOwnerEnderDeferResolver{boolean shouldDefer(PlayerInventoryContext context);}
    private static AuthoritativeContextResolver authoritativeContextResolver;
    private static StructuralReopenResolver structuralReopenResolver;
    private static LegacyOwnerEnderDeferResolver legacyOwnerEnderDeferResolver;
    private ProviderPlayerInventoryScreenCoordinator(){}

    public static synchronized void registerAuthoritativeContextResolver(AuthoritativeContextResolver resolver){
        if(resolver==null)return;
        if(authoritativeContextResolver!=null&&authoritativeContextResolver!=resolver)
            throw new IllegalStateException("authoritative player-inventory context resolver already registered");
        authoritativeContextResolver=resolver;
    }
    private static PlayerInventoryContext authoritativeContext(MenuType<?> type,int containerId){
        AuthoritativeContextResolver resolver=authoritativeContextResolver;
        return resolver==null||type==null?null:resolver.resolve(type,containerId);
    }
    public static synchronized void registerStructuralReopenResolver(StructuralReopenResolver resolver){
        if(resolver==null)return;
        if(structuralReopenResolver!=null&&structuralReopenResolver!=resolver)
            throw new IllegalStateException("player-inventory structural reopen resolver already registered");
        structuralReopenResolver=resolver;
    }
    private static boolean tryStructuralReopen(AbstractContainerMenu menu,PlayerInventoryContext context){
        StructuralReopenResolver resolver=structuralReopenResolver;
        return resolver!=null&&resolver.tryReopen(menu,context);
    }

    public static synchronized void registerLegacyOwnerEnderDeferResolver(LegacyOwnerEnderDeferResolver resolver){
        if(resolver==null)return;
        if(legacyOwnerEnderDeferResolver!=null&&legacyOwnerEnderDeferResolver!=resolver)
            throw new IllegalStateException("player-inventory owner-Ender defer resolver already registered");
        legacyOwnerEnderDeferResolver=resolver;
    }
    private static boolean shouldDeferLegacyOwnerEnder(PlayerInventoryContext context){
        LegacyOwnerEnderDeferResolver resolver=legacyOwnerEnderDeferResolver;
        return resolver!=null&&resolver.shouldDefer(context);
    }

    public static boolean tryUpgradeCurrent(){
        Minecraft minecraft=Minecraft.getInstance();
        if(!Configs.Tweaks.ENHANCED_INVENTORY_GUI.getBooleanValue()||minecraft.player==null)return false;
        Object current=MinecraftCompat.screen(minecraft);
        if(!(current instanceof AbstractContainerScreen<?>))return false;
        AbstractContainerScreen<?> source=(AbstractContainerScreen<?>)current;
        // A canonical MirrorMenu may use the ordinary EnhancedStorageScreen for REAL/OFFLINE.
        // Reclassifying it by the Provider's legacy title would downgrade its 54-slot layout.
        if(source instanceof UnifiedPlayerInventoryScreen
                ||source.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu
                ||source.getMenu().containerId!=minecraft.player.containerMenu.containerId)return false;
        return tryUpgrade(minecraft,source);
    }

    private static boolean tryUpgrade(Minecraft minecraft,AbstractContainerScreen<?> source){
        AbstractContainerMenu sourceMenu=source.getMenu();
        int containerId=sourceMenu.containerId;
        Component title=source.getTitle();

        MenuType<?> type=menuType(sourceMenu);
        if(OrgPlayerInventoryOpenIntent.isOfflineMenu(type,containerId,title))return false;
        PlayerInventoryContext context=authoritativeContext(type,containerId);
        if(context==null)context=type==null?null:PlayerInventoryOpenContext.consumeEnder(type,containerId,title);
        if(context==null)context=OrgPlayerInventoryAdapter.tryContext(sourceMenu,title);
        if(context==null&&type!=null){
            context=GcaPlayerInventoryAdapter.tryContext(type,containerId,title);
            if(context==null)context=PlayerInventoryOpenContext.consumeInventory(type,containerId,title);
        }
        if(context!=null&&"org".equals(context.source())
                &&context.targetState()==PlayerInventoryTargetState.OFFLINE)return false;
        if(context==null)return source instanceof EnhancedStorageScreen?false:tryUpgradeStorage(minecraft,source,sourceMenu,title);
        if(TRACE&&OrgPlayerInventoryAdapter.SOURCE.equals(context.source()))
            TRACE_LOG.info("org_layout_upgrade id="+containerId+" layout="+context.layoutId()
                    +" source_menu="+sourceMenu.getClass().getSimpleName()
                    +" source_slots="+sourceMenu.slots.size()
                    +" context_schema="+context.contextSchema());

        if(tryStructuralReopen(sourceMenu,context))return true;
        if(context.inventoryKind()==PlayerInventoryKind.ENDER_CHEST
                &&(PlayerInventoryLayouts.isGcaEnder(context)||PlayerInventoryLayouts.isOrgEnder(context))
                &&!shouldDeferLegacyOwnerEnder(context)
                &&DdsQcaClientNetwork.sendOpenOwnerEnder(sourceMenu,context)){
            PlayerInventoryOpenContext.expectEnder(context);
        }

        PlayerInventoryActionBinding binding=binding(context);
        if(binding==null)return false;
        DdsQcaClientNetwork.rememberProviderContext(context);
        UnifiedPlayerInventoryMirrorMenu mirror=UnifiedPlayerInventoryMirrorMenu.client(
                containerId,MinecraftCompat.playerInventory(minecraft),context,binding);
        copyMenuState(sourceMenu,mirror,minecraft.player);
        minecraft.player.containerMenu=mirror;
        showEnhancedScreen(minecraft,mirror,title);
        return true;
    }

    public static boolean restoreProviderEnder(AbstractContainerMenu source,PlayerInventoryContext context,Component title){
        Minecraft minecraft=Minecraft.getInstance();
        if(source==null||context==null||context.inventoryKind()!=PlayerInventoryKind.ENDER_CHEST
                ||context.targetState()==PlayerInventoryTargetState.OFFLINE||minecraft.player==null)return false;
        PlayerInventoryActionBinding binding=binding(context);if(binding==null)return false;
        UnifiedPlayerInventoryMirrorMenu mirror=UnifiedPlayerInventoryMirrorMenu.client(source.containerId,MinecraftCompat.playerInventory(minecraft),context,binding);
        copyMenuState(source,mirror,minecraft.player);
        minecraft.player.containerMenu=mirror;
        showEnhancedScreen(minecraft,mirror,title);
        return true;
    }

    /** Provider inventories share rendering/interaction. Real and offline Ender Chests use storage UI. */
    public static void showEnhancedScreen(Minecraft minecraft,UnifiedPlayerInventoryMirrorMenu menu,Component title){
        if(minecraft==null||minecraft.player==null||menu==null)return;
        if(menu.kind()==PlayerInventoryKind.ENDER_CHEST){
            if(menu.targetType()!=PlayerInventoryTargetType.FAKE){
                MinecraftCompat.setScreen(minecraft,new EnhancedStorageScreen(
                        menu,MinecraftCompat.playerInventory(minecraft),nonFakeEnderTitle(menu,title),menu.visualRows()));
            }else{
                MinecraftCompat.setScreen(minecraft,new UnifiedPlayerEnderChestScreen(
                        menu,MinecraftCompat.playerInventory(minecraft),title));
            }
            return;
        }
        MinecraftCompat.setScreen(minecraft,menu.kind()==PlayerInventoryKind.CONTROLLER
                ?new UnifiedPlayerInventoryScreen(menu,MinecraftCompat.playerInventory(minecraft),title)
                :menu.targetType()==PlayerInventoryTargetType.FAKE
                ?new UnifiedPlayerInventoryMainScreen(menu,MinecraftCompat.playerInventory(minecraft),title)
                :new UnifiedPlayerInventoryRealScreen(menu,MinecraftCompat.playerInventory(minecraft),title));
    }

    private static Component nonFakeEnderTitle(UnifiedPlayerInventoryMirrorMenu menu,Component original){
        PlayerInventoryContext context=menu.context();
        String name=context.targetName();
        if(name==null||name.trim().isEmpty()||context.targetType()!=PlayerInventoryTargetType.REAL)return original;
        return MinecraftCompat.translatable(context.targetState()==PlayerInventoryTargetState.OFFLINE
                ?"dds-tweak.playerinventory.offline_ender_title":"dds-tweak.playerinventory.ender_title",name);
    }

    private static boolean tryUpgradeStorage(Minecraft minecraft,AbstractContainerScreen<?> source,AbstractContainerMenu menu,Component title){
        int rows=storageRows(menu);
        if(rows!=3&&rows!=6)return false;
        MinecraftCompat.setScreen(minecraft,new EnhancedStorageScreen(menu,MinecraftCompat.playerInventory(minecraft),title,rows));
        return true;
    }
    private static int storageRows(AbstractContainerMenu menu){
        MenuType<?> type=menuType(menu);
        if(type==MenuType.GENERIC_9x6)return 6;
        if(type==MenuType.GENERIC_9x3||type==MenuType.SHULKER_BOX)return 3;
        if(menu instanceof net.minecraft.world.inventory.ShulkerBoxMenu)return 3;
        return 0;
    }

    public static PlayerInventoryActionBinding binding(PlayerInventoryContext context){
        if(context==null)return null;
        String source=context.source();
        PlayerInventoryActionBinding provider;
        if(GcaPlayerInventoryAdapter.SOURCE.equals(source)||"dds_qca".equals(source))provider=new GcaPlayerInventoryActionBinding();
        else if(OrgPlayerInventoryAdapter.SOURCE.equals(source))provider=new OrgPlayerInventoryActionBinding();
        else return null;
        return PlayerInventoryActionBindingRouter.compose(provider);
    }
    private static MenuType<?> menuType(AbstractContainerMenu menu){
        try{return menu.getType();}catch(UnsupportedOperationException ignored){return null;}
    }
    private static void copyMenuState(AbstractContainerMenu source,UnifiedPlayerInventoryMirrorMenu target,net.minecraft.world.entity.player.Player player){
        int count=Math.min(source.slots.size(),target.slots.size());
        for(int i=0;i<count;i++){
            ItemStack stack=source.getSlot(i).getItem();
            target.getSlot(i).set(stack.isEmpty()?ItemStack.EMPTY:stack.copy());
        }
        MinecraftCompat.copyCarried(source,target,player);
    }
}

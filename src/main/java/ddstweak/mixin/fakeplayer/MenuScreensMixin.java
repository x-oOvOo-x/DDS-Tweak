package ddstweak.mixin.fakeplayer;

import ddstweak.config.Configs;
import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.fakeplayer.DdsFakePlayerClientNetwork;
import ddstweak.integration.dds.fakeplayer.DdsFakePlayerExtensionContext;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.dds.playerinventory.screen.*;
import ddstweak.integration.gca.playerinventory.*;
import ddstweak.integration.org.playerinventory.OrgPlayerInventoryOpenIntent;
import ddstweak.integration.org.playerinventory.OrgPlayerInventoryAdapter;
import ddstweak.integration.provider.ProviderPlayerInventoryScreenCoordinator;
import ddstweak.integration.provider.EnhancedStorageScreen;
import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.playerinventory.PlayerInventoryOpenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tweak owns supported screens when enabled. GCA resolves immediately; Org upgrades from the Tweak storage shell after its command context binds. */
@Mixin(MenuScreens.class)
public abstract class MenuScreensMixin{
    private static final boolean TRACE = Boolean.getBoolean("dds.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger TRACE_LOG =
            org.apache.logging.log4j.LogManager.getLogger("dds.playerinventory.dt");
    @Inject(method="create",at=@At("HEAD"),cancellable=true)
    private static void ddsTweak$createUnifiedPlayerInventory(MenuType<?> type,Minecraft minecraft,int containerId,Component title,CallbackInfo ci){
        if(!Configs.Tweaks.ENHANCED_INVENTORY_GUI.getBooleanValue()||minecraft.player==null)return;

        DdsFakePlayerExtensionContext extension=DdsFakePlayerClientNetwork.peekExtension(containerId);
        PlayerInventoryContext context=DdsFakePlayerClientNetwork.consumeProviderContext(type,containerId);
        if(context==null)context=OrgPlayerInventoryAdapter.observeMenu(type,containerId,title);
        if(context==null){
            context=PlayerInventoryOpenContext.consumeEnder(type,containerId,title);
            if(context!=null&&extension!=null&&"dds_qca".equals(extension.provider()))
                DdsFakePlayerClientNetwork.discardExtension(containerId,extension);
        }
        if(context==null)context=GcaPlayerInventoryAdapter.tryContext(type,containerId,title);
        if(context==null)context=PlayerInventoryOpenContext.consumeInventory(type,containerId,title);

        if(OrgPlayerInventoryOpenIntent.isOfflineMenu(type,containerId,title)
                ||context!=null&&"org".equals(context.source())
                &&context.targetState()==PlayerInventoryTargetState.OFFLINE)return;

        boolean providerEnder=context!=null&&context.inventoryKind()==PlayerInventoryKind.ENDER_CHEST
                &&(PlayerInventoryLayouts.isGcaEnder(context)||PlayerInventoryLayouts.isOrgEnder(context));
        if(TRACE&&(providerEnder||"gca.player.ender_chest".equals(MinecraftCompat.translationKey(title))))
            TRACE_LOG.info("provider_screen_open id="+containerId
                    +" source="+(context==null?"unrecognized":context.source())
                    +" kind="+(context==null?"unrecognized":context.inventoryKind())
                    +" layout="+(context==null?"unrecognized":context.layoutId())
                    +" schema="+(context==null?0:context.contextSchema())
                    +" preopen_strict="+(extension!=null&&extension.strictSession())
                    +" preopen_size="+(extension==null?-1:extension.canonicalSize()));
        if(providerEnder){
            AbstractContainerMenu staging=type.create(containerId,MinecraftCompat.playerInventory(minecraft));
            if(staging!=null&&DdsFakePlayerClientNetwork.tryCanonicalEnder(staging,context)){
                DdsFakePlayerClientNetwork.stageCanonicalFallback(staging,context,title);
                minecraft.player.containerMenu=staging;
                MinecraftCompat.setScreen(minecraft,null);
                ci.cancel();
                return;
            }
            if(staging!=null&&DdsFakePlayerClientNetwork.stageProviderEnderAwaitingStrict(staging,context,title)){
                minecraft.player.containerMenu=staging;
                MinecraftCompat.setScreen(minecraft,null);
                ci.cancel();
                return;
            }
            // Strict Context may arrive on a later client packet. Keep the original
            // Provider menu visible rather than racing it with the 27-slot QCA fallback.
            boolean allowLegacyOwnerEnder=!DdsFakePlayerClientNetwork.shouldDeferLegacyOwnerEnder(context);
            DdsQcaClientNetwork.rememberProviderContext(context);
            if(allowLegacyOwnerEnder&&staging!=null&&DdsQcaClientNetwork.sendOpenOwnerEnder(staging,context)){
                PlayerInventoryOpenContext.expectEnder(context);
                minecraft.player.containerMenu=staging;
                DdsQcaClientNetwork.stageOwnerEnderFallback(staging,context,title);
                MinecraftCompat.setScreen(minecraft,null);
                ci.cancel();
                return;
            }
            if(allowLegacyOwnerEnder&&staging==null&&DdsQcaClientNetwork.sendOpenOwnerEnder(containerId,context))
                PlayerInventoryOpenContext.expectEnder(context);
        }
        if(context==null){
            int rows=type==MenuType.GENERIC_9x6?6:type==MenuType.GENERIC_9x3||type==MenuType.SHULKER_BOX?3:0;
            if(rows==0)return;
            AbstractContainerMenu menu=type.create(containerId,MinecraftCompat.playerInventory(minecraft));
            if(menu==null)return;
            minecraft.player.containerMenu=menu;
            MinecraftCompat.setScreen(minecraft,new EnhancedStorageScreen(menu,MinecraftCompat.playerInventory(minecraft),title,rows));
            // An explicitly matched Org command already has a verified connection/PlayerInfo.
            // Resolve its actual source-menu layout now rather than rendering a generic chest
            // until the next client tick (which can expose a mixed/stale first frame).
            OrgPlayerInventoryOpenIntent.Session org=OrgPlayerInventoryOpenIntent.session(containerId,type,title);
            if(org!=null&&org.kind()==OrgPlayerInventoryOpenIntent.Kind.INVENTORY){
                boolean upgraded=ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent();
                if(TRACE)TRACE_LOG.info("org_first_open id="+containerId+" enhanced="+upgraded
                        +" source_menu="+menu.getClass().getSimpleName());
            }
            ci.cancel();
            return;
        }

        PlayerInventoryActionBinding binding=ProviderPlayerInventoryScreenCoordinator.binding(context);
        if(binding==null)return;
        DdsQcaClientNetwork.rememberProviderContext(context);
        UnifiedPlayerInventoryMirrorMenu menu=UnifiedPlayerInventoryMirrorMenu.client(containerId,MinecraftCompat.playerInventory(minecraft),context,binding);
        minecraft.player.containerMenu=menu;
        ProviderPlayerInventoryScreenCoordinator.showEnhancedScreen(minecraft,menu,title);
        ci.cancel();
    }
}

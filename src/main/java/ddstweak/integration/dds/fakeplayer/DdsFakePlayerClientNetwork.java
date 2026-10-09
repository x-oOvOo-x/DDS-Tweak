package ddstweak.integration.dds.fakeplayer;

import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.dds.playerinventory.legacy.DdsFakePlayerLegacyContextAdapter;
import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryScreen;
import ddstweak.integration.provider.ProviderPlayerInventoryScreenCoordinator;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.integration.gca.playerinventory.GcaPlayerInventoryAdapter;
import ddstweak.integration.org.playerinventory.OrgPlayerInventoryAdapter;
import ddstweak.network.DdsClientNetwork;
import ddstweak.network.DdsClientSessionHooks;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.playerinventory.PlayerInventoryClientActionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import java.util.*;

/** DDS fake-player client network: legacy DDS menu compatibility plus optional GCA extension metadata/RPC. */
public final class DdsFakePlayerClientNetwork{
    private static final boolean TRACE = Boolean.getBoolean("dds.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger TRACE_LOG =
            org.apache.logging.log4j.LogManager.getLogger("dds.playerinventory.dt");
    private static final long CONTEXT_MAX_AGE_NANOS=5_000_000_000L,STRUCTURE_TIMEOUT_NANOS=3_000_000_000L,PROVIDER_CONTEXT_WAIT_NANOS=750_000_000L;
    private static final DdsFakePlayerMenuOpenState<ClientPacketListener> OPEN_STATE=new DdsFakePlayerMenuOpenState<>(CONTEXT_MAX_AGE_NANOS);
    private static final DdsFakePlayerExtensionOpenState<ClientPacketListener> EXTENSION_OPEN_STATE=new DdsFakePlayerExtensionOpenState<>(CONTEXT_MAX_AGE_NANOS);
    private static final DdsPlayerInventoryStructureState<ClientPacketListener,AbstractContainerMenu,UUID> STRUCTURE_STATE=
            new DdsPlayerInventoryStructureState<>(STRUCTURE_TIMEOUT_NANOS);
    private static final Map<Integer,DdsFakePlayerExtensionContext> ACTIVE_EXTENSIONS=new HashMap<>();
    private static ClientPacketListener structureRequestConnection;
    private static long nextStructureRequestId=1L;
    private static StructureFallback structureFallback;
    private static AwaitingProviderEnder awaitingProviderEnder;
    private static AbstractContainerMenu returnCloseBypass;
    private static boolean extensionInitialized;
    private DdsFakePlayerClientNetwork(){}

    /** Optional DDS capability slice for provider menus. It never registers or owns the legacy open-menu packet. */
    public static synchronized void initializeExtension(){
        if(extensionInitialized)return;
        DdsClientNetwork.registerC2S(DdsFakePlayerPackets.ACTION_C2S);
        DdsClientNetwork.registerC2S(DdsFakePlayerPackets.PROVIDER_ACTION_C2S);
        //#if MC >= 12111
        DdsClientNetwork.registerC2S(DdsFakePlayerPackets.CANONICAL_ENDER_C2S);
        DdsClientNetwork.registerC2S(DdsFakePlayerPackets.RETURN_PARENT_C2S);
        DdsClientNetwork.registerOrderedS2CHandler(DdsFakePlayerPackets.STRUCTURE_RESULT_S2C,DdsFakePlayerClientNetwork::handleStructureResult);
        //#endif
        PlayerInventoryActionBindingRouter.registerPreferred(DdsFakePlayerExtensionActionBinding::new);
        ProviderPlayerInventoryScreenCoordinator.registerAuthoritativeContextResolver(DdsFakePlayerClientNetwork::consumeProviderContext);
        ProviderPlayerInventoryScreenCoordinator.registerStructuralReopenResolver(DdsFakePlayerClientNetwork::tryCanonicalEnder);
        ProviderPlayerInventoryScreenCoordinator.registerLegacyOwnerEnderDeferResolver(DdsFakePlayerClientNetwork::shouldDeferLegacyOwnerEnder);
        DdsClientNetwork.registerOrderedS2CHandler(DdsFakePlayerPackets.CONTEXT_S2C,DdsFakePlayerClientNetwork::handleExtensionContext);
        DdsClientNetwork.registerS2CHandler(DdsFakePlayerPackets.STATE_S2C,DdsFakePlayerClientNetwork::handleExtensionState);
        DdsClientNetwork.registerS2CHandler(DdsFakePlayerPackets.SESSION_V3_CAPABILITY_S2C,data->{});
        DdsClientSessionHooks.registerResetHook(DdsFakePlayerClientNetwork::clearSession);
        extensionInitialized=true;
    }

    private static void handleOpenMenu(ClientPacketListener connection,CompoundTag data){
        DdsFakePlayerOpenContext context=DdsFakePlayerOpenContext.parse(data);
        if(context!=null)OPEN_STATE.expect(connection,context,System.nanoTime());
    }
    private static void handleExtensionContext(ClientPacketListener connection,CompoundTag data){
        if(DdsFakePlayerExtensionContext.isClear(data)){handleExtensionClear(connection,data);return;}
        DdsFakePlayerExtensionContext context=DdsFakePlayerExtensionContext.parseContext(data);
        if(context==null){
            if(TRACE&&data!=null&&"gca".equals(MinecraftCompat.stringOr(data,"provider","")))
                TRACE_LOG.info("context_parse_failed id="+MinecraftCompat.intOr(data,"container_id",-1)
                        +" schema="+MinecraftCompat.intOr(data,"context_schema",0)
                        +" kind="+MinecraftCompat.stringOr(data,"inventory_kind","")
                        +" layout="+MinecraftCompat.stringOr(data,"layout_id",""));
            return;
        }
        if (TRACE && context.inventoryKind()==PlayerInventoryKind.ENDER_CHEST)
            TRACE_LOG.info("context_rx id="+context.containerId()+" provider="+context.provider()
                    +" layout="+context.layoutId()+" canonical_size="+context.canonicalSize()
                    +" generation="+context.sessionGeneration()+" strict="+context.strictSession()
                    +" current_id="+(Minecraft.getInstance().player==null?-1:Minecraft.getInstance().player.containerMenu.containerId)
                    +" screen="+(MinecraftCompat.screen(Minecraft.getInstance())==null?"none"
                            :MinecraftCompat.screen(Minecraft.getInstance()).getClass().getSimpleName()));
        long now=System.nanoTime();
        DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> structural=
                STRUCTURE_STATE.current(connection,now);
        if(structural!=null&&matchesStructuralContext(structural,context))
            STRUCTURE_STATE.observeNextContext(connection,context.targetUuid(),context.containerId(),context.sessionGeneration(),now);
        EXTENSION_OPEN_STATE.expect(connection,context,now);
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.player.containerMenu.containerId!=context.containerId())return;
        AwaitingProviderEnder awaiting=awaitingProviderEnder;
        if(awaiting!=null&&awaiting.matches(context)&&mc.player.containerMenu==awaiting.menu){
            PlayerInventoryContext authoritative=consumeProviderContext(menuType(awaiting.menu),context.containerId());
            if(authoritative!=null&&tryCanonicalEnder(awaiting.menu,authoritative)){
                stageCanonicalFallback(awaiting.menu,authoritative,awaiting.title);
                awaitingProviderEnder=null;
                if(TRACE)TRACE_LOG.info("provider_ender_wait_complete id="+context.containerId()
                        +" result=canonical_requested");
                return;
            }
            awaitingProviderEnder=null;
            ProviderPlayerInventoryScreenCoordinator.restoreProviderEnder(awaiting.menu,awaiting.context,awaiting.title);
            if(TRACE)TRACE_LOG.info("provider_ender_wait_complete id="+context.containerId()
                    +" result=fallback_visible");
            return;
        }
        if(!(MinecraftCompat.screen(mc) instanceof UnifiedPlayerInventoryScreen)){
            ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent();
            return;
        }
        UnifiedPlayerInventoryScreen screen=(UnifiedPlayerInventoryScreen)MinecraftCompat.screen(mc);
        if(screen.getMenu().containerId!=context.containerId())return;
        if(!context.providerMatches(screen.getMenu().context())){discardExtension(context.containerId(),context);return;}
        if(!commitExtension(context.containerId(),context))return;
        PlayerInventoryContext merged=context.merge(screen.getMenu().context());
        screen.getMenu().updateContext(merged);
        // Context may arrive after GCA/Org has already opened its enhanced Provider screen.
        // Retry strict canonicalization instead of leaving that 3-row Provider layout permanently visible.
        if((PlayerInventoryLayouts.isGcaEnder(merged)||PlayerInventoryLayouts.isOrgEnder(merged))
                &&tryCanonicalEnder(screen.getMenu(),merged)){
            stageCanonicalFallback(screen.getMenu(),merged,screen.getTitle());
            MinecraftCompat.setScreen(mc,null);
        }
    }
    private static void handleExtensionClear(ClientPacketListener connection,CompoundTag data){
        int containerId=MinecraftCompat.intOr(data,"container_id",-1);
        long generation=MinecraftCompat.longOr(data,"session_generation",0L);
        if(containerId<0||generation<=0L)return;
        DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> structural=
                STRUCTURE_STATE.current(connection,System.nanoTime());
        if(structural!=null&&structural.sourceContainerId()==containerId&&structural.sourceGeneration()==generation){
            STRUCTURE_STATE.clear();
            handleStructureFailure(structural);
        }
        EXTENSION_OPEN_STATE.discard(connection,containerId,generation);
        DdsFakePlayerExtensionContext current=ACTIVE_EXTENSIONS.get(containerId);
        if(current!=null&&current.strictSession()&&current.sessionGeneration()==generation){
            ACTIVE_EXTENSIONS.remove(containerId);
            revokeVisibleExtensionAuthority(containerId,current);
            DdsQcaClientNetwork.revokeProviderExtensionAuthority(current.targetUuid());
        }
    }
    private static void revokeVisibleExtensionAuthority(int containerId,DdsFakePlayerExtensionContext extension){
        Minecraft mc=Minecraft.getInstance();
        if(!(MinecraftCompat.screen(mc) instanceof UnifiedPlayerInventoryScreen))return;
        UnifiedPlayerInventoryMirrorMenu menu=((UnifiedPlayerInventoryScreen)MinecraftCompat.screen(mc)).getMenu();
        PlayerInventoryContext base=menu.context();
        if(menu.containerId!=containerId||base.contextSchema()!=3||base.targetUuid()==null
                ||extension==null||!base.targetUuid().equals(extension.targetUuid())||!extension.providerMatches(base))return;
        menu.updateContext(PlayerIdentityResolver.withoutStrictSession(base));
    }
    private static void revokeVisibleExtensionAuthority(){
        Minecraft mc=Minecraft.getInstance();
        if(!(MinecraftCompat.screen(mc) instanceof UnifiedPlayerInventoryScreen))return;
        UnifiedPlayerInventoryMirrorMenu menu=((UnifiedPlayerInventoryScreen)MinecraftCompat.screen(mc)).getMenu();
        DdsFakePlayerExtensionContext extension=ACTIVE_EXTENSIONS.get(menu.containerId);
        if(extension!=null)revokeVisibleExtensionAuthority(menu.containerId,extension);
    }

    private static void handleExtensionState(CompoundTag data){
        int containerId=MinecraftCompat.intOr(data,"container_id",-1);
        DdsFakePlayerExtensionContext current=ACTIVE_EXTENSIONS.get(containerId);
        if(current==null)return;
        DdsFakePlayerExtensionContext updated=current.applyState(data);
        if(updated==current)return;
        ACTIVE_EXTENSIONS.put(containerId,updated);
        synchronizeVisibleActionState(updated);
    }
    private static void synchronizeVisibleActionState(DdsFakePlayerExtensionContext updated){
        Minecraft mc=Minecraft.getInstance();
        if(!(MinecraftCompat.screen(mc) instanceof UnifiedPlayerInventoryScreen))return;
        UnifiedPlayerInventoryScreen screen=(UnifiedPlayerInventoryScreen)MinecraftCompat.screen(mc);
        UnifiedPlayerInventoryMirrorMenu menu=screen.getMenu();
        if(menu.containerId!=updated.containerId()||menu.context().targetUuid()==null
                ||!menu.context().targetUuid().equals(updated.targetUuid())||!updated.providerMatches(menu.context()))return;
        PlayerInventoryClientActionState.forContext(menu.context()).observeAuthoritative(
                updated.selectedHotbar(),updated.attackMode(),updated.attackInterval(),
                updated.useMode(),updated.useInterval(),updated.sneaking());
    }

    public static DdsFakePlayerOpenContext peekOpenMenu(int containerId){return OPEN_STATE.peek(Minecraft.getInstance().getConnection(),containerId,System.nanoTime());}
    public static boolean commitOpenMenu(int containerId,DdsFakePlayerOpenContext context){return OPEN_STATE.commit(Minecraft.getInstance().getConnection(),containerId,context);}
    public static DdsFakePlayerExtensionContext peekExtension(int containerId){return EXTENSION_OPEN_STATE.peek(Minecraft.getInstance().getConnection(),containerId,System.nanoTime());}
    public static PlayerInventoryContext consumeProviderContext(MenuType<?> type,int containerId){
        DdsFakePlayerExtensionContext extension=peekExtension(containerId);
        if(extension==null)return null;
        PlayerInventoryContext wire=extension.standalone(type),context;
        if(wire==null)return null;
        boolean canonical=extension.strictSession()&&isStructuralExtension(extension)
                &&extension.inventoryKind()==PlayerInventoryKind.ENDER_CHEST
                &&(PlayerInventoryLayouts.ENDER_3_V1.equals(extension.layoutId())
                ||PlayerInventoryLayouts.ENDER_6_V1.equals(extension.layoutId()));
        if(canonical)context=wire;
        else if(GcaPlayerInventoryAdapter.SOURCE.equals(extension.provider()))
            context=GcaPlayerInventoryAdapter.authoritativeContext(type,wire);
        else if(OrgPlayerInventoryAdapter.SOURCE.equals(extension.provider()))
            context=OrgPlayerInventoryAdapter.authoritativeContext(type,wire);
        else return null;
        if(context==null||!commitExtension(containerId,extension))return null;
        completeStructuralOpen(Minecraft.getInstance().getConnection(),extension);
        return extension.merge(context);
    }

    private static void completeStructuralOpen(ClientPacketListener connection,DdsFakePlayerExtensionContext extension){
        if(connection==null||extension==null||!extension.strictSession()||extension.targetUuid()==null)return;
        DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> completed=
                STRUCTURE_STATE.consumeObservedOpen(connection,extension.containerId(),extension.targetUuid(),
                        extension.sessionGeneration(),System.nanoTime());
        if(completed==null)return;
        StructureFallback fallback=structureFallback;
        if(fallback!=null&&fallback.sourceMenu==completed.sourceMenu())structureFallback=null;
    }
    public static boolean commitExtension(int containerId,DdsFakePlayerExtensionContext context){
        if(!EXTENSION_OPEN_STATE.commit(Minecraft.getInstance().getConnection(),containerId,context))return false;
        DdsFakePlayerExtensionContext existing=ACTIVE_EXTENSIONS.get(containerId);
        if(existing!=null&&existing.strictSession()){
            if(!context.strictSession()||context.sessionGeneration()<existing.sessionGeneration())return false;
        }
        ACTIVE_EXTENSIONS.put(containerId,context);return true;
    }
    public static boolean discardExtension(int containerId,DdsFakePlayerExtensionContext context){
        if(!EXTENSION_OPEN_STATE.commit(Minecraft.getInstance().getConnection(),containerId,context))return false;
        ACTIVE_EXTENSIONS.remove(containerId);return true;
    }
    public static DdsFakePlayerExtensionContext activeExtension(int containerId,UUID targetUuid){
        DdsFakePlayerExtensionContext context=ACTIVE_EXTENSIONS.get(containerId);
        return context!=null&&targetUuid!=null&&targetUuid.equals(context.targetUuid())?context:null;
    }
    private static DdsFakePlayerExtensionContext activeStructuralExtension(int containerId,UUID targetUuid){
        DdsFakePlayerExtensionContext extension=activeExtension(containerId,targetUuid);
        return extension!=null&&extension.strictSession()&&isStructuralExtension(extension)?extension:null;
    }
    public static DdsFakePlayerExtensionContext activeExtension(UnifiedPlayerInventoryMirrorMenu menu){
        if(menu==null)return null;
        PlayerInventoryContext base=menu.context();
        DdsFakePlayerExtensionContext extension=activeExtension(menu.containerId,base.targetUuid());
        return extension!=null&&extension.inventoryKind()==menu.kind()&&extension.providerMatches(base)
                &&PlayerIdentityResolver.isAuthoritativeFake(base)?extension:null;
    }
    public static boolean canSendExtensionAction(UnifiedPlayerInventoryMirrorMenu menu,PlayerInventoryAction action){
        DdsFakePlayerExtensionContext extension=activeExtension(menu);
        return extension!=null&&action!=null&&action.allowedIn(menu.kind())&&extension.supports(action)&&canUseActions();
    }
    public static boolean sendExtensionAction(UnifiedPlayerInventoryMirrorMenu menu,PlayerInventoryAction action,int value){
        DdsFakePlayerExtensionContext extension=activeExtension(menu);
        return extension!=null&&canSendExtensionAction(menu,action)&&sendExtensionAction(extension,action,value);
    }

    /** A provider menu can precede its strict Context packet. Do not let the historical
     * owner-Ender request replace it while the server advertises strict canonical support. */
    public static boolean stageProviderEnderAwaitingStrict(AbstractContainerMenu menu,PlayerInventoryContext context,Component title){
        //#if MC >= 12111
        if(menu==null||context==null||context.inventoryKind()!=PlayerInventoryKind.ENDER_CHEST
                ||!isProvider(context)||!DdsClientNetwork.canSend(DdsFakePlayerPackets.CANONICAL_ENDER_C2S))return false;
        if(!isStructuralTarget(context))return false;
        awaitingProviderEnder=new AwaitingProviderEnder(menu,context,title,System.nanoTime());
        if(TRACE)TRACE_LOG.info("provider_ender_wait id="+menu.containerId+" provider="+context.source()
                +" state="+context.targetState()+" type="+context.targetType());
        return true;
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean shouldDeferLegacyOwnerEnder(PlayerInventoryContext context){
        //#if MC >= 12111
        boolean defer=context!=null&&context.inventoryKind()==PlayerInventoryKind.ENDER_CHEST
                &&isProvider(context)&&context.targetUuid()!=null
                &&isStructuralTarget(context)
                &&DdsClientNetwork.canSend(DdsFakePlayerPackets.CANONICAL_ENDER_C2S);
        if(TRACE&&defer)TRACE_LOG.info("legacy_owner_ender_deferred id="+context.containerId()
                +" provider="+context.source()+" layout="+context.layoutId());
        return defer;
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean tryCanonicalEnder(AbstractContainerMenu menu,PlayerInventoryContext context){
        //#if MC >= 12111
        boolean providerEnder=context!=null&&context.inventoryKind()==PlayerInventoryKind.ENDER_CHEST
                &&isProvider(context);
        if(menu==null||!providerEnder||!isStructuralTarget(context)){
            if(TRACE&&providerEnder)TRACE_LOG.info("canonical_gate id="+(menu==null?-1:menu.containerId)
                    +" result=untrusted_source schema="+context.contextSchema());
            return false;
        }
        if(!DdsClientNetwork.canSend(DdsFakePlayerPackets.CANONICAL_ENDER_C2S)){
            if(TRACE)TRACE_LOG.info("canonical_gate id="+menu.containerId+" result=no_structural_transport");
            return false;
        }
        DdsFakePlayerExtensionContext extension=activeStructuralExtension(menu.containerId,context.targetUuid());
        if(extension==null||!extension.providerMatches(context)
                ||extension.inventoryKind()!=PlayerInventoryKind.ENDER_CHEST
                ||extension.canonicalSize()!=27&&extension.canonicalSize()!=54){
            if(TRACE)TRACE_LOG.info("canonical_gate id="+menu.containerId+" result=no_matching_strict_ender"
                    +" extension_size="+(extension==null?-1:extension.canonicalSize())
                    +" extension_kind="+(extension==null?"missing":extension.inventoryKind())
                    +" schema="+context.contextSchema());
            return false;
        }
        boolean requested=beginStructure(menu,context,extension,DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,
                DdsFakePlayerPackets.CANONICAL_ENDER_C2S);
        if(TRACE)TRACE_LOG.info("canonical_gate id="+menu.containerId+" result="+(requested?"requested":"pending_or_send_failed")
                +" canonical_size="+extension.canonicalSize()+" generation="+extension.sessionGeneration());
        return requested;
        //#else
        //$$ return false;
        //#endif
    }

    public static void stageCanonicalFallback(AbstractContainerMenu menu,PlayerInventoryContext context,Component title){
        ClientPacketListener connection=Minecraft.getInstance().getConnection();
        DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> pending=
                STRUCTURE_STATE.current(connection,System.nanoTime());
        if(pending==null||pending.operation()!=DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER
                ||pending.sourceMenu()!=menu||context==null||!Objects.equals(pending.target(),context.targetUuid()))return;
        structureFallback=new StructureFallback(menu,context,title);
    }

    public static boolean canReturnParent(UnifiedPlayerInventoryMirrorMenu menu){
        //#if MC >= 12111
        if(menu==null||menu.kind()!=PlayerInventoryKind.ENDER_CHEST
                ||menu.context().targetUuid()==null
                ||!menu.context().allows(PlayerInventoryInteractionCapability.RETURN_PARENT)
                ||!DdsClientNetwork.canSend(DdsFakePlayerPackets.RETURN_PARENT_C2S))return false;
        DdsFakePlayerExtensionContext extension=activeStructuralExtension(menu.containerId,menu.context().targetUuid());
        return extension!=null&&extension.inventoryKind()==PlayerInventoryKind.ENDER_CHEST
                &&extension.providerMatches(menu.context())&&extension.returnParent();
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean sendReturnParent(UnifiedPlayerInventoryMirrorMenu menu){
        if(!canReturnParent(menu))return false;
        DdsFakePlayerExtensionContext extension=activeStructuralExtension(menu.containerId,menu.context().targetUuid());
        return extension!=null&&beginStructure(menu,menu.context(),extension,
                DdsPlayerInventoryStructureState.Operation.RETURN_PARENT,DdsFakePlayerPackets.RETURN_PARENT_C2S);
    }

    public static boolean consumeReturnCloseBypass(AbstractContainerMenu menu){
        if(menu==null||returnCloseBypass!=menu)return false;
        returnCloseBypass=null;
        return true;
    }

    public static void onClientTick(){
        ClientPacketListener connection=Minecraft.getInstance().getConnection();
        long now=System.nanoTime();
        DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> expired=
                STRUCTURE_STATE.expire(connection,now);
        if(expired!=null)handleStructureFailure(expired);
        AwaitingProviderEnder awaiting=awaitingProviderEnder;
        if(awaiting!=null&&now-awaiting.createdNanos>=PROVIDER_CONTEXT_WAIT_NANOS){
            awaitingProviderEnder=null;
            Minecraft mc=Minecraft.getInstance();
            if(mc.player!=null&&mc.player.containerMenu==awaiting.menu&&MinecraftCompat.screen(mc)==null){
                ProviderPlayerInventoryScreenCoordinator.restoreProviderEnder(awaiting.menu,awaiting.context,awaiting.title);
                if(TRACE)TRACE_LOG.info("provider_ender_wait_timeout id="+awaiting.menu.containerId);
            }
        }
    }

    private static boolean beginStructure(AbstractContainerMenu menu,PlayerInventoryContext context,
                                          DdsFakePlayerExtensionContext extension,
                                          DdsPlayerInventoryStructureState.Operation operation,String packet){
        Minecraft mc=Minecraft.getInstance();
        ClientPacketListener connection=mc.getConnection();
        if(connection==null||menu==null||context==null||extension==null)return false;
        long now=System.nanoTime();
        DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> existing=
                STRUCTURE_STATE.current(connection,now);
        if(existing!=null)return sameStructure(existing,menu,context.targetUuid(),extension.sessionGeneration(),operation);
        long requestId=nextStructureRequestId(connection);
        if(requestId<=0L)return false;
        DdsPlayerInventoryStructureState.Start start=STRUCTURE_STATE.begin(connection,menu,context.targetUuid(),menu.containerId,
                extension.sessionGeneration(),requestId,operation,now);
        if(start!=DdsPlayerInventoryStructureState.Start.STARTED)return start==DdsPlayerInventoryStructureState.Start.EXISTING;
        structureFallback=new StructureFallback(menu,context,currentTitle());
        CompoundTag data=new CompoundTag();
        data.putInt("container_id",menu.containerId);
        data.putString("target_uuid",context.targetUuid().toString());
        data.putLong("session_generation",extension.sessionGeneration());
        data.putLong("request_id",requestId);
        if(DdsClientNetwork.send(packet,data)){
            if(TRACE)TRACE_LOG.info("structure_sent operation="+operation+" source_id="+menu.containerId
                    +" generation="+extension.sessionGeneration()+" request_id="+requestId);
            return true;
        }
        if(TRACE)TRACE_LOG.info("structure_send_failed operation="+operation+" source_id="+menu.containerId);
        STRUCTURE_STATE.clear();
        structureFallback=null;
        return false;
    }

    private static boolean sameStructure(
            DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> pending,
            AbstractContainerMenu menu,UUID target,long generation,DdsPlayerInventoryStructureState.Operation operation){
        return pending!=null&&pending.sourceMenu()==menu&&Objects.equals(pending.target(),target)
                &&pending.sourceContainerId()==menu.containerId&&pending.sourceGeneration()==generation
                &&pending.operation()==operation;
    }

    private static long nextStructureRequestId(ClientPacketListener connection){
        if(connection==null)return -1L;
        if(structureRequestConnection!=connection){
            structureRequestConnection=connection;
            nextStructureRequestId=1L;
        }
        if(nextStructureRequestId<=0L)return -1L;
        long result=nextStructureRequestId;
        nextStructureRequestId=result==Long.MAX_VALUE?0L:result+1L;
        return result;
    }

    private static void handleStructureResult(ClientPacketListener connection,CompoundTag data){
        if(connection==null||data==null)return;
        int containerId=MinecraftCompat.intOr(data,"container_id",-1);
        long generation=MinecraftCompat.longOr(data,"session_generation",0L);
        long requestId=MinecraftCompat.longOr(data,"request_id",0L);
        String targetText=MinecraftCompat.stringOr(data,"target_uuid","");
        String status=MinecraftCompat.stringOr(data,"status","");
        if(TRACE)TRACE_LOG.info("structure_reply source_id="+containerId+" generation="+generation
                +" request_id="+requestId+" status="+status
                +" reason="+MinecraftCompat.stringOr(data,"reason",""));
        UUID target;
        try{target=UUID.fromString(targetText);}catch(IllegalArgumentException ignored){return;}
        long now=System.nanoTime();
        DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> before=
                STRUCTURE_STATE.current(connection,now);
        DdsPlayerInventoryStructureState.Result result=
                STRUCTURE_STATE.applyResult(connection,containerId,target,generation,requestId,status,now);
        if(result==DdsPlayerInventoryStructureState.Result.REJECTED&&before!=null){
            handleStructureFailure(before);
            return;
        }
        if(result==DdsPlayerInventoryStructureState.Result.ACCEPTED)return;
    }

    private static boolean matchesStructuralContext(
            DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> pending,
            DdsFakePlayerExtensionContext context){
        if(pending==null||context==null||!context.strictSession()||context.targetUuid()==null
                ||!Objects.equals(pending.target(),context.targetUuid())
                ||pending.sourceGeneration()==Long.MAX_VALUE
                ||context.sessionGeneration()!=pending.sourceGeneration()+1L)return false;
        StructureFallback fallback=structureFallback;
        if(fallback!=null&&fallback.sourceMenu==pending.sourceMenu()
                &&!fallback.context.source().equals(context.provider()))return false;
        if(pending.operation()==DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER)
            return context.inventoryKind()==PlayerInventoryKind.ENDER_CHEST
                    &&(context.canonicalSize()==27||context.canonicalSize()==54)
                    &&(PlayerInventoryLayouts.ENDER_3_V1.equals(context.layoutId())
                    ||PlayerInventoryLayouts.ENDER_6_V1.equals(context.layoutId()));
        return context.inventoryKind()==PlayerInventoryKind.INVENTORY;
    }

    private static void handleStructureFailure(
            DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> pending){
        if(pending==null)return;
        if(TRACE)TRACE_LOG.info("structure_fallback operation="+pending.operation()
                +" source_id="+pending.sourceContainerId()+" generation="+pending.sourceGeneration());
        StructureFallback fallback=structureFallback;
        if(fallback!=null&&fallback.sourceMenu==pending.sourceMenu())structureFallback=null;
        if(pending.operation()==DdsPlayerInventoryStructureState.Operation.RETURN_PARENT){
            closeAfterReturnFailure(pending);
            return;
        }
        restoreCanonicalFallback(pending,fallback);
    }

    private static void restoreCanonicalFallback(
            DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> pending,
            StructureFallback fallback){
        Minecraft mc=Minecraft.getInstance();
        if(mc.getConnection()!=pending.connection()||mc.player==null||mc.player.containerMenu!=pending.sourceMenu())return;
        if(MinecraftCompat.screen(mc)!=null)return;
        if(fallback!=null&&fallback.sourceMenu==pending.sourceMenu()&&fallback.title!=null
                &&ProviderPlayerInventoryScreenCoordinator.restoreProviderEnder(
                        fallback.sourceMenu,fallback.context,fallback.title))return;
        closeSourceMenu(pending.sourceMenu());
    }

    private static void closeAfterReturnFailure(
            DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> pending){
        Minecraft mc=Minecraft.getInstance();
        if(mc.getConnection()!=pending.connection()||mc.player==null||mc.player.containerMenu!=pending.sourceMenu())return;
        Object visible=MinecraftCompat.screen(mc);
        if(visible instanceof Screen){
            returnCloseBypass=pending.sourceMenu();
            ((Screen)visible).onClose();
            return;
        }
        closeSourceMenu(pending.sourceMenu());
    }

    private static void closeSourceMenu(AbstractContainerMenu source){
        Minecraft mc=Minecraft.getInstance();
        ClientPacketListener connection=mc.getConnection();
        if(source==null||mc.player==null||mc.player.containerMenu!=source)return;
        if(connection!=null)connection.send(new ServerboundContainerClosePacket(source.containerId));
        mc.player.containerMenu=mc.player.inventoryMenu;
        MinecraftCompat.setScreen(mc,null);
    }

    private static Component currentTitle(){
        Object visible=MinecraftCompat.screen(Minecraft.getInstance());
        return visible instanceof Screen?((Screen)visible).getTitle():null;
    }

    private static MenuType<?> menuType(AbstractContainerMenu menu){
        if(menu==null)return null;
        try{return menu.getType();}catch(UnsupportedOperationException ignored){return null;}
    }

    private static boolean isStructuralExtension(DdsFakePlayerExtensionContext extension){
        if(extension==null||extension.targetUuid()==null)return false;
        return extension.targetState()==PlayerInventoryTargetState.ONLINE
                &&(extension.targetType()==PlayerInventoryTargetType.FAKE
                    ||extension.targetType()==PlayerInventoryTargetType.REAL);
    }

    private static boolean isStructuralTarget(PlayerInventoryContext context){
        if(context==null||context.targetUuid()==null)return false;
        PlayerInventoryTargetType type=PlayerIdentityResolver.currentType(context);
        return context.targetState()==PlayerInventoryTargetState.ONLINE
                &&(type==PlayerInventoryTargetType.FAKE||type==PlayerInventoryTargetType.REAL);
    }

    private static boolean isProvider(PlayerInventoryContext context){
        return context!=null&&(GcaPlayerInventoryAdapter.SOURCE.equals(context.source())
                ||OrgPlayerInventoryAdapter.SOURCE.equals(context.source()));
    }

    public static void rejectOpenMenu(int containerId){
        Minecraft mc=Minecraft.getInstance();ClientPacketListener c=mc.getConnection();
        OPEN_STATE.clear();EXTENSION_OPEN_STATE.clear();ACTIVE_EXTENSIONS.remove(containerId);
        if(c!=null)c.send(new ServerboundContainerClosePacket(containerId));
        if(mc.player!=null)mc.player.containerMenu=mc.player.inventoryMenu;
        MinecraftCompat.setScreen(mc,null);
    }
    public static void clearSession(){
        DdsPlayerInventoryStructureState.Pending<ClientPacketListener,AbstractContainerMenu,UUID> pending=STRUCTURE_STATE.clear();
        if(pending!=null)handleStructureFailure(pending);
        revokeVisibleExtensionAuthority();
        for(DdsFakePlayerExtensionContext extension:new ArrayList<>(ACTIVE_EXTENSIONS.values()))
            DdsQcaClientNetwork.revokeProviderExtensionAuthority(extension.targetUuid());
        OPEN_STATE.clear();EXTENSION_OPEN_STATE.clear();ACTIVE_EXTENSIONS.clear();
        structureFallback=null;awaitingProviderEnder=null;returnCloseBypass=null;
    }
    public static boolean canUseActions(){return DdsClientNetwork.canSend(DdsFakePlayerPackets.ACTION_C2S);}
    public static boolean canUseProviderActions(){return DdsClientNetwork.canSend(DdsFakePlayerPackets.PROVIDER_ACTION_C2S);}
    public static boolean canSendProviderAction(UnifiedPlayerInventoryMirrorMenu menu,PlayerInventoryAction action){
        if(menu==null||action==null||menu.kind()==PlayerInventoryKind.ENDER_CHEST||!action.allowedIn(menu.kind())
                ||action!=PlayerInventoryAction.STOP_ATTACK&&action!=PlayerInventoryAction.STOP_USE)return false;
        PlayerInventoryContext context=menu.context();
        String source=context.source();
        return ("gca".equals(source)||"org".equals(source))
                &&context.targetUuid()!=null&&context.supports(action)
                &&PlayerIdentityResolver.isAuthoritativeFake(context)&&canUseProviderActions();
    }
    public static boolean sendProviderAction(UnifiedPlayerInventoryMirrorMenu menu,PlayerInventoryAction action,int value){
        if(!canSendProviderAction(menu,action))return false;
        DdsFakePlayerAction wire=DdsFakePlayerLegacyContextAdapter.legacyAction(action);
        if(wire==null)return false;
        CompoundTag data=new CompoundTag();
        data.putInt("container_id",menu.containerId);
        data.putString("target_uuid",menu.context().targetUuid().toString());
        data.putString("action",wire.wireName());
        data.putInt("value",value);
        return DdsClientNetwork.send(DdsFakePlayerPackets.PROVIDER_ACTION_C2S,data);
    }

    public static PlayerInventoryActionBinding actionBinding(PlayerInventoryContext context){
        return new PlayerInventoryActionBinding(){
            @Override public boolean canSend(){return canUseActions();}
            @Override public boolean send(PlayerInventoryAction action,int value){return sendLegacyAction(context,action,value);}
            @Override public int dataSlotCount(PlayerInventoryContext c){return c.inventoryKind()==PlayerInventoryKind.INVENTORY?5:1;}
        };
    }
    public static boolean sendLegacyAction(PlayerInventoryContext context,PlayerInventoryAction action,int value){
        DdsFakePlayerAction legacy=DdsFakePlayerLegacyContextAdapter.legacyAction(action);
        if(context==null||action==null||!action.allowedIn(context.inventoryKind())||!"dds".equals(context.source())||legacy==null
                ||!context.supports(action)||!PlayerIdentityResolver.isAuthoritativeFake(context)||!canUseActions())return false;
        CompoundTag data=new CompoundTag();data.putString("action",legacy.wireName());data.putInt("value",value);
        return DdsClientNetwork.send(DdsFakePlayerPackets.ACTION_C2S,data);
    }
    public static boolean sendExtensionAction(DdsFakePlayerExtensionContext context,PlayerInventoryAction action,int value){
        DdsFakePlayerAction legacy=DdsFakePlayerLegacyContextAdapter.legacyAction(action);
        if(context==null||action==null||!action.allowedIn(context.inventoryKind())||legacy==null||!context.supports(action)||!canUseActions())return false;
        CompoundTag data=new CompoundTag();
        data.putInt("container_id",context.containerId());
        data.putString("target_uuid",context.targetUuid().toString());
        if(context.strictSession())data.putLong("session_generation",context.sessionGeneration());
        data.putString("action",legacy.wireName());
        data.putInt("value",value);
        return DdsClientNetwork.send(DdsFakePlayerPackets.ACTION_C2S,data);
    }
    private static final class AwaitingProviderEnder{
        private final AbstractContainerMenu menu;
        private final PlayerInventoryContext context;
        private final Component title;
        private final long createdNanos;
        private AwaitingProviderEnder(AbstractContainerMenu menu,PlayerInventoryContext context,Component title,long createdNanos){
            this.menu=menu;this.context=context;this.title=title;this.createdNanos=createdNanos;
        }
        private boolean matches(DdsFakePlayerExtensionContext extension){
            if(extension==null||extension.containerId()!=menu.containerId
                    ||!extension.providerMatches(context)
                    ||extension.inventoryKind()!=PlayerInventoryKind.ENDER_CHEST)return false;
            if(context.targetUuid()!=null)return context.targetUuid().equals(extension.targetUuid());
            return context.targetName()!=null&&context.targetName().equals(extension.targetName());
        }
    }

    private static final class StructureFallback{
        private final AbstractContainerMenu sourceMenu;
        private final PlayerInventoryContext context;
        private final Component title;
        private StructureFallback(AbstractContainerMenu sourceMenu,PlayerInventoryContext context,Component title){
            this.sourceMenu=sourceMenu;this.context=context;this.title=title;
        }
    }

}

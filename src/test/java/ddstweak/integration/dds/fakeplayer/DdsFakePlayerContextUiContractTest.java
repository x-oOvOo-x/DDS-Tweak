package ddstweak.integration.dds.fakeplayer;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Regression guard for the retired DDS-as-player-inventory-provider architecture. */
final class DdsFakePlayerContextUiContractTest{
    private static final Path ROOT=Path.of("").toAbsolutePath(),JAVA=ROOT.resolve("src/main/java");

    @Test void unifiedDispatchUsesDdsOnlyAsOptionalAuthoritativePreOpenMetadata()throws IOException{
        String mixin=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        assertTrue(mixin.contains("DdsFakePlayerClientNetwork.consumeProviderContext(type,containerId)"));
        assertTrue(mixin.contains("OrgPlayerInventoryAdapter.observeMenu"));
        assertTrue(mixin.contains("GcaPlayerInventoryAdapter.tryContext"));
        assertTrue(mixin.contains("PlayerInventoryOpenContext.consumeEnder"));
        assertFalse(coordinator.contains("DdsFakePlayer"));
        assertTrue(coordinator.contains("OrgPlayerInventoryAdapter.tryContext"));
        assertTrue(coordinator.contains("GcaPlayerInventoryAdapter.tryContext"));
    }

    @Test void providerEnderAlwaysStagesBeforeCanonicalReopen()throws IOException{
        String mixin=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        int start=mixin.indexOf("boolean providerEnder="),end=mixin.indexOf("if(context==null){",start);
        assertTrue(start>=0&&end>start);
        String transition=mixin.substring(start,end);
        assertTrue(transition.contains("if(providerEnder){"));
        assertTrue(transition.contains("DdsQcaClientNetwork.sendOpenOwnerEnder(containerId,context)"));
        assertTrue(transition.contains("MinecraftCompat.setScreen(minecraft,null)"));
        assertFalse(transition.contains("canonicalSize()==54"));
    }

    @Test void ddsBootstrapRegistersOnlyProviderExtensionCapabilityNotLegacyMenuOwnership()throws IOException{
        String bootstrap=read("ddstweak/integration/dds/DdsIntegrationBootstrap.java");
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        assertTrue(bootstrap.contains("DdsFakePlayerClientNetwork.initializeExtension()"));
        int start=network.indexOf("initializeExtension()");
        int end=network.indexOf("private static void handleOpenMenu",start);
        assertTrue(start>=0&&end>start);
        String init=network.substring(start,end);
        assertTrue(init.contains("DdsFakePlayerPackets.ACTION_C2S"));
        assertTrue(init.contains("DdsFakePlayerPackets.PROVIDER_ACTION_C2S"));
        assertTrue(init.contains("DdsFakePlayerPackets.CONTEXT_S2C"));
        assertTrue(init.contains("DdsFakePlayerPackets.STATE_S2C"));
        assertTrue(init.contains("DdsFakePlayerPackets.SESSION_V3_CAPABILITY_S2C"));
        assertFalse(init.contains("DdsFakePlayerPackets.OPEN_MENU_S2C"));
        assertTrue(bootstrap.contains("DdsQcaClientNetwork.initialize()"));
        assertTrue(bootstrap.contains("DdsUndoRedoClientNetwork.initialize()"));
    }

    @Test void strictSessionSchemaUsesGenerationRevisionAndTargetedClear()throws IOException{
        String context=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionContext.java");
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String pending=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionOpenState.java");
        String packets=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerPackets.java");
        String compat=read("ddstweak/platform/minecraft/MinecraftCompat.java");
        assertTrue(context.contains("schema!=1&&schema!=2&&schema!=3"));
        assertTrue(context.contains("\"open\".equals(MinecraftCompat.stringOr(tag,\"event\",\"\"))"));
        assertTrue(context.contains("sessionGeneration"));
        assertTrue(context.contains("stateRevision"));
        assertTrue(context.contains("incoming<=stateRevision"));
        assertTrue(context.contains("session_generation"));
        assertTrue(context.contains("state_revision"));
        assertTrue(network.contains("DdsFakePlayerExtensionContext.isClear(data)"));
        assertTrue(network.contains("EXTENSION_OPEN_STATE.discard(connection,containerId,generation)"));
        assertTrue(network.contains("current.sessionGeneration()==generation"));
        assertTrue(network.contains("context.sessionGeneration()<existing.sessionGeneration()"));
        assertTrue(pending.contains("p.context.strictSession()&&p.context.sessionGeneration()==generation"));
        assertTrue(packets.contains("player_inventory_session_v3_s2c"));
        assertTrue(compat.contains("longOr(CompoundTag tag,String key,long fallback)"));
    }

    @Test void strictAuthorityIsRevokedFromTheVisibleMenuOnClearOrProtocolLoss()throws IOException{
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String resolver=read("ddstweak/integration/provider/PlayerIdentityResolver.java");
        String open=read("ddstweak/playerinventory/PlayerInventoryOpenContext.java");
        String qca=read("ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        assertTrue(network.contains("revokeVisibleExtensionAuthority(containerId,current)"));
        assertTrue(network.contains("PlayerIdentityResolver.withoutStrictSession(base)"));
        assertTrue(network.contains("DdsQcaClientNetwork.revokeProviderExtensionAuthority(current.targetUuid())"));
        int clearStart=network.indexOf("public static void clearSession(){");
        int clearEnd=network.indexOf("public static boolean canUseActions()",clearStart);
        assertTrue(clearStart>=0&&clearEnd>clearStart);
        String clearSession=network.substring(clearStart,clearEnd);
        assertTrue(clearSession.contains("STRUCTURE_STATE.clear()"));
        assertTrue(clearSession.contains("handleStructureFailure(pending)"));
        assertTrue(clearSession.contains("revokeVisibleExtensionAuthority()"));
        assertTrue(resolver.contains("withoutStrictSession(PlayerInventoryContext context)"));
        assertTrue(resolver.contains("capabilities.remove(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP)"));
        assertTrue(open.contains("revokeStrictAuthority(UUID targetUuid)"));
        assertTrue(qca.contains("nestedEnderParent=PlayerIdentityResolver.withoutStrictSession"));
        assertTrue(qca.contains("nestedInventoryParent=PlayerIdentityResolver.withoutStrictSession"));
    }

    @Test void strictProviderSessionCarriesCurrentFakeAuthority()throws IOException{
        String identity=read("ddstweak/integration/provider/PlayerIdentityResolver.java");
        assertTrue(identity.contains("context.contextSchema()==3"));
        assertTrue(identity.contains("return context.targetType()"));
        assertTrue(identity.contains("if(context.contextSchema()==3)return true"));
    }

    @Test void customTickUiKeepsTheClientsLastInputAcrossScreenReopenings()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        String state=read("ddstweak/playerinventory/PlayerInventoryClientActionState.java");
        String gca=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        String org=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java");
        assertTrue(screen.contains("PlayerInventoryClientActionState.forContext(menu.context())"));
        assertTrue(screen.contains("rememberInterval(attack,v)"));
        assertTrue(state.contains("rememberAttackInterval"));
        assertTrue(state.contains("rememberUseInterval"));
        assertTrue(gca.contains("PlayerInventoryClientActionState.forContext(menu.context())"));
        assertTrue(org.contains("PlayerInventoryClientActionState.forContext(menu.context())"));
        assertFalse(screen.contains("tooltip.unknown"));
    }

    @Test void pendingLocalActionStateMasksStaleExtensionSnapshots()throws IOException{
        String state=read("ddstweak/playerinventory/PlayerInventoryClientActionState.java");
        String binding=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionActionBinding.java");
        assertTrue(state.contains("PENDING_NANOS"));
        assertTrue(state.contains("preferLocalSelected"));
        assertTrue(state.contains("preferLocalAttack"));
        assertTrue(state.contains("preferLocalUse"));
        assertTrue(state.contains("preferLocalSneaking"));
        assertTrue(binding.contains("preferLocalSelected(e)?PlayerInventoryStateConfidence.LOCAL"));
        assertTrue(binding.contains("preferLocalAttack(e)?PlayerInventoryStateConfidence.LOCAL"));
        assertTrue(binding.contains("preferLocalUse(e)?PlayerInventoryStateConfidence.LOCAL"));
        assertTrue(binding.contains("preferLocalSneaking(e)?PlayerInventoryStateConfidence.LOCAL"));
    }

    @Test void intervalRangeMatchesCarpetPositiveIntegerContract()throws IOException{
        String behavior=read("ddstweak/integration/dds/playerinventory/PlayerInventoryBehaviorV1.java");
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        assertTrue(behavior.contains("MAX_INTERVAL=Integer.MAX_VALUE"));
        assertTrue(screen.contains("setMaxLength(10)"));
        assertTrue(screen.contains("text.length()>10"));
    }

    private static String read(String p)throws IOException{return Files.readString(JAVA.resolve(p));}
}

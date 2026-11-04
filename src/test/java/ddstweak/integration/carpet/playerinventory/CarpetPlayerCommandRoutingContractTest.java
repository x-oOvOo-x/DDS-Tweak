package ddstweak.integration.carpet.playerinventory;

import ddstweak.integration.dds.playerinventory.PlayerInventoryAction;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetPlayerCommandRoutingContractTest {
    private static final Path ROOT=findRoot();

    @Test void onceAndExactStopUseDistinctStandardCommands(){
        assertEquals("player bot attack once",CarpetPlayerCommandBridge.commandFor("bot",PlayerInventoryAction.ATTACK_ONCE,0));
        assertEquals("player bot use once",CarpetPlayerCommandBridge.commandFor("bot",PlayerInventoryAction.USE_ONCE,0));
        assertEquals("player bot attack stop",CarpetPlayerCommandBridge.commandFor("bot",PlayerInventoryAction.STOP_ATTACK,0));
        assertEquals("player bot use stop",CarpetPlayerCommandBridge.commandFor("bot",PlayerInventoryAction.STOP_USE,0));
    }

    @Test void enderSneakIsServerAuthorizedButEnderBatchNeedsItsOwnProtocol(){
        assertTrue(PlayerInventoryAction.TOGGLE_SNEAK.allowedWithoutFakeIdentity());
        assertTrue(PlayerInventoryAction.TOGGLE_SNEAK.allowedIn(ddstweak.integration.dds.playerinventory.PlayerInventoryKind.ENDER_CHEST));
        assertFalse(PlayerInventoryAction.TOGGLE_SNEAK.allowedIn(ddstweak.integration.dds.playerinventory.PlayerInventoryKind.INVENTORY));
        assertEquals("player bot sneak",CarpetPlayerCommandBridge.commandFor("bot",PlayerInventoryAction.TOGGLE_SNEAK,1));
        assertEquals("player bot unsneak",CarpetPlayerCommandBridge.commandFor("bot",PlayerInventoryAction.TOGGLE_SNEAK,0));
        assertFalse(PlayerInventoryAction.ENDER_DROP_ALL.allowedWithoutFakeIdentity());
    }

    @Test void commandRouteRegistersBeforeDdsAndProviderFallbacks()throws IOException{
        String bootstrap=Files.readString(ROOT.resolve("src/main/java/ddstweak/DDSTweak.java"));
        int command=bootstrap.indexOf("CarpetPlayerCommandActionBinding.initialize()");
        int dds=bootstrap.indexOf("DdsIntegrationBootstrap.initialize()");
        assertTrue(command>=0&&dds>command);

        String binding=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandActionBinding.java");
        String router=read("ddstweak/integration/dds/playerinventory/PlayerInventoryActionBindingRouter.java");
        assertTrue(binding.contains("registerPreferred(CarpetPlayerCommandActionBinding::new)"));
        assertTrue(binding.contains("CarpetPlayerCommandBridge.send"));
        assertTrue(router.contains("for(PlayerInventoryActionBinding route:routes)if(route.supports(action))return route"));
    }

    @Test void providerBindingsRemainFallbacksInsteadOfOwningStandardRoute()throws IOException{
        for(String path:new String[]{
                "ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java",
                "ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java"}){
            String source=read(path);
            assertTrue(source.contains("CarpetPlayerCommandBridge.send"),path);
            assertTrue(source.contains("sendNative(slot)"),path);
            assertTrue(source.contains("//#if MC >= 12111"),path);
            assertTrue(source.contains("return false;"),path);
        }
    }

    @Test void modernStandardControlsNeverRequireClientFakeClassification()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        int ordinary=menu.indexOf("// Ordinary Carpet controls are identical for REAL, FAKE and UNKNOWN.");
        int legacy=menu.indexOf("//#else",ordinary);
        assertTrue(ordinary>0&&legacy>ordinary);
        String rule=menu.substring(ordinary,legacy);
        assertTrue(rule.contains("action.allowedWithoutFakeIdentity()"));
        assertTrue(rule.contains("CarpetPlayerCommandBridge.validTargetName(context.targetName())"));
        assertTrue(rule.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertFalse(rule.contains("targetType()"));
        assertFalse(rule.contains("verifiedFake"));
        assertFalse(rule.contains("isAuthoritativeFake"));
        assertTrue(menu.contains("return canonical&&PlayerIdentityResolver.isAuthoritativeFake(context)"));

        for(String path:new String[]{
                "ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java",
                "ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java"}){
            String source=read(path);
            int method=source.indexOf("@Override public boolean supports(PlayerInventoryAction action)");
            int modern=source.indexOf("//#if MC >= 12111",method);
            int old=source.indexOf("//#else",modern);
            assertTrue(method>0&&modern>method&&old>modern,path);
            String policy=source.substring(modern,old);
            assertTrue(policy.contains("action.allowedWithoutFakeIdentity()"),path);
            assertTrue(policy.contains("CarpetPlayerCommandBridge.supports(action)"),path);
            assertFalse(policy.contains("targetType()"),path);
            assertFalse(policy.contains("PlayerInventoryTargetType"),path);
        }
    }

    @Test void standardCarpetButtonsAreNotBlockedByFilteredBrigadierPermissions()throws IOException{
        String bridge=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandBridge.java");
        int start=bridge.indexOf("public static boolean supports(PlayerInventoryAction action)");
        int end=bridge.indexOf("public static Set<PlayerInventoryAction> supportedActions()",start);
        assertTrue(start>=0&&end>start);
        String policy=bridge.substring(start,end);
        assertTrue(policy.contains("return true;"));
        assertTrue(policy.contains("STOP_ATTACK"));
        assertTrue(policy.contains("STOP_USE"));
        assertTrue(policy.contains("path(targetNode()"));
        assertFalse(policy.contains("if(target==null)return false"));
        assertTrue(bridge.contains("return mc!=null&&mc.getConnection()!=null"));
        assertTrue(bridge.contains("return child(drop,\"inventory\")!=null?\"inventory\":\"all\";"));

        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        assertTrue(screen.contains("attackInterval.active=editable;"));
        assertTrue(screen.contains("useInterval.active=editable;"));
        assertTrue(screen.contains("target_unresolved"));
        assertTrue(screen.contains("exact_stop_unavailable"));
        assertTrue(screen.contains("menu.context().targetState()!=PlayerInventoryTargetState.OFFLINE"));
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("CarpetPlayerCommandBridge.validTargetName(context.targetName())"));
        assertTrue(menu.contains("return canonical&&PlayerIdentityResolver.isAuthoritativeFake(context)"));
    }

    private static String read(String path)throws IOException{return Files.readString(ROOT.resolve("src/main/java").resolve(path));}
    private static Path findRoot(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.exists(p.resolve("settings.json"))&&Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root");}
}

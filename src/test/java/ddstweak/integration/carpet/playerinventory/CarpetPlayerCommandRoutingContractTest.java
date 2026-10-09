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

    private static String read(String path)throws IOException{return Files.readString(ROOT.resolve("src/main/java").resolve(path));}
    private static Path findRoot(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.exists(p.resolve("settings.json"))&&Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root");}
}

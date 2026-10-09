package ddstweak.integration.dds.fakeplayer;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class DdsFakePlayerStateSyncContractTest {
    private static final Path ROOT=findRoot();

    @Test void acceptedRevisionReconcilesVisibleCacheWithoutBypassingPending()throws IOException{
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String state=read("ddstweak/playerinventory/PlayerInventoryClientActionState.java");
        assertTrue(network.contains("if(updated==current)return"));
        assertTrue(network.contains("synchronizeVisibleActionState(updated)"));
        assertTrue(network.contains("observeAuthoritative("));
        assertTrue(state.contains("observeAuthoritative("));
        assertTrue(state.contains("preferLocalSelected(selectedHotbar)"));
        assertTrue(state.contains("preferLocalAttack(attackMode,attackInterval)"));
        assertTrue(state.contains("preferLocalUse(useMode,useInterval)"));
        assertTrue(state.contains("preferLocalSneaking(sneaking)"));
    }

    private static String read(String path)throws IOException{return Files.readString(ROOT.resolve("src/main/java").resolve(path));}
    private static Path findRoot(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.exists(p.resolve("settings.json"))&&Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root");}
}

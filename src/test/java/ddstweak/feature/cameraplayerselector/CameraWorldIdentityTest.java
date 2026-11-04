package ddstweak.feature.cameraplayerselector;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CameraWorldIdentityTest {
    @Test void trailingRootDotDoesNotMergeDifferentWorlds(){
        assertEquals("world-folder:World_A",CameraCommandProfileStore.worldKey(Path.of("saves","World_A",".")));
        assertEquals("world-folder:World_B",CameraCommandProfileStore.worldKey(Path.of("saves","World_B",".")));
        assertEquals(CameraCommandProfileStore.worldKey(Path.of("saves","World_A")),
                CameraCommandProfileStore.worldKey(Path.of("saves","World_A",".")));
    }

    @Test void missingOrRootOnlyPathHasNoLocalScope(){
        assertEquals("",CameraCommandProfileStore.worldKey(null));
        assertEquals("",CameraCommandProfileStore.worldKey(Path.of("/").toAbsolutePath().getRoot()));
    }
}

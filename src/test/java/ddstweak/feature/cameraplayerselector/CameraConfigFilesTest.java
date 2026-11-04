package ddstweak.feature.cameraplayerselector;

import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class CameraConfigFilesTest {
    @TempDir Path directory;

    @Test void existingBrokenJsonIsNeverOverwritten() throws Exception {
        Path path=directory.resolve("quick.json");
        Files.writeString(path,"{broken");
        assertThrows(IllegalStateException.class,()->CameraConfigFiles.read(path));
        assertThrows(IllegalStateException.class,()->CameraConfigFiles.write(path,new JsonObject()));
        assertEquals("{broken",Files.readString(path));
    }

    @Test void replacementKeepsPreviousGoodVersionAsBackup() {
        Path path=directory.resolve("quick.json");
        JsonObject first=new JsonObject();
        first.addProperty("value",1);
        CameraConfigFiles.write(path,first);
        JsonObject second=new JsonObject();
        second.addProperty("value",2);
        CameraConfigFiles.write(path,second);
        assertEquals(2,CameraConfigFiles.read(path).get("value").getAsInt());
        assertEquals(1,CameraConfigFiles.read(path.resolveSibling("quick.json.bak")).get("value").getAsInt());
    }

    @Test void rootTypeCannotBeReset() throws Exception {
        Path path=directory.resolve("quick.json");
        Files.writeString(path,"[]");
        assertThrows(IllegalStateException.class,()->CameraConfigFiles.write(path,new JsonObject()));
        assertEquals("[]",Files.readString(path));
    }
}

package ddstweak.build;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class PreprocessorMarkerGuardContractTest {
    @Test void buildVerifierRejectsSingleDollarMarker() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("stripped.startsWith('//$') && !stripped.startsWith('//$$')"));
        assertTrue(build.contains("Invalid single-dollar ReplayMod preprocess marker"));
    }
}

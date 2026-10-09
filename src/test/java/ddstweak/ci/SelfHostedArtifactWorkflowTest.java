package ddstweak.ci;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class SelfHostedArtifactWorkflowTest {
    private static final Path WORKFLOW=Path.of(".github/workflows/build.yml").toAbsolutePath();
    private static String workflow() throws Exception{return Files.readString(WORKFLOW).replace("\r\n","\n");}

    @Test void rollingPrereleasePublishesTheVerifiedBuildWithoutRebuilding() throws Exception {
        String y=workflow();
        assertTrue(y.contains("types: [opened, synchronize, reopened, closed]"));
        assertTrue(y.contains("contents: write"));
        assertTrue(y.contains("Publish rolling prerelease"));
        assertTrue(y.contains("format('ci-pr-{0}', github.event.pull_request.number) || 'ci-main'"));
        assertTrue(y.contains("prerelease = $true"));
        assertTrue(y.contains("releases/tags/$env:CI_TAG"));
        assertTrue(y.contains("releases/assets/$($asset.id)"));
        assertTrue(y.contains("build/releases/dds-tweak-v*.jar"));
        assertFalse(y.contains("  publish:\n    needs: build-self-hosted"));
        assertFalse(y.contains("Build test distribution"));
        assertEquals(1,count(y,"gradlew.bat buildAndGather --stacktrace"),"self-hosted CI must build the full distribution exactly once");
        assertTrue(y.contains("  cleanup-pr-release:"));
        assertTrue(y.contains("github.event.action == 'closed'"));
    }

    @Test void selfHostedCacheIsPersistentShortAndLocal() throws Exception {
        String y=workflow();
        assertTrue(y.contains("Configure persistent short Gradle home"));
        assertTrue(y.contains("'g\\dds-tweak'"));
        assertTrue(y.contains("cache-disabled: true"));
        assertTrue(y.contains("git clean -ffdx -e .gradle/"));
        assertFalse(y.contains("RUNNER_TEMP\\dds-tweak-gradle"));
    }

    @Test void concurrencySeparatesMainPushFromPrClose() throws Exception {
        String y=workflow();
        assertTrue(y.contains("group: build-${{ github.workflow }}-${{ github.event_name }}-${{ github.ref }}-${{ github.event.pull_request.number || 'main' }}"));
        assertTrue(y.contains("cancel-in-progress: true"));
        assertFalse(y.contains("group: build-${{ github.workflow }}-${{ github.ref }}\n"));
    }

    private static int count(String text,String token){int n=0,p=0;while((p=text.indexOf(token,p))>=0){n++;p+=token.length();}return n;}
}

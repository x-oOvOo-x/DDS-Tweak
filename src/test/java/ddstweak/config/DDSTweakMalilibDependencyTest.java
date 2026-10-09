package ddstweak.config;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class DDSTweakMalilibDependencyTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p)throws Exception{return Files.readString(ROOT.resolve(p));}

    @Test void malilibIsVersionOwnedAndRequired()throws Exception{
        String common=read("common.gradle"),v111=read("versions/1.21.11/gradle.properties"),v110=read("versions/1.21.10/gradle.properties"),mod=read("src/main/resources/fabric.mod.json");
        assertTrue(v111.contains("malilib_version=0.27.20"));
        assertTrue(v111.contains("malilib_coordinate=fi.dy.masa.malilib:malilib-fabric-1.21.11:0.27.20"));
        assertTrue(v110.contains("malilib_version=0.26.8"));
        assertTrue(v110.contains("malilib_coordinate=fi.dy.masa.malilib:malilib-fabric-1.21.10:0.26.8"));
        assertTrue(common.contains("project.malilib_coordinate"));
        assertTrue(common.contains("https://masa.dy.fi/maven/sakura-ryoko"));
        assertTrue(mod.contains("\"malilib\": \"${malilib_dependency}\""));
    }
}

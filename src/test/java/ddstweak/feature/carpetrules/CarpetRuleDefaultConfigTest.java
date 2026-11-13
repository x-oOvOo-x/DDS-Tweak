package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleDefaultConfigTest {
    private static CarpetRuleCatalog.RemoteRule rule(String manager,String name){
        return new CarpetRuleCatalog.RemoteRule(new CarpetRuleCatalog.RuleKey(manager,name),"false",true);
    }
    @Test void integratedWorldPathUsesMappedVersionSpecificApi() throws Exception {
        String source=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleDefaultConfig.java"));
        assertTrue(source.contains("Path world=null;"));
        assertTrue(source.contains("server.getWorldPath(LevelResource.ROOT)"));
        assertTrue(source.contains("world=legacyWorldDirectory()"));
        assertTrue(source.contains("carpet.getField(\"settingsManager\")"));
        assertTrue(source.contains("type.getDeclaredMethod(\"getFile\")"));
        assertFalse(source.contains("server.getLevelStorage()"));
        assertTrue(source.contains("if(world==null)return unknown();"));
        assertFalse(source.contains("getStorageSource()"));
        assertFalse(source.contains("getLevelIdName()"));
        assertFalse(source.contains("Class.forName(\"net.minecraft.world.level.storage.LevelResource\")"));
        assertFalse(source.contains("getMethod(\"getWorldPath\""));
    }

    private static final class LegacyManager {
        private final java.io.File config;

        LegacyManager(java.io.File config){this.config=config;}
        private java.io.File getFile(){return config;}
    }

    @Test void resolvesLegacyConfigDirectoryThroughPrivateCarpetManagerMethod() throws Exception {
        java.nio.file.Path world=java.nio.file.Files.createTempDirectory("dt-carpet-legacy");
        try{
            assertEquals(world,CarpetRuleDefaultConfig.managerConfigDirectory(
                    new LegacyManager(world.resolve("carpet.conf").toFile())));
            assertNull(CarpetRuleDefaultConfig.managerConfigDirectory(null));
            assertNull(CarpetRuleDefaultConfig.managerConfigDirectory(new Object()));
        }finally{
            java.nio.file.Files.delete(world);
        }
    }

    @Test void integratedDefaultConfigReadsAreThrottledAndInvalidatedOnSave() throws Exception {
        String integrated=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetIntegratedRules.java"));
        String network=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleClientNetwork.java"));
        assertTrue(integrated.contains("DEFAULT_CONFIG_REFRESH_NS=TimeUnit.SECONDS.toNanos(5)"));
        assertTrue(integrated.contains("readCachedDefaults(server,rules,revision)"));
        assertTrue(integrated.contains("static long defaultsRevision()"));
        assertTrue(integrated.contains("long revision=DEFAULTS_REVISION.get()"));
        assertTrue(network.contains("snapshot.defaultsRevision!=CarpetIntegratedRules.defaultsRevision()"));
        int reply=network.indexOf("OPERATIONS.acknowledge(id,returnCode,hasError,");
        int verified=network.indexOf("if(verifyPersistence){",reply);
        int invalidated=network.indexOf("CarpetIntegratedRules.invalidatePersistedDefaults();",verified);
        assertTrue(reply>=0&&verified>reply&&invalidated>verified,
                "Successful local reply must invalidate persisted-default cache");
        assertTrue(integrated.contains("cachedServer.get()!=server"));
        assertTrue(integrated.contains("cachedRevision!=revision"));
        assertTrue(integrated.contains("new WeakReference<>(server)"));
        assertTrue(network.contains("CarpetIntegratedRules.invalidatePersistedDefaults()"));
    }

    @Test void localDefaultIconsAndSavedFilterPreferKnownDiskSnapshot() throws Exception {
        String screen=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerScreen.java"));
        String network=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleClientNetwork.java"))
                .replace("\r\n","\n");
        String authority=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleAuthority.java"));

        int method=screen.indexOf("int savedDefaultState(");
        int end=screen.indexOf("void submitDefault(",method);
        assertTrue(method>=0&&end>method);
        String icon=screen.substring(method,end);
        int local=icon.indexOf("if(CarpetRuleClientNetwork.integratedDefaultKnown(key))");
        int remote=icon.indexOf("CarpetRuleClientNetwork.serverPersisted(key)");
        assertTrue(local>=0&&remote>local,
                "Known singleplayer disk state must take precedence over slow CDA metadata");
        assertTrue(icon.contains("integratedSavedDefaults().contains(key)?1:2"));
        assertTrue(icon.contains("state==1||state==2?state:-1"),
                "Remote worlds must still use verified server metadata");

        int mergeStart=network.indexOf("Set<CarpetRuleCatalog.RuleKey> savedDefaults()");
        int mergeEnd=network.indexOf("Set<CarpetRuleCatalog.RuleKey> integratedSavedDefaults()",mergeStart);
        assertTrue(mergeStart>=0&&mergeEnd>mergeStart);
        String merged=network.substring(mergeStart,mergeEnd);
        int remove=merged.indexOf("merged.removeIf(key->integratedKnownManagers.contains(key.manager()))");
        int add=merged.indexOf("merged.addAll(integratedDefaults)");
        assertTrue(remove>=0&&add>remove,
                "Local removal must also clear a stale CDA saved-default entry");
        assertTrue(merged.contains("CarpetRuleAuthority.saved()"),
                "Unknown and remote managers must retain server-reported state");

        assertTrue(network.contains("if(verifyPersistence){\n"
                +"                    CarpetIntegratedRules.invalidatePersistedDefaults();\n"
                +"                    // The Carpet command has replied: schedule a fresh local"));
        assertTrue(network.contains("integratedTicks=20;"));
        assertTrue(authority.contains("ticks=-560;"),
                "Do not increase the normal CDA metadata polling rate");
        assertTrue(authority.contains("MIN_REQUEST_GAP_NS"),
                "Post-write refreshes must respect the CDA metadata rate limit");
    }

    @Test void parsesOnlyKnownCarpetRuleLinesWithoutInventingAddonDefaults(){
        CarpetRuleCatalog.RemoteRule core=rule("carpet","foo");
        CarpetRuleCatalog.RemoteRule extension=rule("tis","bar");
        CarpetRuleDefaultConfig.ReadResult result=CarpetRuleDefaultConfig.parse(
                Arrays.asList("# note","locked","foo true","bar false","unknown 5",
                        "foo", "  ", "foo false"),
                Arrays.asList(core,extension));
        assertTrue(result.known());
        assertEquals(Collections.singleton(core.key()),result.rules());
        assertEquals("false",result.values().get(core.key()));
        assertFalse(result.values().containsKey(extension.key()));
        assertFalse(result.rules().contains(extension.key()));
    }

    @Test void everyRegisteredManagerUsesItsOwnConfigAndWorldOverridesFallback()
            throws Exception {
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory("dt-carpet-world");
        java.nio.file.Path fallback=java.nio.file.Files.createTempDirectory("dt-carpet-defaults");
        try{
            java.util.List<CarpetRuleCatalog.RemoteRule> registered=Arrays.asList(
                    rule("carpet","foo"),rule("tis","bar"),rule("other","baz"));
            java.nio.file.Files.writeString(root.resolve("carpet.conf"),"foo true\n");
            java.nio.file.Files.writeString(root.resolve("tis.conf"),"bar false\n");
            java.nio.file.Files.writeString(fallback.resolve("default_tis.conf"),"");
            java.nio.file.Files.writeString(fallback.resolve("default_other.conf"),"baz true\n");
            CarpetRuleDefaultConfig.ReadResult state=
                    CarpetRuleDefaultConfig.readManagers(root,fallback,registered);
            assertTrue(state.known());
            assertEquals(3,state.knownManagers().size());
            assertTrue(state.rules().contains(registered.get(0).key()));
            assertTrue(state.rules().contains(registered.get(1).key()));
            assertTrue(state.rules().contains(registered.get(2).key()));
            assertEquals("true",state.values().get(registered.get(0).key()));
            assertEquals("false",state.values().get(registered.get(1).key()));
            assertEquals("true",state.values().get(registered.get(2).key()));
            java.nio.file.Files.delete(root.resolve("tis.conf"));
            state=CarpetRuleDefaultConfig.readManagers(root,fallback,registered);
            assertTrue(state.known());
            assertFalse(state.rules().contains(registered.get(1).key()));
            assertFalse(state.values().containsKey(registered.get(1).key()));
            // A known missing config is a valid empty default, not a sync failure.
            java.nio.file.Files.delete(fallback.resolve("default_other.conf"));
            state=CarpetRuleDefaultConfig.readManagers(root,fallback,registered);
            assertTrue(state.known());
            assertFalse(state.rules().contains(registered.get(2).key()));
        }finally{
            try(java.util.stream.Stream<java.nio.file.Path> files=java.nio.file.Files.walk(root)){
                files.sorted(java.util.Comparator.reverseOrder()).forEach(x->{
                    try{java.nio.file.Files.deleteIfExists(x);}
                    catch(java.io.IOException ignored){}
                });
            }
            try(java.util.stream.Stream<java.nio.file.Path> files=java.nio.file.Files.walk(fallback)){
                files.sorted(java.util.Comparator.reverseOrder()).forEach(x->{
                    try{java.nio.file.Files.deleteIfExists(x);}
                    catch(java.io.IOException ignored){}
                });
            }
        }
    }

    @Test void unreadableManagerConfigDoesNotEraseOtherKnownStates() throws Exception {
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory("dt-carpet-read-test");
        java.nio.file.Path fallback=java.nio.file.Files.createTempDirectory("dt-carpet-fallback-test");
        try{
            java.nio.file.Files.createDirectory(root.resolve("bad.conf"));
            java.util.List<CarpetRuleCatalog.RemoteRule> registered=Arrays.asList(
                    rule("carpet","foo"),rule("bad","bar"));
            CarpetRuleDefaultConfig.ReadResult state=
                    CarpetRuleDefaultConfig.readManagers(root,fallback,registered);
            assertFalse(state.known());
            assertTrue(state.knownManagers().contains("carpet"));
            assertFalse(state.knownManagers().contains("bad"));
        }finally{
            java.nio.file.Files.delete(root.resolve("bad.conf"));
            java.nio.file.Files.delete(root);
            java.nio.file.Files.delete(fallback);
        }
    }

    @Test void missingConfigIsKnownEmptyButUnreadableLocationIsNotKnown(){
        CarpetRuleCatalog.RemoteRule core=rule("carpet","foo");
        CarpetRuleDefaultConfig.ReadResult empty=CarpetRuleDefaultConfig.read(
                java.nio.file.Paths.get("/nonexistent-dt-test-187-world-dir/carpet.conf"),
                Collections.singletonList(core));
        assertTrue(empty.known());
        assertTrue(empty.rules().isEmpty());
    }
}

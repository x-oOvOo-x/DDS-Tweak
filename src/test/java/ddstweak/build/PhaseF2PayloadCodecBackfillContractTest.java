package ddstweak.build;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class PhaseF2PayloadCodecBackfillContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static String read(String path) throws IOException {
        return Files.readString(ROOT.resolve(path));
    }

    private static String compactContract() throws IOException {
        return read("config/dds-client-ownership-contract.json").replaceAll("\\s+", "");
    }

    private static String target(String contract, String version) {
        String marker = "\"minecraft\":\"" + version + "\"";
        int markerIndex = contract.indexOf(marker);
        assertTrue(markerIndex >= 0, version);
        int start = contract.lastIndexOf('{', markerIndex);
        int end = contract.indexOf('}', markerIndex);
        assertTrue(start >= 0 && end > markerIndex, version);
        return contract.substring(start, end + 1);
    }

    @Test
    void ownershipContractSeparatesActiveNodesFromVerifiedPairing() throws IOException {
        String settings = read("settings.json");
        assertFalse(settings.contains("\"handoffVersions\""));
        assertFalse(settings.contains("\"legacyCoexistenceVersions\""));

        String contract = compactContract();
        assertTrue(contract.contains("\"canonicalSource\":\"x-oOvOo-x/Carpet-DDS-Addition-Dev:config/dds-client-ownership-contract.json\""));

        for (String version : new String[]{
                "26.3", "26.2", "26.1.2", "1.21.11", "1.21.10", "1.21.9",
                "1.21.8", "1.21.5", "1.21.4", "1.21.3", "1.21.1", "1.20.6",
                "1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.18.2", "1.17.1", "1.16.5", "1.15.2", "1.14.4"
        }) {
            assertTrue(target(contract, version).contains("\"tweakNodeStatus\":\"active\""), version);
        }

        String target1219 = target(contract, "1.21.9");
        assertTrue(target1219.contains("\"tweakNodeStatus\":\"active\""));
        assertTrue(target1219.contains("\"ddsPublishesTweakOwnership\":true"));
        assertTrue(target1219.contains("\"pairedHandoffVerified\":false"));

    }

    @Test
    void everyActiveDdsTargetIsPreCutoverReadyOnTweakSide() throws IOException {
        String contract = compactContract();
        for (String version : new String[]{
                "26.2", "26.1.2", "1.21.11", "1.21.10", "1.21.9", "1.21.8",
                "1.21.5", "1.21.4", "1.21.3", "1.21.1", "1.20.6", "1.20.4",
                "1.20.2", "1.20.1", "1.19.4", "1.18.2", "1.17.1", "1.16.5",
                "1.15.2", "1.14.4"
        }) {
            String target = target(contract, version);
            assertTrue(target.contains("\"ddsStatus\":\"active\""), version);
            assertTrue(target.contains("\"tweakNodeStatus\":\"active\""), version);
            assertTrue(
                    target.contains("\"wireMode\":\"DDS_PAYLOAD_CODEC\"")
                            || target.contains("\"wireMode\":\"DDS_PAYLOAD_READER_MAP\"")
                            || target.contains("\"wireMode\":\"LEGACY_CUSTOM_PAYLOAD\""),
                    version
            );
        }
        String planned263 = target(contract, "26.3");
        assertTrue(planned263.contains("\"ddsStatus\":\"planned\""));
        assertTrue(planned263.contains("\"tweakNodeStatus\":\"active\""));
    }

    @Test
    void target1219HasDedicatedDevelopmentNodeAndCorrectMappingBoundary() throws IOException {
        String legacyRange = read("versions/1.21.10/gradle.properties");
        assertTrue(legacyRange.contains("minecraft_dependency=>=1.21.9 <=1.21.10"));

        String properties = read("versions/1.21.9/gradle.properties");
        assertTrue(properties.contains("minecraft_version=1.21.9"));
        assertTrue(properties.contains("minecraft_dependency=1.21.9"));
        assertTrue(properties.contains("malilib_version=0.26.1"));

        String target1219 = target(compactContract(), "1.21.9");
        assertTrue(target1219.contains("\"tweakNodeStatus\":\"active\""));
        assertFalse(target1219.contains("\"pairedHandoffVerified\":true"));

        String mappingTo1219 = read("versions/mapping-1.21.8-1.21.9.txt");
        assertTrue(mappingTo1219.contains("GameProfile getId() id()"));
        assertTrue(mappingTo1219.contains("GameProfile getName() name()"));
        assertTrue(read("versions/mapping-1.21.9-1.21.10.txt").isEmpty());
        assertFalse(Files.exists(ROOT.resolve("versions/mapping-1.21.8-1.21.10.txt")));
    }

    @Test
    void readerMapTargetsAreDedicatedButNotYetPairedHandoffVerified() throws IOException {
        String contract = compactContract();
        for (String version : new String[]{"1.20.4", "1.20.2"}) {
            String target = target(contract, version);
            assertTrue(target.contains("\"tweakNodeStatus\":\"active\""), version);
            assertTrue(target.contains("\"wireMode\":\"DDS_PAYLOAD_READER_MAP\""), version);
            assertTrue(target.contains("\"ddsPublishesTweakOwnership\":false"), version);
            assertTrue(target.contains("\"pairedHandoffVerified\":false"), version);
        }
        assertTrue(read("versions/1.20.4/gradle.properties").contains("malilib_version=0.18.3"));
        assertTrue(read("versions/1.20.2/gradle.properties").contains("malilib_version=0.17.0"));
    }

    @Test
    void legacy1201HasDedicatedNodeAndRawPayloadTransport() throws IOException {
        String target = target(compactContract(), "1.20.1");
        assertTrue(target.contains("\"tweakNodeStatus\":\"active\""));
        assertTrue(target.contains("\"wireMode\":\"LEGACY_CUSTOM_PAYLOAD\""));
        assertTrue(target.contains("\"ddsPublishesTweakOwnership\":false"));
        assertFalse(target.contains("\"pairedHandoffVerified\":true"));
        String properties = read("versions/1.20.1/gradle.properties");
        assertTrue(properties.contains("malilib_version=0.16.3"));
        assertTrue(properties.contains("minecraft_version=1.20.1"));
    }

    @Test
    void legacy1194ReusesRawPayloadTransport() throws IOException {
        String target = target(compactContract(), "1.19.4");
        assertTrue(target.contains("\"tweakNodeStatus\":\"active\""));
        assertTrue(target.contains("\"wireMode\":\"LEGACY_CUSTOM_PAYLOAD\""));
        assertTrue(target.contains("\"ddsPublishesTweakOwnership\":false"));
        assertFalse(target.contains("\"pairedHandoffVerified\":true"));
        String properties = read("versions/1.19.4/gradle.properties");
        assertTrue(properties.contains("malilib_version=0.15.4"));
        assertTrue(properties.contains("minecraft_version=1.19.4"));
    }

    @Test
    void legacy1182ReusesRawPayloadTransportAndJava17() throws IOException {
        String target = target(compactContract(), "1.18.2");
        assertTrue(target.contains("\"tweakNodeStatus\":\"active\""));
        assertTrue(target.contains("\"wireMode\":\"LEGACY_CUSTOM_PAYLOAD\""));
        assertTrue(target.contains("\"ddsPublishesTweakOwnership\":false"));
        assertFalse(target.contains("\"pairedHandoffVerified\":true"));
        String properties = read("versions/1.18.2/gradle.properties");
        assertTrue(properties.contains("malilib_version=0.12.3-alpha.2"));
        assertTrue(properties.contains("minecraft_version=1.18.2"));
        assertTrue(properties.contains("java_dependency=>=17"));
        String compat = read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        assertTrue(compat.contains("new TextComponent(text)"));
        assertTrue(compat.contains("new TranslatableComponent(key,args)"));
    }

    @Test
    void legacy1171ReusesRawPayloadTransportAndJava16() throws IOException {
        String target = target(compactContract(), "1.17.1");
        assertTrue(target.contains("\"tweakNodeStatus\":\"active\""));
        assertTrue(target.contains("\"wireMode\":\"LEGACY_CUSTOM_PAYLOAD\""));
        assertTrue(target.contains("\"ddsPublishesTweakOwnership\":false"));
        assertFalse(target.contains("\"pairedHandoffVerified\":true"));
        String properties = read("versions/1.17.1/gradle.properties");
        assertTrue(properties.contains("minecraft_version=1.17.1"));
        assertTrue(properties.contains("fabric_api_version=0.46.1+1.17"));
        assertTrue(properties.contains("malilib_version=0.10.0-dev.26"));
        assertTrue(properties.contains("java_dependency=>=16"));
        assertTrue(properties.contains("malilib_coordinate=maven.modrinth:GcWjdA9I:HljUtdhE"));
    }

    @Test
    void legacy1165ReusesRawPayloadTransportAndJava8() throws IOException {
        String target = target(compactContract(), "1.16.5");
        assertTrue(target.contains("\"tweakNodeStatus\":\"active\""));
        assertTrue(target.contains("\"wireMode\":\"LEGACY_CUSTOM_PAYLOAD\""));
        assertTrue(target.contains("\"ddsPublishesTweakOwnership\":false"));
        assertFalse(target.contains("\"pairedHandoffVerified\":true"));
        String properties = read("versions/1.16.5/gradle.properties");
        assertTrue(properties.contains("minecraft_version=1.16.5"));
        assertTrue(properties.contains("fabric_api_version=0.42.0+1.16"));
        assertTrue(properties.contains("malilib_version=0.10.0-dev.21+arne.8"));
        assertTrue(properties.contains("java_dependency=>=8"));
        assertTrue(properties.contains("malilib_coordinate=maven.modrinth:GcWjdA9I:Lav6afIY"));
    }

    @Test
    void legacy1152ReusesRawPayloadTransportAndJava8() throws IOException {
        String target = target(compactContract(), "1.15.2");
        assertTrue(target.contains("\"tweakNodeStatus\":\"active\""));
        assertTrue(target.contains("\"wireMode\":\"LEGACY_CUSTOM_PAYLOAD\""));
        assertTrue(target.contains("\"ddsPublishesTweakOwnership\":false"));
        assertFalse(target.contains("\"pairedHandoffVerified\":true"));
        String properties = read("versions/1.15.2/gradle.properties");
        assertTrue(properties.contains("minecraft_version=1.15.2"));
        assertTrue(properties.contains("fabric_api_version=0.28.5+1.15"));
        assertTrue(properties.contains("malilib_version=0.10.0-dev.21+arne.3"));
        assertTrue(properties.contains("java_dependency=>=8"));
        assertTrue(properties.contains("malilib_coordinate=maven.modrinth:GcWjdA9I:yn5ciBxa"));
        String resources = read("src/main/java/ddstweak/integration/dds/fakeplayer/client/DdsBuiltinResourcePackCompat.java");
        String common = read("common.gradle");
        assertTrue(resources.contains("MC >= 11600"));
        assertTrue(resources.contains("UI assets are flattened into the mod's root assets"));
        assertTrue(common.contains("mcVersion < 1_16_00"));
        assertTrue(common.contains("resourcepacks/dds_fake_player_ui/assets"));
        assertTrue(common.contains("into new File(destinationDir, 'assets')"));
    }

    @Test
    void legacy1144CompletesLegacyTargetMatrix() throws IOException {
        String target = target(compactContract(), "1.14.4");
        assertTrue(target.contains("\"tweakNodeStatus\":\"active\""));
        assertTrue(target.contains("\"wireMode\":\"LEGACY_CUSTOM_PAYLOAD\""));
        assertTrue(target.contains("\"ddsPublishesTweakOwnership\":false"));
        assertFalse(target.contains("\"pairedHandoffVerified\":true"));
        String properties = read("versions/1.14.4/gradle.properties");
        assertTrue(properties.contains("minecraft_version=1.14.4"));
        assertTrue(properties.contains("fabric_api_version=0.28.5+1.14"));
        assertTrue(properties.contains("malilib_version=0.10.0-dev.20+arne.2"));
        assertTrue(properties.contains("malilib_coordinate=maven.modrinth:GcWjdA9I:ROauLEoP"));
        assertTrue(properties.contains("java_dependency=>=8"));
    }

    @Test
    void fabricMetadataUsesRuntimeRangeInsteadOfBuildVersion() throws IOException {
        String mod = read("src/main/resources/fabric.mod.json");
        String common = read("common.gradle");
        assertTrue(mod.contains("\"minecraft\": \"${minecraft_dependency}\""));
        assertFalse(mod.contains("\"minecraft\": \"${minecraft_version}\""));
        assertTrue(common.contains("minecraft_dependency: project.minecraft_dependency"));
    }

    @Test
    void targetDependenciesRemainVersionOwned() throws IOException {
        String common = read("common.gradle");
        assertTrue(common.contains("project.malilib_coordinate"));
        assertTrue(common.contains("project.mcVersion"));
        assertTrue(read("versions/1.21.8/gradle.properties").contains("malilib_version=0.25.7"));
        assertTrue(read("versions/1.20.6/gradle.properties").contains("malilib_version=0.19.0"));
    }

    @Test
    void repeatedLegacyDifferencesUsePlatformFacades() throws IOException {
        String mc = read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        String ml = read("src/main/java/ddstweak/platform/malilib/MalilibCompat.java");
        assertTrue(mc.contains("MC >= 12105"));
        assertTrue(mc.contains("MC >= 12101"));
        assertTrue(ml.contains("MC > 12006"));
    }

    @Test
    void guiTextureBlitTracksModernAndPoseStackRenderBoundaries() throws IOException {
        String path = "src/main/java/ddstweak/platform/minecraft/ScreenRenderCompat.java";
        String source = read(path);
        assertTrue(source.contains("MC >= 260100"), path);
        assertTrue(source.contains("//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;"), path);
        assertTrue(source.contains("MC >= 12106"), path);
        assertTrue(source.contains("MC >= 12102"), path);
        assertTrue(source.contains("MC >= 12000"), path);
        assertTrue(source.contains("RenderPipelines.GUI_TEXTURED"), path);
        assertTrue(source.contains("RenderType::guiTextured"), path);
        assertTrue(source.contains("GuiComponent.blit"), path);
    }

    @Test
    void legacy1206SlotIconUsesOldMappedResourceLocationName() throws IOException {
        String source = read("src/main/java/ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(source.contains("Pair<net.minecraft.resources.ResourceLocation,net.minecraft.resources.ResourceLocation> getNoItemIcon()"));
        assertFalse(source.contains("Pair<Identifier,Identifier> getNoItemIcon()"));
    }

    @Test
    void finalCutoverVerificationComesFromTheCurrentContractNotHistoricalF2Docs() throws IOException {
        String contract = compactContract();
        assertTrue(target(contract, "1.21.11").contains("\"pairedHandoffVerified\":true"));
        assertTrue(target(contract, "1.20.6").contains("\"pairedHandoffVerified\":true"));
        assertFalse(target(contract, "1.21.9").contains("\"pairedHandoffVerified\":true"));
        assertFalse(target(contract, "26.2").contains("\"pairedHandoffVerified\":true"));
        assertFalse(target(contract, "26.1.2").contains("\"pairedHandoffVerified\":true"));
    }
}

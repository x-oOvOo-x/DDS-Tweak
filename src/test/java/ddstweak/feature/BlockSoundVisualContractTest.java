package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class BlockSoundVisualContractTest {
    private static String source(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Test void everySoundMarkerUsesTheSameThroughWallOutlineAndFill() throws IOException {
        String rail=source("src/main/java/ddstweak/feature/railpowerguide/RailPowerGuideRenderer.java");
        String sound=source("src/main/java/ddstweak/feature/blocksoundtracker/BlockSoundRenderer.java");
        assertTrue(rail.contains("0.003F,4.0F,OUTLINE,false"));
        assertTrue(sound.contains("OUTLINE=new Color4f(0.659F,0.333F,0.969F,0.90F)"));
        assertTrue(sound.contains("FILL=new Color4f(0.659F,0.333F,0.969F,0.14F)"));
        assertFalse(sound.contains("POSITION_FILL"));
        assertFalse(sound.contains("marker.confirmed()"),
                "Do not visually distinguish confirmed blocks from estimated sound positions");
        assertTrue(sound.contains("renderThroughFill(pos);"));
        assertTrue(sound.contains("RenderUtils.renderBlockOutline(pos,0.003F,4.0F,OUTLINE,true);"));
    }

    @Test void facesUseExplicitNoDepthPipelineInsteadOfSortingFlag() throws IOException {
        String sound=source("src/main/java/ddstweak/feature/blocksoundtracker/BlockSoundRenderer.java");
        assertTrue(sound.contains("MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL"));
        assertTrue(sound.contains("RenderUtils.renderAreaSidesBatched(pos,pos,FILL,0.002,ctx.getBuilder())"));
        assertTrue(sound.contains("ctx.draw(false);"));
        assertTrue(sound.contains("//#if MC >= 260200"));
        assertFalse(sound.contains("RenderUtils.renderAreaSides(pos,pos,"),
                "MaLiLib renderAreaSides forces depth testing even with its sorting flag set");
        assertFalse(sound.contains("//$ "));
        assertFalse(sound.matches("(?s).*\\n\\s*//\\$(?!\\$).*"),
                "Every ReplayMod preprocessor line must use the two-dollar marker");
    }

    @Test void thereIsNoThroughWallsSettingAndTheMainTipIsConcise() throws IOException {
        String config=source("src/main/java/ddstweak/config/Configs.java");
        String screen=source("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        assertFalse(config.contains("BLOCK_SOUND_THROUGH_WALLS"));
        assertFalse(screen.contains("BLOCK_SOUND_THROUGH_WALLS"));

        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            String lang=source("src/main/resources/assets/dds-tweak/lang/"+locale+".json");
            assertFalse(lang.contains("blockSoundThroughWalls"),locale);
            assertFalse(lang.contains("blocksoundthroughwalls"),locale);
        }
        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            var lang=com.google.gson.JsonParser.parseString(
                    source("src/main/resources/assets/dds-tweak/lang/"+locale+".json")).getAsJsonObject();
            String canonical=lang.get("dds-tweak.config.visual.comment.blockSoundTracker").getAsString();
            String alias=lang.get("dds-tweak.config.visual.comment.blocksoundtracker").getAsString();
            assertEquals(canonical,alias,"Legacy alias must retain the same meaning");
            assertFalse(canonical.isBlank());
            assertTrue(canonical.codePointCount(0,canonical.length())<=(locale.equals("en_us")?80:40),
                    locale+": block sound tip is too long");
        }
    }
}

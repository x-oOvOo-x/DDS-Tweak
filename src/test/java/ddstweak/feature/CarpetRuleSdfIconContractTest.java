package ddstweak.feature;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contract only: renderer compilation, resource reload and actual GPU output need runtime QA. */
final class CarpetRuleSdfIconContractTest {
    private static String read(String file) throws Exception {
        return Files.readString(Path.of(file));
    }

    @Test void selectedAccentsUseTheSame26xGpuMaskAsTheirForeground() throws Exception {
        String style=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleUiStyle.java");
        assertTrue(style.contains("SdfRoundedRectRenderer.leftAccent(g,left,top,right,top+height,radius,shift,ACCENT)"));
        assertFalse(style.contains("ScreenRenderCompat.beginScissor(g,left-shift,top,left,top+height)"));
        String sdf=read("src/main/java/ddstweak/gui/SdfRoundedRectRenderer.java");
        String legacy=read("src/main/java/ddstweak/gui/LegacyGpuSdfRenderer.java");
        assertTrue(sdf.contains("draw(gui, left - shift, top, right - shift, bottom, radius, argb, 4 + shift)"));
        assertTrue(legacy.contains("drawMode(gui, x1, y1, x2, y2, radius, argb, 4 + shift)"));
        for(String shaderFile:new String[]{"rounded_rect.fsh","rounded_rect_legacy.fsh"}){
            String shader=read("src/main/resources/assets/dds-tweak/shaders/core/"+shaderFile);
            assertTrue(shader.contains("roundedDistance(localPoint - vec2(shift, 0.0), halfSize, cornerRadius)"));
            assertTrue(shader.contains("max(0.0, backingCoverage - foregroundCoverage)"));
        }
        assertTrue(style.contains("static int roundedCoverage(")); // pre-26.x fallback remains.
    }

    @Test void halfPixelSelectionRadiusIsIdenticalInBothShaderPipelines() throws Exception {
        String style=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleUiStyle.java");
        assertTrue(style.contains("selected?7.5f:7.0f"));
        assertTrue(style.contains("float radius,int shift"));
        String modern=read("src/main/java/ddstweak/gui/SdfRoundedRectRenderer.java");
        String legacy=read("src/main/java/ddstweak/gui/LegacyGpuSdfRenderer.java");
        assertTrue(modern.contains("Math.round(Math.max(0, Math.min(radius,"));
        assertTrue(legacy.contains("Math.round(Math.max(0, Math.min(radius,"));
        for(String file:new String[]{"rounded_rect.vsh","rounded_rect_legacy.vsh"}){
            String source=read("src/main/resources/assets/dds-tweak/shaders/core/"+file);
            assertTrue(source.contains("cornerRadius = float(UV1.x) * 0.5;"));
        }
    }

    @Test void editorControlHoverHasPriorityOverRuleTooltip() throws Exception {
        String main=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerScreen.java");
        String presentation=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerRenderer.java");
        assertTrue(presentation.contains("owner.hoveredFavoriteRule=entry;"));
        assertTrue(main.contains("else if(hoveredFavoriteRule!=null)"));
        assertTrue(presentation.contains("if(hover&&owner.mouseX<controlLeft)owner.hoveredRule=entry;"));
        assertTrue(main.contains("tr(\"dds-tweak.carpet_rules.default_left_hint\")"));
        assertTrue(main.contains("tr(\"dds-tweak.carpet_rules.default_right_hint\")"));
        // Normalize line endings: Windows checkouts may use CRLF, not LF.
        assertTrue(main.replace("\r\n", "\n").contains(
                "tr(\"dds-tweak.carpet_rules.default_right_hint\"),\n                        \" \""));
        String preset=read("src/main/java/ddstweak/feature/carpetrules/CarpetRulePresetScreen.java");
        assertTrue(preset.contains("tooltipRule=null;"));
        assertTrue(preset.contains("hoveredRemoveRule=true;"));
    }

    @Test void downAndRightArrowsShareGpuSdfAndRetainOldPixelFallback() throws Exception {
        String style=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleUiStyle.java");
        String sdf=read("src/main/java/ddstweak/gui/SdfRoundedRectRenderer.java");
        String legacy=read("src/main/java/ddstweak/gui/LegacyGpuSdfRenderer.java");
        assertTrue(style.contains("SdfRoundedRectRenderer.triangle(g,x,y,x+8,y+4,false,color)"));
        assertTrue(style.contains("SdfRoundedRectRenderer.triangle(g,x+1,y,x+5,y+8,true,color)"));
        assertTrue(style.contains("ScreenRenderCompat.fill(g,x+3,y+3,x+5,y+4,color)"));
        assertTrue(sdf.contains("right ? 3 : 2"));
        assertTrue(legacy.contains("drawTriangle("));
        for(String filename:new String[]{"rounded_rect.fsh","rounded_rect_legacy.fsh"}){
            String shader=read("src/main/resources/assets/dds-tweak/shaders/core/"+filename);
            assertTrue(shader.contains("triangleDistance(localPoint, a, b, c)"));
            assertTrue(shader.contains("float winding = sign(cross2(b - a, c - a))"));
            assertTrue(shader.contains("outlineWidth > 0.0 && outlineWidth < 1.5"));
        }
    }

    @Test void pre26ScreensKeepRoundedCornersWithoutGpuSdf() throws Exception {
        String renderer=read("src/main/java/ddstweak/gui/SdfRoundedRectRenderer.java");
        assertTrue(renderer.contains("cpuRoundedFill(gui, x1, y1, x2, y2, radius, argb)"));
        assertTrue(renderer.contains("private static void cpuRoundedFill("));
        assertTrue(renderer.contains("private static void paintRoundedRows("));
        assertTrue(renderer.contains("cornerCoverageMask(r,steps)"));
        assertTrue(renderer.contains("CORNER_COVERAGE_CACHE"));
        assertTrue(renderer.contains("if(next==coverage)continue;"));
        assertTrue(renderer.contains("if(mask[row+steps-1]!=16)"));
        assertTrue(renderer.contains("private static int cornerCoverage(float radius,"));
        String style=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleUiStyle.java");
        assertTrue(style.contains("if(selected)selectedAccent(g,left,top,right,height,radius,SELECTION_BACKING_SHIFT)"));
        assertTrue(style.contains("GuiIconRenderer.VANILLA_SEARCH"));
        assertTrue(style.contains("LEGACY_SEARCH.render(false,false)"));
    }

    @Test void searchAndPlanEditUseOriginalIconsAcrossVersionBoundaries() throws Exception {
        String style=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleUiStyle.java");
        assertTrue(style.contains("LEGACY_SEARCH.render(false,false)"));
        assertTrue(style.contains("GuiIconRenderer.VANILLA_SEARCH"));
        assertTrue(style.contains("new WidgetIcon(0,0,MaLiLibIcons.SEARCH)"));
        assertTrue(style.contains("LEGACY_SEARCH.setPosition("));
        assertTrue(style.contains("import fi.dy.masa.malilib.gui.widgets.WidgetIcon"));
        assertFalse(style.contains("ScreenRenderCompat.blit(g,MaLiLibIcons.SEARCH.getTexture()"));
        assertTrue(style.contains("expanded?0xBB28553F:0x994B535A"));
        assertFalse(style.contains("textures/gui/recipe_book.png"));
        assertFalse(style.contains("expanded?0xFFF0F6F1"));
        assertFalse(style.contains("SEARCH_GLYPH"));
        assertFalse(style.contains("EDIT_GLYPH"));
        assertFalse(style.contains("drawMask("));

        String renderer=read("src/main/java/ddstweak/gui/GuiIconRenderer.java");
        assertTrue(renderer.contains("ScreenRenderCompat.blitTintedResource(graphics,\"dds-tweak\""));
        assertTrue(renderer.contains("textures/gui/sprites/carpet/"));
        assertTrue(renderer.contains("64,64,tint"));
        String legacy=read("src/main/java/ddstweak/platform/minecraft/ScreenRenderCompat.java");
        assertTrue(legacy.contains("float scaleX=width/(float)textureWidth,scaleY=height/(float)textureHeight"));
        assertTrue(legacy.contains("blit(graphics,texture,0,0,u,v,textureWidth,textureHeight,textureWidth,textureHeight)"));
        assertTrue(legacy.contains("graphics.pose().scale(scaleX,scaleY,1.0F)"));

        String presets=read("src/main/java/ddstweak/feature/carpetrules/CarpetRulePresetScreen.java");
        assertTrue(presets.contains("GuiIconRenderer.draw(g,GuiIconRenderer.PLAN_EDIT"));
        assertFalse(presets.contains("CarpetRuleUiStyle.glyph(g,\"edit\""));
    }

    @Test void operationLifecycleRemainsOwnedByTheScreen() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerScreen.java");
        String renderer=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerRenderer.java");
        assertTrue(screen.contains("private void refreshOperationFeedback()"));
        assertTrue(screen.contains("CarpetRuleClientNetwork.operations().expire(System.currentTimeMillis());"));
        assertTrue(screen.contains("if(lastOperation==null)return;"));
        assertFalse(renderer.contains("CarpetRuleClientNetwork.operations().expire("));
        assertFalse(renderer.contains("owner.lastOperation="));
        assertFalse(renderer.contains("owner.feedback="));
    }

    @Test void resourcePacksCanReplaceSevenStateSpritesWithoutFixedUvDimensions() throws Exception {
        String base="src/main/resources/assets/dds-tweak/textures/gui/sprites/carpet/";
        String[] icons={"rule_favorite_off","rule_favorite_on","rule_default_off","rule_default_on","rule_reset",
                "rule_plan_edit","rule_plan_delete"};
        for(String icon:icons){
            Path png=Path.of(base+icon+".png");
            assertTrue(Files.isRegularFile(png),"missing "+png);
            BufferedImage image=ImageIO.read(png.toFile());
            assertNotNull(image,icon);
            assertEquals(64,image.getWidth(),icon);
            assertEquals(64,image.getHeight(),icon);
            assertTrue(image.getColorModel().hasAlpha(),icon);
        }
        String renderer=read("src/main/java/ddstweak/gui/GuiIconRenderer.java");
        assertTrue(renderer.contains("Identifier.fromNamespaceAndPath(namespace,path)"));
        assertTrue(renderer.contains("VANILLA_SEARCH = \"icon/search\""));
        assertTrue(renderer.contains("String namespace=VANILLA_SEARCH.equals(icon)?\"minecraft\":\"dds-tweak\""));
        assertTrue(renderer.contains("String path=VANILLA_SEARCH.equals(icon)?icon:\"carpet/\"+icon"));
        assertTrue(renderer.contains("graphics.blitSprite(RenderPipelines.GUI_TEXTURED,sprite,x,y,width,height,tint)"));
        assertTrue(renderer.contains("graphics.blitSprite(sprite,x,y,width,height)"));
        assertTrue(renderer.contains("//#if MC >= 12002"));
        // ResourceLocation constructors became private starting in Minecraft 1.21.
        assertTrue(renderer.contains("//#elseif MC >= 12101"));
        assertTrue(renderer.contains("ScreenRenderCompat.blitTintedResource(graphics,\"dds-tweak\""));
        String compat=read("src/main/java/ddstweak/platform/minecraft/ScreenRenderCompat.java");
        assertTrue(compat.contains("ResourceLocation.fromNamespaceAndPath(namespace,path)"));
        assertTrue(compat.contains("new ResourceLocation(namespace,path)"));
        assertFalse(renderer.contains("textureWidth"));
        String presentation=read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerRenderer.java");
        assertTrue(presentation.contains("GuiIconRenderer.FAVORITE_ON"));
        assertTrue(presentation.contains("GuiIconRenderer.FAVORITE_OFF"));
        assertTrue(presentation.contains("saveGlyph(g,sx+4,sy+4,saved==1"));
        assertTrue(presentation.contains("saveGlyph(g,glyphX,modeY-14,savedState==1"));
        assertTrue(renderer.contains("PLAN_EDIT = \"rule_plan_edit\""));
        assertTrue(renderer.contains("PLAN_DELETE = \"rule_plan_delete\""));
        String presets=read("src/main/java/ddstweak/feature/carpetrules/CarpetRulePresetScreen.java");
        assertTrue(presets.contains("GuiIconRenderer.PLAN_EDIT"));
        assertTrue(presets.contains("GuiIconRenderer.PLAN_DELETE"));
        assertTrue(presets.contains("deleteNameButton=hitButton("));
        assertTrue(presets.contains("addCompat(cancelNameButton);addCompat(saveNameButton)"));
        assertTrue(presets.contains("if(naming&&x>=middleLeft())return true"));
        assertTrue(presets.contains("if(naming||importing||plan==null||selectedRule==null)"));
        assertTrue(presets.contains("hoveredRemoveRule=true;"));
        assertTrue(presets.contains("if(hoveredRemoveRule)"));
        assertTrue(presets.contains("this::deleteWithShift"));
        assertTrue(presets.contains("if(!shiftHeld())return;"));
        assertTrue(presets.contains("deleteNameButton=hitButton(deleteButtonX(),createTop()+4,20,20"));
        assertTrue(presets.contains("positionWidget(deleteNameButton,deleteButtonX(),y+4)"));
        assertTrue(presets.contains("positionWidget(cancelNameButton,x+4,y+52)"));
        assertTrue(presets.contains("positionWidget(saveNameButton,x+8+bw,y+52)"));
        assertTrue(presets.contains("if(!renaming||!deleteNameButton.visible)return;"));
        assertTrue(presets.contains("tr(\"dds-tweak.carpet_rules.plans.shift_delete_hint\")"));
        assertFalse(presets.contains("text(g,fit(tr(\"dds-tweak.carpet_rules.plans.shift_delete_hint\")"));
        assertTrue(presets.contains("renderListTooltip(null);"));
        assertFalse(presets.contains("planMenuTop("));
        assertTrue(presets.contains("CarpetRulePresetLayout.rowAt(y,listTop(),listBottom(),offset,"));
        assertTrue(presets.contains("positionNameWidgets()"));
    }
}

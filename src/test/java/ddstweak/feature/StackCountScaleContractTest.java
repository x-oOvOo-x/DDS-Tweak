package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class StackCountScaleContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}

    @Test void visualConfigIsDefaultOffWithVanillaScale() throws Exception {
        String c=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(c.contains("STACK_COUNT_SCALE_ENABLED=MalilibCompat.bool(\"stackCountScaleEnabled\",false,VISUAL_KEY)"));
        assertTrue(c.contains("ConfigInteger STACK_COUNT_SCALE"));
        assertTrue(c.contains("MalilibCompat.integer(\"stackCountScale\",100,50,100,true,VISUAL_KEY)"));
        assertTrue(c.contains("STACK_COUNT_SCALE_ENABLED,STACK_COUNT_SCALE"));
    }

    @Test void featureOwnsEnablementAndPercentageScale() throws Exception {
        String f=read("src/main/java/ddstweak/feature/stackcountscale/StackCountScaleFeature.java");
        assertTrue(f.contains("Configs.Visual.STACK_COUNT_SCALE_ENABLED.getBooleanValue()"));
        assertTrue(f.contains("Configs.Visual.STACK_COUNT_SCALE.getIntegerValue()/100.0F"));
    }

    @Test void mixinReplacesOnlyItemCountTextAtBottomRightAnchor() throws Exception {
        String m=read("src/main/java/ddstweak/mixin/feature/stackcountscale/GuiGraphicsMixin.java"),j=read("src/main/resources/dds-tweak.mixins.json");
        for(String token:new String[]{"@Mixin(GuiGraphics.class)","//#elseif MC >= 12102","method=\"renderItemCount\"","at=@At(\"HEAD\")","cancellable=true","StackCountScaleFeature.enabled()","StackCountScaleFeature.scale()","pose.pushMatrix()","pose.translate(x+17,y+18)","pose.scale(scale,scale)","drawString(font,amount,-font.width(amount),-font.lineHeight,-1,true)","pose.popMatrix()","ci.cancel()"})assertTrue(m.contains(token),token);
        for(String token:new String[]{"//#if MC >= 260100","@Mixin(GuiGraphicsExtractor.class)","method=\"itemCount\"","g.text(font,amount,-font.width(amount),-font.lineHeight,-1,true)"})assertTrue(m.contains(token),token);
        assertFalse(m.contains("renderItemBar"));assertFalse(m.contains("renderItemCooldown"));assertTrue(j.contains("feature.stackcountscale.GuiGraphicsMixin"));
    }

    @Test void legacyTargetsRedirectOnlyTheEmbeddedCountDraw() throws Exception {
        String m=read("src/main/java/ddstweak/mixin/feature/stackcountscale/GuiGraphicsMixin.java");
        for(String token:new String[]{"@Redirect(method=\"renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V\"","target=\"Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I\"","ddstweak$scaleLegacyStackCount","pose.translate(x+width,y+font.lineHeight,0)","g.drawString(font,amount,-width,-font.lineHeight,color,shadow)","//$$ import com.mojang.blaze3d.vertex.PoseStack;","//$$ @Redirect"})assertTrue(m.contains(token),token);
        assertFalse(m.contains("//$ import com.mojang.blaze3d.vertex.PoseStack;\n"),"legacy PoseStack import must use the real //$$ preprocess marker");
    }
}

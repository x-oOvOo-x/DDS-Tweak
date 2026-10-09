package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class HelpfulHitboxesContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}

    @Test void tweakIsOptInAndRegistered() throws Exception {
        String c=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(c.contains("HELPFUL_HITBOXES=MalilibCompat.boolHotkeyed(\"helpfulHitboxes\",false,\"\",TWEAKS_KEY)"));
        int options=c.indexOf("public static final List<IConfigBase> OPTIONS=ImmutableList.of(",c.indexOf("HELPFUL_HITBOXES"));
        String registered=c.substring(options,c.indexOf(");",options));
        assertTrue(registered.contains("HELPFUL_HITBOXES"));
    }

    @Test void fullCubeAndExpandedFamiliesAreSeparated() throws Exception {
        String f=read("src/main/java/ddstweak/feature/helpfulhitboxes/HelpfulHitboxesFeature.java");
        for(String token:new String[]{"isHoldingMatchingItem(block,context)","context.isHoldingItem(block.asItem())","getOffhandItem().getItem()==block.asItem()","block instanceof FenceBlock","block instanceof IronBarsBlock","Shapes.block()","block instanceof EndRodBlock","block instanceof BambooStalkBlock","BambooBlock","block instanceof ChainBlock","block instanceof LightningRodBlock","block instanceof PointedDripstoneBlock","EXPAND=2.0D/16.0D","shape.bounds()","Shapes.box("})assertTrue(f.contains(token),token);
        assertFalse(f.contains("Candle"));
        assertFalse(f.contains("SeaPickle"));
        assertFalse(f.contains("TurtleEgg"));
    }

    @Test void modernOutlineMixinOnlyAdjustsReturnedSelectionShape() throws Exception {
        String m=read("src/main/java/ddstweak/mixin/feature/helpfulhitboxes/BlockStateBaseMixin.java");
        String j=read("src/main/resources/dds-tweak.mixins.json");
        for(String token:new String[]{"//#if MC >= 11600","@Mixin(BlockBehaviour.BlockStateBase.class)","method=\"getShape(","at=@At(\"RETURN\")","HelpfulHitboxesFeature.adjust"})assertTrue(m.contains(token),token);
        assertFalse(m.contains("getCollisionShape"));
        assertTrue(j.contains("feature.helpfulhitboxes.BlockStateBaseMixin"));
    }

    @Test void legacyOutlineMixinUsesBlockStateDelegationHook() throws Exception {
        String m=read("src/main/java/ddstweak/mixin/feature/helpfulhitboxes/LegacyBlockShapeMixin.java");
        String j=read("src/main/resources/dds-tweak.mixins.json");
        String g=read("common.gradle");
        for(String token:new String[]{"//#if MC < 11600","@Mixin(BlockState.class)","@Shadow public abstract Block getBlock()","method=\"getShape(Lnet/minecraft/world/level/BlockGetter;","HelpfulHitboxesFeature.adjust(getBlock(),context,original)"})assertTrue(m.contains(token),token);
        assertFalse(m.contains("CrossCollisionBlock.class"));
        assertFalse(m.contains("getCollisionShape"));
        assertTrue(j.contains("feature.helpfulhitboxes.LegacyBlockShapeMixin"));
        assertTrue(g.contains("mcVersion < 1_16_00"));
        assertTrue(g.contains("unavailable.add('feature.helpfulhitboxes.BlockStateBaseMixin')"));
        assertTrue(g.contains("unavailable.add('feature.helpfulhitboxes.LegacyBlockShapeMixin')"));
    }
    @Test void matchingHeldItemCanRevealHelpfulOutline() throws Exception {
        String f=read("src/main/java/ddstweak/feature/helpfulhitboxes/HelpfulHitboxesFeature.java");
        assertTrue(f.contains("public static boolean shouldRevealOutline()"));
        assertTrue(f.contains("minecraft.hitResult.getType()!=HitResult.Type.BLOCK"));
        assertTrue(f.contains("((BlockHitResult)minecraft.hitResult).getBlockPos()"));
        assertTrue(f.contains("isHelpfulBlock(block)"));
        assertTrue(f.contains("minecraft.player.getMainHandItem().getItem()==block.asItem()"));
        assertTrue(f.contains("minecraft.player.getOffhandItem().getItem()==block.asItem()"));
    }

    @Test void sneakingRestoresVanillaPreciseOutline() throws Exception {
        String f=read("src/main/java/ddstweak/feature/helpfulhitboxes/HelpfulHitboxesFeature.java");
        assertTrue(f.contains("||isSneaking()||"));
        assertTrue(f.contains("Minecraft.getInstance()"));
        assertTrue(f.contains("minecraft.player!=null&&minecraft.player.isShiftKeyDown()"));
    }
}

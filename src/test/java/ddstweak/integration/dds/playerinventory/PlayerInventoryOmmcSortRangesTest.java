package ddstweak.integration.dds.playerinventory;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryOmmcSortRangesTest{
    private static int[] gca(PlayerInventorySlotRole role,int slot){
        return PlayerInventorySortRanges.range(role,slot,18,45,45,54,54,81,90);
    }
    private static int[] org(PlayerInventorySlotRole role,int slot){
        return PlayerInventorySortRanges.range(role,slot,9,36,0,9,54,81,90);
    }
    @Test void gcaSortsOnlyTheHoveredStorageDomain(){
        assertArrayEquals(new int[]{18,45},gca(PlayerInventorySlotRole.MAIN,18));
        assertArrayEquals(new int[]{18,45},gca(PlayerInventorySlotRole.MAIN,44));
        assertArrayEquals(new int[]{45,54},gca(PlayerInventorySlotRole.HOTBAR,45));
        assertArrayEquals(new int[]{54,81},gca(PlayerInventorySlotRole.VIEWER_MAIN,54));
        assertArrayEquals(new int[]{81,90},gca(PlayerInventorySlotRole.VIEWER_HOTBAR,89));
        assertNull(gca(PlayerInventorySlotRole.MAIN,17));
        assertNull(gca(PlayerInventorySlotRole.MAIN,45));
    }
    @Test void orgHotbarBeforeMainStaysIndependent(){
        assertArrayEquals(new int[]{0,9},org(PlayerInventorySlotRole.HOTBAR,0));
        assertArrayEquals(new int[]{9,36},org(PlayerInventorySlotRole.MAIN,35));
        assertArrayEquals(new int[]{54,81},org(PlayerInventorySlotRole.VIEWER_MAIN,80));
        assertNull(org(PlayerInventorySlotRole.MAIN,8));
    }
    @Test void enderAndLegacyLayoutsHaveNoInventedSlots(){
        assertArrayEquals(new int[]{27,54},PlayerInventorySortRanges.range(
                PlayerInventorySlotRole.ENDER_STORAGE,53,27,54,54,54,54,81,90));
        assertArrayEquals(new int[]{0,54},PlayerInventorySortRanges.range(
                PlayerInventorySlotRole.ENDER_STORAGE,53,0,54,54,54,54,81,90));
        assertArrayEquals(new int[]{5,32},PlayerInventorySortRanges.range(
                PlayerInventorySlotRole.MAIN,31,5,32,32,41,41,68,77));
        assertNull(PlayerInventorySortRanges.range(
                PlayerInventorySlotRole.HOTBAR,54,27,54,54,54,54,81,90));
    }
    @Test void controlsEquipmentDisabledAndUnknownNeverGenerateClicks(){
        for(PlayerInventorySlotRole role:new PlayerInventorySlotRole[]{
                PlayerInventorySlotRole.CONTROL,PlayerInventorySlotRole.EQUIPMENT,
                PlayerInventorySlotRole.DISABLED,PlayerInventorySlotRole.PLACEHOLDER,
                PlayerInventorySlotRole.EXTRA,PlayerInventorySlotRole.UNKNOWN}){
            assertNull(gca(role,18),role.toString());
        }
        assertNull(gca(PlayerInventorySlotRole.UNKNOWN,-1));
        assertNull(gca(PlayerInventorySlotRole.MAIN,Integer.MAX_VALUE));
    }
    @Test void plannedClicksAreValidatedBeforeAnyPickup(){
        int[] main={18,45};
        assertTrue(PlayerInventorySortRanges.contains(main,18));
        assertTrue(PlayerInventorySortRanges.contains(main,44));
        assertFalse(PlayerInventorySortRanges.contains(main,17));
        assertFalse(PlayerInventorySortRanges.contains(main,45));
        assertFalse(PlayerInventorySortRanges.contains(main,7));
        assertFalse(PlayerInventorySortRanges.contains(null,18));
        assertTrue(PlayerInventorySortRanges.allowsMergeClick(main,18));
        assertTrue(PlayerInventorySortRanges.allowsMergeClick(main,-18));
        assertTrue(PlayerInventorySortRanges.allowsMergeClick(main,-44));
        assertFalse(PlayerInventorySortRanges.allowsMergeClick(main,45));
        assertFalse(PlayerInventorySortRanges.allowsMergeClick(main,-7));
        assertFalse(PlayerInventorySortRanges.allowsMergeClick(main,-999));
        assertFalse(PlayerInventorySortRanges.allowsMergeClick(main,Integer.MIN_VALUE));
        assertFalse(PlayerInventorySortRanges.allowsMergeClick(main,null));
    }
    @Test void compatibilityProbesBothUpstreamImplementationsAndFailsClosed()throws IOException{
        Path root=Path.of("").toAbsolutePath().resolve("src/main");
        Path code=root.resolve("java/ddstweak/");
        String legacy=Files.readString(code.resolve("mixin/integration/ommc/SortInventoryHelperMixin.java"));
        String modern=Files.readString(code.resolve("mixin/integration/ommc/SortInventoryModernMixin.java"));
        String probe=Files.readString(code.resolve("mixin/integration/ommc/OmmcSortTargetProbe.java"));
        String guards=Files.readString(code.resolve("integration/ommc/OmmcSortCompatibility.java"));
        String fallback=Files.readString(code.resolve("mixin/integration/ommc/SortInventoryModernFallbackMixin.java"));
        String plugin=Files.readString(code.resolve("mixin/DdsTweakMixinConfigPlugin.java"));
        String config=Files.readString(root.resolve("resources/dds-tweak.mixins.json"));
        assertTrue(legacy.contains("com.plusls.ommc.feature.sortInventory.SortInventoryUtil"));
        assertTrue(modern.contains("com.plusls.ommc.impl.feature.sortInventory.SortInventoryHelper"));
        for(String source:new String[]{legacy,modern}){
            assertTrue(source.contains("@Pseudo"));
            assertTrue(source.contains("require=0"));
            assertTrue(source.contains("OmmcSortCompatibility.range("));
            assertTrue(source.contains("OmmcSortCompatibility.clicks("));
            assertTrue(source.contains("//#if MC < 260200"));
        }
        assertTrue(probe.contains("mod.findPath(file)"));
        assertTrue(probe.contains("new ClassReader(input)"));
        assertTrue(probe.contains("rangeCustom"));
        assertTrue(probe.contains("tupleAdapter?adapted:direct"));
        assertTrue(probe.contains("fallback?sort&&!full:tupleAdapter?adapted:direct"));
        assertTrue(probe.contains("sorterMethods.add(method.name+method.desc)"));
        assertTrue(probe.contains("sort={}, range={}, clicks={}, tuple={}, methods={}"));
        assertTrue(guards.contains("WARNED_UNSUPPORTED.compareAndSet(false,true)"));
        assertTrue(fallback.contains("@Inject(method=\"sort\""));
        assertTrue(fallback.contains("OmmcSortCompatibility.unsupported(cir)"));
        assertTrue(guards.contains("menu.slots.indexOf(hovered)"));
        assertTrue(guards.contains("PlayerInventorySortRanges.allowsMergeClick(range,click)"));
        assertTrue(guards.contains("ci.cancel()"));
        assertTrue(guards.contains("OMMC sort range hook"));
        assertTrue(guards.contains("OMMC sort click batch blocked"));
        String adapter=Files.readString(code.resolve("mixin/integration/ommc/SortInventoryModernTupleAdapterMixin.java"));
        assertTrue(adapter.contains("OmmcSortCompatibility.rangeTupleAdapter("));
        assertTrue(adapter.contains("OmmcSortCompatibility.clicksTupleAdapter("));
        assertTrue(probe.contains("com/plusls/ommc/util/Tuple.class"));
        assertTrue(guards.contains("OmmcSortTupleAccess.indices(swap)"));
        assertTrue(plugin.contains("OmmcSortTargetProbe.shouldApply(target,fallback,tupleAdapter)"));
        for(String name:new String[]{"SortInventoryHelperMixin","SortInventoryModernMixin",
                "SortInventoryLegacyFallbackMixin","SortInventoryModernFallbackMixin",
                "SortInventoryModernTupleAdapterMixin"}){
            assertTrue(config.contains("integration.ommc."+name));
        }
    }
}

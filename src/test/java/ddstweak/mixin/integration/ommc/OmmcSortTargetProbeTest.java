package ddstweak.mixin.integration.ommc;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import static org.junit.jupiter.api.Assertions.*;

/** Regression for Mojang-named DT vs intermediary-named OMMC release JARs. */
final class OmmcSortTargetProbeTest{
    @Test void mapsAllFiveSorterTypesToFabricIntermediary(){
        String[][] pairs={
                {"net.minecraft.world.inventory.AbstractContainerMenu","net.minecraft.class_1703"},
                {"net.minecraft.world.inventory.Slot","net.minecraft.class_1735"},
                {"net.minecraft.util.Tuple","net.minecraft.class_3545"},
                {"net.minecraft.world.entity.player.Player","net.minecraft.class_1657"},
                {"net.minecraft.client.multiplayer.MultiPlayerGameMode","net.minecraft.class_636"}
        };
        for(String[] pair:pairs){
            assertEquals(pair[1],OmmcSortTargetProbe.intermediaryName(pair[0]));
            assertTrue(OmmcSortTargetProbe.is(Type.getObjectType(pair[1].replace('.','/')),pair[0]));
            assertTrue(OmmcSortTargetProbe.is(Type.getObjectType(pair[0].replace('.','/')),pair[0]));
        }
    }
    @Test void doesNotMistakePrimitivesOrUnknownTypesForMinecraftClasses(){
        assertNull(OmmcSortTargetProbe.intermediaryName("java.lang.String"));
        assertFalse(OmmcSortTargetProbe.is(Type.INT_TYPE,"net.minecraft.world.inventory.Slot"));
    }
}

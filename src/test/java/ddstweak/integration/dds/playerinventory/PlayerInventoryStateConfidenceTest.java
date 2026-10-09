package ddstweak.integration.dds.playerinventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryStateConfidenceTest {
    @Test void authorityOutranksLocalAndUnknown(){
        assertTrue(PlayerInventoryStateConfidence.AUTHORITATIVE.outranks(PlayerInventoryStateConfidence.LOCAL));
        assertTrue(PlayerInventoryStateConfidence.AUTHORITATIVE.outranks(PlayerInventoryStateConfidence.UNKNOWN));
        assertTrue(PlayerInventoryStateConfidence.LOCAL.outranks(PlayerInventoryStateConfidence.UNKNOWN));
        assertFalse(PlayerInventoryStateConfidence.LOCAL.outranks(PlayerInventoryStateConfidence.AUTHORITATIVE));
        assertFalse(PlayerInventoryStateConfidence.UNKNOWN.outranks(PlayerInventoryStateConfidence.LOCAL));
    }
}

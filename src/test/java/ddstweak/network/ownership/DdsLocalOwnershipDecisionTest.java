package ddstweak.network.ownership;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DdsLocalOwnershipDecisionTest {
    @Test void acceptsCodecHandoff(){assertEquals(DdsLocalOwnershipDecision.HANDOFF_DDS_PAYLOAD_CODEC,DdsLocalOwnershipDecision.fromMetadata(1,"tweak","DDS_PAYLOAD_CODEC"));}
    @Test void acceptsLegacyHandoff(){assertEquals(DdsLocalOwnershipDecision.HANDOFF_LEGACY_CUSTOM_PAYLOAD,DdsLocalOwnershipDecision.fromMetadata(1,"tweak","LEGACY_CUSTOM_PAYLOAD"));}
    @Test void acceptsReaderMapHandoff(){assertEquals(DdsLocalOwnershipDecision.HANDOFF_DDS_PAYLOAD_READER_MAP,DdsLocalOwnershipDecision.fromMetadata(1,"TWEAK","dds_payload_reader_map"));}
    @Test void rejectsUnknownContract(){assertEquals(DdsLocalOwnershipDecision.UNSUPPORTED_LOCAL_DDS,DdsLocalOwnershipDecision.fromMetadata(2,"tweak","DDS_PAYLOAD_CODEC"));}
    @Test void rejectsWrongOwner(){assertEquals(DdsLocalOwnershipDecision.UNSUPPORTED_LOCAL_DDS,DdsLocalOwnershipDecision.fromMetadata(1,"dds","DDS_PAYLOAD_CODEC"));}

    @Test
    void preCutoverHandoffOwnsFeaturesWithoutDoubleWireRegistration(){
        assertTrue(DdsLocalOwnershipDecision.HANDOFF_LEGACY_CUSTOM_PAYLOAD.tweakOwnsFeatureClient());
        assertTrue(DdsLocalOwnershipDecision.HANDOFF_DDS_PAYLOAD_READER_MAP.tweakOwnsFeatureClient());
        assertTrue(DdsLocalOwnershipDecision.HANDOFF_DDS_PAYLOAD_CODEC.tweakOwnsFeatureClient());

        assertFalse(DdsLocalOwnershipDecision.HANDOFF_LEGACY_CUSTOM_PAYLOAD.disableTweakCodecMixins());
        assertTrue(DdsLocalOwnershipDecision.HANDOFF_DDS_PAYLOAD_READER_MAP.disableTweakCodecMixins());
        assertTrue(DdsLocalOwnershipDecision.HANDOFF_DDS_PAYLOAD_CODEC.disableTweakCodecMixins());

        assertTrue(DdsLocalOwnershipDecision.LEGACY_COMPAT.disableTweakCodecMixins());
        assertFalse(DdsLocalOwnershipDecision.LEGACY_COMPAT.tweakOwnsFeatureClient());
        assertFalse(DdsLocalOwnershipDecision.TWEAK_STANDALONE.disableTweakCodecMixins());
    }
}

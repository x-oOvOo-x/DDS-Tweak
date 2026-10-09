package ddstweak.network;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class DdsClientStateTest {
    @Test void legacyHelloNeverAuthorizesInputTakeover(){DdsClientState s=new DdsClientState();s.updateHandshake(true,Set.of("qca_storage_click_c2s"),0);assertFalse(s.canTakeOverInput("qca_storage_click_c2s"));assertFalse(s.hasC2SAvailabilitySchema());}
    @Test void schemaOneAuthorizesAdvertisedInput(){DdsClientState s=new DdsClientState();s.updateHandshake(true,Set.of("qca_storage_click_c2s"),1);assertTrue(s.hasC2SAvailabilitySchema());assertTrue(s.canTakeOverInput("qca_storage_click_c2s"));assertFalse(s.canTakeOverInput("undo_c2s"));}
    @Test void resetClearsAvailabilitySchema(){DdsClientState s=new DdsClientState();s.updateHandshake(true,Set.of("undo_c2s"),1);s.resetProtocolState();assertFalse(s.hasC2SAvailabilitySchema());assertFalse(s.canTakeOverInput("undo_c2s"));}
}

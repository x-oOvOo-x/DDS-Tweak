package ddstweak.network;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class DdsClientStateTest {
    @Test void legacyHelloNeverAuthorizesInputTakeover(){DdsClientState s=new DdsClientState();s.updateHandshake(true,Set.of("qca_storage_click_c2s"),0);assertFalse(s.canTakeOverInput("qca_storage_click_c2s"));assertFalse(s.hasC2SAvailabilitySchema());}
    @Test void schemaOneAuthorizesAdvertisedInput(){DdsClientState s=new DdsClientState();s.updateHandshake(true,Set.of("qca_storage_click_c2s"),1);assertTrue(s.hasC2SAvailabilitySchema());assertTrue(s.canTakeOverInput("qca_storage_click_c2s"));assertFalse(s.canTakeOverInput("undo_c2s"));}
    @Test void resetClearsAvailabilitySchema(){DdsClientState s=new DdsClientState();s.updateHandshake(true,Set.of("undo_c2s"),1);s.resetProtocolState();assertFalse(s.hasC2SAvailabilitySchema());assertFalse(s.canTakeOverInput("undo_c2s"));}

    @Test void unansweredHelloUsesBoundedBackoff() {
        DdsClientState s=new DdsClientState();
        assertFalse(s.shouldRetryHello(10_000_000_000L)); // no probe before login

        s.markHelloSent(1_000_000_000L);
        assertFalse(s.shouldRetryHello(1_999_999_999L));
        assertTrue(s.shouldRetryHello(2_000_000_000L));

        s.markHelloSent(2_000_000_000L);
        assertFalse(s.shouldRetryHello(4_999_999_999L));
        assertTrue(s.shouldRetryHello(5_000_000_000L));

        s.markHelloSent(5_000_000_000L);
        assertFalse(s.shouldRetryHello(Long.MAX_VALUE)); // never poll forever
    }

    @Test void explicitDisabledHelloStopsRetriesButAllowsServerReenablePush() {
        DdsClientState s=new DdsClientState();
        s.markHelloSent(1_000_000_000L);
        s.updateHandshake(false,Set.of("undo_c2s"),0);
        assertFalse(s.isServerProtocolActive());
        assertFalse(s.shouldRetryHello(10_000_000_000L));
        assertFalse(s.doesServerSupport("undo_c2s"));

        s.updateHandshake(true,Set.of("undo_c2s"),1);
        assertTrue(s.isServerProtocolActive());
        assertTrue(s.doesServerSupport("undo_c2s"));
        assertTrue(s.canTakeOverInput("undo_c2s"));
        assertFalse(s.shouldRetryHello(10_000_000_000L));
    }

    @Test void successfulHelloStopsRetriesAndConnectionResetRestartsProbeBudget() {
        DdsClientState s=new DdsClientState();
        s.markHelloSent(1_000_000_000L);
        s.updateHandshake(true,Set.of("undo_c2s"),1);
        assertFalse(s.shouldRetryHello(100_000_000_000L));

        s.resetProtocolState();
        assertFalse(s.isServerProtocolActive());
        assertFalse(s.shouldRetryHello(100_000_000_000L));
        s.markHelloSent(101_000_000_000L);
        assertTrue(s.shouldRetryHello(102_000_000_000L));
    }
}

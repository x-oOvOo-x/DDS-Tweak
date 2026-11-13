package ddstweak.feature.carpetrules;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleAuthorityRefreshTest {
    @Test void ordinaryPollingKeepsTheExistingCadence() {
        long last=TimeUnit.SECONDS.toNanos(10);
        assertFalse(CarpetRuleAuthority.shouldRequest(39,false,last+TimeUnit.SECONDS.toNanos(30),last));
        assertTrue(CarpetRuleAuthority.shouldRequest(40,false,last+TimeUnit.SECONDS.toNanos(30),last));
    }

    @Test void postWriteRefreshWaitsOutCdaServerThrottle() {
        long last=TimeUnit.SECONDS.toNanos(10);
        assertFalse(CarpetRuleAuthority.shouldRequest(-559,true,
                last+TimeUnit.SECONDS.toNanos(5),last));
        assertTrue(CarpetRuleAuthority.shouldRequest(-559,true,
                last+TimeUnit.MILLISECONDS.toNanos(5500),last));
        assertFalse(CarpetRuleAuthority.shouldRequest(-559,false,
                last+TimeUnit.SECONDS.toNanos(6),last));
    }

    @Test void firstRequestDoesNotWaitForAnUnsentCooldown() {
        assertTrue(CarpetRuleAuthority.shouldRequest(40,false,100L,0L));
        assertTrue(CarpetRuleAuthority.shouldRequest(0,true,100L,0L));
    }
}

package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleOwnershipTest {
    public interface PublicRuleApi {
        String name();
        Object value();
        List<String> categories();
    }

    private static final class NonPublicRule implements PublicRuleApi {
        public String name(){return "ddsNetworkProtocol";}
        public Object value(){return true;}
        public List<String> categories(){return List.of("feature","client");}
    }

    @Test void nonPublicRuleImplementationsAreReadableThroughPublicCarpetApi() throws Exception {
        NonPublicRule rule=new NonPublicRule();
        assertEquals("ddsNetworkProtocol",CarpetRuleOwnership.invokeRule(rule,"name"));
        assertEquals(true,CarpetRuleOwnership.invokeRule(rule,"value"));
        assertEquals(List.of("feature","client"),CarpetRuleOwnership.invokeRule(rule,"categories"));
    }
}

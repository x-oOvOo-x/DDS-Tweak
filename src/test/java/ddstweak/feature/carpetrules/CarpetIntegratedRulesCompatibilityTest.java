package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetIntegratedRulesCompatibilityTest {
    private static final class LegacyParsedRule {
        public final String name;
        public final String defaultAsString;
        public final List<String> options;
        private final String value;

        LegacyParsedRule(String name,String value,String defaultValue,List<String> options){
            this.name=name;
            this.value=value;
            this.defaultAsString=defaultValue;
            this.options=options;
        }

        public String getAsString(){return value;}
    }

    private static final class LegacySettingsManager {
        private final String identifier;
        private final Collection<LegacyParsedRule> rules;

        LegacySettingsManager(String identifier,LegacyParsedRule... rules){
            this.identifier=identifier;
            this.rules=Arrays.asList(rules);
        }

        public Collection<LegacyParsedRule> getRules(){return rules;}
    }

    private static final class LegacyExtension {
        private final Object manager;
        LegacyExtension(Object manager){this.manager=manager;}
        public Object customSettingsManager(){return manager;}
    }

    private static final class LegacyCarpetServer {
        public static Object settingsManager;
        public static List<Object> extensions=Collections.emptyList();
    }

    @Test void readsLegacyParsedRuleFieldsAndFormattedValues() throws Exception {
        LegacySettingsManager manager=new LegacySettingsManager("carpet",
                new LegacyParsedRule("commandPlayer","ops","ops",
                        Arrays.asList("ops","true","false")));

        CarpetIntegratedRules.ManagerSnapshot snapshot=
                CarpetIntegratedRules.readManager(manager,null);

        assertEquals(1,snapshot.rules.size());
        CarpetRuleCatalog.RemoteRule rule=snapshot.rules.get(0);
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet","commandPlayer");
        assertEquals(key,rule.key());
        assertEquals("ops",rule.value());
        assertEquals("ops",snapshot.definitionDefaults.get(key));
        assertEquals(Arrays.asList("ops","true","false"),snapshot.suggestions.get(key));
    }

    @Test void enumeratesLegacyCoreAndExtensionSettingsManagers() throws Exception {
        LegacyCarpetServer.settingsManager=new LegacySettingsManager("carpet",
                new LegacyParsedRule("commandPlayer","ops","ops",Collections.emptyList()));
        LegacyCarpetServer.extensions=Collections.<Object>singletonList(
                new LegacyExtension(new LegacySettingsManager("carpet-extra",
                        new LegacyParsedRule("hopperCounters","false","false",
                                Collections.emptyList()))));

        List<Object> managers=CarpetIntegratedRules.registeredManagers(LegacyCarpetServer.class);

        assertEquals(2,managers.size());
        assertEquals("carpet",CarpetIntegratedRules.readManager(managers.get(0),null)
                .rules.get(0).key().manager());
        assertEquals("carpet-extra",CarpetIntegratedRules.readManager(managers.get(1),null)
                .rules.get(0).key().manager());
    }
}

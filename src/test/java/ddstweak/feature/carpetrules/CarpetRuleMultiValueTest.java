package ddstweak.feature.carpetrules;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleMultiValueTest {
    @Test void comboSelectionsSerializeInServerDefinedOrderAndHandleExclusiveSentinels(){
        CarpetRuleMultiValue combo=new CarpetRuleMultiValue(
                List.of("none","all","creeper","tnt","tnt_minecart"),"none","all");
        assertEquals("tnt",combo.toggle("none","tnt"));
        assertEquals("creeper,tnt",combo.toggle("tnt","creeper"));
        assertEquals("all",combo.toggle("creeper,tnt","tnt_minecart"));
        assertEquals("creeper,tnt",combo.toggle("all","tnt_minecart"));
        assertEquals("none",combo.toggle("creeper,tnt","none"));
        assertEquals("all",combo.toggle("none","all"));
        assertEquals(Set.of("creeper","tnt","tnt_minecart"),combo.parse("all"));
        assertTrue(combo.checked("all","creeper"));
        assertTrue(combo.checked("none","none"));
        assertEquals(0,combo.selectedCount("none"));
        assertEquals(3,combo.selectedCount("all"));
    }

    @Test void incompatibleServerValuesMustNotBeRewrittenAsNone(){
        CarpetRuleMultiValue combo=new CarpetRuleMultiValue(
                List.of("false","all","obsidian","ores"),"false","all");
        assertNull(combo.parse("invalid"));
        assertNull(combo.parse("obsidian,"));
        assertNull(combo.parse("obsidian,obsidian"));
        assertNull(combo.parse("false,ores"));
        assertNull(combo.toggle("invalid","obsidian"));
        assertEquals("ores",combo.toggle("false","ores"));
        assertEquals("all",combo.toggle("ores","obsidian"));
        assertEquals("false",combo.toggle("ores","ores"));
        CarpetRuleMultiValue three=new CarpetRuleMultiValue(
                List.of("false","all","obsidian","ores","stone"),"false","all");
        assertEquals("obsidian,ores",three.toggle("ores","obsidian"));
        assertEquals("all",three.toggle("obsidian,ores","stone"));
    }

    @Test void onlyAuditedVerifiedDdsRulesExposeCombinationSelection(){
        List<String> explosion=List.of("none","all","creeper","tnt","tnt_minecart");
        CarpetRuleMultiValue noDamage=CarpetRuleMultiValue.forVerifiedDdsRule(
                "carpet-dds-addition","explosionNoBlockDamage","carpet-dds-addition",explosion);
        assertNotNull(noDamage);
        assertEquals("none",noDamage.empty());
        assertEquals("all",noDamage.all());
        assertEquals(List.of("creeper","tnt","tnt_minecart"),noDamage.options());

        CarpetRuleMultiValue passive=CarpetRuleMultiValue.forVerifiedDdsRule(
                "carpet-dds-addition","naturalPassiveSpawning","carpet-dds-addition",
                List.of("none","all","animals","monsters"));
        assertNotNull(passive);
        assertEquals("none",passive.empty());

        CarpetRuleMultiValue mining=CarpetRuleMultiValue.forVerifiedDdsRule(
                "carpet-dds-addition","easyMining","carpet-dds-addition",
                List.of("false","all","ores","stone"));
        assertNotNull(mining);
        assertEquals("false",mining.empty());

        assertNull(CarpetRuleMultiValue.forVerifiedDdsRule(
                "carpet-dds-addition","explosionNoBlockDamage","other-mod",explosion));
        assertNull(CarpetRuleMultiValue.forVerifiedDdsRule(
                "other-manager","explosionNoBlockDamage","carpet-dds-addition",explosion));
        assertNull(CarpetRuleMultiValue.forVerifiedDdsRule(
                "carpet-dds-addition","unreviewedRule","carpet-dds-addition",explosion));
        assertNull(CarpetRuleMultiValue.forVerifiedDdsRule(
                "carpet-dds-addition","easyMining","carpet-dds-addition",
                List.of("false","ores","stone")));
    }

    @Test void rejectsInconsistentMetadata(){
        assertThrows(IllegalArgumentException.class,
                ()->new CarpetRuleMultiValue(List.of("all","tnt"),"none","all"));
        assertThrows(IllegalArgumentException.class,
                ()->new CarpetRuleMultiValue(List.of("none","all","tnt","tnt"),"none","all"));
    }
}

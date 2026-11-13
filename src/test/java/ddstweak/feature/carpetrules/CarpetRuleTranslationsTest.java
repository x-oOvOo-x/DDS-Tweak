package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleTranslationsTest {
    @Test void selectedLanguageUsesMappedMinecraftCalls() throws Exception {
        String source=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleTranslations.java"));
        assertTrue(source.contains("Minecraft.getInstance()"));
        assertTrue(source.contains("getLanguageManager().getSelected()"));
        assertTrue(source.contains("Object selected=net.minecraft.client.Minecraft.getInstance()"));
        assertTrue(source.contains("static String languageCode(Object selected)"));
        assertTrue(source.contains("method.getParameterCount()!=0"));
        assertFalse(source.contains("getSelected().getCode()"));
        assertFalse(source.contains("//#if MC >= 11904"));
        assertFalse(source.contains("getMethod(\"getLanguageManager\")"));
        assertFalse(source.contains("getMethod(\"getSelected\")"));
    }

    @Test void mc11605ReadsSelectedClientLanguageWithoutUnmappedReflection() throws Exception {
        String source=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleTranslations.java"));
        assertTrue(source.contains("//#if MC >= 11600 && MC < 11700"));
        assertTrue(source.contains("Class.forName(\"carpet.CarpetSettings\")"));
        assertTrue(source.contains(".getField(\"language\").get(null)"));
        assertTrue(source.contains("if(code.matches(\"[a-z]{2}_[a-z]{2}\"))return code"),
                "CDA and Carpet must follow Carpet's effective language in 1.16.5");
        assertTrue(source.contains(
                "validLanguage(net.minecraft.client.Minecraft.getInstance().options.languageCode)"),
                "When Carpet language is none, fall back to the client language");
        assertTrue(source.contains("//#else"),
                "Other versions retain the existing selected-language API");
    }

    private static final class LegacyLanguage {
        public String getCode(){return "zh_cn";}
    }

    @Test void acceptsBothStringAndLegacyLanguageObject(){
        assertEquals("zh_cn",CarpetRuleTranslations.languageCode("zh_cn"));
        assertEquals("zh_cn",CarpetRuleTranslations.languageCode(new LegacyLanguage()));
        assertEquals("en_us",CarpetRuleTranslations.languageCode(null));
        assertEquals("en_us",CarpetRuleTranslations.languageCode("invalid"));
    }

    @Test void simplifiedChineseCoversEveryCarpetManagerUiTranslation() throws Exception {
        com.google.gson.JsonObject zh=new com.google.gson.JsonParser().parse(
                java.nio.file.Files.readString(java.nio.file.Path.of(
                        "src/main/resources/assets/dds-tweak/lang/zh_cn.json"))).getAsJsonObject();
        com.google.gson.JsonObject en=new com.google.gson.JsonParser().parse(
                java.nio.file.Files.readString(java.nio.file.Path.of(
                        "src/main/resources/assets/dds-tweak/lang/en_us.json"))).getAsJsonObject();
        java.util.Set<String> chinese=new java.util.HashSet<>(),english=new java.util.HashSet<>();
        for(java.util.Map.Entry<String,com.google.gson.JsonElement> entry:zh.entrySet())
            if(entry.getKey().startsWith("dds-tweak.carpet_rules."))chinese.add(entry.getKey());
        for(java.util.Map.Entry<String,com.google.gson.JsonElement> entry:en.entrySet())
            if(entry.getKey().startsWith("dds-tweak.carpet_rules."))english.add(entry.getKey());
        assertEquals(english,chinese);
        for(String key:english){
            assertTrue(zh.get(key).isJsonPrimitive(),key);
            assertFalse(zh.get(key).getAsString().isEmpty(),key);
        }
    }

    public static final class LegacyParsedRule {
        public final String name;
        public final java.util.List<String> categories;
        public LegacyParsedRule(String name,String... categories){
            this.name=name;
            this.categories=java.util.Arrays.asList(categories);
        }
    }

    public static class LegacyManager {
        public String getIdentifier(){return "carpet";}
        public java.util.Collection<LegacyParsedRule> getRules(){
            return java.util.Collections.singletonList(
                    new LegacyParsedRule("commandPlayer","command"));
        }
    }

    @Test void legacy11605ManagerProducesChineseRuleMetadata(){
        CarpetRuleMetadata metadata=new CarpetRuleMetadata();
        Map<String,String> translations=new HashMap<>();
        translations.put("carpet.rule.commandPlayer.name","玩家控制");
        translations.put("carpet.rule.commandPlayer.desc","允许使用玩家命令");
        CarpetLocalMetadata.append(metadata,new LegacyManager(),translations);
        CarpetRuleMetadata.Description description=metadata.find(
                new CarpetRuleCatalog.RuleKey("carpet","commandPlayer"));
        assertNotNull(description,"Legacy ParsedRule fields and getRules API must resolve");
        assertEquals("玩家控制",description.displayName());
        assertEquals("允许使用玩家命令",description.detail());
        assertTrue(description.categories().contains("command"));
    }

    public static final class LegacyCdaManager extends LegacyManager {
        @Override public String getIdentifier(){return "carpet-dds-addition";}
        @Override public java.util.Collection<LegacyParsedRule> getRules(){
            return java.util.Collections.singletonList(
                    new LegacyParsedRule("explosionNoBlockDamage","feature"));
        }
    }

    @Test void cda11605TranslationsUseTheirOwnManagerNamespace(){
        CarpetRuleMetadata metadata=new CarpetRuleMetadata();
        Map<String,String> translations=new HashMap<>();
        translations.put(CarpetRuleTranslations.qualifiedKey(
                "rule.explosionNoBlockDamage.name","carpet-dds-addition"),"爆炸不破坏方块");
        translations.put(CarpetRuleTranslations.qualifiedKey(
                "rule.explosionNoBlockDamage.desc","carpet-dds-addition"),"禁止指定爆炸破坏方块");
        CarpetLocalMetadata.append(metadata,new LegacyCdaManager(),translations);
        CarpetRuleMetadata.Description d=metadata.find(new CarpetRuleCatalog.RuleKey(
                "carpet-dds-addition","explosionNoBlockDamage"));
        assertNotNull(d);
        assertEquals("爆炸不破坏方块",d.displayName());
        assertEquals("禁止指定爆炸破坏方块",d.detail());
    }

    @Test void metadataRecoversAfterWorldJoinWithoutDroppingEarlierTranslations() throws Exception {
        String screen=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerScreen.java"));
        assertTrue(screen.contains("refreshLocalMetadata();"));
        assertTrue(screen.contains("TimeUnit.SECONDS.toNanos(5)"));
        assertTrue(screen.contains("metadataCoverage(refreshed,rules)>=metadataCoverage(metadata,rules)"));
        assertTrue(screen.contains("!language.equals(metadataLanguage)"),
                "An explicit Carpet language change must replace the previous translation map");
        assertTrue(screen.contains("CarpetLocalMetadata.discover()"));
    }

    @Test void legacyCarpetExtensionManagerApiRemainsDiscoverable() throws Exception {
        String src=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetLocalMetadata.java"));
        assertTrue(src.contains("extensionSettingsManager"));
        assertTrue(src.contains("customSettingsManager"));
        assertTrue(src.contains("Class.forName(\"carpet.CarpetExtension\")"));
    }

    @Test void oldCarpetLanguageKeysAreQualifiedToTheirActualManager(){
        assertEquals("carpet.rule.commandPlayer.name",
                CarpetRuleTranslations.qualifiedKey("rule.commandPlayer.name","carpet"));
        assertEquals("carpet-dds-addition.rule.easyMining.desc",
                CarpetRuleTranslations.qualifiedKey("rule.easyMining.desc","carpet-dds-addition"));
        assertEquals("carpet-dds-addition.rule.easyMining.name",
                CarpetRuleTranslations.qualifiedKey(
                        "carpet-dds-addition.rule.easyMining.name","carpet-dds-addition"));
        assertNull(CarpetRuleTranslations.qualifiedKey("rule.unknown.name",null),
                "Do not attribute an unqualified addon key to the wrong manager");
    }

    @Test void legacyParsedRuleAndManagerApisRemainSupported() throws Exception {
        String local=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetLocalMetadata.java"));
        assertTrue(local.contains("getIdentifier"));
        assertTrue(local.contains("getRules"));
        assertTrue(local.contains("rule.getClass().getField(name).get(rule)"));
        String translations=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleTranslations.java"));
        assertTrue(translations.contains("String manager=extensionManagerId(api,ext)"));
        assertTrue(translations.contains("qualifiedKey(e.getKey(),\"carpet\")"));
    }

    @Test void preservesCdaManagerNamespaceForExtensionTranslations(){
        Map<String,String> values=new HashMap<>();
        values.put("carpet-dds-addition.rule.autoRestock.name","自动补货");
        values.put("carpet-dds-addition.rule.autoRestock.desc","为玩家自动补货");
        assertEquals("自动补货",CarpetRuleTranslations.name(values,"carpet-dds-addition","autoRestock"));
        assertEquals("为玩家自动补货",
                CarpetRuleTranslations.description(values,"carpet-dds-addition","autoRestock"));
    }

    @Test void neverGuessesMissingTranslationOrOtherManagerOwnership(){
        Map<String,String> values=new HashMap<>();
        values.put("carpet.rule.commandPlayer.name","玩家控制");
        values.put("carpet.rule.commandPlayer.desc","允许使用玩家控制命令");
        assertEquals("玩家控制",CarpetRuleTranslations.name(values,"carpet","commandPlayer"));
        assertEquals("commandPlayer",CarpetRuleTranslations.name(values,"cca","commandPlayer"));
        assertEquals("",CarpetRuleTranslations.description(values,"cca","commandPlayer"));
        assertEquals("允许使用玩家控制命令",
                CarpetRuleTranslations.description(values,"carpet","commandPlayer"));
    }
}

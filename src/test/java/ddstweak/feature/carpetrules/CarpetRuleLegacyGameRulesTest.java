package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleLegacyGameRulesTest {
    private static String source(String file) throws Exception {
        return Files.readString(Path.of(
                "src/main/java/ddstweak/feature/carpetrules/"+file)).replace("\r\n","\n");
    }

    @Test void preRegistrySingleplayerGameRulesUseActualServerValues() throws Exception {
        String rules=source("VanillaGameRules.java");
        assertTrue(rules.contains("//#elseif MC < 260000"));
        assertTrue(rules.contains("client.getSingleplayerServer()!=null"));
        assertTrue(rules.contains("server.execute(()->{"));
        assertTrue(rules.contains("Map<String,Entry> snapshot=readIntegrated(server)"));
        assertTrue(rules.contains("client.execute(()->acceptIntegrated(client,current,server,snapshot))"));
        assertTrue(rules.contains("server.getGameRules().createTag()"));
        assertTrue(rules.contains("//#if MC >= 12105"));
        assertTrue(rules.contains("java.util.Set<String> keys=values.keySet()"));
        assertTrue(rules.contains("java.util.Set<String> keys=values.getAllKeys()"));
        assertTrue(rules.contains("client.getConnection()!=source"));
        assertTrue(rules.contains("client.getSingleplayerServer()!=server"));
        assertTrue(rules.contains("if(snapshot==null"));
    }

    @Test void registeredGameRulesIn12111HaveTheirOwnIntegratedPath() throws Exception {
        String rules=source("VanillaGameRules.java");
        assertTrue(rules.contains("//#if MC < 12111"));
        assertTrue(rules.contains("//#elseif MC < 260000"));
        assertTrue(rules.contains("GameRules rules=server.overworld().getGameRules()"));
        assertTrue(rules.contains("import net.minecraft.world.level.gamerules.GameRules;"),
                "The root 1.21.11 build must import the actual registry API, not a commented alternative");
        assertTrue(rules.contains("private static Map<String,Entry> readIntegrated(MinecraftServer server){"),
                "The root 1.21.11 build must contain the actual world reader");
        assertTrue(rules.contains("rules.availableRules().toArray(GameRule[]::new)"));
        assertTrue(rules.contains("rule.getIdentifier().toString()"));
        assertTrue(rules.contains("rule.serialize(rules.get(rule))"));
        assertTrue(rules.contains("rule.serialize(rule.defaultValue())"));
        assertTrue(rules.contains("rule.valueClass()==Boolean.class"));
        assertTrue(rules.contains("((GameRule<?>)entry.rule).deserialize(value)"));
    }

    @Test void localReadWorksWithoutCheatsButWritesStillRequirePermission() throws Exception {
        String rules=source("VanillaGameRules.java");
        int request=rules.indexOf("public static synchronized void request(Minecraft client)");
        int poll=rules.indexOf("public static synchronized void poll(Minecraft client)",request);
        assertTrue(request>=0&&poll>request);
        String localRequest=rules.substring(request,poll);
        assertTrue(localRequest.contains("//#if MC >= 260000\n        //$$ if(!permitted(client))return;"),
                "26.x network request retains its existing permission gate");
        assertTrue(localRequest.contains("if(client.getSingleplayerServer()==null\n                &&!VanillaGameRuleRemoteSync.available())return"),
                "Old versions can read their own integrated-server rules even without cheats");
        int write=rules.indexOf("public static synchronized boolean submit(");
        assertTrue(write>poll);
        assertTrue(rules.substring(write).contains("if(!permitted(client)||!received"),
                "Read-only access must never enable unauthorized gamerule writes");
        String renderer=source("CarpetRuleManagerRenderer.java");
        int empty=renderer.indexOf("if(rules.isEmpty()){");
        int draw=renderer.indexOf("owner.draw(g,empty,",empty);
        assertTrue(empty>=0&&draw>empty);
        String emptyState=renderer.substring(empty,draw);
        assertFalse(emptyState.contains("VanillaGameRules.permitted("),
                "Read-only listings must show loading/no-results, not a write-permission error");
        assertTrue(emptyState.contains("!VanillaGameRules.received()"));
        String screen=source("CarpetRuleManagerScreen.java");
        int open=screen.indexOf("void openVanillaGameRules(){");
        int next=screen.indexOf("boolean vanillaMode()",open);
        assertTrue(open>=0&&next>open);
        String opening=screen.substring(open,next);
        assertFalse(opening.contains("else if(!VanillaGameRules.permitted(minecraft))"),
                "Opening a read-only view must not report an edit-permission denial");
    }

    @Test void registeredGameRuleFailuresDoNotEmptyTheEntire12111List() throws Exception {
        String rules=source("VanillaGameRules.java");
        int registry=rules.indexOf("private static Map<String,Entry> readIntegrated(MinecraftServer server)",
                rules.indexOf("//#elseif MC < 260000"));
        int accept=rules.indexOf("private static synchronized void acceptIntegrated(",registry);
        assertTrue(registry>=0&&accept>registry);
        String block=rules.substring(registry,accept);
        assertTrue(block.contains("try{\n                    Entry entry=registeredEntry(rules,rule)"));
        assertTrue(block.contains("catch(RuntimeException ex){"));
        assertTrue(block.contains("skipped++; // one modded/broken rule must not hide all vanilla rules"));
        assertTrue(block.contains("return snapshot.isEmpty()?null:snapshot"));
        assertFalse(block.contains("if(entry==null||snapshot.putIfAbsent(entry.id,entry)!=null\n"
                +"    //$$                     ||snapshot.size()>512)return null"),
                "One malformed modded rule must not cause an all-or-nothing snapshot");
        assertTrue(block.contains("for(GameRule<?> rule:BuiltInRegistries.GAME_RULE)"),
                "If GameRules.availableRules returns empty, read registered definitions");
        assertTrue(block.contains("Entry entry=registeredEntry(rules,rule)"),
                "Registry fallback must still query the authoritative world's values");
        assertTrue(block.contains("LOGGER.warning("),
                "A failed 1.21.11 snapshot must be diagnosable from logs");

        String renderer=source("CarpetRuleManagerRenderer.java");
        assertTrue(renderer.contains("VanillaGameRules.unavailable()"),
                "Repeated failed reads must not leave the UI waiting forever");
        assertTrue(rules.contains("readFailures++;"));
        assertTrue(rules.contains("readFailures=0;"));
        assertTrue(rules.contains("readFailures>=3"));
    }

    @Test void gameruleWritesRemainPermissionAndValidationGated() throws Exception {
        String rules=source("VanillaGameRules.java");
        assertTrue(rules.contains(".getChild(\"gamerule\")!=null"));
        assertTrue(rules.contains("Integer.parseInt(value)"));
        assertTrue(rules.contains("if(!permitted(client)||!received"));
        assertTrue(rules.contains("||connection!=client.getConnection())return false"));
        assertTrue(rules.contains("MinecraftCompat.sendCommandWithoutHistory(client,"));
        assertTrue(rules.contains("if(sent)lastRequest=0"));
        assertFalse(rules.contains("CATALOG.replace"),
                "Minecraft gamerules are not Carpet rules and must not contaminate the catalog");
    }

    @Test void historyRestoreIsSelfPaintedAndKeepsItsInputWidget() throws Exception {
        String history=source("CarpetRuleHistoryScreen.java");
        assertTrue(history.contains("//#if MC >= 11600 && MC < 11700\n        addFilterWidget(restoreButton);"),
                "Only 1.16.5 uses the verified wide-button background workaround");
        assertTrue(history.contains("//#else\n        //$$ addCompat(restoreButton);"),
                "1.14/1.15 keep their working native button rendering");
        assertTrue(history.contains("if(restoreButton.visible){"));
        assertTrue(history.contains("CarpetRuleUiStyle.toolbarButton(g,font,"));
        assertTrue(history.contains("restoreButton.active,false)"));
        assertTrue(history.contains("restoreButton.active=restoreButton.visible&&canRestore(op)"),
                "Painted buttons must preserve actual permission gating");
    }
}

package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class VanillaGameRuleRemoteSyncTest {
    private static String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/ddstweak/feature/carpetrules/"+name))
                .replace("\r\n","\n");
    }

    @Test void remoteDataRejectsCommandInjectionAndInvalidTypes(){
        assertTrue(VanillaGameRuleRemoteSync.validId("minecraft:keep_inventory"));
        assertTrue(VanillaGameRuleRemoteSync.validId("doDaylightCycle"));
        assertFalse(VanillaGameRuleRemoteSync.validId("keep_inventory\n/gamerule"));
        assertFalse(VanillaGameRuleRemoteSync.validId("../config"));
        assertTrue(VanillaGameRuleRemoteSync.validValue("true"));
        assertTrue(VanillaGameRuleRemoteSync.validValue("-1"));
        assertFalse(VanillaGameRuleRemoteSync.validValue("maybe"));
        assertFalse(VanillaGameRuleRemoteSync.validValue("2\n/gamerule"));
        assertTrue(VanillaGameRuleRemoteSync.validTypedValue("false",true));
        assertFalse(VanillaGameRuleRemoteSync.validTypedValue("1",true));
        assertTrue(VanillaGameRuleRemoteSync.validTypedValue("-8",false));
        assertFalse(VanillaGameRuleRemoteSync.validTypedValue("true",false));
    }

    @Test void remoteEntriesHaveConservativeValueValidation(){
        VanillaGameRules.Entry bool=new VanillaGameRules.Entry(
                "minecraft:keep_inventory","gamerule.keep_inventory","",
                "player","false","false",true,null);
        VanillaGameRules.Entry number=new VanillaGameRules.Entry(
                "minecraft:random_tick_speed","gamerule.random_tick_speed","",
                "world","3","3",false,null);
        assertTrue(VanillaGameRules.valid(bool,"true"));
        assertFalse(VanillaGameRules.valid(bool,"1"));
        assertTrue(VanillaGameRules.valid(number,"0"));
        assertFalse(VanillaGameRules.valid(number,"two"));
        // The server is still authoritative about numeric range and permission.
    }

    @Test void onlyCompleteAuthoritativeResponsesReachTheSharedBrowser() throws Exception {
        String remote=source("VanillaGameRuleRemoteSync.java");
        String network=source("CarpetRuleClientNetwork.java");
        String rules=source("VanillaGameRules.java");
        assertTrue(network.contains("VanillaGameRuleRemoteSync.initialize();"));
        assertTrue(remote.contains("DdsClientNetwork.registerC2S(REQUEST);"));
        assertTrue(remote.contains("DdsClientNetwork.registerOrderedS2CHandler(PAGE"));
        assertTrue(remote.contains("if(source!=awaiting||source!=client.getConnection()"));
        assertTrue(remote.contains("client.getSingleplayerServer()!=null"));
        assertTrue(remote.contains("nonce=nonce==Integer.MAX_VALUE?1:nonce+1"));
        assertTrue(remote.contains("pages.size()!=expectedPages"));
        assertTrue(remote.contains("completed.size()!=expectedTotal"));
        assertTrue(remote.contains("VanillaGameRules.remoteReadFailed(source)"),
                "Malformed pages must not leave the remote rule view in eternal loading");
        assertTrue(remote.contains("VanillaGameRules.remoteReadFailed(awaiting)"),
                "A missing server response must become a diagnosable failure");
        assertTrue(remote.contains("VanillaGameRules.acceptRemote(source,Collections.unmodifiableMap(completed))"));
        assertTrue(rules.contains("||VanillaGameRuleRemoteSync.available()"));
        assertTrue(rules.contains("VanillaGameRuleRemoteSync.request(current)"));
        assertTrue(rules.contains("if(!permitted(client)||!received"));
        assertTrue(rules.contains("if(++readFailures>=3)"),
                "Abandoned remote snapshots cannot remain visible forever");
        assertTrue(rules.contains("if(client.getSingleplayerServer()==null&&!VanillaGameRuleRemoteSync.available())"),
                "A lost server capability must remove stale remote values");
        assertTrue(rules.contains("entries=Collections.emptyMap();"));
        assertTrue(rules.contains("if(live==null||!live.value.equals(expected)"));
        assertFalse(remote.contains("sendCommandWithoutHistory"));
    }
}

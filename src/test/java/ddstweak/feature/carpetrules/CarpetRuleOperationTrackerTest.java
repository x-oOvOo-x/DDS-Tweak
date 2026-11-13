package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleOperationTrackerTest {
    private static final CarpetRuleCatalog.RuleKey KEY=new CarpetRuleCatalog.RuleKey("carpet","commandPlayer");

    private static CarpetRuleCatalog catalog(){
        CarpetRuleCatalog c=new CarpetRuleCatalog();
        Object connection=new Object();
        c.connect(connection);
        assertTrue(c.replace(connection,Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(KEY,"ops",true))));
        return c;
    }

    @Test void temporaryChangeRequiresAckAndMatchingSync(){
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        assertNotNull(op);
        t.acknowledge(op.id(),1,false,c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC,op.status());
        t.observe(KEY,"true",c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.VERIFIED,op.status());
    }

    @Test void positiveCarpetReturnWithoutActualRuleSyncIsNotVerification(){
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"invalid",1000);
        t.acknowledge(op.id(),1,false,c.generation());
        t.expire(17000);
        assertEquals(CarpetRuleOperationTracker.Status.UNCONFIRMED,op.status());
    }

    @Test void persistenceAckIsNotPersistedVerification(){
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.SET_DEFAULT,"true",1000);
        t.acknowledge(op.id(),1,false,c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.COMMAND_ACKNOWLEDGED,op.status());
    }

    @Test void removingDefaultAcknowledgesCommandWithoutRemoteFileVerification() {
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.REMOVE_DEFAULT,null,1000);
        assertNotNull(op);
        tracker.acknowledge(op.id(),1,false,catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.COMMAND_ACKNOWLEDGED,op.status());
        assertFalse(tracker.hasPending(1300));
    }

    @Test void integratedSaveDefaultRequiresAnExactKnownDiskValue(){
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.SET_DEFAULT,"true",1000);
        assertNotNull(op);
        tracker.acknowledge(op.id(),1,false,catalog.generation(),true);
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_PERSISTENCE,op.status());
        assertTrue(tracker.hasPending(1200));

        tracker.observePersistence(Collections.emptyMap(),Collections.emptySet(),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_PERSISTENCE,op.status());

        tracker.observePersistence(Collections.singletonMap(KEY,"true"),
                Collections.singleton("carpet"),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.PERSISTED,op.status());
        assertEquals("true",op.persistedValue());
        assertFalse(tracker.hasPending(1300));
    }

    @Test void integratedPersistenceMismatchReportsTheActualDiskValue(){
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.SET_DEFAULT,"true",1000);
        tracker.acknowledge(op.id(),1,false,catalog.generation(),true);
        tracker.observePersistence(Collections.singletonMap(KEY,"false"),
                Collections.singleton("carpet"),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.PERSISTENCE_MISMATCH,op.status());
        assertEquals("false",op.persistedValue());
    }

    @Test void integratedRemoveDefaultVerifiesThatTheOverrideIsAbsent(){
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.REMOVE_DEFAULT,null,1000);
        tracker.acknowledge(op.id(),1,false,catalog.generation(),true);
        tracker.observePersistence(Collections.emptyMap(),
                Collections.singleton("carpet"),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.PERSISTED,op.status());
        assertNull(op.persistedValue());
    }

    @Test void integratedPersistenceVerificationTimesOutWithoutClaimingFailure(){
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.SET_DEFAULT,"true",1000);
        tracker.acknowledge(op.id(),1,false,catalog.generation(),true);
        assertTrue(tracker.hasPending(2000));
        tracker.expire(16001);
        assertEquals(CarpetRuleOperationTracker.Status.PERSISTENCE_UNCONFIRMED,op.status());
        assertFalse(tracker.hasPending(16002));
    }

    @Test void legacyLocalTemporaryWriteWaitsForActualRuleSnapshotWithoutAck(){
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        assertNotNull(op);
        tracker.awaitLocalVerification(op.id(),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC,op.status());
        assertTrue(tracker.hasPending(1100));
        tracker.observe(KEY,"ops",catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC,op.status());
        tracker.observe(KEY,"true",catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.VERIFIED,op.status());
        assertFalse(tracker.hasPending(1200));
    }

    @Test void legacyLocalPersistenceRequiresActualDiskObservationWithoutAck(){
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.SET_DEFAULT,"true",1000);
        tracker.awaitLocalVerification(op.id(),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_PERSISTENCE,op.status());
        tracker.observePersistence(Collections.emptyMap(),Collections.emptySet(),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_PERSISTENCE,op.status());
        tracker.observePersistence(Collections.singletonMap(KEY,"true"),
                Collections.singleton("carpet"),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.PERSISTED,op.status());
    }

    @Test void legacyLocalUnobservedCommandNeverClaimsSuccess(){
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        tracker.awaitLocalVerification(op.id(),catalog.generation()+1);
        assertEquals(CarpetRuleOperationTracker.Status.SENT,op.status());
        tracker.awaitLocalVerification(op.id(),catalog.generation());
        tracker.expire(17000);
        assertEquals(CarpetRuleOperationTracker.Status.UNCONFIRMED,op.status());
    }

    @Test void legacyLocalRemoveDefaultRequiresMissingOverride(){
        CarpetRuleCatalog catalog=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=tracker.begin(catalog,KEY,
                CarpetRuleCommands.Mode.REMOVE_DEFAULT,null,1000);
        tracker.awaitLocalVerification(op.id(),catalog.generation());
        tracker.observePersistence(Collections.singletonMap(KEY,"ops"),
                Collections.singleton("carpet"),catalog.generation());
        assertEquals(CarpetRuleOperationTracker.Status.PERSISTENCE_MISMATCH,op.status());
    }

    @Test void mc114SingleplayerUsesPlayerCommandAndDoesNotExpectCarpetAck() throws Exception {
        String network=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleClientNetwork.java"));
        assertTrue(network.contains("//#if MC < 11500"));
        assertTrue(network.contains("if(NATIVE_CARPET&&Minecraft.getInstance().getSingleplayerServer()!=null)"));
        assertTrue(network.contains("MinecraftCompat.sendCommandWithoutHistory(Minecraft.getInstance(),command)"));
        assertTrue(network.contains("OPERATIONS.awaitLocalVerification(op.id(),CATALOG.generation())"));
        assertTrue(network.contains("CarpetIntegratedRules.invalidatePersistedDefaults()"));
        // The ordinary remote/newer Carpet path still uses the native command packet.
        assertTrue(network.contains("CarpetRuleWire.send(connection,packet)"));
    }

    @Test void mc12108PersistentWritesUseServerAuthorityRatherThanClientSubcommandCache() throws Exception {
        String network=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleClientNetwork.java"));
        int fallback=network.indexOf("//#if MC >= 12108 && MC < 12109");
        int tree=network.indexOf("com.mojang.brigadier.tree.CommandNode<?> root=",fallback);
        assertTrue(fallback>=0&&tree>fallback);
        String branch=network.substring(fallback,tree);
        assertTrue(branch.contains("if(mode!=CarpetRuleCommands.Mode.TEMPORARY)return true;"));
        // Server-side native Carpet validates the player's real permission and lock.
        assertTrue(network.contains("remote==null || !remote.managerExplicit()"));
        assertTrue(network.contains("OPERATIONS.hasPending(System.currentTimeMillis())"));
        assertTrue(network.contains("CarpetRuleWire.send(connection,packet)"));
        // Do not change regular temporary commands or other Minecraft targets.
        assertTrue(network.contains("return root.getChild(\"setDefault\")!=null;"));
        assertTrue(network.contains("return root.getChild(\"removeDefault\")!=null;"));
        assertTrue(network.contains("if (root==null) return false;"));
    }

    @Test void zeroOrMissingResultDoesNotBounceAnInFlightTemporaryToggle() {
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        t.acknowledge(op.id(),0,false,c.generation(),false);
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC,op.status());
        assertTrue(t.hasPending(1100));
        t.observe(KEY,"ops",c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC,op.status());
        t.observe(KEY,"true",c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.VERIFIED,op.status());
        assertFalse(t.hasPending(1200));
    }

    @Test void ruleSyncCanPrecedeAnAmbiguousZeroReturn() {
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        t.observe(KEY,"true",c.generation());
        t.acknowledge(op.id(),0,false,c.generation(),false);
        assertEquals(CarpetRuleOperationTracker.Status.VERIFIED,op.status());
    }

    @Test void zeroOrMissingResultNeedsSavedConfigProofForPersistentChanges() {
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.SET_DEFAULT,"true",1000);
        t.acknowledge(op.id(),0,false,c.generation(),true);
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_PERSISTENCE,op.status());
        assertTrue(t.hasPending(1100));
        t.observePersistence(Collections.singletonMap(KEY,"true"),
                Collections.singleton("carpet"),c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.PERSISTED,op.status());
    }

    @Test void remoteZeroOrMissingResultNeverClaimsPersistentSuccess() {
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.SET_DEFAULT,"true",1000);
        t.acknowledge(op.id(),0,false,c.generation(),false);
        assertEquals(CarpetRuleOperationTracker.Status.UNCONFIRMED,op.status());
    }

    @Test void explicitErrorRejectsButZeroWithoutErrorAwaitsVerification() {
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation error=t.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        t.acknowledge(error.id(),0,true,c.generation(),false);
        assertEquals(CarpetRuleOperationTracker.Status.REJECTED,error.status());
        CarpetRuleOperationTracker.Operation ordinary=t.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"true",1100);
        t.acknowledge(ordinary.id(),0,false,c.generation(),false);
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC,ordinary.status());
    }

    @Test void carpetReplyFieldsAndUncertainResultsWorkAcrossVersions() throws Exception {
        String network=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleClientNetwork.java"))
                .replace("\r\n","\n");
        int reply=network.indexOf("Tag reply=packet.get(\"clientCommand\")");
        int rules=network.indexOf("Tag tag = packet.get(\"Rules\")",reply);
        assertTrue(reply>=0&&rules>reply);
        String handler=network.substring(reply,rules);
        assertTrue(handler.contains("response.contains(\"return\")"));
        assertTrue(handler.contains("MinecraftCompat.intOr(response,\"return\",0)"));
        assertTrue(handler.contains("MinecraftCompat.intOr(response,\"code\",0)"));
        assertTrue(handler.contains("response.contains(\"error\")"));
        assertTrue(handler.contains("&& !hasError&&returnCode>=0")||
                handler.contains("&&!hasError&&returnCode>=0"));
        assertTrue(handler.contains("verifyPersistence);"));
        assertFalse(handler.contains("//#if MC >= 12108 && MC < 12109"),
                "Ambiguous replies must not be handled only on Minecraft 1.21.8");
    }

    @Test void negativeNumericReplyIsExplicitRejection() {
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        t.acknowledge(op.id(),-1,false,c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.REJECTED,op.status());
    }

    @Test void temporarySwitchNeverFallsBackWhileWaitingForAuthoritativeSync() {
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,
                CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        assertEquals(CarpetRuleOperationTracker.Status.SENT,op.status());
        t.acknowledge(op.id(),0,false,c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC,op.status());
        t.observe(KEY,"ops",c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC,op.status());
        assertTrue(t.hasPending(15000));
        t.observe(KEY,"true",c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.VERIFIED,op.status());
        assertFalse(t.hasPending(15001));
    }

    @Test void integratedSnapshotCannotOverwriteNewerNativeRuleSync() throws Exception {
        String network=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleClientNetwork.java"))
                .replace("\r\n","\n");
        String integrated=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetIntegratedRules.java"))
                .replace("\r\n","\n");
        assertTrue(network.contains("CarpetIntegratedRules.request(client,server,current,CATALOG.generation(),\n"
                +"                nativeRulesVersion,requestId)"));
        assertTrue(integrated.contains("connection,received,nativeRulesVersion,requestId)"));
        int accept=network.indexOf("static synchronized void acceptIntegrated(");
        int mutation=network.indexOf("integratedOwners=new HashMap<>(snapshot.owners)",accept);
        assertTrue(accept>=0&&mutation>accept);
        String beforeMutation=network.substring(accept,mutation);
        assertTrue(beforeMutation.contains("if(requestNativeVersion!=nativeRulesVersion){\n"
                +"            integratedTicks=20;\n"
                +"            return;"),
                "Discard a stale integrated read before changing the catalog or metadata");
        int nativeUpdate=network.indexOf("Tag tag = packet.get(\"Rules\")");
        assertTrue(nativeUpdate>mutation);
        assertTrue(network.indexOf("nativeRulesVersion++;",nativeUpdate)>nativeUpdate,
                "Newer native Carpet updates must invalidate in-flight snapshots");
        assertTrue(network.contains("nativeRulesVersion=0;"),
                "Reset the native event generation between server connections");
    }

    @Test void temporaryRuleChangeRequestsFastLocalVerificationButNotFastIdlePolling() throws Exception {
        String network=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleClientNetwork.java"))
                .replace("\r\n","\n");
        int submit=network.indexOf("public static synchronized CarpetRuleOperationTracker.Operation submit(");
        int connect=network.indexOf("public static synchronized void connected(",submit);
        assertTrue(submit>=0&&connect>submit);
        String send=network.substring(submit,connect);
        assertTrue(send.contains("CarpetRuleWire.send(connection,packet)"));
        assertTrue(send.contains("if(Minecraft.getInstance().getSingleplayerServer()!=null){\n"
                +"            // A live switch change needs prompt authoritative verification too."));
        assertTrue(send.contains("integratedTicks=20;"));
        assertTrue(network.contains("if(++integratedTicks<20)return;"),
                "Idle integrated world should retain its original one-second cadence");
        assertTrue(network.contains("if(++integratedPendingTicks<100)return;"),
                "A stuck first-world snapshot should recover without restarting Minecraft");
        assertTrue(network.contains("if(source!=connection||requestId!=integratedRequestId)return;"),
                "A late timed-out reply must not overwrite the latest snapshot");
        assertTrue(network.contains("integratedRequestId++;"),
                "World changes invalidate outstanding snapshot request identities");
    }

    @Test void repeatedListClicksKeepThePendingBooleanVisibleInDetails() throws Exception {
        String screen=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerScreen.java"));
        String renderer=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerRenderer.java"));
        int click=screen.indexOf("void quickToggle(");
        int next=screen.indexOf("void chooseBoolean(",click);
        assertTrue(click>=0&&next>click);
        String handler=screen.substring(click,next);
        int pending=handler.indexOf("String pending=temporaryInFlight(rule.key())");
        int keep=handler.indexOf("targetBox.setValue(pending)",pending);
        int refresh=handler.indexOf("refreshTarget()",pending);
        assertTrue(pending>=0&&keep>pending&&refresh>keep,
                "Second click must not reset the detail view to the old rule value");
        assertTrue(handler.contains("if(pending!=null){"));
        assertTrue(renderer.contains("owner.temporaryInFlight(entry.key())"));
        assertTrue(renderer.contains("inFlight==null?entry.value():inFlight"));
    }

    @Test void pendingWriteBlocksAnotherUntilSyncOrTimeout() {
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker tracker=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation first=tracker.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        assertTrue(tracker.hasPending(1100));
        assertNull(tracker.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"other",1200));
        tracker.acknowledge(first.id(),1,false,c.generation());
        assertTrue(tracker.hasPending(1300));
        tracker.observe(KEY,"true",c.generation());
        assertFalse(tracker.hasPending(1301));
        assertNotNull(tracker.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"other",1400));
        assertFalse(tracker.hasPending(20000));
    }

    @Test void staleGenerationAndErrorsCannotConfirm(){
        CarpetRuleCatalog c=catalog();
        CarpetRuleOperationTracker t=new CarpetRuleOperationTracker();
        CarpetRuleOperationTracker.Operation op=t.begin(c,KEY,CarpetRuleCommands.Mode.TEMPORARY,"true",1000);
        t.acknowledge(op.id(),1,false,c.generation()+1);
        assertEquals(CarpetRuleOperationTracker.Status.SENT,op.status());
        t.acknowledge(op.id(),0,true,c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.REJECTED,op.status());
        t.observe(KEY,"true",c.generation());
        assertEquals(CarpetRuleOperationTracker.Status.REJECTED,op.status());
    }
}

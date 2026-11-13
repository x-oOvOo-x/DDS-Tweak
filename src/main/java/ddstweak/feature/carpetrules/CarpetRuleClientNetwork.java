package ddstweak.feature.carpetrules;

import ddstweak.network.DdsClientSessionHooks;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * Carpet protocol adapter. Never cancels native packets, never registers an
 * alternative codec when Fabric Carpet is present, and does not use DDS HELLO.
 */
public final class CarpetRuleClientNetwork {
    private static final CarpetRuleCatalog CATALOG = new CarpetRuleCatalog();
    private static final CarpetRuleOperationTracker OPERATIONS = new CarpetRuleOperationTracker();
    private static ClientPacketListener connection;
    private static boolean helloSent;
    private static boolean initialized;
    private static Map<CarpetRuleCatalog.RuleKey,String> integratedOwners=Collections.emptyMap();
    private static Map<CarpetRuleCatalog.RuleKey,List<String>> integratedSuggestions=Collections.emptyMap();
    private static Map<CarpetRuleCatalog.RuleKey,String> integratedDefinitions=Collections.emptyMap();
    private static Map<CarpetRuleCatalog.RuleKey,CarpetRuleMultiValue> integratedMultiValues=Collections.emptyMap();
    private static Set<CarpetRuleCatalog.RuleKey> integratedDefaults=Collections.emptySet();
    private static Map<CarpetRuleCatalog.RuleKey,String> integratedPersistedValues=Collections.emptyMap();
    private static Set<String> integratedKnownManagers=Collections.emptySet();
    private static boolean integratedDefaultsKnown;
    private static int integratedTicks;
    private static boolean integratedRequestPending;
    private static int integratedPendingTicks;
    private static long integratedRequestId;
    private static int integratedReadFailures;
    private static long nativeRulesVersion;
    private static final boolean NATIVE_CARPET = FabricLoader.getInstance().isModLoaded("carpet");
    private static final int MAX_RULES = 4096;

    private CarpetRuleClientNetwork() {}

    public static synchronized void initialize() {
        if (initialized) return;
        DdsClientSessionHooks.registerConnectionResetHook(CarpetRuleClientNetwork::reset);
        ClientTickEvents.END_CLIENT_TICK.register(CarpetRuleClientNetwork::tickIntegrated);
        CarpetRuleAuthority.initialize();
        //#if MC < 260000
        VanillaGameRuleRemoteSync.initialize();
        //#endif
        initialized = true;
    }

    private static synchronized void tickIntegrated(Minecraft client) {
        MinecraftServer server=client.getSingleplayerServer();
        ClientPacketListener current=client.getConnection();
        if(current!=null){
            connected(current);
            CarpetRuleAuthority.tick(current,CATALOG);
        }
        if(!NATIVE_CARPET || server==null || current==null)return;
        connected(current);
        if(integratedRequestPending){
            // A join-time task may never complete (for example, a stale world).
            // Do not strand an empty catalog behind a permanent pending flag.
            if(++integratedPendingTicks<100)return;
            integratedRequestPending=false;
            integratedTicks=20;
        }
        if(++integratedTicks<20)return;
        integratedTicks=0;
        integratedPendingTicks=0;
        integratedRequestPending=true;
        long requestId=++integratedRequestId;
        CarpetIntegratedRules.request(client,server,current,CATALOG.generation(),
                nativeRulesVersion,requestId);
    }

    static synchronized void acceptIntegrated(ClientPacketListener source,
                     CarpetIntegratedRules.Snapshot snapshot,long requestNativeVersion,
                     long requestId) {
        // Timed-out and previous-world callbacks must not complete a new request.
        if(source!=connection||requestId!=integratedRequestId)return;
        integratedRequestPending=false;
        integratedPendingTicks=0;
        // An asynchronous server snapshot captured before a newer native rule
        // sync must never overwrite that newer value or bounce the UI switch.
        if(requestNativeVersion!=nativeRulesVersion){
            integratedTicks=20;
            return;
        }
        if(snapshot==null){
            integratedReadFailures++;
            return;
        }
        if(snapshot.defaultsRevision!=CarpetIntegratedRules.defaultsRevision())return;
        List<CarpetRuleCatalog.RemoteRule> incoming=snapshot.rules;
        if(incoming.size()>MAX_RULES)return;
        integratedReadFailures=0;
        integratedOwners=new HashMap<>(snapshot.owners);
        integratedSuggestions=Collections.unmodifiableMap(new HashMap<>(snapshot.suggestions));
        integratedDefinitions=Collections.unmodifiableMap(new HashMap<>(snapshot.definitionDefaults));
        integratedMultiValues=Collections.unmodifiableMap(new HashMap<>(snapshot.multiValues));
        integratedDefaults=Collections.unmodifiableSet(new java.util.HashSet<>(snapshot.savedDefaults));
        integratedPersistedValues=Collections.unmodifiableMap(new HashMap<>(snapshot.persistedValues));
        integratedKnownManagers=Collections.unmodifiableSet(new java.util.HashSet<>(snapshot.knownDefaultManagers));
        integratedDefaultsKnown=snapshot.defaultsKnown;
        if(!CATALOG.isReady()) {
            CATALOG.replace(source,incoming);
            OPERATIONS.observePersistence(integratedPersistedValues,integratedKnownManagers,CATALOG.generation());
            return;
        }
        for(CarpetRuleCatalog.RemoteRule rule:incoming) {
            CarpetRuleCatalog.RemoteRule previous=CATALOG.get(rule.key());
            if(previous==null || !previous.value().equals(rule.value())
                    || previous.managerExplicit()!=rule.managerExplicit()) {
                if(CATALOG.update(source,rule))
                    OPERATIONS.observe(rule.key(),rule.value(),CATALOG.generation());
            }
        }
        OPERATIONS.observePersistence(integratedPersistedValues,integratedKnownManagers,CATALOG.generation());
    }

    public static synchronized boolean integratedSyncUnavailable(){
        return integratedReadFailures>=3 && !CATALOG.isReady();
    }

    /** Verified only from the running integrated server, never from remote mod lists. */
    public static synchronized String integratedOwner(CarpetRuleCatalog.RuleKey key){
        String serverOwner=CarpetRuleAuthority.owner(key);
        return serverOwner!=null?serverOwner:integratedOwners.get(key);
    }

    /** Server-provided name, or integrated-server Fabric metadata for a verified owner. */
    public static synchronized String ownerDisplayName(CarpetRuleCatalog.RuleKey key){
        String serverName=CarpetRuleAuthority.ownerName(key);
        if(serverName!=null)return serverName;
        if(Minecraft.getInstance().getSingleplayerServer()==null)return null;
        String id=integratedOwners.get(key);
        if(id==null)return null;
        return FabricLoader.getInstance().getModContainer(id)
                .map(mod->mod.getMetadata().getName())
                .filter(name->name!=null&&!name.trim().isEmpty())
                .orElse(null);
    }

    public static synchronized String sourceDisplayName(String owner){
        for(CarpetRuleCatalog.RemoteRule rule:CATALOG.snapshot()){
            if(!owner.equals(integratedOwner(rule.key())))continue;
            String full=ownerDisplayName(rule.key());
            if(full!=null)return full;
        }
        return null;
    }

    public static synchronized List<String> suggestions(CarpetRuleCatalog.RuleKey key){
        List<String> fromServer=CarpetRuleAuthority.suggestions(key);
        if(!fromServer.isEmpty())return fromServer;
        return integratedSuggestions.getOrDefault(key,Collections.emptyList());
    }

    public static synchronized CarpetRuleMultiValue multiValue(CarpetRuleCatalog.RuleKey key){
        CarpetRuleMultiValue fromServer=CarpetRuleAuthority.multiValue(key);
        return fromServer!=null?fromServer:integratedMultiValues.get(key);
    }

    public static synchronized String definitionDefault(CarpetRuleCatalog.RuleKey key){
        String server=CarpetRuleAuthority.definitionDefault(key);
        return server!=null?server:integratedDefinitions.get(key);
    }

    public static int serverPersisted(CarpetRuleCatalog.RuleKey key){
        return CarpetRuleAuthority.persisted(key);
    }

    /** True when a server can assert at least part of persistent configuration. */
    public static synchronized boolean defaultsAvailable(){
        return CarpetRuleAuthority.anyPersistedKnown()||!integratedKnownManagers.isEmpty();
    }

    public static synchronized boolean defaultsComplete(){
        return CarpetRuleAuthority.allPersistedKnown()||integratedDefaultsKnown;
    }

    public static synchronized Set<CarpetRuleCatalog.RuleKey> savedDefaults(){
        Set<CarpetRuleCatalog.RuleKey> merged=new java.util.HashSet<>(CarpetRuleAuthority.saved());
        // A known local manager overrides stale remote metadata in both directions:
        // save and remove must update the icon and "Saved defaults" filter together.
        merged.removeIf(key->integratedKnownManagers.contains(key.manager()));
        merged.addAll(integratedDefaults);
        return Collections.unmodifiableSet(merged);
    }

    /** Available only for the integrated world, never for remote servers. */
    public static synchronized Set<CarpetRuleCatalog.RuleKey> integratedSavedDefaults(){
        return integratedDefaults;
    }
    public static synchronized boolean integratedDefaultsKnown(){
        return integratedDefaultsKnown;
    }

    /** Local fallback confidence is per registered Carpet manager, not global. */
    public static synchronized boolean integratedDefaultKnown(CarpetRuleCatalog.RuleKey key){
        return key!=null&&integratedKnownManagers.contains(key.manager());
    }

    public static CarpetRuleCatalog catalog() { return CATALOG; }
    public static CarpetRuleOperationTracker operations() { return OPERATIONS; }

    /**
     * An advisory client-command-tree check. Server permission and validators
     * remain authoritative, even if this returns true.
     */
    public static synchronized boolean canSubmit(CarpetRuleCatalog.RuleKey key) {
        return canSubmit(key, CarpetRuleCommands.Mode.TEMPORARY);
    }

    public static synchronized boolean canSubmit(CarpetRuleCatalog.RuleKey key,
                                                 CarpetRuleCommands.Mode mode) {
        Minecraft mc=Minecraft.getInstance();
        if (key==null || mode==null || mc.getConnection()==null
                || mc.getConnection()!=connection || !CATALOG.isReady()
                || OPERATIONS.hasPending(System.currentTimeMillis())) return false;
        CarpetRuleCatalog.RemoteRule remote=CATALOG.get(key);
        if (remote==null || !remote.managerExplicit()) return false;
        // The client command tree is advisory. In 1.21.8 its default-command
        // nodes may be absent even though Carpet accepts the native payload.
        // The server's SnoopyCommandSource uses the real player's permissions
        // and still enforces both manager locks and rule validators.
        //#if MC >= 12108 && MC < 12109
        //$$ if(mode!=CarpetRuleCommands.Mode.TEMPORARY)return true;
        //#endif
        com.mojang.brigadier.tree.CommandNode<?> root=
                mc.getConnection().getCommands().getRoot().getChild(key.manager());
        if (root==null) return false;
        if (mode==CarpetRuleCommands.Mode.SET_DEFAULT)
            return root.getChild("setDefault")!=null;
        if (mode==CarpetRuleCommands.Mode.REMOVE_DEFAULT)
            return root.getChild("removeDefault")!=null;
        return true;
    }

    /**
     * Submit through native Carpet's clientCommand transport, preserving its
     * reply ID. The call only means 'sent'; read Operation.status() for outcome.
     */
    public static synchronized CarpetRuleOperationTracker.Operation submit(
            CarpetRuleCatalog.RuleKey key, CarpetRuleCommands.Mode mode, String target) {
        if(!canSubmit(key,mode))return null;
        String command;
        try {
            command=CarpetRuleCommands.build(key,mode,target);
        }catch(IllegalArgumentException ex){return null;}
        CarpetRuleOperationTracker.Operation op=OPERATIONS.begin(CATALOG,key,mode,target,System.currentTimeMillis());
        if(op==null)return null;

        // Carpet 1.14 has no clientCommand transport. Send as the actual player:
        // the server command tree and Carpet validators remain authoritative.
        // 原版玩家权限生效；规则变更仍须由集成服快照进行验证。
        //#if MC < 11500
        //$$ if(NATIVE_CARPET&&Minecraft.getInstance().getSingleplayerServer()!=null){
        //$$     if(!MinecraftCompat.sendCommandWithoutHistory(Minecraft.getInstance(),command)){
        //$$         OPERATIONS.failToSend(op.id());
        //$$         return null;
        //$$     }
        //$$     OPERATIONS.awaitLocalVerification(op.id(),CATALOG.generation());
        //$$     integratedTicks=20;
        //$$     if(mode!=CarpetRuleCommands.Mode.TEMPORARY)
        //$$         CarpetIntegratedRules.invalidatePersistedDefaults();
        //$$     return op;
        //$$ }
        //#endif

        CompoundTag details=new CompoundTag();
        details.putString("id",op.id());
        details.putString("command",command);
        CompoundTag packet=new CompoundTag();
        packet.put("clientCommand",details);
        try {
            if(!CarpetRuleWire.send(connection,packet)){
                OPERATIONS.failToSend(op.id());
                return null;
            }
        }catch(RuntimeException ex){
            OPERATIONS.failToSend(op.id());
            return null;
        }
        if(Minecraft.getInstance().getSingleplayerServer()!=null){
            // A live switch change needs prompt authoritative verification too.
            // Existing in-flight snapshots are allowed to finish; the next tick
            // will request a fresh one without increasing idle polling.
            integratedTicks=20;
            if(mode!=CarpetRuleCommands.Mode.TEMPORARY)
                CarpetIntegratedRules.invalidatePersistedDefaults();
        }
        return op;
    }

    public static synchronized void connected(ClientPacketListener current) {
        if (current == null || current == connection) return;
        connection = current;
        VanillaGameRules.reset();
        integratedOwners=Collections.emptyMap();
        integratedSuggestions=Collections.emptyMap();
        integratedDefinitions=Collections.emptyMap();
        integratedMultiValues=Collections.emptyMap();
        integratedDefaults=Collections.emptySet();
        integratedPersistedValues=Collections.emptyMap();
        integratedKnownManagers=Collections.emptySet();
        integratedDefaultsKnown=false;
        helloSent = false;
        integratedTicks=0;
        integratedRequestPending=false;
        integratedPendingTicks=0;
        integratedRequestId++;
        integratedReadFailures=0;
        nativeRulesVersion=0;
        OPERATIONS.reset();
        CATALOG.connect(current);
    }

    public static synchronized void reset() {
        connection = null;
        VanillaGameRules.reset();
        integratedOwners=Collections.emptyMap();
        integratedSuggestions=Collections.emptyMap();
        integratedDefinitions=Collections.emptyMap();
        integratedMultiValues=Collections.emptyMap();
        integratedDefaults=Collections.emptySet();
        integratedPersistedValues=Collections.emptyMap();
        integratedKnownManagers=Collections.emptySet();
        integratedDefaultsKnown=false;
        helloSent = false;
        integratedTicks=0;
        integratedRequestPending=false;
        integratedPendingTicks=0;
        integratedRequestId++;
        integratedReadFailures=0;
        nativeRulesVersion=0;
        OPERATIONS.reset();
        CATALOG.disconnect();
    }

    /** Observation only. Always let Carpet or the game's packet handler continue. */
    public static void observe(Object listener, Object payload) {
        if (!(listener instanceof ClientPacketListener)) return;
        ClientPacketListener source = (ClientPacketListener) listener;
        CompoundTag packet = CarpetRuleWire.inbound(payload);
        if (packet == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != source) return;
        if (mc.isSameThread()) process(source, packet);
        else mc.execute(() -> { if (mc.getConnection() == source) process(source, packet); });
    }

    private static synchronized void process(ClientPacketListener source, CompoundTag packet) {
        if (Minecraft.getInstance().getConnection() != source) return;
        connected(source);
        if (packet.contains("69") && !NATIVE_CARPET && !helloSent) {
            helloSent = CarpetRuleWire.hello(source);
        }
        Tag reply=packet.get("clientCommand");
        if(reply instanceof CompoundTag){
            CompoundTag response=(CompoundTag)reply;
            String id=MinecraftCompat.stringOr(response,"id","");
            if(!id.isEmpty()){
                // Old Carpet replies use "code"; intermediate releases
                // have no numeric result; recent releases use "return".
                int returnCode=response.contains("return")
                        ?MinecraftCompat.intOr(response,"return",0)
                        :MinecraftCompat.intOr(response,"code",0);
                boolean hasError=response.contains("error");
                CarpetRuleOperationTracker.Operation operation=OPERATIONS.find(id);
                boolean persistentReply=operation!=null
                        &&operation.status()==CarpetRuleOperationTracker.Status.SENT
                        &&operation.mode()!=CarpetRuleCommands.Mode.TEMPORARY
                        &&!hasError&&returnCode>=0;
                boolean verifyPersistence=persistentReply
                        &&Minecraft.getInstance().getSingleplayerServer()!=null;
                OPERATIONS.acknowledge(id,returnCode,hasError,CATALOG.generation(),
                        verifyPersistence);
                if(verifyPersistence){
                    CarpetIntegratedRules.invalidatePersistedDefaults();
                    // The Carpet command has replied: schedule a fresh local
                    // disk snapshot on the next tick instead of waiting a second.
                    integratedTicks=20;
                }else if(persistentReply){
                    // Remote CDA metadata normally refreshes every 30 seconds.
                    // Requery after its rate limit instead of showing stale icons.
                    CarpetRuleAuthority.requestPostWriteRefresh();
                }
            }
        }
        Tag tag = packet.get("Rules");
        if (!(tag instanceof CompoundTag)) return;
        CompoundTag rules = (CompoundTag) tag;
        //#if MC >= 12105
        Set<String> keys = rules.keySet();
        //#else
        //$$ Set<String> keys = rules.getAllKeys();
        //#endif
        if (keys.size() > MAX_RULES) return;
        List<CarpetRuleCatalog.RemoteRule> parsed = new ArrayList<>(keys.size());
        for (String wireKey : keys) {
            Tag member = rules.get(wireKey);
            if (!(member instanceof CompoundTag)) return;
            CompoundTag entry = (CompoundTag)member;
            boolean explicit = entry.contains("Manager");
            String manager = explicit ? MinecraftCompat.stringOr(entry,"Manager","") : "carpet";
            String name = explicit ? MinecraftCompat.stringOr(entry,"Rule","") : wireKey;
            String value = MinecraftCompat.stringOr(entry,"Value","");
            if (manager.isEmpty() || name.isEmpty() || !entry.contains("Value")) return;
            try {
                parsed.add(new CarpetRuleCatalog.RemoteRule(
                        new CarpetRuleCatalog.RuleKey(manager,name), value, explicit));
            } catch (IllegalArgumentException ex) {
                return;
            }
        }
        if(!CATALOG.isReady()){
            if(CATALOG.replace(source,parsed))nativeRulesVersion++;
        }else for(CarpetRuleCatalog.RemoteRule rule:parsed){
            CarpetRuleCatalog.RemoteRule previous=CATALOG.get(rule.key());
            if(CATALOG.update(source,rule)){
                if(previous==null||!previous.value().equals(rule.value())
                        ||previous.managerExplicit()!=rule.managerExplicit())
                    nativeRulesVersion++;
                OPERATIONS.observe(rule.key(),rule.value(),CATALOG.generation());
            }
        }
    }
}

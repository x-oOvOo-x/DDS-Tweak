package ddstweak.feature.carpetrules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.server.MinecraftServer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;
import java.util.Set;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.LinkedHashSet;

/**
 * Integrated servers use Carpet's in-memory connection: the native Carpet
 * handshake skips a rule snapshot for this case. Read the running server's
 * actual registered managers on its server thread, then publish on the client
 * thread. Never use the locally installed mods as remote-server evidence.
 */
final class CarpetIntegratedRules {
    private static final long DEFAULT_CONFIG_REFRESH_NS=TimeUnit.SECONDS.toNanos(5);
    private static final AtomicLong DEFAULTS_REVISION=new AtomicLong();
    // Accessed only from the integrated server thread; never retain a closed world.
    private static WeakReference<MinecraftServer> cachedServer=new WeakReference<>(null);
    private static CarpetRuleDefaultConfig.ReadResult cachedDefaults;
    private static long cachedAtNs,cachedRevision;

    private CarpetIntegratedRules() {}

    static void invalidatePersistedDefaults(){DEFAULTS_REVISION.incrementAndGet();}

    static long defaultsRevision(){return DEFAULTS_REVISION.get();}

    private static CarpetRuleDefaultConfig.ReadResult readCachedDefaults(
            MinecraftServer server,List<CarpetRuleCatalog.RemoteRule> rules,long revision){
        long now=System.nanoTime();
        if(cachedDefaults==null||cachedServer.get()!=server||cachedRevision!=revision
                ||now-cachedAtNs>=DEFAULT_CONFIG_REFRESH_NS){
            cachedDefaults=CarpetRuleDefaultConfig.readWorld(server,rules);
            cachedServer=new WeakReference<>(server);
            cachedRevision=revision;
            cachedAtNs=now;
        }
        return cachedDefaults;
    }

    static void request(Minecraft client, MinecraftServer server,
                        ClientPacketListener connection, long generation,
                        long nativeRulesVersion,long requestId) {
        server.execute(() -> {
            Snapshot received=readOnServerThread(server);
            client.execute(() -> {
                if(client.getSingleplayerServer()!=server || client.getConnection()!=connection
                        || CarpetRuleClientNetwork.catalog().generation()!=generation)return;
                CarpetRuleClientNetwork.acceptIntegrated(
                        connection,received,nativeRulesVersion,requestId);
            });
        });
    }

    static final class Snapshot {
        final List<CarpetRuleCatalog.RemoteRule> rules;
        final Map<CarpetRuleCatalog.RuleKey,String> owners;
        final Set<CarpetRuleCatalog.RuleKey> savedDefaults;
        final Set<String> knownDefaultManagers;
        final Map<CarpetRuleCatalog.RuleKey,String> persistedValues;
        final long defaultsRevision;
        final Map<CarpetRuleCatalog.RuleKey,List<String>> suggestions;
        final Map<CarpetRuleCatalog.RuleKey,String> definitionDefaults;
        final Map<CarpetRuleCatalog.RuleKey,CarpetRuleMultiValue> multiValues;
        final boolean defaultsKnown;
        Snapshot(List<CarpetRuleCatalog.RemoteRule> rules,
                 Map<CarpetRuleCatalog.RuleKey,String> owners,
                 Set<CarpetRuleCatalog.RuleKey> savedDefaults,boolean defaultsKnown,
                 Set<String> knownDefaultManagers,
                 Map<CarpetRuleCatalog.RuleKey,String> persistedValues,long defaultsRevision,
                  Map<CarpetRuleCatalog.RuleKey,List<String>> suggestions,
                 Map<CarpetRuleCatalog.RuleKey,String> definitionDefaults,
                 Map<CarpetRuleCatalog.RuleKey,CarpetRuleMultiValue> multiValues){
            this.rules=rules;this.owners=owners;
            this.savedDefaults=savedDefaults;this.defaultsKnown=defaultsKnown;
            this.knownDefaultManagers=knownDefaultManagers;
            this.persistedValues=persistedValues;this.defaultsRevision=defaultsRevision;
            this.suggestions=suggestions;
            this.definitionDefaults=definitionDefaults;
            this.multiValues=multiValues;
        }
    }

    private static Snapshot readOnServerThread(MinecraftServer server){
        try {
            Class<?> carpet=Class.forName("carpet.CarpetServer");
            List<Object> managers=registeredManagers(carpet);
            Method normalize=findRuleNormalizer();
            List<CarpetRuleCatalog.RemoteRule> rules=new ArrayList<>();
            Map<CarpetRuleCatalog.RuleKey,String> owners=new HashMap<>();
            Map<CarpetRuleCatalog.RuleKey,List<String>> suggestions=new HashMap<>();
            Map<CarpetRuleCatalog.RuleKey,String> definitionDefaults=new HashMap<>();
            Map<CarpetRuleCatalog.RuleKey,CarpetRuleMultiValue> multiValues=new HashMap<>();
            for(Object manager:managers){
                try{
                    ManagerSnapshot snapshot=readManager(manager,normalize);
                    rules.addAll(snapshot.rules);
                    owners.putAll(snapshot.owners);
                    suggestions.putAll(snapshot.suggestions);
                    definitionDefaults.putAll(snapshot.definitionDefaults);
                    multiValues.putAll(snapshot.multiValues);
                }catch(ReflectiveOperationException|IllegalArgumentException ignored){
                    // An incompatible extension cannot disable the other managers.
                }
            }
            if(rules.isEmpty())return null;
            long revision=DEFAULTS_REVISION.get();
            CarpetRuleDefaultConfig.ReadResult defaults=readCachedDefaults(server,rules,revision);
            return new Snapshot(rules,owners,defaults.rules(),
                    defaults.known(),defaults.knownManagers(),defaults.values(),revision,
                    suggestions,definitionDefaults,multiValues);
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){
            return null;
        }
    }

    /** Read either the modern manager registry or the legacy 1.14.x fields. */
    static List<Object> registeredManagers(Class<?> carpet) throws ReflectiveOperationException {
        List<Object> managers=new ArrayList<>();
        Method forEach=findMethod(carpet,"forEachManager",Consumer.class);
        if(forEach!=null&&Modifier.isStatic(forEach.getModifiers())){
            forEach.invoke(null,(Consumer<Object>)manager->{
                if(manager!=null)managers.add(manager);
            });
            return managers;
        }

        Object core=readField(carpet,null,"settingsManager");
        if(core!=null)managers.add(core);
        Object extensions=readField(carpet,null,"extensions");
        if(extensions instanceof Iterable){
            for(Object extension:(Iterable<?>)extensions){
                if(extension==null)continue;
                try{
                    Object manager=invokeOptional(extension,"extensionSettingsManager");
                    if(manager==null)manager=invokeOptional(extension,"customSettingsManager");
                    if(manager!=null)managers.add(manager);
                }catch(ReflectiveOperationException|RuntimeException ignored){
                    // A broken extension must not hide Carpet's core manager.
                }
            }
        }
        if(managers.isEmpty())throw new NoSuchMethodException("Carpet settings manager registry");
        return managers;
    }

    /** ParsedRule in legacy Carpet exposes fields/getAsString instead of CarpetRule methods. */
    static ManagerSnapshot readManager(Object manager,Method normalize)
            throws ReflectiveOperationException {
        Object rawId=invokeOptional(manager,"identifier");
        if(!(rawId instanceof String))rawId=readField(manager.getClass(),manager,"identifier");
        if(!(rawId instanceof String)||((String)rawId).trim().isEmpty())
            throw new NoSuchFieldException("identifier");
        String id=(String)rawId;

        Object rawRules=invokeOptional(manager,"getCarpetRules");
        if(!(rawRules instanceof Collection))rawRules=invokeOptional(manager,"getRules");
        if(!(rawRules instanceof Collection))
            throw new NoSuchMethodException("getCarpetRules/getRules");

        ManagerSnapshot snapshot=new ManagerSnapshot();
        for(Object rule:(Collection<?>)rawRules){
            if(rule==null)continue;
            try{
                Object rawName=invokeOptional(rule,"name");
                if(!(rawName instanceof String))rawName=readField(rule.getClass(),rule,"name");
                if(!(rawName instanceof String)||((String)rawName).trim().isEmpty())continue;
                String name=(String)rawName;

                Object rawValue=invokeOptional(rule,"value");
                if(rawValue==null)rawValue=invokeOptional(rule,"getAsString");
                String value=ruleString(rawValue,normalize);
                if(value==null)continue;

                CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey(id,name);
                snapshot.rules.add(new CarpetRuleCatalog.RemoteRule(key,value,true));

                Object rawDefault=invokeOptional(rule,"defaultValue");
                if(rawDefault==null)rawDefault=readField(rule.getClass(),rule,"defaultAsString");
                String standard=ruleString(rawDefault,normalize);
                if(standard!=null)snapshot.definitionDefaults.put(key,standard);

                String owner=null;
                try{owner=CarpetRuleOwnership.owner(rule);}
                catch(RuntimeException ignored){}
                if(owner!=null)snapshot.owners.put(key,owner);

                Object rawChoices=invokeOptional(rule,"suggestions");
                if(!(rawChoices instanceof Collection))
                    rawChoices=readField(rule.getClass(),rule,"options");
                List<String> choices=validSuggestions(rawChoices);
                if(!choices.isEmpty()){
                    snapshot.suggestions.put(key,choices);
                    CarpetRuleMultiValue multi=CarpetRuleMultiValue.forVerifiedDdsRule(
                            id,name,owner,choices);
                    if(multi!=null)snapshot.multiValues.put(key,multi);
                }
            }catch(ReflectiveOperationException|IllegalArgumentException ignored){
                // Skip one malformed rule without losing the remaining registered rules.
            }
        }
        return snapshot;
    }

    private static Method findRuleNormalizer(){
        try{
            Class<?> helper=Class.forName("carpet.api.settings.RuleHelper");
            return findMethod(helper,"toRuleString",Object.class);
        }catch(ClassNotFoundException|LinkageError ignored){
            // Legacy Carpet stores the already formatted value on ParsedRule.
            return null;
        }
    }

    private static Method findMethod(Class<?> type,String name,Class<?>... parameters){
        try{
            Method method=type.getMethod(name,parameters);
            method.setAccessible(true);
            return method;
        }catch(NoSuchMethodException ignored){}
        for(Class<?> current=type;current!=null;current=current.getSuperclass()){
            try{
                Method method=current.getDeclaredMethod(name,parameters);
                method.setAccessible(true);
                return method;
            }catch(NoSuchMethodException ignored){}
        }
        return null;
    }

    private static Object invokeOptional(Object target,String name)
            throws ReflectiveOperationException {
        if(target==null)return null;
        Method method=findMethod(target.getClass(),name);
        return method==null?null:method.invoke(target);
    }

    private static Object readField(Class<?> type,Object target,String name)
            throws IllegalAccessException {
        for(Class<?> current=type;current!=null;current=current.getSuperclass()){
            try{
                Field field=current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            }catch(NoSuchFieldException ignored){}
        }
        return null;
    }

    private static String ruleString(Object value,Method normalize)
            throws ReflectiveOperationException {
        if(value==null)return null;
        if(value instanceof String)return (String)value;
        if(normalize!=null){
            Object formatted=normalize.invoke(null,value);
            if(formatted instanceof String)return (String)formatted;
        }
        return String.valueOf(value);
    }

    private static List<String> validSuggestions(Object raw){
        if(!(raw instanceof Collection))return Collections.emptyList();
        Collection<?> choices=(Collection<?>)raw;
        if(choices.isEmpty()||choices.size()>16)return Collections.emptyList();
        List<String> values=new ArrayList<>(choices.size());
        for(Object choice:choices){
            if(!(choice instanceof String))return Collections.emptyList();
            String item=(String)choice;
            if(item.isEmpty()||item.length()>80||item.indexOf('\n')>=0
                    ||item.indexOf('\r')>=0||values.contains(item))
                return Collections.emptyList();
            values.add(item);
        }
        return values;
    }

    static final class ManagerSnapshot {
        final List<CarpetRuleCatalog.RemoteRule> rules=new ArrayList<>();
        final Map<CarpetRuleCatalog.RuleKey,String> owners=new HashMap<>();
        final Map<CarpetRuleCatalog.RuleKey,List<String>> suggestions=new HashMap<>();
        final Map<CarpetRuleCatalog.RuleKey,String> definitionDefaults=new HashMap<>();
        final Map<CarpetRuleCatalog.RuleKey,CarpetRuleMultiValue> multiValues=new HashMap<>();
    }
}

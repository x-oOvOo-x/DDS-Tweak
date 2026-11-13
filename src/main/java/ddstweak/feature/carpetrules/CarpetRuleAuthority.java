package ddstweak.feature.carpetrules;

import ddstweak.network.DdsClientNetwork;
import ddstweak.network.DdsClientSessionHooks;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Optional CDA-only facts. Native Carpet remains fully usable without CDA.
 * No packet can create a rule unless it has been enumerated by the server
 * and the snapshot has been assembled atomically on this connection.
 */
final class CarpetRuleAuthority {
    static final String REQUEST="carpet_rule_info_request_v1";
    static final String PAGE="carpet_rule_info_page_v1";
    private static final int MAX_RULES=4096, PAGE_SIZE=20, MAX_PAGES=205;
    // CDA accepts at most one metadata request per player every 5 seconds.
    // Leave a margin so a post-write refresh is not silently rate-limited.
    private static final long MIN_REQUEST_GAP_NS=TimeUnit.MILLISECONDS.toNanos(5500);
    private static Map<CarpetRuleCatalog.RuleKey,Fact> facts=Collections.emptyMap();
    private static Map<Integer,List<Entry>> pages=new HashMap<>();
    private static ClientPacketListener awaiting;
    private static long generation=-1L;
    private static int nonce,expectedPages=-1,expectedTotal=-1,ticks,waitTicks;
    private static long lastRequestNs;
    private static boolean refreshAfterWrite;
    private static boolean initialized;

    private CarpetRuleAuthority(){}
    static synchronized void initialize(){
        if(initialized)return;
        initialized=true;
        DdsClientNetwork.registerC2S(REQUEST);
        DdsClientNetwork.registerOrderedS2CHandler(PAGE,CarpetRuleAuthority::receive);
        DdsClientSessionHooks.registerResetHook(CarpetRuleAuthority::reset);
    }

    static synchronized void reset(){
        facts=Collections.emptyMap();
        pages=new HashMap<>();
        awaiting=null;generation=-1L;
        expectedPages=-1;expectedTotal=-1;ticks=0;waitTicks=0;
        lastRequestNs=0;refreshAfterWrite=false;
    }

    /** A command reply is not proof of persistence: ask CDA for a fresh snapshot. */
    static synchronized void requestPostWriteRefresh(){
        // A snapshot started before the write must not complete the new verification.
        discardPending();
        refreshAfterWrite=true;
    }

    static boolean shouldRequest(int ticks,boolean postWrite,long nowNs,long lastNs){
        return (postWrite||ticks>=40)
                &&(lastNs==0||nowNs-lastNs>=MIN_REQUEST_GAP_NS);
    }

    /** Called from Carpet's existing client tick, with bounded polling. */
    static synchronized void tick(ClientPacketListener current,CarpetRuleCatalog catalog){
        if(current==null)return;
        if(awaiting!=null&&++waitTicks>200)discardPending();
        ++ticks;
        if(!refreshAfterWrite&&ticks<40)return;
        if(awaiting!=null||!DdsClientNetwork.canSend(REQUEST))return;
        long nowNs=System.nanoTime();
        if(!shouldRequest(ticks,refreshAfterWrite,nowNs,lastRequestNs))return;
        ticks=0;
        awaiting=current;
        generation=catalog.generation();
        expectedPages=-1;expectedTotal=-1;pages.clear();waitTicks=0;
        nonce=nonce==Integer.MAX_VALUE?1:nonce+1;
        CompoundTag request=new CompoundTag();
        request.putInt("nonce",nonce);
        if(!DdsClientNetwork.send(REQUEST,request))discardPending();
        else{
            lastRequestNs=nowNs;
            refreshAfterWrite=false;
            ticks=-560; // Idle refresh remains about every 30 seconds.
        }
    }

    private static void receive(ClientPacketListener source,CompoundTag data){
        Minecraft mc=Minecraft.getInstance();
        Runnable task=()->accept(source,data);
        if(mc.isSameThread())task.run();
        else mc.execute(()->{
            if(mc.getConnection()==source)task.run();
        });
    }

    private static synchronized void accept(ClientPacketListener source,CompoundTag packet){
        Minecraft mc=Minecraft.getInstance();
        CarpetRuleCatalog catalog=CarpetRuleClientNetwork.catalog();
        if(source!=mc.getConnection()||source!=awaiting||generation!=catalog.generation())return;
        if(MinecraftCompat.intOr(packet,"schema",0)!=1
                ||MinecraftCompat.intOr(packet,"nonce",-1)!=nonce)return;
        int index=MinecraftCompat.intOr(packet,"page",-1);
        int total=MinecraftCompat.intOr(packet,"total",-1);
        int count=MinecraftCompat.intOr(packet,"count",-1);
        int parts=MinecraftCompat.intOr(packet,"pages",-1);
        if(parts<1||parts>MAX_PAGES||total<0||total>MAX_RULES
                ||index<0||index>=parts||count<0||count>PAGE_SIZE
                ||parts!=(Math.max(1,(total+PAGE_SIZE-1)/PAGE_SIZE))
                ||count!=Math.min(PAGE_SIZE,Math.max(0,total-index*PAGE_SIZE))
                ||(expectedPages!=-1&&(expectedPages!=parts||expectedTotal!=total))
                ||pages.containsKey(index)){discardPending();return;}
        expectedPages=parts;expectedTotal=total;
        List<Entry> part=new ArrayList<>(count);
        for(int i=0;i<count;i++){
            String k=Integer.toString(i);
            String manager=MinecraftCompat.stringOr(packet,"m"+k,"");
            String name=MinecraftCompat.stringOr(packet,"n"+k,"");
            String value=MinecraftCompat.stringOr(packet,"v"+k,"");
            if(manager.isEmpty()||name.isEmpty()||manager.length()>128
                    ||name.length()>128||value.length()>256){discardPending();return;}
            CarpetRuleCatalog.RuleKey key;
            try{key=new CarpetRuleCatalog.RuleKey(manager,name);}
            catch(IllegalArgumentException ex){discardPending();return;}
            String owner=packet.contains("o"+k)?MinecraftCompat.stringOr(packet,"o"+k,""):null;
            String ownerName=packet.contains("l"+k)?MinecraftCompat.stringOr(packet,"l"+k,""):null;
            String def=packet.contains("d"+k)?MinecraftCompat.stringOr(packet,"d"+k,""):null;
            if(owner!=null&&(owner.isEmpty()||owner.length()>128))owner=null;
            if(ownerName!=null&&(ownerName.isEmpty()||ownerName.length()>128
                    ||ownerName.indexOf('\n')>=0||ownerName.indexOf('\r')>=0)){
                discardPending();return;
            }
            if(owner==null)ownerName=null;
            if(def!=null&&def.length()>256){discardPending();return;}
            int persist=MinecraftCompat.intOr(packet,"p"+k,0);
            int choices=MinecraftCompat.intOr(packet,"q"+k,0);
            if(persist<0||persist>2||choices<0||choices>16){discardPending();return;}
            int tags=MinecraftCompat.intOr(packet,"t"+k,0);
            if(tags<0||tags>16){discardPending();return;}
            List<String> categories=new ArrayList<>(tags);
            for(int j=0;j<tags;j++){
                String tag=MinecraftCompat.stringOr(packet,"c"+k+"_"+j,"");
                if(tag.isEmpty()||tag.length()>64||tag.indexOf('\n')>=0
                        ||tag.indexOf('\r')>=0||categories.contains(tag)){
                    discardPending();return;
                }
                categories.add(tag);
            }
            List<String> options=new ArrayList<>(choices);
            for(int j=0;j<choices;j++){
                String option=MinecraftCompat.stringOr(packet,"s"+k+"_"+j,"");
                if(option.isEmpty()||option.length()>80||option.indexOf('\n')>=0
                        ||option.indexOf('\r')>=0||options.contains(option)){
                    discardPending();return;
                }
                options.add(option);
            }
            String empty=packet.contains("e"+k)?MinecraftCompat.stringOr(packet,"e"+k,""):null;
            String all=packet.contains("a"+k)?MinecraftCompat.stringOr(packet,"a"+k,""):null;
            if(empty!=null&&(all==null||empty.equals(all)||!options.contains(empty)
                    ||!options.contains(all)||options.size()<3)){
                discardPending();return;
            }
            if(all!=null&&empty==null){discardPending();return;}
            part.add(new Entry(key,value,new Fact(owner,ownerName,def,persist,
                    options,categories,empty,all)));
        }
        pages.put(index,part);
        if(pages.size()!=expectedPages)return;
        Map<CarpetRuleCatalog.RuleKey,Fact> newFacts=new HashMap<>();
        List<CarpetRuleCatalog.RemoteRule> snapshot=new ArrayList<>(total);
        for(int p=0;p<expectedPages;p++){
            List<Entry> rows=pages.get(p);
            if(rows==null){discardPending();return;}
            for(Entry row:rows){
                if(newFacts.putIfAbsent(row.key,row.fact)!=null){discardPending();return;}
                snapshot.add(new CarpetRuleCatalog.RemoteRule(row.key,row.value,true));
            }
        }
        if(newFacts.size()!=expectedTotal){discardPending();return;}
        if(!catalog.isReady())catalog.replace(source,snapshot);
        else{
            // The Carpet catalog is primary for rule identity and current value.
            newFacts.keySet().retainAll(catalogKeys(catalog));
        }
        facts=Collections.unmodifiableMap(newFacts);
        discardPending();
    }

    private static Set<CarpetRuleCatalog.RuleKey> catalogKeys(CarpetRuleCatalog catalog){
        Set<CarpetRuleCatalog.RuleKey> keys=new HashSet<>();
        for(CarpetRuleCatalog.RemoteRule rule:catalog.snapshot())keys.add(rule.key());
        return keys;
    }

    private static void discardPending(){
        awaiting=null;generation=-1L;expectedPages=-1;expectedTotal=-1;
        pages.clear();waitTicks=0;
    }

    static synchronized String owner(CarpetRuleCatalog.RuleKey key){
        Fact f=facts.get(key);return f==null?null:f.owner;
    }
    static synchronized String ownerName(CarpetRuleCatalog.RuleKey key){
        Fact f=facts.get(key);return f==null?null:f.ownerName;
    }
    static synchronized List<String> suggestions(CarpetRuleCatalog.RuleKey key){
        Fact fact=facts.get(key);
        return fact==null?Collections.emptyList():fact.suggestions;
    }
    static synchronized List<String> categories(CarpetRuleCatalog.RuleKey key){
        Fact fact=facts.get(key);
        return fact==null?Collections.emptyList():fact.categories;
    }
    static synchronized CarpetRuleMultiValue multiValue(CarpetRuleCatalog.RuleKey key){
        Fact fact=facts.get(key);
        if(fact==null||fact.multiEmpty==null)return null;
        return new CarpetRuleMultiValue(fact.suggestions,fact.multiEmpty,fact.multiAll);
    }
    static synchronized int verifiedCount(){
        int n=0;
        for(Fact fact:facts.values())if(fact.owner!=null)n++;
        return n;
    }
    static synchronized int factCount(){return facts.size();}
    static synchronized String syncStatus(){
        if(!facts.isEmpty())return "ready";
        if(awaiting!=null)return "waiting";
        return DdsClientNetwork.canSend(REQUEST)?"available":"offline";
    }
    static synchronized String definitionDefault(CarpetRuleCatalog.RuleKey key){
        Fact f=facts.get(key);return f==null?null:f.definitionDefault;
    }
    static synchronized int persisted(CarpetRuleCatalog.RuleKey key){
        Fact f=facts.get(key);return f==null?0:f.persisted;
    }
    static synchronized boolean available(){return !facts.isEmpty();}
    static synchronized Set<CarpetRuleCatalog.RuleKey> saved(){
        Set<CarpetRuleCatalog.RuleKey> out=new HashSet<>();
        for(Map.Entry<CarpetRuleCatalog.RuleKey,Fact> entry:facts.entrySet())
            if(entry.getValue().persisted==1)out.add(entry.getKey());
        return Collections.unmodifiableSet(out);
    }
    static synchronized boolean anyPersistedKnown(){
        for(Fact f:facts.values())if(f.persisted!=0)return true;
        return false;
    }
    static synchronized boolean allPersistedKnown(){
        if(facts.isEmpty())return false;
        for(Fact f:facts.values())if(f.persisted==0)return false;
        return true;
    }

    private static final class Entry{
        final CarpetRuleCatalog.RuleKey key;
        final String value;
        final Fact fact;
        Entry(CarpetRuleCatalog.RuleKey key,String value,Fact fact){
            this.key=key;this.value=value;this.fact=fact;
        }
    }
    private static final class Fact{
        final String owner,ownerName,definitionDefault,multiEmpty,multiAll;
        final int persisted;
        final List<String> suggestions,categories;
        Fact(String owner,String ownerName,String definitionDefault,int persisted,
             List<String> suggestions,List<String> categories,String multiEmpty,String multiAll){
            this.owner=owner;this.ownerName=ownerName;
            this.multiEmpty=multiEmpty;this.multiAll=multiAll;
            this.definitionDefault=definitionDefault;this.persisted=persisted;
            this.suggestions=Collections.unmodifiableList(new ArrayList<>(suggestions));
            this.categories=Collections.unmodifiableList(new ArrayList<>(categories));
        }
    }
}

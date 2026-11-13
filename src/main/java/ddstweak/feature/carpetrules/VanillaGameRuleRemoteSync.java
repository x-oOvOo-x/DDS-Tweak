package ddstweak.feature.carpetrules;

import ddstweak.network.DdsClientNetwork;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional, permission-scoped read-only gamerule snapshots from CDA servers
 * on pre-26.x Minecraft. Native 26.x gamerule packets remain authoritative.
 * Never infers a remote value from the command tree or client registry.
 */
//#if MC < 260000
final class VanillaGameRuleRemoteSync {
    static final String REQUEST="vanilla_gamerule_info_request_v1";
    static final String PAGE="vanilla_gamerule_info_page_v1";
    private static final int MAX_RULES=512,PAGE_SIZE=20,MAX_PAGES=26;
    private static boolean initialized;
    private static ClientPacketListener awaiting;
    private static int nonce,expectedPages=-1,expectedTotal=-1;
    private static long requestedAt;
    private static final Map<Integer,List<VanillaGameRules.Entry>> pages=new HashMap<>();

    private VanillaGameRuleRemoteSync(){}

    static synchronized void initialize(){
        if(initialized)return;
        DdsClientNetwork.registerC2S(REQUEST);
        DdsClientNetwork.registerOrderedS2CHandler(PAGE,VanillaGameRuleRemoteSync::receive);
        initialized=true;
    }
    static boolean available(){return DdsClientNetwork.canSend(REQUEST);}

    static synchronized void reset(){
        awaiting=null;expectedPages=-1;expectedTotal=-1;requestedAt=0;pages.clear();
        // Deliberately retain nonce: old replies cannot match a new world.
    }
    static synchronized void request(ClientPacketListener source){
        if(source==null||!available())return;
        if(awaiting!=null&&requestedAt>0
                &&System.currentTimeMillis()-requestedAt>=3500L)
            VanillaGameRules.remoteReadFailed(awaiting);
        reset();
        nonce=nonce==Integer.MAX_VALUE?1:nonce+1;
        awaiting=source;
        requestedAt=System.currentTimeMillis();
        CompoundTag data=new CompoundTag();
        data.putInt("nonce",nonce);
        if(!DdsClientNetwork.send(REQUEST,data))reset();
    }

    private static void receive(ClientPacketListener source,CompoundTag data){
        Minecraft client=Minecraft.getInstance();
        if(client.isSameThread())accept(source,data);
        else client.execute(()->{
            if(client.getConnection()==source)accept(source,data);
        });
    }
    private static synchronized void accept(ClientPacketListener source,CompoundTag data){
        Minecraft client=Minecraft.getInstance();
        if(source!=awaiting||source!=client.getConnection()
                ||client.getSingleplayerServer()!=null)return;
        if(MinecraftCompat.intOr(data,"schema",0)!=1
                ||MinecraftCompat.intOr(data,"nonce",-1)!=nonce)return;
        int page=MinecraftCompat.intOr(data,"page",-1);
        int count=MinecraftCompat.intOr(data,"count",-1);
        int total=MinecraftCompat.intOr(data,"total",-1);
        int parts=MinecraftCompat.intOr(data,"pages",-1);
        if(total<0||total>MAX_RULES||parts<1||parts>MAX_PAGES
                ||parts!=Math.max(1,(total+PAGE_SIZE-1)/PAGE_SIZE)
                ||page<0||page>=parts||count<0||count>PAGE_SIZE
                ||count!=Math.min(PAGE_SIZE,Math.max(0,total-page*PAGE_SIZE))
                ||expectedPages!=-1&&(expectedPages!=parts||expectedTotal!=total)
                ||pages.containsKey(page)){
            fail(source);return;
        }
        expectedPages=parts;expectedTotal=total;
        List<VanillaGameRules.Entry> rows=new ArrayList<>(count);
        for(int i=0;i<count;i++){
            String k=Integer.toString(i);
            String id=MinecraftCompat.stringOr(data,"i"+k,"");
            String value=MinecraftCompat.stringOr(data,"v"+k,"");
            String category=MinecraftCompat.stringOr(data,"c"+k,"");
            String titleKey=MinecraftCompat.stringOr(data,"t"+k,"");
            String standard=data.contains("d"+k)
                    ?MinecraftCompat.stringOr(data,"d"+k,""):null;
            int booleanType=MinecraftCompat.intOr(data,"b"+k,-1);
            if(!VanillaGameRuleRemoteSync.validId(id)
                    ||booleanType<0||booleanType>1
                    ||!validTypedValue(value,booleanType==1)
                    ||standard!=null&&!validTypedValue(standard,booleanType==1)
                    ||category.isEmpty()||category.length()>64||control(category)
                    ||titleKey.length()>128||control(titleKey)){
                fail(source);return;
            }
            rows.add(new VanillaGameRules.Entry(id,
                    titleKey.isEmpty()?id:titleKey,"",category,value,standard,
                    booleanType==1,null));
        }
        pages.put(page,rows);
        if(pages.size()!=expectedPages)return;
        Map<String,VanillaGameRules.Entry> completed=new HashMap<>();
        for(int i=0;i<expectedPages;i++){
            List<VanillaGameRules.Entry> batch=pages.get(i);
            if(batch==null){fail(source);return;}
            for(VanillaGameRules.Entry entry:batch)
                if(completed.putIfAbsent(entry.id,entry)!=null){fail(source);return;}
        }
        if(completed.size()!=expectedTotal){fail(source);return;}
        reset();
        VanillaGameRules.acceptRemote(source,Collections.unmodifiableMap(completed));
    }

    private static void fail(ClientPacketListener source){
        reset();
        VanillaGameRules.remoteReadFailed(source);
    }

    static boolean validId(String id){
        return id!=null&&id.matches("[A-Za-z0-9_:.\\-]{1,128}");
    }
    static boolean validValue(String value){
        if(value==null||value.isEmpty()||value.length()>256)return false;
        if("true".equals(value)||"false".equals(value))return true;
        try{Integer.parseInt(value);return true;}
        catch(NumberFormatException ignored){return false;}
    }
    static boolean validTypedValue(String value,boolean booleanType){
        if(!validValue(value))return false;
        if(booleanType)return "true".equals(value)||"false".equals(value);
        return !"true".equals(value)&&!"false".equals(value);
    }
    private static boolean control(String value){
        for(int i=0;i<value.length();i++)
            if(Character.isISOControl(value.charAt(i)))return true;
        return false;
    }
}
//#endif

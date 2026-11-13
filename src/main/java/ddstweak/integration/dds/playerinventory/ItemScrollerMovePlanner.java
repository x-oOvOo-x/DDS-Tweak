package ddstweak.integration.dds.playerinventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Predicts exact, left-click PICKUP routes for one item type. All positions
 * are menu IDs, not inventory backing indices. No server or MC classes here.
 * 仅计算单类物品的菜单槽位移动，不访问服务端。
 */
public final class ItemScrollerMovePlanner {
    public static final int MAX_CLICKS=512;

    public static final class SlotState {
        public final int id,count,limit;
        public final boolean usable;
        public SlotState(int id,int count,int limit,boolean usable){
            if(id<0||count<0||limit<0||count>limit)throw new IllegalArgumentException("slot snapshot");
            this.id=id;this.count=count;this.limit=limit;this.usable=usable;
        }
    }

    public static final class Plan {
        public final List<Integer> clicks;
        public final Map<Integer,Integer> before,after;
        public final int moved;
        private Plan(List<Integer> clicks,Map<Integer,Integer> before,Map<Integer,Integer> after,int moved){
            this.clicks=Collections.unmodifiableList(new ArrayList<>(clicks));
            this.before=Collections.unmodifiableMap(new LinkedHashMap<>(before));
            this.after=Collections.unmodifiableMap(new LinkedHashMap<>(after));
            this.moved=moved;
        }
    }

    private ItemScrollerMovePlanner(){}

    /**
     * All passed source stacks have the same item/components. Destination
     * entries are empty or compatible; incompatible slots must not be passed.
     * If capacity is insufficient, return the remaining cursor to its source.
     * A null plan means no safe complete plan fits within the click bound.
     */
    public static Plan plan(List<SlotState> sources,List<SlotState> destinations){
        if(sources==null||destinations==null)return null;
        Map<Integer,Integer> before=new LinkedHashMap<>();
        Map<Integer,Integer> after=new LinkedHashMap<>();
        for(SlotState slot:sources){
            if(!register(slot,before,after))return null;
        }
        for(SlotState slot:destinations){
            if(!register(slot,before,after))return null;
        }
        List<Integer> clicks=new ArrayList<>();
        int moved=0;
        for(SlotState source:sources){
            if(!source.usable||after.get(source.id)==0)continue;
            int available=0;
            for(SlotState destination:destinations){
                if(destination.usable)
                    available+=Math.max(0,destination.limit-after.get(destination.id));
            }
            if(available==0)break;
            int carried=after.get(source.id);
            after.put(source.id,0);
            clicks.add(source.id); // pick up the source stack
            // Merge into occupied matching stacks first; use empty slots second.
            for(int pass=0;pass<2&&carried>0;pass++){
                for(SlotState destination:destinations){
                    if(!destination.usable||carried==0)continue;
                    int occupied=after.get(destination.id);
                    if((pass==0)!=(occupied>0))continue;
                    int amount=Math.min(destination.limit-occupied,carried);
                    if(amount<=0)continue;
                    after.put(destination.id,occupied+amount);
                    carried-=amount;
                    moved+=amount;
                    clicks.add(destination.id);
                }
            }
            if(carried>0){
                // The emptied source can always hold its original remainder.
                after.put(source.id,carried);
                clicks.add(source.id);
            }
            if(clicks.size()>MAX_CLICKS)return null;
        }
        if(moved==0)return new Plan(Collections.emptyList(),before,before,0);
        long initial=0,finalTotal=0;
        for(int value:before.values())initial+=value;
        for(int value:after.values())finalTotal+=value;
        if(initial!=finalTotal||clicks.size()>MAX_CLICKS)throw new IllegalStateException("item count mismatch");
        return new Plan(clicks,before,after,moved);
    }

    private static boolean register(SlotState slot,Map<Integer,Integer> before,
                                    Map<Integer,Integer> after){
        if(slot==null||before.containsKey(slot.id))return false;
        before.put(slot.id,slot.count);
        after.put(slot.id,slot.count);
        return true;
    }
}

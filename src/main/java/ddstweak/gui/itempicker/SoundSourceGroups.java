package ddstweak.gui.itempicker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** UI-only grouping. Persist actual sound IDs so old configurations keep working. */
public final class SoundSourceGroups {
    private SoundSourceGroups(){}

    public static String key(String id){
        if(id.equals("event:1000")||id.equals("event:1001")||id.equals("event:1002"))
            return "event:dispenser_dropper";
        int colon=id.indexOf(':');
        if(colon<0)return id;
        String path=id.substring(colon+1);
        if(!path.startsWith("block."))return id;
        int next=path.indexOf('.',6);
        return next<0?id:id.substring(0,colon+1)+path.substring(0,next);
    }

    public static Map<String,List<String>> group(Collection<String> candidates,Collection<String> selected){
        Map<String,List<String>> groups=new LinkedHashMap<>();
        for(String id:candidates)add(groups,id);
        for(String id:selected)add(groups,id);
        return groups;
    }

    private static void add(Map<String,List<String>> groups,String id){
        List<String> values=groups.computeIfAbsent(key(id),ignored->new ArrayList<>());
        if(!values.contains(id))values.add(id);
    }

    public static List<String> selectedKeys(Map<String,List<String>> groups,Collection<String> selected){
        Set<String> chosen=new LinkedHashSet<>(selected);
        List<String> result=new ArrayList<>();
        for(Map.Entry<String,List<String>> entry:groups.entrySet())
            if(entry.getValue().stream().anyMatch(chosen::contains))result.add(entry.getKey());
        return result;
    }

    /** Include all related sounds or remove every saved sound belonging to the category. */
    public static List<String> toggle(List<String> current,List<String> group,boolean include){
        Set<String> result=new LinkedHashSet<>(current);
        if(include)result.addAll(group);
        else result.removeAll(group);
        return new ArrayList<>(result);
    }
}

package ddstweak.feature.carpetrules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Declarative comma-combination editor; never assumes suggestions imply multi-select. */
public final class CarpetRuleMultiValue {
    private final List<String> options;
    private final String empty,all;

    public CarpetRuleMultiValue(List<String> suggestions,String empty,String all){
        if(suggestions==null||empty==null||all==null||empty.equals(all)
                ||!suggestions.contains(empty)||!suggestions.contains(all))
            throw new IllegalArgumentException("Invalid multi-value metadata");
        List<String> values=new ArrayList<>(suggestions);
        values.remove(empty);
        values.remove(all);
        if(values.isEmpty()||new LinkedHashSet<>(values).size()!=values.size())
            throw new IllegalArgumentException("Invalid combination options");
        this.options=Collections.unmodifiableList(values);
        this.empty=empty;this.all=all;
    }

    /** Combination metadata is granted only to the three audited CDA rules. */
    static CarpetRuleMultiValue forVerifiedDdsRule(String manager,String name,String owner,
                                                    List<String> suggestions){
        if(!"carpet-dds-addition".equals(manager)||!manager.equals(owner)||name==null)return null;
        String empty;
        switch(name){
            case "explosionNoBlockDamage":
            case "naturalPassiveSpawning":empty="none";break;
            case "easyMining":empty="false";break;
            default:return null;
        }
        if(suggestions==null||suggestions.size()<3||suggestions.size()>16
                ||!suggestions.contains(empty)||!suggestions.contains("all"))return null;
        try{return new CarpetRuleMultiValue(suggestions,empty,"all");}
        catch(IllegalArgumentException ex){return null;}
    }

    public List<String> options(){return options;}
    public String empty(){return empty;}
    public String all(){return all;}

    /** Returns null for invalid current values instead of silently replacing them. */
    public Set<String> parse(String value){
        if(value==null)return null;
        if(empty.equals(value))return Collections.emptySet();
        if(all.equals(value))return new LinkedHashSet<>(options);
        Set<String> selected=new LinkedHashSet<>();
        for(String token:value.split(",",-1)){
            String clean=token.trim();
            if(!options.contains(clean)||!selected.add(clean))return null;
        }
        return selected;
    }

    public String toggle(String current,String option){
        if(empty.equals(option))return empty;
        if(all.equals(option))return all;
        if(!options.contains(option))return null;
        Set<String> parsed=parse(current);
        if(parsed==null)return null;
        Set<String> selected=new LinkedHashSet<>(parsed);
        if(!selected.add(option))selected.remove(option);
        if(selected.isEmpty())return empty;
        if(selected.size()==options.size())return all;
        List<String> ordered=new ArrayList<>();
        for(String value:options)if(selected.contains(value))ordered.add(value);
        return String.join(",",ordered);
    }

    public boolean checked(String current,String option){
        if(empty.equals(option))return empty.equals(current);
        if(all.equals(option))return all.equals(current);
        Set<String> selected=parse(current);
        return selected!=null&&selected.contains(option);
    }

    public int selectedCount(String current){
        Set<String> selected=parse(current);
        return selected==null?-1:selected.size();
    }
}

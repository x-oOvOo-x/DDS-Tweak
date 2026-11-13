package ddstweak.feature.carpetrules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** UI-only navigation; never asserts that a Carpet manager is a mod owner. */
public final class CarpetRuleSidebar {
    public enum Kind { ALL, CARPET, OWNER, UNKNOWN, CATEGORY, FAVORITES, MODIFIED, GAME_RULES, SAVED_DEFAULTS, GROUP, PLAN }
    /** Which characters receive the additional 0.5px Han-only sidebar correction. */
    static boolean isChineseLabelCodepoint(int cp){
        return Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN
                ||cp>=0x3000&&cp<=0x303F||cp==0xFF08||cp==0xFF09;
    }

    public enum Section { NONE, EXTENSIONS, CATEGORIES, GROUPS }

    /** Keep the selected section expanded; fixed filters close both accordions. */
    public static Section sectionFor(Kind kind){
        switch(kind){
            case CARPET:
            case OWNER:
            case UNKNOWN:return Section.EXTENSIONS;
            case CATEGORY:return Section.CATEGORIES;
            case GROUP:
            case PLAN:return Section.GROUPS;
            default:return Section.NONE;
        }
    }

    /** A collapsed source occupies only its heading; quick filters remain pinned. */
    public static int groupHeaderY(int top,int bottom,boolean sourceExpanded,int sourceRows){
        int y=top+62+(sourceExpanded?Math.max(0,sourceRows)*22+5:0);
        return Math.min(y,quickHeaderY(bottom)-30);
    }

    public static int sourceRows(int top,int bottom,int count){
        int available=Math.max(0,(quickHeaderY(bottom)-top-62-30)/22);
        return Math.min(count,available);
    }

    public static int choiceTop(int top,int bottom,Section section,int groupHeader){
        return section==Section.EXTENSIONS?top+62:groupHeader+26;
    }

    public static int choiceBottom(int top,int bottom,Section section,int groupHeader){
        return section==Section.EXTENSIONS?groupHeader-5:quickHeaderY(bottom)-8;
    }

    /** Quick filters stay anchored to the sidebar bottom. */
    public static int quickHeaderY(int bottom){return bottom-118;}

    /** Display true Carpet tags only; owner is already visible in the source filter/details. */
    public static String cardMetadata(String verifiedOwner,java.util.Collection<String> categories){
        StringBuilder out=new StringBuilder();
        if(categories!=null)for(String tag:categories){
            if(tag==null||tag.trim().isEmpty())continue;
            String clean=tag.trim();
            if(clean.equalsIgnoreCase(verifiedOwner))continue;
            if(out.length()>0)out.append(" · ");
            out.append(clean);
        }
        return out.toString();
    }

    /** Hide test-only MagicLib legacy-compat rules in this UI, not in the server catalog. */
    public static boolean hiddenCompatibilityRule(CarpetRuleCatalog.RemoteRule rule,String owner){
        return rule!=null&&owner!=null&&owner.startsWith("magiclib-")
                &&"magiclib".equals(rule.key().manager());
    }

    public static String addonLabel(String owner){
        switch(owner){
            case "carpet":return "Carpet";
            case "carpet-dds-addition":return "DDS";
            case "carpet-igny-addition":return "IGNY";
            case "carpet-org-addition":return "Org";
            case "carpet-ams-addition":return "AMS";
            case "carpet-tis-addition":return "TIS";
            case "carpet-rof-addition":return "Rof";
            case "carpet_cuo_addition":return "CuO";
            case "remscarpetaddition":return "RMS";
            case "cca":return "CCA";
            case "gca":
            case "gugle-carpet-addition":return "GCA";
            default:return owner;
        }
    }

    /** A stable ID groups rules; full name is trusted display metadata only. */
    public static String sourceLabel(String owner,String fullName){
        if("carpet".equals(owner))return "Carpet";
        String name=fullName==null||fullName.trim().isEmpty()?owner:fullName.trim();
        String abbreviated=addonLabel(owner);
        if(abbreviated.equals(owner)||abbreviated.equalsIgnoreCase(name))return name;
        return abbreviated+"（"+name+"）";
    }

    /** The left navigation is intentionally concise; long names belong in native tooltips. */
    public static String sourceShortLabel(String owner,String fullName){
        String alias=addonLabel(owner);
        if(!alias.equals(owner))return alias;
        if(fullName!=null){
            String clean=fullName.trim();
            if(clean.startsWith("Carpet ")&&clean.endsWith(" Addition")){
                String middle=clean.substring(7,clean.length()-9).trim();
                if(!middle.isEmpty()&&middle.length()<=16)return middle;
            }
            if(!clean.isEmpty())return clean;
        }
        return owner;
    }

    public static int groupAddX(int sidebarLeft,int sidebarWidth){
        return sidebarLeft+sidebarWidth-47;
    }

    /** Display-only change; never alter the canonical server Manager+Rule key. */
    public static String displayRuleId(CarpetRuleCatalog.RuleKey key){
        return "carpet".equals(key.manager())?key.name():key.toString();
    }

    public static final class Entry {
        private final Kind kind;
        private final String id;
        private final int count;
        private final boolean available;
        private final boolean containsUnverified;

        private Entry(Kind kind,String id,int count,boolean available){
            this(kind,id,count,available,false);
        }
        private Entry(Kind kind,String id,int count,boolean available,boolean containsUnverified){
            this.kind=kind;this.id=id;this.count=count;this.available=available;
            this.containsUnverified=containsUnverified;
        }
        public static Entry plan(String name,int count){
            return new Entry(Kind.PLAN,name,count,true);
        }
        public static Entry category(String name,int count){
            return new Entry(Kind.CATEGORY,name,count,true);
        }
        public Kind kind(){return kind;}
        public String id(){return id;}
        public int count(){return count;}
        public boolean available(){return available;}
        public boolean containsUnverified(){return containsUnverified;}
        public boolean selectable(){return available;}
        public boolean matches(Kind kind,String id){return this.kind==kind&&this.id.equals(id);}
    }

    private CarpetRuleSidebar(){}

    public static List<Entry> entries(List<CarpetRuleCatalog.RemoteRule> rules,
                                      CarpetRuleMetadata metadata,CarpetRulePreferences prefs){
        return entries(rules,metadata,prefs,Collections.emptyMap());
    }

    public static List<Entry> entries(List<CarpetRuleCatalog.RemoteRule> rules,
                                      CarpetRuleMetadata metadata,CarpetRulePreferences prefs,
                                      Map<CarpetRuleCatalog.RuleKey,String> verifiedOwners){
        return entries(rules,metadata,prefs,verifiedOwners,Collections.emptySet(),false);
    }

    public static List<Entry> entries(List<CarpetRuleCatalog.RemoteRule> rules,
                                      CarpetRuleMetadata metadata,CarpetRulePreferences prefs,
                                      Map<CarpetRuleCatalog.RuleKey,String> verifiedOwners,
                                      java.util.Set<CarpetRuleCatalog.RuleKey> savedDefaults,
                                      boolean defaultsKnown){
        return entries(rules,metadata,prefs,verifiedOwners,savedDefaults,defaultsKnown,
                Collections.emptyMap());
    }

    public static List<Entry> entries(List<CarpetRuleCatalog.RemoteRule> rules,
                                      CarpetRuleMetadata metadata,CarpetRulePreferences prefs,
                                      Map<CarpetRuleCatalog.RuleKey,String> verifiedOwners,
                                      java.util.Set<CarpetRuleCatalog.RuleKey> savedDefaults,
                                      boolean defaultsKnown,
                                      Map<CarpetRuleCatalog.RuleKey,String> definitionDefaults){
        List<Entry> out=new ArrayList<>();
        out.add(new Entry(Kind.ALL,"",rules.size(),true));
        Map<String,Integer> owners=new TreeMap<>();
        int carpet=0,unknown=0,favorite=0,defaults=0,modified=0,knownDefinitions=0;
        for(CarpetRuleCatalog.RemoteRule rule:rules){
            String owner=verifiedOwners.get(rule.key());
            Kind source=sourceKind(rule,owner);
            if(source==Kind.CARPET)carpet++;
            else if(source==Kind.OWNER)owners.merge(owner,1,Integer::sum);
            else unknown++;
            if(prefs.isFavorite(rule.key()))favorite++;
            CarpetRuleModified.State state=CarpetRuleModified.classify(
                    rule.value(),definitionDefaults.get(rule.key()));
            if(state!=CarpetRuleModified.State.UNKNOWN){
                knownDefinitions++;
                if(state==CarpetRuleModified.State.MODIFIED)modified++;
            }
            if(defaultsKnown&&savedDefaults.contains(rule.key()))defaults++;
        }
        out.add(new Entry(Kind.CARPET,"",carpet,true));
        for(Map.Entry<String,Integer> e:owners.entrySet())
            out.add(new Entry(Kind.OWNER,e.getKey(),e.getValue(),true));
        if(unknown>0)out.add(new Entry(Kind.UNKNOWN,"",unknown,true));
        out.add(new Entry(Kind.FAVORITES,"",favorite,true));
        // No trusted default: unknown rather than silently counting the rule as unchanged.
        out.add(new Entry(Kind.MODIFIED,"",knownDefinitions>0?modified:-1,knownDefinitions>0));
        // The native gamerule editor is separate from Carpet's rule protocol.
        out.add(new Entry(Kind.SAVED_DEFAULTS,"",defaultsKnown?defaults:-1,defaultsKnown));
        out.add(new Entry(Kind.GAME_RULES,"",-1,true));
        for(String group:prefs.groupNames()){
            int count=0;
            for(CarpetRuleCatalog.RemoteRule rule:rules)
                if(prefs.inGroup(group,rule.key()))count++;
            out.add(new Entry(Kind.GROUP,group,count,true));
        }
        return Collections.unmodifiableList(out);
    }

    public static boolean matches(Kind kind,String id,CarpetRuleCatalog.RemoteRule rule,
                                  CarpetRuleMetadata metadata,CarpetRulePreferences prefs){
        return matches(kind,id,rule,metadata,prefs,null);
    }

    public static boolean matches(Kind kind,String id,CarpetRuleCatalog.RemoteRule rule,
                                  CarpetRuleMetadata metadata,CarpetRulePreferences prefs,
                                  String verifiedOwner){
        switch(kind){
            case ALL:return true;
            case CARPET:return sourceKind(rule,verifiedOwner)==Kind.CARPET;
            case OWNER:return sourceKind(rule,verifiedOwner)==Kind.OWNER&&id.equals(verifiedOwner);
            case UNKNOWN:return sourceKind(rule,verifiedOwner)==Kind.UNKNOWN;
            case FAVORITES:return prefs.isFavorite(rule.key());
            case MODIFIED:return false; // Requires an authoritative definition default.
            case CATEGORY:return false; // Screen filters authoritative raw tags.
            case GAME_RULES:return false; // A native game rule is not a Carpet rule.
            case GROUP:return prefs.inGroup(id,rule.key());
            case PLAN:return false; // Plan membership comes from saved target values.
            default:return false;
        }
    }

    /**
     * Verified owner wins over a shared Manager. For missing owners, the
     * Manager only decides a UI fallback; it is never claimed as real origin.
     */
    private static Kind sourceKind(CarpetRuleCatalog.RemoteRule rule,String verifiedOwner){
        if(verifiedOwner!=null&&!verifiedOwner.isEmpty()){
            if("carpet".equals(verifiedOwner))return Kind.CARPET;
            return Kind.OWNER;
        }
        return Kind.UNKNOWN;
    }

}

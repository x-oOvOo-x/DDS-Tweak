package ddstweak.feature.carpetrules;

import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.gui.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Consistent rule hover content for browser, presets, membership and history. */
final class CarpetRuleUiTooltip {
    private CarpetRuleUiTooltip(){}

    static List<String> lines(Font font,CarpetRuleCatalog.RemoteRule rule,
            CarpetRuleMetadata metadata,boolean vanilla,int screenWidth,int screenHeight){
        if(rule==null)return Collections.emptyList();
        int textWidth=Math.max(84,Math.min(264,screenWidth-48)-16);
        CarpetRuleMetadata.Description info=metadata.find(rule.key());
        VanillaGameRules.Entry nativeRule=vanilla?VanillaGameRules.get(rule.key()):null;
        String name=nativeRule!=null?nativeRule.title:
                info==null?rule.key().name():info.displayName();
        List<String> lines=new ArrayList<>();
        for(String line:wrapped(font,name,textWidth))lines.add("§f§l"+line);
        for(String line:wrapped(font,rule.key().name(),textWidth))lines.add("§7"+line);
        String owner=nativeRule!=null?"minecraft":
                CarpetRuleClientNetwork.integratedOwner(rule.key());
        String source=owner==null?tr("dds-tweak.carpet_rules.owner_unverified"):
                nativeRule!=null?"Minecraft":CarpetRuleSidebar.sourceLabel(owner,
                        CarpetRuleClientNetwork.ownerDisplayName(rule.key()));
        lines.add("§b"+fit(font,source,textWidth));
        List<String> tags=nativeRule!=null?Collections.singletonList(nativeRule.category):
                CarpetRuleAuthority.categories(rule.key());
        if(tags.isEmpty()&&info!=null)tags=new ArrayList<>(info.categories());
        if(!tags.isEmpty())lines.add("§8"+fit(font,String.join(" · ",tags),textWidth));

        List<String> values=new ArrayList<>();
        values.add(" ");
        for(String line:wrapped(font,tr("dds-tweak.carpet_rules.tooltip.current",
                rule.value()),textWidth))values.add("§a"+line);
        String defined=nativeRule!=null?nativeRule.defaultValue:
                CarpetRuleClientNetwork.definitionDefault(rule.key());
        if(defined!=null)
            for(String line:wrapped(font,tr("dds-tweak.carpet_rules.tooltip.default",
                    defined),textWidth))values.add("§7"+line);
        CarpetRuleMultiValue multi=nativeRule!=null?null:
                CarpetRuleAuthority.multiValue(rule.key());
        List<String> options=nativeRule!=null?Collections.emptyList():
                CarpetRuleClientNetwork.suggestions(rule.key());
        if(multi!=null){
            int count=multi.selectedCount(rule.value());
            if(count>=0)values.add("§7"+tr("dds-tweak.carpet_rules.tooltip.selected",
                    count,multi.options().size()));
        }else if(!options.isEmpty()){
            values.add("§7"+fit(font,tr("dds-tweak.carpet_rules.tooltip.options",
                    String.join(" · ",options)),textWidth));
        }

        int capacity=Math.max(6,Math.min(17,
                (screenHeight-42)/CarpetRuleDetailLayout.LINE_HEIGHT));
        String description=nativeRule!=null?nativeRule.description:
                info==null?"":info.detail();
        List<String> descriptions=wrapped(font,description,textWidth);
        int remaining=Math.max(0,capacity-lines.size()-values.size()-1);
        if(remaining>0&&!descriptions.isEmpty()){
            lines.add(" ");
            for(int i=0;i<remaining&&i<descriptions.size();i++)
                lines.add("§f"+descriptions.get(i));
            if(descriptions.size()>remaining)lines.set(lines.size()-1,"§8…");
        }
        lines.addAll(values);
        return lines.size()>capacity?new ArrayList<>(lines.subList(0,capacity)):lines;
    }

    private static String tr(String key,Object...args){
        return MinecraftCompat.translatable(key,args).getString();
    }

    private static String fit(Font font,String value,int width){
        if(value==null||width<=0)return "";
        if(font.width(value)<=width)return value;
        int end=value.length();
        while(end>0&&font.width(value.substring(0,end)+"…")>width)end--;
        return value.substring(0,end)+"…";
    }

    private static List<String> wrapped(Font font,String value,int maxWidth){
        List<String> result=new ArrayList<>();
        if(value==null||value.isEmpty())return result;
        int width=Math.max(20,maxWidth);
        for(String paragraph:value.replace("\r","").split("\n",-1)){
            StringBuilder line=new StringBuilder();
            for(int i=0;i<paragraph.length();i++){
                String next=paragraph.substring(i,i+1);
                if(line.length()>0&&font.width(line.toString()+next)>width){
                    result.add(line.toString());
                    line.setLength(0);
                }
                line.append(next);
            }
            result.add(line.toString());
        }
        return result;
    }
}

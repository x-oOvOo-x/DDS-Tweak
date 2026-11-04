package ddstweak.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/** View-only disclosure: hidden options remain registered, loaded and saved. */
final class ConfigRowGrouping {
    static final float DEFAULT_NAME_SCALE=0.65f;
    static final float MAX_NAME_SCALE=1.0f;

    private ConfigRowGrouping(){}

    static <T> List<T> visible(List<T> options,Map<T,T> parents,Set<T> expanded,boolean searching){
        List<T> rows=new ArrayList<>(options.size());
        for(T option:options){
            T parent=parents.get(option);
            if(parent==null||searching||expanded.contains(parent))rows.add(option);
        }
        return rows;
    }

    /** Width of the visible name column, including indentation of expanded child rows. */
    static <T> int maxLabelWidth(List<T> visible,Map<T,T> parents,ToIntFunction<T> textWidth,int childIndent){
        int width=0;
        for(T option:visible){
            width=Math.max(width,textWidth.applyAsInt(option)+(parents.containsKey(option)?childIndent:0));
        }
        return width;
    }

    /** Reserves the rendered width at the selected scale. */
    static int displayWidth(int translatedWidth,int originalKeyWidth){
        return displayWidth(translatedWidth,originalKeyWidth,DEFAULT_NAME_SCALE);
    }

    static int displayWidth(int translatedWidth,int originalKeyWidth,float scale){
        return Math.max(translatedWidth,(int)Math.ceil(originalKeyWidth*scale)+2);
    }

    static boolean showOriginalName(String localized,String key){
        if(localized==null||localized.trim().isEmpty()||key==null||key.trim().isEmpty())return false;
        String visible=localized.replaceAll("(?i)§[0-9a-fk-or]","");
        // Missing translations can fall back to the raw translation-key identifier.
        return !visible.trim().isEmpty()&&!visible.equals(key)&&!visible.startsWith("dds-tweak.config.");
    }

    /** Crops only the DT overlay when a narrow window cannot fit the complete key. */
    static String fitOriginalName(String key,int availableWidth,ToIntFunction<String> textWidth){
        return fitOriginalName(key,availableWidth,textWidth,DEFAULT_NAME_SCALE);
    }

    static String fitOriginalName(String key,int availableWidth,ToIntFunction<String> textWidth,float scale){
        if(key==null||key.isEmpty()||availableWidth<=0)return "";
        if(textWidth.applyAsInt(key)*scale<=availableWidth)return key;
        final String suffix="…";
        if(textWidth.applyAsInt(suffix)*scale>availableWidth)return "";
        int end=key.length();
        while(end>0){
            end=key.offsetByCodePoints(0,key.codePointCount(0,end)-1);
            String candidate=key.substring(0,end)+suffix;
            if(textWidth.applyAsInt(candidate)*scale<=availableWidth)return candidate;
        }
        return suffix;
    }

    /** Give every visible row the same control width while reserving room for Reset. */
    static int controlWidth(int entryWidth,int labelWidth,int preferredWidth,int gap,int trailingSpace){
        return Math.min(preferredWidth,Math.max(80,entryWidth-labelWidth-gap-trailingSpace));
    }
}

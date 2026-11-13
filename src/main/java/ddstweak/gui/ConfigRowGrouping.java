package ddstweak.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * View-only disclosure: hidden options remain registered, loaded and saved.
 * 仅控制界面折叠；隐藏项仍正常注册、读取和保存。
 */
final class ConfigRowGrouping {
    static final float DEFAULT_NAME_SCALE=0.65f;
    static final float MAX_NAME_SCALE=1.0f;

    private ConfigRowGrouping(){}

    /**
     * Search reveals child rows without changing their persisted options.
     * 搜索时显示折叠子项，但不改变配置值。
     */
    static <T> List<T> visible(List<T> options,Map<T,T> parents,Set<T> expanded,boolean searching){
        List<T> rows=new ArrayList<>(options.size());
        for(T option:options){
            T parent=parents.get(option);
            if(parent==null||searching||expanded.contains(parent))rows.add(option);
        }
        return rows;
    }

    /**
     * Width of the visible name column, including indentation of expanded child rows.
     * 计算可见名称宽度，并包含子项缩进。
     */
    static <T> int maxLabelWidth(List<T> visible,Map<T,T> parents,ToIntFunction<T> textWidth,int childIndent){
        int width=0;
        for(T option:visible){
            width=Math.max(width,textWidth.applyAsInt(option)+(parents.containsKey(option)?childIndent:0));
        }
        return width;
    }

    /**
     * Reserves the rendered width at the selected scale.
     * 按照选定缩放比例预留名称宽度。
     */
    static int displayWidth(int translatedWidth,int originalKeyWidth){
        return displayWidth(translatedWidth,originalKeyWidth,DEFAULT_NAME_SCALE);
    }

    static int displayWidth(int translatedWidth,int originalKeyWidth,float scale){
        return Math.max(translatedWidth,(int)Math.ceil(originalKeyWidth*scale)+2);
    }

    /** Matches TweakerMore's original-name contrast: retain alpha, darken RGB to 60%. */
    static int darkerColor(int color){
        int r=(int)(((color>>>16)&0xFF)*0.6);
        int g=(int)(((color>>>8)&0xFF)*0.6);
        int b=(int)((color&0xFF)*0.6);
        return (color&0xFF000000)|(r<<16)|(g<<8)|b;
    }

    static boolean showOriginalName(String localized,String key){
        if(localized==null||localized.trim().isEmpty()||key==null||key.trim().isEmpty())return false;
        String visible=localized.replaceAll("(?i)§[0-9a-fk-or]","");
        // Missing translations can fall back to the raw translation-key identifier.
        // 缺失翻译时可能回退显示原始翻译键。
        return !visible.trim().isEmpty()&&!visible.equals(key)&&!visible.startsWith("dds-tweak.config.");
    }

    /**
     * Crops only the DT overlay when a narrow window cannot fit the complete key.
     * 窗口过窄时仅截断 DT 叠加显示的原始键名。
     */
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

    /**
     * Give every visible row the same control width while reserving room for Reset.
     * 统一可见行的控件宽度，并预留重置按钮空间。
     */
    static int controlWidth(int entryWidth,int labelWidth,int preferredWidth,int gap,int trailingSpace){
        return Math.min(preferredWidth,Math.max(80,entryWidth-labelWidth-gap-trailingSpace));
    }
}

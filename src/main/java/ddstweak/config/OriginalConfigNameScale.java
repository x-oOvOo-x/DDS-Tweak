package ddstweak.config;

/**
 * Persist an integer percentage, render a unitless multiplier.
 * 以整数百分比保存原始名称的显示缩放。
 */
public final class OriginalConfigNameScale {
    public static final int MIN_PERCENT=50;
    public static final int DEFAULT_PERCENT=65;
    public static final int MAX_PERCENT=100;

    private OriginalConfigNameScale(){}

    public static float asMultiplier(int percent){
        return Math.max(MIN_PERCENT,Math.min(MAX_PERCENT,percent))/100f;
    }

    public static boolean isLegacyMultiplier(double value){
        return Double.isFinite(value)&&value>=0.0&&value<=1.0;
    }

    public static int fromLegacyMultiplier(double value){
        return Math.max(MIN_PERCENT,Math.min(MAX_PERCENT,(int)Math.round(value*100.0)));
    }
}

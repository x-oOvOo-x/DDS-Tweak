package ddstweak.feature.carpetrules;

/** Shared responsive width policy for the rule browser and preset editor.
 * 规则浏览器和预设编辑器共用的响应式宽度策略。
 */
final class CarpetRuleScreenLayout {
    static final int WIDE_BREAKPOINT=720;
    static final int MIN_MIDDLE_WIDTH=300,MIN_DETAIL_WIDTH=230,MAX_DETAIL_WIDTH=300;
    private CarpetRuleScreenLayout(){}

    static boolean wide(int width){return width>=WIDE_BREAKPOINT;}

    /** Reserve useful room for the detail panel before giving extra width to the list.
     * 先为详情面板预留可用宽度，再将额外空间分配给列表。
     */
    static int middleWidth(int availableWidth){
        int available=Math.max(0,availableWidth);
        int detail=Math.max(MIN_DETAIL_WIDTH,
                Math.min(MAX_DETAIL_WIDTH,(int)(available*0.31F)));
        return Math.max(MIN_MIDDLE_WIDTH,available-detail);
    }
}

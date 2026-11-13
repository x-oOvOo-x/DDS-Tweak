package ddstweak.feature.carpetrules;

/** Shared vertical positions for the rule browser and preset editor.
 * 规则浏览器与预设编辑器共用的纵向布局位置。
 */
final class CarpetRuleDetailLayout {
    static final int LINE_HEIGHT=12,MAX_DESCRIPTION_LINES=12;
    private static final int VALUES_GAP=66,TARGET_GAP=73,MODE_GAP=56;
    private CarpetRuleDetailLayout(){}

    static int descriptionTop(int panelTop){return panelTop+60;}
    static int descriptionHeight(int panelHeight){
        return Math.min(MAX_DESCRIPTION_LINES*LINE_HEIGHT,Math.max(36,panelHeight-380));
    }
    private static int compactReduction(int panelHeight){
        return Math.max(0,351+descriptionHeight(panelHeight)-panelHeight);
    }
    private static int valuesGapReduction(int panelHeight){
        return Math.min(30,(compactReduction(panelHeight)+1)/2);
    }
    private static int targetGapReduction(int panelHeight){
        return Math.min(37,Math.max(0,compactReduction(panelHeight)-valuesGapReduction(panelHeight)));
    }
    private static int modeGapReduction(int panelHeight){
        return Math.min(8,Math.max(0,compactReduction(panelHeight)
                -valuesGapReduction(panelHeight)-targetGapReduction(panelHeight)));
    }
    static int valuesTop(int panelTop,int panelHeight){
        return descriptionTop(panelTop)+descriptionHeight(panelHeight)
                +VALUES_GAP-valuesGapReduction(panelHeight);
    }
    static int targetTop(int panelTop,int panelHeight){
        return valuesTop(panelTop,panelHeight)+TARGET_GAP-targetGapReduction(panelHeight);
    }
    static int modeTop(int panelTop,int panelHeight){
        return targetTop(panelTop,panelHeight)+MODE_GAP-modeGapReduction(panelHeight);
    }
    static boolean compact(int panelHeight){return compactReduction(panelHeight)>0;}

    /** Keep two mode rows and one status line above the bottom action button.
     * 确保两行模式选项和状态提示位于底部操作按钮上方。
     */
    static boolean modeOptionsFit(int panelHeight){
        return modeTop(0,panelHeight)+63<=panelHeight-33;
    }
}

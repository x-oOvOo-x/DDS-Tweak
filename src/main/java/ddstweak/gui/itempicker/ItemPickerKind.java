package ddstweak.gui.itempicker;

/**
 * Candidate scope for the shared client-side item picker.
 * 定义通用物品选择器的候选类型。
 */
public enum ItemPickerKind {
    ITEM, FOOD, BLOCK
    //#if MC >= 12111
    , SOUND, SOUND_BLOCK
    //#endif
}

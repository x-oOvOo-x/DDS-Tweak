package ddstweak.gui.itempicker;

/** Candidate scope for the shared client-side item picker. */
public enum ItemPickerKind {
    ITEM, FOOD, BLOCK
    //#if MC >= 12111
    , SOUND, SOUND_BLOCK
    //#endif
}

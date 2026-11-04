package ddstweak.platform.minecraft;

import net.minecraft.client.gui.components.EditBox;

/** Isolate the renamed EditBox focus API from the version-mapped GUI screen. */
public final class ItemPickerFocusCompat {
    private ItemPickerFocusCompat(){}

    public static void unfocus(EditBox field){
        if(field==null)return;
        //#if MC >= 11904
        field.setFocused(false);
        //#else
        //$$ field.setFocus(false);
        //#endif
    }
}

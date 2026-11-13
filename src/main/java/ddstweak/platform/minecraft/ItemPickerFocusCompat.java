package ddstweak.platform.minecraft;

import net.minecraft.client.gui.components.EditBox;

/**
 * Isolate the renamed EditBox focus API from the version-mapped GUI screen.
 * 隔离不同版本文本输入框的焦点接口。
 */
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

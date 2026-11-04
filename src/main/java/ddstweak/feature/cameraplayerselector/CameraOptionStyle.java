package ddstweak.feature.cameraplayerselector;

import net.minecraft.client.gui.components.Button;

/** Keep Camera option states within the vanilla Button renderer. */
final class CameraOptionStyle {
    private CameraOptionStyle(){}

    /** Selected: normal; unselected: disabled look; hovered unselected: native hover state. */
    static void update(Button button,boolean selected,boolean available,boolean hovered){
        // In 1.19.4+ a selected button's retained keyboard focus can also highlight its texture.
        //#if MC >= 11904
        if(selected)button.setFocused(false);
        //#endif
        button.active=available&&(selected||hovered);
    }

    static void update(Button button,boolean selected,boolean available){
        update(button,selected,available,false);
    }

    /** Vanilla uses mouse coordinates to pick the highlighted sprite.
     * Suppress that highlight only over the already-selected option. */
    static int renderMouseX(int mouseX,boolean selectedHovered){
        return selectedHovered?-1:mouseX;
    }

}

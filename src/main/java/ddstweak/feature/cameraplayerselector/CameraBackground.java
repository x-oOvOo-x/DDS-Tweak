package ddstweak.feature.cameraplayerselector;

import ddstweak.gui.SharedScreenBackground;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

/**
 * A single blurred world background is drawn by the owning vanilla Screen first.
 * 复用统一界面渲染器绘制相机导航背景。
 */
final class CameraBackground {
    static final int NAV_HEIGHT=SharedScreenBackground.NAV_HEIGHT;
    static final int HEADER_RULE_Y=SharedScreenBackground.HEADER_RULE_Y,FOOTER_RULE_Y=SharedScreenBackground.FOOTER_RULE_Y,FOOTER_ROW_Y=SharedScreenBackground.FOOTER_ROW_Y;
    private CameraBackground(){}
    static int navigationTop(int height){return SharedScreenBackground.navigationTop(height);}

    /** Shared 360-GUI-pixel header/footer rules for Camera navigation and editors. */
    static void drawNavigationRules(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int width,int top){
        SharedScreenBackground.drawNavigationRules(graphics,width,top);
    }

    static void draw(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int width,int height,int contentTop,int contentBottom) {
        SharedScreenBackground.draw(graphics,width,height,contentTop,contentBottom,0x40000000);
    }
}
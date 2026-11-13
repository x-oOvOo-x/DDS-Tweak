package ddstweak.gui;

/**
 * The old MaLiLib slider background must not sample into adjacent widgets.png UVs.
 * 固定旧版滑块纹理取样范围，避免读取相邻控件。
 */
final class LegacySliderTextureSegments {
    private static final int BACKGROUND_UV_WIDTH=196;
    private static final int LEFT_EDGE=4;
    private static final int MIDDLE_TILE=180;

    private LegacySliderTextureSegments(){}

    @FunctionalInterface
    interface PartConsumer {
        void accept(int offset,int textureX,int width);
    }

    /** Offsets are relative to the original slider X; its right cap remains at width-5. */
    static void forEachBackground(int sliderWidth,PartConsumer draw){
        int backgroundWidth=sliderWidth-6;
        if(backgroundWidth<=0)return;
        if(backgroundWidth<=BACKGROUND_UV_WIDTH){
            draw.accept(1,0,backgroundWidth);
            return;
        }
        draw.accept(1,0,LEFT_EDGE);
        for(int offset=LEFT_EDGE;offset<backgroundWidth;){
            int length=Math.min(MIDDLE_TILE,backgroundWidth-offset);
            draw.accept(offset+1,LEFT_EDGE,length);
            offset+=length;
        }
    }
}

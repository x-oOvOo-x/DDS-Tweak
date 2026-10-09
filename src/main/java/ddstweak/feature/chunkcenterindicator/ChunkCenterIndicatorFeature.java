package ddstweak.feature.chunkcenterindicator;

import ddstweak.config.Configs;

public final class ChunkCenterIndicatorFeature {
    public static final double ARM=0.25D,VERTICAL=0.5D,Y_OFFSET=0.02D;
    public static final int COLOR_ARGB=0xE6FFC714;
    public static final float COLOR_R=1.0F,COLOR_G=0.78F,COLOR_B=0.08F,COLOR_A=0.90F;
    private ChunkCenterIndicatorFeature(){}

    public static boolean enabled(){return Configs.Visual.CHUNK_CENTER_INDICATOR.getBooleanValue();}
    public static double chunkCenter(double coordinate){return Math.floor(coordinate/16.0D)*16.0D+8.0D;}
    public static double markerY(double cameraY){return Math.floor(cameraY)+Y_OFFSET;}
}

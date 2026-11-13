package ddstweak.feature.compactstatuseffects;

/**
 * Represents the rectangle occupied by compact status effects.
 * 表示紧凑状态效果占用的界面区域。
 */
public final class CompactStatusEffectBounds {
    private final int x,y,width,height;
    public CompactStatusEffectBounds(int x,int y,int width,int height){this.x=x;this.y=y;this.width=width;this.height=height;}
    public int x(){return x;} public int y(){return y;} public int width(){return width;} public int height(){return height;}
    public boolean contains(int px,int py){return px>=x&&py>=y&&px<x+width&&py<y+height;}
}

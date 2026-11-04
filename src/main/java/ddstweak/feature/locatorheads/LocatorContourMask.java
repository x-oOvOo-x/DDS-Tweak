package ddstweak.feature.locatorheads;

//#if MC >= 12108
import java.util.Arrays;

/**
 * A silhouette-only alpha mask. The face always occupies 32 mask texels;
 * extra padding lets even a small head retain a fixed-width GUI outline.
 */
public final class LocatorContourMask {
    public static final int SIZE=128,FACE_START=48,FACE_SIZE=32;
    private static final int HAT_START=44,HAT_SIZE=40;
    private LocatorContourMask(){}

    /** Quantize the requested GUI-pixel thickness for the final visible face width. */
    public static int radius(float thickness,float faceWidth){
        if(!Float.isFinite(thickness)||!Float.isFinite(faceWidth)||thickness<=0||faceWidth<=0)
            throw new IllegalArgumentException("Positive finite outline size required");
        return Math.max(1,Math.min(HAT_START,Math.round(thickness*FACE_SIZE/faceWidth)));
    }

    /** Standard-size helper for existing callers and tests (8 GUI pixels per face). */
    public static byte[] create(boolean[] face,boolean[] hat,boolean showHat,float thickness){
        return create(face,hat,showHat,radius(thickness,8f));
    }

    public static byte[] create(boolean[] face,boolean[] hat,boolean showHat,int radius){
        if(face.length!=64||hat.length!=64)throw new IllegalArgumentException("Skin faces must be 8x8");
        radius=Math.max(1,Math.min(HAT_START,radius));
        boolean[] occupied=new boolean[SIZE*SIZE];
        paint(face,occupied,FACE_START,FACE_SIZE);
        if(showHat)paint(hat,occupied,HAT_START,HAT_SIZE);

        byte[] outline=new byte[SIZE*SIZE];
        int[] spans=new int[radius*2+1];
        for(int dy=-radius;dy<=radius;dy++)
            spans[dy+radius]=(int)Math.sqrt(radius*radius-dy*dy);
        // Dilate silhouette boundaries rather than searching every mask pixel.
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
            int index=y*SIZE+x;
            if(!occupied[index])continue;
            if(x>0&&x<SIZE-1&&y>0&&y<SIZE-1&&
                    occupied[index-1]&&occupied[index+1]&&
                    occupied[index-SIZE]&&occupied[index+SIZE])continue;
            for(int dy=-radius;dy<=radius;dy++){
                int yy=y+dy;
                if(yy<0||yy>=SIZE)continue;
                int halfWidth=spans[dy+radius];
                int left=Math.max(0,x-halfWidth),right=Math.min(SIZE,x+halfWidth+1);
                Arrays.fill(outline,yy*SIZE+left,yy*SIZE+right,(byte)255);
            }
        }
        for(int i=0;i<occupied.length;i++)if(occupied[i])outline[i]=0;
        return outline;
    }

    private static void paint(boolean[] pixels,boolean[] target,int start,int size){
        for(int y=0;y<size;y++)for(int x=0;x<size;x++)
            if(pixels[(y*8/size)*8+x*8/size])target[(start+y)*SIZE+start+x]=true;
    }

    public static boolean hasAlpha(byte[] mask){
        for(byte value:mask)if(value!=0)return true;
        return false;
    }

    public record SkinAlpha(boolean[] face,boolean[] hat){
        public SkinAlpha{
            if(face.length!=64||hat.length!=64)throw new IllegalArgumentException("Expected 8x8 layers");
            face=Arrays.copyOf(face,64);
            hat=Arrays.copyOf(hat,64);
        }
    }
}
//#else
//$$ public final class LocatorContourMask { private LocatorContourMask(){} }
//#endif

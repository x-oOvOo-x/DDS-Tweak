package ddstweak.feature.windowplayerhead;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;
import org.apache.logging.log4j.*;
import java.nio.*;
import java.nio.file.*;

final class PlayerHeadRenderer {
    private static final Logger LOGGER=LogManager.getLogger("DDS Tweak/PlayerHeadWindowIcon");
    static final int FACE_X=8,FACE_Y=8,HAT_X=40,HAT_Y=8;
    static final class Icon{
        private final int width,height;private final byte[] rgba;
        Icon(int width,int height,byte[] rgba){this.width=width;this.height=height;this.rgba=rgba;}
        int width(){return width;}int height(){return height;}byte[] rgba(){return rgba;}
    }
    private static final class Source{
        private final int width,height;private final byte[] rgba;
        Source(int width,int height,byte[] rgba){this.width=width;this.height=height;this.rgba=rgba;}
        int width(){return width;}int height(){return height;}byte[] rgba(){return rgba;}
    }

    static Icon[] renderIcons(Path skin){
        Source source=load(skin);return source==null?null:new Icon[]{render(source,32),render(source,64)};
    }
    private static Source load(Path path){
        ByteBuffer input=null,image=null;
        try{
            byte[] bytes=Files.readAllBytes(path);if(bytes.length==0)return null;input=MemoryUtil.memAlloc(bytes.length);input.put(bytes).flip();IntBuffer w=MemoryUtil.memAllocInt(1),h=MemoryUtil.memAllocInt(1),c=MemoryUtil.memAllocInt(1);
            try{image=STBImage.stbi_load_from_memory(input,w,h,c,4);if(image==null)return null;int width=w.get(0),height=h.get(0);if(width<64||width%64!=0||(height!=width&&height*2!=width))return null;byte[] rgba=new byte[width*height*4];image.get(rgba);return new Source(width,height,rgba);}
            finally{MemoryUtil.memFree(w);MemoryUtil.memFree(h);MemoryUtil.memFree(c);}
        }catch(Exception e){LOGGER.warn("Failed to decode cached player skin {}",path,e);return null;}
        finally{if(image!=null)STBImage.stbi_image_free(image);if(input!=null)MemoryUtil.memFree(input);}
    }
    private static Icon render(Source source,int size){
        byte[] out=new byte[size*size*4];int scale=source.width()/64,faceOffset=(int)Math.round(size/18.0),faceSize=size-2*faceOffset;
        draw(source,out,size,FACE_X*scale,FACE_Y*scale,8*scale,faceOffset,faceOffset,faceSize,faceSize);
        draw(source,out,size,HAT_X*scale,HAT_Y*scale,8*scale,0,0,size,size);return new Icon(size,size,out);
    }
    private static void draw(Source s,byte[] out,int outSize,int sx0,int sy0,int srcSize,int dx0,int dy0,int dw,int dh){
        for(int y=0;y<dh;y++)for(int x=0;x<dw;x++){int sx=sx0+x*srcSize/dw,sy=sy0+y*srcSize/dh,si=(sy*s.width()+sx)*4,di=((dy0+y)*outSize+dx0+x)*4,sa=s.rgba()[si+3]&255;if(sa==0)continue;int da=out[di+3]&255,oa=sa+(da*(255-sa)+127)/255;for(int c=0;c<3;c++){int sv=s.rgba()[si+c]&255,dv=out[di+c]&255,p=sv*sa+(dv*da*(255-sa)+127)/255;out[di+c]=(byte)(oa==0?0:(p+oa/2)/oa);}out[di+3]=(byte)oa;}
    }
    private PlayerHeadRenderer(){}
}

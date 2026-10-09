package ddstweak.feature.windowplayerhead;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerHeadRendererTest {
    @Test void rendersFaceAndTransparentHatAt32And64() throws Exception {
        BufferedImage skin=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);for(int y=8;y<16;y++)for(int x=8;x<16;x++)skin.setRGB(x,y,0xffff0000);skin.setRGB(40,8,0xff0000ff);
        Path file=Files.createTempFile("dds-tweak-skin-",".png");try{ImageIO.write(skin,"PNG",file.toFile());PlayerHeadRenderer.Icon[] icons=PlayerHeadRenderer.renderIcons(file);assertNotNull(icons);assertEquals(2,icons.length);assertEquals(32,icons[0].width());assertEquals(64,icons[1].width());assertPixel(icons[0],16,16,255,0,0,255);assertPixel(icons[0],0,0,0,0,255,255);assertPixel(icons[0],31,31,0,0,0,0);}finally{Files.deleteIfExists(file);}
    }
    private static void assertPixel(PlayerHeadRenderer.Icon icon,int x,int y,int r,int g,int b,int a){byte[] p=icon.rgba();int i=(y*icon.width()+x)*4;assertEquals(r,p[i]&255);assertEquals(g,p[i+1]&255);assertEquals(b,p[i+2]&255);assertEquals(a,p[i+3]&255);}
}

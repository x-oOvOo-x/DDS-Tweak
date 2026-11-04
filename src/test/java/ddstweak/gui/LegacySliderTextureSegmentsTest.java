package ddstweak.gui;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class LegacySliderTextureSegmentsTest {
    @Test void normalWidthsUseNativeAtlasCoordinates(){
        for(int width:new int[]{80,196,202}){
            List<int[]> parts=parts(width);
            assertEquals(1,parts.size());
            assertArrayEquals(new int[]{1,0,width-6},parts.get(0));
        }
    }

    @Test void wideBackgroundCoversIdenticalGeometryWithoutSamplingRightCap(){
        for(int width:new int[]{203,206,207,260,394,1024}){
            int nextOffset=1;
            List<int[]> parts=parts(width);
            assertTrue(parts.size()>1);
            for(int[] part:parts){
                assertEquals(nextOffset,part[0],"No background gaps or overlaps");
                assertTrue(part[1]>=0&&part[1]+part[2]<=196,"No UVs beyond the button body");
                assertTrue(part[2]>0);
                nextOffset+=part[2];
            }
            assertEquals(width-5,nextOffset,"Preserve native right cap position");
        }
    }

    @Test void rendererDoesNotReplaceMaLiLibLayoutOrInput() throws Exception {
        String screen=Files.readString(Path.of("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java"));
        assertTrue(screen.contains("new LegacyAtlasSlider(x,y,configWidth,configHeight,callback)"));
        assertTrue(screen.contains("new WidgetSlider(x,y,configWidth,configHeight,callback)"));
        assertTrue(screen.contains("super.addConfigSliderEntry(x,y,resetX,configWidth,configHeight,config)"));
        assertTrue(screen.contains("drawTexturedRect(this.x+this.width-5,this.y,196,46,4,20)"));
        assertFalse(screen.contains("configWidth=Math.min("));
    }

    private static List<int[]> parts(int width){
        List<int[]> result=new ArrayList<>();
        LegacySliderTextureSegments.forEachBackground(width,
                (offset,u,length)->result.add(new int[]{offset,u,length}));
        return result;
    }
}

package ddstweak.feature.cameraplayerselector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CameraOptionStyleTest {
    @Test void selectedHoverRendersAsNormalWhileUnselectedHoverIsPreserved(){
        assertEquals(-1,CameraOptionStyle.renderMouseX(123,true));
        assertEquals(123,CameraOptionStyle.renderMouseX(123,false));
        assertEquals(0,CameraOptionStyle.renderMouseX(0,false));
    }
}

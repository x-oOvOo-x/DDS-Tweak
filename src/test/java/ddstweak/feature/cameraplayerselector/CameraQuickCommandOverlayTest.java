package ddstweak.feature.cameraplayerselector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class CameraQuickCommandOverlayTest {
    @Test void popupTopPaddingIsNotAnExecutableCommandRow() {
        int top=100;
        assertEquals(-1,CameraQuickCommandOverlay.rowIndex(100,top));
        assertEquals(-1,CameraQuickCommandOverlay.rowIndex(101,top));
        assertEquals(0,CameraQuickCommandOverlay.rowIndex(102,top));
        assertEquals(0,CameraQuickCommandOverlay.rowIndex(119,top));
        assertEquals(1,CameraQuickCommandOverlay.rowIndex(120,top));
    }

    @Test void rowIndexKeepsAddRowAndLowerBorderSeparate() {
        int top=100;
        assertEquals(6,CameraQuickCommandOverlay.rowIndex(210,top));
        assertEquals(6,CameraQuickCommandOverlay.rowIndex(227,top));
        assertEquals(7,CameraQuickCommandOverlay.rowIndex(228,top));
    }
}

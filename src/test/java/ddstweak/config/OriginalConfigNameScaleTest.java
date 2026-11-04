package ddstweak.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OriginalConfigNameScaleTest {
    @Test void percentBoundsAndLegacyDoubleAreStable(){
        assertEquals(50,OriginalConfigNameScale.MIN_PERCENT);
        assertEquals(65,OriginalConfigNameScale.DEFAULT_PERCENT);
        assertEquals(100,OriginalConfigNameScale.MAX_PERCENT);
        assertEquals(0.65f,OriginalConfigNameScale.asMultiplier(65));
        assertEquals(1f,OriginalConfigNameScale.asMultiplier(100));
        assertEquals(0.5f,OriginalConfigNameScale.asMultiplier(0));
        assertEquals(64,OriginalConfigNameScale.fromLegacyMultiplier(0.6419));
        assertEquals(65,OriginalConfigNameScale.fromLegacyMultiplier(0.65));
        assertEquals(100,OriginalConfigNameScale.fromLegacyMultiplier(1.0));
        assertTrue(OriginalConfigNameScale.isLegacyMultiplier(0.6419));
        assertFalse(OriginalConfigNameScale.isLegacyMultiplier(65.0));
        assertFalse(OriginalConfigNameScale.isLegacyMultiplier(Double.NaN));
    }

    @Test void nativeSliderIsScopedAndMouseReleaseRemainsWithMalilib() throws Exception {
        String screen=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/gui/DDSTweakConfigScreen.java"));
        assertTrue(screen.contains("new WidgetSlider(x,y,configWidth,configHeight,callback)"));
        assertTrue(screen.contains("delegate.getFormattedDisplayValue()+\"%\""));
        assertTrue(screen.contains("delegate.setValueRelative(value)"));
        assertTrue(screen.contains("addButton(reset,new ConfigOptionListenerResetConfig(config,null,reset,null))"));
        assertFalse(screen.contains("@Override public boolean mouseReleased("));
        assertFalse(screen.contains("onMouseReleasedImpl("));
        assertFalse(screen.contains("onMouseClickedImpl("));
    }
}

package ddstweak.mixin.feature.compactstatuseffects;

//#if MC >= 12102
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the screen bounds used for compact effect positioning.
 * 读取容器界面尺寸，供紧凑效果定位使用。
 */
@Mixin(AbstractContainerScreen.class)
public interface CompactAbstractContainerScreenAccessor {
    @Accessor("leftPos") int ddstweak$getLeftPos();
    @Accessor("topPos") int ddstweak$getTopPos();
    @Accessor("imageWidth") int ddstweak$getImageWidth();
}
//#else
//$$ public interface CompactAbstractContainerScreenAccessor {}
//#endif

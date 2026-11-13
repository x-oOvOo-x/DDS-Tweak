package ddstweak.mixin.feature.bookscroll;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the active Scribble lectern page for optional wheel navigation.
 * 读取可选 Scribble 讲台书本的当前页。
 */
@Pseudo
@Mixin(targets="me.chrr.scribble.screen.ScribbleLecternScreen",remap=false)
public interface ScribbleLecternScreenMarker {
    @Accessor(value="effectivePage",remap=false)
    int ddstweak$effectivePage();
}

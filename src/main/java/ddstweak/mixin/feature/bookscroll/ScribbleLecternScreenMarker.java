package ddstweak.mixin.feature.bookscroll;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets="me.chrr.scribble.screen.ScribbleLecternScreen",remap=false)
public interface ScribbleLecternScreenMarker {
    @Accessor(value="effectivePage",remap=false)
    int ddstweak$effectivePage();
}

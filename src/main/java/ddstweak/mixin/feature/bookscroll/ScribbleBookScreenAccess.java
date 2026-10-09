package ddstweak.mixin.feature.bookscroll;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets="me.chrr.scribble.screen.ScribbleBookScreen",remap=false)
public interface ScribbleBookScreenAccess {
    @Accessor(value="currentPage",remap=false)
    int ddstweak$currentPage();

    @Accessor(value="pagesToShow",remap=false)
    int ddstweak$pagesToShow();

    @Invoker(value="getTotalPages",remap=false)
    int ddstweak$getTotalPages();

    @Invoker(value="showPage",remap=false)
    void ddstweak$showPage(int page,boolean insertIfMissing);

    @Invoker(value="goPageForward",remap=false)
    void ddstweak$goPageForward(boolean toEnd);

    @Invoker(value="goPageBackward",remap=false)
    void ddstweak$goPageBackward(boolean toStart);
}

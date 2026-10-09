package ddstweak.mixin.feature.bookscroll;

import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BookEditScreen.class)
public interface BookEditScreenAccess {
    @Accessor("currentPage") int ddstweak$currentPage();
    @Invoker("getNumPages") int ddstweak$getNumPages();
    @Invoker("pageBack") void ddstweak$pageBack();
    @Invoker("pageForward") void ddstweak$pageForward();

    //#if MC < 12108
    //$$ @Accessor("isSigning") boolean ddstweak$isSigning();
    //#endif
}

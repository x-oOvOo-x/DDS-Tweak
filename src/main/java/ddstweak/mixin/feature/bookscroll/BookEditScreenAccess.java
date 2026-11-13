package ddstweak.mixin.feature.bookscroll;

import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accesses vanilla book editor page navigation for wheel scrolling.
 * 向滚轮翻页提供原版书本编辑器的页码接口。
 */
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

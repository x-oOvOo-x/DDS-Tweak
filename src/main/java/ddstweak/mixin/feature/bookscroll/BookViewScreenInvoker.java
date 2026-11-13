package ddstweak.mixin.feature.bookscroll;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accesses vanilla book viewer page navigation for wheel scrolling.
 * 向滚轮翻页提供原版书本查看器的页码接口。
 */
@Mixin(BookViewScreen.class)
public interface BookViewScreenInvoker {
    @Accessor("currentPage") int ddstweak$currentPage();
    @Invoker("getNumPages") int ddstweak$getNumPages();
    @Invoker("pageBack") void ddstweak$pageBack();
    @Invoker("pageForward") void ddstweak$pageForward();
}

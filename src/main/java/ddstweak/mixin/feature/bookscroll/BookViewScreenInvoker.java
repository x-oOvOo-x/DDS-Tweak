package ddstweak.mixin.feature.bookscroll;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BookViewScreen.class)
public interface BookViewScreenInvoker {
    @Accessor("currentPage") int ddstweak$currentPage();
    @Invoker("getNumPages") int ddstweak$getNumPages();
    @Invoker("pageBack") void ddstweak$pageBack();
    @Invoker("pageForward") void ddstweak$pageForward();
}

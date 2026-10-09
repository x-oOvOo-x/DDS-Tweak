package ddstweak.mixin.feature.bookscroll;

import ddstweak.config.Configs;
import ddstweak.feature.bookscroll.BookScrollState;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface BookScrollContainerEventHandlerMixin {
    //#if MC >= 12003
    @Inject(method="mouseScrolled",at=@At("HEAD"),cancellable=true)
    default void ddstweak$scrollBook(double mouseX,double mouseY,double horizontal,double vertical,CallbackInfoReturnable<Boolean> cir){
        if(ddstweak$handleBookScroll(vertical))cir.setReturnValue(true);
    }
    //#else
    //$$ @Inject(method="mouseScrolled",at=@At("HEAD"),cancellable=true)
    //$$ default void ddstweak$scrollBook(double mouseX,double mouseY,double amount,CallbackInfoReturnable<Boolean> cir){
    //$$     if(ddstweak$handleBookScroll(amount))cir.setReturnValue(true);
    //$$ }
    //#endif

    default boolean ddstweak$handleBookScroll(double amount){
        if(!Configs.Tweaks.SCROLL_BOOK_PAGES.getBooleanValue()||amount==0.0D)return false;

        Object screen=this;
        if(screen instanceof BookViewScreen){
            BookViewScreenInvoker book=(BookViewScreenInvoker)screen;
            ddstweak$scrollBookView(book,BookScrollState.consumeSteps(screen,amount));
            return true;
        }

        if(screen instanceof BookEditScreen){
            BookEditScreenAccess book=(BookEditScreenAccess)screen;
            //#if MC < 12108
            //$$ if(book.ddstweak$isSigning())return false;
            //#endif
            ddstweak$scrollBookEdit(book,BookScrollState.consumeSteps(screen,amount));
            return true;
        }

        //#if MC >= 12000
        if(screen instanceof ScribbleBookScreenAccess book){
            ddstweak$scrollScribble(screen,book,BookScrollState.consumeSteps(screen,amount));
            return true;
        }
        //#endif

        return false;
    }

    default void ddstweak$scrollBookView(BookViewScreenInvoker book,int steps){
        int page=book.ddstweak$currentPage(),pages=book.ddstweak$getNumPages();
        while(steps>0&&page>0){
            book.ddstweak$pageBack();
            page--;
            steps--;
        }
        while(steps<0&&page<pages-1){
            book.ddstweak$pageForward();
            page++;
            steps++;
        }
    }

    default void ddstweak$scrollBookEdit(BookEditScreenAccess book,int steps){
        int page=book.ddstweak$currentPage(),pages=book.ddstweak$getNumPages();
        while(steps>0&&page>0){
            book.ddstweak$pageBack();
            page--;
            steps--;
        }
        while(steps<0&&page<pages-1){
            book.ddstweak$pageForward();
            page++;
            steps++;
        }
    }

    //#if MC >= 12000
    default void ddstweak$scrollScribble(Object screen,ScribbleBookScreenAccess book,int steps){
        if(screen instanceof ScribbleLecternScreenMarker lectern){
            int page=lectern.ddstweak$effectivePage(),pages=book.ddstweak$getTotalPages();
            while(steps>0&&page>0){
                book.ddstweak$goPageBackward(false);
                page--;
                steps--;
            }
            while(steps<0&&page<pages-1){
                book.ddstweak$goPageForward(false);
                page++;
                steps++;
            }
        }else{
            while(steps>0){
                int before=book.ddstweak$currentPage();
                book.ddstweak$showPage(before-1,false);
                if(book.ddstweak$currentPage()==before)break;
                steps--;
            }
            while(steps<0){
                int before=book.ddstweak$currentPage();
                book.ddstweak$showPage(before+Math.max(1,book.ddstweak$pagesToShow()),false);
                if(book.ddstweak$currentPage()==before)break;
                steps++;
            }
        }
    }
    //#endif
}

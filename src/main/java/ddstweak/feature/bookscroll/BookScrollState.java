package ddstweak.feature.bookscroll;

import ddstweak.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.WeakHashMap;

public final class BookScrollState {
    private static final Map<Object,Double> PROGRESS=new WeakHashMap<Object,Double>();

    private BookScrollState(){}

    public static int consumeSteps(Object screen,double amount){
        int multiplier=ddstweak$controlDown()?Configs.Tweaks.SCROLL_BOOK_CTRL_MULTIPLIER.getIntegerValue():1;
        double progress=PROGRESS.containsKey(screen)?PROGRESS.get(screen):0.0D;
        progress+=amount*multiplier;

        int steps=progress>=1.0D?(int)Math.floor(progress):progress<=-1.0D?(int)Math.ceil(progress):0;
        progress-=steps;
        if(Math.abs(progress)<1.0E-7D)PROGRESS.remove(screen);
        else PROGRESS.put(screen,progress);
        return steps;
    }


    private static boolean ddstweak$controlDown(){
        //#if MC >= 12109
        return Minecraft.getInstance().hasControlDown();
        //#else
        //$$ return Screen.hasControlDown();
        //#endif
    }
}

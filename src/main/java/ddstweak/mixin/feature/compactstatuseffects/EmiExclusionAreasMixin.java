package ddstweak.mixin.feature.compactstatuseffects;

//#if MC >= 12101
import ddstweak.feature.compactstatuseffects.CompactStatusEffectBounds;
import ddstweak.feature.compactstatuseffects.CompactStatusEffectsFeature;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="dev.emi.emi.registry.EmiExclusionAreas",remap=false)
public abstract class EmiExclusionAreasMixin {
    @Inject(method="getExclusion",at=@At("RETURN"),cancellable=true,require=0,remap=false)
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void ddstweak$appendCompactEffectArea(@Coerce Object base,CallbackInfoReturnable<List> ci){
        try{
            Method screenMethod=base.getClass().getMethod("screen");
            Object screenObject=screenMethod.invoke(base);
            if(!(screenObject instanceof Screen screen))return;
            CompactStatusEffectBounds area=CompactStatusEffectsFeature.bounds(screen);
            if(area==null)return;
            Class<?> boundsClass=Class.forName("dev.emi.emi.api.widget.Bounds");
            Object bounds=boundsClass.getConstructor(int.class,int.class,int.class,int.class)
                    .newInstance(area.x(),area.y(),area.width(),area.height());
            List result=new ArrayList(ci.getReturnValue());
            result.add(bounds);
            ci.setReturnValue(result);
        }catch(ReflectiveOperationException|LinkageError ignored){}
    }
}
//#else
//$$ public final class EmiExclusionAreasMixin {}
//#endif

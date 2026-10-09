package ddstweak.mixin.feature.compactstatuseffects;

//#if MC >= 12101 && MC < 12102
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectBounds;
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectsAccess;
//$$ import java.util.Collection;
//$$ import java.util.List;
//$$ import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ @Pseudo
//$$ @Mixin(targets="me.shedaniel.rei.plugin.client.exclusionzones.DefaultPotionEffectExclusionZones",remap=false)
//$$ public abstract class ReiPotionEffectExclusionMixin {
//$$     @Inject(method="provide",at=@At("HEAD"),cancellable=true,require=0,remap=false)
//$$     @SuppressWarnings({"rawtypes","unchecked"})
//$$     private void ddstweak$compactEffectArea(EffectRenderingInventoryScreen<?> screen,CallbackInfoReturnable<Collection> ci){
//$$         if(!(screen instanceof CompactStatusEffectsAccess access))return;
//$$         CompactStatusEffectBounds area=access.ddstweak$getCompactStatusEffectBounds();
//$$         if(area==null)return;
//$$         try{
//$$             Class<?> rectangle=Class.forName("me.shedaniel.math.Rectangle");
//$$             Object value=rectangle.getConstructor(int.class,int.class,int.class,int.class)
//$$                     .newInstance(area.x(),area.y(),area.width(),area.height());
//$$             ci.setReturnValue(List.of(value));
//$$         }catch(ReflectiveOperationException|LinkageError ignored){}
//$$     }
//$$ }
//#else
public abstract class ReiPotionEffectExclusionMixin {}
//#endif

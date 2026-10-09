package ddstweak.mixin.feature.compactstatuseffects;

//#if MC >= 12101 && MC < 12102
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectBounds;
//$$ import ddstweak.feature.compactstatuseffects.CompactStatusEffectsAccess;
//$$ import java.util.List;
//$$ import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
//$$ import net.minecraft.client.renderer.Rect2i;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ @Pseudo
//$$ @Mixin(targets="mezz.jei.library.plugins.vanilla.gui.InventoryEffectRendererGuiHandler",remap=false)
//$$ public abstract class JeiInventoryEffectAreaMixin {
//$$     @Inject(method="getGuiExtraAreas",at=@At("HEAD"),cancellable=true,require=0,remap=false)
//$$     private void ddstweak$compactEffectArea(EffectRenderingInventoryScreen<?> screen,CallbackInfoReturnable<List<Rect2i>> ci){
//$$         if(!(screen instanceof CompactStatusEffectsAccess access))return;
//$$         CompactStatusEffectBounds area=access.ddstweak$getCompactStatusEffectBounds();
//$$         if(area!=null)ci.setReturnValue(List.of(new Rect2i(area.x(),area.y(),area.width(),area.height())));
//$$     }
//$$ }
//#else
public abstract class JeiInventoryEffectAreaMixin {}
//#endif

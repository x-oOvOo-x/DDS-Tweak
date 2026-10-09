package ddstweak.mixin.integration.minihud;

import ddstweak.integration.dds.fakeplayer.DdsFakePlayerClientNetwork;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets="fi.dy.masa.minihud.renderer.InventoryOverlayHandler",remap=false)
public abstract class MiniHudInventoryPreviewMixin{
    private static Class<?> contextClass;private static Method inventoryMethod,entityMethod;
    @Inject(method="getTargetInventoryFromEntity",at=@At("RETURN"),require=0,remap=false)
    private void ddsTweak$normalizePlayerPreview(CallbackInfoReturnable<?> cir){if(!DdsFakePlayerClientNetwork.canUseActions())return;Object context=cir.getReturnValue();if(context==null)return;try{Class<?> type=context.getClass();if(contextClass!=type||inventoryMethod==null||entityMethod==null){inventoryMethod=type.getMethod("inv");entityMethod=type.getMethod("entity");contextClass=type;}Object entity=entityMethod.invoke(context),value=inventoryMethod.invoke(context);if(!(entity instanceof Player)||!(value instanceof Container)||value instanceof Inventory)return;Container inventory=(Container)value;if(inventory.getContainerSize()!=36)return;ItemStack[] copy=new ItemStack[36];for(int i=0;i<36;i++)copy[i]=inventory.getItem(i);for(int i=0;i<27;i++)inventory.setItem(i,copy[i+9]);for(int i=0;i<9;i++)inventory.setItem(27+i,copy[i]);inventory.setChanged();}catch(ReflectiveOperationException|LinkageError ignored){}}
}

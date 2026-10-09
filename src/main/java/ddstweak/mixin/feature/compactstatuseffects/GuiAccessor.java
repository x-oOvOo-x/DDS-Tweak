package ddstweak.mixin.feature.compactstatuseffects;

//#if MC >= 12101 && MC < 12102
//$$ import net.minecraft.client.gui.Gui;
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ @Mixin(Gui.class)
//$$ public interface GuiAccessor {
//$$     @Accessor("EFFECT_BACKGROUND_SPRITE")
//$$     static ResourceLocation ddstweak$getEffectBackgroundSprite(){throw new AssertionError();}
//$$ }
//#else
public interface GuiAccessor {}
//#endif

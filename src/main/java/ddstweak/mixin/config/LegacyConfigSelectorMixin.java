//#if MC <= 12006
//$$ package ddstweak.mixin.config;
//$$
//$$ import ddstweak.platform.malilib.LegacyConfigScreenRegistry;
//$$ import ddstweak.platform.minecraft.MinecraftCompat;
//$$ import ddstweak.platform.malilib.LegacyConfigScreenRegistry.Entry;
//$$ import fi.dy.masa.malilib.gui.GuiBase;
//$$ import fi.dy.masa.malilib.gui.GuiConfigsBase;
//$$ import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
//$$ import net.minecraft.client.Minecraft;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import java.util.List;
//$$
//$$ @Mixin(value=GuiBase.class,remap=false)
//$$ public abstract class LegacyConfigSelectorMixin {
//$$     @Inject(method="initGui",at=@At("RETURN"),remap=false)
//$$     private void ddstweak$addLegacyConfigSelector(CallbackInfo ci){
//$$         if(!((Object)this instanceof GuiConfigsBase))return;GuiConfigsBase gui=(GuiConfigsBase)(Object)this;List<Entry> entries=LegacyConfigScreenRegistry.entries();
//$$         Entry current=LegacyConfigScreenRegistry.find(gui.getModId()).orElse(null);if(current==null||entries.size()<2)return;
//$$         Selector selector=new Selector(entries,gui);selector.setSelectedEntry(current);
//$$         selector.setPosition(MinecraftCompat.guiScaledWidth(Minecraft.getInstance())-selector.getWidth()-10,10);gui.addWidget(selector);
//$$     }
//$$
//$$     private static final class Selector extends WidgetDropDownList<Entry>{
//$$         private final GuiConfigsBase owner;
//$$         Selector(List<Entry> entries,GuiConfigsBase owner){super(0,10,-1,16,128,7,entries);this.owner=owner;}
//$$         @Override protected void setSelectedEntry(int index){
//$$             Entry before=getSelectedEntry();super.setSelectedEntry(index);Entry after=getSelectedEntry();
//$$             if(after!=null&&!after.equals(before)&&!after.modId().equals(owner.getModId()))LegacyConfigScreenRegistry.open(after,owner.getParent());
//$$         }
//$$     }
//$$ }
//#endif

package ddstweak.mixin.feature.chattooltip;

import ddstweak.feature.chattooltip.ChatTooltipOcclusionFeature;
import net.minecraft.client.gui.Font;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import net.minecraft.client.gui.screens.Screen;
//#if MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#else
//$$ import net.minecraft.network.chat.Component;
//#endif
//#endif
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 260100
//$$ @Mixin(GuiGraphicsExtractor.class)
//#elseif MC >= 12000
@Mixin(GuiGraphics.class)
//#else
//$$ @Mixin(Screen.class)
//#endif
public abstract class ChatTooltipRenderMixin {
    //#if MC >= 260100
    //$$ @Inject(method="componentHoverEffect",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$occludeChatTooltip(Font font,Style style,int mouseX,int mouseY,CallbackInfo ci){
    //$$     if(ChatTooltipOcclusionFeature.shouldSuppress(mouseX,mouseY))ci.cancel();
    //$$ }
    //#elseif MC >= 12000
    @Inject(method="renderComponentHoverEffect",at=@At("HEAD"),cancellable=true)
    private void ddstweak$occludeChatTooltip(Font font,Style style,int mouseX,int mouseY,CallbackInfo ci){
        if(ChatTooltipOcclusionFeature.shouldSuppress(mouseX,mouseY))ci.cancel();
    }
    //#elseif MC >= 11600
    //$$ @Inject(method="renderComponentHoverEffect",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$occludeChatTooltip(PoseStack pose,Style style,int mouseX,int mouseY,CallbackInfo ci){
    //$$     if(ChatTooltipOcclusionFeature.shouldSuppress(mouseX,mouseY))ci.cancel();
    //$$ }
    //#else
    //$$ @Inject(method="renderComponentHoverEffect",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$occludeChatTooltip(Component component,int mouseX,int mouseY,CallbackInfo ci){
    //$$     if(ChatTooltipOcclusionFeature.shouldSuppress(mouseX,mouseY))ci.cancel();
    //$$ }
    //#endif
}

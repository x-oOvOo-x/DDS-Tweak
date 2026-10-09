package ddstweak.feature.chattooltip;

import ddstweak.config.Configs;
import ddstweak.mixin.feature.chattooltip.ChatScreenAccessor;
import ddstweak.mixin.feature.chattooltip.CommandSuggestionsAccessor;
import ddstweak.mixin.feature.chattooltip.SuggestionsListAccessor;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

public final class ChatTooltipOcclusionFeature {
    private ChatTooltipOcclusionFeature(){}

    public static boolean shouldSuppress(int mouseX,int mouseY){
        if(!Configs.Tweaks.CHAT_TOOLTIP_OCCLUSION.getBooleanValue())return false;
        Screen screen=MinecraftCompat.screen(Minecraft.getInstance());
        if(!(screen instanceof ChatScreen))return false;

        CommandSuggestions commandSuggestions=((ChatScreenAccessor)screen).ddstweak$commandSuggestions();
        if(commandSuggestions==null)return false;
        CommandSuggestions.SuggestionsList suggestions=((CommandSuggestionsAccessor)(Object)commandSuggestions).ddstweak$suggestions();
        if(suggestions==null)return false;

        Rect2i rect=((SuggestionsListAccessor)(Object)suggestions).ddstweak$rect();
        return rect!=null&&rect.contains(mouseX,mouseY);
    }
}

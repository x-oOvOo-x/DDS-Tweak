package ddstweak.feature.clickthrough;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
//#if MC >= 12000
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.block.entity.SignText;
//#else
//$$ import net.minecraft.network.chat.ClickEvent;
//$$ import net.minecraft.network.chat.Component;
//#endif
//#if MC >= 12100
import net.minecraft.core.component.DataComponents;
//#else
//$$ import net.minecraft.world.item.RecordItem;
//#endif
//#if MC >= 12100 && MC < 12103
//$$ import net.minecraft.world.item.crafting.RecipeType;
//$$ import net.minecraft.world.item.crafting.SingleRecipeInput;
//#endif
//#if MC < 12100
//$$ import net.minecraft.world.SimpleContainer;
//$$ import net.minecraft.world.item.crafting.RecipeType;
//#endif
//#if MC >= 12103
import net.minecraft.world.item.crafting.RecipePropertySet;
//#endif
//#if MC >= 260300
//$$ import net.minecraft.tags.ItemTags;
//$$ import net.minecraft.world.level.block.entity.SignTextSlot;
//#endif

final class ClickThroughCompat {
    private ClickThroughCompat(){}

    static boolean isJukeboxPlayable(ItemStack stack){
        //#if MC >= 12100
        return !stack.isEmpty()&&stack.get(DataComponents.JUKEBOX_PLAYABLE)!=null;
        //#else
        //$$ return !stack.isEmpty()&&stack.getItem() instanceof RecordItem;
        //#endif
    }

    static boolean isCompostable(ItemStack stack){
        //#if MC >= 260300
        //$$ return !stack.isEmpty()&&stack.has(DataComponents.COMPOSTABLE);
        //#else
        return !stack.isEmpty()&&ComposterBlock.COMPOSTABLES.containsKey(stack.getItem());
        //#endif
    }

    static boolean isCampfireInput(ClientLevel level,ItemStack stack){
        if(stack.isEmpty())return false;
        //#if MC >= 12103
        return level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(stack);
        //#elseif MC >= 12100
        //$$ return level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING,new SingleRecipeInput(stack),level).isPresent();
        //#else
        //$$ return level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING,new SimpleContainer(stack),level).isPresent();
        //#endif
    }

    static boolean isCampfireDousingItem(ItemStack stack){
        //#if MC >= 260300
        //$$ return !stack.isEmpty()&&stack.is(ItemTags.DOUSES_CAMPFIRES);
        //#else
        return false;
        //#endif
    }

    static boolean hasSignClickCommand(SignBlockEntity sign,LocalPlayer player){
        //#if MC >= 260300
        //$$ SignTextSlot slot=sign.getSlotPlayerIsFacing(player);
        //$$ return sign.canExecuteClickCommands(slot,player);
        //#elseif MC >= 12000
        boolean front=sign.isFacingFrontText(player);
        return sign.canExecuteClickCommands(front,player);
        //#else
        //$$ for(int line=0;line<4;line++){
        //#if MC >= 11700
        //$$     if(hasRunCommand(sign.getMessage(line,false)))return true;
        //#else
        //$$     if(hasRunCommand(sign.getMessage(line)))return true;
        //#endif
        //$$ }
        //$$ return false;
        //#endif
    }

    static boolean canApplyToSign(SignBlockEntity sign,LocalPlayer player,ItemStack stack){
        if(stack.isEmpty())return false;

        //#if MC >= 12000
        if(sign.isWaxed()||!(stack.getItem() instanceof SignApplicator))return false;

        //#if MC >= 260300
        //$$ SignTextSlot slot=sign.getSlotPlayerIsFacing(player);
        //$$ SignText text=sign.getText(slot);
        //#else
        boolean front=sign.isFacingFrontText(player);
        SignText text=sign.getText(front);
        //#endif

        SignApplicator applicator=(SignApplicator)stack.getItem();
        //#if MC >= 260100
        //$$ if(!applicator.canApplyToSign(text,stack,player))return false;
        //#else
        if(!applicator.canApplyToSign(text,player))return false;
        //#endif

        return signActionChangesState(text,stack);
        //#else
        //$$ if(stack.getItem() instanceof DyeItem)return sign.getColor()!=((DyeItem)stack.getItem()).getDyeColor();
        //#if MC >= 11700
        //$$ if(stack.getItem()==Items.GLOW_INK_SAC)return !sign.hasGlowingText();
        //$$ if(stack.getItem()==Items.INK_SAC)return sign.hasGlowingText();
        //#endif
        //$$ return false;
        //#endif
    }

    //#if MC >= 12000
    private static boolean signActionChangesState(SignText text,ItemStack stack){
        if(stack.getItem()==Items.GLOW_INK_SAC)return !text.hasGlowingText();
        if(stack.getItem()==Items.INK_SAC)return text.hasGlowingText();
        if(stack.getItem() instanceof DyeItem){
            //#if MC >= 260100
            //$$ DyeColor dyeColor=stack.get(DataComponents.DYE);
            //#else
            DyeColor dyeColor=((DyeItem)stack.getItem()).getDyeColor();
            //#endif
            return dyeColor!=null&&text.getColor()!=dyeColor;
        }
        // Honeycomb and third-party SignApplicators keep their own canApplyToSign contract.
        return true;
    }
    //#else
    //$$ private static boolean hasRunCommand(Component component){
    //$$     ClickEvent event=component.getStyle().getClickEvent();
    //$$     if(event!=null&&event.getAction()==ClickEvent.Action.RUN_COMMAND)return true;
    //$$     for(Component sibling:component.getSiblings()){
    //$$         if(hasRunCommand(sibling))return true;
    //$$     }
    //$$     return false;
    //$$ }
    //#endif
}

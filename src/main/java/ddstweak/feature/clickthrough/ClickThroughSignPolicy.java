package ddstweak.feature.clickthrough;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

final class ClickThroughSignPolicy {
    private ClickThroughSignPolicy(){}

    static boolean shouldKeepTarget(ClientLevel level,LocalPlayer player,BlockPos pos,ItemStack stack){
        BlockEntity blockEntity=level.getBlockEntity(pos);
        if(!(blockEntity instanceof SignBlockEntity))return false;

        SignBlockEntity sign=(SignBlockEntity)blockEntity;
        return ClickThroughCompat.hasSignClickCommand(sign,player)
                ||ClickThroughCompat.canApplyToSign(sign,player,stack);
    }
}

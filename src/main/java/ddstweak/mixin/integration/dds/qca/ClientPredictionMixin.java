package ddstweak.mixin.integration.dds.qca;

import ddstweak.integration.dds.qca.DdsQcaClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
//#if MC >= 260102
//$$ import net.minecraft.world.inventory.ContainerInput;
//#else
import net.minecraft.world.inventory.ClickType;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPredictionMixin {
    //#if MC >= 260102
    //$$ @Redirect(method="handleContainerInput",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"))
    //$$ private void ddsTweak$suppressQcaPrediction(AbstractContainerMenu menu,int slotNum,int buttonNum,ContainerInput clickType,Player player){if(DdsQcaClient.shouldSuppressPrediction(menu,slotNum,buttonNum,clickType,player))return;menu.clicked(slotNum,buttonNum,clickType,player);}
    //#else
    @Redirect(method="handleInventoryMouseClick",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))
    private void ddsTweak$suppressQcaPrediction(AbstractContainerMenu menu,int slotNum,int buttonNum,ClickType clickType,Player player){if(DdsQcaClient.shouldSuppressPrediction(menu,slotNum,buttonNum,clickType,player))return;menu.clicked(slotNum,buttonNum,clickType,player);}
    //#endif
}

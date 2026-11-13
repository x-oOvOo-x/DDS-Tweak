//#if MC >= 260000
//$$ package ddstweak.mixin.feature.carpetrules;
//$$ import ddstweak.feature.carpetrules.VanillaGameRules;
//$$ import net.minecraft.client.multiplayer.ClientPacketListener;
//$$ import net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ 
//$$ @Mixin(ClientPacketListener.class)
//$$ public abstract class VanillaGameRulePacketMixin {
//$$     /** Observe but do not cancel vanilla's handler. */
//$$     @Inject(method="handleGameRuleValues",at=@At("TAIL"),require=1)
//$$     private void ddstweak$readGameRuleValues(ClientboundGameRuleValuesPacket packet,CallbackInfo ci){
//$$         VanillaGameRules.accept((ClientPacketListener)(Object)this,packet.values());
//$$     }
//$$ }
//#else
package ddstweak.mixin.feature.carpetrules;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
/** Kept annotated for Mixin discovery; config plugin disables this on legacy targets. */
@Mixin(ClientPacketListener.class)
public abstract class VanillaGameRulePacketMixin {}
//#endif

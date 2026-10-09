package ddstweak.integration.dds.qca;

import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.nbt.CompoundTag;
//#if MC >= 12005
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
//#endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Full-fidelity ItemStack transport for QCA Creative cursor transactions. */
public final class DdsQcaStackCodec {
    private DdsQcaStackCodec(){}

    public static void put(CompoundTag payload,String key,Player player,ItemStack stack){
        if(stack==null||stack.isEmpty()){payload.putInt(key+"_empty",1);return;}
        //#if MC >= 12005
        RegistryOps<Tag> ops=RegistryOps.create(NbtOps.INSTANCE,player.level().registryAccess());
        Tag encoded=ItemStack.CODEC.encodeStart(ops,stack).result().orElse(null);
        if(encoded!=null)payload.put(key,encoded);
        //#else
        //$$ payload.put(key,stack.save(new CompoundTag()));
        //#endif
    }

    public static ItemStack get(CompoundTag payload,String key,Player player){
        if(MinecraftCompat.intOr(payload,key+"_empty",0)!=0)return ItemStack.EMPTY;
        //#if MC >= 12005
        Tag encoded=payload.get(key);
        if(encoded==null)return ItemStack.EMPTY;
        RegistryOps<Tag> ops=RegistryOps.create(NbtOps.INSTANCE,player.level().registryAccess());
        return ItemStack.CODEC.parse(ops,encoded).result().orElse(ItemStack.EMPTY);
        //#else
        //$$ return payload.contains(key)?ItemStack.of(payload.getCompound(key)):ItemStack.EMPTY;
        //#endif
    }
}

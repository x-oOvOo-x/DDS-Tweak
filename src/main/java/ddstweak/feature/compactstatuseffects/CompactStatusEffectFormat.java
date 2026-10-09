package ddstweak.feature.compactstatuseffects;

//#if MC >= 12101
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.world.effect.MobEffectInstance;

public final class CompactStatusEffectFormat {
    private CompactStatusEffectFormat(){}

    public static String levelBadge(MobEffectInstance effect){
        int level=effect.getAmplifier()+1;
        return level<=1?"":Integer.toString(level);
    }

    public static String displayName(MobEffectInstance effect){
        String name=effect.getEffect().value().getDisplayName().getString();
        int amplifier=effect.getAmplifier();
        return amplifier<=0?name:name+" "+MinecraftCompat.translatable("potion.potency."+amplifier).getString();
    }

    public static String durationText(MobEffectInstance effect){
        int ticks=effect.getDuration();
        return ticks<0?"∞":String.format("%d:%02d",(ticks/20)/60,(ticks/20)%60);
    }

    public static String durationLine(MobEffectInstance effect){
        return MinecraftCompat.translatable("dds-tweak.compact_status_effects.remaining",durationText(effect)).getString();
    }
}
//#else
//$$ public final class CompactStatusEffectFormat {private CompactStatusEffectFormat(){}}
//#endif

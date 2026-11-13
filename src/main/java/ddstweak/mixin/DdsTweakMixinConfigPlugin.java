package ddstweak.mixin;

import ddstweak.network.ownership.DdsLocalOwnershipDecision;
import ddstweak.network.ownership.DdsLocalOwnershipResolver;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Early ownership gate: a valid DDS marker hands logical client features to Tweak while DDS may retain wire registration.
 * 按模组安装状态和 DDS 本地接管权启用可选 Mixin。
 */
public final class DdsTweakMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Set<String> CODEC_MIXINS=Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("ddstweak.mixin.network.ClientboundCustomPayloadPacketMixin","ddstweak.mixin.network.ServerboundCustomPayloadPacketMixin")));
    private static final String DDS_FEATURE_PREFIX="ddstweak.mixin.integration.dds.";
    @Override public void onLoad(String mixinPackage){}
    @Override public String getRefMapperConfig(){return null;}
    @Override public boolean shouldApplyMixin(String targetClassName,String mixinClassName){
        if(mixinClassName.endsWith(".carpetrules.VanillaGameRulePacketMixin")){
            String game=FabricLoader.getInstance().getModContainer("minecraft")
                    .map(m->m.getMetadata().getVersion().getFriendlyString()).orElse("");
            return game.startsWith("26.");
        }
        if(mixinClassName.endsWith("CarpetRuleClientboundPayloadMixin")||mixinClassName.endsWith("CarpetRuleServerboundPayloadMixin")){
            String game=FabricLoader.getInstance().getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString()).orElse("");
            return !FabricLoader.getInstance().isModLoaded("carpet")&&!game.startsWith("1.14.")&&!game.startsWith("1.15.")&&!game.startsWith("1.16.")&&!game.startsWith("1.17.")&&!game.startsWith("1.18.")&&!game.startsWith("1.19.")&&!game.startsWith("1.20.1");
        }
        if(mixinClassName.startsWith("ddstweak.mixin.integration.ommc.")){
            boolean modern=mixinClassName.contains("Modern");
            boolean fallback=mixinClassName.endsWith("FallbackMixin");
            boolean tupleAdapter=mixinClassName.endsWith("TupleAdapterMixin");
            String target=modern
                    ?ddstweak.mixin.integration.ommc.OmmcSortTargetProbe.MODERN
                    :ddstweak.mixin.integration.ommc.OmmcSortTargetProbe.LEGACY;
            return ddstweak.mixin.integration.ommc.OmmcSortTargetProbe.shouldApply(target,fallback,tupleAdapter);
        }
        if(mixinClassName.endsWith(".qca.LegacyCreativeScreenInputMixin")){String mc=FabricLoader.getInstance().getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString()).orElse("");if(!(mc.startsWith("1.14.")||mc.startsWith("1.15.")||mc.startsWith("1.16.")))return false;}if(mixinClassName.contains(".feature.locatorheads.")){
        String game=FabricLoader.getInstance().getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString()).orElse("");
        if(FabricLoader.getInstance().isModLoaded("locator-heads")||
                !(game.equals("1.21.8")||game.equals("1.21.9")||game.equals("1.21.10")||game.equals("1.21.11")||game.equals("26.1.2")||game.equals("26.2")||game.equals("26.3")))return false;
    }DdsLocalOwnershipDecision d=DdsLocalOwnershipResolver.current();if(d.disableTweakCodecMixins()&&CODEC_MIXINS.contains(mixinClassName))return false;if(mixinClassName.startsWith(DDS_FEATURE_PREFIX)&&(mixinClassName.contains(".qca.")||mixinClassName.contains(".undoredo.")))return d.tweakOwnsFeatureClient();return true;}
    @Override public void acceptTargets(Set<String> myTargets,Set<String> otherTargets){}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String targetClassName,ClassNode targetClass,String mixinClassName,IMixinInfo mixinInfo){}
    @Override public void postApply(String targetClassName,ClassNode targetClass,String mixinClassName,IMixinInfo mixinInfo){}
}

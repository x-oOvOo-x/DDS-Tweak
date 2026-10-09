package ddstweak.mixin;

import ddstweak.network.ownership.DdsLocalOwnershipDecision;
import ddstweak.network.ownership.DdsLocalOwnershipResolver;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.*;

/** Early ownership gate: a valid DDS marker hands logical client features to Tweak while DDS may retain wire registration. */
public final class DdsTweakMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Set<String> CODEC_MIXINS=Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("ddstweak.mixin.network.ClientboundCustomPayloadPacketMixin","ddstweak.mixin.network.ServerboundCustomPayloadPacketMixin")));
    private static final String DDS_FEATURE_PREFIX="ddstweak.mixin.integration.dds.";
    @Override public void onLoad(String mixinPackage){}
    @Override public String getRefMapperConfig(){return null;}
    @Override public boolean shouldApplyMixin(String targetClassName,String mixinClassName){DdsLocalOwnershipDecision d=DdsLocalOwnershipResolver.current();if(d.disableTweakCodecMixins()&&CODEC_MIXINS.contains(mixinClassName))return false;if(mixinClassName.startsWith(DDS_FEATURE_PREFIX)&&(mixinClassName.contains(".qca.")||mixinClassName.contains(".undoredo.")))return d.tweakOwnsFeatureClient();return true;}
    @Override public void acceptTargets(Set<String> myTargets,Set<String> otherTargets){}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String targetClassName,ClassNode targetClass,String mixinClassName,IMixinInfo mixinInfo){}
    @Override public void postApply(String targetClassName,ClassNode targetClass,String mixinClassName,IMixinInfo mixinInfo){}
}

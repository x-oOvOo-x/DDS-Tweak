package ddstweak.network.ownership;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;

/** Loader-safe local DDS ownership metadata resolver. */
public final class DdsLocalOwnershipResolver {
    public static final String DDS_MOD_ID="carpet-dds-addition",OWNERSHIP_KEY="dds:network_ownership";
    private static volatile DdsLocalOwnershipDecision cached;
    private DdsLocalOwnershipResolver(){}
    public static DdsLocalOwnershipDecision current(){DdsLocalOwnershipDecision v=cached;if(v!=null)return v;synchronized(DdsLocalOwnershipResolver.class){if(cached==null)cached=resolve();return cached;}}
    private static DdsLocalOwnershipDecision resolve(){
        ModContainer mod=FabricLoader.getInstance().getModContainer(DDS_MOD_ID).orElse(null);
        if(mod==null)return DdsLocalOwnershipDecision.TWEAK_STANDALONE;
        CustomValue raw=mod.getMetadata().getCustomValue(OWNERSHIP_KEY);
        if(raw==null)return DdsLocalOwnershipDecision.LEGACY_COMPAT;
        try{
            CustomValue.CvObject obj=raw.getAsObject();
            CustomValue c=obj.get("contract"),o=obj.get("logical_client_owner"),w=obj.get("wire_mode");
            Integer contract=c==null?null:c.getAsNumber().intValue();
            return DdsLocalOwnershipDecision.fromMetadata(contract,o==null?null:o.getAsString(),w==null?null:w.getAsString());
        }catch(RuntimeException ex){return DdsLocalOwnershipDecision.UNSUPPORTED_LOCAL_DDS;}
    }
}

package ddstweak.network.ownership;

/** Local co-install ownership state. Valid markers activate Tweak logical-client ownership while DDS may retain wire registration. */
public enum DdsLocalOwnershipDecision {
    TWEAK_STANDALONE(false,false,false),
    LEGACY_COMPAT(true,false,true),
    HANDOFF_LEGACY_CUSTOM_PAYLOAD(true,true,false),
    HANDOFF_DDS_PAYLOAD_READER_MAP(true,true,true),
    HANDOFF_DDS_PAYLOAD_CODEC(true,true,true),
    UNSUPPORTED_LOCAL_DDS(true,false,false);

    private final boolean localDds,handoff,disableTweakCodecMixins;
    DdsLocalOwnershipDecision(boolean localDds,boolean handoff,boolean disableTweakCodecMixins){this.localDds=localDds;this.handoff=handoff;this.disableTweakCodecMixins=disableTweakCodecMixins;}
    public boolean localDdsPresent(){return localDds;}public boolean handoffActive(){return handoff;}public boolean disableTweakCodecMixins(){return disableTweakCodecMixins;}public boolean tweakOwnsFeatureClient(){return this==TWEAK_STANDALONE||handoff;}
    public static DdsLocalOwnershipDecision fromMetadata(Integer contract,String owner,String wireMode){
        if(contract==null||contract!=1||owner==null||wireMode==null)return UNSUPPORTED_LOCAL_DDS;
        if(!"tweak".equalsIgnoreCase(owner.trim()))return UNSUPPORTED_LOCAL_DDS;
        String mode=wireMode.trim().toUpperCase();
        if("LEGACY_CUSTOM_PAYLOAD".equals(mode))return HANDOFF_LEGACY_CUSTOM_PAYLOAD;
        if("DDS_PAYLOAD_READER_MAP".equals(mode))return HANDOFF_DDS_PAYLOAD_READER_MAP;
        if("DDS_PAYLOAD_CODEC".equals(mode))return HANDOFF_DDS_PAYLOAD_CODEC;
        return UNSUPPORTED_LOCAL_DDS;
    }
}

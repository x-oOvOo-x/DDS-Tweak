package ddstweak.network.transport;

/** Selects the DDS client wire transport for the current Minecraft protocol generation. */
public final class DdsClientTransports {
    //#if MC >= 12002
    private static final DdsClientTransport CURRENT=new PayloadCodecClientTransport();
    //#else
    //$$ private static final DdsClientTransport CURRENT=new LegacyCustomPayloadClientTransport();
    //#endif

    private DdsClientTransports(){}

    public static DdsClientTransport current(){return CURRENT;}
}

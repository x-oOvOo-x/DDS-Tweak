package ddstweak.network.transport;

/**
 * Selects the DDS client wire transport for the current Minecraft protocol generation.
 * 根据当前协议代际选用网络传输实现。
 */
public final class DdsClientTransports {
    //#if MC >= 12002
    private static final DdsClientTransport CURRENT=new PayloadCodecClientTransport();
    //#else
    //$$ private static final DdsClientTransport CURRENT=new LegacyCustomPayloadClientTransport();
    //#endif

    private DdsClientTransports(){}

    public static DdsClientTransport current(){return CURRENT;}
}

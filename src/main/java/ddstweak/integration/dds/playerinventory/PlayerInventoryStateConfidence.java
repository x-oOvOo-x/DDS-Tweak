package ddstweak.integration.dds.playerinventory;

/**
 * Describes how strongly the client knows a displayed action state.
 * 表示客户端显示的操作状态有多可靠。
 */
public enum PlayerInventoryStateConfidence {
    /** Provider/server state was observed directly. */
    AUTHORITATIVE,
    /** Value reflects the most recent command issued by this client only. */
    LOCAL,
    /** No trustworthy current value is available. */
    UNKNOWN;

    public boolean outranks(PlayerInventoryStateConfidence other){
        return priority()>(other==null?0:other.priority());
    }
    private int priority(){
        switch(this){
            case AUTHORITATIVE:return 2;
            case LOCAL:return 1;
            default:return 0;
        }
    }
}

package ddstweak.integration.dds.playerinventory;

public final class PlayerInventoryBehaviorV1{
    public static final String ID="player_inventory_behavior_v1";
    public static final int MODE_NONE=0,MODE_INTERVAL=1,MODE_CONTINUOUS=2,DEFAULT_INTERVAL=12,MIN_INTERVAL=1,MAX_INTERVAL=Integer.MAX_VALUE;
    private PlayerInventoryBehaviorV1(){}
    public static int clampInterval(int value){return Math.max(MIN_INTERVAL,value);}
}

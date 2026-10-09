package ddstweak.integration.dds.playerinventory;

public enum PlayerInventoryAction{
    SELECT_HOTBAR,ATTACK_ONCE,ATTACK_INTERVAL,ATTACK_CONTINUOUS,STOP_ATTACK,USE_ONCE,USE_INTERVAL,USE_CONTINUOUS,STOP_USE,STOP_ALL,
    DROP_ALL,ENDER_DROP_ALL,ENDER_STORE_ALL,ENDER_TAKE_ALL,TOGGLE_SNEAK,DISCONNECT;

    public boolean allowedIn(PlayerInventoryKind kind){
        if(kind==null)return false;
        switch(this){
            case ENDER_DROP_ALL:
            case ENDER_STORE_ALL:
            case ENDER_TAKE_ALL:
            case TOGGLE_SNEAK:
                return kind==PlayerInventoryKind.ENDER_CHEST;
            case DISCONNECT:
                return true;
            default:
                return kind==PlayerInventoryKind.INVENTORY||kind==PlayerInventoryKind.CONTROLLER;
        }
    }
}

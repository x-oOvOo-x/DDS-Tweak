package ddstweak.integration.dds.playerinventory;

/**
 * Lists player-inventory actions subject to server authorization.
 * 定义需要服务端授权的玩家背包操作。
 */
public enum PlayerInventoryAction{
    SELECT_HOTBAR,ATTACK_ONCE,ATTACK_INTERVAL,ATTACK_CONTINUOUS,STOP_ATTACK,USE_ONCE,USE_INTERVAL,USE_CONTINUOUS,STOP_USE,STOP_ALL,
    DROP_ALL,ENDER_DROP_ALL,ENDER_STORE_ALL,ENDER_TAKE_ALL,TOGGLE_SNEAK,DISCONNECT;

    /**
     * Ordinary Carpet commands are authorized by the server, not by fake-player identity.
     * 普通 Carpet 指令由服务端授权，不依赖假人身份。
     */
    public boolean allowedWithoutFakeIdentity(){
        switch(this){
            case SELECT_HOTBAR:
            case ATTACK_ONCE: case ATTACK_INTERVAL: case ATTACK_CONTINUOUS: case STOP_ATTACK:
            case USE_ONCE: case USE_INTERVAL: case USE_CONTINUOUS: case STOP_USE:
            case STOP_ALL:
            case TOGGLE_SNEAK: // Carpet's sneak/unsneak uses normal server-side command permission checks.
            case DROP_ALL: // Carpet checks the caller's permission even for a real target.
            case DISCONNECT: // Carpet's kill rejects real-player targets on the server.
                return true;
            default: return false;
        }
    }

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

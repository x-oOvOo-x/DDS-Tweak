package ddstweak.playerinventory;

import ddstweak.integration.dds.playerinventory.PlayerInventoryAction;

/** Mirrors /player commands sent by this client into the non-authoritative local prediction cache. */
public final class PlayerInventoryCommandStateObserver {
    private PlayerInventoryCommandStateObserver(){}

    public static void observeCommand(String command){
        if(command==null)return;
        String value=command.trim();
        while(value.startsWith("/"))value=value.substring(1).trim();
        if(value.isEmpty())return;
        String[] parts=value.split("\\s+");
        if(parts.length<3||!"player".equals(parts[0]))return;

        PlayerInventoryClientActionState.State state=PlayerInventoryClientActionState.forTargetName(parts[1]);
        if(state==null)return;
        String action=parts[2];
        if("stop".equals(action)){if(parts.length==3)state.record(PlayerInventoryAction.STOP_ALL,0);return;}
        if("attack".equals(action)){observeAction(state,parts,PlayerInventoryAction.ATTACK_INTERVAL,PlayerInventoryAction.ATTACK_CONTINUOUS,PlayerInventoryAction.ATTACK_ONCE,PlayerInventoryAction.STOP_ATTACK);return;}
        if("use".equals(action)){observeAction(state,parts,PlayerInventoryAction.USE_INTERVAL,PlayerInventoryAction.USE_CONTINUOUS,PlayerInventoryAction.USE_ONCE,PlayerInventoryAction.STOP_USE);return;}
        if("hotbar".equals(action)&&parts.length==4){
            Integer slot=positiveInt(parts[3]);if(slot!=null&&slot<=9)state.record(PlayerInventoryAction.SELECT_HOTBAR,slot);return;
        }
        if("sneak".equals(action)&&parts.length==3)state.record(PlayerInventoryAction.TOGGLE_SNEAK,1);
        else if("unsneak".equals(action)&&parts.length==3)state.record(PlayerInventoryAction.TOGGLE_SNEAK,0);
    }

    private static void observeAction(PlayerInventoryClientActionState.State state,String[] parts,
                                      PlayerInventoryAction interval,PlayerInventoryAction continuous,
                                      PlayerInventoryAction once,PlayerInventoryAction stop){
        if(parts.length<4)return;
        switch(parts[3]){
            case "continuous":if(parts.length==4)state.record(continuous,0);break;
            case "once":if(parts.length==4)state.record(once,0);break;
            case "stop":if(parts.length==4)state.record(stop,0);break;
            case "interval":
                if(parts.length==5){Integer ticks=positiveInt(parts[4]);if(ticks!=null)state.record(interval,ticks);}
                break;
            default:break;
        }
    }

    private static Integer positiveInt(String value){
        try{int parsed=Integer.parseInt(value);return parsed>0?parsed:null;}
        catch(NumberFormatException ignored){return null;}
    }
}

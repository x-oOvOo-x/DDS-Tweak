package ddstweak.integration.dds.playerinventory;

/** Half-open menu-slot sort domains; invalid and non-storage roles are never sortable. */
public final class PlayerInventorySortRanges{
    private PlayerInventorySortRanges(){}

    public static int[] range(PlayerInventorySlotRole role,int hovered,
                              int mainStart,int mainEnd,int hotbarStart,int hotbarEnd,
                              int viewerMainStart,int viewerHotbarStart,int viewerEnd){
        int start,end;
        switch(role){
            case MAIN:
            case ENDER_STORAGE:
                start=mainStart;end=mainEnd;break;
            case HOTBAR:
                start=hotbarStart;end=hotbarEnd;break;
            case VIEWER_MAIN:
                start=viewerMainStart;end=viewerHotbarStart;break;
            case VIEWER_HOTBAR:
                start=viewerHotbarStart;end=viewerEnd;break;
            default:
                return null;
        }
        return start>=0&&end-start>1&&hovered>=start&&hovered<end
                ?new int[]{start,end}:null;
    }

    /** Range is half-open and expressed in menu ids, never backing container indices. */
    public static boolean contains(int[] range,int slot){
        return range!=null&&range.length==2&&slot>=range[0]&&slot<range[1];
    }

    /** OMMC uses negative menu ids for right-click merges; -999 is outside. */
    public static boolean allowsMergeClick(int[] range,Integer click){
        return click!=null&&click!=-999&&click!=Integer.MIN_VALUE
                &&contains(range,click<0?-click:click);
    }
}

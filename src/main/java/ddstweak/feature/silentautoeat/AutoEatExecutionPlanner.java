package ddstweak.feature.silentautoeat;

/**
 * Selects a safe execution path without reading or mutating Inventory.
 * Source selection is owned by the existing SilentAutoEatRules.selectFoodSource.
 * No path here authorizes inventory clicks until it has its own ACK/restore code.
 * 仅选择执行路径，不修改背包或授权点击操作。
 */
final class AutoEatExecutionPlanner {
    enum Path {
        NONE,
        USE_OFFHAND_DIRECT,
        SWAP_TO_OFFHAND,
        MAIN_HAND_HOTBAR,
        MAIN_HAND_STAGED
    }

    private AutoEatExecutionPlanner(){}

    /** Keep source order, excluding inventory paths which cannot execute this session. */
    static int selectFoodSource(AutoEatOffhandPolicy policy,boolean offhandEmpty,
                                boolean critical,int selected,java.util.function.IntPredicate eligible,
                                boolean preferHotbar,boolean stagingAllowed){
        boolean inventoryAllowed=stagingAllowed||canReplaceOffhand(policy,offhandEmpty,critical);
        return SilentAutoEatRules.selectFoodSource(selected,
                index->(index<9||inventoryAllowed)&&eligible.test(index),preferHotbar);
    }

    static Path plan(AutoEatOffhandPolicy policy,boolean offhandIsAllowedFood,
                     boolean offhandEmpty,boolean offhandCritical,int sourceIndex,
                     int selectedIndex){
        return plan(policy,offhandIsAllowedFood,offhandEmpty,offhandCritical,
                sourceIndex,selectedIndex,false);
    }

    static Path plan(AutoEatOffhandPolicy policy,boolean offhandIsAllowedFood,
                     boolean offhandEmpty,boolean offhandCritical,int sourceIndex,
                     int selectedIndex,boolean preferMainhandHotbar){
        // 26.3's real offhand SWAP produces vanilla hand-change feedback.
        // Prefer a real hotbar mainhand use when possible; never fake F/SWAP.
        if(sourceIndex>=0&&sourceIndex<9
                &&(sourceIndex==selectedIndex||preferMainhandHotbar))
            return Path.MAIN_HAND_HOTBAR;
        if(offhandIsAllowedFood)return Path.USE_OFFHAND_DIRECT;
        if(sourceIndex<0||sourceIndex>=36)return Path.NONE;
        if(canReplaceOffhand(policy,offhandEmpty,offhandCritical))
            return Path.SWAP_TO_OFFHAND;
        return sourceIndex<9?Path.MAIN_HAND_HOTBAR:Path.MAIN_HAND_STAGED;
    }

    static boolean canReplaceOffhand(AutoEatOffhandPolicy policy,
                                     boolean offhandEmpty,boolean critical){
        if(offhandEmpty)return true;
        if(policy==AutoEatOffhandPolicy.ALLOW_REPLACE)return true;
        return policy==AutoEatOffhandPolicy.PROTECT_CRITICAL&&!critical;
    }
}

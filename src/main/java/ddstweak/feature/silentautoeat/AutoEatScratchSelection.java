package ddstweak.feature.silentautoeat;

import java.util.function.IntPredicate;

/** Choose a scratch slot other than the player's selected hotbar slot.
 * A slot with the same contents as the food cannot prove the staging SWAP happened.
 * 选择安全暂存槽位，避免产生无法验证的交换结果。
 */
final class AutoEatScratchSelection {
    private AutoEatScratchSelection(){}

    static int choose(int selected,IntPredicate isEmpty,IntPredicate distinctFromFood){
        for(int i=0;i<9;i++)if(i!=selected&&isEmpty.test(i))return i;
        for(int i=0;i<9;i++)if(i!=selected&&distinctFromFood.test(i))return i;
        return -1;
    }

    /** Development-only staging into the already selected mainhand slot. */
    static int selected(int selected,IntPredicate distinctFromFood){
        return selected>=0&&selected<9&&distinctFromFood.test(selected)?selected:-1;
    }

    static boolean isOwnedStageSource(int source,int scratch){
        return source>=9&&source<36&&scratch>=0&&scratch<9;
    }
}

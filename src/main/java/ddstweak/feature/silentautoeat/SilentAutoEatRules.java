package ddstweak.feature.silentautoeat;

final class SilentAutoEatRules {
    private SilentAutoEatRules(){}

    static boolean shouldTrigger(boolean enabled,boolean safeMenu,
                                 boolean usingItem,boolean spectator,boolean canEat,
                                 int foodLevel,int threshold){
        return enabled&&safeMenu&&!usingItem&&!spectator&&canEat&&foodLevel<=threshold;
    }
}

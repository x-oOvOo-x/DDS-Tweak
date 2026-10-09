package ddstweak.feature.clickthrough;

final class ClickThroughSupportRules {
    private ClickThroughSupportRules(){}

    static boolean jukebox(boolean hasEntity,boolean hasRecord,boolean playable){
        return hasEntity&&(hasRecord||playable);
    }

    static boolean composter(int level,boolean compostable){
        return level==8||level<8&&compostable;
    }

    static boolean campfire(boolean cookable,boolean dousing){
        return cookable||dousing;
    }

    static boolean beehive(int honeyLevel,boolean harvestTool){
        return honeyLevel>=5&&harvestTool;
    }

    static boolean respawnAnchor(int charge,boolean glowstone){
        return charge>0||charge<4&&glowstone;
    }

    static boolean cake(boolean canEat,boolean canAddCandle){
        return canEat||canAddCandle;
    }

    static boolean candleCake(boolean canEat,boolean canExtinguish,boolean canIgnite){
        return canEat||canExtinguish||canIgnite;
    }

    static boolean enderChest(boolean hasEntity,boolean blocked){
        return hasEntity&&!blocked;
    }

    static boolean lectern(boolean hasBook,boolean lecternBook){
        return hasBook||lecternBook;
    }
}

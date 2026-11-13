package ddstweak.integration.dds.qca;

/**
 * Identity of the one creative gesture whose reply may still update the cursor.
 * 仅允许匹配的创造模式操作响应更新光标物品。
 */
final class CreativeStorageSession<C,P,S,M> {
    private final C connection;
    private final P player;
    private final S screen;
    private final M menu;

    CreativeStorageSession(C connection,P player,S screen,M menu){
        this.connection=java.util.Objects.requireNonNull(connection);
        this.player=java.util.Objects.requireNonNull(player);
        this.screen=java.util.Objects.requireNonNull(screen);
        this.menu=java.util.Objects.requireNonNull(menu);
    }

    boolean matches(C connection,P player,S screen,M menu){
        return this.connection==connection&&this.player==player
                &&this.screen==screen&&this.menu==menu;
    }

    M menu(){return menu;}
}

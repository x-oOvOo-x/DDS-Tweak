package ddstweak.integration.dds.fakeplayer;

/** Stable DDS Protocol v1 fake-player packet ids. Legacy open-menu remains for older DDS servers. */
public final class DdsFakePlayerPackets {
    public static final String ACTION_C2S="fake_player_action_c2s";
    public static final String PROVIDER_ACTION_C2S="fake_player_provider_action_c2s";
    public static final String OPEN_MENU_S2C="fake_player_open_menu_s2c";
    public static final String CONTEXT_S2C="fake_player_context_s2c";
    public static final String STATE_S2C="fake_player_state_s2c";
    public static final String CANONICAL_ENDER_C2S="player_inventory_canonical_ender_c2s";
    public static final String RETURN_PARENT_C2S="player_inventory_return_parent_c2s";
    public static final String STRUCTURE_RESULT_S2C="player_inventory_structure_result_s2c";
    /** HELLO-only marker for strict session generation/revision/clear semantics. */
    public static final String SESSION_V3_CAPABILITY_S2C="player_inventory_session_v3_s2c";
    public static final int MENU_NONE=0,MENU_INVENTORY=1,MENU_ENDER_3=3,MENU_ENDER_6=6;
    private DdsFakePlayerPackets(){}
    public static boolean isMenuLayout(int layout){return layout==MENU_INVENTORY||layout==MENU_ENDER_3||layout==MENU_ENDER_6;}
}

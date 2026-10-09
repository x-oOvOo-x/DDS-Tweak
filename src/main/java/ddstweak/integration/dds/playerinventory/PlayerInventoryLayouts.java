package ddstweak.integration.dds.playerinventory;

public final class PlayerInventoryLayouts{
    public static final String INVENTORY_V1="player_inventory_v1",ENDER_3_V1="player_ender_3_v1",ENDER_6_V1="player_ender_6_v1",
            GCA_INVENTORY_V1="gca_player_inventory_v1",GCA_CONTROLLER_3_V1="gca_player_controller_3_v1",GCA_ENDER_3_V1="gca_player_ender_3_v1",
            ORG_INVENTORY_V1="org_player_inventory_v1",ORG_CONTROLS_V1="org_player_inventory_controls_v1",ORG_ENDER_3_V1="org_player_ender_3_v1";
    private PlayerInventoryLayouts(){}
    public static boolean isInventory(PlayerInventoryContext context){
        return context!=null&&context.inventoryKind()==PlayerInventoryKind.INVENTORY&&supportedLayout(context.layoutId(),context.inventoryKind());
    }
    public static boolean isGcaInventory(PlayerInventoryContext context){return context!=null&&context.inventoryKind()==PlayerInventoryKind.INVENTORY&&GCA_INVENTORY_V1.equals(context.layoutId());}
    public static boolean isGcaController(PlayerInventoryContext context){return context!=null&&context.inventoryKind()==PlayerInventoryKind.CONTROLLER&&GCA_CONTROLLER_3_V1.equals(context.layoutId());}
    public static boolean isOrgInventory(PlayerInventoryContext context){return context!=null&&context.inventoryKind()==PlayerInventoryKind.INVENTORY&&ORG_INVENTORY_V1.equals(context.layoutId());}
    public static boolean isOrgControls(PlayerInventoryContext context){return context!=null&&context.inventoryKind()==PlayerInventoryKind.INVENTORY&&ORG_CONTROLS_V1.equals(context.layoutId());}
    public static boolean isControlledInventory(PlayerInventoryContext context){return isGcaInventory(context)||isOrgControls(context);}
    public static boolean isGcaEnder(PlayerInventoryContext context){return context!=null&&context.inventoryKind()==PlayerInventoryKind.ENDER_CHEST&&GCA_ENDER_3_V1.equals(context.layoutId());}
    public static boolean isOrgEnder(PlayerInventoryContext context){return context!=null&&context.inventoryKind()==PlayerInventoryKind.ENDER_CHEST&&ORG_ENDER_3_V1.equals(context.layoutId());}
    public static int enderRows(PlayerInventoryContext context){if(context==null||context.inventoryKind()!=PlayerInventoryKind.ENDER_CHEST)return 0;return ENDER_3_V1.equals(context.layoutId())||GCA_ENDER_3_V1.equals(context.layoutId())||ORG_ENDER_3_V1.equals(context.layoutId())?3:ENDER_6_V1.equals(context.layoutId())?6:0;}
    public static boolean supported(PlayerInventoryContext context){return isInventory(context)||isGcaController(context)||enderRows(context)>0;}
    public static boolean supportedLayout(String layoutId,PlayerInventoryKind kind){
        if(kind==PlayerInventoryKind.INVENTORY)return INVENTORY_V1.equals(layoutId)||GCA_INVENTORY_V1.equals(layoutId)||ORG_INVENTORY_V1.equals(layoutId)||ORG_CONTROLS_V1.equals(layoutId);
        if(kind==PlayerInventoryKind.CONTROLLER)return GCA_CONTROLLER_3_V1.equals(layoutId);
        return kind==PlayerInventoryKind.ENDER_CHEST&&(ENDER_3_V1.equals(layoutId)||ENDER_6_V1.equals(layoutId)||GCA_ENDER_3_V1.equals(layoutId)||ORG_ENDER_3_V1.equals(layoutId));
    }
}

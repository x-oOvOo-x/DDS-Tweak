package ddstweak.integration.dds.qca;

import ddstweak.integration.dds.playerinventory.PlayerInventoryLayouts;

/**
 * Slot allowlist for server-attested native GCA/Org menus (no enhanced UI).
 * 仅允许已验证的原生 GCA/Org 菜单布局对应槽位。
 */
final class DdsQcaNativeSlotLayout {
    private DdsQcaNativeSlotLayout() {}

    static boolean storageSlot(String provider, String layout, int totalSlots, int slotId) {
        if (("gca".equals(provider) && !layout.startsWith("gca_"))
                || ("org".equals(provider) && !layout.startsWith("org_"))
                || (!"gca".equals(provider) && !"org".equals(provider))) return false;
        int top;
        int first;
        int end;
        int offhand = -1;
        switch (layout) {
            case PlayerInventoryLayouts.GCA_INVENTORY_V1:
            case PlayerInventoryLayouts.ORG_CONTROLS_V1:
                top = 54; first = 18; end = 54; offhand = 7; break;
            case PlayerInventoryLayouts.GCA_ENDER_3_V1:
                top = 54; first = 27; end = 54; break;
            case PlayerInventoryLayouts.ORG_INVENTORY_V1:
                top = 54; first = 0; end = 36; offhand = 40; break;
            case PlayerInventoryLayouts.ORG_ENDER_3_V1:
                top = 27; first = 0; end = 27; break;
            case PlayerInventoryLayouts.GCA_CONTROLLER_3_V1:
                top = 27; first = 0; end = 0; break;
            default:
                return false;
        }
        if (totalSlots != top + 36 || slotId < 0 || slotId >= totalSlots) return false;
        return slotId == offhand || (slotId >= first && slotId < end) || slotId >= top;
    }
}

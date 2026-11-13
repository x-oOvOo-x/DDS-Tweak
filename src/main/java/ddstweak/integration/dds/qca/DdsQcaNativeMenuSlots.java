package ddstweak.integration.dds.qca;

import ddstweak.integration.dds.fakeplayer.DdsFakePlayerClientNetwork;
import ddstweak.integration.dds.fakeplayer.DdsFakePlayerExtensionContext;
import ddstweak.integration.dds.playerinventory.PlayerInventoryContext;
import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetState;
import ddstweak.integration.gca.playerinventory.GcaPlayerInventoryAdapter;
import ddstweak.integration.org.playerinventory.OrgPlayerInventoryAdapter;
import ddstweak.integration.org.playerinventory.OrgPlayerInventoryOpenIntent;
import net.minecraft.network.chat.Component;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

/**
 * Classifies known native Provider storage slots for optional CDA QCA input.
 * Server context takes precedence; local recognition only guides the mouse and
 * never authorizes a server action. Unknown layouts fail closed.
 * 仅识别 Provider 槽位以辅助输入，不授予服务端权限。
 */
public final class DdsQcaNativeMenuSlots {
    private static ClientPacketListener connection;
    private static MenuType<?> openingType;
    private static int openingId = -1;
    private static DdsFakePlayerExtensionContext extension;
    private static PlayerInventoryContext localContext;

    private DdsQcaNativeMenuSlots() {}

    public static void onOpen(MenuType<?> type, int id) {
        connection = Minecraft.getInstance().getConnection();
        openingType = type;
        openingId = id;
        extension = DdsFakePlayerClientNetwork.peekExtension(id);
        localContext = null;
    }

    /** Called when the native GUI remains active, including enhancedInventoryGui=false. */
    public static void observeLocal(MenuType<?> type, int id, Component title) {
        if (id != openingId || type != openingType
                || OrgPlayerInventoryOpenIntent.isOfflineMenu(type, id, title)) return;
        PlayerInventoryContext context = GcaPlayerInventoryAdapter.tryContext(type, id, title);
        if (context == null) context = OrgPlayerInventoryAdapter.observeMenu(type, id, title);
        rememberLocal(type, id, context);
    }

    /** Cache GUI identification without confusing it with server authorization. */
    public static void rememberLocal(MenuType<?> type, int id, PlayerInventoryContext context) {
        if (id != openingId || type != openingType || context == null
                || context.targetState() == PlayerInventoryTargetState.OFFLINE
                || (!"gca".equals(context.source()) && !"org".equals(context.source()))) return;
        localContext = context;
    }

    public static void onContext(ClientPacketListener source, DdsFakePlayerExtensionContext context) {
        if (context != null && source == connection && context.containerId() == openingId)
            extension = context;
    }

    public static void revoke(int containerId) {
        if (openingId == containerId) {
            extension = null;
            localContext = null;
        }
    }

    public static void reset() {
        connection = null;
        openingType = null;
        openingId = -1;
        extension = null;
        localContext = null;
    }

    /** Null: not an authenticated native Provider menu. False: recognized but unsafe slot. */
    public static Boolean storageRole(AbstractContainerMenu menu, Slot slot) {
        Minecraft mc = Minecraft.getInstance();
        if (menu == null || slot == null || mc.player == null
                || menu instanceof UnifiedPlayerInventoryMirrorMenu
                || mc.getConnection() != connection || mc.player.containerMenu != menu
                || menu.containerId != openingId || menu.getType() != openingType) return null;

        PlayerInventoryContext context = extension != null
                ? extension.standalone(openingType) : localContext;
        if (context == null || (!"gca".equals(context.source()) && !"org".equals(context.source())))
            return null;
        int slotId = menu.slots.indexOf(slot);
        return DdsQcaNativeSlotLayout.storageSlot(context.source(), context.layoutId(),
                menu.slots.size(), slotId) && slot.mayPickup(mc.player);
    }
}

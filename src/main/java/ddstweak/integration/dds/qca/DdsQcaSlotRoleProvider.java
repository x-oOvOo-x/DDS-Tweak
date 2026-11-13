package ddstweak.integration.dds.qca;

/**
 * Neutral client slot roles used by QCA without depending on DDS server-side container interfaces.
 * 提供与服务端 DDS 菜单解耦的中立槽位角色。
 */
public interface DdsQcaSlotRoleProvider {
    enum Role{TARGET_STORAGE,TARGET_EQUIPMENT,TARGET_OFFHAND,VIEWER_INVENTORY,ENDER_STORAGE,CONTROL,DISABLED,UNKNOWN}
    Role ddsTweak$qcaRole(int menuSlot);
}

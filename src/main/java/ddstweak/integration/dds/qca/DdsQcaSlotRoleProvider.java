package ddstweak.integration.dds.qca;

/** Neutral client slot roles used by QCA without depending on DDS server-side container interfaces. */
public interface DdsQcaSlotRoleProvider {
    enum Role{TARGET_STORAGE,TARGET_EQUIPMENT,VIEWER_INVENTORY,ENDER_STORAGE,CONTROL,DISABLED,UNKNOWN}
    Role ddsTweak$qcaRole(int menuSlot);
}

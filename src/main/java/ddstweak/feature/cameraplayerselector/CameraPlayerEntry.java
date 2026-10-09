package ddstweak.feature.cameraplayerselector;

import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import java.util.UUID;

final class CameraPlayerEntry {
    final UUID uuid;
    final String name;
    final PlayerInventoryTargetType type;
    CameraPlayerEntry(UUID uuid,String name,PlayerInventoryTargetType type){this.uuid=uuid;this.name=name;this.type=type;}
}

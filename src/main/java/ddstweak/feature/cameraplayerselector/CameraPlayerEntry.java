package ddstweak.feature.cameraplayerselector;

import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import java.util.UUID;

/**
 * Represents a selectable player and its inventory target type.
 * 保存可选玩家及其背包目标类型。
 */
final class CameraPlayerEntry {
    final UUID uuid;
    final String name;
    final PlayerInventoryTargetType type;
    CameraPlayerEntry(UUID uuid,String name,PlayerInventoryTargetType type){this.uuid=uuid;this.name=name;this.type=type;}
}

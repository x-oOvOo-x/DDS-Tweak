package ddstweak.integration.dds.qca;

/**
 * Delegates legacy creative-screen mouse overrides to the shared QCA gesture state.
 * 将旧版创造界面的鼠标操作交给 QCA 统一状态处理。
 */
public interface DdsQcaMouseInputBridge {
    boolean ddstweak$qcaMouseClicked(double x,double y,int button);
    boolean ddstweak$qcaMouseDragged(double x,double y,int button);
    boolean ddstweak$qcaMouseReleased(int button);
}

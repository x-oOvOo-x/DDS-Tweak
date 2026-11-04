package ddstweak.integration.dds.qca;

/** Delegates legacy creative-screen mouse overrides to the shared QCA gesture state. */
public interface DdsQcaMouseInputBridge {
    boolean ddstweak$qcaMouseClicked(double x,double y,int button);
    boolean ddstweak$qcaMouseDragged(double x,double y,int button);
    boolean ddstweak$qcaMouseReleased(int button);
}

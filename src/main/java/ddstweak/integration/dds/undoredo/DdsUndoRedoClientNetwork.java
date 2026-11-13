package ddstweak.integration.dds.undoredo;

import ddstweak.network.DdsClientNetwork;
import net.minecraft.nbt.CompoundTag;

/**
 * Client network slice for DDS Undo/Redo shortcuts.
 * 注册撤销重做快捷请求，并检查服务端输入接管权限。
 */
public final class DdsUndoRedoClientNetwork {
    private static boolean initialized;
    private DdsUndoRedoClientNetwork(){}
    public static synchronized void initialize(){if(initialized)return;DdsClientNetwork.registerC2S(DdsUndoRedoPackets.UNDO_C2S);DdsClientNetwork.registerC2S(DdsUndoRedoPackets.REDO_C2S);initialized=true;}
    /**
     * Requires both Undo and Redo channels to be eligible for input takeover.
     * 撤销与重做两个通道均获准接管输入后才启用快捷键。
     */
    public static boolean canUseShortcut(){return DdsClientNetwork.canTakeOverInput(DdsUndoRedoPackets.UNDO_C2S)&&DdsClientNetwork.canTakeOverInput(DdsUndoRedoPackets.REDO_C2S);}
    public static boolean sendUndo(){return canUseShortcut()&&DdsClientNetwork.send(DdsUndoRedoPackets.UNDO_C2S,new CompoundTag());}
    public static boolean sendRedo(){return canUseShortcut()&&DdsClientNetwork.send(DdsUndoRedoPackets.REDO_C2S,new CompoundTag());}
}

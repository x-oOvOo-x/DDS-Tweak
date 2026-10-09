package ddstweak.mixin.integration.dds.undoredo;

import ddstweak.integration.dds.undoredo.DdsUndoRedoClientNetwork;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
//#if MC >= 260300
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif
//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
//#endif
//#if MC < 260300
import org.lwjgl.glfw.GLFW;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    //#if MC >= 12109
    @Inject(method="keyPress(JILnet/minecraft/client/input/KeyEvent;)V",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$undoRedoShortcut(long window,int action,KeyEvent event,CallbackInfo ci){
        //#if MC >= 260300
        //$$ if(action!=1||event.key()!=InputConstants.KEY_Z||!event.hasControlDown()||event.hasShiftDown())return;
        //#else
        if(action!=GLFW.GLFW_PRESS||event.key()!=GLFW.GLFW_KEY_Z||!event.hasControlDown()||event.hasShiftDown())return;
        //#endif
        Minecraft mc=Minecraft.getInstance();
        //#if MC >= 260200
        //$$ if(mc.player==null||!mc.player.isCreative()||mc.getConnection()==null||mc.gui.screen()!=null)return;
        //#else
        if(mc.player==null||!mc.player.isCreative()||mc.getConnection()==null||mc.screen!=null)return;
        //#endif
        boolean sent=event.hasAltDown()?DdsUndoRedoClientNetwork.sendRedo():DdsUndoRedoClientNetwork.sendUndo();
        if(sent)ci.cancel();
    }
    //#else
    //$$ @Inject(method="keyPress(JIIII)V",at=@At("HEAD"),cancellable=true)
    //$$ private void ddsTweak$undoRedoShortcut(long window,int key,int scan,int action,int modifiers,CallbackInfo ci){
    //$$     if(action!=GLFW.GLFW_PRESS||key!=GLFW.GLFW_KEY_Z||(modifiers&GLFW.GLFW_MOD_CONTROL)==0||(modifiers&GLFW.GLFW_MOD_SHIFT)!=0)return;
    //$$     Minecraft mc=Minecraft.getInstance();
    //$$     if(mc.player==null||!mc.player.isCreative()||mc.getConnection()==null||mc.screen!=null)return;
    //$$     boolean sent=(modifiers&GLFW.GLFW_MOD_ALT)!=0?DdsUndoRedoClientNetwork.sendRedo():DdsUndoRedoClientNetwork.sendUndo();
    //$$     if(sent)ci.cancel();
    //$$ }
    //#endif
}

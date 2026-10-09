package ddstweak.feature.windowplayerhead;

import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
//#if MC >= 260300
//$$ import org.lwjgl.sdl.SDL_Surface;
//$$ import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_RGBA32;
//$$ import static org.lwjgl.sdl.SDLSurface.*;
//$$ import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowIcon;
//#else
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;
//#endif
import org.apache.logging.log4j.*;
import java.nio.ByteBuffer;

final class WindowIcon {
    private static final Logger LOGGER=LogManager.getLogger("DDS Tweak/PlayerHeadWindowIcon");
    static void set(Minecraft client,PlayerHeadRenderer.Icon[] icons){
        if(icons==null||icons.length==0)return;
        try{
            //#if MC >= 260300
            //$$ setSdl(client,icons[icons.length-1]);
            //#else
            try(MemoryStack stack=MemoryStack.stackPush()){
                //#if MC >= 11900
                GLFWImage.Buffer images=GLFWImage.malloc(icons.length,stack);
                //#else
                //$$ GLFWImage.Buffer images=GLFWImage.mallocStack(icons.length,stack);
                //#endif
                for(int i=0;i<icons.length;i++){PlayerHeadRenderer.Icon icon=icons[i];ByteBuffer pixels=stack.malloc(icon.rgba().length);pixels.put(icon.rgba()).flip();images.get(i).set(icon.width(),icon.height(),pixels);}
                GLFW.glfwSetWindowIcon(MinecraftCompat.windowHandle(client),images);
            }
            //#endif
        }catch(Exception e){LOGGER.warn("Failed to set layered player head window icon",e);}
    }

    //#if MC >= 260300
    //$$ private static void setSdl(Minecraft client,PlayerHeadRenderer.Icon icon){
    //$$     ByteBuffer pixels=ByteBuffer.allocateDirect(icon.rgba().length);pixels.put(icon.rgba()).flip();
    //$$     SDL_Surface surface=SDL_CreateSurfaceFrom(icon.width(),icon.height(),SDL_PIXELFORMAT_RGBA32,pixels,icon.width()*4);
    //$$     if(surface==null)return;
    //$$     try{SDL_SetWindowIcon(client.getWindow().handle(),surface);}finally{SDL_DestroySurface(surface);}
    //$$ }
    //#endif

    private WindowIcon(){}
}

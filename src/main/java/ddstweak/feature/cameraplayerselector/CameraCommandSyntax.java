package ddstweak.feature.cameraplayerselector;

import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import java.util.regex.Pattern;

/**
 * Routes editable Camera inputs without interpreting chat triggers as slash commands.
 * 区分聊天文本与斜杠命令输入。
 */
final class CameraCommandSyntax {
    private static final Pattern SERVER_ROOT=Pattern.compile("[A-Za-z0-9_:.+-]+");
    private static final Pattern CHAT_ROOT=Pattern.compile("(?:!+|#+)[A-Za-z0-9_:.+-]+");

    private CameraCommandSyntax(){}

    static boolean isChat(String value){
        return value!=null&&!value.isEmpty()&&(value.charAt(0)=='!'||value.charAt(0)=='#');
    }

    static boolean validRoot(String value){
        if(value==null||value.isEmpty())return false;
        boolean slash=value.startsWith("/");
        String text=slash?value.substring(1):value;
        if(text.isEmpty())return false;
        String root=text.split("\\s+",2)[0];
        return (slash||!isChat(value)?SERVER_ROOT:CHAT_ROOT).matcher(root).matches();
    }

    static String display(String value){
        return value.startsWith("/")||isChat(value)?value:"/"+value;
    }

    static boolean send(Minecraft minecraft,String value){
        if(value==null||value.trim().isEmpty())return false;
        return isChat(value)?MinecraftCompat.sendChatWithoutHistory(minecraft,value)
                :MinecraftCompat.sendCommandWithoutHistory(minecraft,value);
    }
}

package ddstweak.platform.minecraft;

import java.util.function.BooleanSupplier;

/**
 * Keeps Tweak-initiated GUI commands out of chat's up-arrow history.
 * The scope only covers one synchronous vanilla dispatch; manual chat remains untouched.
 */
public final class CommandHistoryGuard {
    private static final ThreadLocal<String> ACTIVE_COMMAND=new ThreadLocal<>();

    private CommandHistoryGuard(){}

    public static boolean run(String command,BooleanSupplier dispatch){
        return runScoped(command.startsWith("/")?command:"/"+command,dispatch);
    }

    public static boolean runChat(String message,BooleanSupplier dispatch){
        return runScoped(message,dispatch);
    }

    private static boolean runScoped(String message,BooleanSupplier dispatch){
        String previous=ACTIVE_COMMAND.get();
        ACTIVE_COMMAND.set(message);
        try{
            return dispatch.getAsBoolean();
        }finally{
            if(previous==null)ACTIVE_COMMAND.remove();
            else ACTIVE_COMMAND.set(previous);
        }
    }

    public static boolean shouldSuppress(String message){
        String active=ACTIVE_COMMAND.get();
        return active!=null&&(active.equals(message)||(active.startsWith("/")&&active.substring(1).equals(message)));
    }
}

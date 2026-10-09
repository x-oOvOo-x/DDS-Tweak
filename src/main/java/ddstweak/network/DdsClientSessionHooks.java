package ddstweak.network;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.*;

/** Reset registry separating DDS protocol state from whole-connection provider state. */
public final class DdsClientSessionHooks {
    private static final Logger LOGGER=LogManager.getLogger("DDS Tweak");
    private static final List<Runnable> PROTOCOL_RESET_HOOKS=new ArrayList<Runnable>();
    private static final List<Runnable> CONNECTION_RESET_HOOKS=new ArrayList<Runnable>();
    private DdsClientSessionHooks(){}

    /** DDS-owned state: clear on protocol loss and on connection changes. */
    public static synchronized void registerResetHook(Runnable hook){PROTOCOL_RESET_HOOKS.add(Objects.requireNonNull(hook,"hook"));}

    /** Provider/client-session state: clear only when the Minecraft connection changes. */
    public static synchronized void registerConnectionResetHook(Runnable hook){CONNECTION_RESET_HOOKS.add(Objects.requireNonNull(hook,"hook"));}

    public static void resetProtocol(){run(snapshot(PROTOCOL_RESET_HOOKS));}

    public static void resetConnection(){
        resetProtocol();
        run(snapshot(CONNECTION_RESET_HOOKS));
    }

    private static synchronized List<Runnable> snapshot(List<Runnable> source){return new ArrayList<Runnable>(source);}
    private static void run(List<Runnable> hooks){for(Runnable hook:hooks)try{hook.run();}catch(RuntimeException ex){LOGGER.error("DDS Tweak client session reset hook failed",ex);}}
}

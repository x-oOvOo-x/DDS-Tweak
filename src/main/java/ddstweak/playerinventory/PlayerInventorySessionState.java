package ddstweak.playerinventory;

import ddstweak.integration.org.playerinventory.OrgPlayerInventoryOpenIntent;
import ddstweak.network.DdsClientSessionHooks;

/** Registers provider/UI state that must never survive a client connection change. */
public final class PlayerInventorySessionState {
    private static boolean initialized;
    private PlayerInventorySessionState(){}

    public static synchronized void initialize(){
        if(initialized)return;
        DdsClientSessionHooks.registerConnectionResetHook(OrgPlayerInventoryOpenIntent::reset);
        DdsClientSessionHooks.registerConnectionResetHook(PlayerInventoryClientActionState::reset);
        DdsClientSessionHooks.registerConnectionResetHook(PlayerInventoryOpenContext::clear);
        initialized=true;
    }
}

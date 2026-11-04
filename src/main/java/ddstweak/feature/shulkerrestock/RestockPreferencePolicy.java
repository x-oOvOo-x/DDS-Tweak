package ddstweak.feature.shulkerrestock;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Client-side preflight for the server's bounded item-ID preference protocol. */
public final class RestockPreferencePolicy {
    public static final int MAX_IDS=128, MAX_ID_LENGTH=128, MAX_ENCODED_LENGTH=8192;
    private RestockPreferencePolicy(){}

    public static List<String> effectiveIds(String mode, List<String> selected){
        return "ALL".equals(mode)?Collections.emptyList():selected;
    }

    /** Reapply preferences when an unsolicited server status enters CLIENT mode. */
    public static boolean retryAfterModeChange(int requestId,String previousMode,String currentMode){
        return requestId==0 && "client".equals(currentMode) && !"client".equals(previousMode);
    }

    public static boolean valid(List<String> ids){
        if(ids==null||ids.size()>MAX_IDS)return false;
        int length=0;
        Set<String> unique=new HashSet<>();
        for(String id:ids){
            if(id==null||id.length()>MAX_ID_LENGTH||
                    !id.matches("[a-z0-9._-]+:[a-z0-9/._-]+")||!unique.add(id))return false;
            length+=id.length()+(unique.size()>1?1:0);
            if(length>MAX_ENCODED_LENGTH)return false;
        }
        return true;
    }
}

package ddstweak.integration.dds.fakeplayer;

import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A complete server snapshot is required before absence can mean REAL. */
public final class FakePlayerIdentityState {
    private final Set<UUID> fake=new HashSet<>();
    private boolean complete;

    public PlayerInventoryTargetType resolve(UUID uuid){
        if(uuid==null||!complete)return PlayerInventoryTargetType.UNKNOWN;
        return fake.contains(uuid)?PlayerInventoryTargetType.FAKE:PlayerInventoryTargetType.REAL;
    }
    public boolean acceptSnapshot(String encoded){
        if(encoded==null||encoded.length()>262144){clear();return false;}
        Set<UUID> next=new HashSet<>();
        if(!encoded.isEmpty())for(String token:encoded.split(",",-1)){
            if(next.size()>=4096){clear();return false;}
            try{
                UUID uuid=UUID.fromString(token);
                if(!uuid.toString().equals(token)){clear();return false;}
                next.add(uuid);
            }catch(IllegalArgumentException ex){clear();return false;}
        }
        fake.clear();fake.addAll(next);complete=true;return true;
    }
    public void applyChange(UUID uuid,boolean present){
        if(!complete||uuid==null)return;
        if(present)fake.add(uuid);else fake.remove(uuid);
    }
    public void clear(){fake.clear();complete=false;}
}

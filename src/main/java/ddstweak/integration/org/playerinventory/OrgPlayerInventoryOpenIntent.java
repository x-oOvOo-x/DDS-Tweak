package ddstweak.integration.org.playerinventory;

import ddstweak.integration.dds.playerinventory.PlayerInventoryContext;
import ddstweak.integration.dds.playerinventory.PlayerInventoryKind;
import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetState;
import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Correlates one explicit Org inventory/Ender intent with one matching OpenScreen on the
 * current connection. Correlation is discovery metadata only; Org remains authoritative.
 */
public final class OrgPlayerInventoryOpenIntent {
    public enum Kind { INVENTORY, ENDER_CHEST }

    public static final class Session{
        private final int containerId;
        private final String target;
        private final Kind kind;
        private final long createdNanos;
        private final ClientPacketListener connection;
        private final PlayerInfo targetInfo;
        private final PlayerInventoryContext returnContext;

        private Session(int containerId,String target,Kind kind,long createdNanos,
                        ClientPacketListener connection,PlayerInfo targetInfo,PlayerInventoryContext returnContext){
            this.containerId=containerId;this.target=target;this.kind=kind;this.createdNanos=createdNanos;
            this.connection=connection;this.targetInfo=targetInfo;this.returnContext=returnContext;
        }

        public int containerId(){return containerId;}
        public String target(){return target;}
        public Kind kind(){return kind;}
        public long createdNanos(){return createdNanos;}
        public PlayerInventoryContext returnContext(){return returnContext;}
    }

    private static final boolean TRACE=Boolean.getBoolean("dds.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger TRACE_LOG=
            org.apache.logging.log4j.LogManager.getLogger("dds.playerinventory.dt");
    private static final long PENDING_TTL_NANOS=3_000_000_000L,SESSION_TTL_NANOS=60_000_000_000L;
    private static final Map<Integer,Session> SESSIONS=new HashMap<Integer,Session>();
    private static Pending pending;

    private OrgPlayerInventoryOpenIntent(){}

    public static synchronized void observeCommand(String command){
        purge();
        ParsedCommand parsed=parse(command);
        ClientPacketListener connection=currentConnection();
        if(parsed==null||connection==null)return;
        PlayerInfo targetInfo=playerInfo(connection,null,parsed.target);
        // Explicit Org commands may target an offline player that is intentionally absent
        // from the client tab list. Keep that intent as discovery metadata only; provider
        // authority still comes from the opened Org menu / Extension Context.
        pending=new Pending(parsed.target,parsed.kind,System.nanoTime(),connection,targetInfo,null);
        if(TRACE)TRACE_LOG.info("org_command_intent kind="+parsed.kind);
    }

    /** Bind an authorized QCA return to the exact Org parent context, not only its display name. */
    public static synchronized void expectReopenInventory(PlayerInventoryContext source){
        purge();
        ClientPacketListener connection=currentConnection();
        if(connection==null||source==null||!"org".equals(source.source())
                ||source.inventoryKind()!=PlayerInventoryKind.INVENTORY
                ||source.targetName()==null||source.targetName().trim().isEmpty())return;
        PlayerInfo targetInfo=playerInfo(connection,source.targetUuid(),source.targetName());
        boolean online=source.targetState()==PlayerInventoryTargetState.ONLINE&&targetInfo!=null;
        if(!online)return;
        pending=new Pending(source.targetName(),Kind.INVENTORY,System.nanoTime(),connection,targetInfo,source);
        if(TRACE)TRACE_LOG.info("org_return_intent parent_layout="+source.layoutId()+" state="+source.targetState());
    }

    /** Consume only the matching OpenScreen. An unrelated menu must not steal an Org
     * command or trusted parent-return intent while it is still within its short TTL. */
    public static synchronized Session bind(MenuType<?> type,int containerId,Component title){
        purge();
        Pending p=pending;
        if(p==null)return null;
        if(!sameConnection(p.connection)||!samePlayerInfo(p.connection,p.targetInfo)){
            pending=null;
            return null;
        }
        boolean typeMatches=matchesType(type,p.kind,p.targetInfo==null);
        boolean titleMatches=matchesTitle(title,p.target,p.targetInfo,p.kind);
        if(!typeMatches||!titleMatches){
            if(TRACE)TRACE_LOG.info("org_bind_deferred id="+containerId
                    +" type_matches="+typeMatches+" title_matches="+titleMatches
                    +" return_intent="+(p.returnContext!=null));
            return null;
        }
        pending=null;
        Session session=new Session(containerId,p.target,p.kind,p.createdNanos,p.connection,p.targetInfo,p.returnContext);
        SESSIONS.put(containerId,session);
        if(TRACE)TRACE_LOG.info("org_bind id="+containerId+" kind="+p.kind
                +" return_intent="+(p.returnContext!=null));
        return session;
    }

    public static synchronized Session session(int containerId,MenuType<?> type,Component title){
        purge();
        Session session=SESSIONS.get(containerId);
        if(session==null)return null;
        if(!sameConnection(session.connection)||!samePlayerInfo(session.connection,session.targetInfo)
                ||!matchesType(type,session.kind,session.targetInfo==null)
                ||!matchesTitle(title,session.target,session.targetInfo,session.kind)){
            SESSIONS.remove(containerId);
            return null;
        }
        return session;
    }

    public static synchronized Session session(int containerId){
        purge();
        return SESSIONS.get(containerId);
    }

    /** Recognized offline Org menus belong to Org, not DT's screen pipeline. */
    public static synchronized boolean isOfflineMenu(MenuType<?> type,int containerId,Component title){
        if(isOfflineTitle(title))return true;
        Session session=session(containerId,type,title);
        return session!=null&&session.targetInfo==null;
    }

    /** Detect Org's own translatable offline marker, including composed Ender titles. */
    static boolean isOfflineTitle(Component title){
        if(title==null)return false;
        String key=MinecraftCompat.translationKey(title);
        if("carpet-org-addition.operation.offline_player_name".equals(key)
                ||"carpet.commands.player.inventory.offline.display_name".equals(key))return true;
        for(Component sibling:title.getSiblings())if(isOfflineTitle(sibling))return true;
        return false;
    }

    public static synchronized void close(int containerId){SESSIONS.remove(containerId);}
    public static synchronized void reset(){pending=null;SESSIONS.clear();}

    static boolean matchesVisibleTitle(String label,String target,String display){
        if(label==null||target==null)return false;
        String visible=label.trim(),expected=target.trim();
        if(visible.isEmpty()||expected.isEmpty())return false;
        if(visible.equals(expected))return true;
        return display!=null&&!display.trim().isEmpty()&&visible.equals(display.trim());
    }

    private static ParsedCommand parse(String command){
        if(command==null)return null;
        String normalized=command.trim();
        while(normalized.startsWith("/"))normalized=normalized.substring(1).trim();
        String[] parts=normalized.split("\\s+");
        if((parts.length!=3&&parts.length!=4)||!"player".equals(parts[0]))return null;
        if(parts.length==4&&!"true".equalsIgnoreCase(parts[3])&&!"false".equalsIgnoreCase(parts[3]))return null;
        Kind kind="inventory".equals(parts[2])?Kind.INVENTORY:"enderChest".equals(parts[2])?Kind.ENDER_CHEST:null;
        String target=parts[1].trim();
        return kind==null||target.isEmpty()?null:new ParsedCommand(target,kind);
    }

    private static boolean matchesType(MenuType<?> type,Kind kind,boolean offlineCandidate){
        int rows=type==MenuType.GENERIC_9x3?3:type==MenuType.GENERIC_9x6?6:0;
        return matchesRows(rows,kind,offlineCandidate);
    }

    static boolean matchesRows(int rows,Kind kind,boolean offlineCandidate){
        return kind==Kind.INVENTORY?rows==6
                :kind==Kind.ENDER_CHEST&&(rows==3||offlineCandidate&&rows==6);
    }

    private static boolean matchesTitle(Component title,String target,PlayerInfo info,Kind kind){
        if(title==null)return false;
        String visible=title.getString();
        if(matchesVisibleTitle(visible,target,displayName(info)))return true;
        if(info!=null)return false;
        String offline=MinecraftCompat.translatable("carpet-org-addition.operation.offline_player_name",
                MinecraftCompat.literal(target)).getString();
        if(visible.trim().equals(offline.trim()))return true;
        String legacyOffline=MinecraftCompat.translatable("carpet.commands.player.inventory.offline.display_name",
                MinecraftCompat.literal(target)).getString();
        if(visible.trim().equals(legacyOffline.trim()))return true;
        if(kind!=Kind.ENDER_CHEST)return false;
        String enderTitle=MinecraftCompat.translatable("container.enderchest").getString();
        return matchesOfflineEnderTitle(visible,enderTitle,offline)
                ||matchesOfflineEnderTitle(visible,enderTitle,legacyOffline);
    }

    static boolean matchesOfflineEnderTitle(String visible,String enderTitle,String offlineName){
        return visible!=null&&enderTitle!=null&&offlineName!=null
                &&visible.trim().equals(enderTitle+" - "+offlineName.trim());
    }

    private static String displayName(PlayerInfo info){
        if(info==null)return null;
        UUID uuid=MinecraftCompat.gameProfileId(info.getProfile());
        Minecraft mc=Minecraft.getInstance();
        if(uuid!=null&&mc.level!=null)
            for(Player player:mc.level.players())if(uuid.equals(player.getUUID()))return player.getDisplayName().getString();
        return null;
    }

    private static ClientPacketListener currentConnection(){return Minecraft.getInstance().getConnection();}
    private static boolean sameConnection(ClientPacketListener expected){return expected!=null&&currentConnection()==expected;}

    private static PlayerInfo playerInfo(ClientPacketListener connection,UUID uuid,String name){
        if(connection==null)return null;
        for(PlayerInfo candidate:connection.getOnlinePlayers()){
            if(candidate==null||candidate.getProfile()==null)continue;
            UUID id=MinecraftCompat.gameProfileId(candidate.getProfile());
            String profileName=MinecraftCompat.gameProfileName(candidate.getProfile());
            if(uuid!=null&&uuid.equals(id)||uuid==null&&name.equals(profileName))return candidate;
        }
        return null;
    }

    /** PlayerInfo object identity changes when the target leaves and rejoins, even with the same UUID. */
    private static boolean samePlayerInfo(ClientPacketListener connection,PlayerInfo expected){
        if(expected==null)return true;
        if(connection==null)return false;
        for(PlayerInfo candidate:connection.getOnlinePlayers())if(candidate==expected)return true;
        return false;
    }

    private static void purge(){
        long now=System.nanoTime();
        if(pending!=null&&(now-pending.createdNanos>PENDING_TTL_NANOS
                ||!sameConnection(pending.connection)||!samePlayerInfo(pending.connection,pending.targetInfo)))pending=null;
        SESSIONS.values().removeIf(session->!sameConnection(session.connection)
                ||!samePlayerInfo(session.connection,session.targetInfo)
                ||sessionExpired(now,session.createdNanos(),isCurrentMenu(session)));
    }

    /** Keep recognized native Org menus protected for their whole open lifetime. */
    private static boolean isCurrentMenu(Session session){
        Minecraft mc=Minecraft.getInstance();
        return mc.player!=null&&mc.player.containerMenu!=mc.player.inventoryMenu
                &&mc.player.containerMenu.containerId==session.containerId;
    }

    static boolean sessionExpired(long now,long createdNanos,boolean currentMenu){
        return !currentMenu&&(now<createdNanos||now-createdNanos>SESSION_TTL_NANOS);
    }

    private static final class Pending{
        private final String target;
        private final Kind kind;
        private final long createdNanos;
        private final ClientPacketListener connection;
        private final PlayerInfo targetInfo;
        private final PlayerInventoryContext returnContext;

        private Pending(String target,Kind kind,long createdNanos,ClientPacketListener connection,
                        PlayerInfo targetInfo,PlayerInventoryContext returnContext){
            this.target=target;this.kind=kind;this.createdNanos=createdNanos;
            this.connection=connection;this.targetInfo=targetInfo;this.returnContext=returnContext;
        }
    }

    private static final class ParsedCommand{
        private final String target;
        private final Kind kind;
        private ParsedCommand(String target,Kind kind){this.target=target;this.kind=kind;}
    }
}

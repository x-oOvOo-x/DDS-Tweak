package ddstweak.integration.gca.playerinventory;

import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge;
import ddstweak.integration.dds.playerinventory.PlayerInventoryKind;
import ddstweak.integration.org.playerinventory.OrgPlayerInventoryAdapter;
import ddstweak.network.DdsClientSessionHooks;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import java.util.UUID;

/**
 * Associates the next GCA or Org player menu with the player actually right-clicked.
 * A client interaction is discovery metadata, never proof that the target is FAKE
 * or that the server has authorized an action.
 * 客户端点击仅用于定位 GCA/Org 菜单目标，不代表授权。
 */
public final class GcaPlayerInteractionIntent {
    private static final boolean TRACE=Boolean.getBoolean("ddstweak.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger TRACE_LOG=
            org.apache.logging.log4j.LogManager.getLogger("ddstweak.playerinventory");
    private static final long TTL_NANOS=3_000_000_000L;
    private static final int SOURCE_UNBOUND=0,SOURCE_GCA=1,SOURCE_ORG_ENDER=2;
    private static Pending pending;

    static {DdsClientSessionHooks.registerConnectionResetHook(GcaPlayerInteractionIntent::clear);}
    private GcaPlayerInteractionIntent(){}

    /** Called before a local right-click is processed; an unrelated click invalidates stale intent. */
    public static synchronized void observeUse(Minecraft minecraft){
        if(minecraft==null||minecraft.getConnection()==null||minecraft.player==null
                ||!(minecraft.hitResult instanceof EntityHitResult)){
            if(TRACE)TRACE_LOG.info("gca_click_no_player_hit hit={}",
                    minecraft==null?null:minecraft.hitResult);
            pending=null;
            return;
        }
        net.minecraft.world.entity.Entity entity=((EntityHitResult)minecraft.hitResult).getEntity();
        if(!(entity instanceof Player)){
            if(TRACE)TRACE_LOG.info("gca_click_not_player entity={}",entity.getType());
            pending=null;
            return;
        }
        Player player=(Player)entity;
        pending=new Pending(minecraft.getConnection(),System.nanoTime(),player.getUUID(),
                player.getName().getString(),player.getDisplayName().getString());
        if(TRACE)TRACE_LOG.info("gca_click_capture target={} uuid={} display={}",
                pending.name,pending.uuid,pending.displayName);
    }

    /**
     * Called at MenuScreens.create before the provider adapters resolve the menu.
     * Unrelated openings invalidate the pending click.
     */
    public static synchronized void observeMenu(MenuType<?> type,int containerId,Component title){
        Pending previous=pending;
        if(previous==null){
            if(TRACE&&GcaPlayerInventoryAdapter.isGcaMenu(type,title))
                TRACE_LOG.info("gca_menu_without_click id={} type={} title={}",
                        containerId,type,title);
            return;
        }
        Minecraft minecraft=Minecraft.getInstance();
        boolean gca=GcaPlayerInventoryAdapter.isGcaMenu(type,title);
        boolean orgEnder=false;
        //#if MC >= 12111
        orgEnder=OrgPlayerInventoryAdapter.isClickedEnderTitle(type,title,previous.name);
        //#endif
        if(!fresh(previous.at,System.nanoTime())
                ||minecraft.getConnection()!=previous.connection
                ||!gca&&!orgEnder){
            pending=null;
            if(TRACE)TRACE_LOG.info("gca_click_discard id={} menu_type={} title={}",containerId,type,title);
            return;
        }
        previous.containerId=containerId;
        previous.menuSource=gca?SOURCE_GCA:SOURCE_ORG_ENDER;
        if(TRACE)TRACE_LOG.info("player_click_bound id={} source={} target={} title_match={}",
                containerId,gca?"gca":"org_ender",previous.name,matchesTitle(title,previous.displayName));
    }

    /**
     * Consume the player actually clicked for this exact opened GCA container.
     * Display name equality is diagnostic, never the source of account identity:
     * scoreboard, fake markers and server-side naming plugins may differ.
     */
    static synchronized Target consume(int containerId,Component title,PlayerInventoryKind kind){
        Target target=consumeBound(containerId,SOURCE_GCA);
        if(TRACE&&target!=null)TRACE_LOG.info("gca_click_consume id={} kind={} target={} display_match={}",
                containerId,kind,target.name,matchesTitle(title,target.displayName));
        return target;
    }

    /** Consume only an Org Ender menu matched to the clicked online player. */
    public static synchronized Target consumeOrgEnder(int containerId){
        Target target=consumeBound(containerId,SOURCE_ORG_ENDER);
        if(TRACE&&target!=null)TRACE_LOG.info("org_ender_click_consume id={} target={}",containerId,target.name);
        return target;
    }

    private static Target consumeBound(int containerId,int source){
        Pending previous=pending;
        pending=null;
        if(previous==null||previous.menuSource!=source)return null;
        Minecraft minecraft=Minecraft.getInstance();
        if(!fresh(previous.at,System.nanoTime())
                ||minecraft.getConnection()!=previous.connection
                ||previous.containerId!=containerId
                ||!CarpetPlayerCommandBridge.validTargetName(previous.name))return null;
        return new Target(previous.uuid,previous.name,previous.displayName);
    }

    static boolean matchesTitle(Component title,String display){
        if(title==null||display==null||display.isEmpty())return false;
        Object[] args=MinecraftCompat.translationArgs(title);
        return args.length==1&&args[0] instanceof Component
                &&display.equals(((Component)args[0]).getString());
    }

    static boolean fresh(long recorded,long now){
        return now>=recorded&&now-recorded<=TTL_NANOS;
    }

    public static synchronized void clear(){pending=null;}

    public static final class Target{
        final UUID uuid;
        final String name,displayName;
        Target(UUID uuid,String name,String displayName){this.uuid=uuid;this.name=name;this.displayName=displayName;}
        public UUID uuid(){return uuid;}
        public String name(){return name;}
    }

    private static final class Pending{
        final Object connection;
        final long at;
        final UUID uuid;
        final String name,displayName;
        int containerId=-1,menuSource=SOURCE_UNBOUND;
        Pending(Object connection,long at,UUID uuid,String name,String displayName){
            this.connection=connection;this.at=at;this.uuid=uuid;
            this.name=name;this.displayName=displayName;
        }
    }
}

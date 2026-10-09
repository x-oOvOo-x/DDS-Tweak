package ddstweak.feature.railpowerguide;

import ddstweak.config.Configs;
import fi.dy.masa.malilib.event.RenderEventHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class RailPowerPlacementGuide {
    private static final int MAX_TRACE_RAILS=4096;
    private static final int DISPLAY_RADIUS=20;
    private static final int SCAN_RADIUS=DISPLAY_RADIUS+RailPowerPlanner.POWER_PROPAGATION_RADIUS;
    private static final int VERTICAL_SCAN_RADIUS=12;
    private static final int RECALCULATE_TICKS=5;
    private static final int NO_RAIL_TIMEOUT_TICKS=100;
    private static final int MAX_SOURCES=12;
    private static final int MAX_MARKERS=20;

    private static ClientLevel lastLevel;
    private static RailKind activeKind;
    private static List<BlockPos> markers=Collections.emptyList();
    private static int recalculateDelay;
    private static int noRailTicks;
    private static boolean initialized;

    private RailPowerPlacementGuide(){}

    public static void initialize(){
        if(initialized)return;
        initialized=true;
        ClientTickEvents.END_CLIENT_TICK.register(RailPowerPlacementGuide::tick);
        RenderEventHandler.getInstance().registerWorldLastRenderer(RailPowerGuideRenderer.INSTANCE);
    }

    public static List<BlockPos> getTargets(){
        return Configs.Visual.RAIL_POWER_PLACEMENT_GUIDE.getBooleanValue()?markers:Collections.<BlockPos>emptyList();
    }

    public static BlockPos getTarget(){
        List<BlockPos> targets=getTargets();
        return targets.isEmpty()?null:targets.get(0);
    }

    public static void clear(){
        activeKind=null;
        markers=Collections.emptyList();
        recalculateDelay=0;
        noRailTicks=0;
    }

    private static void tick(Minecraft mc){
        if(!Configs.Visual.RAIL_POWER_PLACEMENT_GUIDE.getBooleanValue()){
            clear();
            lastLevel=mc.level;
            return;
        }
        if(mc.level==null||mc.player==null){
            clear();
            lastLevel=null;
            return;
        }
        if(lastLevel!=mc.level){
            clear();
            lastLevel=mc.level;
        }

        RailKind looked=getLookedRailKind(mc);
        if(looked!=null){
            if(activeKind!=looked){
                activeKind=looked;
                recalculateDelay=0;
            }
            noRailTicks=0;
        }
        if(activeKind==null){
            markers=Collections.emptyList();
            return;
        }
        if(recalculateDelay>0){
            recalculateDelay--;
            return;
        }
        recalculateDelay=RECALCULATE_TICKS-1;

        //#if MC >= 11500
        PlanResult plan=planLocal(mc.level,activeKind,mc.player.getX(),mc.player.getY(),mc.player.getZ());
        //#else
        //$$ PlanResult plan=planLocal(mc.level,activeKind,mc.player.x,mc.player.y,mc.player.z);
        //#endif
        markers=plan.markers;
        if(plan.hasRails){
            noRailTicks=0;
        }else{
            noRailTicks+=RECALCULATE_TICKS;
            if(noRailTicks>=NO_RAIL_TIMEOUT_TICKS)clear();
        }
    }

    private static RailKind getLookedRailKind(Minecraft mc){
        HitResult hit=mc.hitResult;
        if(!(hit instanceof BlockHitResult)||hit.getType()!=HitResult.Type.BLOCK)return null;
        return RailKind.from(mc.level.getBlockState(((BlockHitResult)hit).getBlockPos()));
    }

    private static PlanResult planLocal(ClientLevel level,RailKind kind,double playerX,double playerY,double playerZ){
        List<BlockPos> seeds=findLocalRails(level,kind,playerX,playerY,playerZ);
        if(seeds.isEmpty())return PlanResult.none();

        List<Segment> segments=new ArrayList<Segment>();
        Set<BlockPos> traced=new HashSet<BlockPos>();
        for(BlockPos seed:seeds){
            if(traced.contains(seed))continue;
            Segment segment=Segment.create(level,seed,kind,segments.size());
            if(segment==null){
                traced.add(seed);
                continue;
            }
            segments.add(segment);
            traced.addAll(segment.path);
        }
        if(segments.isEmpty())return new PlanResult(true,Collections.<BlockPos>emptyList());

        Map<BlockPos,Integer> requirementIds=new HashMap<BlockPos,Integer>();
        for(Segment segment:segments){
            for(int i=0;i<segment.path.size();i++){
                BlockPos pos=segment.path.get(i);
                if(!segment.powered[i]&&nearPlayer(pos,playerX,playerY,playerZ,DISPLAY_RADIUS)){
                    requirementIds.put(pos,requirementIds.size());
                }
            }
        }
        if(requirementIds.isEmpty())return new PlanResult(true,Collections.<BlockPos>emptyList());

        Map<BlockPos,SourceCandidate> candidateBySource=new HashMap<BlockPos,SourceCandidate>();
        for(Segment segment:segments){
            for(int i=0;i<segment.path.size();i++){
                BlockPos rail=segment.path.get(i);
                if(segment.powered[i]||!requirementIds.containsKey(rail))continue;
                for(BlockPos source:sourcePositions(rail)){
                    if(!loaded(level,source)||isAnyRail(level.getBlockState(source)))continue;
                    SourceCandidate candidate=candidateBySource.get(source);
                    if(candidate==null){
                        candidate=new SourceCandidate(source);
                        candidateBySource.put(source,candidate);
                    }
                    candidate.addDirectRail(segment,i);
                }
            }
        }

        List<SourceCandidate> candidates=new ArrayList<SourceCandidate>();
        for(SourceCandidate candidate:candidateBySource.values()){
            candidate.buildCoverage(requirementIds);
            if(!candidate.coverage.isEmpty())candidates.add(candidate);
        }
        if(candidates.isEmpty())return new PlanResult(true,Collections.<BlockPos>emptyList());

        Collections.sort(candidates,new Comparator<SourceCandidate>(){
            @Override public int compare(SourceCandidate a,SourceCandidate b){
                int c=Integer.compare(a.source.getX(),b.source.getX());
                if(c!=0)return c;
                c=Integer.compare(a.source.getY(),b.source.getY());
                return c!=0?c:Integer.compare(a.source.getZ(),b.source.getZ());
            }
        });

        int[][] coverage=new int[candidates.size()][];
        int[] sharedSegments=new int[candidates.size()];
        double[] distanceSq=new double[candidates.size()];
        for(int i=0;i<candidates.size();i++){
            SourceCandidate candidate=candidates.get(i);
            coverage[i]=candidate.coverageArray();
            sharedSegments[i]=candidate.segmentIds.size();
            distanceSq[i]=distanceSq(candidate.source,playerX,playerY,playerZ);
        }

        int[] selected=RailPowerPlanner.selectSources(requirementIds.size(),coverage,sharedSegments,distanceSq,MAX_SOURCES);
        boolean[] covered=new boolean[requirementIds.size()];
        LinkedHashSet<BlockPos> markerSet=new LinkedHashSet<BlockPos>();

        for(int index:selected){
            SourceCandidate candidate=candidates.get(index);
            Set<Integer> shownSegments=new HashSet<Integer>();
            for(RailRef direct:candidate.directRails){
                if(shownSegments.contains(direct.segment.id))continue;
                if(directContributes(direct,requirementIds,covered)){
                    markerSet.add(direct.pos());
                    shownSegments.add(direct.segment.id);
                }
            }
            for(int id:coverage[index])if(id>=0&&id<covered.length)covered[id]=true;
        }

        List<BlockPos> result=new ArrayList<BlockPos>(markerSet);
        Collections.sort(result,new Comparator<BlockPos>(){
            @Override public int compare(BlockPos a,BlockPos b){
                int c=Double.compare(distanceSq(a,playerX,playerY,playerZ),distanceSq(b,playerX,playerY,playerZ));
                if(c!=0)return c;
                c=Integer.compare(a.getY(),b.getY());
                if(c!=0)return c;
                c=Integer.compare(a.getX(),b.getX());
                return c!=0?c:Integer.compare(a.getZ(),b.getZ());
            }
        });
        if(result.size()>MAX_MARKERS)result=new ArrayList<BlockPos>(result.subList(0,MAX_MARKERS));
        return new PlanResult(true,Collections.unmodifiableList(result));
    }

    private static boolean directContributes(RailRef direct,Map<BlockPos,Integer> requirementIds,boolean[] covered){
        int start=Math.max(0,direct.index-RailPowerPlanner.POWER_PROPAGATION_RADIUS);
        int end=Math.min(direct.segment.path.size()-1,direct.index+RailPowerPlanner.POWER_PROPAGATION_RADIUS);
        for(int i=start;i<=end;i++){
            Integer id=requirementIds.get(direct.segment.path.get(i));
            if(id!=null&&!covered[id])return true;
        }
        return false;
    }

    private static List<BlockPos> findLocalRails(ClientLevel level,RailKind kind,double playerX,double playerY,double playerZ){
        int centerX=(int)Math.floor(playerX),centerY=(int)Math.floor(playerY),centerZ=(int)Math.floor(playerZ);
        int radiusSq=SCAN_RADIUS*SCAN_RADIUS;
        List<BlockPos> rails=new ArrayList<BlockPos>();

        for(int dx=-SCAN_RADIUS;dx<=SCAN_RADIUS;dx++){
            for(int dz=-SCAN_RADIUS;dz<=SCAN_RADIUS;dz++){
                if(dx*dx+dz*dz>radiusSq)continue;
                int x=centerX+dx,z=centerZ+dz;
                BlockPos column=new BlockPos(x,centerY,z);
                if(!loaded(level,column))continue;
                for(int dy=-VERTICAL_SCAN_RADIUS;dy<=VERTICAL_SCAN_RADIUS;dy++){
                    BlockPos pos=new BlockPos(x,centerY+dy,z);
                    if(RailKind.from(level.getBlockState(pos))==kind)rails.add(pos);
                }
            }
        }
        return rails;
    }

    private static BlockPos[] sourcePositions(BlockPos rail){
        int x=rail.getX(),y=rail.getY(),z=rail.getZ();
        return new BlockPos[]{
                new BlockPos(x+1,y,z),
                new BlockPos(x-1,y,z),
                new BlockPos(x,y,z+1),
                new BlockPos(x,y,z-1)
        };
    }

    private static boolean isAnyRail(BlockState state){
        return state.getBlock()==Blocks.RAIL||
                state.getBlock()==Blocks.POWERED_RAIL||
                state.getBlock()==Blocks.DETECTOR_RAIL||
                state.getBlock()==Blocks.ACTIVATOR_RAIL;
    }

    private static boolean nearPlayer(BlockPos pos,double x,double y,double z,int radius){
        return distanceSq(pos,x,y,z)<=radius*radius;
    }

    private static double distanceSq(BlockPos pos,double x,double y,double z){
        double dx=pos.getX()+0.5-x,dy=pos.getY()+0.5-y,dz=pos.getZ()+0.5-z;
        return dx*dx+dy*dy+dz*dz;
    }

    private enum RailKind {
        POWERED,ACTIVATOR;

        static RailKind from(BlockState state){
            if(state.getBlock()==Blocks.POWERED_RAIL)return POWERED;
            if(state.getBlock()==Blocks.ACTIVATOR_RAIL)return ACTIVATOR;
            return null;
        }
    }

    private static final class Segment {
        final int id;
        final RailKind kind;
        final List<BlockPos> path;
        final boolean[] powered;

        private Segment(int id,RailKind kind,List<BlockPos> path,boolean[] powered){
            this.id=id;
            this.kind=kind;
            this.path=path;
            this.powered=powered;
        }

        static Segment create(ClientLevel level,BlockPos seed,RailKind kind,int id){
            if(RailKind.from(level.getBlockState(seed))!=kind)return null;

            SideTrace left=traceSide(level,seed,0,kind);
            SideTrace right=traceSide(level,seed,1,kind);
            if(!left.valid||!right.valid)return null;

            List<BlockPos> ordered=new ArrayList<BlockPos>(left.positions.size()+1+right.positions.size());
            for(int i=left.positions.size()-1;i>=0;i--)ordered.add(left.positions.get(i));
            ordered.add(seed);
            ordered.addAll(right.positions);

            boolean[] powered=new boolean[ordered.size()];
            for(int i=0;i<ordered.size();i++){
                BlockState state=level.getBlockState(ordered.get(i));
                if(RailKind.from(state)!=kind)return null;
                powered[i]=state.getValue(PoweredRailBlock.POWERED);
            }
            return new Segment(id,kind,ordered,powered);
        }
    }

    private static final class RailRef {
        final Segment segment;
        final int index;

        RailRef(Segment segment,int index){
            this.segment=segment;
            this.index=index;
        }

        BlockPos pos(){return segment.path.get(index);}
    }

    private static final class SourceCandidate {
        final BlockPos source;
        final List<RailRef> directRails=new ArrayList<RailRef>();
        final Set<Integer> segmentIds=new HashSet<Integer>();
        final Set<Integer> coverage=new HashSet<Integer>();

        SourceCandidate(BlockPos source){this.source=source;}

        void addDirectRail(Segment segment,int index){
            for(RailRef direct:directRails){
                if(direct.segment.id==segment.id&&direct.index==index)return;
            }
            directRails.add(new RailRef(segment,index));
            segmentIds.add(segment.id);
        }

        void buildCoverage(Map<BlockPos,Integer> requirementIds){
            coverage.clear();
            for(RailRef direct:directRails){
                int start=Math.max(0,direct.index-RailPowerPlanner.POWER_PROPAGATION_RADIUS);
                int end=Math.min(direct.segment.path.size()-1,direct.index+RailPowerPlanner.POWER_PROPAGATION_RADIUS);
                for(int i=start;i<=end;i++){
                    Integer id=requirementIds.get(direct.segment.path.get(i));
                    if(id!=null)coverage.add(id);
                }
            }
        }

        int[] coverageArray(){
            List<Integer> ids=new ArrayList<Integer>(coverage);
            Collections.sort(ids);
            int[] result=new int[ids.size()];
            for(int i=0;i<result.length;i++)result[i]=ids.get(i);
            return result;
        }
    }

    private static SideTrace traceSide(ClientLevel level,BlockPos seed,int endpointIndex,RailKind kind){
        List<BlockPos> positions=new ArrayList<BlockPos>();
        Set<BlockPos> visited=new HashSet<BlockPos>();
        visited.add(seed);

        Connection connection=connectionAt(level,seed,endpointIndex,kind);
        if(!connection.valid)return SideTrace.invalid();
        if(connection.next==null)return new SideTrace(positions,true);

        BlockPos previous=seed,current=connection.next;
        while(positions.size()<MAX_TRACE_RAILS){
            if(!visited.add(current))return SideTrace.invalid();
            positions.add(current);

            ForwardStep step=forwardStep(level,current,previous,kind);
            if(!step.valid)return SideTrace.invalid();
            if(step.next==null)return new SideTrace(positions,true);
            previous=current;
            current=step.next;
        }
        return new SideTrace(positions,true);
    }

    private static ForwardStep forwardStep(ClientLevel level,BlockPos current,BlockPos previous,RailKind kind){
        Connection first=connectionAt(level,current,0,kind);
        Connection second=connectionAt(level,current,1,kind);
        if(!first.valid||!second.valid)return ForwardStep.invalid();

        boolean firstBack=previous.equals(first.next);
        boolean secondBack=previous.equals(second.next);
        if(firstBack==secondBack)return ForwardStep.invalid();
        return new ForwardStep(firstBack?second.next:first.next,true);
    }

    private static Connection connectionAt(ClientLevel level,BlockPos pos,int endpointIndex,RailKind kind){
        BlockState state=level.getBlockState(pos);
        if(RailKind.from(state)!=kind)return Connection.invalid();

        Endpoint endpoint=endpoints(pos,state.getValue(PoweredRailBlock.SHAPE))[endpointIndex];
        int dx=endpoint.x2==pos.getX()*2?-1:endpoint.x2==pos.getX()*2+2?1:0;
        int dz=endpoint.z2==pos.getZ()*2?-1:endpoint.z2==pos.getZ()*2+2?1:0;
        if((dx==0)==(dz==0))return Connection.invalid();

        BlockPos column=new BlockPos(pos.getX()+dx,pos.getY(),pos.getZ()+dz);
        if(!loaded(level,column))return new Connection(null,true);

        BlockPos match=null;
        for(int dy=-1;dy<=1;dy++){
            BlockPos candidate=new BlockPos(column.getX(),pos.getY()+dy,column.getZ());
            BlockState candidateState=level.getBlockState(candidate);
            if(RailKind.from(candidateState)!=kind)continue;
            Endpoint[] candidateEndpoints=endpoints(candidate,candidateState.getValue(PoweredRailBlock.SHAPE));
            if(endpoint.equals(candidateEndpoints[0])||endpoint.equals(candidateEndpoints[1])){
                if(match!=null&&!match.equals(candidate))return Connection.invalid();
                match=candidate;
            }
        }
        return new Connection(match,true);
    }

    private static Endpoint[] endpoints(BlockPos pos,RailShape shape){
        int x=pos.getX()*2,y=pos.getY()*2,z=pos.getZ()*2;
        switch(shape){
            case NORTH_SOUTH:
                return new Endpoint[]{new Endpoint(x+1,y,z),new Endpoint(x+1,y,z+2)};
            case EAST_WEST:
                return new Endpoint[]{new Endpoint(x,y,z+1),new Endpoint(x+2,y,z+1)};
            case ASCENDING_NORTH:
                return new Endpoint[]{new Endpoint(x+1,y+2,z),new Endpoint(x+1,y,z+2)};
            case ASCENDING_SOUTH:
                return new Endpoint[]{new Endpoint(x+1,y,z),new Endpoint(x+1,y+2,z+2)};
            case ASCENDING_WEST:
                return new Endpoint[]{new Endpoint(x,y+2,z+1),new Endpoint(x+2,y,z+1)};
            case ASCENDING_EAST:
                return new Endpoint[]{new Endpoint(x,y,z+1),new Endpoint(x+2,y+2,z+1)};
            default:
                throw new IllegalArgumentException("Unsupported powered rail shape: "+shape);
        }
    }

    private static boolean loaded(ClientLevel level,BlockPos pos){
        return level.hasChunkAt(pos);
    }

    private static final class Endpoint {
        final int x2,y2,z2;

        Endpoint(int x2,int y2,int z2){
            this.x2=x2;
            this.y2=y2;
            this.z2=z2;
        }

        @Override public boolean equals(Object obj){
            if(this==obj)return true;
            if(!(obj instanceof Endpoint))return false;
            Endpoint other=(Endpoint)obj;
            return x2==other.x2&&y2==other.y2&&z2==other.z2;
        }

        @Override public int hashCode(){return (x2*31+y2)*31+z2;}
    }

    private static final class Connection {
        final BlockPos next;
        final boolean valid;

        Connection(BlockPos next,boolean valid){
            this.next=next;
            this.valid=valid;
        }

        static Connection invalid(){return new Connection(null,false);}
    }

    private static final class ForwardStep {
        final BlockPos next;
        final boolean valid;

        ForwardStep(BlockPos next,boolean valid){
            this.next=next;
            this.valid=valid;
        }

        static ForwardStep invalid(){return new ForwardStep(null,false);}
    }

    private static final class SideTrace {
        final List<BlockPos> positions;
        final boolean valid;

        SideTrace(List<BlockPos> positions,boolean valid){
            this.positions=positions;
            this.valid=valid;
        }

        static SideTrace invalid(){return new SideTrace(Collections.<BlockPos>emptyList(),false);}
    }

    private static final class PlanResult {
        final boolean hasRails;
        final List<BlockPos> markers;

        PlanResult(boolean hasRails,List<BlockPos> markers){
            this.hasRails=hasRails;
            this.markers=markers;
        }

        static PlanResult none(){return new PlanResult(false,Collections.<BlockPos>emptyList());}
    }
}

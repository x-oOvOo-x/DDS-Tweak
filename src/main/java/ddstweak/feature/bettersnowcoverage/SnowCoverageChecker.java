package ddstweak.feature.bettersnowcoverage;

//#if MC >= 12111
import ddstweak.config.Configs;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public final class SnowCoverageChecker {
    static final int MAX_HORIZONTAL_DISTANCE=8;
    static final int MAX_VERTICAL_DISTANCE=2;

    private SnowCoverageChecker(){}

    public static SnowCoverageResult evaluate(BlockGetter world,BlockPos basePos,Context context){
        if(world==null||basePos==null||!Configs.Visual.BETTER_SNOW_COVERAGE.getBooleanValue())return SnowCoverageResult.NONE;
        long key=basePos.asLong();
        SnowCoverageResult cached=context.results.get(key);
        if(cached!=null)return cached;

        BlockPos above=basePos.above();
        BlockState aboveState=world.getBlockState(above);
        if(matches(aboveState,context.blacklist))return remember(context,key,SnowCoverageResult.NONE);
        if(context.whitelistEnabled&&!matches(aboveState,context.whitelist))return remember(context,key,SnowCoverageResult.NONE);

        if(aboveState.isFaceSturdy(world,above,Direction.DOWN)
                ||aboveState.is(BlockTags.AIR)
                ||aboveState.is(BlockTags.SNOW)
                ||aboveState.getBlock()==Blocks.WATER
                ||!world.getBlockState(basePos).isCollisionShapeFullBlock(world,basePos)){
            return remember(context,key,SnowCoverageResult.NONE);
        }

        SnowCoverageResult result=hasSnowNeighbor(world,above,context)
                ?SnowCoverageResult.WITH_LAYER
                :SnowCoverageResult.NONE;
        return remember(context,key,result);
    }

    public static BlockState snowyState(BlockState state){
        return state.hasProperty(BlockStateProperties.SNOWY)
                ?state.setValue(BlockStateProperties.SNOWY,true)
                :state;
    }

    private static SnowCoverageResult remember(Context context,long key,SnowCoverageResult result){
        context.results.put(key,result);
        return result;
    }

    private static boolean matches(BlockState state,List<BlockRule> rules){
        for(BlockRule rule:rules)if(rule.matches(state))return true;
        return false;
    }

    private static List<BlockRule> parseRules(List<String> entries){
        List<BlockRule> rules=new ArrayList<>();
        for(String raw:entries){
            if(raw==null)continue;
            String value=raw.trim();
            if(value.isEmpty())continue;
            boolean tag=value.charAt(0)=='#';
            Identifier id=Identifier.tryParse(tag?value.substring(1):value);
            if(id!=null)rules.add(tag?BlockRule.tag(id):BlockRule.block(id));
        }
        return rules;
    }

    private static boolean hasConfiguredEntries(List<String> entries){
        for(String raw:entries)if(raw!=null&&!raw.trim().isEmpty())return true;
        return false;
    }

    private static boolean hasSnowNeighbor(BlockGetter world,BlockPos above,Context context){
        DirectionWalker[] walkers={
                new DirectionWalker(Direction.NORTH,above.below()),
                new DirectionWalker(Direction.EAST,above.below()),
                new DirectionWalker(Direction.SOUTH,above.below()),
                new DirectionWalker(Direction.WEST,above.below())
        };
        boolean[] active={true,true,true,true};
        int snowCount=0;
        int noneCount=0;

        for(int distance=0;distance<MAX_HORIZONTAL_DISTANCE;distance++){
            for(int i=0;i<walkers.length;i++){
                if(!active[i])continue;
                Probe probe=walkers[i].next(world,context);
                if(probe.result==ProbeResult.CONTINUE)continue;
                active[i]=false;
                if(probe.result==ProbeResult.SNOW)snowCount++;
                else if(probe.result==ProbeResult.NONE)noneCount++;
            }

            if(context.mode==SnowCoverageMode.PREFER_SNOW&&(snowCount!=0||noneCount!=0))return snowCount>=noneCount;
            if(context.mode==SnowCoverageMode.ALL_SIDES){
                if(noneCount!=0)return false;
                if(snowCount==4)return true;
            }
        }
        return false;
    }

    private static Probe probe(BlockGetter world,BlockPos start,Context context){
        Probe cached=context.probes.get(start.asLong());
        if(cached!=null)return cached;

        BlockPos pos=start;
        int remaining=MAX_VERTICAL_DISTANCE;
        while(!world.getBlockState(pos).isCollisionShapeFullBlock(world,pos)){
            if(remaining--<=0)return rememberProbe(context,start,new Probe(pos,ProbeResult.UNDEFINED));
            pos=pos.below();
        }
        while(world.getBlockState(pos.above()).isCollisionShapeFullBlock(world,pos.above())){
            if(remaining--<=0)return rememberProbe(context,start,new Probe(pos,ProbeResult.UNDEFINED));
            pos=pos.above();
        }

        BlockState surface=world.getBlockState(pos.above());
        ProbeResult result=surface.is(BlockTags.SNOW)
                ?ProbeResult.SNOW
                :surface.is(BlockTags.AIR)?ProbeResult.NONE:ProbeResult.CONTINUE;
        return rememberProbe(context,start,new Probe(pos,result));
    }

    private static Probe rememberProbe(Context context,BlockPos start,Probe probe){
        context.probes.put(start.asLong(),probe);
        return probe;
    }

    public static final class Context {
        private final Long2ObjectOpenHashMap<SnowCoverageResult> results=new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<Probe> probes=new Long2ObjectOpenHashMap<>();
        private final List<BlockRule> blacklist=parseRules(Configs.Visual.BETTER_SNOW_COVERAGE_BLACKLIST.getStrings());
        private final List<BlockRule> whitelist=parseRules(Configs.Visual.BETTER_SNOW_COVERAGE_WHITELIST.getStrings());
        private final boolean whitelistEnabled=hasConfiguredEntries(Configs.Visual.BETTER_SNOW_COVERAGE_WHITELIST.getStrings());
        private final SnowCoverageMode mode=(SnowCoverageMode)Configs.Visual.BETTER_SNOW_COVERAGE_MODE.getOptionListValue();
    }

    private static final class BlockRule {
        private final Identifier id;
        private final TagKey<Block> tag;

        private BlockRule(Identifier id,TagKey<Block> tag){this.id=id;this.tag=tag;}
        private static BlockRule block(Identifier id){return new BlockRule(id,null);}
        private static BlockRule tag(Identifier id){return new BlockRule(null,TagKey.create(Registries.BLOCK,id));}

        private boolean matches(BlockState state){
            return tag!=null?state.is(tag):id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }
    }

    private static final class DirectionWalker {
        private final Direction direction;
        private BlockPos lastPos;

        private DirectionWalker(Direction direction,BlockPos lastPos){
            this.direction=direction;
            this.lastPos=lastPos;
        }

        private Probe next(BlockGetter world,Context context){
            Probe probe=probe(world,lastPos.relative(direction),context);
            lastPos=probe.surfaceBase;
            return probe;
        }
    }

    private static final class Probe {
        private final BlockPos surfaceBase;
        private final ProbeResult result;

        private Probe(BlockPos surfaceBase,ProbeResult result){
            this.surfaceBase=surfaceBase;
            this.result=result;
        }
    }

    private enum ProbeResult {
        CONTINUE,
        SNOW,
        NONE,
        UNDEFINED
    }
}
//#endif

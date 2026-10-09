package ddstweak.feature.bettersnowcoverage;

//#if MC >= 12111
import ddstweak.config.Configs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicLong;

public final class SnowCoverageBuildContext {
    private static final AtomicLong GENERATION=new AtomicLong();
    private static final ThreadLocal<SectionCache> CACHE=ThreadLocal.withInitial(SectionCache::new);

    private SnowCoverageBuildContext(){}

    public static void invalidate(){GENERATION.incrementAndGet();CACHE.remove();}

    public static SnowCoverageResult evaluate(BlockGetter world,BlockPos pos){
        if(!Configs.Visual.BETTER_SNOW_COVERAGE.getBooleanValue())return SnowCoverageResult.NONE;
        SectionCache cache=CACHE.get();
        cache.prepare(world,pos,GENERATION.get());
        return SnowCoverageChecker.evaluate(world,pos,cache.context);
    }

    public static BlockState snowyState(BlockState state){return SnowCoverageChecker.snowyState(state);}

    private static final class SectionCache {
        private BlockGetter world;
        private long generation=Long.MIN_VALUE;
        private int sectionX=Integer.MIN_VALUE,sectionY=Integer.MIN_VALUE,sectionZ=Integer.MIN_VALUE;
        private SnowCoverageChecker.Context context=new SnowCoverageChecker.Context();

        private void prepare(BlockGetter nextWorld,BlockPos pos,long nextGeneration){
            int x=pos.getX()>>4,y=pos.getY()>>4,z=pos.getZ()>>4;
            if(world==nextWorld&&generation==nextGeneration&&sectionX==x&&sectionY==y&&sectionZ==z)return;
            world=nextWorld;
            generation=nextGeneration;
            sectionX=x;
            sectionY=y;
            sectionZ=z;
            context=new SnowCoverageChecker.Context();
        }
    }
}
//#endif

package ddstweak.feature.bettersnowcoverage;

//#if MC >= 12111
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//#if MC >= 260000
//$$ import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
//#else
import net.minecraft.client.renderer.block.model.BlockStateModel;
//#endif

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BetterSnowModelLoading {
    private static final Map<BlockState,BlockStateModel> MODELS=new ConcurrentHashMap<>();

    private BetterSnowModelLoading(){}

    public static void register(){
        ModelLoadingPlugin.register(context->{
            MODELS.clear();
            SnowCoverageBuildContext.invalidate();
            context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE,(model,modelContext)->{
                BlockState state=modelContext.state();
                MODELS.put(state,model);
                if(state.isAir()||state.is(BlockTags.SNOW)||state.getBlock()==Blocks.WATER)return model;
                return new BetterSnowBlockStateModel(model);
            });
        });
    }

    static BlockStateModel model(BlockState state){return MODELS.get(state);}
}
//#endif

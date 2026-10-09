package ddstweak.feature.bettersnowcoverage;

//#if MC >= 12111
import ddstweak.config.Configs;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
//#if MC >= 260000
//$$ import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
//$$ import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
//$$ import net.minecraft.client.renderer.block.BlockAndTintGetter;
//$$ import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
//#else
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.BlockAndTintGetter;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Predicate;

public final class BetterSnowBlockStateModel extends WrapperBlockStateModel {
    private static final BlockState SNOW=Blocks.SNOW.defaultBlockState();

    BetterSnowBlockStateModel(BlockStateModel wrapped){super(wrapped);}

    @Override
    public void emitQuads(QuadEmitter emitter,BlockAndTintGetter level,BlockPos pos,BlockState state,RandomSource random,Predicate<Direction> cullTest){
        if(!Configs.Visual.BETTER_SNOW_COVERAGE.getBooleanValue()){
            fabric(wrapped).emitQuads(emitter,level,pos,state,random,cullTest);
            return;
        }

        RenderPlan plan=plan(level,pos,state);
        emit(plan.baseModel,emitter,level,pos,plan.baseState,random,cullTest);

        if(plan.layerModel!=null){
            BlockPos snowPos=pos.above();
            emitter.pushTransform(quad->{
                for(int vertex=0;vertex<4;vertex++)quad.pos(vertex,quad.x(vertex),quad.y(vertex)+1.0F,quad.z(vertex));
                quad.cullFace(null);
                //#if MC == 12111
                quad.renderLayer(ItemBlockRenderTypes.getChunkRenderType(SNOW));
                //#endif
                return true;
            });
            emit(plan.layerModel,emitter,level,snowPos,SNOW,random,direction->false);
            emitter.popTransform();
        }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level,BlockPos pos,BlockState state,RandomSource random){
        if(!Configs.Visual.BETTER_SNOW_COVERAGE.getBooleanValue())return fabric(wrapped).createGeometryKey(level,pos,state,random);

        RenderPlan plan=plan(level,pos,state);
        random.setSeed(plan.baseState.getSeed(pos));
        Object baseKey=fabric(plan.baseModel).createGeometryKey(level,pos,plan.baseState,random);
        if(baseKey==null)return null;
        if(plan.baseModel==wrapped&&plan.layerModel==null)return baseKey;

        Object layerKey=null;
        if(plan.layerModel!=null){
            BlockPos snowPos=pos.above();
            random.setSeed(SNOW.getSeed(snowPos));
            layerKey=fabric(plan.layerModel).createGeometryKey(level,snowPos,SNOW,random);
            if(layerKey==null)return null;
        }
        return new GeometryKey(plan.baseState,baseKey,layerKey);
    }

    private RenderPlan plan(BlockAndTintGetter level,BlockPos pos,BlockState state){
        BlockState baseState=state;
        BlockStateModel baseModel=wrapped;

        SnowCoverageResult coverage=SnowCoverageBuildContext.evaluate(level,pos);
        if(coverage==SnowCoverageResult.WITH_LAYER){
            if(state.hasProperty(BlockStateProperties.SNOWY)&&!state.getValue(BlockStateProperties.SNOWY)){
                BlockState snowy=SnowCoverageBuildContext.snowyState(state);
                BlockStateModel snowyModel=BetterSnowModelLoading.model(snowy);
                if(snowyModel!=null){
                    baseState=snowy;
                    baseModel=snowyModel;
                }
            }
        }

        BlockStateModel layerModel=coverage==SnowCoverageResult.WITH_LAYER
                ?BetterSnowModelLoading.model(SNOW)
                :null;
        return new RenderPlan(baseModel,baseState,layerModel);
    }

    private static void emit(BlockStateModel model,QuadEmitter emitter,BlockAndTintGetter level,BlockPos pos,BlockState state,RandomSource random,Predicate<Direction> cullTest){
        random.setSeed(state.getSeed(pos));
        fabric(model).emitQuads(emitter,level,pos,state,random,cullTest);
    }

    private static FabricBlockStateModel fabric(BlockStateModel model){return (FabricBlockStateModel)(Object)model;}

    private record RenderPlan(BlockStateModel baseModel,BlockState baseState,BlockStateModel layerModel){}
    private record GeometryKey(BlockState baseState,Object baseKey,Object layerKey){}
}
//#endif

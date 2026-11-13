package ddstweak.mixin.gui;

//#if MC >= 260100
//#if MC < 260300

//$$ import com.mojang.blaze3d.pipeline.RenderPipeline;
//$$ import java.util.Map;
//$$ import net.minecraft.client.renderer.RenderPipelines;
//$$ import net.minecraft.resources.Identifier;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;

//$$ /** GPU Pipeline registry access only for 26.1.2 and 26.2. */
//$$ @Mixin(RenderPipelines.class)
//$$ public interface RenderPipelinesAccess {
    //$$ @Accessor("PIPELINES_BY_LOCATION")
    //$$ static Map<Identifier, RenderPipeline> ddsTweak$getPipelinesByLocation() {
        //$$ throw new AssertionError("Mixin accessor not transformed");
    //$$ }
//$$ }

//#endif
//#endif

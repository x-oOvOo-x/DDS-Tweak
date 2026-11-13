package ddstweak.gui;

import ddstweak.platform.minecraft.ScreenRenderCompat;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
//#if MC >= 12106
import net.minecraft.client.renderer.RenderPipelines;
//#elseif MC >= 12102
//$$ import net.minecraft.client.renderer.RenderType;
//#endif
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#elseif MC >= 12002
//$$ import net.minecraft.resources.ResourceLocation;
//#endif

/**
 * Resource-pack replaceable icons. 1.20.2+ uses GUI atlas sprites; older
 * versions draw the same 64x64 PNG through the legacy texture API.
 */
public final class GuiIconRenderer {
    public static final String FAVORITE_OFF = "rule_favorite_off";
    public static final String FAVORITE_ON = "rule_favorite_on";
    public static final String DEFAULT_OFF = "rule_default_off";
    public static final String DEFAULT_ON = "rule_default_on";
    public static final String RESET = "rule_reset";
    public static final String PLAN_EDIT = "rule_plan_edit";
    public static final String PLAN_DELETE = "rule_plan_delete";
    public static final String VANILLA_SEARCH = "icon/search";

    private GuiIconRenderer() {}

    public static boolean draw(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            String icon,int x,int y,int width,int height,int tint){
        if(width<=0||height<=0)return true;
        //#if MC >= 12002
        String namespace=VANILLA_SEARCH.equals(icon)?"minecraft":"dds-tweak";
        String path=VANILLA_SEARCH.equals(icon)?icon:"carpet/"+icon;
        //#if MC >= 12111
        Identifier sprite=Identifier.fromNamespaceAndPath(namespace,path);
        //#elseif MC >= 12101
        //$$ ResourceLocation sprite=ResourceLocation.fromNamespaceAndPath(namespace,path);
        //#else
        //$$ ResourceLocation sprite=new ResourceLocation(namespace,path);
        //#endif
        //#if MC >= 12106
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,sprite,x,y,width,height,tint);
        //#elseif MC >= 12102
        //$$ graphics.blitSprite(RenderType::guiTextured,sprite,x,y,width,height,tint);
        //#else
        //$$ graphics.blitSprite(sprite,x,y,width,height);
        //#endif
        return true;
        //#else
        //$$ if(VANILLA_SEARCH.equals(icon))return false;
        //$$ ScreenRenderCompat.blitTintedResource(graphics,"dds-tweak",
        //$$         "textures/gui/sprites/carpet/"+icon+".png",
        //$$         x,y,0,0,width,height,64,64,tint);
        //$$ return true;
        //#endif
    }
}

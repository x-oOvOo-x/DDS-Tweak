package ddstweak.integration.provider;

import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Tweak-owned storage screen for 9-wide storage menus.
 * The server menu remains authoritative; only client rendering/input ownership changes.
 */
public final class EnhancedStorageScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    private static final int WIDTH=176;
    private static final
    //#if MC >= 12111
    Identifier
    //#else
    //$$ ResourceLocation
    //#endif
    BACKGROUND=MinecraftCompat.id("minecraft","textures/gui/container/generic_54.png");

    private final int rows;

    public EnhancedStorageScreen(AbstractContainerMenu menu,Inventory inventory,Component title,int rows){
        //#if MC >= 260100
        //$$ super(menu,inventory,title,WIDTH,114+rows*18);
        //#else
        super(menu,inventory,title);
        imageWidth=WIDTH;
        imageHeight=114+rows*18;
        //#endif
        this.rows=rows;
        //#if MC >= 11600
        inventoryLabelY=imageHeight-94;
        //#endif
    }

    public int rows(){return rows;}

    /*
     * AbstractContainerScreen does not render slot tooltips from super.render()
     * on the legacy render pipeline. Because this screen replaces vanilla
     * 9-wide storage screens, it must preserve the vanilla tooltip stage too.
     */
    //#if MC >= 260100
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){super.extractRenderState(g,mouseX,mouseY,partialTick);}
    //#elseif MC >= 12000
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);}
    //#elseif MC >= 11700
    //$$ @Override public void render(PoseStack g,int mouseX,int mouseY,float partialTick){super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack g,int mouseX,int mouseY,float partialTick){super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);}
    //#else
    //$$ @Override public void render(int mouseX,int mouseY,float partialTick){super.render(mouseX,mouseY,partialTick);renderTooltip(mouseX,mouseY);}
    //#endif

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){super.extractBackground(g,mouseX,mouseY,partialTick);renderStorageBackground(g);}
    //#elseif MC >= 12000
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY){renderStorageBackground(g);}
    //#elseif MC >= 11600
    //$$ @Override protected void renderBg(PoseStack g,float partialTick,int mouseX,int mouseY){renderStorageBackground(g);}
    //#else
    //$$ @Override protected void renderBg(float partialTick,int mouseX,int mouseY){renderStorageBackground(null);}
    //#endif

    private void renderStorageBackground(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g
        //#elseif MC >= 12000
        GuiGraphics g
        //#elseif MC >= 11600
        //$$ PoseStack g
        //#else
        //$$ Object g
        //#endif
    ){
        int upper=17+rows*18;
        blit(g,BACKGROUND,leftPos,topPos,0,0,imageWidth,upper,256,256);
        blit(g,BACKGROUND,leftPos,topPos+upper,0,126,imageWidth,96,256,256);
    }

    private static void blit(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        //#if MC >= 12111
        Identifier texture,
        //#else
        //$$ ResourceLocation texture,
        //#endif
        int x,int y,int u,int v,int w,int h,int tw,int th){
        ScreenRenderCompat.blit(g,texture,x,y,u,v,w,h,tw,th);
    }
}

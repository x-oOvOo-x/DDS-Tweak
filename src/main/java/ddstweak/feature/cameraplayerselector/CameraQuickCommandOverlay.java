package ddstweak.feature.cameraplayerselector;

import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Compact upward popup at the bottom-left of the Camera content area. */
final class CameraQuickCommandOverlay {
    private static final int BUTTON=14,WIDTH=88,ROW_HEIGHT=18,MAX_ROWS=6;
    private final CameraPlayerSelectorScreen parent;
    private List<CameraQuickCommandStore.Entry> entries=Collections.emptyList();
    private boolean open,loadFailed;
    private int offset;

    CameraQuickCommandOverlay(CameraPlayerSelectorScreen parent){this.parent=parent;}
    void reload(Minecraft minecraft){
        try{
            entries=CameraQuickCommandStore.load(minecraft);
            loadFailed=false;
        }catch(IllegalStateException failure){
            entries=Collections.emptyList();
            loadFailed=true;
            System.err.println("[DDS Tweak] "+failure.getMessage());
        }
        offset=Math.max(0,Math.min(offset,Math.max(0,entries.size()-MAX_ROWS)));
    }
    boolean isOpen(){return open;}
    void close(){open=false;}
    static int buttonX(int width){return Math.max(4,(width-Math.min(360,width-16))/2);}
    static int buttonY(int actionY){return actionY+4;}
    private int popupX(int width){return buttonX(width);}
    private int popupHeight(){return (visibleRows()+1)*ROW_HEIGHT+4;}
    private int popupY(int actionY){return buttonY(actionY)-popupHeight()-4;}
    private int visibleRows(){return Math.min(MAX_ROWS,entries.size());}
    /** The popup has a two-pixel top inset; clicking its border is not row zero. */
    static int rowIndex(double mouseY,int popupTop){
        double localY=mouseY-popupTop-2;
        return localY<0?-1:(int)(localY/ROW_HEIGHT);
    }
    private static boolean inside(double x,double y,int left,int top,int w,int h){
        return x>=left&&x<left+w&&y>=top&&y<top+h;
    }

    void render(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            Font font,int width,int actionY,int mouseX,int mouseY){
        int bx=buttonX(width),by=buttonY(actionY);
        boolean hoverButton=inside(mouseX,mouseY,bx,by,BUTTON,BUTTON);
        CameraSlotRender.fill(graphics,bx,by,BUTTON,BUTTON,hoverButton||open?0x90000000:0x32000000);
        CameraSlotRender.outline(graphics,bx,by,BUTTON,BUTTON,hoverButton||open?0xFFD8DEE3:0x728A9299);
        ScreenRenderCompat.text(graphics,font,"/",bx+(BUTTON-font.width("/"))/2,by+3,0xFFE4E8EC,false);
        if(!open)return;

        int x=popupX(width),y=popupY(actionY),h=popupHeight();
        CameraSlotRender.fill(graphics,x,y,WIDTH,h,0xE80A0B0D);
        CameraSlotRender.outline(graphics,x,y,WIDTH,h,0xB0929BA4);
        for(int i=0;i<visibleRows();i++){
            int rowY=y+2+i*ROW_HEIGHT;
            boolean hover=inside(mouseX,mouseY,x+2,rowY,WIDTH-4,ROW_HEIGHT);
            if(hover)CameraSlotRender.fill(graphics,x+2,rowY,WIDTH-4,ROW_HEIGHT,0x686B727A);
            CameraQuickCommandStore.Entry entry=entries.get(offset+i);
            String label=entry.name;
            while(label.length()>1&&font.width(label)>WIDTH-12)label=label.substring(0,label.length()-1);
            ScreenRenderCompat.text(graphics,font,label,x+5,rowY+5,0xFFE5E8EB,false);
            if(i<visibleRows()-1)
                ScreenRenderCompat.fill(graphics,x+5,rowY+ROW_HEIGHT-1,x+WIDTH-5,rowY+ROW_HEIGHT,0x274F5358);
        }
        int addY=y+2+visibleRows()*ROW_HEIGHT;
        boolean addHover=inside(mouseX,mouseY,x+2,addY,WIDTH-4,ROW_HEIGHT);
        ScreenRenderCompat.fill(graphics,x+3,addY-1,x+WIDTH-3,addY,0x506A6E75);
        if(addHover)CameraSlotRender.fill(graphics,x+2,addY,WIDTH-4,ROW_HEIGHT,0x686B727A);
        ScreenRenderCompat.text(graphics,font,"+",x+(WIDTH-font.width("+"))/2,addY+5,0xFFD9DEE2,false);
    }

    boolean handleClick(Minecraft minecraft,int width,int actionY,double mx,double my,int button){
        boolean left=CameraSelectorCompat.leftMouse(button),right=CameraSelectorCompat.rightMouse(button);
        if(!left&&!right)return false;
        int bx=buttonX(width),by=buttonY(actionY);
        if(inside(mx,my,bx,by,BUTTON,BUTTON)){
            if(left)open=!open;
            return true;
        }
        if(!open)return false;
        int x=popupX(width),y=popupY(actionY);
        if(inside(mx,my,x,y,WIDTH,popupHeight())){
            if(loadFailed)return true;
            int index=rowIndex(my,y);
            if(index>=0&&index<visibleRows()){
                CameraQuickCommandStore.Entry entry=entries.get(offset+index);
                if(right)MinecraftCompat.setScreen(minecraft,new CameraQuickCommandEditorScreen(parent,entry));
                else{
                    String command=CameraQuickCommandStore.commandToSend(entry);
                    if(CameraCommandSyntax.send(minecraft,command))
                        CameraPlayerSelectorFeature.finishAction(parent);
                }
            }else if(index==visibleRows()&&left){
                MinecraftCompat.setScreen(minecraft,new CameraQuickCommandEditorScreen(parent,null));
            }
            return true;
        }
        open=false;
        return true; // Do not inadvertently activate a player or dimension behind the popup.
    }

    boolean scroll(int width,int actionY,double mx,double my,double amount){
        if(!open||amount==0||!inside(mx,my,popupX(width),popupY(actionY),WIDTH,popupHeight()))return false;
        int max=Math.max(0,entries.size()-MAX_ROWS);
        offset=Math.max(0,Math.min(max,offset+(amount<0?1:-1)));
        return true;
    }

    List<Component> tooltip(int width,int actionY,double mx,double my){
        int bx=buttonX(width),by=buttonY(actionY);
        if(inside(mx,my,bx,by,BUTTON,BUTTON))
            return Collections.singletonList(MinecraftCompat.translatable("dds-tweak.camera.quick.title"));
        if(!open)return null;
        int x=popupX(width),y=popupY(actionY);
        if(!inside(mx,my,x,y,WIDTH,popupHeight()))return null;
        if(loadFailed)return Collections.singletonList(MinecraftCompat.translatable("dds-tweak.camera.quick.loadFailed"));
        int index=rowIndex(my,y);
        if(index>=0&&index<visibleRows()){
            CameraQuickCommandStore.Entry entry=entries.get(offset+index);
            List<Component> lines=new ArrayList<>();
            lines.add(MinecraftCompat.literal(entry.name));
            lines.add(MinecraftCompat.grayLiteral(CameraCommandSyntax.display(entry.command)));
            lines.add(MinecraftCompat.grayTranslatable(entry.scope==CameraQuickCommandStore.Scope.GLOBAL
                    ?"dds-tweak.camera.quick.scope.global":"dds-tweak.camera.quick.scope.local"));
            lines.add(MinecraftCompat.grayTranslatable("dds-tweak.camera.quick.editHint"));
            return lines;
        }
        if(index==visibleRows())
            return Collections.singletonList(MinecraftCompat.translatable("dds-tweak.camera.quick.add"));
        return Collections.emptyList(); // Keep tooltips behind the popup from showing through.
    }
}

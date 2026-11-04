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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 12109
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import java.util.function.Consumer;

/** Edit a single command without leaving the Camera navigation screen permanently. */
final class CameraQuickCommandEditorScreen extends Screen {
    private static final int BUTTON_H=20,FIELD_H=18,DELETE_ICON=14,FOOTER_WIDTH=72,FOOTER_GAP=4;
    private final CameraPlayerSelectorScreen parent;
    private final String existingId;
    private final CameraQuickCommandStore.Scope originalScope;
    private CameraQuickCommandStore.Scope scope;
    private EditBox nameField,commandField;
    private final Button[] scopeButtons=new Button[2];
    private String draftName,draftCommand,status="";
    private String openedServerKey;

    CameraQuickCommandEditorScreen(CameraPlayerSelectorScreen parent,CameraQuickCommandStore.Entry entry){
        super(MinecraftCompat.translatable(entry==null
                ?"dds-tweak.camera.quick.editor.add":"dds-tweak.camera.quick.editor.edit"));
        this.parent=parent;
        this.existingId=entry==null?null:entry.id;
        this.originalScope=entry==null?CameraQuickCommandStore.Scope.GLOBAL:entry.scope;
        this.scope=originalScope;
        draftName=entry==null?"":entry.name;
        draftCommand=entry==null?"":entry.command;
    }

    @Override public boolean isPauseScreen(){return false;}
    private int top(){return CameraBackground.navigationTop(height);}
    private int left(){return (width-contentWidth())/2;}
    private int contentWidth(){return Math.min(380,Math.max(250,width-24));}
    private int innerLeft(){return left()+10;}
    private int innerWidth(){return contentWidth()-20;}
    private int bottomY(){return top()+CameraBackground.FOOTER_ROW_Y;}
    private int deleteY(){return bottomY()+(BUTTON_H-DELETE_ICON)/2;}

    @Override protected void init(){
        if(openedServerKey==null)openedServerKey=CameraCommandProfileStore.serverKey(minecraft);
        int x=innerLeft(),y=top(),w=innerWidth();
        nameField=editBox(x,y+55,w,draftName,64,text->{draftName=text;status="";});
        commandField=editBox(x,y+103,w,draftCommand,256,text->{draftCommand=text;status="";});
        addWidgetCompat(nameField);
        addWidgetCompat(commandField);
        int half=(w-FOOTER_GAP)/2;
        scopeButtons[0]=button(x,y+154,half,tr("dds-tweak.camera.quick.scope.global"),
                ()->setScope(CameraQuickCommandStore.Scope.GLOBAL));
        scopeButtons[1]=button(x+half+FOOTER_GAP,y+154,w-half-FOOTER_GAP,
                tr("dds-tweak.camera.quick.scope.local"),()->setScope(CameraQuickCommandStore.Scope.LOCAL));
        for(Button option:scopeButtons)addWidgetCompat(option);
        refreshScopeButtons();
        int saveX=x+w-FOOTER_WIDTH;
        int cancelX=saveX-FOOTER_GAP-FOOTER_WIDTH;
        addWidgetCompat(button(cancelX,bottomY(),FOOTER_WIDTH,tr("dds-tweak.camera.destination.cancel"),this::onClose));
        addWidgetCompat(button(saveX,bottomY(),FOOTER_WIDTH,tr("dds-tweak.camera.destination.save"),this::saveEntry));
    }

    private void saveEntry(){
        String error=CameraQuickCommandStore.validate(draftName,draftCommand);
        if(error!=null){
            status=tr("dds-tweak.camera.quick.editor.error."+error);
            return;
        }
        try{
            if(scope==CameraQuickCommandStore.Scope.LOCAL||originalScope==CameraQuickCommandStore.Scope.LOCAL)
                CameraCommandProfileStore.requireServerIdentity(openedServerKey,
                        CameraCommandProfileStore.serverKey(minecraft));
            CameraQuickCommandStore.upsert(minecraft,
                    new CameraQuickCommandStore.Entry(existingId,draftName,draftCommand,scope),originalScope);
            parent.reloadQuickCommands();
            MinecraftCompat.setScreen(minecraft,parent);
        }catch(IllegalStateException failure){
            status=tr("dds-tweak.camera.quick.saveFailed");
            System.err.println("[DDS Tweak] "+failure.getMessage());
        }
    }

    private void deleteEntry(){
        if(existingId==null||!CameraSelectorCompat.shiftDown(minecraft))return;
        try{
            if(originalScope==CameraQuickCommandStore.Scope.LOCAL)
                CameraCommandProfileStore.requireServerIdentity(openedServerKey,
                        CameraCommandProfileStore.serverKey(minecraft));
            CameraQuickCommandStore.delete(minecraft,existingId,originalScope);
            parent.reloadQuickCommands();
            MinecraftCompat.setScreen(minecraft,parent);
        }catch(IllegalStateException failure){
            status=tr("dds-tweak.camera.quick.saveFailed");
            System.err.println("[DDS Tweak] "+failure.getMessage());
        }
    }

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(super.mouseClicked(event,doubleClick))return true;
        return handleEditorClick(event.x(),event.y(),event.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
    //$$     if(super.mouseClicked(mouseX,mouseY,button))return true;
    //$$     return handleEditorClick(mouseX,mouseY,button);
    //$$ }
    //#endif

    private void setScope(CameraQuickCommandStore.Scope target){
        if(target==CameraQuickCommandStore.Scope.LOCAL&&!CameraQuickCommandStore.hasLocalScope(minecraft))return;
        scope=target;status="";refreshScopeButtons();
    }

    private void refreshScopeButtons(){
        CameraOptionStyle.update(scopeButtons[0],scope==CameraQuickCommandStore.Scope.GLOBAL,true);
        CameraOptionStyle.update(scopeButtons[1],scope==CameraQuickCommandStore.Scope.LOCAL,
                CameraQuickCommandStore.hasLocalScope(minecraft));
    }

    private boolean handleEditorClick(double mouseX,double mouseY,int button){
        if(!CameraSelectorCompat.leftMouse(button))return false;
        int x=innerLeft(),y=top()+154,w=innerWidth(),half=(w-FOOTER_GAP)/2;
        if(mouseY>=y&&mouseY<y+BUTTON_H){
            if(mouseX>=x&&mouseX<x+half){
                setScope(CameraQuickCommandStore.Scope.GLOBAL);return true;
            }
            if(mouseX>=x+half+FOOTER_GAP&&mouseX<x+w){
                setScope(CameraQuickCommandStore.Scope.LOCAL);return true;
            }
        }
        return handleDelete(mouseX,mouseY,button);
    }

    private boolean handleDelete(double mouseX,double mouseY,int button){
        if(existingId==null||!CameraSelectorCompat.leftMouse(button))return false;
        if(mouseX>=innerLeft()&&mouseX<innerLeft()+DELETE_ICON
                &&mouseY>=deleteY()&&mouseY<deleteY()+DELETE_ICON){
            deleteEntry();
            return true;
        }
        return false;
    }

    @Override public void onClose(){MinecraftCompat.setScreen(minecraft,parent);}

    private EditBox editBox(int x,int y,int w,String value,int max,Consumer<String> responder){
        //#if MC >= 11600
        EditBox field=new EditBox(font,x,y,w,FIELD_H,MinecraftCompat.literal(""));
        //#else
        //$$ EditBox field=new EditBox(font,x,y,w,FIELD_H,"");
        //#endif
        field.setMaxLength(max);
        field.setValue(value);
        field.setResponder(responder);
        return field;
    }

    private Button button(int x,int y,int w,String label,Runnable action){
        //#if MC >= 11903
        return Button.builder(MinecraftCompat.literal(label),ignored->action.run()).bounds(x,y,w,BUTTON_H).build();
        //#elseif MC >= 11600
        //$$ return new Button(x,y,w,BUTTON_H,MinecraftCompat.literal(label),ignored->action.run());
        //#else
        //$$ return new Button(x,y,w,BUTTON_H,label,ignored->action.run());
        //#endif
    }

    private <T extends AbstractWidget> void addWidgetCompat(T widget){
        //#if MC >= 11700
        addRenderableWidget(widget);
        //#else
        //$$ addButton(widget);
        //#endif
    }

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){super.extractBackground(graphics,mouseX,mouseY,partialTick);}
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){drawBase(graphics);super.extractRenderState(graphics,syncScopeHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        //#if MC >= 12005
        super.renderBackground(graphics,mouseX,mouseY,partialTick);
        //#else
        //$$ renderTransparentBackground(graphics);
        //#endif
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){drawBase(graphics);super.render(graphics,syncScopeHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);drawBase(graphics);super.render(graphics,syncScopeHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);drawBase(graphics);super.render(graphics,syncScopeHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(graphics,mouseX,mouseY);}
    //#else
    //$$ @Override public void render(int mouseX,int mouseY,float partialTick){renderBackground();drawBase(null);super.render(syncScopeHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(null,mouseX,mouseY);}
    //#endif

    /** Unselected scopes use the vanilla disabled look until hovered. */
    /** Keep the selected scope visually normal, even while the mouse stays over it. */
    private int syncScopeHover(int mouseX,int mouseY){
        int x=innerLeft(),y=top()+154,w=innerWidth(),half=(w-FOOTER_GAP)/2;
        boolean row=mouseY>=y&&mouseY<y+BUTTON_H;
        boolean globalHover=row&&mouseX>=x&&mouseX<x+half;
        boolean localHover=row&&mouseX>=x+half+FOOTER_GAP&&mouseX<x+w;
        boolean globalSelected=scope==CameraQuickCommandStore.Scope.GLOBAL;
        CameraOptionStyle.update(scopeButtons[0],globalSelected,true,globalHover);
        CameraOptionStyle.update(scopeButtons[1],!globalSelected,
                CameraQuickCommandStore.hasLocalScope(minecraft),localHover);
        return CameraOptionStyle.renderMouseX(mouseX,
                (globalSelected&&globalHover)||(!globalSelected&&localHover));
    }

    private void drawBase(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics
            //#elseif MC >= 12000
            GuiGraphics graphics
            //#elseif MC >= 11600
            //$$ PoseStack graphics
            //#else
            //$$ Object graphics
            //#endif
    ){
        CameraBackground.draw(graphics,width,height,top()-8,top()+CameraBackground.NAV_HEIGHT+8);
    }
    private void drawOverlay(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY){
        int x=innerLeft(),y=top();
        centered(graphics,title.getString(),width/2,y,0xFFFFFFFF);
        CameraBackground.drawNavigationRules(graphics,width,y);
        drawText(graphics,tr("dds-tweak.camera.quick.editor.name"),x,y+40,0xFFCAD0D4);
        drawText(graphics,tr("dds-tweak.camera.quick.editor.command"),x,y+88,0xFFCAD0D4);
        drawText(graphics,tr("dds-tweak.camera.quick.editor.hint"),x,y+129,0xFFB0B6BB);
        drawText(graphics,tr("dds-tweak.camera.quick.editor.scope"),x,y+141,0xFFCAD0D4);
        if(existingId!=null){
            int deleteX=innerLeft(),deleteY=deleteY();
            boolean shift=CameraSelectorCompat.shiftDown(minecraft);
            boolean hover=mouseX>=deleteX&&mouseX<deleteX+DELETE_ICON
                    &&mouseY>=deleteY&&mouseY<deleteY+DELETE_ICON;
            ScreenRenderCompat.crossButton(graphics,deleteX,deleteY,shift&&hover,shift);
            if(hover&&status.isEmpty()){
                String hint=tr(shift?"dds-tweak.camera.destination.delete_ready":"dds-tweak.camera.destination.delete_shift");
                drawText(graphics,hint,deleteX+DELETE_ICON+6,
                        deleteY+(DELETE_ICON-font.lineHeight)/2+1,
                        shift?0xFFFF8080:0xFFB8B8B8);
            }
        }
        if(!status.isEmpty())centered(graphics,status,width/2,y+179,0xFFFF8888);
    }

    private void centered(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            String value,int x,int y,int color){
        drawText(graphics,value,x-font.width(value)/2,y,color);
    }
    private void drawText(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            String value,int x,int y,int color){
        ScreenRenderCompat.text(graphics,font,value,x,y,color,true);
    }
    private static String tr(String key){return MinecraftCompat.translatable(key).getString();}
}

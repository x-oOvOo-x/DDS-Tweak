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

/**
 * Three compact editing categories, with unsaved drafts isolated by scope.
 * 编辑全局及当前服务器的相机指令配置。
 */
final class CameraCommandConfigScreen extends Screen {
    private enum Tab { PLAYER, CAMERA, DESTINATION }
    private static final int PANEL_W=316,FIELD_H=18,BUTTON_H=18,GAP=4;
    private static final CameraCommandProfile.Action[] PLAYER={
            CameraCommandProfile.Action.PLAYER_TP,CameraCommandProfile.Action.PLAYER_FOLLOW,
            CameraCommandProfile.Action.PLAYER_DIRECT_TP};
    private static final CameraCommandProfile.Action[] DESTINATION={
            CameraCommandProfile.Action.DIMENSION,CameraCommandProfile.Action.DESTINATION};
    private final CameraPlayerSelectorScreen parent;
    private CameraCommandProfile globalDraft,serverDraft;
    private boolean global,dirtyGlobal,dirtyServer,refreshing,modeSelected;
    private String openedServerKey;
    private Tab tab=Tab.PLAYER;
    private int previewIndex;
    private final EditBox[] fields=new EditBox[3];
    private Button resetButton,restoreButton;
    private final Button[] scopeButtons=new Button[2],categoryButtons=new Button[3],modeButtons=new Button[2];
    private String message="";

    CameraCommandConfigScreen(CameraPlayerSelectorScreen parent){
        super(MinecraftCompat.translatable("dds-tweak.camera.command.title"));
        this.parent=parent;
    }

    @Override public boolean isPauseScreen(){return false;}
    private int top(){return CameraBackground.navigationTop(height);}
    private int left(){return Math.max(4,(width-contentWidth())/2);}
    private int contentWidth(){return Math.min(PANEL_W,width-8);}
    private boolean hasServer(){return !CameraCommandProfileStore.serverKey(minecraft).isEmpty();}
    private CameraCommandProfile selected(){return global?globalDraft:serverDraft;}
    private void markDirty(){if(global)dirtyGlobal=true;else dirtyServer=true;}
    private CameraCommandProfile.Action[] actions(){
        if(tab==Tab.PLAYER)return PLAYER;
        if(tab==Tab.DESTINATION)return DESTINATION;
        return selected().mode==CameraCommandProfile.Mode.TOGGLE
                ?new CameraCommandProfile.Action[]{CameraCommandProfile.Action.TOGGLE,CameraCommandProfile.Action.BACK}
                :new CameraCommandProfile.Action[]{CameraCommandProfile.Action.ENTER,CameraCommandProfile.Action.EXIT,CameraCommandProfile.Action.BACK};
    }

    @Override protected void init(){
        if(globalDraft==null){
            openedServerKey=CameraCommandProfileStore.serverKey(minecraft);
            globalDraft=CameraCommandProfileStore.loadGlobal();
            serverDraft=CameraCommandProfileStore.load(minecraft);
            global=true; // Open on global defaults; server-specific editing remains selectable.
        }
        int x=left(),y=top(),w=contentWidth(),half=(w-GAP)/2,third=(w-GAP*2)/3;
        for(int i=0;i<fields.length;i++){
            final int row=i;
            fields[i]=editBox(x+104,y+91+i*24,w-104,"",value->{
                if(refreshing)return;
                CameraCommandProfile.Action[] visible=actions();
                if(row<visible.length){
                    selected().set(visible[row],value);
                    previewIndex=row;
                    modeSelected=false;
                    markDirty();
                    message="";
                }
            });
            addWidgetCompat(fields[i]);
        }
        scopeButtons[0]=button(x,y+24,half,tr("dds-tweak.camera.command.server"),()->setScope(false));
        scopeButtons[1]=button(x+half+GAP,y+24,w-half-GAP,tr("dds-tweak.camera.command.global"),()->setScope(true));
        for(Button option:scopeButtons)addWidgetCompat(option);
        String[] labels={"player","camera","destination"};
        for(int i=0;i<categoryButtons.length;i++){
            final Tab target=Tab.values()[i];
            int bx=x+i*(third+GAP),bw=i==2?w-2*(third+GAP):third;
            categoryButtons[i]=button(bx,y+46,bw,tr("dds-tweak.camera.command.tab."+labels[i]),()->setTab(target));
            addWidgetCompat(categoryButtons[i]);
        }
        modeButtons[0]=button(x,y+68,half,tr("dds-tweak.camera.command.modeToggle"),
                ()->setMode(CameraCommandProfile.Mode.TOGGLE));
        modeButtons[1]=button(x+half+GAP,y+68,w-half-GAP,tr("dds-tweak.camera.command.modeSeparate"),
                ()->setMode(CameraCommandProfile.Mode.SEPARATE));
        for(Button option:modeButtons)addWidgetCompat(option);
        addWidgetCompat(button(x,y+169,third,tr("dds-tweak.camera.command.cda"),this::cdaPreset));
        addWidgetCompat(button(x+third+GAP,y+169,third,tr("dds-tweak.camera.command.vanilla"),this::vanillaPreset));
        resetButton=button(x+2*(third+GAP),y+169,w-2*(third+GAP),
                tr("dds-tweak.camera.command.resetCategory"),this::resetOrInherit);
        restoreButton=button(x+2*(third+GAP),y+169,w-2*(third+GAP),
                tr("dds-tweak.camera.command.restoreInherited"),this::resetOrInherit);
        addWidgetCompat(resetButton);
        addWidgetCompat(restoreButton);
        addWidgetCompat(button(x,y+CameraBackground.FOOTER_ROW_Y,half,tr("dds-tweak.camera.destination.cancel"),this::onClose));
        addWidgetCompat(button(x+half+GAP,y+CameraBackground.FOOTER_ROW_Y,w-half-GAP,tr("dds-tweak.camera.command.save"),this::save));
        refresh();
    }

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(super.mouseClicked(event,doubleClick))return true;
        return handleOptionClick(event.x(),event.y(),event.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
    //$$     if(super.mouseClicked(mouseX,mouseY,button))return true;
    //$$     return handleOptionClick(mouseX,mouseY,button);
    //$$ }
    //#endif

    private boolean handleOptionClick(double mouseX,double mouseY,int button){
        if(!CameraSelectorCompat.leftMouse(button))return false;
        int x=left(),y=top(),w=contentWidth(),half=(w-GAP)/2,third=(w-2*GAP)/3;
        if(clicked(mouseX,mouseY,x,y+24,half,BUTTON_H)){setScope(false);return true;}
        if(clicked(mouseX,mouseY,x+half+GAP,y+24,w-half-GAP,BUTTON_H)){setScope(true);return true;}
        for(int i=0;i<3;i++){
            int bx=x+i*(third+GAP),bw=i==2?w-2*(third+GAP):third;
            if(clicked(mouseX,mouseY,bx,y+46,bw,BUTTON_H)){
                setTab(Tab.values()[i]);return true;
            }
        }
        if(tab==Tab.CAMERA){
            if(clicked(mouseX,mouseY,x,y+68,half,BUTTON_H)){
                setMode(CameraCommandProfile.Mode.TOGGLE);return true;
            }
            if(clicked(mouseX,mouseY,x+half+GAP,y+68,w-half-GAP,BUTTON_H)){
                setMode(CameraCommandProfile.Mode.SEPARATE);return true;
            }
        }
        return false;
    }

    private static boolean clicked(double mx,double my,int x,int y,int w,int h){
        return mx>=x&&mx<x+w&&my>=y&&my<y+h;
    }

    private void setTab(Tab target){
        tab=target;previewIndex=0;modeSelected=false;message="";refresh();
    }
    private void setScope(boolean globalScope){
        if(!globalScope&&!hasServer())return;
        global=globalScope;
        if(!global)serverDraft.inheritFrom(globalDraft);
        previewIndex=0;modeSelected=false;message="";refresh();
    }
    private void setMode(CameraCommandProfile.Mode mode){
        if(selected().mode!=mode||( !global&&!selected().overridesMode())){
            selected().setMode(mode);
            markDirty();
        }
        previewIndex=0;modeSelected=true;message="";refresh();
    }
    private void refresh(){
        refreshing=true;
        CameraCommandProfile.Action[] visible=actions();
        for(int i=0;i<fields.length;i++){
            boolean enabled=i<visible.length;
            fields[i].visible=enabled;
            fields[i].active=enabled;
            fields[i].setValue(enabled?selected().get(visible[i]):"");
        }
        previewIndex=Math.min(previewIndex,visible.length-1);
        CameraOptionStyle.update(scopeButtons[0],!global,hasServer());
        CameraOptionStyle.update(scopeButtons[1],global,true);
        for(int i=0;i<categoryButtons.length;i++)
            CameraOptionStyle.update(categoryButtons[i],tab.ordinal()==i,true);
        boolean cameraTab=tab==Tab.CAMERA;
        for(int i=0;i<modeButtons.length;i++){
            modeButtons[i].visible=cameraTab;
            CameraOptionStyle.update(modeButtons[i],cameraTab&&selected().mode==
                    (i==0?CameraCommandProfile.Mode.TOGGLE:CameraCommandProfile.Mode.SEPARATE),cameraTab);
        }
        if(resetButton!=null)resetButton.visible=global;
        if(restoreButton!=null)restoreButton.visible=!global;
        refreshing=false;
    }
    private static CameraCommandProfile.Action[] category(Tab value){
        if(value==Tab.PLAYER)return PLAYER;
        if(value==Tab.DESTINATION)return DESTINATION;
        return new CameraCommandProfile.Action[]{
                CameraCommandProfile.Action.TOGGLE,CameraCommandProfile.Action.ENTER,
                CameraCommandProfile.Action.EXIT,CameraCommandProfile.Action.BACK};
    }
    /** Presets affect only the selected tab; unrelated actions remain untouched. */
    private void applyPreset(CameraCommandProfile preset){
        for(CameraCommandProfile.Action action:category(tab))selected().set(action,preset.get(action));
        if(tab==Tab.CAMERA)selected().setMode(preset.mode);
        markDirty();modeSelected=false;message="";refresh();
    }
    private void cdaPreset(){applyPreset(new CameraCommandProfile());}
    private void vanillaPreset(){
        CameraCommandProfile vanilla=new CameraCommandProfile();
        vanilla.mode=CameraCommandProfile.Mode.SEPARATE;
        vanilla.set(CameraCommandProfile.Action.ENTER,"");
        vanilla.set(CameraCommandProfile.Action.EXIT,"");
        vanilla.set(CameraCommandProfile.Action.TOGGLE,"");
        vanilla.set(CameraCommandProfile.Action.BACK,"");
        vanilla.set(CameraCommandProfile.Action.PLAYER_TP,"/tp {player}");
        vanilla.set(CameraCommandProfile.Action.PLAYER_FOLLOW,"");
        vanilla.set(CameraCommandProfile.Action.DIMENSION,"");
        vanilla.set(CameraCommandProfile.Action.DESTINATION,"/tp {x} {y} {z}");
        applyPreset(vanilla);
    }
    private void resetOrInherit(){
        if(global){
            for(CameraCommandProfile.Action action:category(tab))selected().set(action,action.defaultTemplate);
            if(tab==Tab.CAMERA)selected().setMode(CameraCommandProfile.Mode.TOGGLE);
        }else if(tab==Tab.CAMERA&&modeSelected){
            serverDraft.restoreMode(globalDraft);
        }else{
            CameraCommandProfile.Action[] visible=actions();
            serverDraft.restore(visible[Math.max(0,Math.min(previewIndex,visible.length-1))],globalDraft);
        }
        markDirty();message="";refresh();
    }

    private void save(){
        if(!checkDraft(globalDraft,true)||!checkDraft(serverDraft,false))return;
        try{
            if(dirtyServer)
                CameraCommandProfileStore.requireServerIdentity(openedServerKey,
                        CameraCommandProfileStore.serverKey(minecraft));
            CameraCommandProfileStore.saveBoth(minecraft,globalDraft,dirtyGlobal,serverDraft,dirtyServer);
            parent.reloadProfile();
            MinecraftCompat.setScreen(minecraft,parent);
        }catch(IllegalStateException failure){
            message=tr("dds-tweak.camera.command.saveFailed");
            System.err.println("[DDS Tweak] "+failure.getMessage());
        }
    }
    private boolean checkDraft(CameraCommandProfile draft,boolean scope){
        if(!(scope?dirtyGlobal:dirtyServer))return true;
        for(CameraCommandProfile.Action action:CameraCommandProfile.Action.values()){
            String error=CameraCommandProfile.validate(action,draft.get(action));
            if(error!=null){
                global=scope;
                tab=tabFor(action);
                previewIndex=0;
                refresh();
                CameraCommandProfile.Action[] visible=actions();
                for(int i=0;i<visible.length;i++)if(visible[i]==action)previewIndex=i;
                message=localizedError(error);
                return false;
            }
        }
        return true;
    }
    private static Tab tabFor(CameraCommandProfile.Action action){
        if(action==CameraCommandProfile.Action.PLAYER_TP||action==CameraCommandProfile.Action.PLAYER_FOLLOW
                ||action==CameraCommandProfile.Action.PLAYER_DIRECT_TP)return Tab.PLAYER;
        if(action==CameraCommandProfile.Action.DIMENSION||action==CameraCommandProfile.Action.DESTINATION)return Tab.DESTINATION;
        return Tab.CAMERA;
    }
    private static String localizedError(String error){
        String suffix=error.startsWith("Missing ")? "missing":
                error.startsWith("Unknown variable:")?"unknown":
                error.startsWith("Unexpected ")? "unexpected":
                error.equals("Control character")?"control":"invalid";
        return tr("dds-tweak.camera.command.error."+suffix);
    }
    @Override public void onClose(){MinecraftCompat.setScreen(minecraft,parent);}

    private EditBox editBox(int x,int y,int w,String value,Consumer<String> responder){
        //#if MC >= 11600
        EditBox field=new EditBox(font,x,y,w,FIELD_H,MinecraftCompat.literal(""));
        //#else
        //$$ EditBox field=new EditBox(font,x,y,w,FIELD_H,"");
        //#endif
        field.setMaxLength(256);
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
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){drawBase(graphics);super.extractRenderState(graphics,syncOptionHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        //#if MC >= 12005
        super.renderBackground(graphics,mouseX,mouseY,partialTick);
        //#else
        //$$ renderTransparentBackground(graphics);
        //#endif
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){drawBase(graphics);super.render(graphics,syncOptionHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);drawBase(graphics);super.render(graphics,syncOptionHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);drawBase(graphics);super.render(graphics,syncOptionHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(graphics,mouseX,mouseY);}
    //#else
    //$$ @Override public void render(int mouseX,int mouseY,float partialTick){renderBackground();drawBase(null);super.render(syncOptionHover(mouseX,mouseY),mouseY,partialTick);drawOverlay(null,mouseX,mouseY);}
    //#endif

    /** Resolve native Button state before vanilla rendering; unavailable options stay inert. */
    /** Native hover is shown only for options that are not already selected. */
    private int syncOptionHover(int mouseX,int mouseY){
        int x=left(),y=top(),w=contentWidth(),half=(w-GAP)/2,third=(w-2*GAP)/3;
        boolean overSelected=false;
        boolean serverHover=clicked(mouseX,mouseY,x,y+24,half,BUTTON_H);
        boolean globalHover=clicked(mouseX,mouseY,x+half+GAP,y+24,w-half-GAP,BUTTON_H);
        CameraOptionStyle.update(scopeButtons[0],!global,hasServer(),serverHover);
        CameraOptionStyle.update(scopeButtons[1],global,true,globalHover);
        overSelected=(!global&&serverHover)||(global&&globalHover);
        for(int i=0;i<categoryButtons.length;i++){
            int bx=x+i*(third+GAP),bw=i==2?w-2*(third+GAP):third;
            boolean hover=clicked(mouseX,mouseY,bx,y+46,bw,BUTTON_H);
            CameraOptionStyle.update(categoryButtons[i],tab.ordinal()==i,true,hover);
            overSelected|=tab.ordinal()==i&&hover;
        }
        if(tab==Tab.CAMERA){
            boolean toggleHover=clicked(mouseX,mouseY,x,y+68,half,BUTTON_H);
            boolean separateHover=clicked(mouseX,mouseY,x+half+GAP,y+68,w-half-GAP,BUTTON_H);
            boolean toggle=selected().mode==CameraCommandProfile.Mode.TOGGLE;
            CameraOptionStyle.update(modeButtons[0],toggle,true,toggleHover);
            CameraOptionStyle.update(modeButtons[1],!toggle,true,separateHover);
            overSelected|=(toggle&&toggleHover)||(!toggle&&separateHover);
        }
        return CameraOptionStyle.renderMouseX(mouseX,overSelected);
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
        int top=top();
        CameraBackground.draw(graphics,width,height,top-8,top+CameraBackground.NAV_HEIGHT+8);
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
        int x=left(),y=top(),w=contentWidth();
        centered(graphics,tr("dds-tweak.camera.command.title"),width/2,y,0xFFFFFFFF);
        CameraBackground.drawNavigationRules(graphics,width,y);
        if(tab!=Tab.CAMERA)
            centered(graphics,tr("dds-tweak.camera.command.hint"),width/2,y+72,0xFFCAD0D4);
        CameraCommandProfile.Action[] visible=actions();
        for(int i=0;i<visible.length;i++){
            String label=tr("dds-tweak.camera.command.short."+visible[i].key);
            int max=98;
            while(font.width(label)>max&&label.length()>1)label=label.substring(0,label.length()-1);
            ScreenRenderCompat.text(graphics,font,label,x+2,y+96+i*24,0xFFFFFFFF,true);
        }
        for(int i=0;i<visible.length;i++)
            if(!modeSelected&&fields[i].visible&&fields[i].isFocused())previewIndex=i;
        CameraCommandProfile.Action action=visible[Math.max(0,Math.min(previewIndex,visible.length-1))];
        String error=CameraCommandProfile.validate(action,selected().get(action));
        String preview=message;
        if(preview.isEmpty()){
            if(error!=null)preview=localizedError(error);
            else if(selected().get(action).isEmpty())preview=tr("dds-tweak.camera.command.disabled");
            else {
                String result=selected().render(action,"Steve","minecraft:overworld","10","64","-5");
                preview=tr("dds-tweak.camera.command.example")+": "+(result==null?"":CameraCommandSyntax.display(result));
            }
        }
        if(!preview.isEmpty()){
            while(font.width(preview)>w&&preview.length()>4)preview=preview.substring(0,preview.length()-4)+"...";
            centered(graphics,preview,width/2,y+158,(error!=null||!message.isEmpty())?0xFFFF8080:0xFFD1D8DE);
        }
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
        ScreenRenderCompat.text(graphics,font,value,x-font.width(value)/2,y,color,true);
    }
    private static String tr(String key){return MinecraftCompat.translatable(key).getString();}
}
package ddstweak.feature.cameraplayerselector;

import ddstweak.feature.dropconfirmation.DropConfirmationItemScreen;
import ddstweak.feature.dropconfirmation.ItemRegistryCompat;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Locale;
import java.util.function.Consumer;

final class CameraDestinationEditorScreen extends Screen {
    private static final int FIELD_H=18,BUTTON_H=20,GAP=5,ICON_SLOT=24,DIM_SLOT=24,DIM_GAP=28,DELETE_ICON=14,FOOTER_WIDTH=72;
    private static final String OVERWORLD="minecraft:overworld",NETHER="minecraft:the_nether",END="minecraft:the_end";
    private static final String[] DIMENSIONS={OVERWORLD,NETHER,END};
    private static final ItemStack[] DIMENSION_ICONS={
            new ItemStack(Items.GRASS_BLOCK),
            new ItemStack(Items.NETHERRACK),
            new ItemStack(Items.END_PORTAL_FRAME)
    };

    private final CameraPlayerSelectorScreen parent;
    private final String existingId;
    private String draftName,draftX,draftY,draftZ,selectedDimension;
    private String iconId;
    private ItemStack iconStack;
    private EditBox nameField,xField,yField,zField;
    private Button iconButton;
    private String status="";
    private boolean statusError;

    CameraDestinationEditorScreen(CameraPlayerSelectorScreen parent,CameraDestination destination){
        super(MinecraftCompat.translatable(destination==null?"dds-tweak.camera.destination.editor.new":"dds-tweak.camera.destination.editor.edit"));
        this.parent=parent;
        this.existingId=destination==null?null:destination.id();
        this.draftName=destination==null?"":destination.name();
        this.draftX=destination==null?"":CameraSelectorCompat.formatNumber(destination.x());
        this.draftY=destination==null?"":CameraSelectorCompat.formatNumber(destination.y());
        this.draftZ=destination==null?"":CameraSelectorCompat.formatNumber(destination.z());
        this.selectedDimension=destination==null?"":destination.dimension();
        setIcon(destination==null?CameraDestination.DEFAULT_ICON_ID:destination.iconId());
    }

    @Override public boolean isPauseScreen(){return false;}

    @Override protected void init(){
        if(selectedDimension==null||selectedDimension.trim().isEmpty()){
            String current=CameraSelectorCompat.currentDimension(minecraft);
            selectedDimension=isVanillaDimension(current)?current:OVERWORLD;
        }

        Layout l=layout();
        nameField=editBox(l.nameFieldX,l.nameFieldY,l.nameFieldWidth,draftName,64,value->{draftName=value;clearStatus();});
        xField=editBox(l.innerLeft,l.coordY,l.coordW,draftX,32,value->{draftX=value;clearStatus();});
        yField=editBox(l.innerLeft+l.coordW+GAP,l.coordY,l.coordW,draftY,32,value->{draftY=value;clearStatus();});
        zField=editBox(l.innerLeft+(l.coordW+GAP)*2,l.coordY,l.coordW,draftZ,32,value->{draftZ=value;clearStatus();});
        addWidgetCompat(nameField);addWidgetCompat(xField);addWidgetCompat(yField);addWidgetCompat(zField);
        iconButton=button(l.iconX,l.nameFieldY,ICON_SLOT,"",this::openIconPicker);
        addWidgetCompat(iconButton);

        addWidgetCompat(button(l.coordButtonX,l.coordButtonY,l.buttonWidth,tr("dds-tweak.camera.destination.use_current_position"),this::useCurrentPosition));

        int footerGap=4;
        int saveX=l.innerLeft+l.innerWidth-FOOTER_WIDTH;
        int cancelX=saveX-footerGap-FOOTER_WIDTH;
        addWidgetCompat(button(cancelX,l.bottomY,FOOTER_WIDTH,tr("dds-tweak.camera.destination.cancel"),this::onClose));
        addWidgetCompat(button(saveX,l.bottomY,FOOTER_WIDTH,tr("dds-tweak.camera.destination.save"),this::saveDestination));
    }

    private EditBox editBox(int x,int y,int w,String value,int maxLength,Consumer<String> responder){
        //#if MC >= 11600
        EditBox field=new EditBox(font,x,y,w,FIELD_H,MinecraftCompat.literal(""));
        //#else
        //$$ EditBox field=new EditBox(font,x,y,w,FIELD_H,"");
        //#endif
        field.setMaxLength(maxLength);
        field.setValue(value==null?"":value);
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

    private <T extends net.minecraft.client.gui.components.AbstractWidget> void addWidgetCompat(T widget){
        //#if MC >= 11700
        addRenderableWidget(widget);
        //#else
        //$$ addButton(widget);
        //#endif
    }

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){extractTransparentBackground(graphics);}
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){renderBase(graphics);super.extractRenderState(graphics,mouseX,mouseY,partialTick);renderOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderTransparentBackground(graphics);}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBase(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);renderBase(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);renderBase(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderOverlay(graphics,mouseX,mouseY);}
    //#else
    //$$ @Override public void render(int mouseX,int mouseY,float partialTick){renderBackground();renderBase(null);super.render(mouseX,mouseY,partialTick);renderOverlay(null,mouseX,mouseY);}
    //#endif

    private void renderBase(
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
        Layout l=layout();
        ScreenRenderCompat.fill(graphics,l.left,l.top,l.left+l.width,l.top+l.height,0xEE101010);
        outline(graphics,l.left,l.top,l.width,l.height,0xFF707070);
    }

    private void renderOverlay(
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
        Layout l=layout();
        centered(graphics,title.getString(),l.left+l.width/2,l.top+8,0xFFFFFFFF);

        text(graphics,tr("dds-tweak.camera.destination.field.name"),l.innerLeft,l.nameRowY-11,0xFFD0D0D0);
        int iconInsetX=(ICON_SLOT-16)/2;
        int iconInsetY=(BUTTON_H-16)/2;
        if(!iconStack.isEmpty())ScreenRenderCompat.item(graphics,iconStack,l.iconX+iconInsetX,l.nameFieldY+iconInsetY);

        text(graphics,"X",l.innerLeft,l.coordY-11,0xFFD0D0D0);
        text(graphics,"Y",l.innerLeft+l.coordW+GAP,l.coordY-11,0xFFD0D0D0);
        text(graphics,"Z",l.innerLeft+(l.coordW+GAP)*2,l.coordY-11,0xFFD0D0D0);

        text(graphics,tr("dds-tweak.camera.destination.field.dimension"),l.innerLeft,l.dimensionY-11,0xFFD0D0D0);
        int start=dimensionStart(l);
        for(int i=0;i<DIMENSIONS.length;i++){
            int x=start+i*(DIM_SLOT+DIM_GAP);
            boolean selected=DIMENSIONS[i].equals(selectedDimension);
            boolean hover=inside(mouseX,mouseY,x,l.dimensionY,DIM_SLOT,DIM_SLOT);
            if(selected){
                ScreenRenderCompat.fill(graphics,x+1,l.dimensionY+1,x+DIM_SLOT-1,l.dimensionY+DIM_SLOT-1,0x30000000);
                outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT,0xFFFFFFFF);
            }else if(hover){
                ScreenRenderCompat.fill(graphics,x+1,l.dimensionY+1,x+DIM_SLOT-1,l.dimensionY+DIM_SLOT-1,0x50505050);
                outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT,0xFFAAAAAA);
            }
            ScreenRenderCompat.item(graphics,DIMENSION_ICONS[i],x+(DIM_SLOT-16)/2,l.dimensionY+(DIM_SLOT-16)/2);
        }

        if(existingId!=null){
            int deleteX=deleteX(l),deleteY=deleteY(l);
            boolean shift=CameraSelectorCompat.shiftDown(minecraft);
            boolean hover=inside(mouseX,mouseY,deleteX,deleteY,DELETE_ICON,DELETE_ICON);
            ScreenRenderCompat.crossButton(graphics,deleteX,deleteY,shift&&hover,shift);
            if(hover&&status.isEmpty()){
                String hint=tr(shift?"dds-tweak.camera.destination.delete_ready":"dds-tweak.camera.destination.delete_shift");
                int hintY=deleteY+(DELETE_ICON-font.lineHeight)/2;
                text(graphics,hint,deleteX+DELETE_ICON+6,hintY,shift?0xFFFF8080:0xFFB8B8B8);
            }
        }

        if(!status.isEmpty())centered(graphics,status,l.left+l.width/2,l.statusY,statusError?0xFFFF7070:0xFFB8D8B8);
    }

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(super.mouseClicked(event,doubleClick))return true;
        return handleClick(event.x(),event.y(),event.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){
    //$$     if(super.mouseClicked(x,y,button))return true;
    //$$     return handleClick(x,y,button);
    //$$ }
    //#endif

    private boolean handleClick(double mouseX,double mouseY,int button){
        if(!CameraSelectorCompat.leftMouse(button))return false;
        Layout l=layout();

        if(existingId!=null&&inside(mouseX,mouseY,deleteX(l),deleteY(l),DELETE_ICON,DELETE_ICON)){
            if(CameraSelectorCompat.shiftDown(minecraft))deleteDestination();
            return true;
        }

        int start=dimensionStart(l);
        for(int i=0;i<DIMENSIONS.length;i++){
            int x=start+i*(DIM_SLOT+DIM_GAP);
            if(inside(mouseX,mouseY,x,l.dimensionY,DIM_SLOT,DIM_SLOT)){
                selectedDimension=DIMENSIONS[i];
                clearStatus();
                return true;
            }
        }
        return false;
    }

    //#if MC >= 12109
    @Override public boolean keyPressed(KeyEvent event){
        if(event.isEscape()){onClose();return true;}
        if(super.keyPressed(event))return true;
        if(event.key()==257){saveDestination();return true;}
        return false;
    }
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
    //$$     if(keyCode==256){onClose();return true;}
    //$$     if(super.keyPressed(keyCode,scanCode,modifiers))return true;
    //$$     if(keyCode==257){saveDestination();return true;}
    //$$     return false;
    //$$ }
    //#endif

    @Override public void onClose(){
        MinecraftCompat.setScreen(minecraft,parent);
    }

    private void useCurrentPosition(){
        if(minecraft==null||minecraft.player==null){error("dds-tweak.camera.destination.error.position");return;}
        draftX=CameraSelectorCompat.formatCapturedCoordinate(CameraSelectorCompat.playerX(minecraft));
        draftY=CameraSelectorCompat.formatCapturedCoordinate(CameraSelectorCompat.playerY(minecraft));
        draftZ=CameraSelectorCompat.formatCapturedCoordinate(CameraSelectorCompat.playerZ(minecraft));
        if(xField!=null)xField.setValue(draftX);
        if(yField!=null)yField.setValue(draftY);
        if(zField!=null)zField.setValue(draftZ);
        clearStatus();
    }

    private void openIconPicker(){
        MinecraftCompat.setScreen(minecraft,new DropConfirmationItemScreen(
                this,iconId,this::setIcon,"dds-tweak.camera.destination.icon_picker"
        ));
    }

    private void saveDestination(){
        String name=draftName==null?"":draftName.trim();
        if(name.isEmpty()){error("dds-tweak.camera.destination.error.name");return;}
        double x,y,z;
        try{
            x=Double.parseDouble(draftX.trim());
            y=Double.parseDouble(draftY.trim());
            z=Double.parseDouble(draftZ.trim());
        }catch(RuntimeException exception){
            error("dds-tweak.camera.destination.error.coordinates");
            return;
        }
        String dimension=selectedDimension==null?"":selectedDimension.trim().toLowerCase(Locale.ROOT);
        CameraDestination destination=new CameraDestination(existingId,name,dimension,x,y,z,iconId);
        if(!CameraDestination.validResourceId(dimension)){error("dds-tweak.camera.destination.error.dimension");return;}
        if(!destination.isValid()){error("dds-tweak.camera.destination.error.coordinates");return;}
        CameraDestinationStore.upsert(destination);
        parent.reloadDestinations();
        MinecraftCompat.setScreen(minecraft,parent);
    }

    private void deleteDestination(){
        CameraDestinationStore.delete(existingId);
        parent.reloadDestinations();
        MinecraftCompat.setScreen(minecraft,parent);
    }

    private void setIcon(String id){
        iconId=id==null||id.trim().isEmpty()?CameraDestination.DEFAULT_ICON_ID:id.trim().toLowerCase(Locale.ROOT);
        iconStack=findIcon(iconId);
    }

    private ItemStack findIcon(String id){
        for(Item item:ItemRegistryCompat.allItems())if(ItemRegistryCompat.id(item).equals(id))return ItemRegistryCompat.stack(item);
        return new ItemStack(Items.COMPASS);
    }

    private int deleteX(Layout l){return l.innerLeft;}
    private int deleteY(Layout l){return l.bottomY+(BUTTON_H-DELETE_ICON)/2;}

    private static boolean isVanillaDimension(String dimension){
        return OVERWORLD.equals(dimension)||NETHER.equals(dimension)||END.equals(dimension);
    }

    private int dimensionStart(Layout l){
        int rowWidth=DIM_SLOT*DIMENSIONS.length+DIM_GAP*(DIMENSIONS.length-1);
        return l.left+(l.width-rowWidth)/2;
    }

    private void clearStatus(){status="";statusError=false;}

    private void error(String key){
        status=tr(key);statusError=true;
    }

    private Layout layout(){
        int panelWidth=Math.min(380,Math.max(250,width-24));
        int panelHeight=Math.min(198,Math.max(184,height-16));
        int left=(width-panelWidth)/2,top=(height-panelHeight)/2;
        int innerLeft=left+10,innerWidth=panelWidth-20;
        int nameRowY=top+38,iconX=innerLeft,nameFieldX=iconX+ICON_SLOT+GAP,nameFieldY=nameRowY+2,nameFieldWidth=Math.max(80,innerWidth-ICON_SLOT-GAP);
        int coordY=nameRowY+43,buttonWidth=92;
        int coordButtonX=innerLeft+innerWidth-buttonWidth,coordButtonY=coordY-1;
        int coordAreaWidth=Math.max(126,coordButtonX-innerLeft-GAP);
        int coordW=Math.max(38,(coordAreaWidth-2*GAP)/3);
        int dimensionY=coordY+43,statusY=dimensionY+31,bottomY=top+panelHeight-25;
        return new Layout(left,top,panelWidth,panelHeight,innerLeft,innerWidth,nameRowY,iconX,nameFieldX,nameFieldY,nameFieldWidth,coordY,coordW,coordButtonX,coordButtonY,buttonWidth,dimensionY,statusY,bottomY);
    }

    private void outline(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x,int y,int w,int h,int color){
        ScreenRenderCompat.fill(graphics,x,y,x+w,y+1,color);ScreenRenderCompat.fill(graphics,x,y+h-1,x+w,y+h,color);
        ScreenRenderCompat.fill(graphics,x,y,x+1,y+h,color);ScreenRenderCompat.fill(graphics,x+w-1,y,x+w,y+h,color);
    }

    private void text(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            String value,int x,int y,int color){ScreenRenderCompat.text(graphics,font,value,x,y,color,true);}

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
            String value,int centerX,int y,int color){text(graphics,value,centerX-font.width(value)/2,y,color);}

    private static boolean inside(double x,double y,int left,int top,int width,int height){return x>=left&&x<left+width&&y>=top&&y<top+height;}
    private static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}

    private static final class Layout {
        final int left,top,width,height,innerLeft,innerWidth,nameRowY,iconX,nameFieldX,nameFieldY,nameFieldWidth,coordY,coordW,coordButtonX,coordButtonY,buttonWidth,dimensionY,statusY,bottomY;
        Layout(int left,int top,int width,int height,int innerLeft,int innerWidth,int nameRowY,int iconX,int nameFieldX,int nameFieldY,int nameFieldWidth,int coordY,int coordW,int coordButtonX,int coordButtonY,int buttonWidth,int dimensionY,int statusY,int bottomY){
            this.left=left;this.top=top;this.width=width;this.height=height;this.innerLeft=innerLeft;this.innerWidth=innerWidth;
            this.nameRowY=nameRowY;this.iconX=iconX;this.nameFieldX=nameFieldX;this.nameFieldY=nameFieldY;this.nameFieldWidth=nameFieldWidth;
            this.coordY=coordY;this.coordW=coordW;this.coordButtonX=coordButtonX;this.coordButtonY=coordButtonY;this.buttonWidth=buttonWidth;
            this.dimensionY=dimensionY;this.statusY=statusY;this.bottomY=bottomY;
        }
    }
}

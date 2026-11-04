package ddstweak.feature.cameraplayerselector;

import ddstweak.feature.dropconfirmation.ItemRegistryCompat;
import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
//#if MC >= 12109
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
//#endif
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

public final class CameraPlayerSelectorScreen extends Screen {
    private static final int SLOT=22,ICON=16,GAP=3,PEEK=6,ROW_H=25,MAX_VISIBLE=5;
    // Anchors calibrated from the 4K reference using Minecraft GUI units, not screenshot pixels.
    private static final int DIMENSION_Y=27,FAKE_Y=70,REAL_Y=116,SOLO_PLAYER_Y=93;
    private static final int DESTINATION_Y=163,ACTION_Y=200;
    private static final ItemStack OVERWORLD=new ItemStack(Items.GRASS_BLOCK),NETHER=new ItemStack(Items.NETHERRACK),END=new ItemStack(Items.END_PORTAL_FRAME);
    private static final ItemStack CAMERA=new ItemStack(Items.ENDER_EYE),BACK=new ItemStack(Items.COMPASS),SETTINGS=new ItemStack(Items.WRITABLE_BOOK),DEFAULT_DESTINATION_ICON=new ItemStack(Items.COMPASS);

    private final List<CameraPlayerEntry> real=new ArrayList<>(),fake=new ArrayList<>(),unknown=new ArrayList<>(),realRow=new ArrayList<>();
    private final List<CameraDestination> destinations=new ArrayList<>();
    private final Map<String,ItemStack> destinationIcons=new HashMap<>();
    private CameraCommandProfile profile;
    private final CameraQuickCommandOverlay quickOverlay=new CameraQuickCommandOverlay(this);
    void reloadProfile(){profile=CameraCommandProfileStore.load(minecraft);}
    void reloadQuickCommands(){quickOverlay.reload(minecraft);}
    private int realOffset,fakeOffset,destinationOffset;

    CameraPlayerSelectorScreen(List<CameraPlayerEntry> snapshot,CameraCommandProfile profile){
        super(MinecraftCompat.translatable("dds-tweak.camera.title"));
        this.profile=profile;
        for(CameraPlayerEntry entry:snapshot){
            if(entry.type==PlayerInventoryTargetType.REAL)real.add(entry);
            else if(entry.type==PlayerInventoryTargetType.FAKE)fake.add(entry);
            else unknown.add(entry);
        }
        realRow.addAll(real);
        realRow.addAll(unknown);
        reloadDestinations();
        quickOverlay.reload(net.minecraft.client.Minecraft.getInstance());
    }

    @Override public boolean isPauseScreen(){return false;}

    void reloadDestinations(){
        destinations.clear();
        destinations.addAll(CameraDestinationStore.load());
        destinationIcons.clear();
        Set<String> needed=new HashSet<>();
        for(CameraDestination destination:destinations)needed.add(destination.iconId());
        Map<String,ItemStack> found=new HashMap<>();
        if(!needed.isEmpty()){
            for(Item item:ItemRegistryCompat.allItems()){
                String id=ItemRegistryCompat.id(item);
                if(needed.contains(id))found.put(id,ItemRegistryCompat.stack(item));
            }
        }
        for(CameraDestination destination:destinations){
            ItemStack stack=found.get(destination.iconId());
            destinationIcons.put(destination.id(),stack==null||stack.isEmpty()?DEFAULT_DESTINATION_ICON:stack);
        }
        destinationOffset=clampOffset(destinationOffset,destinations.size(),Math.max(0,layout().capacity-1));
    }

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){super.extractBackground(graphics,mouseX,mouseY,partialTick);}
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){super.extractRenderState(graphics,mouseX,mouseY,partialTick);renderContent(graphics,mouseX,mouseY);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        //#if MC >= 12005
        super.renderBackground(graphics,mouseX,mouseY,partialTick);
        //#else
        //$$ renderTransparentBackground(graphics);
        //#endif
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){super.render(graphics,mouseX,mouseY,partialTick);renderContent(graphics,mouseX,mouseY);}
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderContent(graphics,mouseX,mouseY);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderContent(graphics,mouseX,mouseY);}
    //#else
    //$$ @Override public void render(int mouseX,int mouseY,float partialTick){renderBackground();super.render(mouseX,mouseY,partialTick);renderContent(null,mouseX,mouseY);}
    //#endif

    private void renderContent(
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
        CameraBackground.draw(graphics,width,height,l.top-8,l.top+CameraBackground.NAV_HEIGHT+8);
        centered(graphics,tr("dds-tweak.camera.title"),width/2,l.top,0xFFFFFFFF);
        CameraBackground.drawNavigationRules(graphics,width,l.top);
        int panelLeft=(width-Math.min(360,width-16))/2;
        groupLabel(graphics,"dds-tweak.camera.group.dimension",panelLeft,l.dimensionY);
        if(!fake.isEmpty())groupLabel(graphics,"dds-tweak.camera.group.fake",panelLeft,l.fakeY);
        if(!realRow.isEmpty())groupLabel(graphics,"dds-tweak.camera.group.real",panelLeft,l.realY);
        groupLabel(graphics,"dds-tweak.camera.group.destination",panelLeft,l.destinationY);

        renderFixedItems(graphics,mouseX,mouseY,l,l.dimensionY,new ItemStack[]{OVERWORLD,NETHER,END});
        renderPlayers(graphics,mouseX,mouseY,l,fake,fakeOffset,l.fakeY);
        renderPlayers(graphics,mouseX,mouseY,l,realRow,realOffset,l.realY);
        renderDestinations(graphics,mouseX,mouseY,l);
        renderFixedItems(graphics,mouseX,mouseY,l,l.actionY,
                new ItemStack[]{CAMERA,BACK,SETTINGS});

        quickOverlay.render(graphics,font,width,l.actionY,mouseX,mouseY);
        List<Component> quickTip=quickOverlay.tooltip(width,l.actionY,mouseX,mouseY);
        renderTooltip(graphics,mouseX,mouseY,quickTip!=null?quickTip:tooltip(mouseX,mouseY,l));
    }

    private void groupLabel(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            String key,int x,int y){
        text(graphics,tr(key),x,y+(SLOT-font.lineHeight)/2,0xFFADB3B8);
    }

    private void renderFixedItems(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY,Layout l,int y,ItemStack[] items){
        int start=centeredStart(l,items.length);
        for(int i=0;i<items.length;i++){
            int x=start+i*(SLOT+GAP);
            boolean hover=inside(mouseX,mouseY,x,y,SLOT,SLOT);
            renderItemSlot(graphics,items[i],x,y,hover);
        }
    }

    private void renderPlayers(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY,Layout l,List<CameraPlayerEntry> list,int offset,int y){
        int startIndex=clampOffset(offset,list.size(),l.capacity);
        int count=Math.min(l.capacity,Math.max(0,list.size()-startIndex));
        int start=centeredStart(l,count);
        int hoveredX=-1;
        for(int local=0;local<count;local++){
            CameraPlayerEntry entry=list.get(startIndex+local);
            int x=start+local*(SLOT+GAP);
            boolean hover=inside(mouseX,mouseY,x,y,SLOT,SLOT);
            boolean online=CameraSelectorCompat.online(minecraft,entry.uuid);
            if(hover)CameraSlotRender.fill(graphics,x,y,SLOT,SLOT,0x38000000);
            PlayerInfo info=online?CameraSelectorCompat.info(minecraft,entry.uuid):null;
            int hx=x+(SLOT-ICON)/2,hy=y+(SLOT-ICON)/2;
            if(info!=null)ScreenRenderCompat.playerHead(graphics,info,hx,hy,ICON);
            else{
                ScreenRenderCompat.fill(graphics,hx,hy,hx+ICON,hy+ICON,0xFF55585D);
                String initial=entry.name.isEmpty()?"?":entry.name.substring(0,1).toUpperCase(Locale.ROOT);
                centered(graphics,initial,hx+ICON/2,hy+4,0xFFE6E6E6);
            }
            if(entry.type==PlayerInventoryTargetType.UNKNOWN){
                ScreenRenderCompat.fill(graphics,x+SLOT-8,y+1,x+SLOT-1,y+9,0xCC202020);
                centered(graphics,"?",x+SLOT-4,y+1,0xFFFFD45A);
            }
            if(hover)hoveredX=x;
        }
        renderEdgePeek(graphics,list,startIndex,start,y,count);
        if(hoveredX>=0)CameraSlotRender.outline(graphics,hoveredX,y,SLOT,SLOT,0xFFFFFFFF);
    }

    private void renderDestinations(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY,Layout l){
        int visibleCapacity=Math.max(0,l.capacity-1);
        int startIndex=clampOffset(destinationOffset,destinations.size(),visibleCapacity);
        int count=Math.min(visibleCapacity,Math.max(0,destinations.size()-startIndex));
        int start=centeredStart(l,count+1);
        for(int local=0;local<count;local++){
            CameraDestination destination=destinations.get(startIndex+local);
            int x=start+local*(SLOT+GAP);
            boolean hover=inside(mouseX,mouseY,x,l.destinationY,SLOT,SLOT);
            ItemStack stack=destinationIcons.get(destination.id());
            renderItemSlot(graphics,stack==null?DEFAULT_DESTINATION_ICON:stack,x,l.destinationY,hover);
        }
        int addX=start+count*(SLOT+GAP);
        boolean addHover=inside(mouseX,mouseY,addX,l.destinationY,SLOT,SLOT);
        CameraSlotRender.fill(graphics,addX,l.destinationY,SLOT,SLOT,addHover?0x65000000:0x22000000);
        CameraSlotRender.outline(graphics,addX,l.destinationY,SLOT,SLOT,addHover?0xE8FFFFFF:0x547E858B);
        int plusX=addX+SLOT/2,plusY=l.destinationY+SLOT/2;
        int plusColor=addHover?0xFFFFFFFF:0xFFE8E8E8;
        ScreenRenderCompat.fill(graphics,plusX-4,plusY-1,plusX+4,plusY+1,plusColor);
        ScreenRenderCompat.fill(graphics,plusX-1,plusY-4,plusX+1,plusY+4,plusColor);
    }

    private void renderEdgePeek(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            List<CameraPlayerEntry> list,int startIndex,int start,int y,int count){
        if(count<=0)return;
        int headY=y+(SLOT-ICON)/2;
        if(startIndex>0){
            PlayerInfo info=CameraSelectorCompat.info(minecraft,list.get(startIndex-1).uuid);
            if(info!=null){
                int clipRight=start-GAP,clipLeft=clipRight-PEEK;
                if(ScreenRenderCompat.beginScissor(graphics,clipLeft,y,clipRight,y+SLOT)){
                    ScreenRenderCompat.playerHead(graphics,info,clipRight-ICON,headY,ICON);
                    ScreenRenderCompat.endScissor(graphics);
                }
            }
        }
        if(startIndex+count<list.size()){
            PlayerInfo info=CameraSelectorCompat.info(minecraft,list.get(startIndex+count).uuid);
            if(info!=null){
                int rowRight=start+rowWidth(count),clipLeft=rowRight+GAP,clipRight=clipLeft+PEEK;
                if(ScreenRenderCompat.beginScissor(graphics,clipLeft,y,clipRight,y+SLOT)){
                    ScreenRenderCompat.playerHead(graphics,info,clipLeft,headY,ICON);
                    ScreenRenderCompat.endScissor(graphics);
                }
            }
        }
    }

    private void renderItemSlot(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            ItemStack stack,int x,int y,boolean hover){
        CameraSlotRender.fill(graphics,x,y,SLOT,SLOT,hover?0x58000000:0x22000000);
        CameraSlotRender.outline(graphics,x,y,SLOT,SLOT,hover?0xE8E6EBF0:0x547E858B);
        if(stack!=null&&!stack.isEmpty())ScreenRenderCompat.item(graphics,stack,x+(SLOT-16)/2,y+(SLOT-16)/2);
    }

    /** Use the same vanilla tooltip pipeline as fake-player inventory action buttons. */
    private void renderTooltip(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY,List<Component> lines){
        if(lines==null||lines.isEmpty())return;
        //#if MC >= 260100
        //$$ graphics.setTooltipForNextFrame(font,lines,Optional.empty(),mouseX,mouseY);
        //#elseif MC >= 12000
        graphics.setTooltipForNextFrame(font,lines,Optional.empty(),mouseX,mouseY);
        //#elseif MC >= 11700
        //$$ renderTooltip(graphics,lines,Optional.empty(),mouseX,mouseY);
        //#elseif MC >= 11600
        //$$ renderComponentTooltip(graphics,lines,mouseX,mouseY);
        //#else
        //$$ List<String> legacy=new ArrayList<>();for(Component line:lines)legacy.add(line.getString());renderTooltip(legacy,mouseX,mouseY);
        //#endif
    }

    //#if MC >= 12109
    @Override public boolean keyPressed(KeyEvent event){
        if(event.isEscape()&&quickOverlay.isOpen()){quickOverlay.close();return true;}
        return super.keyPressed(event);
    }
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
    //$$     if(keyCode==256&&quickOverlay.isOpen()){quickOverlay.close();return true;}
    //$$     return super.keyPressed(keyCode,scanCode,modifiers);
    //$$ }
    //#endif

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){if(handleClick(event.x(),event.y(),event.button()))return true;return super.mouseClicked(event,doubleClick);}
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){if(handleClick(x,y,button))return true;return super.mouseClicked(x,y,button);}
    //#endif

    private boolean handleClick(double mouseX,double mouseY,int button){
        Layout l=layout();
        boolean left=CameraSelectorCompat.leftMouse(button),right=CameraSelectorCompat.rightMouse(button);
        if(quickOverlay.handleClick(minecraft,width,l.actionY,mouseX,mouseY,button))return true;

        int dimension=fixedIndex(mouseX,mouseY,l,l.dimensionY,3);
        if(dimension>=0){
            if(left){
                String id=dimension==0?"minecraft:overworld":dimension==1?"minecraft:the_nether":"minecraft:the_end";
                if(CameraSelectorCompat.sendDimension(minecraft,profile,id))CameraPlayerSelectorFeature.finishAction(this);
            }
            return left||right;
        }

        CameraPlayerEntry entry=playerAt(mouseX,mouseY,l,fake,fakeOffset,l.fakeY);
        if(entry==null)entry=playerAt(mouseX,mouseY,l,realRow,realOffset,l.realY);
        if(entry!=null){
            if(!CameraSelectorCompat.matchesSnapshot(minecraft,entry))return left||right;
            CameraCommandProfile.Action input=left
                    ?(CameraSelectorCompat.shiftDown(minecraft)?CameraCommandProfile.Action.PLAYER_DIRECT_TP:CameraCommandProfile.Action.PLAYER_TP)
                    :CameraCommandProfile.Action.PLAYER_FOLLOW;
            boolean sent=(left||right)&&CameraSelectorCompat.sendPlayer(minecraft,profile,input,entry.name);
            if(sent)CameraPlayerSelectorFeature.finishAction(this);
            return left||right;
        }

        CameraDestination destination=destinationAt(mouseX,mouseY,l);
        if(destination!=null){
            if(left&&CameraSelectorCompat.sendDestination(minecraft,profile,destination))CameraPlayerSelectorFeature.finishAction(this);
            else if(right)MinecraftCompat.setScreen(minecraft,new CameraDestinationEditorScreen(this,destination));
            return left||right;
        }
        if(addDestinationAt(mouseX,mouseY,l)){
            if(left)MinecraftCompat.setScreen(minecraft,new CameraDestinationEditorScreen(this,null));
            return left||right;
        }

        int action=fixedIndex(mouseX,mouseY,l,l.actionY,3);
        if(action==2){
            if(left)MinecraftCompat.setScreen(minecraft,new CameraCommandConfigScreen(this));
            return left||right;
        }
        if(action==0){
            CameraCommandProfile.Action camera=profile.mode==CameraCommandProfile.Mode.TOGGLE
                    ?CameraCommandProfile.Action.TOGGLE:(left?CameraCommandProfile.Action.ENTER:CameraCommandProfile.Action.EXIT);
            if((left||right)&&(left||profile.mode==CameraCommandProfile.Mode.SEPARATE)
                    &&CameraSelectorCompat.sendAction(minecraft,profile,camera))CameraPlayerSelectorFeature.finishAction(this);
            return left||right;
        }
        if(action==1){
            if(left&&CameraSelectorCompat.sendAction(minecraft,profile,CameraCommandProfile.Action.BACK))
                CameraPlayerSelectorFeature.finishAction(this);
            return left||right;
        }
        return false;
    }

    //#if MC >= 12002
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){if(scroll(mouseX,mouseY,vertical))return true;return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);}
    //#else
    //$$ @Override public boolean mouseScrolled(double mouseX,double mouseY,double vertical){if(scroll(mouseX,mouseY,vertical))return true;return super.mouseScrolled(mouseX,mouseY,vertical);}
    //#endif

    private boolean scroll(double mouseX,double mouseY,double amount){
        if(amount==0)return false;
        Layout l=layout();
        if(quickOverlay.scroll(width,l.actionY,mouseX,mouseY,amount))return true;
        if(!fake.isEmpty()&&inside(mouseX,mouseY,l.contentX,l.fakeY,l.contentW,SLOT)){
            fakeOffset=step(fakeOffset,fake.size(),l.capacity,amount);
            return true;
        }
        if(!realRow.isEmpty()&&inside(mouseX,mouseY,l.contentX,l.realY,l.contentW,SLOT)){
            realOffset=step(realOffset,realRow.size(),l.capacity,amount);
            return true;
        }
        if(inside(mouseX,mouseY,l.contentX,l.destinationY,l.contentW,SLOT)){
            destinationOffset=step(destinationOffset,destinations.size(),Math.max(0,l.capacity-1),amount);
            return true;
        }
        return false;
    }

    private List<Component> tooltip(double mouseX,double mouseY,Layout l){
        int dimension=fixedIndex(mouseX,mouseY,l,l.dimensionY,3);
        if(dimension>=0){
            String key=dimension==0?"dds-tweak.camera.dimension.overworld":dimension==1?"dds-tweak.camera.dimension.nether":"dds-tweak.camera.dimension.end";
            String id=dimension==0?"minecraft:overworld":dimension==1?"minecraft:the_nether":"minecraft:the_end";
            List<Component> lines=new ArrayList<>();
            lines.add(MinecraftCompat.translatable(key));
            appendActionTip(lines,CameraCommandProfile.Action.DIMENSION,
                    profile.renderDimension(CameraCommandProfile.Action.DIMENSION,id),"left");
            return lines;
        }
        CameraPlayerEntry entry=playerAt(mouseX,mouseY,l,fake,fakeOffset,l.fakeY);
        if(entry==null)entry=playerAt(mouseX,mouseY,l,realRow,realOffset,l.realY);
        if(entry!=null){
            List<Component> lines=new ArrayList<>();
            lines.add(MinecraftCompat.literal(entry.name));
            if(entry.type==PlayerInventoryTargetType.UNKNOWN)
                lines.add(MinecraftCompat.grayTranslatable("dds-tweak.camera.identity.unknown"));
            for(CameraCommandProfile.Action action:new CameraCommandProfile.Action[]{
                    CameraCommandProfile.Action.PLAYER_TP,CameraCommandProfile.Action.PLAYER_FOLLOW,
                    CameraCommandProfile.Action.PLAYER_DIRECT_TP}){
                String mouse=action==CameraCommandProfile.Action.PLAYER_TP?"left"
                        :action==CameraCommandProfile.Action.PLAYER_FOLLOW?"right":"shiftLeft";
                appendActionTip(lines,action,profile.renderPlayer(action,entry.name),mouse);
            }
            return lines;
        }
        CameraDestination destination=destinationAt(mouseX,mouseY,l);
        if(destination!=null){
            List<Component> lines=new ArrayList<>();
            lines.add(MinecraftCompat.literal(destination.name()));
            lines.add(MinecraftCompat.grayLiteral(destination.dimension()+" · "+CameraSelectorCompat.coordinateText(destination)));
            appendActionTip(lines,CameraCommandProfile.Action.DESTINATION,profile.renderDestination(destination),"left");
            lines.add(MinecraftCompat.grayTranslatable("dds-tweak.camera.destination.edit_help"));
            return lines;
        }
        if(addDestinationAt(mouseX,mouseY,l))
            return Collections.singletonList(MinecraftCompat.translatable("dds-tweak.camera.destination.add"));
        int action=fixedIndex(mouseX,mouseY,l,l.actionY,3);
        if(action==0){
            List<Component> lines=new ArrayList<>();
            lines.add(MinecraftCompat.translatable("dds-tweak.camera.title"));
            CameraCommandProfile.Action left=profile.mode==CameraCommandProfile.Mode.TOGGLE
                    ?CameraCommandProfile.Action.TOGGLE:CameraCommandProfile.Action.ENTER;
            appendActionTip(lines,left,profile.render(left),"left");
            if(profile.mode==CameraCommandProfile.Mode.SEPARATE)
                appendActionTip(lines,CameraCommandProfile.Action.EXIT,
                        profile.render(CameraCommandProfile.Action.EXIT),"right");
            return lines;
        }
        if(action==1){
            List<Component> lines=new ArrayList<>();
            lines.add(MinecraftCompat.translatable("dds-tweak.camera.action.back"));
            appendActionTip(lines,CameraCommandProfile.Action.BACK,
                    profile.render(CameraCommandProfile.Action.BACK),"left");
            return lines;
        }
        if(action==2)
            return Collections.singletonList(MinecraftCompat.translatable("dds-tweak.camera.command.settings"));
        return null;
    }

    private void appendActionTip(List<Component> lines,CameraCommandProfile.Action action,String command,String mouse){
        // Show every configured operation; server authorization is checked on execution.
        if(command==null)return;
        lines.add(MinecraftCompat.translatable("dds-tweak.camera.tooltip.gesture",
                MinecraftCompat.translatable("dds-tweak.camera.tooltip.mouse."+mouse),
                MinecraftCompat.translatable("dds-tweak.camera.command.short."+action.key)));
        lines.add(MinecraftCompat.grayLiteral(CameraCommandSyntax.display(command)));
    }

    private CameraPlayerEntry playerAt(double mouseX,double mouseY,Layout l,List<CameraPlayerEntry> list,int offset,int y){
        int startIndex=clampOffset(offset,list.size(),l.capacity);
        int count=Math.min(l.capacity,Math.max(0,list.size()-startIndex));
        if(count<=0)return null;
        int start=centeredStart(l,count);
        for(int local=0;local<count;local++)if(inside(mouseX,mouseY,start+local*(SLOT+GAP),y,SLOT,SLOT))return list.get(startIndex+local);
        return null;
    }

    private CameraDestination destinationAt(double mouseX,double mouseY,Layout l){
        int visibleCapacity=Math.max(0,l.capacity-1);
        int startIndex=clampOffset(destinationOffset,destinations.size(),visibleCapacity);
        int count=Math.min(visibleCapacity,Math.max(0,destinations.size()-startIndex));
        int start=centeredStart(l,count+1);
        for(int local=0;local<count;local++)if(inside(mouseX,mouseY,start+local*(SLOT+GAP),l.destinationY,SLOT,SLOT))return destinations.get(startIndex+local);
        return null;
    }

    private boolean addDestinationAt(double mouseX,double mouseY,Layout l){
        int visibleCapacity=Math.max(0,l.capacity-1);
        int startIndex=clampOffset(destinationOffset,destinations.size(),visibleCapacity);
        int count=Math.min(visibleCapacity,Math.max(0,destinations.size()-startIndex));
        int start=centeredStart(l,count+1),x=start+count*(SLOT+GAP);
        return inside(mouseX,mouseY,x,l.destinationY,SLOT,SLOT);
    }

    private int fixedIndex(double mouseX,double mouseY,Layout l,int y,int count){
        int start=centeredStart(l,count);
        for(int i=0;i<count;i++)if(inside(mouseX,mouseY,start+i*(SLOT+GAP),y,SLOT,SLOT))return i;
        return -1;
    }

    private Layout layout(){
        int largest=Math.max(3,Math.max(fake.size(),Math.max(realRow.size(),destinations.size()+1)));
        int maxByWidth=Math.max(1,(width-8+GAP)/(SLOT+GAP));
        int capacity=Math.max(1,Math.min(Math.min(MAX_VISIBLE,largest),maxByWidth));
        int contentW=rowWidth(capacity);
        int contentX=(width-contentW)/2;
        int top=CameraBackground.navigationTop(height);
        boolean both=!fake.isEmpty()&&!realRow.isEmpty();
        int fakeY=top+(both?FAKE_Y:SOLO_PLAYER_Y);
        int realY=top+(both?REAL_Y:SOLO_PLAYER_Y);
        return new Layout(top,contentX,contentW,capacity,
                top+DIMENSION_Y,fakeY,realY,top+DESTINATION_Y,top+ACTION_Y);
    }

    private static int rowWidth(int count){return count<=0?0:count*SLOT+(count-1)*GAP;}
    private static int centeredStart(Layout l,int count){return l.contentX+(l.contentW-rowWidth(count))/2;}
    private static int clampOffset(int offset,int size,int capacity){return capacity<=0?0:Math.max(0,Math.min(Math.max(0,size-capacity),offset));}
    private static int step(int current,int size,int capacity,double amount){if(capacity<=0)return 0;int max=Math.max(0,size-capacity);return Math.max(0,Math.min(max,current+(amount<0?1:-1)));}
    private static boolean inside(double x,double y,int left,int top,int width,int height){return x>=left&&x<left+width&&y>=top&&y<top+height;}

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

    private static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}

    private static final class Layout {
        final int top,contentX,contentW,capacity,dimensionY,fakeY,realY,destinationY,actionY;
        Layout(int top,int contentX,int contentW,int capacity,int dimensionY,int fakeY,int realY,int destinationY,int actionY){
            this.top=top;this.contentX=contentX;this.contentW=contentW;this.capacity=capacity;
            this.dimensionY=dimensionY;this.fakeY=fakeY;this.realY=realY;this.destinationY=destinationY;this.actionY=actionY;
        }
    }
}

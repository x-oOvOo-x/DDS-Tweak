package ddstweak.integration.dds.playerinventory.screen;

import ddstweak.DDSTweak;
import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
import ddstweak.playerinventory.PlayerInventoryClientActionState;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Shared player-inventory client UI. Source-specific adapters provide context and action binding; vanilla packets own item/data state. */
public class UnifiedPlayerInventoryScreen extends AbstractContainerScreen<UnifiedPlayerInventoryMirrorMenu>{
    private static final int IMAGE_WIDTH=176,INVENTORY_HEIGHT=267,FIELD_X=77,FIELD_WIDTH=34,FIELD_HEIGHT=18,FIELD_EDIT_HEIGHT=14,FIELD_EDIT_Y=5,ARROW_X=111,ARROW_WIDTH=9,ARROW_HEIGHT=9,ACTION_RIGHT_X=151,ACTION_STEP=22,ATTACK_Y=7,USE_Y=29,INTERVAL_BUTTON_X=ACTION_RIGHT_X-ACTION_STEP,CONTINUOUS_BUTTON_X=ACTION_RIGHT_X,PERSISTENT_Y=61,STOP_X=ACTION_RIGHT_X-ACTION_STEP*2,DROP_ALL_X=ACTION_RIGHT_X-ACTION_STEP,DISCONNECT_X=ACTION_RIGHT_X,HOTBAR_NUMBER_X=7,HOTBAR_NUMBER_Y=161,HOTBAR_NUMBER_WIDTH=18,HOTBAR_NUMBER_HEIGHT=6;
    private static final int TOOLBAR_HEIGHT=8,ENDER_BUTTON_Y=-3,ENDER_SNEAK_X=7,ENDER_DISCONNECT_X=29;
    private static final int BUTTON_WIDTH=18,BUTTON_HEIGHT=18,BUTTON_RENDER_SIZE=20,BUTTON_RENDER_OFFSET=1,ICON_SIZE=16,ICON_ATLAS_WIDTH=160,ICON_ATTACK_INTERVAL=0,ICON_ATTACK_CONTINUOUS=1,ICON_USE_INTERVAL=2,ICON_USE_CONTINUOUS=3,ICON_STOP=4,ICON_DROP=5,ICON_DISCONNECT=6,ICON_SNEAK=9;
    private static final long ONCE_FEEDBACK_NANOS=220_000_000L;
    private static final Identifier INVENTORY_BACKGROUND=id("textures/gui/container/gca_fake_player_inventory.png"),ENDER_BACKGROUND_3=id("textures/gui/container/gca_fake_player_ender_chest_3.png"),ENDER_BACKGROUND_6=id("textures/gui/container/gca_fake_player_ender_chest_6.png"),BUTTON_NORMAL=id("textures/gui/gca/action_button_normal.png"),BUTTON_HOVER=id("textures/gui/gca/action_button_hover.png"),BUTTON_ACTIVE=id("textures/gui/gca/action_button_active.png"),ACTION_ICONS=id("textures/gui/gca/action_icons.png"),INTERVAL_INPUT=id("textures/gui/gca/interval_input.png"),INTERVAL_INPUT_FOCUSED=id("textures/gui/gca/interval_input_focused.png"),INTERVAL_ARROWS=id("textures/gui/gca/interval_arrows.png"),HOTBAR_NUMBERS=id("textures/gui/gca/hotbar_number_buttons.png"),HOTBAR_SELECTED=id("textures/gui/gca/hotbar_selected.png");
    private final List<ActionButton> buttons=new ArrayList<>();private final String legacyTargetName;private LivingEntity targetEntity;private EditBox attackInterval,useInterval;private int lastAttackInterval=-1,lastUseInterval=-1,pressedStepper=-1;private long attackOnceFeedbackUntil,useOnceFeedbackUntil;private boolean syncingIntervalFields;

    public UnifiedPlayerInventoryScreen(UnifiedPlayerInventoryMirrorMenu menu,Inventory inventory,Component title){
        //#if MC >= 260100
        //$$ super(menu,inventory,title,IMAGE_WIDTH,menu.kind()==PlayerInventoryKind.ENDER_CHEST?enderHeight(menu.visualRows()):INVENTORY_HEIGHT);
        //#else
        super(menu,inventory,title);imageWidth=IMAGE_WIDTH;imageHeight=menu.kind()==PlayerInventoryKind.ENDER_CHEST?enderHeight(menu.visualRows()):INVENTORY_HEIGHT;
        //#endif
        //#if MC >= 11600
        titleLabelX=inventoryLabelX=-10000;titleLabelY=inventoryLabelY=-10000;
        //#endif
        legacyTargetName=title.getString();
    }
    private boolean isInventory(){return menu.kind()!=PlayerInventoryKind.ENDER_CHEST;}private boolean isEnder(){return menu.kind()==PlayerInventoryKind.ENDER_CHEST;}

    @Override protected void init(){super.init();buttons.clear();attackInterval=useInterval=null;if(isEnder()){topPos+=TOOLBAR_HEIGHT;initEnder();}else initInventory();}
    private void initInventory(){attackInterval=createIntervalBox(leftPos+FIELD_X,topPos+ATTACK_Y+FIELD_EDIT_Y,tr("dds-tweak.fakeplayer.attack.interval"));useInterval=createIntervalBox(leftPos+FIELD_X,topPos+USE_Y+FIELD_EDIT_Y,tr("dds-tweak.fakeplayer.use.interval"));attackInterval.setResponder(this::onAttackIntervalChanged);
        //#if MC >= 11700
        addRenderableWidget(attackInterval);
        //#else
        //$$ addButton(attackInterval);
        //#endif
        useInterval.setResponder(this::onUseIntervalChanged);
        //#if MC >= 11700
        addRenderableWidget(useInterval);
        //#else
        //$$ addButton(useInterval);
        //#endif
        Component attackIntervalTip=tr("dds-tweak.fakeplayer.action.attack_interval"),attackContinuousTip=tr("dds-tweak.fakeplayer.action.attack_continuous"),useIntervalTip=tr("dds-tweak.fakeplayer.action.use_interval"),useContinuousTip=tr("dds-tweak.fakeplayer.action.use_continuous");
        addButton(INTERVAL_BUTTON_X,ATTACK_Y,ICON_ATTACK_INTERVAL,EnumSet.of(PlayerInventoryAction.ATTACK_INTERVAL,PlayerInventoryAction.STOP_ATTACK),menu::isAttackIntervalActive,this::toggleAttackInterval,toggleTooltip(attackIntervalTip,false),toggleTooltip(attackIntervalTip,true));
        addButton(CONTINUOUS_BUTTON_X,ATTACK_Y,ICON_ATTACK_CONTINUOUS,EnumSet.of(PlayerInventoryAction.ATTACK_CONTINUOUS,PlayerInventoryAction.STOP_ATTACK),menu::isAttackContinuousActive,this::toggleAttackContinuous,toggleTooltip(attackContinuousTip,false),toggleTooltip(attackContinuousTip,true));
        addButton(INTERVAL_BUTTON_X,USE_Y,ICON_USE_INTERVAL,EnumSet.of(PlayerInventoryAction.USE_INTERVAL,PlayerInventoryAction.STOP_USE),menu::isUseIntervalActive,this::toggleUseInterval,toggleTooltip(useIntervalTip,false),toggleTooltip(useIntervalTip,true));
        addButton(CONTINUOUS_BUTTON_X,USE_Y,ICON_USE_CONTINUOUS,EnumSet.of(PlayerInventoryAction.USE_CONTINUOUS,PlayerInventoryAction.STOP_USE),menu::isUseContinuousActive,this::toggleUseContinuous,toggleTooltip(useContinuousTip,false),toggleTooltip(useContinuousTip,true));
        addButton(STOP_X,PERSISTENT_Y,ICON_STOP,Collections.singleton(PlayerInventoryAction.STOP_ALL),()->false,this::stopAll,tr("dds-tweak.fakeplayer.action.stop_all"));
        addButton(DROP_ALL_X,PERSISTENT_Y,ICON_DROP,Collections.singleton(PlayerInventoryAction.DROP_ALL),()->false,()->send(PlayerInventoryAction.DROP_ALL,0),tr("dds-tweak.fakeplayer.action.drop_all"));
        addButton(DISCONNECT_X,PERSISTENT_Y,ICON_DISCONNECT,Collections.singleton(PlayerInventoryAction.DISCONNECT),()->false,()->send(PlayerInventoryAction.DISCONNECT,0),tr("dds-tweak.fakeplayer.action.disconnect"));initializeIntervalFields();}
    private void initEnder(){
        addButton(ENDER_SNEAK_X,ENDER_BUTTON_Y,ICON_SNEAK,Collections.singleton(PlayerInventoryAction.TOGGLE_SNEAK),menu::isSneakingActive,this::toggleSneak,tr("dds-tweak.fakeplayer.ender.sneak"),tr("dds-tweak.fakeplayer.ender.unsneak"));
        addButton(ENDER_DISCONNECT_X,ENDER_BUTTON_Y,ICON_DISCONNECT,Collections.singleton(PlayerInventoryAction.DISCONNECT),()->false,()->send(PlayerInventoryAction.DISCONNECT,0),tr("dds-tweak.fakeplayer.action.disconnect"));
    }

    private EditBox createIntervalBox(int x,int y,Component narration){
        //#if MC >= 12108
        EditBox box=new EditBox(font,x,y,FIELD_WIDTH,FIELD_EDIT_HEIGHT,narration);box.setCentered(true);
        //#else
        //$$ EditBox box=new LegacyCenteredEditBox(x,y,narration);
        //#endif
        //#if MC < 260102
        box.setFilter(UnifiedPlayerInventoryScreen::isValidIntervalText);
        //#endif
        box.setMaxLength(10);box.setBordered(false);return box;
    }
    //#if MC < 12108
    //$$ private final class LegacyCenteredEditBox extends EditBox{
    //#if MC >= 11600
    //$$     LegacyCenteredEditBox(int x,int y,Component narration){super(font,x,y,FIELD_WIDTH,FIELD_EDIT_HEIGHT,narration);}
    //#else
    //$$     LegacyCenteredEditBox(int x,int y,Component narration){super(font,x,y,FIELD_WIDTH,FIELD_EDIT_HEIGHT,narration.getString());}
    //#endif
    //$$     private int centerOffset(){return Math.max(0,(getWidth()-font.width(getValue()))/2);}
    //#if MC >= 12000
    //$$     @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick){int x=getX(),offset=centerOffset();setX(x+offset);try{super.renderWidget(g,mouseX,mouseY,partialTick);}finally{setX(x);}}
    //#elseif MC >= 11904
    //$$     @Override public void renderWidget(PoseStack g,int mouseX,int mouseY,float partialTick){int x=getX(),offset=centerOffset();setX(x+offset);try{super.renderWidget(g,mouseX,mouseY,partialTick);}finally{setX(x);}}
    //#elseif MC >= 11600
    //$$     @Override public void renderButton(PoseStack g,int mouseX,int mouseY,float partialTick){int oldX=this.x,offset=centerOffset();this.x=oldX+offset;try{super.renderButton(g,mouseX,mouseY,partialTick);}finally{this.x=oldX;}}
    //#else
    //$$     @Override public void renderButton(int mouseX,int mouseY,float partialTick){int oldX=this.x,offset=centerOffset();this.x=oldX+offset;try{super.renderButton(mouseX,mouseY,partialTick);}finally{this.x=oldX;}}
    //#endif
    //$$     @Override public void onClick(double mouseX,double mouseY){super.onClick(mouseX-centerOffset(),mouseY);}
    //$$ }
    //#endif

    private void addButton(int x,int y,int icon,Set<PlayerInventoryAction> required,BooleanSupplier selected,Runnable action,Component tip){addButton(x,y,icon,required,selected,action,tip,tip);}private void addButton(int x,int y,int icon,Set<PlayerInventoryAction> required,BooleanSupplier selected,Runnable action,Component off,Component on){buttons.add(new ActionButton(leftPos+x,topPos+y,icon,required,selected,action,off,on));}
    //#if MC >= 11700
    @Override protected void containerTick(){super.containerTick();if(isInventory()){syncActionAvailability();syncIntervalFields();}}
    //#else
    //$$ @Override public void tick(){super.tick();if(isInventory()){syncActionAvailability();syncIntervalFields();}}
    //#endif
    //#if MC >= 260100
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){super.extractRenderState(g,mouseX,mouseY,partialTick);List<Component> tooltip=controlTooltip(mouseX,mouseY);if(tooltip!=null)g.setTooltipForNextFrame(font,tooltip,Optional.empty(),mouseX,mouseY);}
    //$$ @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){super.extractBackground(g,mouseX,mouseY,partialTick);if(isEnder())renderEnderBg(g,mouseX,mouseY);else renderInventoryBg(g,mouseX,mouseY);}
    //#elseif MC >= 12000
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);List<Component> tooltip=controlTooltip(mouseX,mouseY);if(tooltip!=null)g.setTooltipForNextFrame(font,tooltip,Optional.empty(),mouseX,mouseY);}
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY){if(isEnder())renderEnderBg(g,mouseX,mouseY);else renderInventoryBg(g,mouseX,mouseY);}
    //#elseif MC >= 11700
    //$$ @Override public void render(PoseStack g,int mouseX,int mouseY,float partialTick){super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);List<Component> tooltip=controlTooltip(mouseX,mouseY);if(tooltip!=null)renderTooltip(g,tooltip,Optional.empty(),mouseX,mouseY);}
    //$$ @Override protected void renderBg(PoseStack g,float partialTick,int mouseX,int mouseY){if(isEnder())renderEnderBg(g,mouseX,mouseY);else renderInventoryBg(g,mouseX,mouseY);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack g,int mouseX,int mouseY,float partialTick){super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);List<Component> tooltip=controlTooltip(mouseX,mouseY);if(tooltip!=null)renderComponentTooltip(g,tooltip,mouseX,mouseY);}
    //$$ @Override protected void renderBg(PoseStack g,float partialTick,int mouseX,int mouseY){if(isEnder())renderEnderBg(g,mouseX,mouseY);else renderInventoryBg(g,mouseX,mouseY);}
    //#else
    //$$ @Override public void render(int mouseX,int mouseY,float partialTick){super.render(mouseX,mouseY,partialTick);renderTooltip(mouseX,mouseY);List<Component> tooltip=controlTooltip(mouseX,mouseY);if(tooltip!=null)renderTooltip(legacyTooltip(tooltip),mouseX,mouseY);}
    //$$ @Override protected void renderBg(float partialTick,int mouseX,int mouseY){if(isEnder())renderEnderBg(null,mouseX,mouseY);else renderInventoryBg(null,mouseX,mouseY);}
    //#endif
    private void renderInventoryBg(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        int mouseX,int mouseY){blit(g,INVENTORY_BACKGROUND,leftPos,topPos,imageWidth,imageHeight);renderInventoryControls(g,mouseX,mouseY);renderTargetPlayer(g,mouseX,mouseY);if(menu.kind()==PlayerInventoryKind.INVENTORY)drawSelectedHotbar(g);renderButtons(g,mouseX,mouseY);renderTargetName(g);}
    private void renderTargetName(
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
        if(menu.targetType()==PlayerInventoryTargetType.FAKE)return;
        String name=menu.context().targetName();
        if(name==null||name.trim().isEmpty())return;
        String label=menu.context().targetState()==PlayerInventoryTargetState.OFFLINE
                ?tr("dds-tweak.playerinventory.offline_name",name).getString():name;
        int max=86;
        if(font.width(label)>max){
            while(!label.isEmpty()&&font.width(label+"…")>max)label=label.substring(0,label.length()-1);
            label+="…";
        }
        ScreenRenderCompat.text(g,font,label,leftPos+78,topPos+18,0x404040,false);
    }
    private void renderEnderBg(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        int mouseX,int mouseY){int panelTop=topPos-TOOLBAR_HEIGHT;Identifier background=menu.visualRows()==6?ENDER_BACKGROUND_6:ENDER_BACKGROUND_3;blitRegion(g,background,leftPos,panelTop,0,0,IMAGE_WIDTH,imageHeight,IMAGE_WIDTH,imageHeight);renderButtons(g,mouseX,mouseY);}
    private void renderInventoryControls(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        int mouseX,int mouseY){if(attackInterval!=null&&attackInterval.visible){blit(g,attackInterval.isFocused()?INTERVAL_INPUT_FOCUSED:INTERVAL_INPUT,leftPos+FIELD_X,topPos+ATTACK_Y,FIELD_WIDTH,FIELD_HEIGHT);renderArrows(g,ATTACK_Y,0);}if(useInterval!=null&&useInterval.visible){blit(g,useInterval.isFocused()?INTERVAL_INPUT_FOCUSED:INTERVAL_INPUT,leftPos+FIELD_X,topPos+USE_Y,FIELD_WIDTH,FIELD_HEIGHT);renderArrows(g,USE_Y,2);}if(canUse(PlayerInventoryAction.SELECT_HOTBAR))for(int i=0;i<9;i++){int x=leftPos+HOTBAR_NUMBER_X+i*18,y=topPos+HOTBAR_NUMBER_Y,state=menu.selectedTargetHotbarSlot()==i?2:inside(mouseX,mouseY,x,y,HOTBAR_NUMBER_WIDTH,HOTBAR_NUMBER_HEIGHT)?1:0;blitRegion(g,HOTBAR_NUMBERS,x,y,i*HOTBAR_NUMBER_WIDTH,state*HOTBAR_NUMBER_HEIGHT,HOTBAR_NUMBER_WIDTH,HOTBAR_NUMBER_HEIGHT,162,18);}}
    private void renderArrows(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        int rowY,int base){int x=leftPos+ARROW_X,y=topPos+rowY;blitRegion(g,INTERVAL_ARROWS,x,y,pressedStepper==base?ARROW_WIDTH:0,0,ARROW_WIDTH,ARROW_HEIGHT,18,18);blitRegion(g,INTERVAL_ARROWS,x,y+ARROW_HEIGHT,pressedStepper==base+1?ARROW_WIDTH:0,ARROW_HEIGHT,ARROW_WIDTH,ARROW_HEIGHT,18,18);}
    private void renderTargetPlayer(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        int mouseX,int mouseY){LivingEntity target=findTargetPlayer();if(target==null)return;
        //#if MC >= 260100
        //$$ InventoryScreen.extractEntityInInventoryFollowsMouse(g,leftPos+26,topPos+8,leftPos+75,topPos+78,30,0.0625F,mouseX,mouseY,target);
        //#elseif MC >= 12002
        InventoryScreen.renderEntityInInventoryFollowsMouse(g,leftPos+26,topPos+8,leftPos+75,topPos+78,30,0.0625F,mouseX,mouseY,target);
        //#elseif MC >= 11904
        //$$ InventoryScreen.renderEntityInInventoryFollowsMouse(g,leftPos+51,topPos+75,30,(float)(leftPos+51-mouseX),(float)(topPos+25-mouseY),target);
        //#else
        //$$ InventoryScreen.renderEntityInInventory(leftPos+51,topPos+75,30,(float)(leftPos+51-mouseX),(float)(topPos+25-mouseY),target);
        //#endif
    }
    private void renderButtons(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        int mouseX,int mouseY){for(ActionButton b:buttons){if(!visible(b))continue;boolean enabled=supported(b),active=enabled&&b.isSelected(),pulse=enabled&&onceFeedback(b),hovered=enabled&&!active&&!pulse&&b.contains(mouseX,mouseY);Identifier background=pulse?(active?BUTTON_NORMAL:BUTTON_ACTIVE):active?BUTTON_ACTIVE:hovered?BUTTON_HOVER:BUTTON_NORMAL;int o=BUTTON_RENDER_OFFSET;blit(g,background,b.x-o,b.y-o,BUTTON_RENDER_SIZE,BUTTON_RENDER_SIZE);renderActionIcon(g,b.icon,b.x+1,b.y+1);}}
    private void drawSelectedHotbar(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g
        //#elseif MC >= 12000
        GuiGraphics g
        //#elseif MC >= 11600
        //$$ PoseStack g
        //#else
        //$$ Object g
        //#endif
    ){Slot slot=selectedTargetHotbarSlot();if(slot!=null)blit(g,HOTBAR_SELECTED,leftPos+slot.x-2,topPos+slot.y-2,20,20);}

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){int button=e.button();if(button==0&&isInventory()&&(pressIntervalStepper(e.x(),e.y())||pressHotbarNumber(e.x(),e.y())))return true;if((button==0||button==1)&&pressActionButton(e.x(),e.y(),button))return true;return super.mouseClicked(e,doubleClick);}@Override public boolean mouseReleased(MouseButtonEvent e){if(e.button()==0)pressedStepper=-1;return super.mouseReleased(e);}@Override public boolean keyPressed(KeyEvent e){if(isInventory()&&isIntervalFieldEditing())return super.keyPressed(e);if(isInventory()&&trySelectHotbar(e))return true;return super.keyPressed(e);}
    private boolean trySelectHotbar(KeyEvent e){if(hoveredSlot!=null||!canUse(PlayerInventoryAction.SELECT_HOTBAR))return false;for(int i=0;i<minecraft.options.keyHotbarSlots.length;i++)if(minecraft.options.keyHotbarSlots[i].matches(e)){selectHotbar(i);return true;}return false;}
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&isInventory()&&(pressIntervalStepper(x,y)||pressHotbarNumber(x,y)))return true;if((button==0||button==1)&&pressActionButton(x,y,button))return true;return super.mouseClicked(x,y,button);}@Override public boolean mouseReleased(double x,double y,int button){if(button==0)pressedStepper=-1;return super.mouseReleased(x,y,button);}@Override public boolean keyPressed(int key,int scan,int modifiers){if(isInventory()&&isIntervalFieldEditing())return super.keyPressed(key,scan,modifiers);if(isInventory()&&trySelectHotbar(key,scan))return true;return super.keyPressed(key,scan,modifiers);}
    //$$ private boolean trySelectHotbar(int key,int scan){if(hoveredSlot!=null||!canUse(PlayerInventoryAction.SELECT_HOTBAR))return false;for(int i=0;i<minecraft.options.keyHotbarSlots.length;i++)if(minecraft.options.keyHotbarSlots[i].matches(key,scan)){selectHotbar(i);return true;}return false;}
    //#endif

    private boolean pressIntervalStepper(double x,double y){return attackInterval!=null&&attackInterval.active&&pressStepper(x,y,ATTACK_Y,true,0)||useInterval!=null&&useInterval.active&&pressStepper(x,y,USE_Y,false,2);}private boolean pressStepper(double mx,double my,int rowY,boolean attack,int base){int x=leftPos+ARROW_X,y=topPos+rowY;if(inside(mx,my,x,y,ARROW_WIDTH,ARROW_HEIGHT)){pressedStepper=base;stepInterval(attack,1);return true;}if(inside(mx,my,x,y+ARROW_HEIGHT,ARROW_WIDTH,ARROW_HEIGHT)){pressedStepper=base+1;stepInterval(attack,-1);return true;}return false;}
    private void stepInterval(boolean attack,int delta){EditBox box=attack?attackInterval:useInterval;long next=(long)readInterval(box)+delta;int v=(int)Math.max(PlayerInventoryBehaviorV1.MIN_INTERVAL,Math.min((long)PlayerInventoryBehaviorV1.MAX_INTERVAL,next));setFieldValue(box,v);rememberInterval(attack,v);if(attack)lastAttackInterval=v;else lastUseInterval=v;}private boolean pressHotbarNumber(double mx,double my){if(!canUse(PlayerInventoryAction.SELECT_HOTBAR))return false;for(int i=0;i<9;i++)if(inside(mx,my,leftPos+HOTBAR_NUMBER_X+i*18,topPos+HOTBAR_NUMBER_Y,HOTBAR_NUMBER_WIDTH,HOTBAR_NUMBER_HEIGHT)){selectHotbar(i);return true;}return false;}private boolean pressActionButton(double mx,double my,int button){for(ActionButton b:buttons)if(visible(b)&&b.contains(mx,my)){if(button==0){if(supported(b))b.press();return true;}PlayerInventoryAction once=onceAction(b);if(once!=null&&canUse(once))sendOnce(once);return true;}return false;}private static PlayerInventoryAction onceAction(ActionButton b){if(b==null)return null;return b.icon==ICON_ATTACK_CONTINUOUS?PlayerInventoryAction.ATTACK_ONCE:b.icon==ICON_USE_CONTINUOUS?PlayerInventoryAction.USE_ONCE:null;}private void sendOnce(PlayerInventoryAction action){if(!send(action,0))return;long until=System.nanoTime()+ONCE_FEEDBACK_NANOS;if(action==PlayerInventoryAction.ATTACK_ONCE){attackOnceFeedbackUntil=until;menu.predictAttackOnce();}else if(action==PlayerInventoryAction.USE_ONCE){useOnceFeedbackUntil=until;menu.predictUseOnce();}}private boolean onceFeedback(ActionButton button){long now=System.nanoTime();return button.icon==ICON_ATTACK_CONTINUOUS?now<attackOnceFeedbackUntil:button.icon==ICON_USE_CONTINUOUS&&now<useOnceFeedbackUntil;}
    private boolean isIntervalFieldEditing(){return attackInterval!=null&&attackInterval.canConsumeInput()||useInterval!=null&&useInterval.canConsumeInput();}private void selectHotbar(int slot){if(send(PlayerInventoryAction.SELECT_HOTBAR,slot+1))menu.predictSelectedHotbar(slot);}
    private List<Component> controlTooltip(int mx,int my){
        if(isInventory()){
            if(attackInterval!=null&&attackInterval.visible&&inside(mx,my,leftPos+FIELD_X,topPos+ATTACK_Y,FIELD_WIDTH,FIELD_HEIGHT))return Collections.singletonList(intervalFieldTooltip(true));
            if(useInterval!=null&&useInterval.visible&&inside(mx,my,leftPos+FIELD_X,topPos+USE_Y,FIELD_WIDTH,FIELD_HEIGHT))return Collections.singletonList(intervalFieldTooltip(false));
            for(int y:new int[]{ATTACK_Y,USE_Y}){
                if(y==ATTACK_Y&&(attackInterval==null||!attackInterval.visible)||y==USE_Y&&(useInterval==null||!useInterval.visible))continue;
                int x=leftPos+ARROW_X,py=topPos+y;
                if(inside(mx,my,x,py,ARROW_WIDTH,ARROW_HEIGHT))return Collections.singletonList(tr("dds-tweak.fakeplayer.interval.increase"));
                if(inside(mx,my,x,py+ARROW_HEIGHT,ARROW_WIDTH,ARROW_HEIGHT))return Collections.singletonList(tr("dds-tweak.fakeplayer.interval.decrease"));
            }
            if(canUse(PlayerInventoryAction.SELECT_HOTBAR))for(int i=0;i<9;i++)
                if(inside(mx,my,leftPos+HOTBAR_NUMBER_X+i*18,topPos+HOTBAR_NUMBER_Y,HOTBAR_NUMBER_WIDTH,HOTBAR_NUMBER_HEIGHT))
                    return commandTooltip(tr("dds-tweak.fakeplayer.hotbar.select",i+1),PlayerInventoryAction.SELECT_HOTBAR,i+1);
        }
        for(ActionButton b:buttons)if(visible(b)&&b.contains(mx,my))
            return supported(b)?actionTooltip(b):Arrays.asList(b.tooltip(),MinecraftCompat.grayTranslatable("dds-tweak.fakeplayer.action.unavailable"));
        return null;
    }
    private List<Component> actionTooltip(ActionButton button){
        PlayerInventoryAction action;
        switch(button.icon){
            case ICON_ATTACK_INTERVAL:action=button.isSelected()?PlayerInventoryAction.STOP_ATTACK:PlayerInventoryAction.ATTACK_INTERVAL;break;
            case ICON_ATTACK_CONTINUOUS:action=button.isSelected()?PlayerInventoryAction.STOP_ATTACK:PlayerInventoryAction.ATTACK_CONTINUOUS;break;
            case ICON_USE_INTERVAL:action=button.isSelected()?PlayerInventoryAction.STOP_USE:PlayerInventoryAction.USE_INTERVAL;break;
            case ICON_USE_CONTINUOUS:action=button.isSelected()?PlayerInventoryAction.STOP_USE:PlayerInventoryAction.USE_CONTINUOUS;break;
            case ICON_STOP:action=PlayerInventoryAction.STOP_ALL;break;
            case ICON_DROP:action=isEnder()?null:PlayerInventoryAction.DROP_ALL;break;
            case ICON_DISCONNECT:action=PlayerInventoryAction.DISCONNECT;break;
            case ICON_SNEAK:action=PlayerInventoryAction.TOGGLE_SNEAK;break;
            default:action=null;
        }
        int value;
        switch(button.icon){
            case ICON_ATTACK_INTERVAL:value=currentInterval(true);break;
            case ICON_USE_INTERVAL:value=currentInterval(false);break;
            case ICON_SNEAK:value=menu.isSneakingActive()?0:1;break;
            default:value=0;
        }
        List<Component> lines=new ArrayList<>();lines.add(button.tooltip());
        PlayerInventoryAction once=onceAction(button);
        if(once!=null&&canUse(once)){
            PlayerInventoryAction continuous=button.icon==ICON_ATTACK_CONTINUOUS?PlayerInventoryAction.ATTACK_CONTINUOUS:PlayerInventoryAction.USE_CONTINUOUS;
            appendCommandTooltip(lines,continuous,0);
            lines.add(tr("dds-tweak.fakeplayer.action.once_hint",tr(button.icon==ICON_ATTACK_CONTINUOUS?"dds-tweak.fakeplayer.action.attack_once":"dds-tweak.fakeplayer.action.use_once")));
            appendCommandTooltip(lines,once,0);
            return lines;
        }
        appendCommandTooltip(lines,action,value);
        return lines;
    }
    private List<Component> commandTooltip(Component base,PlayerInventoryAction action,int value){
        List<Component> lines=new ArrayList<>();lines.add(base);appendCommandTooltip(lines,action,value);return lines;
    }
    private void appendCommandTooltip(List<Component> lines,PlayerInventoryAction action,int value){
        if(action==null||!ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge.supports(action))return;
        String command=ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge.commandFor(menu.context().targetName(),action,value);
        if(command!=null)lines.add(MinecraftCompat.grayLiteral("/"+command));
    }
    private Slot selectedTargetHotbarSlot(){int i=menu.selectedTargetHotbarSlot();return i>=0&&i<=8?menu.getSlot(menu.targetHotbarStart()+i):null;}private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}

    private void onAttackIntervalChanged(String s){onIntervalChanged(s,true);}private void onUseIntervalChanged(String s){onIntervalChanged(s,false);}
    private void onIntervalChanged(String text,boolean attack){
        if(syncingIntervalFields)return;
        //#if MC >= 260102
        //$$ if(!isValidIntervalText(text)){EditBox box=attack?attackInterval:useInterval;String clean=digitsOnly(text);syncingIntervalFields=true;try{if(box!=null&&!clean.equals(box.getValue()))box.setValue(clean);}finally{syncingIntervalFields=false;}return;}
        //#endif
        Integer v=parseInterval(text);if(v==null)return;rememberInterval(attack,v);if(attack)lastAttackInterval=v;else lastUseInterval=v;int mode=attack?menu.attackMode():menu.useMode();if(mode!=PlayerInventoryBehaviorV1.MODE_NONE)selectInterval(attack,v);
    }
    private void toggleAttackInterval(){toggleInterval(true);}private void toggleUseInterval(){toggleInterval(false);}private void toggleInterval(boolean attack){boolean active=attack?menu.isAttackIntervalActive():menu.isUseIntervalActive();if(active){PlayerInventoryAction action=attack?PlayerInventoryAction.STOP_ATTACK:PlayerInventoryAction.STOP_USE;if(send(action,0)){if(attack)menu.predictStopAttack();else menu.predictStopUse();}return;}selectInterval(attack,currentInterval(attack));}private void toggleAttackContinuous(){toggleContinuous(true);}private void toggleUseContinuous(){toggleContinuous(false);}private void toggleContinuous(boolean attack){boolean active=attack?menu.isAttackContinuousActive():menu.isUseContinuousActive();if(active){PlayerInventoryAction action=attack?PlayerInventoryAction.STOP_ATTACK:PlayerInventoryAction.STOP_USE;if(send(action,0)){if(attack)menu.predictStopAttack();else menu.predictStopUse();}return;}PlayerInventoryAction action=attack?PlayerInventoryAction.ATTACK_CONTINUOUS:PlayerInventoryAction.USE_CONTINUOUS;if(send(action,0)){if(attack)menu.predictAttackContinuous();else menu.predictUseContinuous();}}
    private void selectInterval(boolean attack,int interval){int v=PlayerInventoryBehaviorV1.clampInterval(interval);PlayerInventoryAction action=attack?PlayerInventoryAction.ATTACK_INTERVAL:PlayerInventoryAction.USE_INTERVAL;if(send(action,v)){if(attack)menu.predictAttackInterval(v);else menu.predictUseInterval(v);}}private void stopAll(){if(send(PlayerInventoryAction.STOP_ALL,0))menu.predictStopAll();}private void toggleSneak(){boolean next=!menu.isSneakingActive();if(send(PlayerInventoryAction.TOGGLE_SNEAK,next?1:0))menu.predictSneaking(next);}
    private void syncActionAvailability(){boolean transport=menu.canSendActions();if(attackInterval!=null){boolean enabled=transport&&menu.supports(PlayerInventoryAction.ATTACK_INTERVAL);attackInterval.active=enabled;attackInterval.visible=enabled;}if(useInterval!=null){boolean enabled=transport&&menu.supports(PlayerInventoryAction.USE_INTERVAL);useInterval.active=enabled;useInterval.visible=enabled;}}
    private void initializeIntervalFields(){syncActionAvailability();syncingIntervalFields=true;try{lastAttackInterval=initialInterval(true);lastUseInterval=initialInterval(false);setFieldValue(attackInterval,lastAttackInterval);setFieldValue(useInterval,lastUseInterval);}finally{syncingIntervalFields=false;}}
    private int initialInterval(boolean attack){return PlayerInventoryBehaviorV1.clampInterval(attack?menu.attackInterval():menu.useInterval());}
    private void syncIntervalFields(){syncingIntervalFields=true;try{lastAttackInterval=syncIntervalField(attackInterval,true,lastAttackInterval);lastUseInterval=syncIntervalField(useInterval,false,lastUseInterval);}finally{syncingIntervalFields=false;}}
    private int syncIntervalField(EditBox box,boolean attack,int old){if(box==null||box.isFocused()||!(attack?menu.isAttackIntervalActive():menu.isUseIntervalActive()))return old;int v=PlayerInventoryBehaviorV1.clampInterval(attack?menu.attackInterval():menu.useInterval());if(v!=old)setFieldValue(box,v);return v;}
    private Component intervalFieldTooltip(boolean attack){return tr("dds-tweak.fakeplayer.interval.tooltip",PlayerInventoryBehaviorV1.MIN_INTERVAL,PlayerInventoryBehaviorV1.MAX_INTERVAL);}
    private static Component toggleTooltip(Component base,boolean active){return tr(active?"dds-tweak.fakeplayer.action.toggle_active":"dds-tweak.fakeplayer.action.toggle",base);}
    private static void setFieldValue(EditBox box,int v){if(box!=null)box.setValue(Integer.toString(v));}private static int readInterval(EditBox box){if(box==null)return PlayerInventoryBehaviorV1.DEFAULT_INTERVAL;Integer v=parseInterval(box.getValue());return v==null?PlayerInventoryBehaviorV1.DEFAULT_INTERVAL:v;}private static Integer parseInterval(String text){if(!isValidIntervalText(text)||text.isEmpty())return null;try{return PlayerInventoryBehaviorV1.clampInterval(Integer.parseInt(text));}catch(NumberFormatException ignored){return null;}}private static boolean isValidIntervalText(String text){if(text==null||text.length()>10)return false;for(int i=0;i<text.length();i++)if(!Character.isDigit(text.charAt(i)))return false;return true;}private static String digitsOnly(String text){if(text==null||text.isEmpty())return "";StringBuilder out=new StringBuilder(Math.min(10,text.length()));for(int i=0;i<text.length()&&out.length()<10;i++){char c=text.charAt(i);if(Character.isDigit(c))out.append(c);}return out.toString();}
    private int currentInterval(boolean attack){EditBox box=attack?attackInterval:useInterval;return box==null?PlayerInventoryBehaviorV1.clampInterval(attack?menu.attackInterval():menu.useInterval()):readInterval(box);}
    private void rememberInterval(boolean attack,int value){PlayerInventoryClientActionState.State state=PlayerInventoryClientActionState.forContext(menu.context());if(attack)state.rememberAttackInterval(value);else state.rememberUseInterval(value);}
    private boolean canUse(PlayerInventoryAction action){return menu.canSendActions()&&menu.supports(action);}private boolean visible(ActionButton button){return menu.targetType()==PlayerInventoryTargetType.FAKE&&(isEnder()||supported(button));}private boolean supported(ActionButton button){if(!menu.canSendActions())return false;for(PlayerInventoryAction action:button.required)if(!menu.supports(action))return false;return true;}
    private LivingEntity findTargetPlayer(){if(targetEntity!=null)return targetEntity;if(minecraft==null||minecraft.level==null)return null;PlayerInventoryContext c=menu.context();UUID id=c.targetUuid();String name=c.targetName();if(id!=null)for(Player p:minecraft.level.players())if(id.equals(p.getUUID()))return targetEntity=p;if(!name.trim().isEmpty())for(Player p:minecraft.level.players())if(p.getName().getString().equals(name)||p.getDisplayName().getString().equals(name))return targetEntity=p;for(Player p:minecraft.level.players())if(p.getName().getString().equals(legacyTargetName))return targetEntity=p;return null;}private boolean send(PlayerInventoryAction action,int value){return menu.send(action,value);}
    private static int enderHeight(int rows){return 114+rows*18+TOOLBAR_HEIGHT;}
    private static List<String> legacyTooltip(List<Component> tooltip){List<String> lines=new ArrayList<String>(tooltip.size());for(Component line:tooltip)lines.add(line.getString());return lines;}
    private static Component tr(String key,Object...args){return MinecraftCompat.translatable(key,args);}

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
        Identifier t,int x,int y,int w,int h){
        ScreenRenderCompat.blit(g,t,x,y,0,0,w,h,w,h);
    }
    private static void blitRegion(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        Identifier t,int x,int y,int u,int v,int w,int h,int tw,int th){
        ScreenRenderCompat.blit(g,t,x,y,u,v,w,h,tw,th);
    }
    private static void renderActionIcon(
        //#if MC >= 260100
        //$$ GuiGraphicsExtractor g,
        //#elseif MC >= 12000
        GuiGraphics g,
        //#elseif MC >= 11600
        //$$ PoseStack g,
        //#else
        //$$ Object g,
        //#endif
        int icon,int x,int y){if(icon<0||icon*ICON_SIZE>=ICON_ATLAS_WIDTH)return;blitRegion(g,ACTION_ICONS,x,y,icon*ICON_SIZE,0,ICON_SIZE,ICON_SIZE,ICON_ATLAS_WIDTH,ICON_SIZE);}private static Identifier id(String path){return MinecraftCompat.id(DDSTweak.MOD_ID,path);}
    private static final class ActionButton{final int x,y,icon;final Set<PlayerInventoryAction> required;private final BooleanSupplier selected;private final Runnable action;private final Component inactiveTooltip,activeTooltip;ActionButton(int x,int y,int icon,Set<PlayerInventoryAction> required,BooleanSupplier selected,Runnable action,Component inactiveTooltip,Component activeTooltip){this.x=x;this.y=y;this.icon=icon;this.required=Collections.unmodifiableSet(EnumSet.copyOf(required));this.selected=selected;this.action=action;this.inactiveTooltip=inactiveTooltip;this.activeTooltip=activeTooltip;}boolean contains(double mouseX,double mouseY){return mouseX>=x&&mouseX<x+BUTTON_WIDTH&&mouseY>=y&&mouseY<y+BUTTON_HEIGHT;}boolean isSelected(){return selected.getAsBoolean();}Component tooltip(){return isSelected()?activeTooltip:inactiveTooltip;}void press(){action.run();}}
}

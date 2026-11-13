package ddstweak.feature.carpetrules;

import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//#if MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#else
//#if MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
//#endif
//#endif
//#if MC >= 12109
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
//#endif
//#if MC >= 260300
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Connection-local history: a verified temporary change can be reversed only
 * after fresh permission and current-value checks. Persistent ACKs are not rollback data. */
public final class CarpetRuleHistoryScreen extends Screen {
    private static final int MARGIN=14,PANEL_TOP=50,TOP=116,ROW=CarpetRuleListScroll.STRIDE,LIST_PAD=5;
    private static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    private final Screen parent;
    private final CarpetRuleMetadata metadata=CarpetLocalMetadata.discover();
    private String selectedId,feedback="",search="",filter="all";
    private final CarpetRuleListScroll listScroll=new CarpetRuleListScroll();
    private boolean narrowDetail,searchOpen;
    private EditBox searchBox;
    private Button restoreButton,allFilterButton,temporaryFilterButton,persistentFilterButton;

    public CarpetRuleHistoryScreen(Screen parent){
        super(MinecraftCompat.translatable("dds-tweak.carpet_rules.history.title"));
        this.parent=parent;
    }

    @Override public boolean isPauseScreen(){return false;}

    @Override protected void init(){
        addCompat(button(width-MARGIN-65,12,62,tr("dds-tweak.carpet_rules.back"),this::back));
        int tabW=Math.max(44,Math.min(86,(listWidth()-32)/3));
        allFilterButton=button(listLeft()+12,62,tabW,
                tr("dds-tweak.carpet_rules.history.filter.all"),()->setFilter("all"));
        temporaryFilterButton=button(listLeft()+16+tabW,62,tabW,
                tr("dds-tweak.carpet_rules.history.filter.temporary"),()->setFilter("temporary"));
        persistentFilterButton=button(listLeft()+20+2*tabW,62,tabW,
                tr("dds-tweak.carpet_rules.history.filter.persistent"),()->setFilter("persistent"));
        addFilterWidget(allFilterButton);
        addFilterWidget(temporaryFilterButton);
        addFilterWidget(persistentFilterButton);
        //#if MC >= 11600
        searchBox=new EditBox(font,searchInputX(),searchIconY(),searchInputWidth(),18,MinecraftCompat.literal(""));
        //#else
        //$$ searchBox=new EditBox(font,searchInputX(),searchIconY(),searchInputWidth(),18,"");
        //#endif
        searchBox.setMaxLength(120);
        searchBox.setValue(search);
        searchBox.setResponder(v->{search=v;listScroll.reset();selectedId=null;narrowDetail=false;});
        addCompat(searchBox);
        searchBox.visible=searchOpen;
        restoreButton=button(detailLeft()+12,height-43,Math.max(55,detailWidth()-24),
                tr("dds-tweak.carpet_rules.history.restore"),this::restore);
        // Only 1.16.5 has a verified wide-button texture split.
        // Other targets keep the native button, including working 1.14.4.
        //#if MC >= 11600 && MC < 11700
        addFilterWidget(restoreButton);
        //#else
        //$$ addCompat(restoreButton);
        //#endif
    }

    private Button button(int x,int y,int w,String name,Runnable action){
        //#if MC >= 11903
        Button painted=Button.builder(MinecraftCompat.literal(name),b->action.run()).bounds(x,y,w,20).build();
        //#elseif MC >= 11600
        //$$ Button painted=new Button(x,y,w,20,MinecraftCompat.literal(name),b->action.run());
        //#else
        //$$ Button painted=new Button(x,y,w,20,name,b->action.run());
        //#endif
        return painted;
    }


    /** Input-only widgets; selected filter styling is drawn once by this screen. */
    private void addFilterWidget(Button widget){
        //#if MC >= 11600
        addWidget(widget);
        //#else
        //$$ children.add(widget);
        //#endif
    }

    private <T extends AbstractWidget> void addCompat(T widget){
        //#if MC >= 11700
        addRenderableWidget(widget);
        //#else
        //$$ addButton(widget);
        //#endif
    }

    private void back(){
        if(narrow()&&narrowDetail){narrowDetail=false;return;}
        onClose();
    }

    private void setFilter(String value){
        filter=value;
        listScroll.reset();
        selectedId=null;
        narrowDetail=false;
        feedback="";
    }

    private String displayName(CarpetRuleOperationTracker.Operation op){
        CarpetRuleMetadata.Description info=metadata.find(op.key());
        return info==null?op.key().name():info.displayName();
    }

    private String time(long millis){
        return CLOCK.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    private String mode(CarpetRuleCommands.Mode mode){
        return tr("dds-tweak.carpet_rules.history.mode."+mode.name().toLowerCase(Locale.ROOT));
    }

    private String status(CarpetRuleOperationTracker.Operation op){
        return tr("dds-tweak.carpet_rules.status."+op.status().name().toLowerCase(Locale.ROOT));
    }

    private int statusColor(CarpetRuleOperationTracker.Operation op){
        switch(op.status()){
            case PERSISTED:
            case VERIFIED:return 0xFF8BD8AC;
            case PERSISTENCE_MISMATCH:
            case REJECTED:return 0xFFFF9A8F;
            case WAITING_FOR_PERSISTENCE:
            case PERSISTENCE_UNCONFIRMED:return 0xFFE5C991;
            default:return 0xFFB1C1C4;
        }
    }

    private List<CarpetRuleOperationTracker.Operation> entries(){
        CarpetRuleOperationTracker tracker=CarpetRuleClientNetwork.operations();
        tracker.expire(System.currentTimeMillis());
        List<CarpetRuleOperationTracker.Operation> all=new ArrayList<>(tracker.history());
        Collections.reverse(all);
        List<CarpetRuleOperationTracker.Operation> filtered=new ArrayList<>();
        String query=search.trim().toLowerCase(Locale.ROOT);
        for(CarpetRuleOperationTracker.Operation op:all){
            if("temporary".equals(filter)&&op.mode()!=CarpetRuleCommands.Mode.TEMPORARY)continue;
            if("persistent".equals(filter)&&op.mode()==CarpetRuleCommands.Mode.TEMPORARY)continue;
            String haystack=op.key().toString()+" "+displayName(op);
            if(query.isEmpty()||haystack.toLowerCase(Locale.ROOT).contains(query))
                filtered.add(op);
        }
        return filtered;
    }

    private CarpetRuleOperationTracker.Operation selected(){
        return selectedId==null?null:CarpetRuleClientNetwork.operations().find(selectedId);
    }

    private String restoreEligibility(CarpetRuleRestorePlan.Status status){
        return tr("dds-tweak.carpet_rules.history.eligibility."+
                status.name().toLowerCase(Locale.ROOT));
    }

    private boolean canRestore(CarpetRuleOperationTracker.Operation op){
        return CarpetRuleRestorePlan.validate(CarpetRuleClientNetwork.catalog(),
                CarpetRuleClientNetwork.operations(),op)==CarpetRuleRestorePlan.Status.READY
                &&CarpetRuleClientNetwork.canSubmit(op.key());
    }

    private void restore(){
        CarpetRuleCatalog catalog=CarpetRuleClientNetwork.catalog();
        CarpetRuleOperationTracker.Operation op=selected();
        CarpetRuleRestorePlan.Status status=CarpetRuleRestorePlan.validate(
                catalog,CarpetRuleClientNetwork.operations(),op);
        if(status!=CarpetRuleRestorePlan.Status.READY){
            feedback=tr("dds-tweak.carpet_rules.history.blocked",
                    restoreEligibility(status));
            return;
        }
        long generation=catalog.generation();
        if(!CarpetRuleClientNetwork.canSubmit(op.key())){
            feedback=tr("dds-tweak.carpet_rules.history.permission");
            return;
        }
        if(generation!=catalog.generation()
                ||CarpetRuleRestorePlan.validate(catalog,
                        CarpetRuleClientNetwork.operations(),op)
                        !=CarpetRuleRestorePlan.Status.READY){
            feedback=tr("dds-tweak.carpet_rules.history.recheck");
            return;
        }
        CarpetRuleOperationTracker.Operation sent=CarpetRuleClientNetwork.submit(
                op.key(),CarpetRuleCommands.Mode.TEMPORARY,op.original());
        feedback=sent==null?tr("dds-tweak.carpet_rules.history.permission"):
                tr("dds-tweak.carpet_rules.history.sent");
    }

    private int listLeft(){return MARGIN;}
    private int listWidth(){return width<580?width-2*MARGIN:Math.max(160,(width-3*MARGIN)*3/5);}
    private int detailLeft(){return width<580?MARGIN:listLeft()+listWidth()+MARGIN;}
    private int detailWidth(){return width-MARGIN-detailLeft();}
    private boolean narrow(){return width<580;}
    private int searchIconX(){return listLeft()+12;}
    private int searchIconY(){return TOP-27;}
    private int searchInputX(){return searchIconX()+28;}
    private int searchInputWidth(){return Math.max(36,listWidth()-(searchInputX()-listLeft())-16);}
    private int listTop(){return TOP+LIST_PAD;}
    private int listHeight(){return Math.max(ROW,height-listTop()-25);}
    private int rows(){return Math.max(1,listHeight()/ROW);}

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float pt){
    //$$     if(minecraft.level!=null)extractBlurredBackground(g);
    //$$ }
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){renderBase(g);super.extractRenderState(g,mx,my,pt);renderDetails(g,mx,my);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float pt){
        //#if MC >= 12005
        // The MaLiLib-style backdrop is drawn by the shared Carpet screen style.
        //#else
        //$$ // No vanilla background; the shared MaLiLib-style mask owns this layer.
        //#endif
    }
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderCarpetBlur(g,pt);
        renderBase(g);super.render(g,mx,my,pt);renderDetails(g,mx,my);
    }
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics g,int mx,int my,float pt){renderBase(g);super.render(g,mx,my,pt);renderDetails(g,mx,my);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack g,int mx,int my,float pt){renderBase(g);super.render(g,mx,my,pt);renderDetails(g,mx,my);}
    //#else
    //$$ @Override public void render(int mx,int my,float pt){renderBase(null);super.render(mx,my,pt);renderDetails(null,mx,my);}
    //#endif

    //#if MC >= 12002 && MC < 260100
    /** Request one native blur pass before rendering the screen's own panels. */
    private void renderCarpetBlur(GuiGraphics g,float partialTick){
        if(minecraft.level==null)return;
        //#if MC >= 12106
        renderBlurredBackground(g);
        //#elseif MC >= 12102
        //$$ renderBlurredBackground();
        //#elseif MC >= 12005
        //$$ renderBlurredBackground(partialTick);
        //#endif
    }
    //#endif

    private void renderBase(
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
        CarpetRuleUiStyle.background(g,width,height);
        if(!narrowDetail)
            CarpetRuleUiStyle.rounded(g,listLeft(),PANEL_TOP,
                    listLeft()+listWidth(),height-14,9,CarpetRuleUiStyle.PANEL);
        if(!narrow()||narrowDetail)
            CarpetRuleUiStyle.rounded(g,detailLeft(),PANEL_TOP,
                    detailLeft()+detailWidth(),height-14,9,CarpetRuleUiStyle.PANEL);
        draw(g,tr("dds-tweak.carpet_rules.history.title"),MARGIN,17,0xFFFFFFFF);
        draw(g,tr("dds-tweak.carpet_rules.history.scope"),MARGIN,32,0xFFABB9BF);
        if(searchBox!=null)searchBox.visible=searchOpen&&!narrowDetail;
        CarpetRuleOperationTracker.Operation op=selected();
        restoreButton.visible=(!narrow()||narrowDetail)&&op!=null;
        restoreButton.active=restoreButton.visible&&canRestore(op);
    }

    private void renderFilterTabs(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int mx,int my){
        int tabW=Math.max(44,Math.min(86,(listWidth()-32)/3));
        String[] values={"all","temporary","persistent"};
        for(int i=0;i<3;i++){
            int x=listLeft()+12+i*(tabW+4),y=62;
            boolean hovered=mx>=x&&mx<x+tabW&&my>=y&&my<y+20;
            CarpetRuleUiStyle.tabButton(g,font,
                    tr("dds-tweak.carpet_rules.history.filter."+values[i]),
                    x,y,tabW,20,values[i].equals(filter),hovered);
        }
    }

    private void renderDetails(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g
            //#elseif MC >= 12000
            GuiGraphics g
            //#elseif MC >= 11600
            //$$ PoseStack g
            //#else
            //$$ Object g
            //#endif
    ,int mx,int my){
        List<CarpetRuleOperationTracker.Operation> all=entries();
        listScroll.animate(all.size(),listHeight());
        int left=listLeft(),w=listWidth(),top=listTop();
        CarpetRuleCatalog.RemoteRule hoveredRule=null;
        if(!narrowDetail){
            renderFilterTabs(g,mx,my);
            CarpetRuleUiStyle.searchIcon(g,searchIconX(),searchIconY(),20,
                    mx>=searchIconX()&&mx<searchIconX()+20
                            &&my>=searchIconY()&&my<searchIconY()+20,searchOpen);
            ScreenRenderCompat.fill(g,left+12,TOP-5,left+w-12,TOP-4,0x556B7883);
            ScreenRenderCompat.beginScissor(g,left+4,top,left+w-7,top+listHeight());
            int first=Math.max(0,(int)(listScroll.position()/ROW));
            int last=Math.min(all.size(),first+rows()+3);
            for(int i=first;i<last;i++){
                CarpetRuleOperationTracker.Operation op=all.get(i);
                int y=listScroll.top(top,i);
                if(y+CarpetRuleListScroll.CARD_HEIGHT<top||y>=top+listHeight())continue;
                boolean hovered=mx>=left+9&&mx<left+w-9
                        &&my>=y&&my<y+CarpetRuleListScroll.CARD_HEIGHT;
                if(hovered)hoveredRule=CarpetRuleClientNetwork.catalog().get(op.key());
                CarpetRuleUiStyle.ruleCardBackground(g,left+9,y,left+w-9,
                        CarpetRuleListScroll.CARD_HEIGHT,op.id().equals(selectedId),hovered);
                draw(g,fit(displayName(op)+"  "+op.key().name(),w-36),
                        left+17,y+5,0xFFF1F6F4);
                String summary=time(op.sentAt())+" · "+mode(op.mode())+" · "+status(op);
                draw(g,fit(summary,w-36),left+17,y+20,
                        op.status()==CarpetRuleOperationTracker.Status.VERIFIED
                                ?0xFF8BD8AC:0xFFB4C1C6);
            }
            ScreenRenderCompat.endScissor(g);
            CarpetRuleUiStyle.listScrollbar(g,left+w-6,top,listHeight(),
                    all.size(),listScroll);
            if(all.isEmpty())
                draw(g,tr("dds-tweak.carpet_rules.history.empty"),
                        left+12,TOP+6,0xFFABB9BF);
        }
        if(!narrow()||narrowDetail){
            int x=detailLeft(),dw=detailWidth();
            CarpetRuleOperationTracker.Operation op=selected();
            if(op==null)draw(g,tr("dds-tweak.carpet_rules.history.select"),
                    x+12,PANEL_TOP+14,0xFFAEB9BD);
            else{
                ScreenRenderCompat.scaledText(g,font,fit(displayName(op),(int)((dw-24)/1.16F)),
                        x+12,PANEL_TOP+14,0xFFF8FCFF,1.16F);
                draw(g,fit(op.key().name(),dw-24),x+12,PANEL_TOP+34,0xFFBBC7CC);
                draw(g,fit(time(op.sentAt())+" · "+mode(op.mode()),dw-24),
                        x+12,PANEL_TOP+55,0xFFB4C1C6);
                CarpetRuleUiStyle.valuePair(g,font,x,PANEL_TOP+83,dw,
                        tr("dds-tweak.carpet_rules.history.before",""),op.original(),
                        tr("dds-tweak.carpet_rules.target"),
                        op.target()==null?"—":op.target());
                // Status already appears on each history row; avoid a redundant heading.
                int statusY=PANEL_TOP+137,eligibilityY=height-65;
                if(op.mode()!=CarpetRuleCommands.Mode.TEMPORARY
                        &&statusY<eligibilityY-14){
                    draw(g,fit(status(op),dw-24),x+12,statusY,statusColor(op));
                    if(op.status()==CarpetRuleOperationTracker.Status.PERSISTENCE_MISMATCH
                            &&statusY+14<eligibilityY-14)
                        draw(g,fit(tr("dds-tweak.carpet_rules.history.persistence_value",
                                op.persistedValue()==null?"—":op.persistedValue()),dw-24),
                                x+12,statusY+14,0xFFB1C1C4);
                }
                CarpetRuleRestorePlan.Status eligibility=CarpetRuleRestorePlan.validate(
                        CarpetRuleClientNetwork.catalog(),CarpetRuleClientNetwork.operations(),op);
                String qualified=eligibility==CarpetRuleRestorePlan.Status.READY
                        &&!CarpetRuleClientNetwork.canSubmit(op.key())
                        ?tr("dds-tweak.carpet_rules.history.permission")
                        :restoreEligibility(eligibility);
                draw(g,fit(tr("dds-tweak.carpet_rules.history.eligibility",
                        qualified),dw-24),x+12,eligibilityY,0xFFB1C1C4);
            }
        }
        //#if MC >= 11600 && MC < 11700
        if(restoreButton.visible){
            int bx=detailLeft()+12,by=height-43,bw=Math.max(55,detailWidth()-24);
            boolean hover=mx>=bx&&mx<bx+bw&&my>=by&&my<by+20;
            CarpetRuleUiStyle.toolbarButton(g,font,
                    tr("dds-tweak.carpet_rules.history.restore"),
                    bx,by,bw,20,hover,restoreButton.active,false);
        }
        //#endif
        if(!feedback.isEmpty()){
            int x=(!narrow()||narrowDetail)?detailLeft()+12:listLeft()+12;
            int feedbackWidth=(!narrow()||narrowDetail)?detailWidth()-24:listWidth()-24;
            draw(g,fit(feedback,feedbackWidth),x,height-55,0xFFFFD89B);
        }
        // A synchronous tooltip must be the last overlay on older Minecraft clients.
        if(hoveredRule!=null)renderRuleTooltip(g,hoveredRule,mx,my);
    }

    private void renderRuleTooltip(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            CarpetRuleCatalog.RemoteRule rule,int mx,int my){
        List<String> lines=CarpetRuleUiTooltip.lines(font,rule,metadata,false,width,height);
        if(lines.isEmpty())return;
        List<Component> tooltip=new ArrayList<>();
        for(String line:lines)tooltip.add(MinecraftCompat.literal(line));
        //#if MC >= 12106
        g.setComponentTooltipForNextFrame(font,tooltip,mx,my);
        //#elseif MC >= 12000
        //$$ g.renderComponentTooltip(font,tooltip,mx,my);
        //#elseif MC >= 11600
        //$$ renderComponentTooltip(g,tooltip,mx,my);
        //#else
        //$$ renderTooltip(lines,mx,my);
        //#endif
    }

    private void draw(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            String value,int x,int y,int color){
        ScreenRenderCompat.text(g,font,value,x,y,color,true);
    }

    private String fit(String text,int limit){
        if(text==null||limit<=0)return "";
        if(font.width(text)<=limit)return text;
        int n=text.length();
        while(n>0&&font.width(text.substring(0,n)+"…")>limit)n--;
        return text.substring(0,n)+"…";
    }

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(super.mouseClicked(event,doubleClick)){
            if(getFocused() instanceof Button)setFocused(null);
            return true;
        }
        return select(event.x(),event.y(),event.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){
    //$$     if(super.mouseClicked(x,y,button)){
    //$$         if(getFocused() instanceof Button)setFocused(null);
    //$$         return true;
    //$$     }
    //$$     return select(x,y,button);
    //$$ }
    //#endif

    private boolean select(double x,double y,int button){
        //#if MC >= 260300
        //$$ if(button!=InputConstants.MOUSE_BUTTON_LEFT)return false;
        //#else
        if(button!=0)return false;
        //#endif
        if(!narrowDetail&&x>=searchIconX()&&x<searchIconX()+20
                &&y>=searchIconY()&&y<searchIconY()+20){
            searchOpen=!searchOpen;
            if(!searchOpen){
                searchBox.setValue("");
                setFocused(null);
            }else setFocused(searchBox);
            return true;
        }
        if(x<MARGIN||x>=MARGIN+listWidth()||y<listTop()||y>=listTop()+listHeight())return false;
        List<CarpetRuleOperationTracker.Operation> all=entries();
        if(!narrowDetail&&CarpetRuleListScroll.max(all.size(),listHeight())>0
                &&x>=listLeft()+listWidth()-9){
            listScroll.scrollToFraction((y-listTop())/(double)listHeight(),
                    all.size(),listHeight());
            return true;
        }
        int index=listScroll.hit(y,listTop(),all.size(),listHeight());
        if(index<0||index>=all.size())return false;
        selectedId=all.get(index).id();
        if(narrow())narrowDetail=true;
        feedback="";
        return true;
    }

    //#if MC >= 12002
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(scroll(x,y,vertical))return true;
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    //#else
    //$$ @Override public boolean mouseScrolled(double x,double y,double vertical){
    //$$     if(scroll(x,y,vertical))return true;
    //$$     return super.mouseScrolled(x,y,vertical);
    //$$ }
    //#endif

    private boolean scroll(double x,double y,double delta){
        if(narrowDetail||delta==0||x<MARGIN||x>MARGIN+listWidth()
                ||y<listTop()||y>listTop()+listHeight())return false;
        listScroll.wheel(delta,entries().size(),listHeight());
        return true;
    }

    //#if MC >= 12109
    @Override public boolean keyPressed(KeyEvent event){
        if(event.isEscape()&&narrowDetail){narrowDetail=false;return true;}
        return super.keyPressed(event);
    }
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
    //$$     if(keyCode==256&&narrowDetail){narrowDetail=false;return true;}
    //$$     return super.keyPressed(keyCode,scanCode,modifiers);
    //$$ }
    //#endif

    @Override public void onClose(){MinecraftCompat.setScreen(minecraft,parent);}
    private static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}
}

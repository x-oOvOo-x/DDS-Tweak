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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Local-only membership editor; it never writes Carpet server rules. */
public final class CarpetRuleGroupAssignScreen extends Screen {
    private static final int MARGIN=14,TOP=88,ROW=26;
    private final Screen parent;
    private final CarpetRulePreferences preferences;
    private final CarpetRuleCatalog.RuleKey selectedRule;
    private final String selectedGroup;
    private final long generation;
    private final CarpetRuleMetadata metadata=CarpetLocalMetadata.discover();
    private EditBox searchBox;
    private String query="",feedback="";
    private int offset;
    private final CarpetRuleListScroll cardScroll=new CarpetRuleListScroll();

    /**
     * Passing a group opens all server-synchronized rules (including empty groups).
     * Passing a rule opens saved local groups; exactly one mode must be selected.
     */
    public CarpetRuleGroupAssignScreen(Screen parent,CarpetRulePreferences preferences,
            CarpetRuleCatalog.RuleKey selectedRule,String selectedGroup){
        super(MinecraftCompat.translatable("dds-tweak.carpet_rules.groups.assign"));
        if((selectedRule==null)==(selectedGroup==null||selectedGroup.isEmpty()))
            throw new IllegalArgumentException("Choose one rule or one group");
        this.parent=parent;
        this.preferences=preferences;
        this.selectedRule=selectedRule;
        this.selectedGroup=selectedGroup;
        this.generation=CarpetRuleClientNetwork.catalog().generation();
    }

    private boolean choosingRules(){return selectedGroup!=null;}
    private boolean active(){
        if(generation!=CarpetRuleClientNetwork.catalog().generation()
                ||!CarpetRuleClientNetwork.catalog().isReady())return false;
        if(choosingRules())return preferences.groupNames().contains(selectedGroup);
        return CarpetRuleClientNetwork.catalog().get(selectedRule)!=null;
    }

    @Override public boolean isPauseScreen(){return false;}

    @Override protected void init(){
        //#if MC >= 11600
        searchBox=new EditBox(font,MARGIN+24,48,Math.max(56,width-2*MARGIN-24),18,MinecraftCompat.literal(""));
        //#else
        //$$ searchBox=new EditBox(font,MARGIN+24,48,Math.max(56,width-2*MARGIN-24),18,"");
        //#endif
        searchBox.setMaxLength(120);
        searchBox.setValue(query);
        searchBox.setResponder(value->{query=value;offset=0;cardScroll.reset();});
        searchBox.visible=choosingRules();
        addCompat(searchBox);
        addCompat(button(width-MARGIN-68,13,68,tr("dds-tweak.carpet_rules.back"),this::onClose));
    }

    private Button button(int x,int y,int w,String label,Runnable action){
        //#if MC >= 11903
        Button painted=Button.builder(MinecraftCompat.literal(label),b->action.run()).bounds(x,y,w,20).build();
        //#elseif MC >= 11600
        //$$ Button painted=new Button(x,y,w,20,MinecraftCompat.literal(label),b->action.run());
        //#else
        //$$ Button painted=new Button(x,y,w,20,label,b->action.run());
        //#endif
        return painted;
    }


    private <T extends AbstractWidget> void addCompat(T widget){
        //#if MC >= 11700
        addRenderableWidget(widget);
        //#else
        //$$ addButton(widget);
        //#endif
    }

    private List<String> groups(){return new ArrayList<>(preferences.groupNames());}

    private List<CarpetRuleCatalog.RemoteRule> rules(){
        List<CarpetRuleCatalog.RemoteRule> out=new ArrayList<>();
        String needle=query.trim().toLowerCase(Locale.ROOT);
        for(CarpetRuleCatalog.RemoteRule rule:CarpetRuleClientNetwork.catalog().snapshot()){
            CarpetRuleMetadata.Description detail=metadata.find(rule.key());
            String id=rule.key().toString().toLowerCase(Locale.ROOT);
            boolean match=needle.isEmpty()||id.contains(needle)
                    ||rule.key().name().toLowerCase(Locale.ROOT).contains(needle);
            if(!match&&detail!=null){
                match=detail.displayName().toLowerCase(Locale.ROOT).contains(needle)
                        ||detail.detail().toLowerCase(Locale.ROOT).contains(needle);
                if(!match)for(String category:detail.categories())
                    if(category.toLowerCase(Locale.ROOT).contains(needle)){match=true;break;}
            }
            if(match)out.add(rule);
        }
        out.sort(Comparator.comparing((CarpetRuleCatalog.RemoteRule r)->r.key().manager())
                .thenComparing(r->r.key().name()));
        return out;
    }

    private int stride(){return choosingRules()?CarpetRuleListScroll.STRIDE:ROW;}
    private int viewportHeight(){return Math.max(1,height-TOP-38);}
    private int rows(){return Math.max(1,viewportHeight()/stride());}
    private int size(){return choosingRules()?rules().size():groups().size();}

    private void toggle(int index){
        if(!active())return;
        String group;
        CarpetRuleCatalog.RuleKey rule;
        if(choosingRules()){
            List<CarpetRuleCatalog.RemoteRule> all=rules();
            if(index>=all.size())return;
            group=selectedGroup;
            rule=all.get(index).key();
        }else{
            List<String> all=groups();
            if(index>=all.size())return;
            group=all.get(index);
            rule=selectedRule;
        }
        boolean include=!preferences.inGroup(group,rule);
        if(!preferences.setGroupMembership(group,rule,include))return;
        feedback=tr(preferences.save()?"dds-tweak.carpet_rules.groups.saved":
                "dds-tweak.carpet_rules.presets.save_failed");
    }

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float pt){super.extractBackground(g,mx,my,pt);}
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){renderBase(g);renderDetails(g,mx,my);super.extractRenderState(g,mx,my,pt);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float pt){
        //#if MC >= 12005
        super.renderBackground(g,mx,my,pt);
        //#else
        //$$ renderTransparentBackground(g);
        //#endif
    }
    @Override public void render(GuiGraphics g,int mx,int my,float pt){renderBase(g);renderDetails(g,mx,my);super.render(g,mx,my,pt);}
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics g,int mx,int my,float pt){renderBackground(g);renderBase(g);renderDetails(g,mx,my);super.render(g,mx,my,pt);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack g,int mx,int my,float pt){renderBackground(g);renderBase(g);renderDetails(g,mx,my);super.render(g,mx,my,pt);}
    //#else
    //$$ @Override public void render(int mx,int my,float pt){renderBackground();renderBase(null);renderDetails(null,mx,my);super.render(mx,my,pt);}
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
        CarpetRuleUiStyle.rounded(g,MARGIN,TOP-10,width-MARGIN,height-29,9,CarpetRuleUiStyle.PANEL);
        text(g,tr("dds-tweak.carpet_rules.groups.assign"),MARGIN,17,0xFFFFFFFF);
        text(g,tr(choosingRules()?"dds-tweak.carpet_rules.groups.picking_rules":
                "dds-tweak.carpet_rules.groups.picking_groups",
                choosingRules()?selectedGroup:CarpetRuleSidebar.displayRuleId(selectedRule)),
                MARGIN,35,0xFFBCC5D3);
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
            ,int mx,int my
    ){
        if(choosingRules())CarpetRuleUiStyle.searchIcon(g,MARGIN,48,18,false,false);
        if(!active()){
            text(g,tr("dds-tweak.carpet_rules.groups.stale"),MARGIN+8,TOP+8,0xFFFFC6A0);
            return;
        }
        List<CarpetRuleCatalog.RemoteRule> availableRules=choosingRules()?rules():java.util.Collections.emptyList();
        List<String> availableGroups=choosingRules()?java.util.Collections.emptyList():groups();
        int count=choosingRules()?availableRules.size():availableGroups.size(),capacity=rows();
        if(choosingRules())cardScroll.animate(count,viewportHeight());
        else offset=Math.min(offset,Math.max(0,count-capacity));
        int first=choosingRules()?Math.max(0,(int)(cardScroll.position()/stride())):offset;
        CarpetRuleCatalog.RemoteRule hoveredRule=null;
        // Match the existing endScissor and preserve the selected accent inside the panel.
        ScreenRenderCompat.beginScissor(g,MARGIN,TOP,width-MARGIN,height-36);
        for(int i=0;i<capacity+2&&first+i<count;i++){
            int index=first+i;
            int y=choosingRules()?cardScroll.top(TOP,index):TOP+i*ROW;
            if(y>=height-36||y+stride()<=TOP)continue;
            String label,group;
            CarpetRuleCatalog.RuleKey rule;
            if(choosingRules()){
                CarpetRuleCatalog.RemoteRule entry=availableRules.get(index);
                rule=entry.key();
                CarpetRuleMetadata.Description local=metadata.find(rule);
                label=local!=null&&!local.displayName().equals(rule.name())
                        ?local.displayName():CarpetRuleSidebar.displayRuleId(rule);
                group=selectedGroup;
            }else{
                group=availableGroups.get(index);
                rule=selectedRule;
                label=group;
            }
            boolean included=preferences.inGroup(group,rule);
            int cardHeight=choosingRules()?CarpetRuleListScroll.CARD_HEIGHT:ROW-2;
            boolean hover=mx>=MARGIN+4&&mx<width-MARGIN-4&&my>=y&&my<y+cardHeight;
            if(choosingRules()){
                CarpetRuleCatalog.RemoteRule current=availableRules.get(index);
                CarpetRuleMetadata.Description local=metadata.find(current.key());
                String name=local==null?current.key().name():local.displayName();
                String info=CarpetRuleSidebar.cardMetadata(
                        CarpetRuleClientNetwork.integratedOwner(current.key()),
                        local==null?java.util.Collections.emptySet():local.categories());
                CarpetRuleUiStyle.ruleCardBackground(g,MARGIN+4,y,width-MARGIN-4,
                        cardHeight,included,hover);
                if(hover)hoveredRule=current;
                CarpetRuleUiStyle.ruleCardInfo(g,font,MARGIN+12,y,
                        Math.max(0,width-2*MARGIN-60),name,current.key().name(),info);
            }else{
                CarpetRuleUiStyle.tabBackground(g,MARGIN+4,y,width-MARGIN-4,cardHeight,5,
                        included,hover,CarpetRuleUiStyle.ROW);
                text(g,fit(label,width-2*MARGIN-60),MARGIN+12,y+8,0xFFF1F3F5);
            }
            text(g,included?"✓":"+",
                    width-MARGIN-28,y+8,included?0xFF82DE9D:0xFFE2E5E9);
        }
        ScreenRenderCompat.endScissor(g);
        if(hoveredRule!=null)renderRuleTooltip(g,hoveredRule,mx,my);
        if(choosingRules())CarpetRuleUiStyle.listScrollbar(g,width-MARGIN-6,TOP,
                viewportHeight(),count,cardScroll);
        if(count==0)
            text(g,tr(choosingRules()?"dds-tweak.carpet_rules.groups.no_rules":
                    "dds-tweak.carpet_rules.groups.no_groups"),MARGIN+10,TOP+12,0xFFBFC5CC);
        if(!feedback.isEmpty())
            text(g,fit(feedback,width-2*MARGIN),MARGIN,height-22,0xFFFFDFA3);
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

    private String fit(String s,int maxWidth){
        int n=s.length();
        while(n>0&&font.width(s.substring(0,n)+"…")>maxWidth)n--;
        return font.width(s)<=maxWidth?s:s.substring(0,n)+"…";
    }

    private void text(
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

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(super.mouseClicked(event,doubleClick)){
            if(getFocused() instanceof Button)setFocused(null);
            return true;
        }
        return click(event.x(),event.y(),event.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){
    //$$     if(super.mouseClicked(x,y,button)){
    //$$         if(getFocused() instanceof Button)setFocused(null);
    //$$         return true;
    //$$     }
    //$$     return click(x,y,button);
    //$$ }
    //#endif

    private boolean click(double x,double y,int button){
        //#if MC >= 260300
        //$$ if(button!=InputConstants.MOUSE_BUTTON_LEFT)return false;
        //#else
        if(button!=0)return false;
        //#endif
        if(x<MARGIN+4||x>=width-MARGIN-4||y<TOP||y>=height-36)return false;
        if(choosingRules()&&CarpetRuleListScroll.max(size(),viewportHeight())>0
                &&x>=width-MARGIN-9){
            cardScroll.scrollToFraction((y-TOP)/(double)viewportHeight(),
                    size(),viewportHeight());
            return true;
        }
        int index=choosingRules()
                ?cardScroll.hit(y,TOP,size(),viewportHeight())
                :offset+(int)(y-TOP)/ROW;
        if(index<0||index>=size())return false;
        if(!choosingRules()&&(int)(y-TOP)%ROW>=ROW-2)return false;
        toggle(index);
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
        if(delta==0||x<MARGIN||x>=width-MARGIN||y<TOP||y>=height-36)return false;
        if(choosingRules())cardScroll.wheel(delta,size(),viewportHeight());
        else offset=Math.max(0,Math.min(Math.max(0,size()-rows()),offset+(delta<0?2:-2)));
        return true;
    }

    @Override public void onClose(){MinecraftCompat.setScreen(minecraft,parent);}
    private static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}
}

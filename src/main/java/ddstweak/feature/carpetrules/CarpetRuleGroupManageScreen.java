package ddstweak.feature.carpetrules;

import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
//#if MC >= 12109
import net.minecraft.client.input.MouseButtonEvent;
//#endif
//#if MC >= 260300
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif
import java.util.ArrayList;
import java.util.List;

/** Focused, local-only group management instead of the obsolete full-screen edit mode. */
final class CarpetRuleGroupManageScreen extends Screen {
    private static final int MARGIN=22,TOP=82,ROW=25;
    private final Screen parent;
    private final CarpetRulePreferences preferences;
    private final long generation;
    private EditBox nameBox;
    private String selected,deletePending="",feedback="";
    private int listOffset;

    CarpetRuleGroupManageScreen(Screen parent,CarpetRulePreferences preferences,String initial){
        super(MinecraftCompat.translatable("dds-tweak.carpet_rules.groups.manage"));
        this.parent=parent;
        this.preferences=preferences;
        this.selected=initial!=null&&preferences.groupNames().contains(initial)?initial:null;
        this.generation=CarpetRuleClientNetwork.catalog().generation();
    }

    @Override public boolean isPauseScreen(){return false;}
    private int panelWidth(){return Math.max(290,Math.min(600,width-2*MARGIN));}
    private int left(){return (width-panelWidth())/2;}
    private int split(){return left()+Math.max(100,panelWidth()/3);}
    private int contentTop(){return TOP+34;}
    private int listHeight(){return Math.max(48,height-contentTop()-62);}
    private int rows(){return Math.max(1,listHeight()/ROW);}
    private List<String> names(){return new ArrayList<>(preferences.groupNames());}

    @Override protected void init(){
        int right=split()+12,fieldWidth=Math.max(74,left()+panelWidth()-right-12);
        //#if MC >= 11600
        nameBox=new EditBox(font,right,contentTop()+12,fieldWidth,20,MinecraftCompat.literal(""));
        //#else
        //$$ nameBox=new EditBox(font,right,contentTop()+12,fieldWidth,20,"");
        //#endif
        nameBox.setMaxLength(64);
        nameBox.setValue(selected==null?"":selected);
        addCompat(nameBox);
        int half=Math.max(36,(fieldWidth-5)/2);
        addCompat(button(right,contentTop()+44,half,
                tr("dds-tweak.carpet_rules.groups.create"),this::create));
        addCompat(button(right+half+5,contentTop()+44,half,
                tr("dds-tweak.carpet_rules.groups.rename"),this::rename));
        addCompat(button(right,contentTop()+71,half,
                tr("dds-tweak.carpet_rules.groups.delete"),this::delete));
        addCompat(button(right+half+5,contentTop()+71,half,
                tr("dds-tweak.carpet_rules.groups.members"),this::editMembers));
        addCompat(button(left()+panelWidth()-80,height-37,72,
                tr("dds-tweak.carpet_rules.back"),this::onClose));
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

    private void create(){
        String name=nameBox.getValue().trim();
        if(!preferences.createGroup(name)){
            feedback=tr("dds-tweak.carpet_rules.groups.invalid");return;
        }
        selected=name;deletePending="";
        saved();
    }

    private void rename(){
        String name=nameBox.getValue().trim();
        if(selected==null||!preferences.renameGroup(selected,name)){
            feedback=tr("dds-tweak.carpet_rules.groups.invalid");return;
        }
        selected=name;deletePending="";
        saved();
    }

    private void delete(){
        if(selected==null)return;
        if(!selected.equals(deletePending)){
            deletePending=selected;
            feedback=tr("dds-tweak.carpet_rules.groups.confirm");return;
        }
        preferences.removeGroup(selected);
        selected=null;deletePending="";nameBox.setValue("");
        saved();
    }

    private void editMembers(){
        if(selected==null){feedback=tr("dds-tweak.carpet_rules.groups.select_group");return;}
        if(generation!=CarpetRuleClientNetwork.catalog().generation()){
            feedback=tr("dds-tweak.carpet_rules.groups.stale");return;
        }
        MinecraftCompat.setScreen(minecraft,
                new CarpetRuleGroupAssignScreen(this,preferences,null,selected));
    }

    private void saved(){
        feedback=tr(preferences.save()?"dds-tweak.carpet_rules.groups.saved":
                "dds-tweak.carpet_rules.presets.save_failed");
    }

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float pt){super.extractBackground(g,x,y,pt);}
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float pt){renderPanel(g,x,y);super.extractRenderState(g,x,y,pt);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics g,int x,int y,float pt){
        //#if MC >= 12005
        super.renderBackground(g,x,y,pt);
        //#else
        //$$ renderTransparentBackground(g);
        //#endif
    }
    @Override public void render(GuiGraphics g,int x,int y,float pt){
        renderPanel(g,x,y);super.render(g,x,y,pt);
    }
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics g,int x,int y,float pt){renderBackground(g);renderPanel(g,x,y);super.render(g,x,y,pt);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack g,int x,int y,float pt){renderBackground(g);renderPanel(g,x,y);super.render(g,x,y,pt);}
    //#else
    //$$ @Override public void render(int x,int y,float pt){renderBackground();renderPanel(null,x,y);super.render(x,y,pt);}
    //#endif

    private void renderPanel(
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
        CarpetRuleUiStyle.background(g,width,height);
        int l=left(),r=l+panelWidth();
        CarpetRuleUiStyle.rounded(g,l,TOP,r,height-45,9,CarpetRuleUiStyle.PANEL);
        CarpetRuleUiStyle.rounded(g,l+9,contentTop()-3,split()-6,height-51,6,0x66313940);
        CarpetRuleUiStyle.rounded(g,split()+3,contentTop()-3,r-9,height-51,6,0x66313940);
        text(g,tr("dds-tweak.carpet_rules.groups.manage"),l+13,TOP+11,0xFFF3F4F5);
        text(g,tr("dds-tweak.carpet_rules.groups.list"),l+14,TOP+27,0xFFBDC7C9);
        List<String> groups=names();
        listOffset=Math.min(listOffset,Math.max(0,groups.size()-rows()));
        ScreenRenderCompat.beginScissor(g,l+7,contentTop(),split()-8,height-52);
        for(int i=0;i<rows()&&listOffset+i<groups.size();i++){
            String name=groups.get(listOffset+i);
            int y=contentTop()+i*ROW;
            boolean hover=mx>=l+12&&mx<split()-11&&my>=y&&my<y+ROW-2;
            CarpetRuleUiStyle.tabBackground(g,l+12,y,split()-11,ROW-3,5,
                    name.equals(selected),hover,CarpetRuleUiStyle.ROW);
            text(g,fit(name,Math.max(0,split()-l-58)),l+20,y+7,0xFFF1F3F3);
        }
        ScreenRenderCompat.endScissor(g);
        text(g,tr("dds-tweak.carpet_rules.groups.name_label"),
                split()+12,contentTop()-13,0xFFCAD2D1);
        text(g,tr("dds-tweak.carpet_rules.groups.count",selected==null?0:countMembers(selected)),
                split()+12,contentTop()+113,0xFFBBC5C4);
        text(g,tr("dds-tweak.carpet_rules.groups.local_only"),
                split()+12,contentTop()+136,0xFF889D98);
        if(!feedback.isEmpty())text(g,feedback,l+14,height-64,0xFFFFDA9A);
    }

    /** Use glyph widths instead of character count for Chinese and Latin names. */
    private String fit(String value,int width){
        if(value==null||width<=0)return "";
        if(font.width(value)<=width)return value;
        int n=value.length();
        while(n>0&&font.width(value.substring(0,n)+"…")>width)n--;
        return value.substring(0,n)+"…";
    }

    private int countMembers(String name){
        int n=0;
        for(CarpetRuleCatalog.RemoteRule rule:CarpetRuleClientNetwork.catalog().snapshot())
            if(preferences.inGroup(name,rule.key()))n++;
        return n;
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
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(super.mouseClicked(e,doubleClick)){
            if(getFocused() instanceof Button)setFocused(null);
            return true;
        }
        return select(e.x(),e.y(),e.button());
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
        if(x<left()+12||x>=split()-11||y<contentTop()||y>=height-52)return false;
        if((int)(y-contentTop())%ROW>=ROW-3)return false;
        int idx=listOffset+(int)(y-contentTop())/ROW;
        List<String> groups=names();
        if(idx>=groups.size()||idx>=listOffset+rows())return false;
        selected=groups.get(idx);
        deletePending="";feedback="";
        nameBox.setValue(selected);
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
    private boolean scroll(double x,double y,double amount){
        if(amount==0||x<left()||x>=split()||y<contentTop()||y>=height-52)return false;
        listOffset=Math.max(0,Math.min(Math.max(0,names().size()-rows()),listOffset+(amount<0?2:-2)));
        return true;
    }

    @Override public void onClose(){MinecraftCompat.setScreen(minecraft,parent);}
    private static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}
}

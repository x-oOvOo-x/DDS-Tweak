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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Read-only server-suggested values: picking one stages it on the parent screen. */
final class CarpetRuleValueChoiceScreen extends Screen {
    private static final int MARGIN=22, TOP=100, ROW=27;
    private final Screen parent;
    private final List<String> suggestions;
    private final String ruleName;
    private final Consumer<String> onChoice;
    private EditBox search;
    private String query="";
    private int offset;

    CarpetRuleValueChoiceScreen(Screen parent,String ruleName,List<String> options,
                                Consumer<String> onChoice){
        super(MinecraftCompat.translatable("dds-tweak.carpet_rules.choices.title"));
        this.parent=parent;
        this.ruleName=ruleName;
        this.suggestions=Collections.unmodifiableList(new ArrayList<>(options));
        this.onChoice=onChoice;
    }

    @Override public boolean isPauseScreen(){return false;}
    private int left(){return Math.max(MARGIN,(width-Math.min(480,width-2*MARGIN))/2);}
    private int right(){return width-left();}
    private int bottom(){return height-48;}
    private int rows(){return Math.max(1,(bottom()-TOP)/ROW);}
    private List<String> filtered(){
        String term=query.trim().toLowerCase(Locale.ROOT);
        if(term.isEmpty())return suggestions;
        List<String> items=new ArrayList<>();
        for(String value:suggestions)
            if(value.toLowerCase(Locale.ROOT).contains(term))items.add(value);
        return items;
    }
    @Override protected void init(){
        int x=left();
        //#if MC >= 11600
        search=new EditBox(font,x+30,TOP-31,right()-x-38,18,MinecraftCompat.literal(""));
        //#else
        //$$ search=new EditBox(font,x+30,TOP-31,right()-x-38,18,"");
        //#endif
        search.setValue(query);
        search.setResponder(v->{query=v;offset=0;});
        addCompat(search);
        addCompat(button(right()-80,bottom()+12,76,
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

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float pt){super.extractBackground(g,x,y,pt);}
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float pt){draw(g,x,y);super.extractRenderState(g,x,y,pt);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics g,int x,int y,float pt){
        //#if MC >= 12005
        super.renderBackground(g,x,y,pt);
        //#else
        //$$ renderTransparentBackground(g);
        //#endif
    }
    @Override public void render(GuiGraphics g,int x,int y,float pt){draw(g,x,y);super.render(g,x,y,pt);}
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics g,int x,int y,float pt){renderBackground(g);draw(g,x,y);super.render(g,x,y,pt);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack g,int x,int y,float pt){renderBackground(g);draw(g,x,y);super.render(g,x,y,pt);}
    //#else
    //$$ @Override public void render(int x,int y,float pt){renderBackground();draw(null,x,y);super.render(x,y,pt);}
    //#endif

    private void draw(
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
        CarpetRuleUiStyle.rounded(g,left(),TOP-78,right(),bottom()+8,9,CarpetRuleUiStyle.PANEL);
        text(g,ruleName,left()+10,TOP-55,0xFFF0F4F2);
        text(g,tr("dds-tweak.carpet_rules.choices.title"),left()+10,TOP-72,0xFFC1D0C6);
        CarpetRuleUiStyle.searchIcon(g,left()+8,TOP-31,18,false,false);
        List<String> items=filtered();
        offset=Math.min(offset,Math.max(0,items.size()-rows()));
        ScreenRenderCompat.beginScissor(g,left()+5,TOP,right()-5,bottom());
        for(int i=0;i<rows()&&offset+i<items.size();i++){
            int y=TOP+i*ROW;
            boolean over=mx>=left()+8&&mx<right()-8&&my>=y&&my<y+ROW-2;
            CarpetRuleUiStyle.rounded(g,left()+8,y,right()-8,y+ROW-3,5,
                    over?CarpetRuleUiStyle.HOVER:CarpetRuleUiStyle.ROW);
            text(g,items.get(offset+i),left()+17,y+8,0xFFE6EBEA);
        }
        ScreenRenderCompat.endScissor(g);
        if(items.isEmpty())text(g,tr("dds-tweak.carpet_rules.no_results"),
                left()+14,TOP+12,0xFFBCC5CB);
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
            String text,int x,int y,int color){
        ScreenRenderCompat.text(g,font,text,x,y,color,true);
    }
    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(super.mouseClicked(e,twice)){
            if(getFocused() instanceof Button)setFocused(null);
            return true;
        }
        return choose(e.x(),e.y(),e.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){
    //$$     if(super.mouseClicked(x,y,button)){
    //$$         if(getFocused() instanceof Button)setFocused(null);
    //$$         return true;
    //$$     }
    //$$     return choose(x,y,button);
    //$$ }
    //#endif
    private boolean choose(double x,double y,int button){
        //#if MC >= 260300
        //$$ if(button!=InputConstants.MOUSE_BUTTON_LEFT)return false;
        //#else
        if(button!=0)return false;
        //#endif
        if(x<left()+8||x>=right()-8||y<TOP||y>=bottom())return false;
        if((int)(y-TOP)%ROW>=ROW-3)return false;
        int i=offset+(int)(y-TOP)/ROW;
        List<String> values=filtered();
        if(i<0||i>=values.size()||i>=offset+rows())return false;
        onChoice.accept(values.get(i));
        onClose();
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
        if(delta==0||x<left()||x>=right()||y<TOP||y>=bottom())return false;
        offset=Math.max(0,Math.min(Math.max(0,filtered().size()-rows()),offset+(delta<0?2:-2)));
        return true;
    }
    @Override public void onClose(){MinecraftCompat.setScreen(minecraft,parent);}
    private static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}
}

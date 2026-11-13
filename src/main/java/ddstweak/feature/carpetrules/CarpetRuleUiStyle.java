package ddstweak.feature.carpetrules;

import ddstweak.gui.DdsUiPalette;
import ddstweak.gui.GuiIconRenderer;
import ddstweak.gui.SdfRoundedRectRenderer;
import ddstweak.platform.minecraft.ScreenRenderCompat;
//#if MC < 12002
//$$ import fi.dy.masa.malilib.gui.MaLiLibIcons;
//$$ import fi.dy.masa.malilib.gui.widgets.WidgetIcon;
//#endif
import net.minecraft.client.gui.Font;
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

/** Shared panel geometry and world overlay for the Carpet rule screens. */
final class CarpetRuleUiStyle {
    /** Square target-action buttons; draw replacement icons on a 14x14 px canvas. */
    //#if MC < 12002
    //$$ private static final WidgetIcon LEGACY_SEARCH=new WidgetIcon(0,0,MaLiLibIcons.SEARCH);
    //#endif
    static final int TARGET_ACTION_SIZE=18,TARGET_ACTION_GAP=4,TARGET_RIGHT_PADDING=14;
    static final int PANEL = DdsUiPalette.PANEL;
    static final int SELECTED = DdsUiPalette.SELECTED; // Preserve the #187 palette.
    static final int SELECTION_BACKING_SHIFT=2,TAB_BACKING_SHIFT=1,CHECKBOX_SIZE=9;
    static final int HOVER = DdsUiPalette.HOVER;
    static final int ROW = DdsUiPalette.ROW;
    static final int ACCENT=0xFF68C993, LABEL=0xFFE8F0F2, MUTED=0xFFBFC3CC;
    static final int VALUE=0xFFADDDBF, OPTION_ACTIVE=0x995F4A26;
    static final int OPTION_HOVER=0x88525C61, OPTION_IDLE=0x552F343A;
    private CarpetRuleUiStyle() {}

    static void background(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int width,int height){
        // Keep the MaLiLib-style mask after each Carpet screen's separate world-only blur.
        ScreenRenderCompat.fill(g,0,0,width,height,0xB0000000);
    }

    static void rounded(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x1,int y1,int x2,int y2,float radius,int color){
        if(x2<=x1||y2<=y1)return;
        float r=Math.max(0,Math.min(radius,Math.min(x2-x1,y2-y1)/2.0f));
        SdfRoundedRectRenderer.fill(g,x1,y1,x2,y2,r,color);
    }

    /**
     * Shape difference: paint only the left-shifted rounded pixels not occupied
     * by the foreground. Never paint accent beneath its translucent interior.
     */
    static void selectedAccent(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int left,int top,int right,int height,float radius,int shift){
        if(shift<=0||height<=0||right<=left)return;
        //#if MC >= 260100
        //$$ // Subtract both SDF masks; the curved accent extends inside left.
        //$$ SdfRoundedRectRenderer.leftAccent(g,left,top,right,top+height,radius,shift,ACCENT);
        //#else
        int bottom=top+height;
        for(int y=top;y<bottom;y++){
            for(int x=left-shift;x<Math.min(right-shift,left+radius);x++){
                int backing=roundedCoverage(x,y,left-shift,top,right-shift,bottom,radius);
                int front=roundedCoverage(x,y,left,top,right,bottom,radius);
                int visible=Math.max(0,backing-front);
                if(visible>0){
                    int alpha=(255*visible+8)/16;
                    ScreenRenderCompat.fill(g,x,y,x+1,y+1,(alpha<<24)|(ACCENT&0xFFFFFF));
                }
            }
        }
        //#endif
    }

    /** 4x4 subpixel coverage for the exact rounded-rectangle shape. */
    static int roundedCoverage(int x,int y,int left,int top,int right,int bottom,float radius){
        if(x<left||x>=right||y<top||y>=bottom)return 0;
        float r=Math.max(0,Math.min(radius,Math.min(right-left,bottom-top)/2.0f));
        if(r==0)return 16;
        int px=Math.min(x-left,right-1-x),py=Math.min(y-top,bottom-1-y);
        return px>=r||py>=r?16:cornerCoverage(r,px,py);
    }

    /** Shared visual state for rule cards; callers retain their own actions. */
    static void ruleCardBackground(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int left,int top,int right,int height,boolean selected,boolean hovered){
        float radius=selected?7.5f:7.0f;
        if(selected)selectedAccent(g,left,top,right,height,radius,SELECTION_BACKING_SHIFT);
        rounded(g,left,top,right,top+height,radius,
                selected?SELECTED:hovered?HOVER:ROW);
    }

    /** Sidebar and history filter tabs share the same curved, 1px offset accent. */
    static void tabBackground(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int left,int top,int right,int height,int radius,
            boolean selected,boolean hovered,int idleColor){
        if(selected)selectedAccent(g,left,top,right,height,radius,TAB_BACKING_SHIFT);
        rounded(g,left,top,right,top+height,radius,
                selected?SELECTED:hovered?HOVER:idleColor);
    }

    /** One-pixel visual correction for self-painted controls (20px/9px: y+6). */
    static int visualTextY(int top,int height,int lineHeight){
        return top+(height-lineHeight)/2+1;
    }

    /** Shared left-label / right-arrow layout for both filters and rule choices. */
    static void dropdownLabel(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,String label,int x,int y,int width,int height,
            int labelColor,int arrowColor){
        String shown=fit(font,label,Math.max(0,width-30));
        ScreenRenderCompat.text(g,font,shown,x+7,
                visualTextY(y,height,font.lineHeight),labelColor,true);
        dropdownArrow(g,x+width-15,y+(height-4)/2,arrowColor);
    }

    /** Consistent crisp 8x4 down arrow, independent of font glyph and shadow metrics. */
    static void dropdownArrow(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,int color){
        //#if MC >= 260100
        //$$ SdfRoundedRectRenderer.triangle(g,x,y,x+8,y+4,false,color);
        //#else
        ScreenRenderCompat.fill(g,x,y,x+8,y+1,color);
        ScreenRenderCompat.fill(g,x+1,y+1,x+7,y+2,color);
        ScreenRenderCompat.fill(g,x+2,y+2,x+6,y+3,color);
        ScreenRenderCompat.fill(g,x+3,y+3,x+5,y+4,color);
        //#endif
    }

    /** Match the dropdown arrow's pixel shape for collapsible sidebar headings. */
    static void disclosureArrow(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,boolean expanded,int color){
        if(expanded){
            dropdownArrow(g,x,y+1,color);
            return;
        }
        //#if MC >= 260100
        //$$ SdfRoundedRectRenderer.triangle(g,x+1,y,x+5,y+8,true,color);
        //#else
        // Match the established 8x4 pixel arrow on pre-26.x.
        ScreenRenderCompat.fill(g,x+1,y,x+2,y+8,color);
        ScreenRenderCompat.fill(g,x+2,y+1,x+3,y+7,color);
        ScreenRenderCompat.fill(g,x+3,y+2,x+4,y+6,color);
        ScreenRenderCompat.fill(g,x+4,y+3,x+5,y+5,color);
        //#endif
    }

    /** Center both the label and its button background, preserving native clicks. */
    static void tabButton(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,String title,int left,int top,int width,int height,
            boolean selected,boolean hovered){
        rounded(g,left,top,left+width,top+height,5,
                selected?SELECTED:hovered?HOVER:0xFF343D43);
        String shown=fit(font,title,width-10);
        ScreenRenderCompat.text(g,font,shown,left+(width-font.width(shown))/2,
                visualTextY(top,height,font.lineHeight),
                selected?0xFFFFFFFF:0xFFDDE5E4,true);
    }

    /** Consistent display: translated name and technical ID, then verified metadata. */
    static void ruleCardInfo(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,int x,int y,int maxWidth,String title,String technical,String metadata){
        int width=Math.max(0,maxWidth);
        String displayed=fit(font,title,technical.equals(title)?width:width*3/5);
        ScreenRenderCompat.text(g,font,displayed,x,y+7,0xFFF4F4FA,true);
        if(!technical.equals(title)){
            int secondary=x+9+font.width(displayed);
            ScreenRenderCompat.text(g,font,
                    fit(font,technical,Math.max(0,width-(secondary-x))),
                    secondary,y+7,0xFFB4C0C5,true);
        }
        if(metadata!=null&&!metadata.isEmpty())
            ScreenRenderCompat.text(g,font,fit(font,metadata,width),
                    x,y+22,0xFFAEBAC1,true);
    }

    /** High-contrast 11px checkmark shared by every multi-select dropdown. */
    /** Shared dropdown panel and option highlight. */
    static void choicePopupFrame(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,int width,int height){
        rounded(g,x-1,y-1,x+width+1,y+height+1,6,0xFF50625C);
        rounded(g,x,y,x+width,y+height,5,0xFF1B2830);
    }

    static void choicePopupOption(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,int width,int height,boolean selected,boolean hovered){
        if(selected||hovered)
            rounded(g,x+3,y,x+width-3,y+height-1,3,
                    selected?0xFF275D48:0xFF344C4A);
    }

    static void choiceCheckbox(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,boolean checked){
        // Center the smaller square in the unchanged 11px checkbox hit area.
        int inset=(11-CHECKBOX_SIZE)/2;
        ScreenRenderCompat.fill(g,x+inset,y+inset,
                x+inset+CHECKBOX_SIZE,y+inset+CHECKBOX_SIZE,0xFF9FAFB2);
        ScreenRenderCompat.fill(g,x+inset+1,y+inset+1,
                x+inset+CHECKBOX_SIZE-1,y+inset+CHECKBOX_SIZE-1,0xFF282E33);
        if(checked)ScreenRenderCompat.fill(g,x+inset+2,y+inset+2,
                x+inset+CHECKBOX_SIZE-2,y+inset+CHECKBOX_SIZE-2,0xFFE6FFF1);
    }

    static void listScrollbar(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int top,int height,int count,CarpetRuleListScroll scroll){
        if(CarpetRuleListScroll.max(count,height)<=0)return;
        int trackTop=top+2,trackHeight=Math.max(1,height-4);
        int thumb=CarpetRuleListScroll.thumbHeight(count,height,trackHeight);
        ScreenRenderCompat.fill(g,x,trackTop,x+3,trackTop+trackHeight,0x503E4449);
        int thumbY=scroll.thumbTop(trackTop,count,height,trackHeight);
        ScreenRenderCompat.fill(g,x,thumbY,x+3,thumbY+thumb,0xFF7A9386);
    }

    static void resetGlyph(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,int color){
        if(GuiIconRenderer.draw(g,GuiIconRenderer.RESET,x,y,16,16,color))return;
        // A true pixel circle instead of the previous tray/arrow icon.
        for(int py=2;py<14;py++)for(int px=2;px<14;px++){
            int dx=2*px-15,dy=2*py-15,d=dx*dx+dy*dy;
            if(d>=68&&d<=144)
                ScreenRenderCompat.fill(g,x+px,y+py,x+px+1,y+py+1,color);
        }
    }

    /** Accent markers keep hierarchy without stacking horizontal rules. */
    static void sectionHeading(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,String title,int x,int y,int available,float scale){
        ScreenRenderCompat.fill(g,x,y,x+2,y+9,ACCENT);
        ScreenRenderCompat.scaledText(g,font,fit(font,title,(int)((available-10)/scale)),
                x+9,y,LABEL,scale);
    }

    /** Compact, unified two-column current/definition value card. */
    static void valuePair(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,int left,int top,int width,String currentLabel,String current,
            String definitionLabel,String definition){
        int x=left+12,cardW=width-24,half=cardW/2;
        rounded(g,x,top,x+cardW,top+37,6,0x88313940);
        ScreenRenderCompat.fill(g,x+half,top+7,x+half+1,top+30,0x445C6670);
        ScreenRenderCompat.text(g,font,fit(font,currentLabel,half-14),x+7,top+6,
                0xFFD5DEE1,true);
        ScreenRenderCompat.text(g,font,fit(font,current,half-14),x+7,top+22,
                VALUE,true);
        ScreenRenderCompat.text(g,font,fit(font,definitionLabel,cardW-half-14),
                x+half+7,top+6,0xFFD5DEE1,true);
        ScreenRenderCompat.text(g,font,fit(font,definition,cardW-half-14),
                x+half+7,top+22,0xFFE3E6EA,true);
    }

    /** Same checked, hovered and idle presentation in main and preset screens. */
    static void optionRow(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,int x,int y,int width,String label,boolean checked,boolean hover){
        rounded(g,x,y,x+width,y+20,5,
                checked?OPTION_ACTIVE:hover?OPTION_HOVER:OPTION_IDLE);
        ScreenRenderCompat.translated(g,0.0F,0.5F,
                ()->choiceCheckbox(g,x+8,y+4,checked));
        String shown=fit(font,label,Math.max(0,width-36));
        ScreenRenderCompat.text(g,font,shown,x+26,
                visualTextY(y,20,font.lineHeight),
                checked?0xFFFFF2D7:0xFFCBD5DA,true);
    }

    /** Normalize uppercase enum constants for display only; send and compare raw values. */
    static String displayDefinition(String definition){
        if(definition==null||definition.isEmpty())return definition;
        boolean hasLetter=false;
        for(int i=0;i<definition.length();i++){
            char c=definition.charAt(i);
            if(c>='A'&&c<='Z'){hasLetter=true;continue;}
            if(c=='_'||c>='0'&&c<='9')continue;
            return definition;
        }
        return hasLetter?definition.toLowerCase(java.util.Locale.ROOT):definition;
    }

    /** Self-painted toolbar surface; native button still handles focus and accessibility. */
    static void toolbarButton(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,String label,int x,int y,int width,int height,
            boolean hovered,boolean active,boolean selected){
        rounded(g,x,y,x+width,y+height,5,
                !active?0xFF30363C:selected?0xFF2E7358:hovered?0xFF426557:0xFF343D43);
        int color=active?0xFFF0F5F3:0xFF8B969B;
        String shown=fit(font,label,width-10);
        ScreenRenderCompat.text(g,font,shown,x+(width-font.width(shown))/2,
                visualTextY(y,height,font.lineHeight),color,true);
    }

    /** Source/category dropdowns share the same dark control treatment. */
    static void filterButton(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,String label,int x,int y,int width,int height,boolean hovered){
        rounded(g,x,y,x+width,y+height,5,hovered?0xFF414B52:0xFF343B42);
        dropdownLabel(g,font,label,x,y,width,height,0xFFDEEDE6,0xFFB7D7C6);
    }

    /** Same dark choice treatment as the quick-edit selectors in the rule list. */
    static void dropdown(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,int x,int y,int width,int height,boolean hovered,boolean active){
        rounded(g,x,y,x+width,y+height,4,
                !active?0xFF30363C:hovered?0xFF414B52:0xFF343B42);
        dropdownArrow(g,x+(width-8)/2,y+(height-4)/2,
                active?0xFFB7D7C6:0xFF879298);
    }

    /** Pixel-perfect button icon; center measured from geometry, not text shadow. */
    static void actionIcon(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            String kind,int x,int y,int size,boolean hovered,boolean active,boolean selected){
        rounded(g,x,y,x+size,y+size,5,!active?0xFF30363C:
                selected?0xFF2E7358:hovered?0xFF426557:0xFF343D43);
        glyph(g,kind,x+(size-12)/2,y+(size-12)/2,
                active?0xFFE6F1ED:0xFF8B969B);
    }

    /** Draw a centered 12x12 icon independently of font metrics. */
    static void glyph(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            String kind,int x,int y,int color){
        if("plus".equals(kind)){
            ScreenRenderCompat.fill(g,x+2,y+5,x+10,y+7,color);
            ScreenRenderCompat.fill(g,x+5,y+2,x+7,y+10,color);
        }else if("menu".equals(kind)){
            ScreenRenderCompat.fill(g,x+1,y+2,x+11,y+4,color);
            ScreenRenderCompat.fill(g,x+1,y+5,x+11,y+7,color);
            ScreenRenderCompat.fill(g,x+1,y+8,x+11,y+10,color);
        }else if("trash".equals(kind)){
            ScreenRenderCompat.fill(g,x+2,y+3,x+10,y+4,color);
            ScreenRenderCompat.fill(g,x+4,y+2,x+8,y+3,color);
            ScreenRenderCompat.fill(g,x+3,y+4,x+4,y+10,color);
            ScreenRenderCompat.fill(g,x+8,y+4,x+9,y+10,color);
            ScreenRenderCompat.fill(g,x+4,y+10,x+8,y+11,color);
            ScreenRenderCompat.fill(g,x+5,y+5,x+6,y+9,color);
            ScreenRenderCompat.fill(g,x+7,y+5,x+8,y+9,color);
        }else if("remove".equals(kind)){
            for(int i=0;i<7;i++){
                ScreenRenderCompat.fill(g,x+2+i,y+2+i,x+4+i,y+4+i,color);
                ScreenRenderCompat.fill(g,x+8-i,y+2+i,x+10-i,y+4+i,color);
            }
        }
    }

    /** Same search button in the manager and history screens. */
    static void searchIcon(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,int size,boolean hovered,boolean expanded){
        // Preserve the existing button surface; only change the glyph resource.
        // 保留原有底框，不再用白色背景掩盖错误图标。
        if(hovered||expanded)
            rounded(g,x,y,x+size,y+size,4,expanded?0xBB28553F:0x994B535A);
        //#if MC >= 12002
        int iconX=x+(size-12)/2,iconY=y+(size-12)/2;
        GuiIconRenderer.draw(g,GuiIconRenderer.VANILLA_SEARCH,iconX,iconY,12,12,0xFFFFFFFF);
        //#else
        //$$ // WidgetIcon binds the MaLiLib atlas and resets tint before drawing.
        //$$ LEGACY_SEARCH.setPosition(x+(size-MaLiLibIcons.SEARCH.getWidth())/2,
        //$$         y+(size-MaLiLibIcons.SEARCH.getHeight())/2);
        //$$ LEGACY_SEARCH.render(false,false);
        //#endif
    }

    /** Compose icon and label using their visible geometry, independent of font shadow. */
    static void toolbarButtonWithPlus(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            Font font,String label,int x,int y,int width,int height,
            boolean hovered,boolean active){
        rounded(g,x,y,x+width,y+height,5,
                !active?0xFF30363C:hovered?0xFF426557:0xFF343D43);
        String shown=fit(font,label,width-30);
        int total=12+5+font.width(shown),start=x+Math.max(3,(width-total)/2);
        glyph(g,"plus",start,y+(height-12)/2,active?0xFFF0F5F3:0xFF8B969B);
        ScreenRenderCompat.text(g,font,shown,start+17,
                visualTextY(y,height,font.lineHeight),
                active?0xFFF0F5F3:0xFF8B969B,false);
    }

    private static String fit(Font font,String value,int width){
        if(value==null||width<=0)return "";
        if(font.width(value)<=width)return value;
        int n=value.length();
        while(n>0&&font.width(value.substring(0,n)+"…")>width)n--;
        return value.substring(0,n)+"…";
    }

    /** Compact, font-independent save/default glyph shared by list and detail panels. */
    static void saveGlyph(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,boolean saved,int color){
        if(GuiIconRenderer.draw(g,saved?GuiIconRenderer.DEFAULT_ON:GuiIconRenderer.DEFAULT_OFF,
                x-2,y-2,16,16,0xFFFFFFFF))return;
        ScreenRenderCompat.fill(g,x,y,x+12,y+12,color);
        ScreenRenderCompat.fill(g,x+2,y+2,x+9,y+5,0xFF28313A);
        ScreenRenderCompat.fill(g,x+4,y+2,x+7,y+4,color);
        ScreenRenderCompat.fill(g,x+2,y+7,x+10,y+11,0xFF28313A);
        ScreenRenderCompat.fill(g,x+4,y+8,x+8,y+11,0xFFD8E6E5);
    }

    private static int cornerCoverage(float radius,int pixelX,int pixelY){
        int hits=0;
        for(int sy=0;sy<4;sy++)for(int sx=0;sx<4;sx++){
            double x=radius-pixelX-(sx+0.5)/4.0;
            double y=radius-pixelY-(sy+0.5)/4.0;
            if(x*x+y*y<=radius*radius)hits++;
        }
        return hits;
    }
}

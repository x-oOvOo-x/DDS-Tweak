package ddstweak.feature.carpetrules;

import ddstweak.gui.GuiIconRenderer;
import ddstweak.platform.minecraft.ScreenRenderCompat;
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
import java.util.List;

/**
 * Paints the rule cards and sidebar; catalog and UI state remain owned by Screen.
 * 绘制规则列表与侧栏；目录和界面状态仍由 Screen 统一持有。
 */
final class CarpetRuleManagerRenderer {
    private final CarpetRuleManagerScreen owner;
    CarpetRuleManagerRenderer(CarpetRuleManagerScreen owner){this.owner=owner;}

    void renderDetails(
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
        owner.mouseX=mx;
        owner.mouseY=my;
        if(owner.editingGroups){
            List<String> options=owner.groupOptions();
            int top=owner.LIST_TOP+53,capacity=Math.max(1,(owner.listHeight()-53)/owner.LIST_ROW);
            owner.groupOffset=Math.min(owner.groupOffset,Math.max(0,options.size()-capacity));
            ScreenRenderCompat.fill(g,owner.MARGIN,top,owner.screenWidth()-owner.MARGIN,owner.LIST_TOP+owner.listHeight(),0x33000000);
            ScreenRenderCompat.beginScissor(g,owner.MARGIN,top,owner.screenWidth()-owner.MARGIN,owner.LIST_TOP+owner.listHeight());
            for(int i=0;i<capacity&&owner.groupOffset+i<options.size();i++){
                String item=options.get(owner.groupOffset+i),name=item.isEmpty()?owner.tr("dds-tweak.carpet_rules.groups.all"):item;
                int y=top+i*owner.LIST_ROW;
                if(item.equals(owner.group))ScreenRenderCompat.fill(g,owner.MARGIN,y,owner.screenWidth()-owner.MARGIN,y+owner.LIST_ROW-1,0x555A8DB6);
                owner.draw(g,owner.fit(name,owner.screenWidth()-2*owner.MARGIN-24),owner.MARGIN+6,y+7,0xFFFFFFFF);
                if(owner.managedRule!=null&&!item.isEmpty()&&owner.preferences.inGroup(item,owner.managedRule))
                    owner.draw(g,"*",owner.screenWidth()-owner.MARGIN-14,y+7,0xFFFFD279);
            }
            ScreenRenderCompat.endScissor(g);
            if(!owner.feedback.isEmpty())owner.draw(g,owner.fit(owner.feedback,owner.screenWidth()-2*owner.MARGIN),owner.MARGIN,owner.screenHeight()-40,0xFFDDDDBB);
            return;
        }
        List<CarpetRuleCatalog.RemoteRule> rules=owner.visible();
        owner.cardScroll.animate(rules.size(),owner.cardViewportHeight());
        owner.positionInlineNumber(rules);
        boolean detailsOnly=owner.narrow()&&owner.narrowDetails;
        owner.hoveredRule=null;
        owner.hoveredDefaultRule=null;
        owner.hoveredFavoriteRule=null;
        if(!owner.narrow()||owner.sidebarOverlay)renderSidebar(g);
        if(owner.narrow()&&owner.sidebarOverlay)return;
        if(!detailsOnly){
            int left=owner.listLeft(),w=owner.listWidth(),topLimit=owner.cardViewportTop(),bottomLimit=owner.cardViewportBottom();
            owner.rounded(g,left,owner.LIST_TOP,left+w,owner.LIST_TOP+owner.listHeight(),9,CarpetRuleUiStyle.PANEL);
            renderSearchIcon(g);
            ScreenRenderCompat.beginScissor(g,left,topLimit,left+w,bottomLimit);
            int first=Math.max(0,(int)(owner.cardScroll.position()/CarpetRuleListScroll.STRIDE));
            int last=Math.min(rules.size(),first+owner.cardViewportHeight()/CarpetRuleListScroll.STRIDE+3);
            for(int i=first;i<last;i++){
                CarpetRuleCatalog.RemoteRule entry=rules.get(i);
                int y=owner.cardScroll.top(owner.cardViewportTop(),i);
                int x1=left+6,x2=left+w-owner.CARD_RIGHT_INSET,cardTop=y,
                        cardBottom=y+CarpetRuleListScroll.CARD_HEIGHT;
                if(cardBottom<=topLimit||cardTop>=bottomLimit)continue;
                // Popups own pointer hover; cards beneath them must never light up.
                boolean hover=owner.choiceRule==null&&owner.inside(owner.mouseX,owner.mouseY,x1,cardTop,
                        x2-x1,CarpetRuleListScroll.CARD_HEIGHT);
                // Editor widgets own their hover: rule details belong only to the label area.
                int controlLeft=owner.isBoolean(entry)
                        ?CarpetRuleListScroll.switchX(x2)+owner.rowValueShift()-2:
                        owner.isChoiceRule(entry)||owner.isNumberRule(entry)
                                ?CarpetRuleListScroll.choiceX(x2)+owner.rowValueShift()
                                :x2-110+owner.rowValueShift();
                if(hover&&owner.mouseX<controlLeft)owner.hoveredRule=entry;
                boolean chosen=entry.key().equals(owner.selected);
                CarpetRuleUiStyle.ruleCardBackground(g,x1,cardTop,x2,
                        CarpetRuleListScroll.CARD_HEIGHT,chosen,hover);
                VanillaGameRules.Entry nativeRule=owner.vanillaMode()?VanillaGameRules.get(entry.key()):null;
                CarpetRuleMetadata.Description info=nativeRule==null?owner.metadata.find(entry.key()):null;
                String label=nativeRule!=null?nativeRule.title:
                        info==null?entry.key().name():info.displayName();
                String technical=nativeRule!=null?nativeRule.id:entry.key().name();
                int nameX=x1+10;
                int textWidth=Math.max(22,x2-nameX-
                        ((owner.isChoiceRule(entry)||owner.isNumberRule(entry))?151:101));
                String metaText=nativeRule!=null
                        ?owner.tr("dds-tweak.carpet_rules.vanilla.category",nativeRule.category)
                        :CarpetRuleSidebar.cardMetadata(
                                CarpetRuleClientNetwork.integratedOwner(entry.key()),
                                CarpetRuleAuthority.categories(entry.key()).isEmpty()
                                        ?info==null?java.util.Collections.emptySet():info.categories()
                                        :CarpetRuleAuthority.categories(entry.key()));
                CarpetRuleUiStyle.ruleCardInfo(g,owner.renderFont(),nameX,cardTop,textWidth,
                        label,technical,metaText);
                if(owner.isBoolean(entry)){
                    String inFlight=owner.temporaryInFlight(entry.key());
                    boolean active=Boolean.parseBoolean(inFlight==null?entry.value():inFlight);
                    int toggleLeft=CarpetRuleListScroll.switchX(x2)+owner.rowValueShift();
                    owner.drawToggle(g,toggleLeft,cardTop+11,CarpetRuleListScroll.SWITCH_WIDTH,
                            CarpetRuleListScroll.SWITCH_HEIGHT,active,inFlight!=null,
                            owner.inside(owner.mouseX,owner.mouseY,toggleLeft-2,cardTop+8,
                                    CarpetRuleListScroll.SWITCH_WIDTH+4,20));
                }else{
                    String pending=owner.drafts.desired(entry.key());
                    String value=owner.fit(pending==null?entry.value():pending,58);
                    int valueLeft=x2-77-owner.renderFont().width(value)+owner.rowValueShift();
                    if(owner.isChoiceRule(entry)){
                        CarpetRuleMultiValue multi=CarpetRuleClientNetwork.multiValue(entry.key());
                        int choiceLeft=CarpetRuleListScroll.choiceX(x2)+owner.rowValueShift();
                        int choiceWidth=CarpetRuleListScroll.CHOICE_WIDTH;
                        owner.rounded(g,choiceLeft,cardTop+8,choiceLeft+choiceWidth,cardTop+28,4,
                                owner.inside(owner.mouseX,owner.mouseY,choiceLeft,cardTop+8,choiceWidth,20)
                                        ?0xFF3D5A50:chosen?0xFF24343B:0xFF343B42);
                        String shown=pending==null?entry.value():pending;
                        if(multi!=null&&multi.selectedCount(shown)>=0)
                            shown=multi.selectedCount(shown)+"/"+multi.options().size();
                        CarpetRuleUiStyle.dropdownLabel(g,owner.renderFont(),shown,choiceLeft,
                                cardTop+8,choiceWidth,20,
                                chosen?0xFFFFFFFF:0xFFB7D7C6,
                                chosen?0xFFE8F9EF:0xFFD9E7DD);
                    }else if(owner.isNumberRule(entry)){
                        int editLeft=CarpetRuleListScroll.choiceX(x2)+owner.rowValueShift();
                        boolean editing=entry.key().equals(owner.inlineNumberKey);
                        if(!editing){
                            owner.rounded(g,editLeft,cardTop+8,editLeft+CarpetRuleListScroll.CHOICE_WIDTH,
                                    cardTop+28,4,owner.inside(owner.mouseX,owner.mouseY,editLeft,cardTop+8,
                                            CarpetRuleListScroll.CHOICE_WIDTH,20)
                                            ?0xFF414B52:0xFF343B42);
                            owner.draw(g,owner.fit(pending==null?entry.value():pending,
                                    CarpetRuleListScroll.CHOICE_WIDTH-12),
                                    editLeft+6,cardTop+15,0xFFB7D7C6);
                        }
                    }else owner.draw(g,value,valueLeft,cardTop+13,0xFFB7D7C6);
                }
                boolean favorite=owner.preferences.isFavorite(entry.key());
                int favoriteLeft=CarpetRuleListScroll.favoriteX(x2);
                boolean favoriteHover=owner.inside(owner.mouseX,owner.mouseY,favoriteLeft,cardTop+8,
                        CarpetRuleListScroll.STAR_WIDTH,20);
                if(favoriteHover)owner.hoveredFavoriteRule=entry;
                if(favoriteHover)owner.rounded(g,favoriteLeft,cardTop+8,
                        favoriteLeft+CarpetRuleListScroll.STAR_WIDTH,cardTop+28,5,0x88495651);
                if(!GuiIconRenderer.draw(g,favorite?GuiIconRenderer.FAVORITE_ON:
                        GuiIconRenderer.FAVORITE_OFF,
                        favoriteLeft+(CarpetRuleListScroll.STAR_WIDTH-16)/2,
                        cardTop+10,16,16,0xFFFFFFFF)){
                    String favoriteIcon=favorite?"★":"☆";
                    owner.draw(g,favoriteIcon,favoriteLeft+1+
                            (CarpetRuleListScroll.STAR_WIDTH-owner.renderFont().width(favoriteIcon))/2,
                            cardTop+9+(20-owner.renderFont().lineHeight)/2,
                            favorite?0xFFFFD67C:favoriteHover?0xFFFFF4CE:0xFFD0D4E0);
                }
                if(!owner.vanillaMode()){
                    int sx=CarpetRuleListScroll.saveX(x2),sy=cardTop+8;
                    int saved=owner.savedDefaultState(entry.key());
                    boolean overSave=owner.inside(owner.mouseX,owner.mouseY,sx,sy,20,20);
                    if(overSave){
                        owner.rounded(g,sx,sy,sx+20,sy+20,4,0xFF44544F);
                        owner.hoveredDefaultRule=entry;
                    }
                    CarpetRuleUiStyle.saveGlyph(g,sx+4,sy+4,saved==1,
                            saved==1?0xFFE6B45C:saved==2?0xFFB6C1C7:0xFF77868D);
                    if(saved==-1)owner.draw(g,"?",sx+13,sy+1,0xFFFFD18A);
                }
            }
            ScreenRenderCompat.endScissor(g);
            int viewport=owner.cardViewportHeight();
            CarpetRuleUiStyle.listScrollbar(g,left+w-owner.SCROLLBAR_RIGHT_INSET,
                    topLimit,viewport,rules.size(),owner.cardScroll);
            if(rules.isEmpty()){
                String empty=owner.vanillaMode()
                        ?!VanillaGameRules.supported()
                            ?owner.tr("dds-tweak.carpet_rules.sidebar.vanilla_game_rules_unavailable")
                            :!VanillaGameRules.received()
                                ?owner.tr(VanillaGameRules.unavailable()
                                        ?"dds-tweak.carpet_rules.integrated_unavailable"
                                        :"dds-tweak.carpet_rules.waiting")
                                :owner.tr("dds-tweak.carpet_rules.no_results")
                        :CarpetRuleClientNetwork.catalog().isReady()
                            ?owner.tr(owner.emptyPlan()?"dds-tweak.carpet_rules.plans.empty"
                                    :"dds-tweak.carpet_rules.no_results")
                            :CarpetRuleClientNetwork.integratedSyncUnavailable()
                                ?owner.tr("dds-tweak.carpet_rules.integrated_unavailable")
                                :owner.tr("dds-tweak.carpet_rules.waiting");
                owner.draw(g,empty,left+12,owner.cardViewportTop()+8,0xFFC1C4CB);
                if(owner.emptyPlan())
                    owner.draw(g,owner.fit(owner.tr("dds-tweak.carpet_rules.plans.empty_hint"),Math.max(20,w-24)),
                            left+12,owner.cardViewportTop()+32,0xFFBFC6CC);
            }
        }
        if(!owner.narrow()||detailsOnly){
            int left=owner.detailLeft(),w=owner.detailWidth();
            owner.rounded(g,left,owner.LIST_TOP,left+w,owner.LIST_TOP+owner.listHeight(),9,CarpetRuleUiStyle.PANEL);
            CarpetRuleCatalog.RemoteRule entry=null;
            if(owner.selected!=null)for(CarpetRuleCatalog.RemoteRule candidate:rules)
                if(candidate.key().equals(owner.selected)){entry=candidate;break;}
            if(entry==null&&!rules.isEmpty())entry=rules.get(0);
            if(entry!=null){
                VanillaGameRules.Entry nativeRule=owner.vanillaMode()?VanillaGameRules.get(entry.key()):null;
                CarpetRuleMetadata.Description info=nativeRule==null?owner.metadata.find(entry.key()):null;
                String title=nativeRule!=null?nativeRule.title:
                        info==null?entry.key().name():info.displayName();
                owner.detailHeading(g,title,left+12,owner.LIST_TOP+8,w-24,
                        owner.DETAIL_TITLE_SCALE,0xFFF8FCFF);
                String technicalName=nativeRule!=null?nativeRule.id:entry.key().name();
                if(!technicalName.equals(title))
                    owner.draw(g,owner.fit(technicalName,w-24),left+12,owner.LIST_TOP+25,0xFFBBC7CC);
                owner.detailDivider(g,left,w,owner.LIST_TOP+36);

                CarpetRuleUiStyle.sectionHeading(g,owner.renderFont(),
                        owner.tr("dds-tweak.carpet_rules.description_label"),
                        left+12,owner.DESCRIPTION_TOP-15,w-24,owner.DETAIL_LABEL_SCALE);
                List<String> descriptions=owner.wrapped(owner.ruleDescription(entry),w-30);
                int cap=Math.max(1,owner.descriptionHeight()/owner.DESCRIPTION_LINE_H);
                owner.descriptionOffset=Math.min(owner.descriptionOffset,
                        Math.max(0,descriptions.size()-cap));
                ScreenRenderCompat.beginScissor(g,left+11,owner.DESCRIPTION_TOP,
                        left+w-9,owner.DESCRIPTION_TOP+owner.descriptionHeight());
                for(int i=0;i<cap&&owner.descriptionOffset+i<descriptions.size();i++)
                    owner.draw(g,descriptions.get(owner.descriptionOffset+i),left+12,
                            owner.DESCRIPTION_TOP+i*owner.DESCRIPTION_LINE_H,0xFFBFC3CC);
                ScreenRenderCompat.endScissor(g);
                if(descriptions.size()>cap)
                    owner.draw(g,owner.descriptionOffset+cap<descriptions.size()?"▾":"▴",
                            left+w-20,owner.DESCRIPTION_TOP+owner.descriptionHeight()-11,0xFF9EADA8);

                String original=nativeRule!=null?nativeRule.defaultValue:
                        CarpetRuleClientNetwork.definitionDefault(entry.key());
                CarpetRuleUiStyle.valuePair(g,owner.renderFont(),left,owner.detailValuesY(),w,
                        owner.tr("dds-tweak.carpet_rules.current"),entry.value(),
                        owner.tr("dds-tweak.carpet_rules.definition_label"),
                        original==null?"—":CarpetRuleUiStyle.displayDefinition(original));
                CarpetRuleUiStyle.sectionHeading(g,owner.renderFont(),
                        owner.tr("dds-tweak.carpet_rules.target"),
                        left+12,owner.detailTargetY()-15,w-24,owner.DETAIL_LABEL_SCALE);
                if(owner.editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT){
                    int rx=owner.detailResetX(),ry=owner.detailTargetY();
                    boolean enabled=owner.definitionDefault(entry)!=null;
                    boolean over=owner.inside(owner.mouseX,owner.mouseY,rx,ry,
                            CarpetRuleUiStyle.TARGET_ACTION_SIZE,owner.FIELD_H);
                    owner.rounded(g,rx,ry,rx+CarpetRuleUiStyle.TARGET_ACTION_SIZE,ry+owner.FIELD_H,3,
                            enabled?(over?0xFF426B58:0xFF343D44):0xFF303339);
                    owner.drawResetGlyph(g,rx+1,ry+(owner.FIELD_H-16)/2,
                            enabled?0xFFE4EAE9:0xFF697078);
                }
                if(owner.editMode==CarpetRuleCommands.Mode.REMOVE_DEFAULT){
                    owner.draw(g,owner.tr("dds-tweak.carpet_rules.remove_no_target"),left+12,
                            owner.detailTargetY()+4,0xFFB4C3C5);
                }else if(owner.isBoolean(entry)){
                    // The target is the edited value in every mode; the server value
                    // is shown separately above and never replaced by a default draft.
                    String desired=owner.vanillaMode()?owner.vanillaDesired(entry):
                            owner.targetBox==null?null:owner.targetBox.getValue();
                    boolean selectedBoolean=Boolean.parseBoolean(desired==null?entry.value():desired);
                    int switchX=left+12,switchY=owner.detailTargetY(),switchW=w-54;
                    owner.rounded(g,switchX,switchY,switchX+switchW,switchY+owner.FIELD_H,5,0xBB181C24);
                    for(int i=0;i<2;i++){
                        boolean option=i==0;
                        int ox=switchX+i*(switchW/2),ow=i==0?switchW/2-2:switchW-switchW/2;
                        boolean on=selectedBoolean==option;
                        boolean hover=owner.inside(owner.mouseX,owner.mouseY,ox,switchY,ow,owner.FIELD_H);
                        if(on||hover)owner.rounded(g,ox,switchY,ox+ow,switchY+owner.FIELD_H,5,
                                on?(option?0xFF2C785C:0xFF865048):0xFF424A59);
                        String label=option?"true":"false";
                        owner.draw(g,label,ox+Math.max(3,(ow-owner.renderFont().width(label))/2),
                                switchY+(owner.FIELD_H-owner.renderFont().lineHeight)/2+1,
                                on?0xFFFFFFFF:0xFFBFC4CC);
                    }
                }

                if(!owner.vanillaMode()&&owner.showDefaults()){
                    owner.detailDivider(g,left,w,owner.detailTargetY()+30);
                    int modeY=owner.detailModeY(),savedState=owner.savedDefaultState(entry.key());
                    CarpetRuleUiStyle.sectionHeading(g,owner.renderFont(),
                            owner.tr("dds-tweak.carpet_rules.default_section"),
                            left+12,modeY-12,w-24,owner.DETAIL_LABEL_SCALE);
                    String savedLabel=owner.tr(savedState==1?"dds-tweak.carpet_rules.default_saved":
                            savedState==2?"dds-tweak.carpet_rules.default_not_saved":
                            "dds-tweak.carpet_rules.default_status_unknown");
                    int glyphX=left+w-27;
                    String shown=owner.fit(savedLabel,Math.max(24,w-164));
                    owner.draw(g,shown,glyphX-7-owner.renderFont().width(shown),modeY-12,0xFFD5E2DC);
                    CarpetRuleUiStyle.saveGlyph(g,glyphX,modeY-14,savedState==1,
                            savedState==1?0xFFE6B45C:
                            savedState==2?0xFFB9C6CB:0xFF788A95);
                    CarpetRuleCommands.Mode[] persistentModes={
                            CarpetRuleCommands.Mode.SET_DEFAULT,
                            CarpetRuleCommands.Mode.REMOVE_DEFAULT};
                    for(int i=0;i<persistentModes.length;i++){
                        int y=modeY+6+i*24;
                        boolean checked=owner.editMode==persistentModes[i];
                        boolean hover=owner.inside(owner.mouseX,owner.mouseY,left+12,y,w-24,20);
                        CarpetRuleUiStyle.optionRow(g,owner.renderFont(),left+12,y,w-24,
                                owner.tr("dds-tweak.carpet_rules.mode."+
                                        (i==0?"set_default":"remove_default")),
                                checked,hover);
                    }
                    int helpY=modeY+65;
                    int footerY=owner.LIST_TOP+owner.listHeight()-29;
                    if(helpY<=footerY-(owner.feedback.isEmpty()?20:36))
                        owner.draw(g,owner.fit(owner.tr("dds-tweak.carpet_rules.default_help"),w-24),
                                left+12,helpY,0xFFACBDB9);
                }

                if(owner.showPlanAssignment()&&!owner.vanillaMode()){
                    CarpetRuleUiStyle.sectionHeading(g,owner.renderFont(),
                            owner.tr("dds-tweak.carpet_rules.plans.sidebar"),
                            left+12,owner.detailGroupAssignY()-17,w-24,owner.DETAIL_LABEL_SCALE);
                    List<CarpetRulePresetStore.Preset> assigned=owner.assignedPlans(entry.key());
                    if(!entry.key().equals(owner.quickPlanRule)){
                        owner.quickPlanRule=entry.key();
                        owner.quickPlanOffset=0;
                    }
                    int visiblePlanRows=owner.quickPlanRows(),top=owner.quickPlanTop();
                    owner.quickPlanOffset=Math.min(owner.quickPlanOffset,Math.max(0,assigned.size()-visiblePlanRows));
                    if(assigned.isEmpty())
                        owner.draw(g,owner.tr("dds-tweak.carpet_rules.groups.none"),
                                left+12,top+5,0xFFBCC6BF);
                    else if(visiblePlanRows==0)
                        owner.draw(g,owner.fit(assigned.get(0).name(),w-24),left+12,top+4,0xFFBCC6BF);
                    else{
                        ScreenRenderCompat.beginScissor(g,left+10,top,
                                left+w-10,top+visiblePlanRows*35);
                        for(int i=0;i<visiblePlanRows&&i+owner.quickPlanOffset<assigned.size();i++){
                            CarpetRulePresetStore.Preset plan=assigned.get(i+owner.quickPlanOffset);
                            int y=top+i*35,buttonX=left+w-79;
                            CarpetRuleUiStyle.rounded(g,left+12,y,left+w-12,y+31,5,
                                    CarpetRuleUiStyle.ROW);
                            owner.draw(g,owner.fit(plan.name(),Math.max(30,w-119)),
                                    left+19,y+4,0xFFF0F4F4);
                            String mode=owner.tr("dds-tweak.carpet_rules.mode."+
                                    (plan.applyMode()==CarpetRuleCommands.Mode.SET_DEFAULT
                                            ?"set_default":"temporary"));
                            String meta=owner.tr("dds-tweak.carpet_rules.plans.count",plan.values().size())
                                    +" · "+mode;
                            owner.draw(g,owner.fit(meta,Math.max(30,w-119)),left+19,y+18,0xFF9FBEAF);
                            boolean hover=owner.inside(owner.mouseX,owner.mouseY,buttonX,y+5,65,21);
                            CarpetRuleUiStyle.rounded(g,buttonX,y+5,buttonX+65,y+26,5,
                                    hover?0xFF428469:0xFF2D674F);
                            String label=owner.tr("dds-tweak.carpet_rules.plans.quick_apply");
                            owner.draw(g,owner.fit(label,58),buttonX+5,y+12,0xFFFFFFFF);
                        }
                        ScreenRenderCompat.endScissor(g);
                        if(assigned.size()>visiblePlanRows)
                            owner.draw(g,(owner.quickPlanOffset+visiblePlanRows)+"/"+assigned.size(),
                                    left+w-50,top+visiblePlanRows*35+3,0xFFABBDB6);
                    }
                }
            }
            if(entry==null&&owner.sidebarKind==CarpetRuleSidebar.Kind.PLAN&&!owner.emptyPlan())
                owner.draw(g,owner.fit(owner.tr("dds-tweak.carpet_rules.no_results"),w-24),
                        left+12,owner.LIST_TOP+19,0xFFE7EEEE);
            if(detailsOnly)owner.draw(g,owner.tr("dds-tweak.carpet_rules.list_hint"),left+12,owner.LIST_TOP+owner.listHeight()-15,0xFFAAAAAA);
        }
        if(!owner.feedback.isEmpty()&&(!owner.narrow()||detailsOnly)){
            int statusX=owner.detailLeft()+12;
            int statusY=owner.showDefaults()&&CarpetRuleDetailLayout.compact(owner.listHeight())
                    ?owner.detailModeY()+54:owner.LIST_TOP+owner.listHeight()-46;
            owner.draw(g,owner.fit(owner.feedback,Math.max(20,owner.detailWidth()-24)),
                    statusX,statusY,0xFFFFDE9F);
        }
    }

    /** Vanilla-style pixel magnifier; uses the same GUI renderer across target versions. */
    private void renderSearchIcon(
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
        int x=owner.searchIconX(),y=owner.searchIconY();
        CarpetRuleUiStyle.searchIcon(g,x,y,owner.SEARCH_ICON_SIZE,
                owner.inside(owner.mouseX,owner.mouseY,x,y,owner.SEARCH_ICON_SIZE,owner.SEARCH_ICON_SIZE),owner.searchOpen);
    }

    private void renderSidebar(
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
        int left=owner.MARGIN,w=owner.sidebarWidth(),bottom=owner.sidebarBottom();
        owner.hoveredSourceLabel=null;
        owner.rounded(g,left,owner.LIST_TOP,left+w,bottom,9,CarpetRuleUiStyle.PANEL);
        drawSidebarLine(g,left,owner.LIST_TOP+7,
                owner.tr("dds-tweak.carpet_rules.sidebar.all"),
                CarpetRuleSidebar.Kind.ALL,"",
                owner.uiRules().size(),false);

        drawSidebarHeader(g,left,owner.LIST_TOP+34,
                owner.tr("dds-tweak.carpet_rules.sidebar.extensions"),CarpetRuleSidebar.Section.EXTENSIONS,false);
        for(CarpetRuleSidebar.Section section:new CarpetRuleSidebar.Section[]{
                CarpetRuleSidebar.Section.EXTENSIONS,CarpetRuleSidebar.Section.CATEGORIES,
                CarpetRuleSidebar.Section.GROUPS}){
            if(!owner.isExpanded(section))continue;
            int choiceTop=owner.sidebarChoiceTop(section),choiceBottom=owner.sidebarChoiceBottom(section);
            int cap=owner.sidebarRows(section);
            if(cap==0)continue;
            List<CarpetRuleSidebar.Entry> choices=owner.sidebarChoices(section);
            int choiceOffset=Math.min(owner.sidebarOffset(section),Math.max(0,choices.size()-cap));
            owner.setSidebarOffset(section,choiceOffset);
            ScreenRenderCompat.beginScissor(g,left+5,choiceTop,left+w-5,choiceBottom);
            for(int row=0;row<cap&&choiceOffset+row<choices.size();row++){
                CarpetRuleSidebar.Entry item=choices.get(choiceOffset+row);
                drawSidebarLine(g,left,owner.sidebarRowY(section,row),owner.sidebarLabel(item),
                        item.kind(),item.id(),item.count(),true);
            }
            ScreenRenderCompat.endScissor(g);
        }

        int quick=owner.quickHeaderY();
        owner.draw(g,owner.tr("dds-tweak.carpet_rules.sidebar.filters"),
                left+owner.SIDEBAR_TAB_INSET,quick+2,0xFFC6CDDC);
        ScreenRenderCompat.fill(g,left+owner.SIDEBAR_TAB_INSET,quick+15,
                left+w-owner.SIDEBAR_TAB_INSET,quick+16,0x526E788A);
        CarpetRuleSidebar.Entry favorites=owner.findSidebarEntry(CarpetRuleSidebar.Kind.FAVORITES,"");
        CarpetRuleSidebar.Entry modified=owner.findSidebarEntry(CarpetRuleSidebar.Kind.MODIFIED,"");
        CarpetRuleSidebar.Entry defaults=owner.findSidebarEntry(CarpetRuleSidebar.Kind.SAVED_DEFAULTS,"");
        CarpetRuleSidebar.Entry game=owner.findSidebarEntry(CarpetRuleSidebar.Kind.GAME_RULES,"");
        if(favorites!=null)drawSidebarLine(g,left,quick+22,owner.sidebarLabel(favorites),
                favorites.kind(),favorites.id(),favorites.count(),false);
        if(modified!=null)drawSidebarLine(g,left,quick+44,owner.sidebarLabel(modified),
                modified.kind(),modified.id(),modified.count(),false);
        if(defaults!=null)drawSidebarLine(g,left,quick+66,owner.sidebarLabel(defaults),
                defaults.kind(),defaults.id(),defaults.count(),false);
        if(game!=null)drawSidebarLine(g,left,quick+88,owner.sidebarLabel(game),
                game.kind(),game.id(),game.count(),false);
        drawSidebarHeader(g,left,owner.categoryHeaderY(),
                owner.tr("dds-tweak.carpet_rules.sidebar.categories"),
                CarpetRuleSidebar.Section.CATEGORIES,false);
        int groupsY=owner.groupHeaderY();
        drawSidebarHeader(g,left,groupsY,
                owner.tr("dds-tweak.carpet_rules.plans.sidebar"),
                CarpetRuleSidebar.Section.GROUPS,true);
    }

    private void drawSidebarText(
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
        int cursor=x;
        for(int start=0;start<value.length();){
            boolean han=CarpetRuleSidebar.isChineseLabelCodepoint(value.codePointAt(start));
            int end=start;
            do{
                end+=Character.charCount(value.codePointAt(end));
            }while(end<value.length()&&han==CarpetRuleSidebar.isChineseLabelCodepoint(value.codePointAt(end)));
            String run=value.substring(start,end);
            int runX=cursor;
            ScreenRenderCompat.translated(g,0.0F,han?-1.0F:-0.5F,
                    ()->owner.draw(g,run,runX,y,color));
            cursor+=owner.renderFont().width(run);
            start=end;
        }
    }

    private void drawSidebarLine(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,String text,CarpetRuleSidebar.Kind kind,String id,int count,boolean indent){
        int w=owner.sidebarWidth(),inset=indent?12:7;
        boolean picked=owner.sidebarKind==kind&&owner.sidebarId.equals(id);
        boolean hover=owner.inside(owner.mouseX,owner.mouseY,x+6,y,w-12,20);
        if(indent)
            owner.rounded(g,x+owner.SIDEBAR_TAB_INSET,y,x+w-owner.SIDEBAR_TAB_INSET,y+20,5,
                    picked?CarpetRuleUiStyle.SELECTED:
                    hover?CarpetRuleUiStyle.HOVER:0x5530353B);
        else
            CarpetRuleUiStyle.tabBackground(g,x+owner.SIDEBAR_TAB_INSET,y,
                    x+w-owner.SIDEBAR_TAB_INSET,20,5,
                    picked,hover,0x5530353B);
        String number=count<0?"":Integer.toString(count);
        int textX=x+inset+3;
        int right=x+w-12-(count<0?0:owner.renderFont().width(number)+8);
        int available=Math.max(16,right-textX);
        String fittedLabel=owner.fit(text,available);
        drawSidebarText(g,fittedLabel,textX,y+7,0xFFE5E8F2);
        if(hover&&kind==CarpetRuleSidebar.Kind.MODIFIED)
            owner.hoveredSourceLabel=owner.tr("dds-tweak.carpet_rules.sidebar.modified_note");
        if(hover&&kind==CarpetRuleSidebar.Kind.OWNER){
            String full=CarpetRuleClientNetwork.sourceDisplayName(id);
            String shown=CarpetRuleSidebar.sourceLabel(id,full);
            if(!shown.equals(text)||owner.renderFont().width(text)>available)owner.hoveredSourceLabel=shown;
        }
        if(count>=0)ScreenRenderCompat.translated(g,0.0F,-0.5F,
                ()->owner.draw(g,number,x+w-13-owner.renderFont().width(number),y+7,0xFFBCC5D3));
    }

    /** Full source names remain readable despite the owner.narrow fixed sidebar. */
    void renderSourceTooltip(
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
        if(owner.hoveredSourceLabel!=null)
            owner.nativeTooltip(g,owner.wrapped(owner.hoveredSourceLabel,Math.min(240,owner.screenWidth()-2*owner.MARGIN-20)));
    }

    private void drawSidebarHeader(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,String title,CarpetRuleSidebar.Section section,boolean group){
        int w=owner.sidebarWidth();
        boolean hover=owner.inside(owner.mouseX,owner.mouseY,x+5,y,w-10,23);
        boolean selectedParent=CarpetRuleSidebar.sectionFor(owner.sidebarKind)==section;
        CarpetRuleUiStyle.tabBackground(g,x+owner.SIDEBAR_TAB_INSET,y,
                x+w-owner.SIDEBAR_TAB_INSET,22,5,
                selectedParent,hover,0x4430353B);
        int titleX=x+12,titleW=w-(group?66:35);
        String shown=owner.fit(title,titleW);
        drawSidebarText(g,shown,titleX,y+8,
                selectedParent?0xFFF1FFF7:0xFFD6DBE6);
        CarpetRuleUiStyle.disclosureArrow(g,x+w-22,y+7,
                owner.isExpanded(section),0xFFC4C8D6);
        if(group){
            int buttonX=CarpetRuleSidebar.groupAddX(x,w),buttonY=y+2;
            if(owner.inside(owner.mouseX,owner.mouseY,buttonX,buttonY,20,18))
                owner.rounded(g,buttonX+1,buttonY,buttonX+19,buttonY+18,4,0xA0356B52);
            CarpetRuleUiStyle.glyph(g,"plus",buttonX+4,buttonY+3,0xFFF6E8FF);
        }
    }
}

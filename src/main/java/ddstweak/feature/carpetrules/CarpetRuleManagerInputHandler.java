package ddstweak.feature.carpetrules;

import ddstweak.platform.minecraft.MinecraftCompat;
//#if MC >= 260300
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif
import java.util.List;


/**
 * Rule list hit testing, scrolling and keyboard dismissal; native widget events stay in Screen.
 * 处理列表命中、滚动及键盘关闭；原版控件事件仍由 Screen 分发。
 */
final class CarpetRuleManagerInputHandler {
    private final CarpetRuleManagerScreen owner;
    CarpetRuleManagerInputHandler(CarpetRuleManagerScreen owner){this.owner=owner;}

    boolean insideInlineNumber(double x,double y){
        if(owner.inlineNumberKey==null||owner.inlineNumberBox==null||!owner.inlineNumberBox.visible)return false;
        List<CarpetRuleCatalog.RemoteRule> rules=owner.visible();
        for(int i=0;i<rules.size();i++)
            if(owner.inlineNumberKey.equals(rules.get(i).key()))
                return owner.inside(x,y,CarpetRuleListScroll.choiceX(
                        owner.listLeft()+owner.listWidth()-owner.CARD_RIGHT_INSET)+owner.rowValueShift(),
                        owner.cardScroll.top(owner.cardViewportTop(),i)+9,
                        CarpetRuleListScroll.CHOICE_WIDTH,18);
        return false;
    }

    boolean selectAt(double x,double y,int button){
        //#if MC >= 260300
        //$$ if(button!=InputConstants.MOUSE_BUTTON_LEFT)return false;
        //#else
        if(button!=0)return false;
        //#endif
        if(owner.editingGroups){
            int top=owner.LIST_TOP+53,capacity=Math.max(1,(owner.listHeight()-53)/owner.LIST_ROW);
            if(!owner.inside(x,y,owner.MARGIN,top,owner.screenWidth()-2*owner.MARGIN,owner.listHeight()-53))return false;
            List<String> options=owner.groupOptions();
            int index=owner.groupOffset+(int)(y-top)/owner.LIST_ROW;
            if(index<0||index>=options.size()||index>=owner.groupOffset+capacity)return false;
            owner.group=options.get(index);
            owner.groupNameBox.setValue(owner.group);
            owner.pendingGroupDeletion="";
            return true;
        }
        if((!owner.narrow()||owner.sidebarOverlay)&&owner.inside(x,y,owner.MARGIN,owner.LIST_TOP,owner.sidebarWidth(),owner.listHeight())){
            if(owner.inside(x,y,owner.MARGIN+6,owner.LIST_TOP+7,owner.sidebarWidth()-12,20)){
                owner.selectSidebar(owner.findSidebarEntry(CarpetRuleSidebar.Kind.ALL,""));
                return true;
            }
            if(owner.inside(x,y,owner.MARGIN+6,owner.LIST_TOP+34,owner.sidebarWidth()-12,23)){
                owner.toggleSidebarSection(CarpetRuleSidebar.Section.EXTENSIONS);
                return true;
            }
            int categoriesY=owner.categoryHeaderY();
            if(owner.inside(x,y,owner.MARGIN+6,categoriesY,owner.sidebarWidth()-12,23)){
                owner.toggleSidebarSection(CarpetRuleSidebar.Section.CATEGORIES);
                return true;
            }
            int groupY=owner.groupHeaderY();
            if(owner.inside(x,y,owner.MARGIN+6,groupY,owner.sidebarWidth()-12,23)){
                if(owner.inside(x,y,CarpetRuleSidebar.groupAddX(owner.MARGIN,owner.sidebarWidth()),
                        groupY+2,20,18))
                    MinecraftCompat.setScreen(owner.minecraftInstance(),new CarpetRulePresetScreen(owner));
                else owner.toggleSidebarSection(CarpetRuleSidebar.Section.GROUPS);
                return true;
            }
            int quick=owner.quickHeaderY();
            if(owner.inside(x,y,owner.MARGIN+6,quick+22,owner.sidebarWidth()-12,20)){
                owner.selectSidebar(owner.findSidebarEntry(CarpetRuleSidebar.Kind.FAVORITES,""));
                return true;
            }
            if(owner.inside(x,y,owner.MARGIN+6,quick+44,owner.sidebarWidth()-12,20)){
                CarpetRuleSidebar.Entry modified=owner.findSidebarEntry(CarpetRuleSidebar.Kind.MODIFIED,"");
                if(modified!=null&&modified.selectable())owner.selectSidebar(modified);
                else owner.feedback=owner.tr("dds-tweak.carpet_rules.sidebar.modified_unavailable");
                return true;
            }
            if(owner.inside(x,y,owner.MARGIN+6,quick+66,owner.sidebarWidth()-12,20)){
                CarpetRuleSidebar.Entry saved=owner.findSidebarEntry(CarpetRuleSidebar.Kind.SAVED_DEFAULTS,"");
                if(saved!=null&&saved.selectable())owner.selectSidebar(saved);
                else owner.feedback=owner.tr("dds-tweak.carpet_rules.sidebar.default_unavailable");
                return true;
            }
            if(owner.inside(x,y,owner.MARGIN+6,quick+88,owner.sidebarWidth()-12,20)){
                owner.openVanillaGameRules();
                return true;
            }
            for(CarpetRuleSidebar.Section section:new CarpetRuleSidebar.Section[]{
                    CarpetRuleSidebar.Section.EXTENSIONS,CarpetRuleSidebar.Section.CATEGORIES,
                    CarpetRuleSidebar.Section.GROUPS}){
                if(!owner.isExpanded(section))continue;
                int top=owner.sidebarChoiceTop(section),rows=owner.sidebarRows(section);
                if(rows==0||!owner.inside(x,y,owner.MARGIN+6,top,owner.sidebarWidth()-12,rows*22))continue;
                List<CarpetRuleSidebar.Entry> entries=owner.sidebarChoices(section);
                int index=owner.sidebarOffset(section)+(int)((y-top)/22);
                if(index>=0&&index<entries.size()){
                    CarpetRuleSidebar.Entry choice=entries.get(index);
                    if(choice.selectable())owner.selectSidebar(choice);
                }
                break;
            }
            return true;
        }
        if(owner.narrow()&&owner.sidebarOverlay){
            owner.sidebarOverlay=false;
            return true;
        }
        if(!owner.narrow()||owner.narrowDetails){
            CarpetRuleCatalog.RemoteRule detail=owner.detailRule();
            if(detail!=null&&owner.editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT
                    &&owner.definitionDefault(detail)!=null
                    &&owner.inside(x,y,owner.detailResetX(),owner.detailTargetY(),CarpetRuleUiStyle.TARGET_ACTION_SIZE,owner.FIELD_H)){
                owner.restoreDefinitionDefault();
                return true;
            }
            if(detail!=null&&owner.showDefaults()){
                if(!owner.vanillaMode()){
                    CarpetRuleCommands.Mode[] persistentModes={
                            CarpetRuleCommands.Mode.SET_DEFAULT,
                            CarpetRuleCommands.Mode.REMOVE_DEFAULT};
                    for(int i=0;i<persistentModes.length;i++)
                        if(owner.inside(x,y,owner.detailLeft()+12,owner.detailModeY()+6+i*24,
                                owner.detailWidth()-24,20)){
                            owner.selectMode(owner.editMode==persistentModes[i]
                                    ?CarpetRuleCommands.Mode.TEMPORARY:persistentModes[i]);
                            return true;
                        }
                }
            }
            if(detail!=null&&owner.isBoolean(detail)&&owner.editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT
                    &&owner.inside(x,y,owner.detailLeft()+12,owner.detailTargetY(),owner.detailWidth()-54,18)){
                owner.chooseBoolean(x<owner.detailLeft()+12+(owner.detailWidth()-54)/2);
                return true;
            }
        }
        if((!owner.narrow()||owner.narrowDetails)&&!owner.vanillaMode()&&owner.showPlanAssignment()){
            CarpetRuleCatalog.RemoteRule detail=owner.detailRule();
            if(detail!=null){
                List<CarpetRulePresetStore.Preset> assigned=owner.assignedPlans(detail.key());
                int top=owner.quickPlanTop(),cap=owner.quickPlanRows(),left=owner.detailLeft();
                if(cap>0&&owner.inside(x,y,left+12,top,owner.detailWidth()-24,cap*35)){
                    int row=(int)(y-top)/35,index=row+owner.quickPlanOffset;
                    if(index>=0&&index<assigned.size()
                            &&owner.inside(x,y,left+owner.detailWidth()-79,top+row*35+5,65,21)){
                        owner.quickApplyPlan(assigned.get(index).name());
                    }
                    return true;
                }
            }
        }
        if(owner.narrow()&&owner.narrowDetails){
            owner.narrowDetails=false;
            return true;
        }
        if(!owner.inside(x,y,owner.listLeft(),owner.LIST_TOP,owner.listWidth(),owner.listHeight()))return false;
        if(owner.inside(x,y,owner.searchIconX(),owner.searchIconY(),owner.SEARCH_ICON_SIZE,owner.SEARCH_ICON_SIZE)){
            owner.toggleSearch();
            return true;
        }
        List<CarpetRuleCatalog.RemoteRule> rules=owner.visible();
        int viewport=owner.cardViewportHeight();
        if(CarpetRuleListScroll.max(rules.size(),viewport)>0
                &&owner.inside(x,y,owner.listLeft()+owner.listWidth()-owner.SCROLLBAR_RIGHT_INSET-3,
                        owner.cardViewportTop()+2,9,Math.max(1,viewport-4))){
            owner.cardScroll.scrollToFraction((y-(owner.cardViewportTop()+2))/(double)Math.max(1,viewport-4),
                    rules.size(),viewport);
            return true;
        }
        int index=owner.cardScroll.hit(y,owner.cardViewportTop(),rules.size(),owner.cardViewportHeight());
        if(index<0)return false;
        CarpetRuleCatalog.RemoteRule entry=rules.get(index);
        int top=owner.cardScroll.top(owner.cardViewportTop(),index),
                right=owner.listLeft()+owner.listWidth()-owner.CARD_RIGHT_INSET;
        if(!owner.inside(x,y,owner.listLeft()+6,top,owner.listWidth()-owner.CARD_RIGHT_INSET-6,
                CarpetRuleListScroll.CARD_HEIGHT))
            return false;
        if(owner.inside(x,y,CarpetRuleListScroll.favoriteX(right),top+8,
                CarpetRuleListScroll.STAR_WIDTH,20)){
            owner.preferences.toggleFavorite(entry.key());
            owner.feedback=owner.preferences.save()?owner.tr("dds-tweak.carpet_rules.favorite_saved"):
                    owner.tr("dds-tweak.carpet_rules.presets.save_failed");
            return true;
        }
        if(!owner.vanillaMode()&&owner.inside(x,y,CarpetRuleListScroll.saveX(right),top+8,20,20)){
            owner.submitDefault(entry,CarpetRuleCommands.Mode.SET_DEFAULT);
            return true;
        }
        if(owner.isBoolean(entry)&&owner.inside(x,y,CarpetRuleListScroll.switchX(right)+owner.rowValueShift()-2,top+8,
                CarpetRuleListScroll.SWITCH_WIDTH+4,20)){
            owner.quickToggle(entry);
            return true;
        }
        int choiceLeft=CarpetRuleListScroll.choiceX(right)+owner.rowValueShift();
        if(owner.isNumberRule(entry)&&owner.inside(x,y,choiceLeft,top+8,
                CarpetRuleListScroll.CHOICE_WIDTH,20)){
            owner.beginInlineNumber(entry);
            return true;
        }
        if(owner.isChoiceRule(entry)&&owner.inside(x,y,choiceLeft,top+8,
                CarpetRuleListScroll.CHOICE_WIDTH,20)){
            owner.openChoices(entry,true,choiceLeft+CarpetRuleListScroll.CHOICE_WIDTH,
                    top+CarpetRuleListScroll.CARD_HEIGHT+2,0);
            return true;
        }
        owner.finishInlineNumber(true);
        owner.selected=entry.key();
        owner.editMode=CarpetRuleCommands.Mode.TEMPORARY;
        owner.narrowDetails=owner.narrow();
        owner.refreshTarget();
        return true;
    }

    boolean scroll(double x,double y,double amount){
        if(amount==0)return false;
        if(owner.defaultMenuRule!=null){owner.defaultMenuRule=null;return true;}
        if(owner.choiceRule!=null&&owner.inside(x,y,owner.choiceX,owner.choiceY,owner.choiceWidth,
                CarpetRuleChoiceMenu.height(CarpetRuleClientNetwork.suggestions(owner.choiceRule).size(),
                        owner.choiceVisibleRows))){
            int size=CarpetRuleClientNetwork.suggestions(owner.choiceRule).size();
            owner.choiceOffset=CarpetRuleChoiceMenu.clampOffset(
                    owner.choiceOffset+(amount<0?1:-1),size,owner.choiceVisibleRows);
            return true;
        }
        if(owner.choiceRule!=null)owner.choiceRule=null;
        if(!owner.narrow()||owner.narrowDetails){
            CarpetRuleCatalog.RemoteRule detail=owner.detailRule();
            if(detail!=null&&!owner.vanillaMode()&&owner.showPlanAssignment()
                    &&owner.inside(x,y,owner.detailLeft()+12,owner.quickPlanTop(),
                            owner.detailWidth()-24,owner.quickPlanRows()*35)){
                int count=owner.assignedPlans(detail.key()).size(),cap=owner.quickPlanRows();
                owner.quickPlanOffset=Math.max(0,Math.min(Math.max(0,count-cap),
                        owner.quickPlanOffset+(amount<0?1:-1)));
                return true;
            }
            if(owner.inside(x,y,owner.detailLeft()+12,owner.DESCRIPTION_TOP,owner.detailWidth()-24,owner.descriptionHeight())){
                if(detail!=null){
                    int lines=owner.wrapped(owner.ruleDescription(detail),owner.detailWidth()-30).size();
                    int visibleLines=Math.max(1,owner.descriptionHeight()/owner.DESCRIPTION_LINE_H);
                    owner.descriptionOffset=Math.max(0,Math.min(Math.max(0,lines-visibleLines),
                            owner.descriptionOffset+(amount<0?2:-2)));
                    return true;
                }
            }
        }
        if(owner.editingGroups){
            if(!owner.inside(x,y,owner.MARGIN,owner.LIST_TOP,owner.screenWidth()-2*owner.MARGIN,owner.listHeight()))return false;
            int capacity=Math.max(1,(owner.listHeight()-53)/owner.LIST_ROW);
            owner.groupOffset=Math.max(0,Math.min(Math.max(0,owner.groupOptions().size()-capacity),
                    owner.groupOffset+(amount<0?2:-2)));
            return true;
        }
        if((!owner.narrow()||owner.sidebarOverlay)&&owner.inside(x,y,owner.MARGIN,owner.LIST_TOP,owner.sidebarWidth(),owner.listHeight())){
            for(CarpetRuleSidebar.Section section:new CarpetRuleSidebar.Section[]{
                    CarpetRuleSidebar.Section.EXTENSIONS,CarpetRuleSidebar.Section.CATEGORIES,
                    CarpetRuleSidebar.Section.GROUPS}){
                int top=owner.sidebarChoiceTop(section),rows=owner.sidebarRows(section);
                if(!owner.isExpanded(section)||rows==0
                        ||!owner.inside(x,y,owner.MARGIN+6,top,owner.sidebarWidth()-12,rows*22))continue;
                int limit=Math.max(0,owner.sidebarChoices(section).size()-rows);
                owner.setSidebarOffset(section,Math.max(0,Math.min(limit,
                        owner.sidebarOffset(section)+(amount<0?2:-2))));
                return true;
            }
            return false;
        }
        if(owner.narrow()&&owner.sidebarOverlay)return false;
        if(owner.inside(x,y,owner.listLeft(),owner.LIST_TOP,owner.listWidth(),owner.listHeight())){
            owner.cardScroll.wheel(amount,owner.visible().size(),owner.cardViewportHeight());
            return true;
        }
        return false;
    }
    boolean onKey(boolean confirm,boolean escape){
        if(owner.inlineNumberKey!=null&&confirm){owner.finishInlineNumber(true);return true;}
        if(owner.inlineNumberKey!=null&&escape){owner.finishInlineNumber(false);return true;}
        if(!escape)return false;
        if(owner.defaultMenuRule!=null){owner.defaultMenuRule=null;return true;}
        if(owner.choiceRule!=null){owner.choiceRule=null;return true;}
        if(owner.searchOpen){owner.toggleSearch();return true;}
        if(owner.sidebarOverlay){owner.sidebarOverlay=false;return true;}
        if(owner.editingGroups){owner.toggleGroupEditor();return true;}
        if(owner.narrowDetails){owner.narrowDetails=false;return true;}
        return false;
    }
}

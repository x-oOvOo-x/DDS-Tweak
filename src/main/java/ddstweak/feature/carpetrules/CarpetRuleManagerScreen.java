package ddstweak.feature.carpetrules;

import ddstweak.gui.GuiIconRenderer;
import ddstweak.gui.SharedScreenBackground;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 260300
//$$ import fi.dy.masa.malilib.gui.MaLiLibIcons;
//$$ import fi.dy.masa.malilib.render.GuiContext;
//#endif
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Server-synchronized Carpet rule browser with separately reviewed temporary
 * and persistent changes. Local metadata cannot invent server rules.
 */
public final class CarpetRuleManagerScreen extends Screen {
    static final int MARGIN=14, LIST_ROW=42, LIST_TOP=50, FOOTER=28, FIELD_H=18;
    private static final int CARD_TOP_INSET=36,CARD_BOTTOM_INSET=9;
    // Exact left/right edges of the individual rounded sidebar tab, not the parent panel.
    static final int SIDEBAR_TAB_INSET=6;
    static final int CARD_RIGHT_INSET=18,SCROLLBAR_RIGHT_INSET=10;
    static final int SEARCH_ICON_SIZE=18, DESCRIPTION_LINE_H=CarpetRuleDetailLayout.LINE_HEIGHT;
    static final int DESCRIPTION_TOP=CarpetRuleDetailLayout.descriptionTop(LIST_TOP);
    static final float DETAIL_TITLE_SCALE=1.16F,DETAIL_LABEL_SCALE=1.10F;
    private final Screen parent;
    private final CarpetRuleManagerRenderer renderer=new CarpetRuleManagerRenderer(this);
    private final CarpetRuleManagerInputHandler inputHandler=new CarpetRuleManagerInputHandler(this);
    CarpetRuleMetadata metadata = CarpetLocalMetadata.discover();
    private String metadataLanguage=CarpetRuleTranslations.language();
    private long nextMetadataRefreshNs;
    final CarpetRulePreferences preferences = CarpetRulePreferences.load();
    private CarpetRulePresetStore plans;
    private EditBox searchBox;
    EditBox groupNameBox;
    private Button applyButton,confirmDefaultButton,createGroupButton,renameGroupButton,deleteGroupButton,membershipButton;
    boolean editingGroups;
    int groupOffset;
    String pendingGroupDeletion="";
    CarpetRuleCatalog.RuleKey managedRule;
    EditBox targetBox,inlineNumberBox;
    CarpetRuleCatalog.RuleKey inlineNumberKey;
    private Button groupAssignButton,emptyPlanButton,choiceButton;
    CarpetRuleCommands.Mode editMode=CarpetRuleCommands.Mode.TEMPORARY;
    private final CarpetRulePersistentChange persistentDraft=new CarpetRulePersistentChange();
    final CarpetRuleChanges drafts=new CarpetRuleChanges();
    CarpetRuleOperationTracker.Operation lastOperation;
    private boolean reviewing;
    String feedback="";
    private String query="";
    boolean searchOpen;
    private String vanillaDraftId,vanillaDraftOld,vanillaDraftTarget;
    private boolean vanillaReadyWas;
    private String manager="";
    private boolean choosingManager;
    private String savedRuleQuery="";
    private int managerOffset;
    double mouseX=-1,mouseY=-1;
    String hoveredSourceLabel;
    CarpetRuleCatalog.RemoteRule hoveredRule,hoveredDefaultRule,hoveredFavoriteRule;
    CarpetRuleCatalog.RuleKey choiceRule,defaultMenuRule;
    private boolean choiceFromList;
    private int defaultMenuX,defaultMenuY,defaultMenuWidth;
    int choiceX,choiceY,choiceWidth,choiceOffset,choiceVisibleRows,descriptionOffset;
    private String category="";
    String group="";
    private boolean favoritesOnly;
    CarpetRuleCatalog.RuleKey selected;
    private int offset;
    final CarpetRuleListScroll cardScroll=new CarpetRuleListScroll();
    CarpetRuleSidebar.Kind sidebarKind=CarpetRuleSidebar.Kind.ALL;
    String sidebarId="";
    int sourceChoiceOffset,categoryChoiceOffset,groupChoiceOffset,quickPlanOffset;
    CarpetRuleCatalog.RuleKey quickPlanRule;
    private boolean sourcesExpanded=true,categoriesExpanded,groupsExpanded;
    boolean sidebarOverlay;
    boolean narrowDetails;
    private long observedGeneration=CarpetRuleClientNetwork.catalog().generation();
    private boolean renderFrameActive;
    private List<CarpetRuleCatalog.RemoteRule> frameUiRules,frameVisibleRules;
    private List<CarpetRuleSidebar.Entry> frameCategoryEntries,frameSidebarEntries;
    private Set<CarpetRuleCatalog.RuleKey> frameSavedDefaults;
    private Boolean frameDefaultsAvailable;

    public CarpetRuleManagerScreen(Screen parent) {
        super(MinecraftCompat.translatable("dds-tweak.carpet_rules.title"));
        this.parent=parent;
    }

    @Override public boolean isPauseScreen(){return false;}

    private void beginRenderFrame(){
        refreshLocalMetadata();
        renderFrameActive=true;
        frameUiRules=null;
        frameVisibleRules=null;
        frameCategoryEntries=null;
        frameSidebarEntries=null;
        frameSavedDefaults=null;
        frameDefaultsAvailable=null;
        if(!editingGroups)ensureSelectedRule();
        refreshOperationFeedback();
    }

    /** A screen opened before CDA registered its rules must recover without restart. */
    private void refreshLocalMetadata(){
        long now=System.nanoTime();
        if(now<nextMetadataRefreshNs)return;
        List<CarpetRuleCatalog.RemoteRule> rules=CarpetRuleClientNetwork.catalog().snapshot();
        if(rules.isEmpty())return;
        nextMetadataRefreshNs=now+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        CarpetRuleMetadata refreshed=CarpetLocalMetadata.discover();
        String language=CarpetRuleTranslations.language();
        // Preserve fuller results on partial joins, but honor explicit locale changes.
        if(!language.equals(metadataLanguage)
                ||metadataCoverage(refreshed,rules)>=metadataCoverage(metadata,rules)){
            metadata=refreshed;
            metadataLanguage=language;
        }
    }

    private static int metadataCoverage(CarpetRuleMetadata source,
                                        List<CarpetRuleCatalog.RemoteRule> rules){
        int score=0;
        for(CarpetRuleCatalog.RemoteRule rule:rules){
            CarpetRuleMetadata.Description description=source.find(rule.key());
            if(description==null)continue;
            if(!rule.key().name().equals(description.displayName()))score+=2;
            if(!description.detail().isEmpty())score++;
        }
        return score;
    }

    private void ensureSelectedRule(){
        if(selected!=null)return;
        List<CarpetRuleCatalog.RemoteRule> rules=visible();
        if(!rules.isEmpty())selected=rules.get(0).key();
    }

    /** Keep operation expiry and feedback updates in the Screen that owns UI state.
     * 操作过期与状态提示更新由持有界面状态的 Screen 负责。
     */
    private void refreshOperationFeedback(){
        if(editingGroups)return;
        CarpetRuleClientNetwork.operations().expire(System.currentTimeMillis());
        if(lastOperation==null)return;
        CarpetRuleCatalog.RemoteRule currentDetail=detailRule();
        if(currentDetail==null||!currentDetail.key().equals(lastOperation.key())){
            lastOperation=null;
        }else if(CarpetRuleClientNetwork.operations().find(lastOperation.id())!=lastOperation){
            lastOperation=null;
            feedback="";
        }else feedback=tr("dds-tweak.carpet_rules.operation")+": "+
                tr("dds-tweak.carpet_rules.status."+
                lastOperation.status().name().toLowerCase(Locale.ROOT));
    }

    private void endRenderFrame(){
        renderFrameActive=false;
        frameUiRules=null;
        frameVisibleRules=null;
        frameCategoryEntries=null;
        frameSidebarEntries=null;
        frameSavedDefaults=null;
        frameDefaultsAvailable=null;
    }

    private boolean defaultsAvailable(){
        if(renderFrameActive&&frameDefaultsAvailable!=null)return frameDefaultsAvailable;
        boolean available=CarpetRuleClientNetwork.defaultsAvailable();
        if(renderFrameActive)frameDefaultsAvailable=available;
        return available;
    }

    private Set<CarpetRuleCatalog.RuleKey> savedDefaults(){
        if(renderFrameActive&&frameSavedDefaults!=null)return frameSavedDefaults;
        Set<CarpetRuleCatalog.RuleKey> saved=CarpetRuleClientNetwork.savedDefaults();
        if(renderFrameActive)frameSavedDefaults=saved;
        return saved;
    }

    @Override protected void init(){
        plans=CarpetRulePresetStore.load();
        int searchX=listLeft()+6+SEARCH_ICON_SIZE+8;
        int searchWidth=Math.max(36,listWidth()-(searchX-listLeft())-CARD_RIGHT_INSET);
        //#if MC >= 11600
        searchBox = new EditBox(font,searchX,LIST_TOP+7,searchWidth,FIELD_H,MinecraftCompat.literal(""));
        //#else
        //$$ searchBox = new EditBox(font,searchX,LIST_TOP+7,searchWidth,FIELD_H,"");
        //#endif
        searchBox.setMaxLength(120);
        searchBox.setValue(query);
        searchBox.setResponder(v->{query=v;cardScroll.reset();});
        addCompat(searchBox);
        if(narrow())addCompat(button(width-MARGIN-98,LIST_TOP+6,98,
                tr("dds-tweak.carpet_rules.sidebar.open"),()->sidebarOverlay=!sidebarOverlay));
        addCompat(button(width-MARGIN-197,12,62,tr("dds-tweak.carpet_rules.history.tab"),
                ()->MinecraftCompat.setScreen(minecraft,new CarpetRuleHistoryScreen(this))));
        addCompat(button(width-MARGIN-131,12,62,tr("dds-tweak.carpet_rules.presets.tab"),
                ()->MinecraftCompat.setScreen(minecraft,new CarpetRulePresetScreen(this))));
        addCompat(button(width-MARGIN-65,12,62,tr("dds-tweak.carpet_rules.back"),
                this::onClose));
        int detailLeft=detailLeft();
        int detailWidth=detailWidth();
        //#if MC >= 11600
        targetBox=new EditBox(font,detailLeft+12,detailTargetY(),Math.max(30,detailWidth-80),FIELD_H,MinecraftCompat.literal(""));
        //#else
        //$$ targetBox=new EditBox(font,detailLeft+12,detailTargetY(),Math.max(30,detailWidth-80),FIELD_H,"");
        //#endif
        targetBox.setMaxLength(2048);
        CarpetRuleCatalog.RemoteRule chosen=detailRule();
        targetBox.setValue(chosen==null?"":chosen.value());
        targetBox.setCursorPosition(0);
        targetBox.setHighlightPos(0);
        targetBox.setResponder(v->reviewing=false);
        addCompat(targetBox);
        //#if MC >= 11600
        inlineNumberBox=new EditBox(font,listLeft()+10,cardViewportTop(),
                CarpetRuleListScroll.CHOICE_WIDTH,18,
                MinecraftCompat.literal(""));
        //#else
        //$$ inlineNumberBox=new EditBox(font,listLeft()+10,cardViewportTop(),
        //$$         CarpetRuleListScroll.CHOICE_WIDTH,18,"");
        //#endif
        inlineNumberBox.setMaxLength(64);
        inlineNumberBox.setResponder(v->reviewing=false);
        inlineNumberBox.visible=false;
        addCompat(inlineNumberBox);
        choiceButton=button(detailChoiceX(),detailTargetY(),
                CarpetRuleUiStyle.TARGET_ACTION_SIZE,"▾",this::openValuePicker);
        addPainted(choiceButton);
        groupAssignButton=button(detailLeft+12,detailGroupAssignY(),Math.max(50,detailWidth-24),
                tr("dds-tweak.carpet_rules.plans.add_to_plan"),this::openGroupAssignment);
        addCompat(groupAssignButton);
        emptyPlanButton=button(listLeft()+12,cardViewportTop()+63,
                Math.max(50,Math.min(240,listWidth()-24)),
                tr("dds-tweak.carpet_rules.plans.open_to_add"),this::openGroupAssignment);
        addCompat(emptyPlanButton);
        //#if MC >= 11600
        groupNameBox=new EditBox(font,MARGIN+4,LIST_TOP+4,Math.max(30,width-2*MARGIN-8),FIELD_H,MinecraftCompat.literal(""));
        //#else
        //$$ groupNameBox=new EditBox(font,MARGIN+4,LIST_TOP+4,Math.max(30,width-2*MARGIN-8),FIELD_H,"");
        //#endif
        groupNameBox.setMaxLength(64);
        addCompat(groupNameBox);
        int groupWidth=Math.max(30,(width-2*MARGIN-12)/4);
        createGroupButton=button(MARGIN,LIST_TOP+27,groupWidth,tr("dds-tweak.carpet_rules.groups.create"),this::createGroup);
        renameGroupButton=button(MARGIN+groupWidth+4,LIST_TOP+27,groupWidth,tr("dds-tweak.carpet_rules.groups.rename"),this::renameGroup);
        deleteGroupButton=button(MARGIN+2*(groupWidth+4),LIST_TOP+27,groupWidth,tr("dds-tweak.carpet_rules.groups.delete"),this::deleteGroup);
        membershipButton=button(MARGIN+3*(groupWidth+4),LIST_TOP+27,groupWidth,tr("dds-tweak.carpet_rules.groups.toggle"),this::toggleMembership);
        addCompat(createGroupButton);
        addCompat(renameGroupButton);
        addCompat(deleteGroupButton);
        addCompat(membershipButton);
        int footerLeft=detailLeft()+12,footerWidth=detailWidth()-24;
        int footerY=LIST_TOP+listHeight()-29;
        applyButton=button(footerLeft,footerY,footerWidth,
                tr("dds-tweak.carpet_rules.temp_apply"),this::confirmChange);
        confirmDefaultButton=button(footerLeft,footerY,footerWidth,
                tr("dds-tweak.carpet_rules.default_confirm"),this::confirmChange);
        addCompat(applyButton);
        addCompat(confirmDefaultButton);
        setFocused(null);
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
    /** Interaction-only targets for custom-rendered icons and choice controls. */
    private void addPainted(Button widget){
        //#if MC >= 11600
        addWidget(widget);
        //#else
        //$$ children.add(widget);
        //#endif
    }



    /** Search icon is always visible; clicking it toggles the otherwise hidden input. */
    int searchIconX(){return listLeft()+6;}
    int searchIconY(){return LIST_TOP+7;}
    int descriptionHeight(){return CarpetRuleDetailLayout.descriptionHeight(listHeight());}
    int detailValuesY(){return CarpetRuleDetailLayout.valuesTop(LIST_TOP,listHeight());}
    int detailTargetY(){return CarpetRuleDetailLayout.targetTop(LIST_TOP,listHeight());}
    // Reserve room for both mode rows, the status line and the footer action.
    // 为两行模式选项、状态提示及底部操作按钮预留空间。
    boolean showDefaults(){return CarpetRuleDetailLayout.modeOptionsFit(listHeight());}
    boolean showPlanAssignment(){return listHeight()>detailGroupAssignY()-LIST_TOP+90;}
    int detailResetX(){
        return detailLeft()+detailWidth()-CarpetRuleUiStyle.TARGET_RIGHT_PADDING
                -CarpetRuleUiStyle.TARGET_ACTION_SIZE;
    }
    private int detailChoiceX(){
        return detailResetX()-CarpetRuleUiStyle.TARGET_ACTION_GAP
                -CarpetRuleUiStyle.TARGET_ACTION_SIZE;
    }
    /** Vanilla gamerules lack the persisted-default action, freeing a 24px column. */
    int rowValueShift(){return CarpetRuleListScroll.valueShift(vanillaMode());}
    String definitionDefault(CarpetRuleCatalog.RemoteRule rule){
        if(vanillaMode()){
            VanillaGameRules.Entry vanilla=VanillaGameRules.get(rule.key());
            return vanilla==null?null:vanilla.defaultValue;
        }
        return CarpetRuleClientNetwork.definitionDefault(rule.key());
    }
    void drawResetGlyph(
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
        CarpetRuleUiStyle.resetGlyph(g,x,y,color);
    }
    int detailModeY(){return CarpetRuleDetailLayout.modeTop(LIST_TOP,listHeight());}
    int detailGroupAssignY(){return detailModeY()+129;}
    void toggleSearch(){
        searchOpen=!searchOpen;
        if(!searchOpen&&searchBox!=null){
            searchBox.setValue("");
            setFocused(null);
        }else if(searchBox!=null){
            setFocused(searchBox);
        }
    }

    private void nextManager(){
        if(editingGroups)editingGroups=false;
        choosingManager=!choosingManager;
        managerOffset=0;
        if(choosingManager) {
            savedRuleQuery=query;
            searchBox.setValue("");
        } else searchBox.setValue(savedRuleQuery);
    }

    private List<String> matchingManagers(){
        String needle=query.trim().toLowerCase(Locale.ROOT);
        List<String> filtered=new ArrayList<>();
        for(String id:managers())
            if(id.isEmpty()||id.toLowerCase(Locale.ROOT).contains(needle))filtered.add(id);
        return filtered;
    }

    private void nextCategory() {
        SortedSet<String> categories = new TreeSet<>();
        for (CarpetRuleCatalog.RemoteRule rule : CarpetRuleClientNetwork.catalog().snapshot()) {
            CarpetRuleMetadata.Description info=metadata.find(rule.key());
            if(info!=null)categories.addAll(info.categories());
        }
        List<String> all=new ArrayList<>();
        all.add("");
        all.addAll(categories);
        int index=all.indexOf(category)+1;
        category=index>=all.size()?"":all.get(index);
        offset=0;
        selected=null;
        refreshTarget();
    }

    private void openGroupAssignment(){
        CarpetRuleCatalog.RemoteRule rule=detailRule();
        String proposed=rule==null?null:targetBox==null?rule.value():targetBox.getValue();
        MinecraftCompat.setScreen(minecraft,
                new CarpetRulePresetScreen(this,rule==null?null:rule.key(),proposed,
                        sidebarKind==CarpetRuleSidebar.Kind.PLAN?sidebarId:null));
    }

    void openVanillaGameRules(){
        CarpetRuleSidebar.Entry game=findSidebarEntry(CarpetRuleSidebar.Kind.GAME_RULES,"");
        if(game==null)return;
        selectSidebar(game);
        VanillaGameRules.request(minecraft);
        if(!VanillaGameRules.supported())
            feedback=tr("dds-tweak.carpet_rules.sidebar.vanilla_game_rules_unavailable");
        // A read-only local listing is valid even without /gamerule permission.
        // Show the permission error only if an actual modification is attempted.
    }

    boolean vanillaMode(){return sidebarKind==CarpetRuleSidebar.Kind.GAME_RULES;}
    private void clearVanillaDraft(){
        vanillaDraftId=null;vanillaDraftOld=null;vanillaDraftTarget=null;
    }
    String vanillaDesired(CarpetRuleCatalog.RemoteRule rule){
        return rule!=null&&rule.key().name().equals(vanillaDraftId)?vanillaDraftTarget:null;
    }

    void toggleGroupEditor(){
        if(choosingManager){
            choosingManager=false;
            searchBox.setValue(savedRuleQuery);
        }
        editingGroups=!editingGroups;
        reviewing=false;
        pendingGroupDeletion="";
        if(editingGroups){
            CarpetRuleCatalog.RemoteRule current=detailRule();
            managedRule=current==null?null:current.key();
            groupOffset=0;
            groupNameBox.setValue(group);
        }else refreshTarget();
    }

    private void createGroup(){
        String name=groupNameBox.getValue();
        if(!preferences.createGroup(name)){feedback=tr("dds-tweak.carpet_rules.groups.invalid");return;}
        group=name;
        pendingGroupDeletion="";
        feedback=preferences.save()?tr("dds-tweak.carpet_rules.groups.saved"):
                tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    private void renameGroup(){
        String name=groupNameBox.getValue();
        if(!preferences.renameGroup(group,name)){feedback=tr("dds-tweak.carpet_rules.groups.invalid");return;}
        group=name;
        pendingGroupDeletion="";
        feedback=preferences.save()?tr("dds-tweak.carpet_rules.groups.saved"):
                tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    private void deleteGroup(){
        if(group.isEmpty())return;
        if(!group.equals(pendingGroupDeletion)){
            pendingGroupDeletion=group;
            feedback=tr("dds-tweak.carpet_rules.groups.confirm");
            return;
        }
        preferences.removeGroup(group);
        group="";
        groupNameBox.setValue("");
        pendingGroupDeletion="";
        feedback=preferences.save()?tr("dds-tweak.carpet_rules.groups.saved"):
                tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    private void toggleMembership(){
        if(group.isEmpty()||managedRule==null){
            feedback=tr("dds-tweak.carpet_rules.groups.select_rule");
            return;
        }
        boolean include=!preferences.inGroup(group,managedRule);
        if(!preferences.setGroupMembership(group,managedRule,include))return;
        feedback=preferences.save()?tr("dds-tweak.carpet_rules.groups.saved"):
                tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    List<String> groupOptions(){
        List<String> names=new ArrayList<>();
        names.add("");
        names.addAll(preferences.groupNames());
        return names;
    }

    CarpetRuleCatalog.RemoteRule detailRule(){
        List<CarpetRuleCatalog.RemoteRule> rules=visible();
        if(selected!=null)for(CarpetRuleCatalog.RemoteRule r:rules)
            if(selected.equals(r.key()))return r;
        return rules.isEmpty()?null:rules.get(0);
    }

    boolean isBoolean(CarpetRuleCatalog.RemoteRule rule){
        if(rule==null)return false;
        if(vanillaMode()){
            VanillaGameRules.Entry vanilla=VanillaGameRules.get(rule.key());
            return vanilla!=null&&vanilla.booleanType;
        }
        return CarpetRuleValueTypes.booleanEditor(rule.value(),
                CarpetRuleClientNetwork.definitionDefault(rule.key()),
                CarpetRuleClientNetwork.suggestions(rule.key()));
    }

    private boolean booleanRule(){
        CarpetRuleCatalog.RemoteRule rule=detailRule();
        return isBoolean(rule);
    }

    /** One click changes only the live value; server synchronization owns the displayed state. */
    private boolean submitTemporary(CarpetRuleCatalog.RemoteRule rule,String target){
        if(rule==null||target==null||target.equals(rule.value())){
            feedback=tr("dds-tweak.carpet_rules.no_draft");
            return false;
        }
        if(vanillaMode()){
            VanillaGameRules.Entry info=VanillaGameRules.get(rule.key());
            if(info==null||!VanillaGameRules.permitted(minecraft)
                    ||!VanillaGameRules.valid(info,target)){
                feedback=tr("dds-tweak.carpet_rules.vanilla.permission");
                return false;
            }
            boolean sent=VanillaGameRules.submit(minecraft,info,info.value,target);
            feedback=tr(sent?"dds-tweak.carpet_rules.vanilla.sent":
                    "dds-tweak.carpet_rules.conflict");
            if(sent){clearVanillaDraft();VanillaGameRules.request(minecraft);}
            return sent;
        }
        if(!rule.managerExplicit()){
            feedback=tr("dds-tweak.carpet_rules.unavailable");
            return false;
        }
        CarpetRuleMultiValue multi=CarpetRuleClientNetwork.multiValue(rule.key());
        if(multi!=null&&multi.parse(target)==null){
            feedback=tr("dds-tweak.carpet_rules.invalid_combination");
            return false;
        }
        CarpetRuleOperationTracker.Operation operation=CarpetRuleClientNetwork.submit(
                rule.key(),CarpetRuleCommands.Mode.TEMPORARY,target);
        if(operation==null){
            feedback=tr("dds-tweak.carpet_rules.unavailable");
            return false;
        }
        lastOperation=operation;
        drafts.remove(rule.key());
        // Keep the edited target consistent with the submitted temporary value.
        if(rule.key().equals(selected)&&targetBox!=null)targetBox.setValue(target);
        feedback=tr("dds-tweak.carpet_rules.sent");
        return true;
    }

    void quickToggle(CarpetRuleCatalog.RemoteRule rule){
        selected=rule.key();
        editMode=CarpetRuleCommands.Mode.TEMPORARY;
        if(narrow())narrowDetails=false;
        String pending=temporaryInFlight(rule.key());
        if(pending!=null){
            // A second click must not briefly reset the detail editor to the
            // old authoritative value while the list still shows the target.
            if(targetBox!=null)targetBox.setValue(pending);
            return;
        }
        refreshTarget();
        submitTemporary(rule,Boolean.toString(!Boolean.parseBoolean(rule.value())));
    }

    void chooseBoolean(boolean value){
        CarpetRuleCatalog.RemoteRule rule=detailRule();
        if(rule==null||targetBox==null)return;
        String target=Boolean.toString(value);
        if(editMode==CarpetRuleCommands.Mode.SET_DEFAULT){
            // Selecting a persistent target must not dispatch a temporary command.
            // 持久化模式先编辑目标值，用户确认后才提交 setDefault。
            targetBox.setValue(target);
            stage();
            return;
        }
        if(editMode!=CarpetRuleCommands.Mode.TEMPORARY)return;
        targetBox.setValue(target);
        submitTemporary(rule,target);
    }

    private void openValuePicker(){
        CarpetRuleCatalog.RemoteRule current=detailRule();
        if(current==null)return;
        List<String> values=CarpetRuleClientNetwork.suggestions(current.key());
        openChoices(current,false,detailLeft()+detailWidth()-12,
                detailTargetY()+FIELD_H+2,0);
    }

    void restoreDefinitionDefault(){
        CarpetRuleCatalog.RemoteRule current=detailRule();
        if(current==null||targetBox==null)return;
        VanillaGameRules.Entry vanilla=vanillaMode()?VanillaGameRules.get(current.key()):null;
        String original=vanillaMode()?vanilla==null?null:vanilla.defaultValue
                :CarpetRuleClientNetwork.definitionDefault(current.key());
        if(original==null){
            feedback=tr("dds-tweak.carpet_rules.default_unknown");
            return;
        }
        if(editMode==CarpetRuleCommands.Mode.REMOVE_DEFAULT)
            selectMode(CarpetRuleCommands.Mode.TEMPORARY);
        targetBox.setValue(original);
        targetBox.setCursorPosition(0);
        targetBox.setHighlightPos(0);
        if(editMode==CarpetRuleCommands.Mode.SET_DEFAULT){
            stage();
            return;
        }
        if(isBoolean(current)||isChoiceRule(current)
                &&CarpetRuleClientNetwork.multiValue(current.key())==null)
            submitTemporary(current,original);
        else stage();
    }

    boolean isNumberRule(CarpetRuleCatalog.RemoteRule rule){
        return rule!=null&&!isBoolean(rule)
                &&CarpetRuleNumericValue.editable(rule.value(),
                        definitionDefault(rule),
                        !CarpetRuleClientNetwork.suggestions(rule.key()).isEmpty());
    }

    void beginInlineNumber(CarpetRuleCatalog.RemoteRule rule){
        finishInlineNumber(false);
        if(!isNumberRule(rule)||inlineNumberBox==null)return;
        selected=rule.key();
        editMode=CarpetRuleCommands.Mode.TEMPORARY;
        refreshTarget();
        inlineNumberKey=rule.key();
        String pending=drafts.desired(rule.key());
        inlineNumberBox.setValue(pending==null?rule.value():pending);
        inlineNumberBox.setCursorPosition(0);
        inlineNumberBox.setHighlightPos(0);
        reviewing=false;
        setFocused(inlineNumberBox);
    }

    /** Enter sends one temporary command; reject invalid input without closing the editor. */
    boolean finishInlineNumber(boolean save){
        if(inlineNumberKey==null||inlineNumberBox==null)return true;
        CarpetRuleCatalog.RuleKey key=inlineNumberKey;
        String value=inlineNumberBox.getValue();
        if(save){
            if(!CarpetRuleNumericValue.valid(value)){
                feedback=tr("dds-tweak.carpet_rules.number_invalid");
                return false;
            }
            CarpetRuleCatalog.RemoteRule rule=detailRule();
            if(rule==null||!rule.key().equals(key)){
                rule=vanillaMode()?null:CarpetRuleClientNetwork.catalog().get(key);
            }
            if(rule==null||!submitTemporary(rule,value)){
                if(rule!=null&&value.equals(rule.value()))save=false;
                else return false;
            }
        }
        inlineNumberKey=null;
        inlineNumberBox.visible=false;
        setFocused(null);
        if(save){
            selected=key;
            editMode=CarpetRuleCommands.Mode.TEMPORARY;
            if(targetBox!=null){targetBox.setValue(value);targetBox.setCursorPosition(0);}
        }
        return true;
    }

    void positionInlineNumber(List<CarpetRuleCatalog.RemoteRule> rules){
        if(inlineNumberBox==null)return;
        inlineNumberBox.visible=false;
        if(inlineNumberKey==null)return;
        for(int i=0;i<rules.size();i++){
            if(!inlineNumberKey.equals(rules.get(i).key()))continue;
            int y=cardScroll.top(cardViewportTop(),i)+9;
            if(y<cardViewportTop()||y+18>cardViewportBottom())return;
            int right=listLeft()+listWidth()-CARD_RIGHT_INSET;
            inlineNumberBox.setX(CarpetRuleListScroll.choiceX(right)+rowValueShift());
            inlineNumberBox.setWidth(CarpetRuleListScroll.CHOICE_WIDTH);
            //#if MC >= 11903
            inlineNumberBox.setY(y);
            //#else
            //$$ inlineNumberBox.y=y;
            //#endif
            inlineNumberBox.visible=true;
            return;
        }
    }

    boolean isChoiceRule(CarpetRuleCatalog.RemoteRule rule){
        return rule!=null&&!isBoolean(rule)
                &&!CarpetRuleClientNetwork.suggestions(rule.key()).isEmpty();
    }

    /** One in-screen dropdown for detail and list cards; never sends a command. */
    void openChoices(CarpetRuleCatalog.RemoteRule rule,boolean fromList,
                             int x,int y,int menuWidth){
        if(!isChoiceRule(rule))return;
        if(rule.key().equals(choiceRule)&&choiceFromList==fromList){
            choiceRule=null;
            return;
        }
        List<String> options=CarpetRuleClientNetwork.suggestions(rule.key());
        choiceRule=rule.key();
        choiceFromList=fromList;
        choiceOffset=0;
        int longest=0;
        for(String value:options)longest=Math.max(longest,font.width(value));
        int requested=Math.max(90,longest+(CarpetRuleClientNetwork.multiValue(rule.key())!=null?39:20));
        choiceWidth=Math.min(requested,Math.max(60,width-2*MARGIN));
        // x is the right edge of the trigger; grow leftward instead of covering the star.
        choiceX=Math.max(MARGIN,Math.min(x-choiceWidth,width-MARGIN-choiceWidth));
        int maxRows=Math.max(1,(height-LIST_TOP-2*MARGIN-8)/CarpetRuleChoiceMenu.ROW_HEIGHT);
        choiceVisibleRows=CarpetRuleChoiceMenu.rows(options.size(),maxRows);
        int totalHeight=CarpetRuleChoiceMenu.height(options.size(),choiceVisibleRows);
        choiceY=y+totalHeight>height-MARGIN?Math.max(LIST_TOP,y-totalHeight-23):y;
    }

    private boolean chooseFromPopup(double x,double y){
        if(choiceRule==null)return false;
        List<String> options=CarpetRuleClientNetwork.suggestions(choiceRule);
        int h=CarpetRuleChoiceMenu.height(options.size(),choiceVisibleRows);
        if(inside(x,y,choiceX,choiceY,choiceWidth,h)){
            int index=CarpetRuleChoiceMenu.hit((int)y,choiceY,choiceOffset,options.size(),choiceVisibleRows);
            if(index>=0){
                String option=options.get(index);
                CarpetRuleCatalog.RemoteRule rule=CarpetRuleClientNetwork.catalog().get(choiceRule);
                boolean fromList=choiceFromList;
                CarpetRuleMultiValue multi=rule==null?null:CarpetRuleClientNetwork.multiValue(rule.key());
                String chosen=option;
                if(multi!=null){
                    String before=fromList?drafts.desired(rule.key()):targetBox.getValue();
                    if(before==null)before=rule.value();
                    chosen=multi.toggle(before,option);
                    if(chosen==null){
                        feedback=tr("dds-tweak.carpet_rules.unavailable");
                        return true; // Preserve an unrecognized server value; don't coerce to none.
                    }
                }
                if(multi==null)choiceRule=null;
                if(rule!=null&&targetBox!=null){
                    if(fromList&&(!rule.key().equals(selected)
                            ||editMode!=CarpetRuleCommands.Mode.TEMPORARY)){
                        selected=rule.key();
                        editMode=CarpetRuleCommands.Mode.TEMPORARY;
                        if(narrow())narrowDetails=false;
                        CarpetRuleCatalog.RuleKey keep=choiceRule;
                        refreshTarget();
                        choiceRule=keep;
                    }
                    CarpetRuleCatalog.RemoteRule detail=detailRule();
                    if(detail!=null&&rule.key().equals(detail.key())){
                        targetBox.setValue(chosen);
                        targetBox.setCursorPosition(0);
                        if(multi!=null)stage(); // One combined command after Apply, never per checkbox.
                        else submitTemporary(rule,chosen); // One enum choice = one live update.
                    }
                }
            }else choiceRule=null;
        }else choiceRule=null;
        return true; // consume the closing click; do not activate a control behind the menu
    }

    String ruleDescription(CarpetRuleCatalog.RemoteRule rule){
        if(vanillaMode()){
            VanillaGameRules.Entry entry=VanillaGameRules.get(rule.key());
            return entry==null?"":entry.description;
        }
        CarpetRuleMetadata.Description info=metadata.find(rule.key());
        return info==null?"":info.detail();
    }

    List<String> wrapped(String value,int maxWidth){
        List<String> lines=new ArrayList<>();
        if(value==null||value.isEmpty())return lines;
        int limit=Math.max(20,maxWidth);
        for(String paragraph:value.replace("\r","").split("\n",-1)){
            StringBuilder current=new StringBuilder();
            for(int i=0;i<paragraph.length();i++){
                String next=paragraph.substring(i,i+1);
                if(current.length()>0&&font.width(current.toString()+next)>limit){
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(next);
            }
            lines.add(current.toString());
        }
        return lines;
    }

    private void renderOverlays(
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
        if(choiceButton!=null&&choiceButton.visible){
            int x=detailChoiceX(),y=detailTargetY();
            CarpetRuleUiStyle.dropdown(g,font,x,y,
                    CarpetRuleUiStyle.TARGET_ACTION_SIZE,FIELD_H,
                    inside(mouseX,mouseY,x,y,CarpetRuleUiStyle.TARGET_ACTION_SIZE,FIELD_H)
                            ||choiceButton.isFocused(),choiceButton.active);
        }
        if(defaultMenuRule!=null){renderDefaultMenu(g);return;}
        renderer.renderSourceTooltip(g);
        if(choiceRule==null){
            if(hoveredDefaultRule!=null){
                int saved=savedDefaultState(hoveredDefaultRule.key());
                nativeTooltip(g,java.util.Arrays.asList(
                        tr("dds-tweak.carpet_rules.default_left_hint"),
                        tr("dds-tweak.carpet_rules.default_right_hint"),
                        " ",
                        tr(saved==1?"dds-tweak.carpet_rules.default_saved":
                                saved==2?"dds-tweak.carpet_rules.default_not_saved":
                                "dds-tweak.carpet_rules.default_status_unknown")));
            }else if(hoveredFavoriteRule!=null){
                nativeTooltip(g,java.util.Collections.singletonList(tr(
                        preferences.isFavorite(hoveredFavoriteRule.key())
                                ?"dds-tweak.carpet_rules.unfavorite"
                                :"dds-tweak.carpet_rules.favorite_action")));
            }else renderRuleTooltip(g);
            CarpetRuleCatalog.RemoteRule detail=detailRule();
            if(detail!=null&&editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT
                    &&definitionDefault(detail)!=null
                    &&inside(mouseX,mouseY,detailResetX(),detailTargetY(),CarpetRuleUiStyle.TARGET_ACTION_SIZE,FIELD_H))
                nativeTooltip(g,java.util.Collections.singletonList(
                        tr("dds-tweak.carpet_rules.restore_definition")));
        }
        renderChoicePopup(g);
    }

    /** Native Minecraft tooltip pipeline; only content wrapping is local. */
    void nativeTooltip(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            List<String> texts){
        if(texts.isEmpty())return;
        List<Component> tooltip=new ArrayList<>(texts.size());
        for(String text:texts)tooltip.add(MinecraftCompat.literal(text));
        //#if MC >= 12106
        g.setComponentTooltipForNextFrame(font,tooltip,(int)mouseX,(int)mouseY);
        //#elseif MC >= 12000
        //$$ g.renderComponentTooltip(font,tooltip,(int)mouseX,(int)mouseY);
        //#elseif MC >= 11600
        //$$ renderComponentTooltip(g,tooltip,(int)mouseX,(int)mouseY);
        //#else
        //$$ renderTooltip(texts,(int)mouseX,(int)mouseY);
        //#endif
    }

    private void renderChoicePopup(
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
        if(choiceRule==null)return;
        List<String> values=CarpetRuleClientNetwork.suggestions(choiceRule);
        if(values.isEmpty()){choiceRule=null;return;}
        CarpetRuleMultiValue multi=CarpetRuleClientNetwork.multiValue(choiceRule);
        CarpetRuleCatalog.RemoteRule rule=CarpetRuleClientNetwork.catalog().get(choiceRule);
        String current=choiceFromList?drafts.desired(choiceRule):targetBox.getValue();
        if(current==null&&rule!=null)current=rule.value();
        int rows=CarpetRuleChoiceMenu.rows(values.size(),choiceVisibleRows),
                h=CarpetRuleChoiceMenu.height(values.size(),choiceVisibleRows);
        CarpetRuleUiStyle.choicePopupFrame(g,choiceX,choiceY,choiceWidth,h);
        for(int i=0;i<rows;i++){
            int idx=choiceOffset+i;
            int y=choiceY+CarpetRuleChoiceMenu.PADDING+i*CarpetRuleChoiceMenu.ROW_HEIGHT;
            if(idx>=values.size())break;
            boolean hover=inside(mouseX,mouseY,choiceX,y,choiceWidth,CarpetRuleChoiceMenu.ROW_HEIGHT);
            boolean checkedOption=multi!=null?multi.checked(current,values.get(idx))
                    :values.get(idx).equals(current);
            CarpetRuleUiStyle.choicePopupOption(g,choiceX,y,choiceWidth,
                    CarpetRuleChoiceMenu.ROW_HEIGHT,checkedOption,hover);
            if(multi!=null){
                boolean checked=multi.checked(current,values.get(idx));
                // Strong contrast even against a selected green row: light border, dark fill.
                CarpetRuleUiStyle.choiceCheckbox(g,choiceX+8,y+4,checked);
                                draw(g,fit(values.get(idx),choiceWidth-39),choiceX+27,y+5,
                        checkedOption?0xFFFFFFFF:0xFFE4F3ED);
            }else draw(g,fit(values.get(idx),choiceWidth-14),choiceX+7,y+5,
                    checkedOption?0xFFFFFFFF:0xFFE4F3ED);
        }
    }

    private void renderRuleTooltip(
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
        if(hoveredRule==null||editingGroups||sidebarOverlay||choiceRule!=null)return;
        nativeTooltip(g,CarpetRuleUiTooltip.lines(font,hoveredRule,metadata,vanillaMode(),
                width,height));
    }

    /** In singleplayer, the latest known disk snapshot takes precedence over
     * older CDA metadata; remote servers still use server-reported state. */
    int savedDefaultState(CarpetRuleCatalog.RuleKey key){
        if(CarpetRuleClientNetwork.integratedDefaultKnown(key))
            return CarpetRuleClientNetwork.integratedSavedDefaults().contains(key)?1:2;
        int state=CarpetRuleClientNetwork.serverPersisted(key);
        return state==1||state==2?state:-1;
    }

    /** Save the synchronized running value, not any uncommitted target editor contents. */
    void submitDefault(CarpetRuleCatalog.RemoteRule rule,CarpetRuleCommands.Mode mode){
        if(rule==null||vanillaMode()||mode==CarpetRuleCommands.Mode.TEMPORARY)return;
        CarpetRuleCatalog.RemoteRule current=CarpetRuleClientNetwork.catalog().get(rule.key());
        if(current==null){
            feedback=tr("dds-tweak.carpet_rules.unavailable");
            return;
        }
        CarpetRuleOperationTracker.Operation op=CarpetRuleClientNetwork.submit(
                rule.key(),mode,mode==CarpetRuleCommands.Mode.SET_DEFAULT?current.value():null);
        if(op==null){
            feedback=tr("dds-tweak.carpet_rules.unavailable");
            return;
        }
        selected=rule.key();
        if(narrow())narrowDetails=false;
        editMode=CarpetRuleCommands.Mode.TEMPORARY;
        refreshTarget();
        lastOperation=op;
        feedback=tr("dds-tweak.carpet_rules.persistent_sent");
    }

    private void openDefaultMenu(CarpetRuleCatalog.RemoteRule rule,int top,int right){
        choiceRule=null;
        defaultMenuRule=rule.key();
        String label=defaultMenuLabel(rule.key());
        defaultMenuWidth=Math.max(88,font.width(label)+20);
        defaultMenuWidth=Math.min(defaultMenuWidth,Math.max(60,width-2*MARGIN));
        defaultMenuX=Math.max(MARGIN,Math.min(
                CarpetRuleListScroll.saveX(right)+20-defaultMenuWidth,width-MARGIN-defaultMenuWidth));
        int below=top+CarpetRuleListScroll.CARD_HEIGHT+3;
        defaultMenuY=below+26>cardViewportBottom()
                ?Math.max(LIST_TOP,top-29):below;
    }

    private boolean handleDefaultMenuClick(double x,double y,int button){
        CarpetRuleCatalog.RuleKey key=defaultMenuRule;
        defaultMenuRule=null;
        if(key==null)return false;
        //#if MC >= 260300
        //$$ boolean left=button==InputConstants.MOUSE_BUTTON_LEFT;
        //#else
        boolean left=button==0;
        //#endif
        if(left&&inside(x,y,defaultMenuX,defaultMenuY,defaultMenuWidth,26)){
            CarpetRuleCatalog.RemoteRule current=CarpetRuleClientNetwork.catalog().get(key);
            submitDefault(current,CarpetRuleCommands.Mode.REMOVE_DEFAULT);
        }
        return true; // Dismiss without clicking through to a rule behind this popup.
    }

    private boolean openDefaultMenuAt(double x,double y,int button){
        //#if MC >= 260300
        //$$ if(button!=InputConstants.MOUSE_BUTTON_RIGHT)return false;
        //#else
        if(button!=1)return false;
        //#endif
        if(editingGroups||sidebarOverlay||vanillaMode()||(narrow()&&narrowDetails)
                ||!inside(x,y,listLeft(),cardViewportTop(),listWidth(),cardViewportHeight()))
            return false;
        List<CarpetRuleCatalog.RemoteRule> rules=visible();
        int index=cardScroll.hit(y,cardViewportTop(),rules.size(),cardViewportHeight());
        if(index<0)return false;
        int right=listLeft()+listWidth()-CARD_RIGHT_INSET;
        int top=cardScroll.top(cardViewportTop(),index);
        if(!inside(x,y,CarpetRuleListScroll.saveX(right),top+8,20,20))return false;
        openDefaultMenu(rules.get(index),top,right);
        return true;
    }

    private String defaultMenuLabel(CarpetRuleCatalog.RuleKey key){
        if(!CarpetRuleClientNetwork.canSubmit(key,CarpetRuleCommands.Mode.REMOVE_DEFAULT))
            return tr("dds-tweak.carpet_rules.unavailable");
        return tr("dds-tweak.carpet_rules.default_remove_action");
    }

    private void renderDefaultMenu(
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
        if(defaultMenuRule==null)return;
        rounded(g,defaultMenuX-1,defaultMenuY-1,
                defaultMenuX+defaultMenuWidth+1,defaultMenuY+27,5,0xFF94BDB0);
        rounded(g,defaultMenuX,defaultMenuY,
                defaultMenuX+defaultMenuWidth,defaultMenuY+26,4,0xFF222931);
        boolean available=CarpetRuleClientNetwork.canSubmit(
                defaultMenuRule,CarpetRuleCommands.Mode.REMOVE_DEFAULT);
        if(available&&inside(mouseX,mouseY,defaultMenuX,defaultMenuY,defaultMenuWidth,26))
            rounded(g,defaultMenuX+3,defaultMenuY+3,
                    defaultMenuX+defaultMenuWidth-3,defaultMenuY+23,3,0xFF61473D);
        draw(g,fit(defaultMenuLabel(defaultMenuRule),defaultMenuWidth-16),
                defaultMenuX+8,defaultMenuY+9,
                available?0xFFFFE3D9:0xFFB9BEC3);
    }

    void selectMode(CarpetRuleCommands.Mode mode){
        if(vanillaMode()&&mode!=CarpetRuleCommands.Mode.TEMPORARY)return;
        editMode=mode;
        reviewing=false;
        persistentDraft.clear();
        feedback=tr("dds-tweak.carpet_rules.mode.selected",
                tr("dds-tweak.carpet_rules.mode."+modeName()));
    }

    private String modeName(){
        switch(editMode){
            case SET_DEFAULT: return "set_default";
            case REMOVE_DEFAULT: return "remove_default";
            default: return "temporary";
        }
    }

    private void stage(){
        CarpetRuleCatalog.RemoteRule current=detailRule();
        if(current==null||targetBox==null)return;
        if(vanillaMode()){
            VanillaGameRules.Entry info=VanillaGameRules.get(current.key());
            if(info==null||!VanillaGameRules.permitted(minecraft)
                    ||!VanillaGameRules.valid(info,targetBox.getValue())){
                feedback=tr("dds-tweak.carpet_rules.vanilla.invalid");return;
            }
            vanillaDraftId=info.id;vanillaDraftOld=info.value;
            vanillaDraftTarget=targetBox.getValue();
            reviewing=false;feedback=tr("dds-tweak.carpet_rules.staged");
            return;
        }
        CarpetRuleMultiValue combination=CarpetRuleClientNetwork.multiValue(current.key());
        if(editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT
                &&combination!=null&&combination.parse(targetBox.getValue())==null){
            feedback=tr("dds-tweak.carpet_rules.invalid_combination");
            return;
        }
        boolean ok;
        if(editMode==CarpetRuleCommands.Mode.TEMPORARY)
            ok=drafts.stage(CarpetRuleClientNetwork.catalog(),current.key(),targetBox.getValue());
        else ok=persistentDraft.stage(CarpetRuleClientNetwork.catalog(),current.key(),
                editMode,editMode==CarpetRuleCommands.Mode.REMOVE_DEFAULT?null:targetBox.getValue());
        reviewing=false;
        feedback=tr(!ok?"dds-tweak.carpet_rules.unavailable"
                :editMode==CarpetRuleCommands.Mode.TEMPORARY
                    &&drafts.desired(current.key())==null
                    ?"dds-tweak.carpet_rules.draft_cancelled"
                    :"dds-tweak.carpet_rules.staged");
    }

    /** Audit the current form before a single confirmed server submission. */
    private void confirmChange(){
        if(!finishInlineNumber(true))return;
        CarpetRuleCatalog.RemoteRule current=detailRule();
        if(current==null||targetBox==null)return;
        if(editMode==CarpetRuleCommands.Mode.TEMPORARY){
            submitTemporary(current,targetBox.getValue());
            return;
        }
        // Selecting the default action and confirming is the only persistent-write confirmation.
        previewChange();
        if(reviewing)apply();
    }

    private void previewChange(){
        stage();
        CarpetRuleCatalog.RemoteRule current=detailRule();
        if(current==null||targetBox==null)return;
        if(vanillaMode()){
            VanillaGameRules.Entry rule=VanillaGameRules.get(current.key());
            if(rule==null||!rule.id.equals(vanillaDraftId)
                    ||!targetBox.getValue().equals(vanillaDraftTarget))return;
        }else if(editMode==CarpetRuleCommands.Mode.TEMPORARY){
            if(!targetBox.getValue().equals(drafts.desired(current.key())))return;
        }else if(persistentDraft.validate(CarpetRuleClientNetwork.catalog())
                !=CarpetRulePersistentChange.Status.READY
                ||!current.key().equals(persistentDraft.key())
                ||persistentDraft.mode()!=editMode
                ||(editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT
                    &&!targetBox.getValue().equals(persistentDraft.target())))return;
        review();
    }

    private void review(){
        CarpetRuleCatalog.RemoteRule current=detailRule();
        reviewing=false;
        if(current==null)return;
        if(vanillaMode()){
            VanillaGameRules.Entry info=VanillaGameRules.get(current.key());
            reviewing=info!=null&&info.id.equals(vanillaDraftId)
                    &&info.value.equals(vanillaDraftOld)
                    &&VanillaGameRules.valid(info,vanillaDraftTarget);
            feedback=reviewing?vanillaDraftOld+" → "+vanillaDraftTarget:
                    tr("dds-tweak.carpet_rules.conflict");
            return;
        }
        if(editMode!=CarpetRuleCommands.Mode.TEMPORARY){
            CarpetRulePersistentChange.Status state=persistentDraft.validate(CarpetRuleClientNetwork.catalog());
            if(state==CarpetRulePersistentChange.Status.READY
                    &&current.key().equals(persistentDraft.key())
                    &&editMode==persistentDraft.mode()){
                reviewing=true;
                feedback=tr("dds-tweak.carpet_rules.persistent_warning")+" "
                    +persistentDraft.original()+" → "
                    +(persistentDraft.target()==null?"<default>":persistentDraft.target());
            } else feedback=state.name();
            return;
        }
        for(CarpetRuleChanges.Diff diff:drafts.preview(CarpetRuleClientNetwork.catalog())){
            if(diff.key().equals(current.key())){
                reviewing=diff.status()==CarpetRuleChanges.Status.READY;
                feedback=diff.status().name()+": "+diff.current()+" → "+diff.desired();
                return;
            }
        }
        feedback=tr("dds-tweak.carpet_rules.no_draft");
    }

    private void apply(){
        if(!reviewing){
            feedback=tr("dds-tweak.carpet_rules.preview_required");
            return;
        }
        if(vanillaMode()){
            CarpetRuleCatalog.RemoteRule current=detailRule();
            VanillaGameRules.Entry info=current==null?null:VanillaGameRules.get(current.key());
            boolean sent=info!=null&&info.id.equals(vanillaDraftId)
                    &&VanillaGameRules.submit(minecraft,info,vanillaDraftOld,vanillaDraftTarget);
            feedback=tr(sent?"dds-tweak.carpet_rules.vanilla.sent":
                    "dds-tweak.carpet_rules.conflict");
            if(sent){clearVanillaDraft();VanillaGameRules.request(minecraft);}
            return;
        }
        if(editMode!=CarpetRuleCommands.Mode.TEMPORARY&&!reviewing){
            feedback=tr("dds-tweak.carpet_rules.persistent_warning");
            return;
        }
        reviewing=false;
        CarpetRuleCatalog.RemoteRule current=detailRule();
        if(current==null)return;
        if(editMode!=CarpetRuleCommands.Mode.TEMPORARY){
            if(persistentDraft.validate(CarpetRuleClientNetwork.catalog())
                    !=CarpetRulePersistentChange.Status.READY
                    ||!current.key().equals(persistentDraft.key())
                    ||editMode!=persistentDraft.mode()){
                feedback=tr("dds-tweak.carpet_rules.conflict");
                return;
            }
            lastOperation=CarpetRuleClientNetwork.submit(current.key(),editMode,persistentDraft.target());
            if(lastOperation==null)feedback=tr("dds-tweak.carpet_rules.unavailable");
            else{
                persistentDraft.clear();
                // Keep a SET_DEFAULT target visible while the server verifies it.
                // 不把“待保存 true”自动切回临时模式的旧值 false。
                if(editMode==CarpetRuleCommands.Mode.REMOVE_DEFAULT)
                    editMode=CarpetRuleCommands.Mode.TEMPORARY;
                feedback=tr("dds-tweak.carpet_rules.persistent_sent");
            }
            return;
        }
        for(CarpetRuleChanges.Diff diff:drafts.preview(CarpetRuleClientNetwork.catalog())){
            if(!diff.key().equals(current.key()))continue;
            if(diff.status()!=CarpetRuleChanges.Status.READY){
                feedback=tr("dds-tweak.carpet_rules.conflict");
                return;
            }
            lastOperation=CarpetRuleClientNetwork.submit(diff.key(),
                    CarpetRuleCommands.Mode.TEMPORARY,diff.desired());
            if(lastOperation==null)feedback=tr("dds-tweak.carpet_rules.unavailable");
            else{
                feedback=tr("dds-tweak.carpet_rules.sent");
                drafts.remove(diff.key());
            }
            return;
        }
        feedback=tr("dds-tweak.carpet_rules.no_draft");
    }

    void refreshTarget(){
        if(targetBox==null)return;
        CarpetRuleCatalog.RemoteRule current=detailRule();
        String pending=current==null?null:vanillaMode()
                ?vanillaDesired(current):drafts.desired(current.key());
        targetBox.setValue(current==null?"":pending==null?current.value():pending);
        targetBox.setCursorPosition(0);
        targetBox.setHighlightPos(0);
        reviewing=false;
        persistentDraft.clear();
        descriptionOffset=0;
        choiceRule=null;
    }

    private List<String> managers(){
        SortedSet<String> seen=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(CarpetRuleCatalog.RemoteRule rule:CarpetRuleClientNetwork.catalog().snapshot())
            seen.add(rule.key().manager());
        List<String> all=new ArrayList<>();
        all.add("");
        all.addAll(seen);
        return all;
    }

    private boolean hiddenCompatibilityRule(CarpetRuleCatalog.RemoteRule rule){
        return CarpetRuleSidebar.hiddenCompatibilityRule(rule,
                CarpetRuleClientNetwork.integratedOwner(rule.key()));
    }

    List<CarpetRuleCatalog.RemoteRule> uiRules(){
        if(renderFrameActive&&frameUiRules!=null)return frameUiRules;
        List<CarpetRuleCatalog.RemoteRule> out=new ArrayList<>();
        for(CarpetRuleCatalog.RemoteRule rule:CarpetRuleClientNetwork.catalog().snapshot())
            if(!hiddenCompatibilityRule(rule))out.add(rule);
        List<CarpetRuleCatalog.RemoteRule> result=Collections.unmodifiableList(out);
        if(renderFrameActive)frameUiRules=result;
        return result;
    }

    /** Only editable, local plans containing the selected rule are shown here. */
    List<CarpetRulePresetStore.Preset> assignedPlans(CarpetRuleCatalog.RuleKey rule){
        List<CarpetRulePresetStore.Preset> assigned=new ArrayList<>();
        if(rule!=null&&plans!=null)for(CarpetRulePresetStore.Preset plan:plans.editablePlans())
            if(plan.values().containsKey(rule))assigned.add(plan);
        return assigned;
    }

    int quickPlanTop(){return detailGroupAssignY()+29;}
    int quickPlanRows(){
        return Math.max(0,(LIST_TOP+listHeight()-75-quickPlanTop())/35);
    }

    /** Delegate to the existing batch reviewer; never write a rule from a UI shortcut. */
    void quickApplyPlan(String name){
        CarpetRulePresetStore.Preset plan=plans==null?null:plans.get(name);
        if(plan==null||plan.isSnapshot()||plan.values().isEmpty())return;
        MinecraftCompat.setScreen(minecraft,new CarpetRulePresetScreen(this,name,true));
    }

    boolean emptyPlan(){
        if(sidebarKind!=CarpetRuleSidebar.Kind.PLAN||plans==null)return false;
        CarpetRulePresetStore.Preset plan=plans.get(sidebarId);
        return plan!=null&&!plan.isSnapshot()&&plan.values().isEmpty();
    }

    List<CarpetRuleCatalog.RemoteRule> visible(){
        if(renderFrameActive&&frameVisibleRules!=null)return frameVisibleRules;
        if(vanillaMode()){
            List<CarpetRuleCatalog.RemoteRule> result=VanillaGameRules.list(query);
            if(renderFrameActive)frameVisibleRules=result;
            return result;
        }
        boolean defaultsFilter=sidebarKind==CarpetRuleSidebar.Kind.SAVED_DEFAULTS;
        boolean defaultsReady=defaultsFilter&&defaultsAvailable();
        Set<CarpetRuleCatalog.RuleKey> saved=defaultsFilter&&defaultsReady
                ?savedDefaults():Collections.emptySet();
        List<CarpetRuleCatalog.RemoteRule> matches=new ArrayList<>();
        String needle=query.trim().toLowerCase(Locale.ROOT);
        for(CarpetRuleCatalog.RemoteRule rule:uiRules()){
            if(sidebarKind==CarpetRuleSidebar.Kind.CATEGORY){
                boolean found=false;
                for(String tag:ruleCategories(rule))
                    if(tag.equalsIgnoreCase(sidebarId)){found=true;break;}
                if(!found)continue;
            }else if(sidebarKind==CarpetRuleSidebar.Kind.PLAN){
                CarpetRulePresetStore.Preset plan=plans==null?null:plans.get(sidebarId);
                if(plan==null||plan.isSnapshot()||!plan.values().containsKey(rule.key()))continue;
            }else if(defaultsFilter){
                if(!defaultsReady||!saved.contains(rule.key()))continue;
            }else if(sidebarKind==CarpetRuleSidebar.Kind.MODIFIED){
                if(CarpetRuleModified.classify(rule.value(),
                        CarpetRuleClientNetwork.definitionDefault(rule.key()))
                        !=CarpetRuleModified.State.MODIFIED)continue;
            }else if(!CarpetRuleSidebar.matches(sidebarKind,sidebarId,rule,metadata,preferences,
                    CarpetRuleClientNetwork.integratedOwner(rule.key())))continue;
            CarpetRuleMetadata.Description info=metadata.find(rule.key());
            if(!needle.isEmpty()&&!rule.key().name().toLowerCase(Locale.ROOT).contains(needle)
                    &&!rule.key().manager().toLowerCase(Locale.ROOT).contains(needle)
                    &&(info==null||(!info.displayName().toLowerCase(Locale.ROOT).contains(needle)
                    &&!info.detail().toLowerCase(Locale.ROOT).contains(needle))))continue;
            matches.add(rule);
        }
        matches.sort(Comparator.comparing((CarpetRuleCatalog.RemoteRule r)->r.key().manager())
                .thenComparing(r->r.key().name()));
        List<CarpetRuleCatalog.RemoteRule> result=Collections.unmodifiableList(matches);
        if(renderFrameActive)frameVisibleRules=result;
        return result;
    }

    /** Preserve authoritative Carpet categories separately from verified mod owners. */
    private java.util.Collection<String> ruleCategories(CarpetRuleCatalog.RemoteRule rule){
        List<String> server=CarpetRuleAuthority.categories(rule.key());
        if(!server.isEmpty())return server;
        CarpetRuleMetadata.Description info=metadata.find(rule.key());
        return info==null?java.util.Collections.emptyList():info.categories();
    }

    private List<CarpetRuleSidebar.Entry> categoryEntries(){
        if(renderFrameActive&&frameCategoryEntries!=null)return frameCategoryEntries;
        java.util.Map<String,Integer> counts=new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for(CarpetRuleCatalog.RemoteRule rule:uiRules())
            for(String tag:new java.util.LinkedHashSet<>(ruleCategories(rule)))
                if(tag!=null&&!tag.trim().isEmpty())counts.merge(tag,1,Integer::sum);
        List<CarpetRuleSidebar.Entry> result=new ArrayList<>();
        for(java.util.Map.Entry<String,Integer> e:counts.entrySet())
            result.add(CarpetRuleSidebar.Entry.category(e.getKey(),e.getValue()));
        List<CarpetRuleSidebar.Entry> immutable=Collections.unmodifiableList(result);
        if(renderFrameActive)frameCategoryEntries=immutable;
        return immutable;
    }

    private String categoryLabel(String tag){
        switch(tag.toLowerCase(Locale.ROOT)){
            case "survival":return tr("dds-tweak.carpet_rules.category.survival");
            case "command":return tr("dds-tweak.carpet_rules.category.command");
            case "creative":return tr("dds-tweak.carpet_rules.category.creative");
            case "feature":return tr("dds-tweak.carpet_rules.category.feature");
            case "optimization":return tr("dds-tweak.carpet_rules.category.optimization");
            case "debug":return tr("dds-tweak.carpet_rules.category.debug");
            default:return tag;
        }
    }

    boolean narrow(){return !CarpetRuleScreenLayout.wide(width);}
    int sidebarWidth(){return narrow()?width-2*MARGIN:Math.min(172,Math.max(112,width/5));}
    int listLeft(){return narrow()?MARGIN:MARGIN+sidebarWidth()+8;}
    int listWidth(){
        if(narrow())return width-2*MARGIN;
        int available=width-listLeft()-MARGIN-8;
        return CarpetRuleScreenLayout.middleWidth(available);
    }
    int detailLeft(){return narrow()?MARGIN:listLeft()+listWidth()+8;}
    int detailWidth(){return width-MARGIN-detailLeft();}
    int listHeight(){return Math.max(LIST_ROW,height-LIST_TOP-15);}
    private int rowCount(){return Math.max(1,cardViewportHeight()/CarpetRuleListScroll.STRIDE);}
    int cardViewportTop(){return LIST_TOP+CARD_TOP_INSET;}
    int cardViewportBottom(){return LIST_TOP+listHeight()-CARD_BOTTOM_INSET;}
    int cardViewportHeight(){return Math.max(1,cardViewportBottom()-cardViewportTop());}
    private int sourceRows(){
        return sourcesExpanded?CarpetRuleSidebar.sourceRows(LIST_TOP,sidebarBottom()-28,
                sidebarChoices(CarpetRuleSidebar.Section.EXTENSIONS).size()):0;
    }
    int categoryHeaderY(){
        int y=LIST_TOP+62+(sourcesExpanded?sourceRows()*22+5:0);
        return Math.min(y,quickHeaderY()-58);
    }
    private int categoryRows(){
        int available=Math.max(0,(quickHeaderY()-categoryHeaderY()-58)/22);
        return categoriesExpanded?Math.min(categoryEntries().size(),available):0;
    }
    int sidebarChoiceTop(CarpetRuleSidebar.Section section){
        if(section==CarpetRuleSidebar.Section.EXTENSIONS)return LIST_TOP+62;
        if(section==CarpetRuleSidebar.Section.CATEGORIES)return categoryHeaderY()+26;
        return groupHeaderY()+26;
    }
    int sidebarChoiceBottom(CarpetRuleSidebar.Section section){
        int bottom=section==CarpetRuleSidebar.Section.EXTENSIONS?categoryHeaderY()-5
                :section==CarpetRuleSidebar.Section.CATEGORIES?groupHeaderY()-5
                :quickHeaderY()-8;
        return Math.max(sidebarChoiceTop(section),bottom);
    }
    int sidebarRows(CarpetRuleSidebar.Section section){
        if(section==CarpetRuleSidebar.Section.EXTENSIONS)return sourceRows();
        if(section==CarpetRuleSidebar.Section.CATEGORIES)return categoryRows();
        return Math.max(0,(sidebarChoiceBottom(section)-sidebarChoiceTop(section))/22);
    }
    int sidebarOffset(CarpetRuleSidebar.Section section){
        if(section==CarpetRuleSidebar.Section.EXTENSIONS)return sourceChoiceOffset;
        if(section==CarpetRuleSidebar.Section.CATEGORIES)return categoryChoiceOffset;
        return groupChoiceOffset;
    }
    void setSidebarOffset(CarpetRuleSidebar.Section section,int offset){
        if(section==CarpetRuleSidebar.Section.EXTENSIONS)sourceChoiceOffset=offset;
        else if(section==CarpetRuleSidebar.Section.CATEGORIES)categoryChoiceOffset=offset;
        else groupChoiceOffset=offset;
    }
    boolean isExpanded(CarpetRuleSidebar.Section section){
        if(section==CarpetRuleSidebar.Section.EXTENSIONS)return sourcesExpanded;
        if(section==CarpetRuleSidebar.Section.CATEGORIES)return categoriesExpanded;
        return groupsExpanded;
    }
    private List<CarpetRuleSidebar.Entry> sidebarEntries(){
        if(renderFrameActive&&frameSidebarEntries!=null)return frameSidebarEntries;
        List<CarpetRuleCatalog.RemoteRule> rules=uiRules();
        java.util.Map<CarpetRuleCatalog.RuleKey,String> owners=new java.util.HashMap<>();
        for(CarpetRuleCatalog.RemoteRule rule:rules){
            String owner=CarpetRuleClientNetwork.integratedOwner(rule.key());
            if(owner!=null)owners.put(rule.key(),owner);
        }
        java.util.Map<CarpetRuleCatalog.RuleKey,String> definitions=new java.util.HashMap<>();
        for(CarpetRuleCatalog.RemoteRule rule:rules){
            String definition=CarpetRuleClientNetwork.definitionDefault(rule.key());
            if(definition!=null)definitions.put(rule.key(),definition);
        }
        boolean defaultsKnown=defaultsAvailable();
        Set<CarpetRuleCatalog.RuleKey> saved=defaultsKnown?savedDefaults():Collections.emptySet();
        List<CarpetRuleSidebar.Entry> entries=new ArrayList<>(CarpetRuleSidebar.entries(
                rules,metadata,preferences,owners,saved,defaultsKnown,definitions));
        entries.removeIf(e->e.kind()==CarpetRuleSidebar.Kind.GROUP);
        if(plans!=null)for(CarpetRulePresetStore.Preset plan:plans.presets()){
            if(plan.isSnapshot())continue;
            int present=0;
            for(CarpetRuleCatalog.RemoteRule rule:rules)
                if(plan.values().containsKey(rule.key()))present++;
            entries.add(CarpetRuleSidebar.Entry.plan(plan.name(),present));
        }
        entries.addAll(categoryEntries());
        List<CarpetRuleSidebar.Entry> result=Collections.unmodifiableList(entries);
        if(renderFrameActive)frameSidebarEntries=result;
        return result;
    }
    String sidebarLabel(CarpetRuleSidebar.Entry entry){
        switch(entry.kind()){
            case ALL:return tr("dds-tweak.carpet_rules.sidebar.all");
            case CARPET:return tr(entry.containsUnverified()
                    ?"dds-tweak.carpet_rules.sidebar.carpet_fallback"
                    :"dds-tweak.carpet_rules.sidebar.carpet");
            case OWNER:return CarpetRuleSidebar.sourceShortLabel(entry.id(),
                    CarpetRuleClientNetwork.sourceDisplayName(entry.id()));
            case UNKNOWN:return tr("dds-tweak.carpet_rules.sidebar.unknown");
            case FAVORITES:return tr("dds-tweak.carpet_rules.sidebar.favorites");
            case MODIFIED:return tr("dds-tweak.carpet_rules.sidebar.modified");
            case GAME_RULES:return tr("dds-tweak.carpet_rules.sidebar.gamerules");
            case SAVED_DEFAULTS:return tr(defaultsAvailable()
                    ?CarpetRuleClientNetwork.defaultsComplete()
                            ?"dds-tweak.carpet_rules.sidebar.defaults_local"
                            :"dds-tweak.carpet_rules.sidebar.defaults_partial"
                    :"dds-tweak.carpet_rules.sidebar.defaults");
            case CATEGORY:return categoryLabel(entry.id());
            case PLAN:return entry.id();
            case GROUP:return entry.id();
            default:return entry.id();
        }
    }
    void selectSidebar(CarpetRuleSidebar.Entry entry){
        if(!entry.selectable())return;
        sidebarKind=entry.kind();
        sidebarId=entry.id();
        finishInlineNumber(false);
        List<CarpetRuleCatalog.RemoteRule> firstPage=visible();
        selected=firstPage.isEmpty()?null:firstPage.get(0).key();
        clearVanillaDraft();
        editMode=CarpetRuleCommands.Mode.TEMPORARY;
        // Keep independent accordion states, including their current scroll offsets.
        sidebarOverlay=false;
        cardScroll.reset();
        choiceRule=null;
        feedback="";
        refreshTarget();
    }

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float pt){
    //$$     if(minecraft.level!=null)extractBlurredBackground(g);
    //$$ }
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){
    //$$     refreshConnection();
    //$$     beginRenderFrame();
    //$$     try{
    //$$         renderBase(g);renderer.renderDetails(g,mx,my);super.extractRenderState(g,mx,my,pt);renderOverlays(g);
    //$$     }finally{endRenderFrame();}
    //$$ }
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float pt){
        //#if MC >= 12005
        // No vanilla blur: CarpetRuleUiStyle draws the same in-world mask as MaLiLib.
        //#else
        //$$ // No vanilla background; the shared MaLiLib-style mask owns this layer.
        //#endif
    }
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        refreshConnection();
        beginRenderFrame();
        try{
            renderCarpetBlur(g,pt);
            renderBase(g);renderer.renderDetails(g,mx,my);super.render(g,mx,my,pt);renderOverlays(g);
        }finally{endRenderFrame();}
    }
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics g,int mx,int my,float pt){
    //$$     refreshConnection();
    //$$     beginRenderFrame();
    //$$     try{renderBase(g);renderer.renderDetails(g,mx,my);super.render(g,mx,my,pt);renderOverlays(g);}
    //$$     finally{endRenderFrame();}
    //$$ }
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack g,int mx,int my,float pt){
    //$$     refreshConnection();
    //$$     beginRenderFrame();
    //$$     try{renderBase(g);renderer.renderDetails(g,mx,my);super.render(g,mx,my,pt);renderOverlays(g);}
    //$$     finally{endRenderFrame();}
    //$$ }
    //#else
    //$$ @Override public void render(int mx,int my,float pt){
    //$$     refreshConnection();
    //$$     beginRenderFrame();
    //$$     try{renderBase(null);renderer.renderDetails(null,mx,my);super.render(mx,my,pt);renderOverlays(null);}
    //$$     finally{endRenderFrame();}
    //$$ }
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
        CarpetRuleCatalog.RemoteRule detail=detailRule();
        boolean hasDetail=detail!=null;
        boolean editVisible=hasDetail&&!editingGroups&&!sidebarOverlay
                &&(!narrow()||narrowDetails)&&listHeight()>detailTargetY()-LIST_TOP+62;
        boolean binary=booleanRule()&&editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT;
        if(searchBox!=null)searchBox.visible=searchOpen&&!editingGroups&&!sidebarOverlay
                &&(!narrow()||!narrowDetails);
        if(targetBox!=null)targetBox.visible=editVisible&&!binary&&editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT;
        if(choiceButton!=null)choiceButton.visible=editVisible&&!binary
                &&editMode!=CarpetRuleCommands.Mode.REMOVE_DEFAULT
                &&!CarpetRuleClientNetwork.suggestions(detail.key()).isEmpty();

        if(targetBox!=null){
            int right=choiceButton!=null&&choiceButton.visible?detailChoiceX():detailResetX();
            targetBox.setWidth(Math.max(30,right-(detailLeft()+12)-6));
        }
        if(groupAssignButton!=null)groupAssignButton.visible=!vanillaMode()&&!editingGroups&&!sidebarOverlay
                &&(!narrow()||narrowDetails)&&showPlanAssignment()&&hasDetail;
        if(emptyPlanButton!=null)
            emptyPlanButton.visible=emptyPlan()&&!editingGroups&&!sidebarOverlay
                    &&(!narrow()||!narrowDetails);
        boolean showMode=hasDetail&&!editingGroups&&!sidebarOverlay
                &&(!narrow()||narrowDetails)&&showDefaults();

        if(groupNameBox!=null)groupNameBox.visible=editingGroups;
        if(createGroupButton!=null)createGroupButton.visible=editingGroups;
        if(renameGroupButton!=null)renameGroupButton.visible=editingGroups;
        if(deleteGroupButton!=null)deleteGroupButton.visible=editingGroups;
        if(membershipButton!=null)membershipButton.visible=editingGroups;
        if(applyButton!=null){
            boolean temp=editMode==CarpetRuleCommands.Mode.TEMPORARY;
            boolean staged=hasDetail&&targetBox!=null
                    &&!targetBox.getValue().equals(detail.value());
            boolean multi=hasDetail&&CarpetRuleClientNetwork.multiValue(detail.key())!=null;
            boolean quick=hasDetail&&(isBoolean(detail)||isChoiceRule(detail)&&!multi);
            applyButton.visible=editVisible&&temp&&!quick;
            applyButton.active=staged&&(vanillaMode()
                    ?VanillaGameRules.permitted(minecraft)
                    :CarpetRuleClientNetwork.canSubmit(detail.key()));
        }
        if(confirmDefaultButton!=null){
            confirmDefaultButton.visible=editVisible&&showMode&&!vanillaMode()
                    &&editMode!=CarpetRuleCommands.Mode.TEMPORARY;
            confirmDefaultButton.active=hasDetail
                    &&CarpetRuleClientNetwork.canSubmit(detail.key(),editMode);
        }
        if(vanillaMode()){
            VanillaGameRules.poll(minecraft);
            if(VanillaGameRules.received()&&!vanillaReadyWas)refreshTarget();
            vanillaReadyWas=VanillaGameRules.received();
        }else vanillaReadyWas=false;
        CarpetRuleUiStyle.background(g,width,height);
        draw(g,tr("dds-tweak.carpet_rules.title"),MARGIN,17,0xFFFFFFFF);
        String subtitle=tr("dds-tweak.carpet_rules.server_only");
        draw(g,subtitle,MARGIN,32,0xFFAAAAAA);

    }

    private void refreshConnection(){
        long latest=CarpetRuleClientNetwork.catalog().generation();
        if(latest==observedGeneration)return;
        observedGeneration=latest;
        selected=null;
        manager="";
        category="";
        group="";
        sidebarKind=CarpetRuleSidebar.Kind.ALL;
        sidebarId="";
        sourceChoiceOffset=0;
        categoryChoiceOffset=0;
        groupChoiceOffset=0;
        sourcesExpanded=true;
        categoriesExpanded=false;
        groupsExpanded=false;
        sidebarOverlay=false;
        cardScroll.reset();
        query="";
        searchOpen=false;
        choiceRule=null;
        defaultMenuRule=null;
        descriptionOffset=0;
        offset=0;
        narrowDetails=false;
        reviewing=false;
        choosingManager=false;
        editingGroups=false;
        favoritesOnly=false;
        lastOperation=null;
        feedback="";
        drafts.clear();
        persistentDraft.clear();
        clearVanillaDraft();
        if(searchBox!=null)searchBox.setValue("");
        finishInlineNumber(false);
        if(targetBox!=null)targetBox.setValue("");
    }


    List<CarpetRuleSidebar.Entry> sidebarChoices(CarpetRuleSidebar.Section section){
        List<CarpetRuleSidebar.Entry> choices=new ArrayList<>();
        for(CarpetRuleSidebar.Entry entry:sidebarEntries())
            if(CarpetRuleSidebar.sectionFor(entry.kind())==section)choices.add(entry);
        return choices;
    }

    void toggleSidebarSection(CarpetRuleSidebar.Section section){
        boolean expand=!isExpanded(section);
        sourcesExpanded=expand&&section==CarpetRuleSidebar.Section.EXTENSIONS;
        categoriesExpanded=expand&&section==CarpetRuleSidebar.Section.CATEGORIES;
        groupsExpanded=expand&&section==CarpetRuleSidebar.Section.GROUPS;
    }

    CarpetRuleSidebar.Entry findSidebarEntry(CarpetRuleSidebar.Kind kind,String id){
        for(CarpetRuleSidebar.Entry entry:sidebarEntries())
            if(entry.matches(kind,id))return entry;
        return null;
    }

    int sidebarBottom(){return LIST_TOP+listHeight();}
    int quickHeaderY(){return CarpetRuleSidebar.quickHeaderY(sidebarBottom());}
    int groupHeaderY(){
        int y=categoryHeaderY()+26+(categoriesExpanded?categoryRows()*22+5:0);
        return Math.min(y,quickHeaderY()-30);
    }
    int sidebarRowY(CarpetRuleSidebar.Section section,int row){
        return sidebarChoiceTop(section)+row*22;
    }


    static boolean inside(double x,double y,int left,int top,int w,int h){
        return x>=left&&x<left+w&&y>=top&&y<top+h;
    }

    /** Shared geometry with the configuration-plan screen. */
    void rounded(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x1,int y1,int x2,int y2,int radius,int color){
        CarpetRuleUiStyle.rounded(g,x1,y1,x2,y2,radius,color);
    }

    /** Optimistic switch position is always marked as in-flight, never as confirmed. */
    String temporaryInFlight(CarpetRuleCatalog.RuleKey key){
        List<CarpetRuleOperationTracker.Operation> history=CarpetRuleClientNetwork.operations().history();
        for(int i=history.size()-1;i>=0;i--){
            CarpetRuleOperationTracker.Operation op=history.get(i);
            if(!op.key().equals(key)||op.mode()!=CarpetRuleCommands.Mode.TEMPORARY)continue;
            CarpetRuleOperationTracker.Status status=op.status();
            if(status==CarpetRuleOperationTracker.Status.SENT
                    ||status==CarpetRuleOperationTracker.Status.WAITING_FOR_SYNC)
                return op.target();
            return null;
        }
        return null;
    }

    void detailHeading(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            String title,int x,int y,int available,float scale,int color){
        String fitted=fit(title,Math.max(1,(int)(available/scale)));
        ScreenRenderCompat.scaledText(g,font,fitted,x,y,color,scale);
    }

    void detailDivider(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int left,int width,int y){
        ScreenRenderCompat.fill(g,left+12,y,left+width-12,y+1,0x665B676D);
    }

    void drawToggle(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int x,int y,int w,int h,boolean enabled,boolean pending,boolean hover){
        int track=enabled?0xFF307657:0xFF555C66;
        rounded(g,x,y,x+w,y+h,h/2,hover?0xFF529477:track);
        int knob=enabled?x+w-h+2:x+2;
        rounded(g,knob,y+2,knob+h-4,y+h-2,h/2,0xFFF4F5F6);
        if(pending)ScreenRenderCompat.fill(g,x+w-2,y-2,x+w+2,y+2,0xFFFFD27C);
    }

    String fit(String s,int maxWidth){
        if(maxWidth<=0)return "";
        if(font.width(s)<=maxWidth)return s;
        int n=s.length();
        while(n>0&&font.width(s.substring(0,n)+"…")>maxWidth)n--;
        return s.substring(0,n)+"…";
    }

    void draw(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            String value,int x,int y,int color){ScreenRenderCompat.text(g,font,value,x,y,color,true);}

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(inlineNumberKey!=null&&!inputHandler.insideInlineNumber(event.x(),event.y())
                &&!finishInlineNumber(true))return true;
        if(defaultMenuRule!=null)return handleDefaultMenuClick(event.x(),event.y(),event.button());
        if(openDefaultMenuAt(event.x(),event.y(),event.button()))return true;
        if(choiceRule!=null)return chooseFromPopup(event.x(),event.y());
        if(super.mouseClicked(event,doubleClick)){
            if(getFocused() instanceof Button)setFocused(null);
            return true;
        }
        return inputHandler.selectAt(event.x(),event.y(),event.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){
    //$$     if(inlineNumberKey!=null&&!inputHandler.insideInlineNumber(x,y)&&!finishInlineNumber(true))return true;
    //$$     if(defaultMenuRule!=null)return handleDefaultMenuClick(x,y,button);
    //$$     if(openDefaultMenuAt(x,y,button))return true;
    //$$     if(choiceRule!=null)return chooseFromPopup(x,y);
    //$$     if(super.mouseClicked(x,y,button)){
    //$$         if(getFocused() instanceof Button)setFocused(null);
    //$$         return true;
    //$$     }
    //$$     return inputHandler.selectAt(x,y,button);
    //$$ }
    //#endif


    //#if MC >= 12002
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(inputHandler.scroll(x,y,vertical))return true;
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    //#else
    //$$ @Override public boolean mouseScrolled(double x,double y,double vertical){
    //$$     if(inputHandler.scroll(x,y,vertical))return true;
    //$$     return super.mouseScrolled(x,y,vertical);
    //$$ }
    //#endif


    //#if MC >= 12109
    @Override public boolean keyPressed(KeyEvent event){
        if(inputHandler.onKey(event.isConfirmation(),event.isEscape()))return true;
        return super.keyPressed(event);
    }
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
    //$$     if(inputHandler.onKey(keyCode==257||keyCode==335,keyCode==256))return true;
    //$$     return super.keyPressed(keyCode,scanCode,modifiers);
    //$$ }
    //#endif

    // Accessors are confined to package-local Screen collaborators.
    // 仅向同包的渲染与输入组件暴露原版 Screen 上下文。
    int screenWidth(){return width;}
    int screenHeight(){return height;}
    net.minecraft.client.gui.Font renderFont(){return font;}
    Minecraft minecraftInstance(){return minecraft;}

    @Override public void onClose(){MinecraftCompat.setScreen(minecraft,parent);}
    static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}
}

package ddstweak.feature.carpetrules;

import ddstweak.gui.SharedScreenBackground;
import ddstweak.gui.GuiIconRenderer;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.LinkedHashMap;

/** Local rule configuration plans. All server writes remain explicitly reviewed. */
public final class CarpetRulePresetScreen extends Screen {
    private static final int MARGIN=14,TOP=50,ROW=CarpetRulePresetLayout.ROW,
            CARD_HEIGHT=CarpetRulePresetLayout.CARD_HEIGHT,FIELD_H=18;
    private static final float DETAIL_TITLE_SCALE=1.16F,DETAIL_LABEL_SCALE=1.10F;
    private static final String UNKNOWN_OWNER="?";
    private final Screen parent;
    private final CarpetRuleCatalog.RuleKey requestedRule;
    private final String requestedValue;
    private final CarpetRulePresetStore store=CarpetRulePresetStore.load();
    private final CarpetRuleBatchExecutor batch=new CarpetRuleBatchExecutor();
    private final CarpetRuleBatchExecutor.Sender sender=new CarpetRuleBatchExecutor.Sender(){
        @Override public boolean canSend(CarpetRuleCatalog.RuleKey key){
            return CarpetRuleClientNetwork.canSubmit(key);
        }
        @Override public CarpetRuleOperationTracker.Operation send(CarpetRuleCatalog.RuleKey key,String target){
            return CarpetRuleClientNetwork.submit(key,CarpetRuleCommands.Mode.TEMPORARY,target);
        }
        @Override public boolean canSend(CarpetRuleCatalog.RuleKey key,CarpetRuleCommands.Mode mode){
            return CarpetRuleClientNetwork.canSubmit(key,mode);
        }
        @Override public CarpetRuleOperationTracker.Operation send(
                CarpetRuleCatalog.RuleKey key,String target,CarpetRuleCommands.Mode mode){
            return CarpetRuleClientNetwork.submit(key,mode,target);
        }
    };
    private List<CarpetRuleBatchReview.Entry> reviewedEntries;
    private String reviewedName;
    private long reviewedGeneration;
    private Button applyBatchButton,stopBatchButton,createButton,editPlanButton,
            saveNameButton,cancelNameButton,deleteNameButton,pickAddButton,backDetailsButton,
            saveSwitchButton,discardSwitchButton,cancelSwitchButton,
            exportButton,importButton,confirmImportButton,cancelImportButton,
            browseImportButton,openExportButton,emptyImportButton;
    private EditBox nameBox,importPathBox;
    private EditBox ruleSearchBox;
    private Button pickRuleButton,valueChoiceButton;
    private boolean pickingRule,naming,renaming,planFeedback,importing;
    private String importFilePath="";
    private static final long EXPORT_NOTICE_NANOS=5_000_000_000L;
    private java.nio.file.Path exportedFile;
    private long exportNoticeDeadline;
    private String exportNoticeText="";

    private volatile boolean fileOpenFailed;
    private boolean nativePickerPending;
    private long nativePickerGeneration;
    private CarpetRuleLocalFiles.NativePicker nativePicker;
    private boolean browsingImport;
    private java.nio.file.Path importBrowseDirectory;
    private List<java.nio.file.Path> importBrowseEntries=java.util.Collections.emptyList();
    private int importBrowseOffset,descriptionOffset;
    private final Set<CarpetRuleCatalog.RuleKey> pickedRules=new LinkedHashSet<>();
    private String pickerOwner="*",pickerCategory="*";
    private String filterPopup="";
    private int filterOffset,mouseX,mouseY;
    private CarpetRuleCatalog.RemoteRule tooltipRule;
    private boolean hoveredRemoveRule;
    private Button pickOwnerButton,pickCategoryButton;
    private String ruleQuery="";
    private final CarpetRuleListScroll pickerScroll=new CarpetRuleListScroll();
    private boolean valueChoicesOpen;
    private int valueChoiceOffset;
    private EditBox targetBox;
    private Button saveTargetButton,removeTargetButton;
    private CarpetRuleCatalog.RuleKey selectedRule;
    private final CarpetRuleListScroll ruleScroll=new CarpetRuleListScroll();
    private String selected;
    private String quickApplyPending;
    private String feedback="";
    private int offset;
    private boolean narrowDetail,narrowSidebar,targetDirty,refreshingTarget,pendingSwitch,pendingDetails;
    private String pendingPlan,namePreviousPlan;
    private CarpetRuleCatalog.RuleKey pendingRule,namePreviousRule;

    public CarpetRulePresetScreen(Screen parent){
        this(parent,null,null);
    }
    public CarpetRulePresetScreen(Screen parent,CarpetRuleCatalog.RuleKey requestedRule){
        this(parent,requestedRule,null);
    }
    public CarpetRulePresetScreen(Screen parent,CarpetRuleCatalog.RuleKey requestedRule,
                                  String requestedValue){
        this(parent,requestedRule,requestedValue,null);
    }

    /** Single-plan shortcut from the rule browser. Never auto-apply a fallback plan. */
    public CarpetRulePresetScreen(Screen parent,String planName,boolean quickApply){
        this(parent,null,null,planName);
        if(quickApply)quickApplyPending=planName;
    }

    public CarpetRulePresetScreen(Screen parent,CarpetRuleCatalog.RuleKey requestedRule,
                                  String requestedValue,String requestedPlan){
        super(MinecraftCompat.translatable("dds-tweak.carpet_rules.plans.title"));
        this.parent=parent;
        this.requestedRule=requestedRule;
        this.requestedValue=requestedValue;
        this.selectedRule=requestedRule;
        if(requestedPlan!=null&&store.get(requestedPlan)!=null)
            this.selected=requestedPlan;
    }

    @Override public boolean isPauseScreen(){return false;}

    @Override protected void init(){
        int side=sidebarWidth(),middle=middleLeft(),middleW=middleWidth();
        int detail=detailLeft(),detailW=detailWidth();
        addCompat(button(width-MARGIN-65,12,62,tr("dds-tweak.carpet_rules.back"),this::back));
        // Right-align the entire import/export/add group to the middle panel inset.
        int middleRight=middle+middleW;
        int transferX=middleW>=300?CarpetRulePresetLayout.transferActionX(middleRight):
                width-MARGIN-197;
        int transferY=middleW>=300?TOP+7:12;
        exportButton=button(transferX+66,transferY,62,
                tr("dds-tweak.carpet_rules.presets.transfer.export"),this::exportPresets);
        importButton=button(transferX,transferY,62,
                tr("dds-tweak.carpet_rules.presets.transfer.import"),this::importPresets);
        addCompat(exportButton);addCompat(importButton);
        // Keep importing accessible from the sidebar when no plan exists.
        emptyImportButton=button(MARGIN+8,listTop()+37,78,
                tr("dds-tweak.carpet_rules.presets.transfer.import"),this::importPresets);
        addCompat(emptyImportButton);
        editPlanButton=iconButton(MARGIN+side-30,TOP+7,20,this::togglePlanEdit);
        createButton=hitButton(createLeft(),createTop(),createWidth(),
                CarpetRulePresetLayout.CREATE_HEIGHT,this::createOrSave);
        int buttonWidth=nameActionWidth();
        int nameActionsX=createLeft()+4;
        deleteNameButton=hitButton(deleteButtonX(),createTop()+4,20,20,this::deleteWithShift);
        cancelNameButton=button(nameActionsX,createTop()+52,buttonWidth,
                tr("dds-tweak.carpet_rules.plans.switch_cancel"),this::cancelNaming);
        saveNameButton=button(nameActionsX+buttonWidth+4,createTop()+52,buttonWidth,
                tr("dds-tweak.carpet_rules.plans.save_name"),this::createOrSave);
        addPainted(editPlanButton);addPainted(createButton);addPainted(deleteNameButton);
        addCompat(cancelNameButton);addCompat(saveNameButton);
        //#if MC >= 11600
        nameBox=new EditBox(font,createLeft()+5,createTop()+25,Math.max(35,createWidth()-10),18,MinecraftCompat.literal(""));
        //#else
        //$$ nameBox=new EditBox(font,createLeft()+5,createTop()+25,Math.max(35,createWidth()-10),18,"");
        //#endif
        nameBox.setMaxLength(64);addCompat(nameBox);
        //#if MC >= 11600
        ruleSearchBox=new EditBox(font,middle+30,TOP+48,Math.max(36,middleW-229),18,MinecraftCompat.literal(""));
        //#else
        //$$ ruleSearchBox=new EditBox(font,middle+30,TOP+48,Math.max(36,middleW-229),18,"");
        //#endif
        ruleSearchBox.setMaxLength(120);ruleSearchBox.setValue(ruleQuery);
        ruleSearchBox.setResponder(value->{ruleQuery=value;pickerScroll.reset();ruleScroll.reset();});
        addCompat(ruleSearchBox);
        //#if MC >= 11600
        importPathBox=new EditBox(font,middle+12,TOP+48,
                Math.max(55,middleW-(middleW>=245?107:24)),18,MinecraftCompat.literal(""));
        //#else
        //$$ importPathBox=new EditBox(font,middle+12,TOP+48,
        //$$         Math.max(55,middleW-(middleW>=245?107:24)),18,"");
        //#endif
        importPathBox.setMaxLength(1024);
        importPathBox.setValue(importFilePath);
        importPathBox.setResponder(value->importFilePath=value);
        addCompat(importPathBox);
        browseImportButton=button(middle+middleW-80,
                TOP+(middleW>=245?47:74),68,
                tr("dds-tweak.carpet_rules.plans.import_browse"),this::browseImportFile);
        addCompat(browseImportButton);
        int halfImport=Math.min(86,Math.max(44,(middleW-32)/2));
        int cancelImportX=middle+middleW-12-halfImport;
        confirmImportButton=button(cancelImportX-halfImport-4,TOP+7,halfImport,
                tr("dds-tweak.carpet_rules.plans.import_confirm"),this::confirmImport);
        cancelImportButton=button(cancelImportX,TOP+7,halfImport,
                tr("dds-tweak.carpet_rules.plans.import_cancel"),this::cancelImport);
        addCompat(confirmImportButton);addCompat(cancelImportButton);
        openExportButton=button(middle+12,height-43,86,
                tr("dds-tweak.carpet_rules.plans.export_open"),this::openExportDirectory);
        addCompat(openExportButton);
        pickOwnerButton=button(middle+middleW-190,TOP+47,90,
                tr("dds-tweak.carpet_rules.plans.source"),()->toggleFilter("owner"));
        pickCategoryButton=button(middle+middleW-96,TOP+47,88,
                tr("dds-tweak.carpet_rules.plans.category"),()->toggleFilter("category"));
        addPainted(pickOwnerButton);addPainted(pickCategoryButton);
        //#if MC >= 11600
        targetBox=new EditBox(font,detail+12,detailTargetY(),Math.max(30,detailW-88),FIELD_H,MinecraftCompat.literal(""));
        //#else
        //$$ targetBox=new EditBox(font,detail+12,detailTargetY(),Math.max(30,detailW-88),FIELD_H,"");
        //#endif
        targetBox.setMaxLength(2048);
        targetBox.setResponder(value->{
            if(refreshingTarget)return;
            planFeedback=false;
            targetDirty=!value.equals(savedTargetValue());
            invalidateBatchReview();
            if(!targetDirty)cancelPendingSwitch();
            feedback=targetDirty?tr("dds-tweak.carpet_rules.plans.unsaved_target"):"";
        });
        addCompat(targetBox);
        valueChoiceButton=button(detailChoiceX(),detailTargetY(),
                CarpetRuleUiStyle.TARGET_ACTION_SIZE,"▾",this::toggleTargetChoices);
        addPainted(valueChoiceButton);
        saveTargetButton=button(detail+12,height-43,Math.max(55,detailW-24),
                tr("dds-tweak.carpet_rules.presets.target_save"),this::saveTarget);
        removeTargetButton=button(detail+12,height-43,20,
                tr("dds-tweak.carpet_rules.presets.target_remove"),this::removeTarget);
        addCompat(saveTargetButton);addCompat(removeTargetButton);
        int switchWidth=Math.max(40,(detailW-32)/3);
        saveSwitchButton=button(detail+12,height-76,switchWidth,
                tr("dds-tweak.carpet_rules.plans.switch_save"),()->resolveSwitch(true));
        discardSwitchButton=button(detail+16+switchWidth,height-76,switchWidth,
                tr("dds-tweak.carpet_rules.plans.switch_discard"),()->resolveSwitch(false));
        cancelSwitchButton=button(detail+20+2*switchWidth,height-76,switchWidth,
                tr("dds-tweak.carpet_rules.plans.switch_cancel"),this::cancelPendingSwitch);
        addCompat(saveSwitchButton);addCompat(discardSwitchButton);addCompat(cancelSwitchButton);
        pickRuleButton=iconButton(CarpetRulePresetLayout.addActionX(middleRight),
                TOP+7,20,this::togglePicker);
        addPainted(pickRuleButton);
        int actionWidth=Math.min(96,Math.max(78,middleW-134));
        int actionX=middle+8;
        applyBatchButton=button(actionX,TOP+8,actionWidth,
                tr("dds-tweak.carpet_rules.plans.apply_one_click"),this::applyPlan);
        stopBatchButton=button(actionX,TOP+8,actionWidth,
                tr("dds-tweak.carpet_rules.presets.batch.stop"),this::stopBatch);
        pickAddButton=button(actionX,TOP+8,actionWidth,
                tr("dds-tweak.carpet_rules.plans.add_checked"),this::addPickedRules);
        addCompat(applyBatchButton);addCompat(stopBatchButton);addCompat(pickAddButton);
        backDetailsButton=button(detail+detailW-76,TOP+8,68,
                tr("dds-tweak.carpet_rules.back"),this::back);
        addCompat(backDetailsButton);
        if(selected==null&&!entries().isEmpty())selected=entries().get(0).name();
        if(selected!=null&&selectedRule==null){
            CarpetRulePresetStore.Preset p=store.get(selected);
            if(p!=null&&!p.values().isEmpty())selectedRule=p.values().keySet().iterator().next();
        }
        refreshTarget();setFocused(importing?importPathBox:null);
        if(quickApplyPending!=null){
            String requested=quickApplyPending;
            quickApplyPending=null; // Resizing/reinitializing must not repeat a server action.
            CarpetRulePresetStore.Preset plan=store.get(requested);
            if(requested.equals(selected)&&plan!=null&&!plan.isSnapshot())
                applyPlan(); // Existing preview, permission and persistent-mode review gates.
            else feedback=tr("dds-tweak.carpet_rules.presets.pick_unavailable");
        }
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


    /** Interaction-only full-width create card; the background is custom-painted. */
    private Button hitButton(int x,int y,int w,int h,Runnable action){
        //#if MC >= 11903
        Button hit=Button.builder(MinecraftCompat.literal(""),b->action.run()).bounds(x,y,w,h).build();
        //#elseif MC >= 11600
        //$$ Button hit=new Button(x,y,w,h,MinecraftCompat.literal(""),b->action.run());
        //#else
        //$$ Button hit=new Button(x,y,w,h,"",b->action.run());
        //#endif
        return hit;
    }

    /** Native square hitbox kept the same size as the self-painted icon. */
    private Button iconButton(int x,int y,int size,Runnable action){
        //#if MC >= 11903
        Button painted=Button.builder(MinecraftCompat.literal(""),b->action.run()).bounds(x,y,size,size).build();
        //#elseif MC >= 11600
        //$$ Button painted=new Button(x,y,size,size,MinecraftCompat.literal(""),b->action.run());
        //#else
        //$$ Button painted=new Button(x,y,size,size,"",b->action.run());
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



    private final CarpetRuleMetadata metadata=CarpetLocalMetadata.discover();

    private void back(){
        if(importing&&browsingImport){browsingImport=false;return;}
        if(importing){cancelImport();return;}
        if(valueChoicesOpen){valueChoicesOpen=false;return;}
        if(pendingSwitch){cancelPendingSwitch();feedback=tr("dds-tweak.carpet_rules.plans.unsaved_target");return;}
        if(targetDirty){
            refreshTarget();
            feedback="";
            return;
        }
        if(!filterPopup.isEmpty()){
            filterPopup="";
            return;
        }
        if(pickingRule){
            pickingRule=false;
            pickedRules.clear();
            filterPopup="";
            setFocused(null);
            return;
        }
        if(naming){cancelNaming();return;}
        if(narrowDetail){
            narrowDetail=false;
            return;
        }
        if(narrow()&&selected!=null){
            if(!narrowSidebar){narrowSidebar=true;return;}
            selected=null;
            return;
        }
        onClose();
    }

    /** An unsaved placeholder is rendered as a row, not persisted until confirmed. */
    private void createOrSave(){
        if(batchActive()||pickingRule)return;
        planFeedback=true;
        if(!naming){
            if(targetDirty){
                feedback=tr("dds-tweak.carpet_rules.plans.unsaved_target");
                return;
            }
            namePreviousPlan=selected;
            namePreviousRule=selectedRule;
            selected=null;
            selectedRule=null;
            narrowDetail=false;
            renaming=false;
            naming=true;
            filterPopup="";
            valueChoicesOpen=false;
            nameBox.setValue("");
            setFocused(nameBox);
            feedback="";
            return;
        }
        String requestedName=nameBox.getValue();
        if(renaming){
            String previousName=selected;
            if(!store.rename(previousName,requestedName)){
                feedback=tr("dds-tweak.carpet_rules.presets.invalid_name");
                return;
            }
            if(!store.save()){
                store.rename(requestedName,previousName);
                feedback=tr("dds-tweak.carpet_rules.presets.save_failed");
                return;
            }
            selected=requestedName;
        }else{
            if(!savePreset())return;
        }
        naming=false;
        renaming=false;
        namePreviousPlan=null;
        namePreviousRule=null;
        nameBox.setValue("");
        setFocused(null);
        revealSelectedPlan(false);
        refreshTarget();
        feedback=tr("dds-tweak.carpet_rules.presets.saved");
    }

    private void cancelNaming(){
        if(!naming)return;
        if(!renaming){
            selected=namePreviousPlan;
            selectedRule=namePreviousRule;
        }
        naming=renaming=false;
        namePreviousPlan=null;
        namePreviousRule=null;
        nameBox.setValue("");
        setFocused(null);
        refreshTarget();
        feedback="";
    }

    private void startRename(){
        CarpetRulePresetStore.Preset p=store.get(selected);
        if(p==null||batchActive()||pickingRule||targetDirty)return;
        naming=true;
        renaming=true;
        filterPopup="";
        valueChoicesOpen=false;
        planFeedback=true;
        nameBox.setValue(p.name());
        revealSelectedPlan(true);
        positionNameWidgets();
        setFocused(nameBox);
        feedback="";
    }

    /** The upper-right pencil expands the selected plan in place. */
    private void togglePlanEdit(){
        if(naming){
            if(renaming)cancelNaming();
            return;
        }
        startRename();
    }

    private void selectApplyMode(CarpetRuleCommands.Mode mode){
        planFeedback=false;
        CarpetRulePresetStore.Preset p=store.get(selected);
        if(p==null||p.isSnapshot()||batchActive()||p.applyMode()==mode)return;
        if(!store.setApplyMode(selected,mode))return;
        invalidateBatchReview();
        feedback=store.save()?tr("dds-tweak.carpet_rules.plans.mode_saved"):
                tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    private String pickerOwner(CarpetRuleCatalog.RemoteRule rule){
        String verified=CarpetRuleClientNetwork.integratedOwner(rule.key());
        return verified==null?UNKNOWN_OWNER:verified;
    }

    private String pickerOwnerLabel(String owner){
        if(UNKNOWN_OWNER.equals(owner))
            return tr("dds-tweak.carpet_rules.owner_unverified");
        return CarpetRuleSidebar.sourceLabel(owner,null);
    }

    private List<String> pickerSources(){
        Set<String> owners=new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(CarpetRuleCatalog.RemoteRule rule:CarpetRuleClientNetwork.catalog().snapshot())
            owners.add(pickerOwner(rule));
        return new ArrayList<>(owners);
    }

    private List<String> pickerCategories(){
        Set<String> tags=new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(CarpetRuleCatalog.RemoteRule rule:CarpetRuleClientNetwork.catalog().snapshot())
            tags.addAll(pickerTags(rule));
        return new ArrayList<>(tags);
    }

    private List<String> pickerTags(CarpetRuleCatalog.RemoteRule rule){
        List<String> fromServer=CarpetRuleAuthority.categories(rule.key());
        if(!fromServer.isEmpty())return fromServer;
        CarpetRuleMetadata.Description info=metadata.find(rule.key());
        return info==null?java.util.Collections.emptyList():new ArrayList<>(info.categories());
    }

    private String pickerName(CarpetRuleCatalog.RemoteRule rule){
        CarpetRuleMetadata.Description info=metadata.find(rule.key());
        return info==null?rule.key().name():info.displayName();
    }

    private String pickerLabel(String tag){
        switch(tag.toLowerCase(Locale.ROOT)){
            case "survival":case "command":case "creative":
            case "feature":case "optimization":case "debug":
                return tr("dds-tweak.carpet_rules.category."+tag.toLowerCase(Locale.ROOT));
            default:return tag;
        }
    }

    private void toggleFilter(String kind){
        if(!showMiddle())return;
        filterPopup=kind.equals(filterPopup)?"":kind;
        filterOffset=0;
    }

    private List<String> filterValues(){
        List<String> options=new ArrayList<>();
        options.add("*");
        options.addAll("owner".equals(filterPopup)?pickerSources():pickerCategories());
        return options;
    }

    private int filterX(){return middleLeft()+middleWidth()-("category".equals(filterPopup)?96:190);}
    private int filterWidth(){return Math.min(210,Math.max(70,middleLeft()+middleWidth()-8-filterX()));}
    private int filterTop(){return TOP+71;}
    private int filterRows(){return Math.max(1,Math.min(10,(listHeight()-20)/22));}

    private boolean handleFilterClick(double x,double y,int button){
        if(filterPopup.isEmpty())return false;
        //#if MC >= 260300
        //$$ if(button!=InputConstants.MOUSE_BUTTON_LEFT){filterPopup="";return true;}
        //#else
        if(button!=0){filterPopup="";return true;}
        //#endif
        if(y>=TOP+47&&y<TOP+67&&x>=middleLeft()+middleWidth()-190
                &&x<middleLeft()+middleWidth()-8){
            toggleFilter(x<middleLeft()+middleWidth()-98?"owner":"category");
            return true;
        }
        int px=filterX(),py=filterTop(),rows=filterRows();
        if(x>=px&&x<px+filterWidth()&&y>=py&&y<py+Math.min(rows,filterValues().size())*22+4){
            int index=filterOffset+(int)((y-py-2)/22);
            List<String> options=filterValues();
            if(y>=py+2&&index>=0&&index<options.size()){
                if("owner".equals(filterPopup))pickerOwner=options.get(index);
                else pickerCategory=options.get(index);
                pickerScroll.reset();
            }
        }
        filterPopup="";
        return true; // Never toggle a rule through an open filter.
    }


    private void addPickedRules(){
        planFeedback=false;
        CarpetRulePresetStore.Preset preset=store.get(selected);
        if(preset==null||preset.isSnapshot()||batchActive())return;
        int added=0;
        for(CarpetRuleCatalog.RuleKey key:pickedRules){
            if(preset.values().containsKey(key))continue;
            CarpetRuleCatalog.RemoteRule rule=CarpetRuleClientNetwork.catalog().get(key);
            if(rule!=null&&store.setTarget(selected,key,rule.value()))added++;
        }
        CarpetRuleCatalog.RuleKey first=pickedRules.isEmpty()?null:pickedRules.iterator().next();
        pickedRules.clear();
        pickingRule=false;
        filterPopup="";
        if(first!=null){selectedRule=first;refreshTarget();}
        invalidateBatchReview();
        feedback=store.save()?tr("dds-tweak.carpet_rules.plans.added",added):
                tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    /** One click for a compatible runtime plan, a second click for risk/persistence review. */
    private void applyPlan(){
        planFeedback=false;
        if(batchActive()||store.get(selected)==null)return;
        if(targetDirty){feedback=tr("dds-tweak.carpet_rules.plans.unsaved_target");return;}
        if(reviewedEntries!=null){
            confirmBatch();
            return;
        }
        reviewBatch();
        CarpetRulePresetStore.Preset p=store.get(selected);
        if(reviewedEntries!=null &&p.applyMode()==CarpetRuleCommands.Mode.TEMPORARY){
            boolean clean=true;
            for(CarpetRuleBatchReview.Entry e:reviewedEntries)
                if(e.status()!=CarpetRuleBatchReview.Status.SAME
                        &&(e.status()!=CarpetRuleBatchReview.Status.NEEDS_PERMISSION_CHECK
                        ||!sender.canSend(e.key(),p.applyMode())))
                    clean=false;
            if(clean)confirmBatch();
        }
    }

    /** Export notifications are transient and never grant access to another plan. */
    private void clearExportNotice(){
        if(feedback.equals(exportNoticeText))feedback="";
        exportedFile=null;
        exportNoticeDeadline=0;
        exportNoticeText="";
    }

    private void expireExportNotice(){
        if(exportedFile!=null&&System.nanoTime()-exportNoticeDeadline>=0)
            clearExportNotice();
    }

    private void exportPresets(){
        if(batchActive())return;
        planFeedback=false;
        clearExportNotice();
        CarpetRulePresetStore.Preset current=store.get(selected);
        if(current==null||current.isSnapshot()||current.values().isEmpty()){
            feedback=tr("dds-tweak.carpet_rules.plans.empty_export");
            return;
        }
        java.nio.file.Path destination=CarpetRulePresetTransfer.exportFile(selected);
        if(!CarpetRulePresetTransfer.exportSelectedTo(destination,store,selected)){
            feedback=tr("dds-tweak.carpet_rules.presets.transfer.failed");
            return;
        }
        exportedFile=destination;
        exportNoticeDeadline=System.nanoTime()+EXPORT_NOTICE_NANOS;
        exportNoticeText=tr("dds-tweak.carpet_rules.presets.transfer.exported",selected);
        feedback=exportNoticeText;
    }

    /** Explicit local path entry supports imported files without arbitrary remote paths. */
    private void importPresets(){
        if(batchActive()||pickingRule||naming)return;
        if(targetDirty){
            feedback=tr("dds-tweak.carpet_rules.plans.unsaved_target");
            return;
        }
        clearExportNotice();
        if(importing)cancelNativePicker();
        importing=!importing;
        browsingImport=false;
        planFeedback=false;
        feedback="";
        filterPopup="";
        invalidateBatchReview();
        setFocused(importing?importPathBox:null);
    }

    /** Native system chooser first; use the in-game browser only if unavailable. */
    private void browseImportFile(){
        if(!importing||nativePickerPending)return;
        if(browsingImport){browsingImport=false;setFocused(importPathBox);return;}
        java.nio.file.Path exports=CarpetRulePresetTransfer.exportDirectory();
        try{
            java.nio.file.Files.createDirectories(exports);
        }catch(java.io.IOException|SecurityException ignored){
            // The native picker may still open from the parent configuration folder.
        }
        java.nio.file.Path directory=CarpetRuleLocalFiles.browseDirectory(
                importBrowseStart(),CarpetRulePresetTransfer.exchangeFile());
        long request=++nativePickerGeneration;
        nativePickerPending=true;
        nativePicker=CarpetRuleLocalFiles.chooseNativeJson(directory,
                chosen->minecraft.execute(()->finishNativePicker(request,chosen,false)),
                ()->minecraft.execute(()->finishNativePicker(request,null,true)));
    }

    /** An OS Cancel selection is not an API failure; no fallback should appear. */
    private void finishNativePicker(long request,java.nio.file.Path chosen,boolean unavailable){
        if(request!=nativePickerGeneration)return;
        nativePickerPending=false;
        nativePicker=null;
        if(!importing||MinecraftCompat.screen(minecraft)!=this)return;
        if(unavailable){
            browseImportInGame();
            feedback=tr("dds-tweak.carpet_rules.plans.import_browse_fallback");
        }else if(chosen!=null){
            importPathBox.setValue(chosen.toAbsolutePath().toString());
            browsingImport=false;
            setFocused(importPathBox);
        }
    }

    /** Ignore any callbacks after cancelling or closing, without locking the browse button. */
    private void cancelNativePicker(){
        ++nativePickerGeneration;
        CarpetRuleLocalFiles.NativePicker picker=nativePicker;
        nativePicker=null;
        nativePickerPending=false;
        if(picker!=null)picker.cancel();
    }

    private java.nio.file.Path importBrowseStart(){
        String typed=importPathBox.getValue().trim();
        if(!typed.isEmpty()){
            try{
                java.nio.file.Path requested=java.nio.file.Paths.get(typed);
                if(java.nio.file.Files.isDirectory(requested)
                        ||java.nio.file.Files.isRegularFile(requested))return requested;
            }catch(RuntimeException ignored){}
        }
        return CarpetRulePresetTransfer.exportDirectory();
    }

    private void browseImportInGame(){
        showImportDirectory(CarpetRuleLocalFiles.browseDirectory(
                importBrowseStart(),CarpetRulePresetTransfer.exchangeFile()));
        browsingImport=true;
        setFocused(null);
    }

    private void showImportDirectory(java.nio.file.Path directory){
        importBrowseDirectory=directory;
        importBrowseEntries=CarpetRuleLocalFiles.jsonEntries(directory);
        importBrowseOffset=0;
    }

    private int importBrowserTop(){return TOP+135;}
    private int importBrowserRows(){
        return Math.max(0,(rulesBottom()-importBrowserTop()-12)/24);
    }
    private boolean hasImportParent(){
        return importBrowseDirectory!=null&&importBrowseDirectory.getParent()!=null;
    }
    private int importBrowserSize(){
        return importBrowseEntries.size()+(hasImportParent()?1:0);
    }

    private boolean clickImportBrowser(double x,double y){
        if(!browsingImport||x<middleLeft()+8||x>=middleLeft()+middleWidth()-8
                ||y<importBrowserTop()||y>=importBrowserTop()+importBrowserRows()*24)
            return false;
        int index=importBrowseOffset+(int)(y-importBrowserTop())/24;
        if(index>=importBrowserSize())return true;
        if(hasImportParent()&&index==0){
            showImportDirectory(importBrowseDirectory.getParent());
            return true;
        }
        java.nio.file.Path item=importBrowseEntries.get(index-(hasImportParent()?1:0));
        if(java.nio.file.Files.isDirectory(item))showImportDirectory(item);
        else{
            importPathBox.setValue(item.toAbsolutePath().toString());
            browsingImport=false;
            setFocused(importPathBox);
        }
        return true;
    }

    private void openExportDirectory(){
        expireExportNotice();
        if(exportedFile==null)return;
        CarpetRuleLocalFiles.openDirectory(exportedFile.getParent(),
                ok->{if(!ok)fileOpenFailed=true;});
    }

    private void cancelImport(){
        cancelNativePicker();
        importing=false;
        browsingImport=false;
        setFocused(null);
        feedback="";
    }

    private void confirmImport(){
        if(!importing||batchActive())return;
        planFeedback=false;
        try{
            String typed=importPathBox.getValue().trim();
            if(typed.length()>1&&typed.startsWith("\"")&&typed.endsWith("\""))
                typed=typed.substring(1,typed.length()-1);
            if(typed.isEmpty())throw new IllegalArgumentException("empty path");
            java.nio.file.Path file=java.nio.file.Paths.get(typed);
            if(!java.nio.file.Files.isRegularFile(file)){
                feedback=tr("dds-tweak.carpet_rules.plans.import_missing");
                return;
            }
            int imported=CarpetRulePresetTransfer.importFrom(file,store);
            if(imported<0){
                feedback=tr("dds-tweak.carpet_rules.presets.transfer.invalid");
                return;
            }
            if(!store.save()){
                feedback=tr("dds-tweak.carpet_rules.presets.save_failed");
                return;
            }
            cancelNativePicker();
            importing=false;
            browsingImport=false;
            setFocused(null);
            feedback=tr("dds-tweak.carpet_rules.presets.transfer.imported",imported);
        }catch(RuntimeException invalid){
            feedback=tr("dds-tweak.carpet_rules.plans.import_missing");
        }
    }

    private void togglePicker(){
        if(batchActive())return;
        planFeedback=false;
        valueChoicesOpen=false;
        CarpetRulePresetStore.Preset preset=store.get(selected);
        if(preset==null||preset.isSnapshot()||!CarpetRuleClientNetwork.catalog().isReady()){
            feedback=tr("dds-tweak.carpet_rules.presets.pick_unavailable");
            return;
        }
        pickingRule=!pickingRule;
        narrowDetail=false;
        pickedRules.clear();
        pickerOwner=pickerCategory="*";
        filterPopup="";
        ruleSearchBox.setValue("");
        pickerScroll.reset();
        invalidateBatchReview();
    }

    private List<CarpetRuleCatalog.RemoteRule> matchingRules(){
        String query=ruleQuery.trim().toLowerCase(Locale.ROOT);
        List<CarpetRuleCatalog.RemoteRule> matches=new ArrayList<>();
        for(CarpetRuleCatalog.RemoteRule rule:CarpetRuleClientNetwork.catalog().snapshot()){
            if(!"*".equals(pickerOwner)&&!pickerOwner(rule).equalsIgnoreCase(pickerOwner))continue;
            boolean categoryMatch="*".equals(pickerCategory);
            if(!categoryMatch)for(String tag:pickerTags(rule))
                if(tag.equalsIgnoreCase(pickerCategory)){categoryMatch=true;break;}
            if(!categoryMatch)continue;
            String searchText=pickerName(rule)+" "+rule.key().name()+" "+
                    rule.key().manager()+" "+pickerOwner(rule)+" "+String.join(" ",pickerTags(rule));
            if(query.isEmpty()||searchText.toLowerCase(Locale.ROOT).contains(query))
                matches.add(rule);
        }
        matches.sort((a,b)->a.key().toString().compareToIgnoreCase(b.key().toString()));
        return matches;
    }

    private void invalidateBatchReview(){
        reviewedEntries=null;
        reviewedName=null;
    }

    private boolean batchActive(){
        return batch.phase()==CarpetRuleBatchExecutor.Phase.RUNNING;
    }

    private String batchFailureReason(){
        return batch.failure()==CarpetRuleBatchExecutor.Failure.PERSISTENCE_MISMATCH
                ?tr("dds-tweak.carpet_rules.plans.failure.persistence_mismatch")
                :batch.failure().name();
    }

    private String batchCompletedMessage(){
        if(batch.mode()!=CarpetRuleCommands.Mode.SET_DEFAULT)
            return "dds-tweak.carpet_rules.plans.completed_runtime";
        return batch.persistenceVerified()
                ?"dds-tweak.carpet_rules.plans.completed_persistent_verified"
                :"dds-tweak.carpet_rules.plans.completed_persistent";
    }

    private void reviewBatch(){
        if(batchActive()||selected==null)return;
        invalidateBatchReview();
        CarpetRulePresetStore.Preset preset=store.get(selected);
        if(preset==null||preset.isSnapshot())return;
        List<CarpetRuleBatchReview.Entry> entries=review();
        int pending=0,skipped=0;
        for(CarpetRuleBatchReview.Entry entry:entries){
            if(entry.status()==CarpetRuleBatchReview.Status.SAME)continue;
            if(entry.status()!=CarpetRuleBatchReview.Status.NEEDS_PERMISSION_CHECK
                    ||!sender.canSend(entry.key(),preset.applyMode()))skipped++;
            else pending++;
        }
        if(pending==0){
            feedback=tr("dds-tweak.carpet_rules.plans.no_eligible",skipped);
            return;
        }
        if(pending>CarpetRuleBatchExecutor.MAX_CHANGES){
            feedback=tr("dds-tweak.carpet_rules.presets.batch.count_invalid");
            return;
        }
        reviewedName=selected;
        reviewedGeneration=CarpetRuleClientNetwork.catalog().generation();
        reviewedEntries=entries;
        feedback=tr("dds-tweak.carpet_rules.plans.review_summary",pending,skipped);
    }

    private void confirmBatch(){
        if(batchActive()||reviewedEntries==null||!reviewedName.equals(selected)){
            feedback=tr("dds-tweak.carpet_rules.presets.batch.recheck");
            return;
        }
        CarpetRuleCatalog catalog=CarpetRuleClientNetwork.catalog();
        List<CarpetRuleBatchReview.Entry> current=review();
        boolean unchanged=catalog.isReady()&&catalog.generation()==reviewedGeneration
                &&current.size()==reviewedEntries.size();
        for(int i=0;unchanged&&i<current.size();i++){
            CarpetRuleBatchReview.Entry a=current.get(i),b=reviewedEntries.get(i);
            unchanged=a.key().equals(b.key())&&a.status()==b.status()
                    &&java.util.Objects.equals(a.current(),b.current())&&a.target().equals(b.target());
        }
        invalidateBatchReview();
        if(!unchanged){
            feedback=tr("dds-tweak.carpet_rules.presets.batch.recheck");
            return;
        }
        if(!batch.start(catalog,store.get(selected),sender,
                store.get(selected).applyMode())){
            feedback=tr("dds-tweak.carpet_rules.presets.batch.stopped",batchFailureReason());
            return;
        }
        feedback=tr("dds-tweak.carpet_rules.plans.running",0,batch.total(),batchSkipped());
    }

    private int batchSkipped(){
        return batch.skippedMissing()+batch.skippedInvalid()+batch.skippedPermission();
    }

    private void stopBatch(){
        if(!batchActive())return;
        batch.cancel();
        invalidateBatchReview();
        feedback=tr("dds-tweak.carpet_rules.plans.cancelled",batch.completed(),
                batch.total(),batchSkipped());
    }

    private void updateBatch(){
        if(!batchActive())return;
        batch.advance(CarpetRuleClientNetwork.catalog(),CarpetRuleClientNetwork.operations(),sender,System.currentTimeMillis());
        if(batch.phase()==CarpetRuleBatchExecutor.Phase.COMPLETED)
            feedback=tr(batchCompletedMessage(),batch.completed(),
                    batch.skippedMissing()+batch.skippedInvalid()+batch.skippedPermission());
        else if(batch.phase()==CarpetRuleBatchExecutor.Phase.STOPPED)
            feedback=tr("dds-tweak.carpet_rules.plans.stopped",batch.completed(),
                    batch.total(),batchSkipped(),batchFailureReason());
        else
            feedback=tr("dds-tweak.carpet_rules.plans.running",batch.completed(),
                    batch.total(),batchSkipped());
    }

    private boolean savePreset(){
        if(batchActive()||pickingRule||nameBox==null)return false;
        String name=nameBox.getValue();
        if(!store.put(name,"",System.currentTimeMillis(),false,new java.util.LinkedHashMap<>())){
            feedback=tr("dds-tweak.carpet_rules.presets.invalid_name");
            return false;
        }
        if(!store.save()){
            store.delete(name);
            feedback=tr("dds-tweak.carpet_rules.presets.save_failed");
            return false;
        }
        invalidateBatchReview();
        selected=name;
        selectedRule=null;
        ruleScroll.reset();
        return true;
    }

    private String savedTargetValue(){
        CarpetRulePresetStore.Preset plan=store.get(selected);
        if(plan==null||selectedRule==null)return "";
        CarpetRuleCatalog.RemoteRule remote=CarpetRuleClientNetwork.catalog().get(selectedRule);
        String current=remote==null?"":remote.value();
        if(selectedRule.equals(requestedRule)&&requestedValue!=null)current=requestedValue;
        return plan.values().getOrDefault(selectedRule,current);
    }

    private void refreshTarget(){
        valueChoicesOpen=false;
        if(targetBox==null)return;
        refreshingTarget=true;
        targetBox.setValue(savedTargetValue());
        targetBox.setCursorPosition(0);
        targetBox.setHighlightPos(0);
        refreshingTarget=false;
        targetDirty=false;
        cancelPendingSwitch();
    }

    private void cancelPendingSwitch(){
        pendingSwitch=false;
        pendingPlan=null;
        pendingRule=null;
    }

    private void requestNavigation(String plan,CarpetRuleCatalog.RuleKey rule,boolean details){
        if(java.util.Objects.equals(plan,selected)&&java.util.Objects.equals(rule,selectedRule)
                &&(wide()||details==narrowDetail)&&!(narrow()&&narrowSidebar))return;
        if(targetDirty){
            pendingSwitch=true;
            pendingPlan=plan;
            pendingRule=rule;
            pendingDetails=details;
            feedback=tr("dds-tweak.carpet_rules.plans.switch_prompt");
            return;
        }
        navigateTo(plan,rule,details);
    }

    private void navigateTo(String plan,CarpetRuleCatalog.RuleKey rule,boolean details){
        if(batchActive())return;
        CarpetRulePresetStore.Preset target=store.get(plan);
        if(target==null||target.isSnapshot()||rule!=null&&!target.values().containsKey(rule))return;
        boolean changedPlan=!java.util.Objects.equals(selected,plan);
        selected=plan;
        if(narrow())narrowSidebar=false;
        if(!java.util.Objects.equals(selectedRule,rule))descriptionOffset=0;
        selectedRule=rule;
        planFeedback=false;
        if(changedPlan){
            clearExportNotice();
            ruleScroll.reset();
            pickingRule=false;
            filterPopup="";
        }
        narrowDetail=details;
        invalidateBatchReview();
        refreshTarget();
        feedback="";
    }

    private void resolveSwitch(boolean save){
        if(!pendingSwitch)return;
        String nextPlan=pendingPlan;
        CarpetRuleCatalog.RuleKey nextRule=pendingRule;
        boolean nextDetails=pendingDetails;
        if(save){
            saveTarget();
            if(targetDirty)return; // Stay on the editor when saving fails.
        }else refreshTarget(); // Discard only the local edit, never write the server.
        navigateTo(nextPlan,nextRule,nextDetails);
    }

    private void saveTarget(){
        if(batchActive())return;
        planFeedback=false;
        if(targetBox==null||selectedRule==null){
            feedback=tr("dds-tweak.carpet_rules.presets.target_invalid");
            return;
        }
        CarpetRuleMultiValue combination=CarpetRuleAuthority.multiValue(selectedRule);
        if(combination!=null&&combination.parse(targetBox.getValue())==null){
            feedback=tr("dds-tweak.carpet_rules.presets.target_invalid");
            return;
        }
        invalidateBatchReview();
        if(!store.setTarget(selected,selectedRule,targetBox.getValue())){
            feedback=tr("dds-tweak.carpet_rules.presets.target_invalid");
            return;
        }
        boolean saved=store.save();
        targetDirty=!saved;
        if(saved)cancelPendingSwitch();
        feedback=tr(saved?"dds-tweak.carpet_rules.presets.saved":
                "dds-tweak.carpet_rules.presets.save_failed");
    }

    /** Reuse the same cross-version Shift state as Camera's Shift+click actions. */
    private boolean shiftHeld(){
        return MinecraftCompat.shiftDown(minecraft);
    }

    /** Destructive list action: explicit Shift modifier is always required. */
    private void removeRuleWithShift(CarpetRuleCatalog.RuleKey key){
        if(!shiftHeld()){
            planFeedback=false;
            feedback=tr("dds-tweak.carpet_rules.plans.shift_remove");
            return;
        }
        if(batchActive()||targetDirty){
            feedback=tr("dds-tweak.carpet_rules.plans.unsaved_target");
            return;
        }
        if(store.get(selected)==null||key==null)return;
        invalidateBatchReview();
        if(!store.removeTarget(selected,key))return;
        if(key.equals(selectedRule)){
            selectedRule=null;
            refreshTarget();
        }
        planFeedback=false;
        feedback=store.save()
                ?tr("dds-tweak.carpet_rules.presets.target_removed")
                :tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    private void removeTarget(){
        if(batchActive()||!shiftHeld())return;
        planFeedback=false;
        invalidateBatchReview();
        if(selectedRule==null||!store.removeTarget(selected,selectedRule))return;
        selectedRule=null;
        refreshTarget();
        feedback=store.save()?tr("dds-tweak.carpet_rules.presets.target_removed"):tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    private List<CarpetRuleBatchReview.Entry> review(){
        return CarpetRuleBatchReview.preview(CarpetRuleClientNetwork.catalog(),store.get(selected));
    }


    /** A destructive plan operation must check Shift at the actual click. */
    private void deleteWithShift(){
        if(!naming||!renaming||batchActive()||pickingRule||selected==null)return;
        if(!shiftHeld())return; // Shift guidance is available only on delete-icon hover.
        if(targetDirty){
            feedback=tr("dds-tweak.carpet_rules.plans.unsaved_target");
            planFeedback=true;
            return;
        }
        planFeedback=true;
        invalidateBatchReview();
        if(!store.delete(selected))return;
        selected=null;
        selectedRule=null;
        naming=renaming=false;
        nameBox.setValue("");
        setFocused(null);
        refreshTarget();
        feedback=store.save()?tr("dds-tweak.carpet_rules.presets.deleted"):
                tr("dds-tweak.carpet_rules.presets.save_failed");
    }

    private List<CarpetRulePresetStore.Preset> entries(){
        return new ArrayList<>(store.editablePlans());
    }

    private int planIndex(List<CarpetRulePresetStore.Preset> plans){
        if(selected==null)return -1;
        for(int i=0;i<plans.size();i++)if(selected.equals(plans.get(i).name()))return i;
        return -1;
    }

    private int createLeft(){return MARGIN+8;}
    private int createWidth(){return sidebarWidth()-16;}
    private int createTop(){return TOP+44;}
    private int createHeight(){return CarpetRulePresetLayout.createHeight(naming&&!renaming);}
    private int expandedIndex(){return renaming?planIndex(entries()):-1;}
    private int planY(int index,int expanded){
        return CarpetRulePresetLayout.rowY(index,offset,listTop(),expanded);
    }

    private int nameActionWidth(){return Math.max(24,(createWidth()-12)/2);}
    private int deleteButtonX(){return createLeft()+createWidth()-24;}

    private void revealSelectedPlan(boolean expanded){
        List<CarpetRulePresetStore.Preset> plans=entries();
        int index=planIndex(plans);
        if(index<0)return;
        int spare=expanded?CarpetRulePresetLayout.EXPANSION:0;
        int visible=Math.max(1,(listBottom()-listTop()-spare)/ROW);
        offset=Math.min(offset,index);
        offset=Math.min(offset,Math.max(0,plans.size()-visible));
        while(index>=offset+visible&&offset<index)offset++;
        while(expanded&&index>offset&&planY(index,index)+
                CarpetRulePresetLayout.EXPANDED_HEIGHT>listBottom())offset++;
    }

    /** Native widgets move with the expanded card, not with the screen footer. */
    private void positionWidget(AbstractWidget widget,int x,int y){
        //#if MC >= 11904
        widget.setX(x);
        widget.setY(y);
        //#else
        //$$ widget.x=x;
        //$$ widget.y=y;
        //#endif
    }

    private void positionNameWidgets(){
        if(!naming)return;
        int y=createTop();
        if(renaming){
            int index=expandedIndex();
            if(index<0)return;
            y=planY(index,index);
        }
        int x=createLeft(),bw=nameActionWidth();
        positionWidget(nameBox,x+5,y+25);
        nameBox.setWidth(Math.max(35,createWidth()-10));
        positionWidget(deleteNameButton,deleteButtonX(),y+4);
        positionWidget(cancelNameButton,x+4,y+52);
        positionWidget(saveNameButton,x+8+bw,y+52);
    }

    private boolean wide(){return CarpetRuleScreenLayout.wide(width);}
    private boolean narrow(){return !wide();}
    private int sidebarWidth(){return narrow()?width-2*MARGIN:Math.min(172,Math.max(112,width/5));}
    private int middleLeft(){return narrow()?MARGIN:MARGIN+sidebarWidth()+8;}
    private int middleWidth(){
        if(narrow())return width-MARGIN-middleLeft();
        int available=width-middleLeft()-MARGIN-8;
        return CarpetRuleScreenLayout.middleWidth(available);
    }
    private int detailLeft(){return wide()?middleLeft()+middleWidth()+8:middleLeft();}
    private int detailWidth(){return width-MARGIN-detailLeft();}
    private int detailResetX(){
        return detailLeft()+detailWidth()-CarpetRuleUiStyle.TARGET_RIGHT_PADDING
                -CarpetRuleUiStyle.TARGET_ACTION_SIZE;
    }
    private int detailChoiceX(){
        return detailResetX()-CarpetRuleUiStyle.TARGET_ACTION_GAP
                -CarpetRuleUiStyle.TARGET_ACTION_SIZE;
    }
    private int listTop(){return createTop()+createHeight()+CarpetRulePresetLayout.GAP;}
    private int rulesTop(){return TOP+83;}
    private int detailPanelHeight(){return Math.max(0,height-TOP-15);}
    private int descriptionTop(){return CarpetRuleDetailLayout.descriptionTop(TOP);}
    private int descriptionHeight(){return CarpetRuleDetailLayout.descriptionHeight(detailPanelHeight());}
    private int detailValuesY(){return CarpetRuleDetailLayout.valuesTop(TOP,detailPanelHeight());}
    private int detailTargetY(){return CarpetRuleDetailLayout.targetTop(TOP,detailPanelHeight());}
    private int modeRowY(int index){
        return CarpetRuleDetailLayout.modeTop(TOP,detailPanelHeight())+6+index*24;
    }
    private int listBottom(){return Math.max(listTop()+ROW,height-42);}
    private int rulesBottom(){return Math.max(rulesTop()+ROW,height-71);}
    private int rows(){return Math.max(1,(listBottom()-listTop())/ROW);}
    private int planRows(){return Math.max(1,(listBottom()-listTop()-
            (renaming?CarpetRulePresetLayout.EXPANSION:0))/ROW);}
    private int ruleRows(){return Math.max(1,(rulesBottom()-rulesTop())/ROW);}
    private int listHeight(){return Math.max(ROW,height-TOP-44);}
    private boolean showSidebar(){
        return !narrow()||!importing&&(narrowSidebar||selected==null&&!pickingRule);
    }
    private boolean showMiddle(){
        return (selected!=null||!narrow()||importing)&&!narrowDetail&&
                (!narrow()||!narrowSidebar||importing);
    }
    private boolean showDetail(){
        return (wide()||narrowDetail)&&!pickingRule&&!(narrow()&&narrowSidebar);
    }

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float pt){
    //$$     if(minecraft.level!=null)extractBlurredBackground(g);
    //$$ }
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){mouseX=mx;mouseY=my;renderBase(g);renderDetails(g);super.extractRenderState(g,mx,my,pt);renderPaintedControls(g);if(!filterPopup.isEmpty())renderFilterPopup(g);if(valueChoicesOpen)renderTargetChoicePopup(g);renderListTooltip(g);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float pt){
        //#if MC >= 12005
        // MaLiLib-style shared mask replaces vanilla background blur.
        //#else
        //$$ // No vanilla background; the shared MaLiLib-style mask owns this layer.
        //#endif
    }
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderCarpetBlur(g,pt);
        mouseX=mx;mouseY=my;renderBase(g);renderDetails(g);
        super.render(g,mx,my,pt);
        renderPaintedControls(g);
        if(!filterPopup.isEmpty())renderFilterPopup(g);
        if(valueChoicesOpen)renderTargetChoicePopup(g);renderListTooltip(g);
    }
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics g,int mx,int my,float pt){renderBackground(g);mouseX=mx;mouseY=my;renderBase(g);renderDetails(g);super.render(g,mx,my,pt);renderPaintedControls(g);if(!filterPopup.isEmpty())renderFilterPopup(g);if(valueChoicesOpen)renderTargetChoicePopup(g);renderListTooltip(g);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack g,int mx,int my,float pt){renderBackground(g);mouseX=mx;mouseY=my;renderBase(g);renderDetails(g);super.render(g,mx,my,pt);renderPaintedControls(g);if(!filterPopup.isEmpty())renderFilterPopup(g);if(valueChoicesOpen)renderTargetChoicePopup(g);renderListTooltip(g);}
    //#else
    //$$ @Override public void render(int mx,int my,float pt){mouseX=mx;mouseY=my;renderBase(null);renderDetails(null);super.render(mx,my,pt);renderPaintedControls(null);if(!filterPopup.isEmpty())renderFilterPopup(null);if(valueChoicesOpen)renderTargetChoicePopup(null);renderListTooltip(null);}
    //#endif

    /** The three panels share the main rule browser's background and rounded geometry. */
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
        updateBatch();
        expireExportNotice();
        if(fileOpenFailed){
            fileOpenFailed=false;
            feedback=tr("dds-tweak.carpet_rules.plans.export_open_failed");
        }
        CarpetRulePresetStore.Preset current=store.get(selected);
        boolean hasPlan=current!=null&&!current.isSnapshot();
        boolean available=hasPlan&&!batchActive()&&!pickingRule;
        boolean detail=showDetail()&&selectedRule!=null&&hasPlan&&!importing&&!naming;
        boolean booleanValue=detail&&booleanTarget();
        if(naming&&renaming)revealSelectedPlan(true);
        if(naming)positionNameWidgets();
        nameBox.visible=naming&&showSidebar();
        createButton.visible=showSidebar()&&(!naming||renaming);
        createButton.active=!naming&&!batchActive()&&!pickingRule&&!targetDirty&&!importing;
        saveNameButton.visible=cancelNameButton.visible=showSidebar()&&naming;
        //#if MC >= 11600
        saveNameButton.setMessage(MinecraftCompat.literal(tr(renaming
                ?"dds-tweak.carpet_rules.plans.save_name"
                :"dds-tweak.carpet_rules.plans.create_confirm")));
        //#else
        //$$ saveNameButton.setMessage(tr(renaming
        //$$         ?"dds-tweak.carpet_rules.plans.save_name"
        //$$         :"dds-tweak.carpet_rules.plans.create_confirm"));
        //#endif
        deleteNameButton.visible=showSidebar()&&naming&&renaming;
        editPlanButton.visible=showSidebar();
        editPlanButton.active=available&&!targetDirty&&!importing&&(!naming||renaming);
        ruleSearchBox.visible=showMiddle()&&selected!=null&&!importing&&!naming;
        pickOwnerButton.visible=pickCategoryButton.visible=showMiddle()&&selected!=null&&!importing&&!naming;
        importPathBox.visible=confirmImportButton.visible=cancelImportButton.visible=showMiddle()&&importing;
        browseImportButton.visible=showMiddle()&&importing;
        browseImportButton.active=!nativePickerPending;
        String browseLabel=tr(browsingImport
                ?"dds-tweak.carpet_rules.plans.browse_close"
                :"dds-tweak.carpet_rules.plans.import_browse");
        //#if MC >= 11600
        browseImportButton.setMessage(MinecraftCompat.literal(browseLabel));
        //#else
        //$$ browseImportButton.setMessage(browseLabel);
        //#endif
        openExportButton.visible=showMiddle()&&exportedFile!=null&&!importing&&!naming&&!pickingRule;
        openExportButton.active=!batchActive();
        pickRuleButton.visible=showMiddle()&&selected!=null&&!batchActive()&&!importing&&!naming;
        pickRuleButton.active=hasPlan&&CarpetRuleClientNetwork.catalog().isReady();
        targetBox.visible=detail&&!booleanValue;
        valueChoiceButton.visible=detail&&!booleanValue&&!targetChoices().isEmpty();
        valueChoiceButton.active=!batchActive();
        int right=valueChoiceButton.visible?detailChoiceX():detailResetX();
        targetBox.setWidth(Math.max(30,right-(detailLeft()+12)-6));
        saveTargetButton.visible=detail;
        removeTargetButton.visible=false; // Removal now requires Shift on the rule-row icon.
        saveTargetButton.active=available;
        removeTargetButton.active=available;
        boolean switchBar=showDetail()&&pendingSwitch&&!naming;
        saveSwitchButton.visible=discardSwitchButton.visible=cancelSwitchButton.visible=switchBar;
        backDetailsButton.visible=!wide()&&showDetail()&&!naming;
        applyBatchButton.visible=showMiddle()&&selected!=null&&!pickingRule&&!batchActive()&&!importing&&!naming;
        applyBatchButton.active=available&&!current.values().isEmpty()&&!targetDirty;
        stopBatchButton.visible=showMiddle()&&batchActive();
        pickAddButton.visible=showMiddle()&&pickingRule&&!importing;
        pickAddButton.active=!pickedRules.isEmpty();
        exportButton.visible=importButton.visible=selected!=null&&!narrowDetail&&!importing&&!naming;
        emptyImportButton.visible=selected==null&&showSidebar()&&!importing&&!naming&&!pickingRule;
        exportButton.active=!batchActive()&&!importing&&hasPlan&&!current.values().isEmpty();
        importButton.active=!batchActive()&&!pickingRule&&!naming;
        CarpetRuleUiStyle.background(g,width,height);
        text(g,tr("dds-tweak.carpet_rules.plans.title"),MARGIN,17,0xFFFFFFFF);
        if(showSidebar())
            CarpetRuleUiStyle.rounded(g,MARGIN,TOP,MARGIN+sidebarWidth(),height-14,
                    9,CarpetRuleUiStyle.PANEL);
        if(showMiddle())
            CarpetRuleUiStyle.rounded(g,middleLeft(),TOP,middleLeft()+middleWidth(),height-14,
                    9,CarpetRuleUiStyle.PANEL);
        if(showDetail())
            CarpetRuleUiStyle.rounded(g,detailLeft(),TOP,detailLeft()+detailWidth(),height-14,
                    9,CarpetRuleUiStyle.PANEL);
    }

    private List<String> targetChoices(){
        return selectedRule==null?java.util.Collections.emptyList():
                CarpetRuleClientNetwork.suggestions(selectedRule);
    }

    private void toggleTargetChoices(){
        if(batchActive()||!showDetail()||booleanTarget()||targetChoices().isEmpty())return;
        valueChoicesOpen=!valueChoicesOpen;
        valueChoiceOffset=0;
        if(valueChoicesOpen)setFocused(null);
    }

    private int targetChoiceRows(){
        int below=height-MARGIN-(detailTargetY()+26);
        int above=detailTargetY()-(TOP+10);
        return CarpetRuleChoiceMenu.rows(targetChoices().size(),
                Math.max(1,Math.max(below,above)/CarpetRuleChoiceMenu.ROW_HEIGHT));
    }

    private int targetChoiceTop(){
        int menuHeight=CarpetRuleChoiceMenu.height(targetChoices().size(),targetChoiceRows());
        int below=detailTargetY()+26;
        return below+menuHeight>height-MARGIN
                ?Math.max(TOP+10,detailTargetY()-menuHeight-4):below;
    }

    private int targetChoiceLeft(){return detailLeft()+12;}
    private int targetChoiceWidth(){return Math.max(60,detailWidth()-24);}

    private boolean handleTargetChoiceClick(double x,double y,int button){
        if(!valueChoicesOpen)return false;
        //#if MC >= 260300
        //$$ boolean left=button==InputConstants.MOUSE_BUTTON_LEFT;
        //#else
        boolean left=button==0;
        //#endif
        if(!left){valueChoicesOpen=false;return true;}
        List<String> choices=targetChoices();
        int rows=targetChoiceRows(),top=targetChoiceTop();
        int menuHeight=CarpetRuleChoiceMenu.height(choices.size(),rows);
        if(x>=targetChoiceLeft()&&x<targetChoiceLeft()+targetChoiceWidth()
                &&y>=top&&y<top+menuHeight){
            int index=CarpetRuleChoiceMenu.hit((int)y,top,valueChoiceOffset,choices.size(),rows);
            if(index>=0&&selectedRule!=null){
                CarpetRuleMultiValue multi=CarpetRuleAuthority.multiValue(selectedRule);
                String selectedValue=multi==null?choices.get(index):
                        multi.toggle(targetBox.getValue(),choices.get(index));
                if(selectedValue==null){
                    feedback=tr("dds-tweak.carpet_rules.unavailable");
                }else{
                    targetBox.setValue(selectedValue);
                    targetBox.setCursorPosition(0);
                    targetBox.setHighlightPos(0);
                    if(multi==null)valueChoicesOpen=false;
                }
            }
        }else valueChoicesOpen=false;
        return true; // Closing a popup must never select an underlying rule.
    }

    private boolean booleanTarget(){
        CarpetRuleCatalog.RemoteRule rule=selectedRule==null?null:
                CarpetRuleClientNetwork.catalog().get(selectedRule);
        return rule!=null&&CarpetRuleValueTypes.booleanEditor(rule.value(),
                CarpetRuleClientNetwork.definitionDefault(selectedRule),
                CarpetRuleClientNetwork.suggestions(selectedRule));
    }

    /** Filtering affects only the visible list, never the plan's application scope. */
    private List<CarpetRuleBatchReview.Entry> visiblePlanRules(){
        List<CarpetRuleBatchReview.Entry> out=new ArrayList<>();
        String query=ruleQuery.trim().toLowerCase(Locale.ROOT);
        for(CarpetRuleBatchReview.Entry entry:review()){
            CarpetRuleCatalog.RemoteRule rule=CarpetRuleClientNetwork.catalog().get(entry.key());
            if(!"*".equals(pickerOwner)&&
                    (rule==null||!pickerOwner(rule).equalsIgnoreCase(pickerOwner)))continue;
            if(!"*".equals(pickerCategory)){
                boolean matched=false;
                if(rule!=null)for(String tag:pickerTags(rule))
                    if(tag.equalsIgnoreCase(pickerCategory)){matched=true;break;}
                if(!matched)continue;
            }
            if(!query.isEmpty()){
                String name=rule==null?entry.key().name():pickerName(rule);
                String searchable=name+" "+entry.key().toString();
                if(!searchable.toLowerCase(Locale.ROOT).contains(query))continue;
            }
            out.add(entry);
        }
        return out;
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
    ){
        CarpetRulePresetStore.Preset plan=store.get(selected);
        tooltipRule=null;
        hoveredRemoveRule=false;
        if(showSidebar()){
            int left=MARGIN,w=sidebarWidth(),cx=createLeft(),cw=createWidth();
            text(g,tr("dds-tweak.carpet_rules.plans.tab"),left+10,TOP+13,0xFFE8EEF1);
            ScreenRenderCompat.fill(g,left+8,TOP+36,left+w-8,TOP+37,0x556B7883);
            boolean creating=naming&&!renaming;
            boolean createHover=!naming&&mouseX>=cx&&mouseX<cx+cw&&
                    mouseY>=createTop()&&mouseY<createTop()+CarpetRulePresetLayout.CREATE_HEIGHT;
            CarpetRuleUiStyle.rounded(g,cx,createTop(),cx+cw,createTop()+createHeight(),7,
                    creating?CarpetRuleUiStyle.SELECTED:
                            createHover?CarpetRuleUiStyle.HOVER:CarpetRuleUiStyle.ROW);
            if(creating)
                text(g,fit(tr("dds-tweak.carpet_rules.plans.create_title"),cw-14),
                        cx+7,createTop()+8,0xFFF0F5F3);

            List<CarpetRulePresetStore.Preset> plans=entries();
            int active=expandedIndex();
            offset=Math.min(offset,Math.max(0,plans.size()-planRows()));
            if(renaming)revealSelectedPlan(true);
            ScreenRenderCompat.beginScissor(g,left+3,listTop(),left+w-6,listBottom());
            for(int i=offset;i<plans.size();i++){
                CarpetRulePresetStore.Preset item=plans.get(i);
                int y=planY(i,active);
                if(y>=listBottom())break;
                boolean expanded=renaming&&i==active;
                int cardHeight=expanded?CarpetRulePresetLayout.EXPANDED_HEIGHT:CARD_HEIGHT;
                if(y+cardHeight<=listTop())continue;
                boolean selectedRow=item.name().equals(selected);
                boolean hover=mouseX>=left+8&&mouseX<left+w-8&&
                        mouseY>=y&&mouseY<y+cardHeight;
                CarpetRuleUiStyle.tabBackground(g,left+8,y,left+w-8,cardHeight,7,
                        selectedRow,hover,CarpetRuleUiStyle.ROW);
                if(expanded){
                    text(g,fit(tr("dds-tweak.carpet_rules.plans.edit_title"),w-52),
                            left+16,y+8,0xFFF3FFF7);
                }else{
                    text(g,fit(item.name(),w-30),left+16,y+6,0xFFF4F5F8);
                    text(g,tr("dds-tweak.carpet_rules.plans.count",item.values().size()),
                            left+16,y+22,0xFFA8B6B4);
                }
            }
            ScreenRenderCompat.endScissor(g);
            if(plans.isEmpty()&&!creating)
                text(g,fit(tr("dds-tweak.carpet_rules.plans.empty_all"),w-20),
                        left+12,listTop()+9,0xFFC4CBCF);
            if(naming){
                int formY=renaming?planY(active,active):createTop();
                if(!feedback.isEmpty()&&planFeedback)
                    text(g,fit(feedback,cw-14),cx+7,formY+77,0xFFFFDFA3);
            }
        }
        if(showMiddle()&&(selected!=null||importing||naming||pickingRule)){
            int left=middleLeft(),w=middleWidth(),head=TOP+13;
            int total=plan==null?0:plan.values().size();
            if(!naming&&!importing){
                String summary=pickingRule
                        ?tr("dds-tweak.carpet_rules.plans.picked",pickedRules.size())
                        :tr("dds-tweak.carpet_rules.plans.count",total);
                text(g,fit(summary,Math.max(20,Math.min(w-152,w-284))),
                        left+116,head+1,0xFFA7BBB5);
            }
            // The toolbar divider follows the sidebar header; search/filter live below it.
            ScreenRenderCompat.fill(g,left+8,TOP+36,left+w-8,TOP+37,0x556B7883);
            if(importing){
                int importTitleW=Math.max(20,w-2*Math.min(86,Math.max(44,(w-32)/2))-32);
                text(g,fit(tr("dds-tweak.carpet_rules.plans.import_title"),importTitleW),
                        left+12,TOP+14,0xFFE8EFF0);
                text(g,fit(tr("dds-tweak.carpet_rules.plans.import_help"),w-24),
                        left+12,TOP+(w>=245?80:108),0xFFB5C7C4);
                if(browsingImport)renderImportBrowser(g);
            }else if(naming){
                // The name form owns the screen; no rules/actions appear behind it.
            }else if(pickingRule){
                List<CarpetRuleCatalog.RemoteRule> rules=matchingRules();
                pickerScroll.animate(rules.size(),rulesBottom()-rulesTop());
                renderRuleRows(g,rules);
            }else if(plan!=null){
                List<CarpetRuleBatchReview.Entry> rules=visiblePlanRules();
                ruleScroll.animate(rules.size(),rulesBottom()-rulesTop());
                renderPlanRows(g,rules);
                if(plan.values().isEmpty()){
                    String message=tr("dds-tweak.carpet_rules.plans.empty");
                    int cx=left+w/2;
                    text(g,fit(message,w-24),cx-font.width(fit(message,w-24))/2,
                            rulesTop()+Math.max(30,(rulesBottom()-rulesTop())/3),0xFFE5E9E9);
                    String hint=fit(tr("dds-tweak.carpet_rules.presets.empty_hint"),w-32);
                    text(g,hint,cx-font.width(hint)/2,
                            rulesTop()+Math.max(52,(rulesBottom()-rulesTop())/3+20),0xFFB8C3C6);
                }
                else if(rules.isEmpty())
                    text(g,fit(tr("dds-tweak.carpet_rules.no_results"),w-24),
                            left+12,rulesTop()+12,0xFFB8C1C4);
            }
            ScreenRenderCompat.fill(g,left+10,rulesBottom()+20,left+w-10,rulesBottom()+21,
                    0x556B7883);
        }
        if(showDetail()){
            int left=detailLeft(),w=detailWidth();
            CarpetRuleCatalog.RemoteRule live=selectedRule==null?null:
                    CarpetRuleClientNetwork.catalog().get(selectedRule);
            if(naming||importing||plan==null||selectedRule==null){
                // Import and no-selection states leave the detail panel empty.
            }else{
                String name=live==null?selectedRule.name():pickerName(live);
                detailHeading(g,name,left+12,TOP+8,w-24,DETAIL_TITLE_SCALE,0xFFF8FCFF);
                if(!name.equals(selectedRule.name()))
                    text(g,fit(selectedRule.name(),w-24),left+12,TOP+25,0xFFBBC7CC);
                // Align title, rule identifier, divider and explanation with the main screen.
                detailDivider(g,left,w,TOP+36);
                if(height>=410){
                    CarpetRuleUiStyle.sectionHeading(g,font,
                            tr("dds-tweak.carpet_rules.description_label"),
                            left+12,descriptionTop()-15,w-24,DETAIL_LABEL_SCALE);
                    CarpetRuleMetadata.Description info=metadata.find(selectedRule);
                    List<String> lines=wrapped(info==null?"":info.detail(),w-30);
                    int rows=Math.max(1,descriptionHeight()/CarpetRuleDetailLayout.LINE_HEIGHT);
                    descriptionOffset=Math.min(descriptionOffset,Math.max(0,lines.size()-rows));
                    ScreenRenderCompat.beginScissor(g,left+11,descriptionTop(),
                            left+w-9,descriptionTop()+descriptionHeight());
                    for(int i=0;i<rows&&descriptionOffset+i<lines.size();i++)
                        text(g,lines.get(descriptionOffset+i),left+12,
                                descriptionTop()+i*CarpetRuleDetailLayout.LINE_HEIGHT,0xFFBFC3CC);
                    ScreenRenderCompat.endScissor(g);
                    if(lines.size()>rows)
                        text(g,descriptionOffset+rows<lines.size()?"▾":"▴",
                                left+w-20,descriptionTop()+descriptionHeight()-11,0xFF9EADA8);
                }
                String original=CarpetRuleClientNetwork.definitionDefault(selectedRule);
                CarpetRuleUiStyle.valuePair(g,font,left,detailValuesY(),w,
                        tr("dds-tweak.carpet_rules.current"),
                        live==null?"—":live.value(),
                        tr("dds-tweak.carpet_rules.definition_label"),
                        original==null?"—":CarpetRuleUiStyle.displayDefinition(original));
                CarpetRuleUiStyle.sectionHeading(g,font,
                        tr("dds-tweak.carpet_rules.target"),
                        left+12,detailTargetY()-15,w-24,DETAIL_LABEL_SCALE);
                if(booleanTarget()){
                    int x=left+12,y=detailTargetY(),switchW=w-54;
                    boolean selectedValue=Boolean.parseBoolean(targetBox.getValue());
                    CarpetRuleUiStyle.rounded(g,x,y,x+switchW,y+FIELD_H,5,0xBB181C24);
                    for(int i=0;i<2;i++){
                        boolean option=i==0,checked=option==selectedValue;
                        int cx=x+i*switchW/2,cw=i==0?switchW/2-2:switchW-switchW/2;
                        boolean hover=mouseX>=cx&&mouseX<cx+cw&&mouseY>=y&&mouseY<y+FIELD_H;
                        if(checked||hover)CarpetRuleUiStyle.rounded(g,cx,y,cx+cw,y+FIELD_H,5,
                                checked?(option?0xFF2C785C:0xFF865048):0xFF424A59);
                        String value=option?"true":"false";
                        text(g,value,cx+Math.max(3,(cw-font.width(value))/2),
                                y+(FIELD_H-font.lineHeight)/2+1,
                                checked?0xFFFFFFFF:0xFFBFC4CC);
                    }
                }
                int resetX=detailResetX(),resetY=detailTargetY();
                boolean resetHover=mouseX>=resetX&&mouseX<resetX+CarpetRuleUiStyle.TARGET_ACTION_SIZE
                        &&mouseY>=resetY&&mouseY<resetY+FIELD_H;
                CarpetRuleUiStyle.rounded(g,resetX,resetY,
                        resetX+CarpetRuleUiStyle.TARGET_ACTION_SIZE,resetY+FIELD_H,3,
                        original==null?0xFF303339:resetHover?0xFF426B58:0xFF343D44);
                CarpetRuleUiStyle.resetGlyph(g,resetX+1,resetY+(FIELD_H-16)/2,
                        original==null?0xFF697078:0xFFE4EAE9);
                ScreenRenderCompat.fill(g,left+12,detailTargetY()+30,
                        left+w-12,detailTargetY()+31,0x665B676D);
                if(!pendingSwitch&&CarpetRuleDetailLayout.modeOptionsFit(detailPanelHeight())){
                    CarpetRuleUiStyle.sectionHeading(g,font,
                            tr("dds-tweak.carpet_rules.mode.title"),
                            left+12,modeRowY(0)-18,w-24,DETAIL_LABEL_SCALE);
                    int savedState=CarpetRuleClientNetwork.serverPersisted(selectedRule);
                    if(savedState!=1&&savedState!=2
                            &&CarpetRuleClientNetwork.integratedDefaultsKnown())
                        savedState=CarpetRuleClientNetwork.integratedSavedDefaults().contains(selectedRule)?1:2;
                    String savedLabel=tr(savedState==1?"dds-tweak.carpet_rules.default_saved":
                            savedState==2?"dds-tweak.carpet_rules.default_not_saved":
                                    "dds-tweak.carpet_rules.default_status_unknown");
                    String shown=fit(savedLabel,Math.max(20,w-166));
                    text(g,shown,left+w-12-font.width(shown),modeRowY(0)-18,0xFFD5E2DC);
                    CarpetRuleCommands.Mode mode=plan.applyMode();
                    for(int i=0;i<2;i++){
                        CarpetRuleCommands.Mode option=i==0?CarpetRuleCommands.Mode.TEMPORARY:
                                CarpetRuleCommands.Mode.SET_DEFAULT;
                        int y=modeRowY(i);
                        boolean checked=mode==option;
                        boolean hover=mouseX>=left+12&&mouseX<left+w-12&&mouseY>=y&&mouseY<y+20;
                        CarpetRuleUiStyle.optionRow(g,font,left+12,y,w-24,
                                tr("dds-tweak.carpet_rules.mode."+
                                        (i==0?"temporary":"set_default")),checked,hover);
                    }
                    if(height-94>modeRowY(1)+22)
                        text(g,fit(tr("dds-tweak.carpet_rules.mode.help."+
                                (mode==CarpetRuleCommands.Mode.TEMPORARY?"temporary":"set_default")),
                                w-24),left+12,modeRowY(1)+28,0xFF91AB9F);
                    if(height-85>modeRowY(1)+87){
                        CarpetRuleUiStyle.sectionHeading(g,font,
                                tr("dds-tweak.carpet_rules.plans.sidebar"),
                                left+12,modeRowY(1)+65,w-24,DETAIL_LABEL_SCALE);
                        text(g,fit(selected,w-34),left+21,modeRowY(1)+85,0xFFBFC3CC);
                    }
                }
            }
        }
        if(pendingSwitch&&showDetail()){
            String prompt=tr("dds-tweak.carpet_rules.plans.switch_prompt");
            String message=feedback.equals(prompt)
                    ||feedback.equals(tr("dds-tweak.carpet_rules.plans.unsaved_target"))
                    ?prompt:feedback;
            text(g,fit(message,detailWidth()-24),
                    detailLeft()+12,height-94,0xFFFFDFA3);
        }else if(targetDirty&&showDetail()){
            int statusY=CarpetRuleDetailLayout.compact(detailPanelHeight())
                    &&CarpetRuleDetailLayout.modeOptionsFit(detailPanelHeight())
                    ?modeRowY(1)+24:height-65;
            text(g,fit(feedback.isEmpty()?tr("dds-tweak.carpet_rules.plans.unsaved_target"):feedback,
                    detailWidth()-24),detailLeft()+12,statusY,0xFFFFDFA3);
        }else if(!feedback.isEmpty()){
            if(planFeedback&&showSidebar()){
                if(!naming)text(g,fit(feedback,sidebarWidth()-20),MARGIN+10,
                        height-29,0xFFFFDFA3);
            }else{
                int x=showMiddle()?middleLeft()+12:showDetail()?detailLeft()+12:MARGIN+12;
                int max=showMiddle()?middleWidth()-24:
                        showDetail()?detailWidth()-24:sidebarWidth()-24;
                if(showMiddle()){
                    if(openExportButton.visible){
                        x=middleLeft()+108;
                        max=Math.max(40,middleWidth()-120);
                    }
                    List<String> lines=wrapped(feedback,max);
                    for(int i=0;i<Math.min(2,lines.size());i++)
                        text(g,fit(lines.get(i),max),x,height-36+i*13,0xFFFFDFA3);
                }else text(g,fit(feedback,max),x,height-65,0xFFFFDFA3);
            }
        }
    }

    /** Scrollable in-game JSON browser, independent of desktop GUI availability. */
    private void renderImportBrowser(
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
        if(importBrowseDirectory==null)return;
        int x=middleLeft()+12,w=middleWidth()-24,top=importBrowserTop();
        text(g,fit(tr("dds-tweak.carpet_rules.plans.browse_location",
                        importBrowseDirectory.toString()),w),
                x,top-24,0xFFB9CECA);
        int rows=importBrowserRows(),count=importBrowserSize();
        importBrowseOffset=Math.min(importBrowseOffset,Math.max(0,count-rows));
        if(rows<=0)return;
        ScreenRenderCompat.beginScissor(g,x-4,top,x+w+4,top+rows*24);
        for(int i=0;i<rows&&i+importBrowseOffset<count;i++){
            int index=i+importBrowseOffset;
            boolean parent=hasImportParent()&&index==0;
            java.nio.file.Path file=parent?importBrowseDirectory.getParent():
                    importBrowseEntries.get(index-(hasImportParent()?1:0));
            boolean folder=parent||java.nio.file.Files.isDirectory(file);
            String label=parent?"..":file.getFileName().toString();
            String prefix=folder?tr("dds-tweak.carpet_rules.plans.browse_folder")+" ":"";
            int y=top+i*24;
            boolean hover=mouseX>=x-4&&mouseX<x+w+4&&mouseY>=y&&mouseY<y+22;
            CarpetRuleUiStyle.rounded(g,x-4,y,x+w+4,y+22,4,
                    hover?CarpetRuleUiStyle.SELECTED:CarpetRuleUiStyle.ROW);
            text(g,fit(prefix+label,w-12),x+4,y+7,folder?0xFFB4DFCC:0xFFE4ECE9);
        }
        ScreenRenderCompat.endScissor(g);
        if(count==0)text(g,tr("dds-tweak.carpet_rules.plans.browse_empty"),
                x+4,top+7,0xFFB9C7C4);
    }

    private void renderPlanRows(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            List<CarpetRuleBatchReview.Entry> rules){
        int left=middleLeft(),w=middleWidth();
        ScreenRenderCompat.beginScissor(g,left+1,rulesTop(),left+w-6,rulesBottom());
        int first=Math.max(0,(int)(ruleScroll.position()/ROW));
        int last=Math.min(rules.size(),first+ruleRows()+3);
        for(int i=first;i<last;i++){
            CarpetRuleBatchReview.Entry entry=rules.get(i);
            int y=ruleScroll.top(rulesTop(),i);
            if(y+CARD_HEIGHT<=rulesTop()||y>=rulesBottom())continue;
            boolean active=entry.key().equals(selectedRule);
            boolean hover=filterPopup.isEmpty()&&!valueChoicesOpen
                    &&mouseX>=left+6&&mouseX<left+w-18&&mouseY>=y&&mouseY<y+CARD_HEIGHT;
            CarpetRuleUiStyle.ruleCardBackground(g,left+6,y,left+w-18,CARD_HEIGHT,active,hover);
            CarpetRuleCatalog.RemoteRule live=CarpetRuleClientNetwork.catalog().get(entry.key());
            if(hover&&live!=null)tooltipRule=live;
            String name=live==null?entry.key().name():pickerName(live);
            String info=live==null?entry.key().manager():CarpetRuleSidebar.cardMetadata(
                    CarpetRuleClientNetwork.integratedOwner(entry.key()),pickerTags(live));
            CarpetRuleUiStyle.ruleCardInfo(g,font,left+16,y,Math.max(36,w-151),
                    name,entry.key().name(),info);
            int removeX=CarpetRulePresetLayout.removeX(left+w);
            int valueW=Math.min(96,w/3);
            text(g,fit(entry.target(),valueW),removeX-valueW-8,
                    y+14,0xFFBCE5CA);
            boolean removeHover=mouseX>=removeX&&mouseX<removeX+20
                    &&mouseY>=y+8&&mouseY<y+28;
            if(removeHover){
                hoveredRemoveRule=true;
                tooltipRule=null;
            }
            if(removeHover)CarpetRuleUiStyle.rounded(g,removeX,y+8,
                    removeX+20,y+28,4,shiftHeld()?0xFF6F3E39:0xFF40484B);
            CarpetRuleUiStyle.glyph(g,"remove",removeX+4,y+12,
                    removeHover&&shiftHeld()?0xFFFFC4BB:0xFFBAC3C5);
        }
        ScreenRenderCompat.endScissor(g);
        CarpetRuleUiStyle.listScrollbar(g,left+w-10,rulesTop(),
                rulesBottom()-rulesTop(),rules.size(),ruleScroll);
        if(feedback.isEmpty()&&mouseX>=CarpetRulePresetLayout.removeX(left+w)
                &&mouseX<CarpetRulePresetLayout.removeX(left+w)+20
                &&mouseY>=rulesTop()&&mouseY<rulesBottom())
            {
                String hint=fit(tr("dds-tweak.carpet_rules.plans.shift_remove"),w-24);
                text(g,hint,left+12,
                        height-42+(20-font.lineHeight)/2,0xFFBBCBC4);
            }
    }

    private void renderRuleRows(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            List<CarpetRuleCatalog.RemoteRule> rules){
        int left=middleLeft(),w=middleWidth();
        ScreenRenderCompat.beginScissor(g,left+1,rulesTop(),left+w-6,rulesBottom());
        int first=Math.max(0,(int)(pickerScroll.position()/ROW));
        int last=Math.min(rules.size(),first+ruleRows()+3);
        for(int i=first;i<last;i++){
            CarpetRuleCatalog.RemoteRule rule=rules.get(i);
            int y=pickerScroll.top(rulesTop(),i);
            if(y+CARD_HEIGHT<=rulesTop()||y>=rulesBottom())continue;
            boolean checked=pickedRules.contains(rule.key());
            boolean hover=filterPopup.isEmpty()&&!valueChoicesOpen
                    &&mouseX>=left+6&&mouseX<left+w-18&&mouseY>=y&&mouseY<y+CARD_HEIGHT;
            CarpetRuleUiStyle.ruleCardBackground(g,left+6,y,left+w-18,CARD_HEIGHT,checked,hover);
            if(hover)tooltipRule=rule;
            int checkX=left+6+(40-11)/2,checkY=y+(CARD_HEIGHT-11)/2;
            CarpetRuleUiStyle.choiceCheckbox(g,checkX,checkY,checked);
            String title=pickerName(rule),technical=rule.key().name();
            String tags=CarpetRuleSidebar.cardMetadata(
                    CarpetRuleClientNetwork.integratedOwner(rule.key()),pickerTags(rule));
            CarpetRuleUiStyle.ruleCardInfo(g,font,left+46,y,Math.max(38,w-160),
                    title,technical,tags);
            text(g,fit(rule.value(),Math.min(86,w/4)),left+w-Math.min(86,w/4)-12,
                    y+14,0xFFBCE5CA);
        }
        ScreenRenderCompat.endScissor(g);
        CarpetRuleUiStyle.listScrollbar(g,left+w-10,rulesTop(),
                rulesBottom()-rulesTop(),rules.size(),pickerScroll);
        if(rules.isEmpty())
            text(g,fit(tr("dds-tweak.carpet_rules.plans.picker_empty"),w-24),
                    left+12,rulesTop()+12,0xFFBCC7CC);
    }

    private void renderTargetChoicePopup(
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
        if(!valueChoicesOpen||naming)return;
        List<String> choices=targetChoices();
        if(!showDetail()||batchActive()||booleanTarget()||choices.isEmpty()){
            valueChoicesOpen=false;
            return;
        }
        CarpetRuleMultiValue multi=CarpetRuleAuthority.multiValue(selectedRule);
        int rows=targetChoiceRows(),top=targetChoiceTop();
        int left=targetChoiceLeft(),w=targetChoiceWidth();
        int h=CarpetRuleChoiceMenu.height(choices.size(),rows);
        valueChoiceOffset=CarpetRuleChoiceMenu.clampOffset(valueChoiceOffset,choices.size(),rows);
        CarpetRuleUiStyle.choicePopupFrame(g,left,top,w,h);
        String current=targetBox.getValue();
        for(int i=0;i<rows;i++){
            int index=i+valueChoiceOffset;
            if(index>=choices.size())break;
            String value=choices.get(index);
            int y=top+CarpetRuleChoiceMenu.PADDING+i*CarpetRuleChoiceMenu.ROW_HEIGHT;
            boolean hover=mouseX>=left&&mouseX<left+w&&mouseY>=y
                    &&mouseY<y+CarpetRuleChoiceMenu.ROW_HEIGHT;
            boolean checked=multi!=null?multi.checked(current,value):value.equals(current);
            CarpetRuleUiStyle.choicePopupOption(g,left,y,w,
                    CarpetRuleChoiceMenu.ROW_HEIGHT,checked,hover);
            if(multi!=null)CarpetRuleUiStyle.choiceCheckbox(g,left+8,y+4,checked);
            text(g,fit(value,multi==null?w-14:w-38),
                    left+(multi==null?7:26),y+5,
                    checked?0xFFFFFFFF:0xFFE4F3ED);
        }
        if(choices.size()>rows){
            int barHeight=Math.max(9,(rows*h)/choices.size());
            int range=Math.max(1,h-barHeight-4);
            int max=CarpetRuleChoiceMenu.maxOffset(choices.size(),rows);
            int y=top+2+(max==0?0:valueChoiceOffset*range/max);
            ScreenRenderCompat.fill(g,left+w-4,y,left+w-2,y+barHeight,0xFF8DA79E);
        }
    }

    /** Shared self-painted controls, layered after native widgets for consistent focus. */
    private void renderPaintedControls(
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
        int side=sidebarWidth(),middle=middleLeft(),middleW=middleWidth();
        if(editPlanButton.visible){
            int x=MARGIN+side-30,y=TOP+7;
            boolean hover=(mouseX>=x&&mouseX<x+20&&mouseY>=y&&mouseY<y+20)
                    ||editPlanButton.isFocused();
            CarpetRuleUiStyle.rounded(g,x,y,x+20,y+20,5,
                    !editPlanButton.active?0xFF30363C:
                            renaming?0xFF2E7358:hover?0xFF426557:0xFF343D43);
            int tint=editPlanButton.active?0xFFE6F1ED:0xFF8B969B;
            GuiIconRenderer.draw(g,GuiIconRenderer.PLAN_EDIT,x+2,y+2,16,16,tint);
        }
        if(createButton.visible){
            int x=createLeft(),y=createTop(),w=createWidth();
            CarpetRuleUiStyle.glyph(g,"plus",x+(w-12)/2,
                    y+(CarpetRulePresetLayout.CREATE_HEIGHT-12)/2,
                    createButton.active?0xFFE6F1ED:0xFF8B969B);
        }
        if(naming&&showSidebar()){
            int y=renaming?planY(expandedIndex(),expandedIndex()):createTop();
            int x=deleteButtonX();
            if(deleteNameButton.visible){
                boolean shift=shiftHeld(),hover=mouseX>=x&&mouseX<x+20&&
                        mouseY>=y+4&&mouseY<y+24;
                CarpetRuleUiStyle.rounded(g,x,y+4,x+20,y+24,4,
                        shift?(hover?0xFF944C47:0xFF753D3A):hover?0xFF434A4D:0xFF343D42);
                if(!GuiIconRenderer.draw(g,GuiIconRenderer.PLAN_DELETE,x+3,y+7,14,14,
                        shift?0xFFFFE8E6:0xFFA6B0B2))
                    CarpetRuleUiStyle.glyph(g,"trash",x+4,y+8,
                            shift?0xFFFFE8E6:0xFFA6B0B2);
            }
        }
        if(pickRuleButton.visible){
            int x=CarpetRulePresetLayout.addActionX(middle+middleW),y=TOP+7;
            CarpetRuleUiStyle.actionIcon(g,"plus",x,y,20,
                    (mouseX>=x&&mouseX<x+20&&mouseY>=y&&mouseY<y+20)
                            ||pickRuleButton.isFocused(),
                    pickRuleButton.active,pickingRule);
        }
        if(ruleSearchBox.visible){
            int x=middle+8,y=TOP+48;
            CarpetRuleUiStyle.searchIcon(g,x,y,18,
                    mouseX>=x&&mouseX<x+18&&mouseY>=y&&mouseY<y+18,false);
        }
        if(pickOwnerButton.visible){
            int x=middle+middleW-190,y=TOP+47;
            CarpetRuleUiStyle.filterButton(g,font,
                    "*".equals(pickerOwner)?tr("dds-tweak.carpet_rules.plans.source"):
                            pickerOwnerLabel(pickerOwner),
                    x,y,90,20,(mouseX>=x&&mouseX<x+90&&mouseY>=y&&mouseY<y+20)
                            ||pickOwnerButton.isFocused());
        }
        if(pickCategoryButton.visible){
            int x=middle+middleW-96,y=TOP+47;
            CarpetRuleUiStyle.filterButton(g,font,
                    "*".equals(pickerCategory)?tr("dds-tweak.carpet_rules.plans.category"):
                            pickerLabel(pickerCategory),
                    x,y,88,20,(mouseX>=x&&mouseX<x+88&&mouseY>=y&&mouseY<y+20)
                            ||pickCategoryButton.isFocused());
        }
        if(valueChoiceButton.visible){
            int x=detailChoiceX(),y=detailTargetY();
            CarpetRuleUiStyle.dropdown(g,font,x,y,CarpetRuleUiStyle.TARGET_ACTION_SIZE,FIELD_H,
                    (mouseX>=x&&mouseX<x+CarpetRuleUiStyle.TARGET_ACTION_SIZE
                            &&mouseY>=y&&mouseY<y+FIELD_H)
                            ||valueChoiceButton.isFocused(),valueChoiceButton.active);
        }
    }

    private void renderListTooltip(
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
        if(importing||!filterPopup.isEmpty()||valueChoicesOpen)return;
        List<String> lines;
        if(naming){
            // Only the delete icon may display a tooltip during inline editing.
            if(!renaming||!deleteNameButton.visible)return;
            int index=expandedIndex();
            if(index<0)return;
            int x=deleteButtonX(),y=planY(index,index)+4;
            if(mouseX<x||mouseX>=x+20||mouseY<y||mouseY>=y+20
                    ||mouseY<listTop()||mouseY>=listBottom())return;
            lines=java.util.Collections.singletonList(
                    tr("dds-tweak.carpet_rules.plans.shift_delete_hint"));
        }else{
            boolean resetHover=selectedRule!=null&&showDetail()
                    &&CarpetRuleClientNetwork.definitionDefault(selectedRule)!=null
                    &&mouseX>=detailResetX()
                    &&mouseX<detailResetX()+CarpetRuleUiStyle.TARGET_ACTION_SIZE
                    &&mouseY>=detailTargetY()&&mouseY<detailTargetY()+FIELD_H;
            if(hoveredRemoveRule)
                lines=java.util.Collections.singletonList(
                        tr("dds-tweak.carpet_rules.plans.shift_remove"));
            else if(resetHover)
                lines=java.util.Collections.singletonList(
                        tr("dds-tweak.carpet_rules.restore_definition"));
            else{
                if(tooltipRule==null)return;
                lines=CarpetRuleUiTooltip.lines(font,tooltipRule,metadata,false,width,height);
            }
        }
        if(lines.isEmpty())return;
        List<Component> tooltip=new ArrayList<>();
        for(String line:lines)tooltip.add(MinecraftCompat.literal(line));
        //#if MC >= 12106
        g.setComponentTooltipForNextFrame(font,tooltip,mouseX,mouseY);
        //#elseif MC >= 12000
        //$$ g.renderComponentTooltip(font,tooltip,mouseX,mouseY);
        //#elseif MC >= 11600
        //$$ renderComponentTooltip(g,tooltip,mouseX,mouseY);
        //#else
        //$$ renderTooltip(lines,mouseX,mouseY);
        //#endif
    }

    private void renderFilterPopup(
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
        if(filterPopup.isEmpty()||naming)return;
        List<String> choices=filterValues();
        int rows=Math.min(choices.size(),filterRows());
        filterOffset=Math.max(0,Math.min(filterOffset,choices.size()-rows));
        int x=filterX(),y=filterTop(),w=filterWidth(),h=rows*22+4;
        CarpetRuleUiStyle.choicePopupFrame(g,x,y,w,h);
        String picked="owner".equals(filterPopup)?pickerOwner:pickerCategory;
        for(int i=0;i<rows;i++){
            String option=choices.get(filterOffset+i);
            int iy=y+2+i*22;
            boolean hover=mouseX>=x+2&&mouseX<x+w-2&&mouseY>=iy&&mouseY<iy+21;
            boolean selected=option.equals(picked);
            CarpetRuleUiStyle.choicePopupOption(g,x,iy,w,22,selected,hover);
            String title="*".equals(option)?tr("dds-tweak.carpet_rules.plans.filter_all"):
                    "category".equals(filterPopup)?pickerLabel(option):pickerOwnerLabel(option);
            text(g,fit(title,w-14),x+7,iy+6,
                    selected?0xFFFFFFFF:0xFFE4F3ED);
        }
    }

    private List<String> wrapped(String value,int maxWidth){
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

    private void detailHeading(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            String label,int x,int y,int available,float scale,int color){
        ScreenRenderCompat.scaledText(g,font,fit(label,Math.max(1,(int)(available/scale))),
                x,y,color,scale);
    }

    private void detailDivider(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor g,
            //#elseif MC >= 12000
            GuiGraphics g,
            //#elseif MC >= 11600
            //$$ PoseStack g,
            //#else
            //$$ Object g,
            //#endif
            int left,int w,int y){
        ScreenRenderCompat.fill(g,left+12,y,left+w-12,y+1,0x665B676D);
    }

    private String fit(String value,int max){
        if(max<=0)return "";
        if(font.width(value)<=max)return value;
        int index=value.length();
        while(index>0&&font.width(value.substring(0,index)+"…")>max)index--;
        return value.substring(0,index)+"…";
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
        if(valueChoicesOpen&&handleTargetChoiceClick(event.x(),event.y(),event.button()))return true;
        if(!filterPopup.isEmpty()&&handleFilterClick(event.x(),event.y(),event.button()))return true;
        if(super.mouseClicked(event,doubleClick)){
            if(getFocused() instanceof Button)setFocused(null);
            return true;
        }
        return select(event.x(),event.y(),event.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){
    //$$     if(valueChoicesOpen&&handleTargetChoiceClick(x,y,button))return true;
    //$$     if(!filterPopup.isEmpty()&&handleFilterClick(x,y,button))return true;
    //$$     if(super.mouseClicked(x,y,button)){
    //$$         if(getFocused() instanceof Button)setFocused(null);
    //$$         return true;
    //$$     }
    //$$     return select(x,y,button);
    //$$ }
    //#endif

    private boolean select(double x,double y,int button){
        if(importing)return clickImportBrowser(x,y);
        if(naming&&x>=middleLeft())return true; // Hidden right-hand content cannot be operated.
        //#if MC >= 260300
        //$$ if(button!=InputConstants.MOUSE_BUTTON_LEFT)return false;
        //#else
        if(button!=0)return false;
        //#endif
        if(showDetail()&&!pendingSwitch&&CarpetRuleDetailLayout.modeOptionsFit(detailPanelHeight())
                &&store.get(selected)!=null&&!batchActive()
                &&x>=detailLeft()+12&&x<detailLeft()+detailWidth()-12
                &&y>=modeRowY(0)&&y<modeRowY(1)+20){
            int index=(int)(y-modeRowY(0))/24;
            if(index>=0&&index<2&&y<modeRowY(index)+20){
                selectApplyMode(index==0?CarpetRuleCommands.Mode.TEMPORARY:
                        CarpetRuleCommands.Mode.SET_DEFAULT);
                return true;
            }
        }
        if(showDetail()&&selectedRule!=null
                &&CarpetRuleClientNetwork.definitionDefault(selectedRule)!=null
                &&x>=detailResetX()
                &&x<detailResetX()+CarpetRuleUiStyle.TARGET_ACTION_SIZE
                &&y>=detailTargetY()&&y<detailTargetY()+FIELD_H){
            targetBox.setValue(CarpetRuleClientNetwork.definitionDefault(selectedRule));
            return true;
        }
        if(showDetail()&&selectedRule!=null&&booleanTarget()&&
                x>=detailLeft()+12&&x<detailLeft()+detailWidth()-42
                &&y>=detailTargetY()&&y<detailTargetY()+FIELD_H){
            int half=(detailWidth()-54)/2;
            targetBox.setValue(x<detailLeft()+12+half?"true":"false");
            return true;
        }
        if(showSidebar()&&x>=createLeft()&&x<createLeft()+createWidth()
                &&y>=createTop()&&y<createTop()+createHeight()){
            // The create card receives clicks through its native hit widget.
            return true;
        }
        if(showSidebar()&&x>=createLeft()&&x<createLeft()+createWidth()&&
                y>=listTop()&&y<listBottom()){
            List<CarpetRulePresetStore.Preset> all=entries();
            int index=CarpetRulePresetLayout.rowAt(y,listTop(),listBottom(),offset,
                    expandedIndex());
            if(index<0||index>=all.size()||batchActive())return false;
            if(renaming&&index==expandedIndex())return true;
            CarpetRulePresetStore.Preset plan=all.get(index);
            if(naming)cancelNaming();
            CarpetRuleCatalog.RuleKey nextRule=requestedRule!=null
                    &&plan.values().containsKey(requestedRule)?requestedRule
                    :plan.values().isEmpty()?null:plan.values().keySet().iterator().next();
            requestNavigation(plan.name(),nextRule,false);
            return true;
        }
        if(!showMiddle()||y<rulesTop()||y>=rulesBottom())return false;
        int scrollX=middleLeft()+middleWidth()-11;
        if(x>=scrollX&&x<middleLeft()+middleWidth()){
            CarpetRuleListScroll scroll=pickingRule?pickerScroll:ruleScroll;
            int count=pickingRule?matchingRules().size():visiblePlanRules().size();
            if(CarpetRuleListScroll.max(count,rulesBottom()-rulesTop())<=0)return false;
            scroll.scrollToFraction((y-rulesTop())/(double)(rulesBottom()-rulesTop()),
                    count,rulesBottom()-rulesTop());
            return true;
        }
        if(x<middleLeft()+6||x>=middleLeft()+middleWidth()-18)return false;
        if(pickingRule){
            List<CarpetRuleCatalog.RemoteRule> matches=matchingRules();
            int index=pickerScroll.hit(y,rulesTop(),matches.size(),rulesBottom()-rulesTop());
            if(index<0||index>=matches.size())return false;
            CarpetRuleCatalog.RuleKey key=matches.get(index).key();
            if(!pickedRules.add(key))pickedRules.remove(key);
            return true;
        }
        List<CarpetRuleBatchReview.Entry> rules=visiblePlanRules();
        int index=ruleScroll.hit(y,rulesTop(),rules.size(),rulesBottom()-rulesTop());
        if(index<0||index>=rules.size())return false;
        if(x>=CarpetRulePresetLayout.removeX(middleLeft()+middleWidth())
                &&x<CarpetRulePresetLayout.removeX(middleLeft()+middleWidth())+20
                &&y>=ruleScroll.top(rulesTop(),index)+8
                &&y<ruleScroll.top(rulesTop(),index)+28){
            removeRuleWithShift(rules.get(index).key());
            return true;
        }
        requestNavigation(selected,rules.get(index).key(),!wide());
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
        if(delta==0)return false;
        if(importing){
            if(browsingImport&&x>=middleLeft()&&x<middleLeft()+middleWidth()
                    &&y>=importBrowserTop()&&y<rulesBottom()){
                int max=Math.max(0,importBrowserSize()-importBrowserRows());
                importBrowseOffset=Math.max(0,Math.min(max,
                        importBrowseOffset+(delta<0?2:-2)));
                return true;
            }
            return false;
        }
        if(showDetail()&&selectedRule!=null&&height>=410
                &&x>=detailLeft()+12&&x<detailLeft()+detailWidth()-12
                &&y>=descriptionTop()&&y<descriptionTop()+descriptionHeight()){
            CarpetRuleMetadata.Description info=metadata.find(selectedRule);
            int count=wrapped(info==null?"":info.detail(),detailWidth()-30).size();
            int rows=Math.max(1,descriptionHeight()/CarpetRuleDetailLayout.LINE_HEIGHT);
            descriptionOffset=Math.max(0,Math.min(Math.max(0,count-rows),
                    descriptionOffset+(delta<0?2:-2)));
            return true;
        }
        if(valueChoicesOpen){
            int top=targetChoiceTop(),rows=targetChoiceRows();
            int h=CarpetRuleChoiceMenu.height(targetChoices().size(),rows);
            if(x>=targetChoiceLeft()&&x<targetChoiceLeft()+targetChoiceWidth()
                    &&y>=top&&y<top+h){
                valueChoiceOffset=CarpetRuleChoiceMenu.clampOffset(
                        valueChoiceOffset+(delta<0?1:-1),targetChoices().size(),rows);
                return true;
            }
            valueChoicesOpen=false;
        }
        if(showMiddle()&&!filterPopup.isEmpty()){
            if(x>=filterX()&&x<filterX()+filterWidth()
                    &&y>=filterTop()&&y<filterTop()+filterRows()*22+4){
                int limit=Math.max(0,filterValues().size()-filterRows());
                filterOffset=Math.max(0,Math.min(limit,filterOffset+(delta<0?2:-2)));
            }
            return true;
        }
        if(y>=listTop()&&y<listBottom()&&showSidebar()
                &&x>=MARGIN&&x<MARGIN+sidebarWidth()){
            if(naming)return true; // Keep the expanded input and actions visible.
            offset=Math.max(0,Math.min(Math.max(0,entries().size()-planRows()),
                    offset+(delta<0?2:-2)));
            return true;
        }
        if(showMiddle()&&x>=middleLeft()&&x<middleLeft()+middleWidth()
                &&y>=rulesTop()&&y<rulesBottom()){
            if(pickingRule)pickerScroll.wheel(delta,matchingRules().size(),rulesBottom()-rulesTop());
            else ruleScroll.wheel(delta,visiblePlanRules().size(),rulesBottom()-rulesTop());
            return true;
        }
        return false;
    }

    //#if MC >= 12109
    @Override public boolean keyPressed(KeyEvent event){
        if(naming&&event.isConfirmation()){createOrSave();return true;}
        if(event.isEscape()&&(importing||valueChoicesOpen||pendingSwitch||targetDirty||pickingRule||naming||narrowDetail)){back();return true;}
        return super.keyPressed(event);
    }
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
    //$$     if(naming&&(keyCode==257||keyCode==335)){createOrSave();return true;}
    //$$     if(keyCode==256&&(importing||valueChoicesOpen||pendingSwitch||targetDirty||pickingRule||naming||narrowDetail)){back();return true;}
    //$$     return super.keyPressed(keyCode,scanCode,modifiers);
    //$$ }
    //#endif
    @Override public void onClose(){
        cancelNativePicker();
        batch.cancel();
        MinecraftCompat.setScreen(minecraft,parent);
    }
    private static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}
}

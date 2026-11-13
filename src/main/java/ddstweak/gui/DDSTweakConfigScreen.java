package ddstweak.gui;

import ddstweak.DDSTweakReference;
import ddstweak.feature.carpetrules.CarpetRuleManagerScreen;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import ddstweak.config.Configs;
import ddstweak.config.OriginalConfigNameScale;
import fi.dy.masa.malilib.config.IConfigSlider;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.gui.SliderCallbackDouble;
import fi.dy.masa.malilib.config.gui.SliderCallbackInteger;
import fi.dy.masa.malilib.config.gui.ConfigOptionListenerResetConfig;
import fi.dy.masa.malilib.gui.interfaces.ISliderCallback;
import fi.dy.masa.malilib.gui.widgets.WidgetSlider;
import ddstweak.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.MaLiLibIcons;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.button.*;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetLabel;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
//#if MC >= 12109
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the categorized configuration screen and its search and grouping behavior.
 * 负责分类配置界面及搜索、分组交互。
 */
public final class DDSTweakConfigScreen extends GuiConfigsBase {
    private static ConfigGuiTab tab=ConfigGuiTab.FEATURES;
    private static final int LABEL_TO_CONTROL_GAP=8;
    private static final int CHILD_LABEL_INDENT=14;
    private static final int MALILIB_LABEL_GAP=10;
    private static final int RESET_AND_EDGE_SPACE=32;
    private static final Map<IConfigBase,IConfigBase> PARENTS=createParents();
    private static final Set<IConfigBase> EXPANDED=Collections.newSetFromMap(new IdentityHashMap<>());
    public DDSTweakConfigScreen(){super(10,50,DDSTweakReference.MOD_ID,null,"dds-tweak.gui.title.configs",DDSTweakReference.MOD_VERSION);}
    @Override public void initGui(){super.initGui();clearOptions();int x=10;for(ConfigGuiTab t:ConfigGuiTab.values())x+=createButton(x,26,t);createCarpetButton(x,26);}
    private void createCarpetButton(int x,int y){ButtonGeneric b=new ButtonGeneric(x,y,-1,20,StringUtils.translate("dds-tweak.gui.button.config_gui.carpet"));addButton(b,(button,mouse)->MinecraftCompat.setScreen(Minecraft.getInstance(),new CarpetRuleManagerScreen(this)));}
    private int createButton(int x,int y,ConfigGuiTab t){ButtonGeneric b=new ButtonGeneric(x,y,-1,20,t.display());b.setEnabled(tab!=t);addButton(b,new ButtonListener(t,this));return b.getWidth()+2;}
    @Override protected int getConfigWidth(){return 260;}
    @Override public List<ConfigOptionWrapper> getConfigs(){return rows(false);}
    /**
     * Expands child rows during search without changing the normal disclosure state.
     * 搜索时展开子项显示，不修改日常折叠状态。
     */
    private List<ConfigOptionWrapper> rows(boolean searching){
        return ConfigOptionWrapper.createFor(ConfigRowGrouping.visible(configsFor(tab),PARENTS,EXPANDED,searching));
    }
    @Override protected WidgetListConfigOptions createListWidget(int x,int y){
        //#if MC >= 12108
        return new GroupedConfigList(x,y,getBrowserWidth(),getBrowserHeight(),getConfigWidth(),useKeybindSearch(),this);
        //#else
        //$$ return new LegacyConfigList(x,y,getBrowserWidth(),getBrowserHeight(),getConfigWidth(),useKeybindSearch(),this);
        //#endif
    }
    /**
     * Toggles only the view state and refreshes the current option rows.
     * 仅切换界面折叠状态，并刷新配置列表。
     */
    private void toggleGroup(IConfigBase parent){
        if(!PARENTS.containsValue(parent))return;
        if(!EXPANDED.add(parent))EXPANDED.remove(parent);
        if(getListWidget()!=null)getListWidget().refreshEntries();
    }
    private static List<IConfigBase> configsFor(ConfigGuiTab tab){
        switch(tab){
            case FEATURES:return featureConfigs();
            case TWEAKS:return tweakConfigs();
            case VISUAL:return visualConfigs();
            case CLIENT:return clientConfigs();
            default:throw new IllegalStateException("Unknown config tab: "+tab);
        }
    }
    private static List<IConfigBase> featureConfigs(){
        List<IConfigBase> c=new ArrayList<>();
        c.add(Configs.Tweaks.ENHANCED_INVENTORY_GUI);
        c.add(Configs.Tweaks.CLICK_THROUGH);
        c.add(Configs.Tweaks.CLICK_THROUGH_MODE);
        c.add(Configs.Tweaks.AUTO_EAT);
        c.add(Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY);
        c.add(Configs.Tweaks.AUTO_EAT_HUNGER_THRESHOLD);
        c.add(Configs.Tweaks.AUTO_EAT_NO_SLOWDOWN);
        c.add(Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST);
        c.add(Configs.Tweaks.SHULKER_RESTOCK_ENABLED);
        c.add(Configs.Tweaks.SHULKER_RESTOCK_MODE);
        c.add(Configs.Tweaks.SHULKER_RESTOCK_WHITELIST);
        c.add(Configs.Tweaks.SHULKER_RESTOCK_BLACKLIST);
        c.add(Configs.Tweaks.SHULKER_RESTOCK_THRESHOLD);
        c.add(Configs.Tweaks.CONFIRM_ITEM_DROP);
        c.add(Configs.Tweaks.DROP_CONFIRM_MODE);
        c.add(Configs.Tweaks.DROP_CONFIRM_BLACKLIST);
        c.add(Configs.Tweaks.DROP_CONFIRM_WHITELIST);
        c.add(Configs.Tweaks.HELPFUL_HITBOXES);
        c.add(Configs.Tweaks.CAMERA_PLAYER_SELECTOR);
        c.add(Hotkeys.CAMERA_PLAYER_SELECTOR);
        c.add(Configs.Tweaks.QUICK_MUTE);
        //#if MC >= 12106
        c.add(Configs.Tweaks.HAPPY_GHAST_RIDING_SPEED_MULTIPLIER);
        //#endif
        c.add(Configs.Visual.RAIL_POWER_PLACEMENT_GUIDE);
        //#if MC >= 12111
        c.add(Configs.Visual.BLOCK_SOUND_TRACKER);
        c.add(Configs.Visual.BLOCK_SOUND_FILTER_MODE);
        c.add(Configs.Visual.BLOCK_SOUND_BLOCK_BLACKLIST);
        c.add(Configs.Visual.BLOCK_SOUND_BLOCK_WHITELIST);
        c.add(Configs.Visual.BLOCK_SOUND_RANGE);
        c.add(Configs.Visual.BLOCK_SOUND_HIGHLIGHT_DURATION);
        c.add(Configs.Visual.BLOCK_SOUND_MAX_HIGHLIGHTS);
        c.add(Configs.Visual.BLOCK_SOUND_REPEATED_ONLY);
        //#endif
        return c;
    }
    private static List<IConfigBase> tweakConfigs(){
        List<IConfigBase> c=new ArrayList<>();
        //#if MC >= 12106
        c.add(Configs.Tweaks.NO_COMMAND_CONFIRM);
        //#endif
        c.add(Configs.Visual.HIDE_BLOCK_OUTLINE);
        c.add(Configs.Tweaks.DISABLE_AUTO_SLEEP_CHAT);
        //#if MC >= 11502
        c.add(Configs.Tweaks.CHAT_TOOLTIP_OCCLUSION);
        //#endif
        c.add(Configs.Tweaks.SCROLL_BOOK_PAGES);
        c.add(Configs.Tweaks.SCROLL_BOOK_CTRL_MULTIPLIER);
        return c;
    }
    private static List<IConfigBase> visualConfigs(){
        List<IConfigBase> c=new ArrayList<>();
        //#if MC >= 12109
        c.add(Configs.Visual.DISABLE_END_FLASH);
        //#endif
        c.add(Configs.Visual.GAMMA_OVERRIDE);
        c.add(Configs.Visual.GAMMA_OVERRIDE_VALUE);
        c.add(Configs.Visual.STACK_COUNT_SCALE_ENABLED);
        c.add(Configs.Visual.STACK_COUNT_SCALE);
        //#if MC >= 12101
        c.add(Configs.Visual.COMPACT_STATUS_EFFECTS);
        c.add(Configs.Visual.COMPACT_STATUS_EFFECTS_ON_LEFT);
        c.add(Configs.Visual.COMPACT_STATUS_EFFECTS_HOLD_TAB);
        //#endif
        c.add(Configs.Visual.CHUNK_CENTER_INDICATOR);
        //#if MC >= 12108
        c.add(Configs.Visual.LOCATOR_HEADS);
        c.add(Configs.Visual.LOCATOR_OUTER_LAYER);
        c.add(Configs.Visual.LOCATOR_HEAD_SIZE);
        c.add(Configs.Visual.LOCATOR_NAME_MODE);
        c.add(Configs.Visual.LOCATOR_BORDER);
        c.add(Configs.Visual.LOCATOR_BORDER_STYLE);
        c.add(Configs.Visual.LOCATOR_BORDER_THICKNESS);
        c.add(Configs.Visual.LOCATOR_BORDER_COLOR);
        c.add(Configs.Visual.LOCATOR_COMPASS);
        c.add(Configs.Visual.LOCATOR_COMPASS_COLOR);
        c.add(Configs.Visual.LOCATOR_COMPASS_SHADOW);
        c.add(Configs.Visual.LOCATOR_ALWAYS_XP);
        c.add(Configs.Visual.LOCATOR_NEARBY_ONLY);
        c.add(Configs.Visual.LOCATOR_PLAYER_FILTER);
        c.add(Configs.Visual.LOCATOR_INCLUDE);
        c.add(Configs.Visual.LOCATOR_EXCLUDE);
        //#endif
        //#if MC >= 12111
        c.add(Configs.Visual.BETTER_SNOW_COVERAGE);
        c.add(Configs.Visual.BETTER_SNOW_COVERAGE_MODE);
        c.add(Configs.Visual.BETTER_SNOW_COVERAGE_BLACKLIST);
        c.add(Configs.Visual.BETTER_SNOW_COVERAGE_WHITELIST);
        c.add(Configs.Visual.TAB_FAKE_PLAYERS_LAST);
        //#endif
        return c;
    }
    private static List<IConfigBase> clientConfigs(){
        List<IConfigBase> c=new ArrayList<>();
        c.add(Hotkeys.OPEN_CONFIG_SCREEN);
        c.add(Hotkeys.OPEN_CARPET_RULE_MANAGER);
        c.add(Configs.Tweaks.SHOW_ORIGINAL_CONFIG_NAME);
        c.add(Configs.Tweaks.ORIGINAL_CONFIG_NAME_SCALE);
        c.add(Configs.Visual.PLAYER_HEAD_WINDOW_ICON);
        c.add(Configs.Tweaks.CLIENT_STARTUP_SOUND);
        c.add(Hotkeys.DDS_UNDO);
        c.add(Hotkeys.DDS_REDO);
        c.add(Configs.Tweaks.AUTO_LOG_CLEANUP);
        c.add(Configs.Tweaks.AUTO_LOG_CLEANUP_MODE);
        c.add(Configs.Tweaks.AUTO_LOG_KEEP_DAYS);
        c.add(Configs.Tweaks.AUTO_LOG_KEEP_LIMIT);
        return c;
    }
    /** Shared DT-only label renderer; no global MaLiLib widget mixin. */
    private static class OriginalNameConfigOption extends WidgetConfigOption {
        // MaLiLib's config-name addLabel() uses white; match TweakerMore's RGB × 0.6.
        private static final int SECONDARY_NAME_COLOR=ConfigRowGrouping.darkerColor(0xFFFFFFFF);
        private WidgetLabel nativeNameLabel;
        private int nativeNameY;
        private int keyX;
        private int keyRowY;
        private int availableNameWidth;
        private String originalKey;

        OriginalNameConfigOption(int x,int y,int width,int height,int labelWidth,int configWidth,
                ConfigOptionWrapper wrapper,int index,DDSTweakConfigScreen screen,WidgetListConfigOptions list){
            super(x,y,width,height,labelWidth,configWidth,wrapper,index,screen,list);
        }

        protected final void captureOriginalName(int x,int y,int availableWidth,IConfigBase config){
            boolean statusRow=config==Configs.Tweaks.SHULKER_RESTOCK_ENABLED;
            if(!statusRow&&!ConfigRowGrouping.showOriginalName(config.getConfigGuiDisplayName(),config.getName()))return;
            WidgetLabel firstLabel=null,plainLabel=null;
            for(WidgetBase widget:subWidgets){
                if(widget instanceof WidgetLabel){
                    if(firstLabel==null)firstLabel=(WidgetLabel)widget;
                    if(widget.getClass()==WidgetLabel.class&&plainLabel==null)plainLabel=(WidgetLabel)widget;
                }
            }
            if(firstLabel==null)return;
            // External mods may append a second label or replace the original one.
            // Keep their widgets intact for the DT-disabled fallback.
            nativeNameLabel=plainLabel!=null?plainLabel:new WidgetLabel(
                    firstLabel.getX(),firstLabel.getY(),firstLabel.getWidth(),firstLabel.getHeight(),
                    0xFFFFFFFF,config.getConfigGuiDisplayName());
            nativeNameY=nativeNameLabel.getY();
            originalKey=config.getName();
            keyX=x;
            keyRowY=y;
            availableNameWidth=availableWidth;
        }

        //#if MC >= 12111
        @Override public void render(fi.dy.masa.malilib.render.GuiContext ctx,int mouseX,int mouseY,boolean selected){
        //#elseif MC >= 12108
        //$$ @Override public void render(net.minecraft.client.gui.GuiGraphics ctx,int mouseX,int mouseY,boolean selected){
        //#elseif MC >= 12000
        //$$ @Override public void render(int mouseX,int mouseY,boolean selected,net.minecraft.client.gui.GuiGraphics ctx){
        //#elseif MC >= 11600
        //$$ @Override public void render(int mouseX,int mouseY,boolean selected,com.mojang.blaze3d.vertex.PoseStack ctx){
        //#else
        //$$ @Override public void render(int mouseX,int mouseY,boolean selected){
        //#endif
            float scale=OriginalConfigNameScale.asMultiplier(Configs.Tweaks.ORIGINAL_CONFIG_NAME_SCALE.getIntegerValue());
            boolean enabled=originalKey!=null&&Configs.Tweaks.SHOW_ORIGINAL_CONFIG_NAME.getBooleanValue();
            String secondary=enabled?ConfigRowGrouping.fitOriginalName(
                    originalKey,availableNameWidth,this::getStringWidth,scale):"";
            // With DT disabled, leave third-party labels entirely in charge.
            // The parent constructor calls addConfigOption() before this subclass is initialized.
            // Inspect MaLiLib's widgets at render time instead of storing a subclass field in captureOriginalName().
            boolean hasExternalLabels=false;
            for(WidgetBase widget:subWidgets){
                if(widget instanceof WidgetLabel&&widget!=nativeNameLabel){
                    hasExternalLabels=true;
                    break;
                }
            }
            boolean externalFallback=!enabled&&hasExternalLabels;
            if(Configs.Tweaks.SHULKER_RESTOCK_ENABLED.getName().equals(originalKey)&&!externalFallback){
                scale=0.65f;
                secondary=restockStatusText();
            }
            boolean show=!secondary.isEmpty();
            int labelShift=Math.max(3,(int)Math.ceil(scale*fontHeight)-3);
            if(nativeNameLabel!=null)nativeNameLabel.setY(nativeNameY-(show?labelShift:0));
            // Replace foreign labels only for this DT row and only while DT's toggle is on.
            // Restore the exact widget list after rendering, including when rendering fails.
            List<WidgetBase> originalWidgets=null;
            if(enabled&&hasExternalLabels){
                originalWidgets=new ArrayList<>(subWidgets);
                subWidgets.removeIf(widget->widget instanceof WidgetLabel&&widget!=nativeNameLabel);
                if(!subWidgets.contains(nativeNameLabel))subWidgets.add(0,nativeNameLabel);
            }
            try{
                //#if MC >= 12108
                super.render(ctx,mouseX,mouseY,selected);
                //#elseif MC >= 11600
                //$$ super.render(mouseX,mouseY,selected,ctx);
                //#else
                //$$ super.render(mouseX,mouseY,selected);
                //#endif
            }finally{
                if(originalWidgets!=null){
                    subWidgets.clear();
                    subWidgets.addAll(originalWidgets);
                }
            }
            if(!show)return;
            int secondaryY=keyRowY+Math.min(14,(int)Math.floor(height-scale*fontHeight-1));
            //#if MC >= 12106
            ctx.pose().pushMatrix();
            ctx.pose().translate((float)keyX,(float)secondaryY);
            ctx.pose().scale(scale,scale);
            drawStringWithShadow(ctx,0,0,SECONDARY_NAME_COLOR,secondary);
            ctx.pose().popMatrix();
            //#elseif MC >= 12000
            //$$ ctx.pose().pushPose();ctx.pose().translate(keyX,secondaryY,0);ctx.pose().scale(scale,scale,1.0f);
            //$$ drawStringWithShadow(0,0,SECONDARY_NAME_COLOR,secondary,ctx);
            //$$ ctx.pose().popPose();
            //#elseif MC >= 11600
            //$$ ctx.pushPose();ctx.translate(keyX,secondaryY,0);ctx.scale(scale,scale,1.0f);
            //$$ drawStringWithShadow(0,0,SECONDARY_NAME_COLOR,secondary,ctx);
            //$$ ctx.popPose();
            //#elseif MC >= 11500
            //$$ com.mojang.blaze3d.systems.RenderSystem.pushMatrix();
            //$$ com.mojang.blaze3d.systems.RenderSystem.translated(keyX,secondaryY,0.0D);
            //$$ com.mojang.blaze3d.systems.RenderSystem.scaled(scale,scale,1.0D);
            //$$ drawStringWithShadow(0,0,SECONDARY_NAME_COLOR,secondary);
            //$$ com.mojang.blaze3d.systems.RenderSystem.popMatrix();
            //#else
            //$$ com.mojang.blaze3d.platform.GlStateManager.pushMatrix();
            //$$ com.mojang.blaze3d.platform.GlStateManager.translatef(keyX,secondaryY,0);
            //$$ com.mojang.blaze3d.platform.GlStateManager.scalef(scale,scale,1);
            //$$ drawStringWithShadow(0,0,SECONDARY_NAME_COLOR,secondary);
            //$$ com.mojang.blaze3d.platform.GlStateManager.popMatrix();
            //#endif
        }

        /** The server reply, not the local toggle, determines actual restock authority. */
        private static String restockStatusText(){
            String prefix="dds-tweak.shulker_restock.status.";
            String mode=ddstweak.feature.shulkerrestock.ShulkerRestockClientNetwork.serverMode();
            boolean local=Configs.Tweaks.SHULKER_RESTOCK_ENABLED.getBooleanValue();
            boolean effective=ddstweak.feature.shulkerrestock.ShulkerRestockClientNetwork.isEffective();
            String status=StringUtils.translate(prefix+"server")+" "+StringUtils.translate(prefix+"mode."+mode)
                    +" / "+StringUtils.translate(prefix+"local")+" "+StringUtils.translate(prefix+(local?"on":"off"))
                    +" / "+StringUtils.translate(prefix+"effective")+" "+StringUtils.translate(prefix+(effective?"on":"off"));
            if(ddstweak.feature.shulkerrestock.ShulkerRestockClientNetwork.hasInvalidList())
                status+=" / "+StringUtils.translate("dds-tweak.shulker_restock.invalid_list");
            return status;
        }

        /** Keep the native callback and geometry; only legacy atlas background rendering differs. */
        @Override protected void addConfigSliderEntry(int x,int y,int resetX,int configWidth,int configHeight,
                IConfigSlider config){
            //#if MC <= 12001
            //$$ if(!(config instanceof IConfigDouble)&&!(config instanceof IConfigInteger))return;
            //$$ ButtonGeneric reset=createResetButton(resetX,y,config);
            //$$ ISliderCallback callback=config instanceof IConfigDouble
            //$$         ?new SliderCallbackDouble((IConfigDouble)config,reset)
            //$$         :new SliderCallbackInteger((IConfigInteger)config,reset);
            //$$ if(config==Configs.Tweaks.ORIGINAL_CONFIG_NAME_SCALE)
            //$$     callback=percentCallback((SliderCallbackInteger)callback);
            //$$ addWidget(new LegacyAtlasSlider(x,y,configWidth,configHeight,callback));
            //$$ addButton(reset,new ConfigOptionListenerResetConfig(config,null,reset,null));
            //#else
            if(config!=Configs.Tweaks.ORIGINAL_CONFIG_NAME_SCALE){
                super.addConfigSliderEntry(x,y,resetX,configWidth,configHeight,config);
                return;
            }
            ButtonGeneric reset=createResetButton(resetX,y,config);
            SliderCallbackInteger delegate=new SliderCallbackInteger(Configs.Tweaks.ORIGINAL_CONFIG_NAME_SCALE,reset);
            ISliderCallback callback=percentCallback(delegate);
            addWidget(new WidgetSlider(x,y,configWidth,configHeight,callback));
            addButton(reset,new ConfigOptionListenerResetConfig(config,null,reset,null));
            //#endif
        }

        private static ISliderCallback percentCallback(SliderCallbackInteger delegate){
            return new ISliderCallback(){
                @Override public int getMaxSteps(){return delegate.getMaxSteps();}
                @Override public double getValueRelative(){return delegate.getValueRelative();}
                @Override public void setValueRelative(double value){delegate.setValueRelative(value);}
                @Override public String getFormattedDisplayValue(){return delegate.getFormattedDisplayValue()+"%";}
            };
        }

        //#if MC <= 12001
        //$$ /** Retains MaLiLib's mouse capture and slider coordinates on old widgets.png. */
        //$$ private static final class LegacyAtlasSlider extends WidgetSlider {
        //$$     LegacyAtlasSlider(int x,int y,int width,int height,ISliderCallback callback){
        //$$         super(x,y,width,height,callback);
        //$$     }
        //#if MC >= 12000
        //$$     @Override public void render(int mouseX,int mouseY,boolean selected,net.minecraft.client.gui.GuiGraphics ctx){
        //#elseif MC >= 11600
        //$$     @Override public void render(int mouseX,int mouseY,boolean selected,com.mojang.blaze3d.vertex.PoseStack ctx){
        //#else
        //$$     @Override public void render(int mouseX,int mouseY,boolean selected){
        //#endif
        //$$         if(dragging&&mouseX!=lastMouseX){
        //$$             callback.setValueRelative(getRelativePosition(mouseX));
        //$$             lastMouseX=mouseX;
        //$$         }
        //$$         bindTexture(VANILLA_WIDGETS);
        //$$         fi.dy.masa.malilib.render.RenderUtils.color(1f,1f,1f,1f);
        //$$         LegacySliderTextureSegments.forEachBackground(this.width,
        //$$                 (offset,u,partWidth)->fi.dy.masa.malilib.render.RenderUtils.drawTexturedRect(
        //$$                         this.x+offset,this.y,u,46,partWidth,20));
        //$$         fi.dy.masa.malilib.render.RenderUtils.drawTexturedRect(this.x+this.width-5,this.y,196,46,4,20);
        //$$         int sw=sliderWidth,usableWidth=this.width-4-sw,half=sw/2;
        //$$         int sliderX=this.x+2+(int)(callback.getValueRelative()*usableWidth);
        //$$         fi.dy.masa.malilib.render.RenderUtils.drawTexturedRect(sliderX,this.y,0,66,half,20);
        //$$         fi.dy.masa.malilib.render.RenderUtils.drawTexturedRect(sliderX+half,this.y,200-half,66,half,20);
        //$$         String value=callback.getFormattedDisplayValue();
        //$$         int textX=this.x+this.width/2-getStringWidth(value)/2;
        //#if MC >= 11600
        //$$         drawString(textX,this.y+6,0xFFFFFFA0,value,ctx);
        //#else
        //$$         drawString(textX,this.y+6,0xFFFFFFA0,value);
        //#endif
        //$$     }
        //$$ }
        //#endif

    }

    /** Only settings with real suboptions get a disclosure arrow; storage remains unchanged. */
    private static Map<IConfigBase,IConfigBase> createParents(){
        Map<IConfigBase,IConfigBase> parents=new IdentityHashMap<>();
        group(parents,Configs.Tweaks.CLICK_THROUGH,Configs.Tweaks.CLICK_THROUGH_MODE);
        group(parents,Configs.Tweaks.AUTO_EAT,
                Configs.Tweaks.AUTO_EAT_OFFHAND_POLICY,Configs.Tweaks.AUTO_EAT_HUNGER_THRESHOLD,Configs.Tweaks.AUTO_EAT_NO_SLOWDOWN,
                Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST);
        group(parents,Configs.Tweaks.CONFIRM_ITEM_DROP,
                Configs.Tweaks.DROP_CONFIRM_MODE,Configs.Tweaks.DROP_CONFIRM_BLACKLIST,
                Configs.Tweaks.DROP_CONFIRM_WHITELIST);
        group(parents,Configs.Tweaks.CAMERA_PLAYER_SELECTOR,Hotkeys.CAMERA_PLAYER_SELECTOR);
        group(parents,Configs.Tweaks.SCROLL_BOOK_PAGES,Configs.Tweaks.SCROLL_BOOK_CTRL_MULTIPLIER);
        group(parents,Configs.Visual.GAMMA_OVERRIDE,Configs.Visual.GAMMA_OVERRIDE_VALUE);
        group(parents,Configs.Visual.STACK_COUNT_SCALE_ENABLED,Configs.Visual.STACK_COUNT_SCALE);
        //#if MC >= 12111
        group(parents,Configs.Visual.BLOCK_SOUND_TRACKER,Configs.Visual.BLOCK_SOUND_FILTER_MODE,
                Configs.Visual.BLOCK_SOUND_BLOCK_BLACKLIST,Configs.Visual.BLOCK_SOUND_BLOCK_WHITELIST,
                Configs.Visual.BLOCK_SOUND_RANGE,Configs.Visual.BLOCK_SOUND_HIGHLIGHT_DURATION,
                Configs.Visual.BLOCK_SOUND_MAX_HIGHLIGHTS,Configs.Visual.BLOCK_SOUND_REPEATED_ONLY);
        //#endif
        //#if MC >= 12101
        group(parents,Configs.Visual.COMPACT_STATUS_EFFECTS,
                Configs.Visual.COMPACT_STATUS_EFFECTS_ON_LEFT,Configs.Visual.COMPACT_STATUS_EFFECTS_HOLD_TAB);
        //#endif
        //#if MC >= 12108
        group(parents,Configs.Visual.LOCATOR_HEADS,
                Configs.Visual.LOCATOR_OUTER_LAYER,Configs.Visual.LOCATOR_HEAD_SIZE,Configs.Visual.LOCATOR_NAME_MODE,
                Configs.Visual.LOCATOR_BORDER,Configs.Visual.LOCATOR_BORDER_STYLE,
                Configs.Visual.LOCATOR_BORDER_THICKNESS,Configs.Visual.LOCATOR_BORDER_COLOR,
                Configs.Visual.LOCATOR_COMPASS,Configs.Visual.LOCATOR_COMPASS_COLOR,
                Configs.Visual.LOCATOR_COMPASS_SHADOW,Configs.Visual.LOCATOR_ALWAYS_XP,
                Configs.Visual.LOCATOR_NEARBY_ONLY,Configs.Visual.LOCATOR_PLAYER_FILTER,
                Configs.Visual.LOCATOR_INCLUDE,Configs.Visual.LOCATOR_EXCLUDE);
        //#endif
        //#if MC >= 12111
        group(parents,Configs.Visual.BETTER_SNOW_COVERAGE,
                Configs.Visual.BETTER_SNOW_COVERAGE_MODE,Configs.Visual.BETTER_SNOW_COVERAGE_BLACKLIST,
                Configs.Visual.BETTER_SNOW_COVERAGE_WHITELIST);
        //#endif
        group(parents,Configs.Tweaks.AUTO_LOG_CLEANUP,
                Configs.Tweaks.AUTO_LOG_CLEANUP_MODE,Configs.Tweaks.AUTO_LOG_KEEP_DAYS,
                Configs.Tweaks.AUTO_LOG_KEEP_LIMIT);
        group(parents,Configs.Tweaks.SHULKER_RESTOCK_ENABLED,Configs.Tweaks.SHULKER_RESTOCK_MODE,
                Configs.Tweaks.SHULKER_RESTOCK_WHITELIST,Configs.Tweaks.SHULKER_RESTOCK_BLACKLIST,
                Configs.Tweaks.SHULKER_RESTOCK_THRESHOLD);
        group(parents,Configs.Tweaks.SHOW_ORIGINAL_CONFIG_NAME,Configs.Tweaks.ORIGINAL_CONFIG_NAME_SCALE);
        return parents;
    }
    private static void group(Map<IConfigBase,IConfigBase> parents,IConfigBase parent,IConfigBase... children){
        for(IConfigBase child:children)parents.put(child,parent);
    }

    //#if MC >= 12108
    /** Reuses native MaLiLib widgets while applying the same disclosure to every tab. */
    private static final class GroupedConfigList extends WidgetListConfigOptions {
        private final DDSTweakConfigScreen screen;
        GroupedConfigList(int x,int y,int width,int height,int configWidth,boolean useKeys,DDSTweakConfigScreen screen){
            super(x,y,width,height,configWidth,0f,useKeys,screen);
            this.screen=screen;
        }
        @Override protected Collection<ConfigOptionWrapper> getAllEntries(){
            return screen.rows(!getFilterText().isEmpty());
        }
        private int labelWidth(IConfigBase config){
            if(!ConfigRowGrouping.showOriginalName(config.getConfigGuiDisplayName(),config.getName())){
                return getStringWidth(config.getConfigGuiDisplayName());
            }
            // Reserve the maximum slider scale so live adjustments never overlap controls.
            return ConfigRowGrouping.displayWidth(getStringWidth(config.getConfigGuiDisplayName()),
                    getStringWidth(config.getName()),ConfigRowGrouping.MAX_NAME_SCALE);
        }
        private int visibleLabelWidth(List<IConfigBase> options){
            return ConfigRowGrouping.maxLabelWidth(options,PARENTS,this::labelWidth,CHILD_LABEL_INDENT);
        }
        @Override public int getMaxNameLengthWrapped(List<ConfigOptionWrapper> wrappers){
            List<IConfigBase> visible=new ArrayList<>(wrappers.size());
            for(ConfigOptionWrapper wrapper:wrappers){
                if(wrapper.getType()==ConfigOptionWrapper.Type.CONFIG&&wrapper.getConfig()!=null){
                    visible.add(wrapper.getConfig());
                }
            }
            int labelWidth=visibleLabelWidth(visible);
            // Use the widest currently visible label across tabs for one common control width.
            // Each tab retains its own label offset, so controls still move with the names.
            int sharedLabelWidth=labelWidth;
            for(ConfigGuiTab configTab:ConfigGuiTab.values()){
                List<IConfigBase> tabOptions=ConfigRowGrouping.visible(
                        configsFor(configTab),PARENTS,EXPANDED,false);
                sharedLabelWidth=Math.max(sharedLabelWidth,visibleLabelWidth(tabOptions));
            }
            configWidth=ConfigRowGrouping.controlWidth(browserEntryWidth,sharedLabelWidth,
                    screen.getConfigWidth(),MALILIB_LABEL_GAP+LABEL_TO_CONTROL_GAP,RESET_AND_EDGE_SPACE);
            return labelWidth;
        }
        @Override protected WidgetConfigOption createListEntryWidget(int x,int y,int index,boolean odd,ConfigOptionWrapper entry){
            return new GroupedConfigOption(x,y,browserEntryWidth,browserEntryHeight,maxLabelWidth,configWidth,
                    entry,index,screen,this);
        }
    }
    private static final class GroupedConfigOption extends OriginalNameConfigOption {
        private final DDSTweakConfigScreen screen;

        GroupedConfigOption(int x,int y,int width,int height,int labelWidth,int configWidth,
                ConfigOptionWrapper wrapper,int index,DDSTweakConfigScreen screen,GroupedConfigList list){
            super(x,y,width,height,labelWidth,configWidth,wrapper,index,screen,list);
            this.screen=screen;
        }

        private static int indentFor(IConfigBase config){
            return PARENTS.containsKey(config)?CHILD_LABEL_INDENT:0;
        }
        private static boolean isGroupParent(IConfigBase config){
            return PARENTS.containsValue(config);
        }
        // MaLiLib adds 10px after labelWidth; align disclosure with that control column.
        // The parent control is shifted by 22px, leaving a 2px gap after this 20px arrow.
        // MaLiLib also increments control y by 1px inside addConfigOption().
        private void addDisclosure(int x,int y,int labelWidth,IConfigBase config){
            if(isGroupParent(config)){
                ButtonGeneric disclosure=new ButtonGeneric(x+labelWidth+10+LABEL_TO_CONTROL_GAP,y+1,20,20,"",
                        EXPANDED.contains(config)?MaLiLibIcons.ARROW_UP:MaLiLibIcons.ARROW_DOWN);
                disclosure.setIconAlignment(LeftRight.CENTER);
                addButton(disclosure,(button,mouse)->screen.toggleGroup(config));
            }
        }

        //#if MC >= 12109
        @Override protected void addConfigOption(int x,int y,int labelWidth,int width,IConfigBase config){
            int indent=indentFor(config),disclosure=isGroupParent(config)?22:0;
            super.addConfigOption(x+indent,y,Math.max(20,labelWidth-indent)+disclosure+LABEL_TO_CONTROL_GAP,
                    width-disclosure-LABEL_TO_CONTROL_GAP,config);
            captureOriginalName(x+indent,y,Math.max(20,labelWidth-indent),config);
            addDisclosure(x,y,labelWidth,config);
        }
        //#else
        //$$ @Override protected void addConfigOption(int x,int y,float zLevel,int labelWidth,int width,IConfigBase config){
        //$$     int indent=indentFor(config),disclosure=isGroupParent(config)?22:0;
        //$$     super.addConfigOption(x+indent,y,zLevel,Math.max(20,labelWidth-indent)+disclosure+LABEL_TO_CONTROL_GAP,
        //$$             width-disclosure-LABEL_TO_CONTROL_GAP,config);
        //$$     captureOriginalName(x+indent,y,Math.max(20,labelWidth-indent),config);
        //$$     addDisclosure(x,y,labelWidth,config);
        //$$ }
        //#endif


    }

    //#else
    //$$ /** Legacy MaLiLib keeps its native widgets while exposing the same DT group hierarchy. */
    //$$ private static final class LegacyConfigList extends WidgetListConfigOptions {
    //$$     private final DDSTweakConfigScreen screen;
    //$$     LegacyConfigList(int x,int y,int width,int height,int configWidth,boolean keys,DDSTweakConfigScreen screen){
    //$$         super(x,y,width,height,configWidth,0f,keys,screen);
    //$$         this.screen=screen;
    //$$     }
    //$$     @Override protected Collection<ConfigOptionWrapper> getAllEntries(){
    //$$         return screen.rows(!getFilterText().isEmpty());
    //$$     }
    //$$     @Override protected java.util.Comparator<ConfigOptionWrapper> getComparator(){
    //$$         List<IConfigBase> order=configsFor(tab);
    //$$         return (a,b)->Integer.compare(order.indexOf(a.getConfig()),order.indexOf(b.getConfig()));
    //$$     }
    //$$     @Override public int getMaxNameLengthWrapped(List<ConfigOptionWrapper> wrappers){
    //$$         int width=0;
    //$$         for(ConfigOptionWrapper wrapper:wrappers){
    //$$             IConfigBase c=wrapper.getConfig();
    //$$             if(c==null)continue;
    //$$             int label=getStringWidth(c.getConfigGuiDisplayName());
    //$$             if(ConfigRowGrouping.showOriginalName(c.getConfigGuiDisplayName(),c.getName())){
    //$$                 label=ConfigRowGrouping.displayWidth(label,getStringWidth(c.getName()),1f);
    //$$             }
    //$$             width=Math.max(width,label+(PARENTS.containsKey(c)?CHILD_LABEL_INDENT:0));
    //$$         }
    //$$         configWidth=ConfigRowGrouping.controlWidth(browserEntryWidth,width,
    //$$                 screen.getConfigWidth(),MALILIB_LABEL_GAP+LABEL_TO_CONTROL_GAP,RESET_AND_EDGE_SPACE);
    //$$         return width;
    //$$     }
    //$$     @Override protected WidgetConfigOption createListEntryWidget(int x,int y,int index,boolean odd,ConfigOptionWrapper entry){
    //$$         return new LegacyConfigOption(x,y,browserEntryWidth,browserEntryHeight,maxLabelWidth,
    //$$                 configWidth,entry,index,screen,this);
    //$$     }
    //$$ }
    //$$ private static final class LegacyConfigOption extends OriginalNameConfigOption {
    //$$     private final DDSTweakConfigScreen screen;
    //$$     LegacyConfigOption(int x,int y,int width,int height,int labelWidth,int configWidth,
    //$$             ConfigOptionWrapper wrapper,int index,DDSTweakConfigScreen screen,LegacyConfigList list){
    //$$         super(x,y,width,height,labelWidth,configWidth,wrapper,index,screen,list);
    //$$         this.screen=screen;
    //$$     }
    //$$     @Override protected void addConfigOption(int x,int y,float zLevel,int labelWidth,int width,IConfigBase config){
    //$$         int indent=PARENTS.containsKey(config)?CHILD_LABEL_INDENT:0;
    //$$         int disclosure=PARENTS.containsValue(config)?22:0;
    //$$         super.addConfigOption(x+indent,y,zLevel,Math.max(20,labelWidth-indent)+disclosure+LABEL_TO_CONTROL_GAP,
    //$$                 width-disclosure-LABEL_TO_CONTROL_GAP,config);
    //$$         captureOriginalName(x+indent,y,Math.max(20,labelWidth-indent),config);
    //$$         if(disclosure>0){
    //$$             ButtonGeneric arrow=new ButtonGeneric(x+labelWidth+10+LABEL_TO_CONTROL_GAP,y+1,20,20,"",
    //$$                     EXPANDED.contains(config)?MaLiLibIcons.ARROW_UP:MaLiLibIcons.ARROW_DOWN);
    //$$             arrow.setIconAlignment(LeftRight.CENTER);
    //$$             addButton(arrow,(button,mouse)->screen.toggleGroup(config));
    //$$         }
    //$$     }
    //$$ }
    //#endif
    private static final class ButtonListener implements IButtonActionListener{
        private final ConfigGuiTab tab;private final DDSTweakConfigScreen parent;
        ButtonListener(ConfigGuiTab tab,DDSTweakConfigScreen parent){this.tab=tab;this.parent=parent;}
        @Override public void actionPerformedWithButton(ButtonBase b,int mouse){DDSTweakConfigScreen.tab=tab;parent.reCreateListWidget();if(parent.getListWidget()!=null)parent.getListWidget().resetScrollbarPosition();parent.initGui();}
    }
    private enum ConfigGuiTab{
        FEATURES("dds-tweak.gui.button.config_gui.features"),TWEAKS("dds-tweak.gui.button.config_gui.tweaks"),VISUAL("dds-tweak.gui.button.config_gui.visual"),CLIENT("dds-tweak.gui.button.config_gui.client");
        private final String key;ConfigGuiTab(String key){this.key=key;}String display(){return StringUtils.translate(key);}
    }
}

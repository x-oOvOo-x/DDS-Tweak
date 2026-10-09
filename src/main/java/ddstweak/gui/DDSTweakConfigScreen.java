package ddstweak.gui;

import ddstweak.DDSTweakReference;
import ddstweak.config.Configs;
import ddstweak.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.*;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

public final class DDSTweakConfigScreen extends GuiConfigsBase {
    private static ConfigGuiTab tab=ConfigGuiTab.FEATURES;
    public DDSTweakConfigScreen(){super(10,50,DDSTweakReference.MOD_ID,null,"dds-tweak.gui.title.configs",DDSTweakReference.MOD_VERSION);}
    @Override public void initGui(){super.initGui();clearOptions();int x=10;for(ConfigGuiTab t:ConfigGuiTab.values())x+=createButton(x,26,t);}
    private int createButton(int x,int y,ConfigGuiTab t){ButtonGeneric b=new ButtonGeneric(x,y,-1,20,t.display());b.setEnabled(tab!=t);addButton(b,new ButtonListener(t,this));return b.getWidth()+2;}
    @Override protected int getConfigWidth(){return 260;}
    @Override protected boolean useKeybindSearch(){return tab==ConfigGuiTab.FEATURES;}
    @Override public List<ConfigOptionWrapper> getConfigs(){return ConfigOptionWrapper.createFor(configsFor(tab));}
    private static List<? extends IConfigBase> configsFor(ConfigGuiTab tab){
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
        c.add(Configs.Tweaks.AUTO_EAT_HUNGER_THRESHOLD);
        c.add(Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST);
        c.add(Configs.Tweaks.CONFIRM_ITEM_DROP);
        c.add(Configs.Tweaks.DROP_CONFIRM_MODE);
        c.add(Configs.Tweaks.DROP_CONFIRM_BLACKLIST);
        c.add(Configs.Tweaks.DROP_CONFIRM_WHITELIST);
        c.add(Configs.Tweaks.HELPFUL_HITBOXES);
        c.add(Configs.Tweaks.CAMERA_PLAYER_SELECTOR);
        c.add(Hotkeys.CAMERA_PLAYER_SELECTOR);
        c.add(Configs.Tweaks.QUICK_MUTE);
        c.add(Configs.Visual.RAIL_POWER_PLACEMENT_GUIDE);
        c.add(Hotkeys.OPEN_CONFIG_SCREEN);
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
        //#if MC >= 12111
        c.add(Configs.Visual.BETTER_SNOW_COVERAGE);
        c.add(Configs.Visual.BETTER_SNOW_COVERAGE_MODE);
        c.add(Configs.Visual.BETTER_SNOW_COVERAGE_BLACKLIST);
        c.add(Configs.Visual.BETTER_SNOW_COVERAGE_WHITELIST);
        //#endif
        return c;
    }
    private static List<IConfigBase> clientConfigs(){
        List<IConfigBase> c=new ArrayList<>();
        c.add(Configs.Visual.PLAYER_HEAD_WINDOW_ICON);
        c.add(Configs.Tweaks.CLIENT_STARTUP_SOUND);
        c.add(Configs.Tweaks.AUTO_LOG_CLEANUP);
        c.add(Configs.Tweaks.AUTO_LOG_CLEANUP_MODE);
        c.add(Configs.Tweaks.AUTO_LOG_KEEP_DAYS);
        c.add(Configs.Tweaks.AUTO_LOG_KEEP_LIMIT);
        return c;
    }
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

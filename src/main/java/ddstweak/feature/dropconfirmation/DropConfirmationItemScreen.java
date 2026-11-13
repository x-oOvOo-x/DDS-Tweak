package ddstweak.feature.dropconfirmation;

import ddstweak.config.Configs;
import ddstweak.gui.itempicker.ItemPickerKind;
import ddstweak.gui.itempicker.ItemPickerConfigBindings;
import ddstweak.feature.shulkerrestock.RestockPreferencePolicy;
//#if MC >= 12111
import ddstweak.gui.itempicker.SoundPickerIcons;
import ddstweak.gui.itempicker.SoundSourceGroups;
import ddstweak.feature.blocksoundtracker.BlockSoundPolicy;
import ddstweak.feature.blocksoundtracker.BlockSoundDefaults;
//#endif
import ddstweak.gui.SharedScreenBackground;
import ddstweak.feature.silentautoeat.SilentAutoEatFoodRules;
import ddstweak.platform.minecraft.ItemPickerFocusCompat;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.platform.minecraft.ScreenRenderCompat;
import fi.dy.masa.malilib.config.IConfigStringList;
import net.minecraft.client.Minecraft;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
//#if MC >= 12109
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
//#if MC >= 260300
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//#if MC < 11500
//$$ import com.mojang.blaze3d.platform.GlStateManager;
//$$ import com.mojang.blaze3d.platform.Lighting;
//#endif
//#if MC >= 12111
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Items;
//#endif
import java.util.*;
import java.util.function.Consumer;

/**
 * Provides an interactive item picker for drop-related configuration.
 * 提供用于丢弃相关配置的物品选择界面。
 */
public final class DropConfirmationItemScreen extends Screen {
    private static final int SLOT=22,GAP=2,CATEGORY_SIZE=22,BUTTON_H=20,FIELD_H=18,FOOTER_WIDTH=72,FOOTER_GAP=4;
    private static final int LABEL_Y=24,LABEL_RULE_Y=40,SELECTED_Y=47,SEARCH_Y=45,CATEGORY_Y=67,ITEMS_Y=91;
    private final Screen parent;
    private final IConfigStringList target;
    private final Consumer<String> singleSelection;
    private final Consumer<List<String>> multiSelection;
    private final List<String> stagedIds=new ArrayList<>();
    private final ItemPickerKind kind;
    private final String titleKey;
    private final String singleDefaultId;
    private String singleSelectedId="";
    private final List<Item> allItems=new ArrayList<>();
    private final Map<String,Item> byId=new LinkedHashMap<>();
    private final List<Item> filtered=new ArrayList<>();
    //#if MC >= 12111
    private final List<String> soundIds=new ArrayList<>();
    private final List<String> filteredSoundGroups=new ArrayList<>();
    private final Map<String,List<String>> soundGroups=new LinkedHashMap<>();
    private final Map<String,Item> itemlessBlockIcons=new LinkedHashMap<>();
    private final List<String> filteredItemlessBlocks=new ArrayList<>();
    //#endif
    private final List<CreativeTabCompat.Tab> categories;
    private String query="";
    private EditBox searchField;
    private String hoveredUnknownId;
    private int categoryIndex,categoryOffset;
    private int itemRowOffset;
    private int selectedRowOffset;

    public DropConfirmationItemScreen(Screen parent,IConfigStringList target,ItemPickerKind kind,String titleKey){
        this(parent,Objects.requireNonNull(target,"target"),kind,titleKey,"","",null,null);
    }

    public DropConfirmationItemScreen(Screen parent,String selectedId,String defaultId,Consumer<String> onSelected,String titleKey){
        this(parent,null,ItemPickerKind.ITEM,titleKey,selectedId,defaultId,Objects.requireNonNull(onSelected,"onSelected"),null);
    }

    /** Standalone multi-select: Done commits; Esc cancels. Existing MaLiLib lists stay live-write. */
    public DropConfirmationItemScreen(Screen parent,Collection<String> initialIds,ItemPickerKind kind,
                                      Consumer<List<String>> onSelected,String titleKey){
        this(parent,null,kind,titleKey,"","",null,Objects.requireNonNull(onSelected,"onSelected"));
        if(initialIds!=null)stagedIds.addAll(new LinkedHashSet<>(initialIds));
        refresh();
    }

    private DropConfirmationItemScreen(Screen parent,IConfigStringList target,ItemPickerKind kind,
                                       String titleKey,String selectedId,String defaultId,
                                       Consumer<String> singleSelection,Consumer<List<String>> multiSelection){
        super(MinecraftCompat.translatable(titleKey));
        this.parent=parent;
        this.target=target;
        this.singleSelection=singleSelection;
        this.multiSelection=multiSelection;
        this.kind=Objects.requireNonNull(kind,"kind");
        this.titleKey=Objects.requireNonNull(titleKey,"titleKey");
        this.singleDefaultId=singleSelection==null?"":Objects.requireNonNull(defaultId,"defaultId").trim();
        if(singleSelection!=null&&singleDefaultId.isEmpty())throw new IllegalArgumentException("Single-selection default ID must not be empty");
        this.singleSelectedId=selectedId==null||selectedId.trim().isEmpty()?singleDefaultId:selectedId.trim();
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND){
            SortedSet<String> ids=new TreeSet<>();
            Set<String> redstoneSources=new HashSet<>();
            for(String id:BlockSoundDefaults.REDSTONE_WHITELIST)redstoneSources.add(SoundSourceGroups.key(id));
            for(SoundEvent sound:BuiltInRegistries.SOUND_EVENT){
                String id=BuiltInRegistries.SOUND_EVENT.getKey(sound).toString();
                if(!id.substring(id.indexOf(':')+1).startsWith("block.")||
                        BlockSoundPolicy.isWorldEventSoundDuplicate(id))continue;
                if(redstoneSources.contains(SoundSourceGroups.key(id))&&
                        !BlockSoundDefaults.REDSTONE_WHITELIST.contains(id))continue;
                ids.add(id);
            }
            ids.add("event:1000");
            ids.add("event:1001");
            ids.add("event:1002");
            soundIds.addAll(ids);
        }else
        //#endif
        for(Item item:ItemRegistryCompat.allItems()){
            if(!isCandidate(item))continue;
            allItems.add(item);
            byId.put(entryId(item),item);
        }
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND_BLOCK){
            for(Block block:BuiltInRegistries.BLOCK){
                String id=BuiltInRegistries.BLOCK.getKey(block).toString();
                if(!byId.containsKey(id))
                    itemlessBlockIcons.put(id,id.equals("minecraft:tripwire")?Items.TRIPWIRE_HOOK:Items.BARRIER);
            }
        }
        //#endif
        // The candidate-backed "All" tab must also work from the title screen, without a loaded world.
        this.categories=CreativeTabCompat.tabs(Minecraft.getInstance(),allItems);
        refresh();
    }

    @Override public boolean isPauseScreen(){return false;}

    @Override protected void init(){
        Layout l=layout();
        //#if MC >= 11600
        searchField=new EditBox(font,l.rightLeft,l.searchTop,l.rightWidth,FIELD_H,MinecraftCompat.literal(""));
        //#else
        //$$ searchField=new EditBox(font,l.rightLeft,l.searchTop,l.rightWidth,FIELD_H,"");
        //#endif
        searchField.setMaxLength(200);
        searchField.setValue(query);
        searchField.setResponder(value->{query=value;refreshQuery();});
        addWidgetCompat(searchField);
        int doneX=l.rightLeft+l.rightWidth-FOOTER_WIDTH;
        int clearX=doneX-FOOTER_GAP-FOOTER_WIDTH;
        String resetLabel=singleSelection==null?"dds-tweak.list_picker.clear":"dds-tweak.list_picker.reset";
        addWidgetCompat(button(clearX,l.bottomTop,FOOTER_WIDTH,tr(resetLabel),()->{resetOrClear();refresh();}));
        addWidgetCompat(button(doneX,l.bottomTop,FOOTER_WIDTH,tr("dds-tweak.list_picker.done"),this::confirmSelection));
    }

    private Button button(int x,int y,int w,String label,Runnable action){
        //#if MC >= 11903
        return Button.builder(MinecraftCompat.literal(label),ignored->action.run()).bounds(x,y,w,BUTTON_H).build();
        //#elseif MC >= 11600
        //$$ return new Button(x,y,w,BUTTON_H,MinecraftCompat.literal(label),ignored->action.run());
        //#else
        //$$ return new Button(x,y,w,BUTTON_H,label,ignored->action.run());
        //#endif
    }

    private <T extends AbstractWidget> void addWidgetCompat(T widget){
        //#if MC >= 11700
        addRenderableWidget(widget);
        //#else
        //$$ addButton(widget);
        //#endif
    }

    //#if MC >= 260100
    //$$ @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){super.extractBackground(graphics,mouseX,mouseY,partialTick);renderBase(graphics);}
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){super.extractRenderState(graphics,mouseX,mouseY,partialTick);renderOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 12002
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        //#if MC >= 12005
        super.renderBackground(graphics,mouseX,mouseY,partialTick);
        //#else
        //$$ renderTransparentBackground(graphics);
        //#endif
        //#if MC >= 12005
        // Vanilla draws the blurred background here before the screen's widgets.
        renderBase(graphics);
        //#endif
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        //#if MC < 12005
        // Pre-1.20.5 Screen.render only renders widgets, so draw the background explicitly.
        renderBackground(graphics,mouseX,mouseY,partialTick);
        renderBase(graphics);
        //#endif
        super.render(graphics,mouseX,mouseY,partialTick);
        renderOverlay(graphics,mouseX,mouseY);
    }
    //#elseif MC >= 12000
    //$$ @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);renderBase(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderOverlay(graphics,mouseX,mouseY);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics);renderBase(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderOverlay(graphics,mouseX,mouseY);}
    //#else
    //$$ @Override public void render(int mouseX,int mouseY,float partialTick){renderBackground();renderBase(null);super.render(mouseX,mouseY,partialTick);renderOverlay(null,mouseX,mouseY);}
    //#endif

    private void renderBase(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics
            //#elseif MC >= 12000
            GuiGraphics graphics
            //#elseif MC >= 11600
            //$$ PoseStack graphics
            //#else
            //$$ Object graphics
            //#endif
    ){
        Layout l=layout();
        SharedScreenBackground.draw(graphics,width,height,l.top-8,l.top+SharedScreenBackground.NAV_HEIGHT+8,0x60000000);
        SharedScreenBackground.drawNavigationRules(graphics,width,l.top);
        centered(graphics,tr(titleKey),width/2,l.top,0xFFFFFFFF);
        int selectedCount=selectedIds().size();
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND)selectedCount=selectedSoundGroups().size();
        //#endif
        text(graphics,tr("dds-tweak.list_picker.selected_header",selectedCount),l.selectedLeft,l.top+LABEL_Y,0xFFD8D8D8);
        text(graphics,tr(
                //#if MC >= 12111
                kind==ItemPickerKind.SOUND?"dds-tweak.list_picker.sound_search_title":
                //#endif
                "dds-tweak.list_picker.search_title"),l.rightLeft,l.top+LABEL_Y,0xFFD8D8D8);
        SharedScreenBackground.rule(graphics,l.selectedLeft,l.top+LABEL_RULE_Y,l.leftWidth);
        SharedScreenBackground.rule(graphics,l.rightLeft,l.top+LABEL_RULE_Y,l.rightWidth);
        int divider=l.selectedLeft+l.leftWidth+5;
        ScreenRenderCompat.fill(graphics,divider,l.top+LABEL_Y,divider+1,l.top+SharedScreenBackground.FOOTER_RULE_Y-6,0x257F858C);
    }

    private void renderOverlay(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY){
        Layout l=layout();
        if(query.isEmpty()&&(searchField==null||!searchField.isFocused())){
            String hint=tr("dds-tweak.list_picker.search",
                    tr(
                            //#if MC >= 12111
                            kind==ItemPickerKind.SOUND?"dds-tweak.list_picker.search_hint.sound":
                            //#endif
                            kind==ItemPickerKind.BLOCK
                            //#if MC >= 12111
                            ||kind==ItemPickerKind.SOUND_BLOCK
                            //#endif
                            ?"dds-tweak.list_picker.search_hint.block":"dds-tweak.list_picker.search_hint.item"));
            text(graphics,fit(hint,l.rightWidth-10),l.rightLeft+5,l.searchTop+5,0xFF909090);
        }
        hoveredUnknownId=null;
        Item hovered=renderSelected(graphics,l,mouseX,mouseY);
        String categoryHovered=renderCategories(graphics,l,mouseX,mouseY);
        Item gridHovered=renderItems(graphics,l,mouseX,mouseY);
        if(gridHovered!=null)hovered=gridHovered;
        if(hovered!=null)tooltip(graphics,mouseX,mouseY,hovered);
        else if(hoveredUnknownId!=null){
            //#if MC >= 12111
            if(kind==ItemPickerKind.SOUND){
                List<String> group=soundGroups.get(hoveredUnknownId);
                if(group!=null&&!group.isEmpty())
                    showTooltip(graphics,mouseX,mouseY,SoundPickerIcons.sourceLabel(group.get(0)),
                            tr("dds-tweak.sound.source.group_count",group.size()));
            }
            else
            //#endif
                textTooltip(graphics,mouseX,mouseY,hoveredUnknownId);
        }
        else if(categoryHovered!=null)textTooltip(graphics,mouseX,mouseY,categoryHovered);
    }

    private Item renderSelected(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            Layout l,int mouseX,int mouseY){
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND){
            renderSoundIcons(graphics,l,mouseX,mouseY,true);
            return null;
        }
        //#endif
        List<SelectedEntry> selected=selectedEntries();
        int maxRow=Math.max(0,(selected.size()+l.selectedColumns-1)/l.selectedColumns-l.selectedRows);
        selectedRowOffset=Math.max(0,Math.min(maxRow,selectedRowOffset));
        int start=selectedRowOffset*l.selectedColumns;
        int end=Math.min(selected.size(),start+l.selectedColumns*l.selectedRows);
        Item hovered=null;
        for(int i=start;i<end;i++){
            int local=i-start,col=local%l.selectedColumns,row=local/l.selectedColumns;
            int x=l.selectedLeft+col*(SLOT+GAP),y=l.selectedTop+row*(SLOT+GAP);
            SelectedEntry entry=selected.get(i);
            Item item=entry.item;
            boolean hover=inside(mouseX,mouseY,x,y,SLOT,SLOT);
            ScreenRenderCompat.fill(graphics,x,y,x+SLOT,y+SLOT,hover?0xA0506050:0xA0304030);
            outline(graphics,x,y,SLOT,SLOT,hover?0xFFFFFFFF:0xFF608060);
            ItemStack stack=item==null?ItemStack.EMPTY:ItemRegistryCompat.stack(item);
            //#if MC >= 12111
            if(stack.isEmpty()&&kind==ItemPickerKind.SOUND_BLOCK){
                Item icon=itemlessBlockIcons.get(entry.id);
                if(icon!=null)stack=ItemRegistryCompat.stack(icon);
            }
            //#endif
            if(!stack.isEmpty())renderPickerItem(graphics,stack,x+3,y+3);
            else centered(graphics,entry.id.startsWith("#")?"#":"?",x+SLOT/2,y+7,0xFFCCCCCC);
            if(hover){
                if(item!=null)hovered=item;
                else hoveredUnknownId=entry.id;
            }
        }
        return hovered;
    }

    private String renderCategories(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            Layout l,int mouseX,int mouseY){
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND)return null;
        //#endif
        int active=query.isEmpty()?categoryIndex:0;
        int maxOffset=Math.max(0,categories.size()-l.categoryColumns);
        categoryOffset=Math.max(0,Math.min(maxOffset,categoryOffset));
        String hovered=null;
        for(int i=categoryOffset;i<Math.min(categories.size(),categoryOffset+l.categoryColumns);i++){
            int x=l.rightLeft+(i-categoryOffset)*(CATEGORY_SIZE+GAP),y=l.categoryTop;
            CreativeTabCompat.Tab tab=categories.get(i);
            boolean selected=i==active,hover=inside(mouseX,mouseY,x,y,CATEGORY_SIZE,CATEGORY_SIZE);
            ScreenRenderCompat.fill(graphics,x,y,x+CATEGORY_SIZE,y+CATEGORY_SIZE,selected?0xA0506050:(hover?0x90505050:0x80303030));
            outline(graphics,x,y,CATEGORY_SIZE,CATEGORY_SIZE,selected?0xFF90A890:(hover?0xFFC0C0C0:0xFF606060));
            if(tab.isAll())centered(graphics,tr("dds-tweak.list_picker.category.all"),x+CATEGORY_SIZE/2,y+7,selected?0xFFFFFFFF:0xFFD0D0D0);
            else if(!tab.icon().isEmpty())renderPickerItem(graphics,tab.icon(),x+3,y+3);
            if(hover)hovered=tab.isAll()?tr("dds-tweak.list_picker.category.all"):tab.title();
        }
        return hovered;
    }

    private Item renderItems(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            Layout l,int mouseX,int mouseY){
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND){
            renderSoundIcons(graphics,l,mouseX,mouseY,false);
            return null;
        }
        //#endif
        int total=filtered.size();
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND_BLOCK)total+=filteredItemlessBlocks.size();
        //#endif
        int maxRow=Math.max(0,(total+l.itemColumns-1)/l.itemColumns-l.itemRows);
        itemRowOffset=Math.max(0,Math.min(maxRow,itemRowOffset));
        int start=itemRowOffset*l.itemColumns;
        int end=Math.min(total,start+l.itemColumns*l.itemRows);
        Item hovered=null;
        for(int i=start;i<end;i++){
            int local=i-start;
            int x=l.itemLeft+(local%l.itemColumns)*(SLOT+GAP);
            int y=l.itemsTop+(local/l.itemColumns)*(SLOT+GAP);
            boolean hover=inside(mouseX,mouseY,x,y,SLOT,SLOT);
            //#if MC >= 12111
            if(kind==ItemPickerKind.SOUND_BLOCK&&i>=filtered.size()){
                String id=filteredItemlessBlocks.get(i-filtered.size());
                Item icon=itemlessBlockIcons.get(id);
                if(icon!=null)renderPickerItem(graphics,ItemRegistryCompat.stack(icon),x+3,y+3);
                if(hover){
                    outline(graphics,x,y,SLOT,SLOT,0xFFFFFFFF);
                    hoveredUnknownId=id;
                }
                continue;
            }
            //#endif
            Item item=filtered.get(i);
            if(hover)ScreenRenderCompat.fill(graphics,x,y,x+SLOT,y+SLOT,0x50303030);
            ItemStack stack=ItemRegistryCompat.stack(item);
            if(!stack.isEmpty())renderPickerItem(graphics,stack,x+3,y+3);
            if(hover){outline(graphics,x,y,SLOT,SLOT,0xFFFFFFFF);hovered=item;}
        }
        return hovered;
    }

    //#if MC >= 12111
    /** One representative icon per source family; actions are toggled together. */
    private void renderSoundIcons(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#else
            GuiGraphics graphics,
            //#endif
            Layout l,int mouseX,int mouseY,boolean selected){
        List<String> ids=selected?selectedSoundGroups():filteredSoundGroups;
        int columns=selected?l.selectedColumns:l.itemColumns;
        int visibleRows=selected?l.selectedRows:l.itemRows;
        int maxOffset=Math.max(0,(ids.size()+columns-1)/columns-visibleRows);
        int offset=Math.max(0,Math.min(selected?selectedRowOffset:itemRowOffset,maxOffset));
        if(selected)selectedRowOffset=offset;
        else itemRowOffset=offset;
        int first=offset*columns;
        int last=Math.min(ids.size(),first+visibleRows*columns);
        int left=selected?l.selectedLeft:l.itemLeft;
        int top=selected?l.selectedTop:l.itemsTop;
        for(int index=first;index<last;index++){
            String id=ids.get(index);
            int local=index-first;
            int x=left+local%columns*(SLOT+GAP);
            int y=top+local/columns*(SLOT+GAP);
            boolean hover=inside(mouseX,mouseY,x,y,SLOT,SLOT);
            if(selected||hover){
                ScreenRenderCompat.fill(graphics,x,y,x+SLOT,y+SLOT,selected
                        ?(hover?0xA0506050:0xA0304030):0x50303030);
                outline(graphics,x,y,SLOT,SLOT,hover?0xFFFFFFFF:(selected?0xFF608060:0xFF606060));
            }
            List<String> sounds=soundGroups.get(id);
            if(sounds!=null&&!sounds.isEmpty()){
                ItemStack icon=SoundPickerIcons.icon(sounds.get(0));
                if(!icon.isEmpty())renderPickerItem(graphics,icon,x+3,y+3);
            }
            if(hover)hoveredUnknownId=id;
        }
    }

    private String soundAt(double mouseX,double mouseY,Layout l){
        if(!inside(mouseX,mouseY,l.itemLeft,l.itemsTop,l.itemColumns*(SLOT+GAP),
                l.itemRows*(SLOT+GAP)))return null;
        int col=(int)((mouseX-l.itemLeft)/(SLOT+GAP));
        int row=(int)((mouseY-l.itemsTop)/(SLOT+GAP));
        int x=l.itemLeft+col*(SLOT+GAP),y=l.itemsTop+row*(SLOT+GAP);
        if(!inside(mouseX,mouseY,x,y,SLOT,SLOT))return null;
        int index=(itemRowOffset+row)*l.itemColumns+col;
        return index<filteredSoundGroups.size()?filteredSoundGroups.get(index):null;
    }
    //#endif

    private void tooltip(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY,Item item){
        String state=contains(item)
                ?tr(singleSelection!=null?"dds-tweak.list_picker.current":"dds-tweak.list_picker.selected")
                :tr("dds-tweak.list_picker.unselected");
        showTooltip(graphics,mouseX,mouseY,ItemRegistryCompat.name(item),entryId(item),state);
    }

    private void textTooltip(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY,String value){
        showTooltip(graphics,mouseX,mouseY,value);
    }

    /** Vanilla positioning and clipping, with a width-bound line split for long registry IDs/tags. */
    private void showTooltip(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int mouseX,int mouseY,String... values){
        List<Component> lines=new ArrayList<>();
        int maxWidth=Math.max(32,width-24);
        for(String value:values)appendTooltipLines(lines,value,maxWidth);
        //#if MC >= 260100
        //$$ graphics.setTooltipForNextFrame(font,lines,Optional.empty(),mouseX,mouseY);
        //#elseif MC >= 12000
        graphics.setTooltipForNextFrame(font,lines,Optional.empty(),mouseX,mouseY);
        //#elseif MC >= 11700
        //$$ renderTooltip(graphics,lines,Optional.empty(),mouseX,mouseY);
        //#elseif MC >= 11600
        //$$ renderComponentTooltip(graphics,lines,mouseX,mouseY);
        //#else
        //$$ List<String> legacy=new ArrayList<>();for(Component line:lines)legacy.add(line.getString());renderTooltip(legacy,mouseX,mouseY);
        //#endif
    }

    private void appendTooltipLines(List<Component> lines,String value,int maxWidth){
        if(value==null||value.isEmpty()){lines.add(MinecraftCompat.literal(" "));return;}
        for(int start=0;start<value.length();){
            int end=start;
            while(end<value.length()){
                int next=value.offsetByCodePoints(end,1);
                if(end>start&&font.width(value.substring(start,next))>maxWidth)break;
                end=next;
            }
            lines.add(MinecraftCompat.literal(value.substring(start,end)));
            start=end;
        }
    }

    private void outline(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x,int y,int w,int h,int color){
        ScreenRenderCompat.fill(graphics,x,y,x+w,y+1,color);ScreenRenderCompat.fill(graphics,x,y+h-1,x+w,y+h,color);
        ScreenRenderCompat.fill(graphics,x,y,x+1,y+h,color);ScreenRenderCompat.fill(graphics,x+w-1,y,x+w,y+h,color);
    }

    //#if MC >= 12109
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(super.mouseClicked(event,doubleClick))return true;
        unfocusSearch();
        return handleClick(event.x(),event.y(),event.button());
    }
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){
    //$$     if(super.mouseClicked(x,y,button))return true;
    //$$     unfocusSearch();
    //$$     return handleClick(x,y,button);
    //$$ }
    //#endif

    private void unfocusSearch(){
        ItemPickerFocusCompat.unfocus(searchField);
    }

    private boolean handleClick(double mouseX,double mouseY,int button){
        //#if MC >= 260300
        //$$ if(button!=InputConstants.MOUSE_BUTTON_LEFT)return false;
        //#else
        if(button!=0)return false;
        //#endif
        Layout l=layout();
        int clickedCategory=categoryAt(mouseX,mouseY,l);
        if(clickedCategory>=0){
            categoryIndex=clickedCategory;
            searchField.setValue("");
            itemRowOffset=0;
            refresh();
            return true;
        }
        String selected=selectedEntryAt(mouseX,mouseY,l);
        if(selected!=null){
            if(singleSelection==null){
                //#if MC >= 12111
                if(kind==ItemPickerKind.SOUND)toggleSoundGroup(selected,false);
                else
                //#endif
                remove(selected);
                refresh();
            }
            return true;
        }
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND){
            String id=soundAt(mouseX,mouseY,l);
            if(id!=null){toggleSoundGroup(id,true);refresh();return true;}
            return false;
        }
        //#endif
        Item item=itemAt(mouseX,mouseY,l);
        if(item!=null){
            if(singleSelection==null){toggle(item);refresh();}
            else toggle(item);
            return true;
        }
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND_BLOCK){
            String id=itemlessBlockAt(mouseX,mouseY,l);
            if(id!=null){toggleId(id);refresh();return true;}
        }
        //#endif
        return false;
    }

    //#if MC >= 12002
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){if(scroll(mouseX,mouseY,vertical))return true;return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);}
    //#else
    //$$ @Override public boolean mouseScrolled(double mouseX,double mouseY,double vertical){if(scroll(mouseX,mouseY,vertical))return true;return super.mouseScrolled(mouseX,mouseY,vertical);}
    //#endif

    private boolean scroll(double mouseX,double mouseY,double amount){
        if(amount==0)return false;
        Layout l=layout();
        int direction=amount<0?1:-1;
        if(inside(mouseX,mouseY,l.selectedLeft,l.selectedTop,l.leftWidth,l.selectedRows*(SLOT+GAP))){
            int selectionSize=selectedIds().size();
            //#if MC >= 12111
            if(kind==ItemPickerKind.SOUND)selectionSize=selectedSoundGroups().size();
            //#endif
            int rows=(selectionSize+l.selectedColumns-1)/l.selectedColumns;
            selectedRowOffset=Math.max(0,Math.min(Math.max(0,rows-l.selectedRows),selectedRowOffset+direction));
            return true;
        }
        //#if MC >= 12111
        if(kind!=ItemPickerKind.SOUND)
        //#endif
        if(inside(mouseX,mouseY,l.rightLeft,l.categoryTop,l.rightWidth,CATEGORY_SIZE)){
            categoryOffset=Math.max(0,Math.min(Math.max(0,categories.size()-l.categoryColumns),categoryOffset+direction));
            return true;
        }
        if(inside(mouseX,mouseY,l.itemLeft,l.itemsTop,l.itemColumns*(SLOT+GAP),l.itemRows*(SLOT+GAP))){
            int rows=(filtered.size()+l.itemColumns-1)/l.itemColumns;
            //#if MC >= 12111
            if(kind==ItemPickerKind.SOUND)rows=(filteredSoundGroups.size()+l.itemColumns-1)/l.itemColumns;
            if(kind==ItemPickerKind.SOUND_BLOCK)rows=(filtered.size()+filteredItemlessBlocks.size()+l.itemColumns-1)/l.itemColumns;
            //#endif
            itemRowOffset=Math.max(0,Math.min(Math.max(0,rows-l.itemRows),itemRowOffset+direction));
            return true;
        }
        return false;
    }

    //#if MC >= 12109
    @Override public boolean keyPressed(KeyEvent event){
        if(event.isEscape()){onClose();return true;}
        if(event.key()==257&&addQueryEntry())return true;
        return super.keyPressed(event);
    }
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
    //$$     if(keyCode==256){onClose();return true;}
    //$$     if(keyCode==257&&addQueryEntry())return true;
    //$$     return super.keyPressed(keyCode,scanCode,modifiers);
    //$$ }
    //#endif

    @Override public void onClose(){
        if(target!=null)new Configs().save();
        MinecraftCompat.setScreen(minecraft,parent);
    }

    private void refresh(){
        String q=query.trim().toLowerCase(Locale.ROOT);
        Set<String> selected=selectedIds();
        filtered.clear();
        //#if MC >= 12111
        filteredItemlessBlocks.clear();
        if(kind==ItemPickerKind.SOUND){
            soundGroups.clear();
            soundGroups.putAll(SoundSourceGroups.group(soundIds,selected));
            filteredSoundGroups.clear();
            Set<String> chosenGroups=new HashSet<>(selectedSoundGroups());
            for(Map.Entry<String,List<String>> group:soundGroups.entrySet()){
                String key=group.getKey();
                if(chosenGroups.contains(key))continue;
                if(q.isEmpty()||key.toLowerCase(Locale.ROOT).contains(q)||
                        SoundPickerIcons.sourceLabel(group.getValue().get(0)).toLowerCase(Locale.ROOT).contains(q)||
                        group.getValue().stream().anyMatch(id->id.toLowerCase(Locale.ROOT).contains(q)))
                    filteredSoundGroups.add(key);
            }
            return;
        }
        //#endif
        boolean searching=!q.isEmpty();
        CreativeTabCompat.Tab active=categories.get(Math.max(0,Math.min(categoryIndex,categories.size()-1)));
        for(Item item:allItems){
            String id=entryId(item);
            if(selected.contains(id))continue;
            if(!searching&&!active.contains(item))continue;
            if(!searching||id.toLowerCase(Locale.ROOT).contains(q)||ItemRegistryCompat.name(item).toLowerCase(Locale.ROOT).contains(q))filtered.add(item);
        }
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND_BLOCK&&(searching||active.isAll())){
            for(String id:itemlessBlockIcons.keySet())
                if(!selected.contains(id)&&(q.isEmpty()||id.toLowerCase(Locale.ROOT).contains(q)))
                    filteredItemlessBlocks.add(id);
        }
        //#endif
    }

    private void refreshQuery(){
        itemRowOffset=0;
        refresh();
    }

    private List<SelectedEntry> selectedEntries(){
        List<SelectedEntry> result=new ArrayList<>();
        for(String id:selectedIds())result.add(new SelectedEntry(id,byId.get(id)));
        return result;
    }

    private String selectedEntryAt(double mouseX,double mouseY,Layout l){
        if(mouseX<l.selectedLeft||mouseY<l.selectedTop)return null;
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND){
            if(!inside(mouseX,mouseY,l.selectedLeft,l.selectedTop,
                    l.selectedColumns*(SLOT+GAP),l.selectedRows*(SLOT+GAP)))return null;
            int col=(int)((mouseX-l.selectedLeft)/(SLOT+GAP));
            int row=(int)((mouseY-l.selectedTop)/(SLOT+GAP));
            int x=l.selectedLeft+col*(SLOT+GAP),y=l.selectedTop+row*(SLOT+GAP);
            if(!inside(mouseX,mouseY,x,y,SLOT,SLOT))return null;
            List<String> selected=selectedSoundGroups();
            int index=(selectedRowOffset+row)*l.selectedColumns+col;
            return index<selected.size()?selected.get(index):null;
        }
        //#endif
        int col=(int)((mouseX-l.selectedLeft)/(SLOT+GAP)),row=(int)((mouseY-l.selectedTop)/(SLOT+GAP));
        if(col<0||col>=l.selectedColumns||row<0||row>=l.selectedRows)return null;
        int x=l.selectedLeft+col*(SLOT+GAP),y=l.selectedTop+row*(SLOT+GAP);
        if(!inside(mouseX,mouseY,x,y,SLOT,SLOT))return null;
        List<SelectedEntry> selected=selectedEntries();
        int index=(selectedRowOffset+row)*l.selectedColumns+col;
        return index>=0&&index<selected.size()?selected.get(index).id:null;
    }

    //#if MC >= 12111
    private String itemlessBlockAt(double mouseX,double mouseY,Layout l){
        if(!inside(mouseX,mouseY,l.itemLeft,l.itemsTop,l.itemColumns*(SLOT+GAP),l.itemRows*(SLOT+GAP)))
            return null;
        int col=(int)((mouseX-l.itemLeft)/(SLOT+GAP)),row=(int)((mouseY-l.itemsTop)/(SLOT+GAP));
        int x=l.itemLeft+col*(SLOT+GAP),y=l.itemsTop+row*(SLOT+GAP);
        if(!inside(mouseX,mouseY,x,y,SLOT,SLOT))return null;
        int index=(itemRowOffset+row)*l.itemColumns+col-filtered.size();
        return index>=0&&index<filteredItemlessBlocks.size()?filteredItemlessBlocks.get(index):null;
    }
    //#endif

    private int categoryAt(double mouseX,double mouseY,Layout l){
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND)return -1;
        //#endif
        if(mouseX<l.rightLeft||mouseY<l.categoryTop)return -1;
        int col=(int)((mouseX-l.rightLeft)/(CATEGORY_SIZE+GAP));
        if(col<0||col>=l.categoryColumns)return -1;
        int x=l.rightLeft+col*(CATEGORY_SIZE+GAP);
        if(!inside(mouseX,mouseY,x,l.categoryTop,CATEGORY_SIZE,CATEGORY_SIZE))return -1;
        int index=categoryOffset+col;
        return index<categories.size()?index:-1;
    }

    private Item itemAt(double mouseX,double mouseY,Layout l){
        if(mouseX<l.itemLeft||mouseY<l.itemsTop)return null;
        int col=(int)((mouseX-l.itemLeft)/(SLOT+GAP)),row=(int)((mouseY-l.itemsTop)/(SLOT+GAP));
        if(col<0||col>=l.itemColumns||row<0||row>=l.itemRows)return null;
        int x=l.itemLeft+col*(SLOT+GAP),y=l.itemsTop+row*(SLOT+GAP);
        if(!inside(mouseX,mouseY,x,y,SLOT,SLOT))return null;
        int index=(itemRowOffset+row)*l.itemColumns+col;
        return index>=0&&index<filtered.size()?filtered.get(index):null;
    }

    private List<String> values(){
        if(target!=null)return target.getStrings();
        return multiSelection!=null?stagedIds:Collections.singletonList(singleSelectedId);
    }
    private void setMultiValues(List<String> next){
        if(target!=null && ItemPickerConfigBindings.isRestockList(target)
                && !RestockPreferencePolicy.valid(next)){
            if(Minecraft.getInstance().player!=null)
                MinecraftCompat.overlayMessage(Minecraft.getInstance(),
                        MinecraftCompat.translatable("dds-tweak.shulker_restock.invalid_list"));
            return;
        }
        if(target!=null)target.setStrings(next);
        else {stagedIds.clear();stagedIds.addAll(next);}
    }
    private Set<String> selectedIds(){return new LinkedHashSet<>(values());}
    private boolean contains(Item item){return selectedIds().contains(entryId(item));}
    private void toggle(Item item){
        String id=entryId(item);
        if(singleSelection!=null){
            singleSelectedId=id;
            refresh();
            return;
        }
        toggleId(id);
    }
    private void toggleId(String id){
        List<String> next=new ArrayList<>(values());
        if(!next.remove(id))next.add(id);
        setMultiValues(next);
    }
    //#if MC >= 12111
    private List<String> selectedSoundGroups(){
        return SoundSourceGroups.selectedKeys(soundGroups,selectedIds());
    }

    private void toggleSoundGroup(String key,boolean include){
        List<String> ids=soundGroups.get(key);
        if(ids!=null)setMultiValues(SoundSourceGroups.toggle(values(),ids,include));
    }
    //#endif

    private void remove(String id){
        if(singleSelection!=null)return;
        List<String> next=new ArrayList<>(values());
        if(next.remove(id))setMultiValues(next);
    }
    private void confirmSelection(){
        if(singleSelection!=null)singleSelection.accept(singleSelectedId);
        else if(multiSelection!=null)multiSelection.accept(new ArrayList<>(stagedIds));
        onClose();
    }

    private void resetOrClear(){
        if(singleSelection!=null){
            singleSelectedId=singleDefaultId;
            refresh();
        }else if(!values().isEmpty())setMultiValues(Collections.emptyList());
    }
    private boolean addQueryEntry(){
        if(searchField==null||!searchField.isFocused())return false;
        //#if MC >= 12111
        if(kind!=ItemPickerKind.BLOCK&&kind!=ItemPickerKind.SOUND_BLOCK&&kind!=ItemPickerKind.SOUND)return false;
        //#else
        //$$ if(kind!=ItemPickerKind.BLOCK)return false;
        //#endif
        String value=query.trim().toLowerCase(Locale.ROOT);
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND_BLOCK&&value.startsWith("#"))return false;
        if(kind==ItemPickerKind.SOUND){
            if(value.startsWith("#"))return false;
            List<String> group=soundGroups.get(SoundSourceGroups.key(value));
            if(group!=null){
                toggleSoundGroup(SoundSourceGroups.key(value),true);
                searchField.setValue("");
                return true;
            }
        }
        //#endif
        String id=value.startsWith("#")?value.substring(1):value;
        if(!validRawId(id))return false;
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND_BLOCK&&!byId.containsKey(id)&&!itemlessBlockIcons.containsKey(id))return false;
        //#endif
        List<String> next=new ArrayList<>(values());
        if(!next.contains(value)){next.add(value);setMultiValues(next);}
        searchField.setValue("");
        return true;
    }
    private boolean isCandidate(Item item){
        if(kind==ItemPickerKind.FOOD)return SilentAutoEatFoodRules.isEdible(item);
        //#if MC >= 12111
        if(kind==ItemPickerKind.BLOCK||kind==ItemPickerKind.SOUND_BLOCK)return item instanceof BlockItem;
        //#endif
        return true;
    }
    private String entryId(Item item){
        //#if MC >= 12111
        if((kind==ItemPickerKind.BLOCK||kind==ItemPickerKind.SOUND_BLOCK)&&item instanceof BlockItem)
            return BuiltInRegistries.BLOCK.getKey(((BlockItem)item).getBlock()).toString();
        //#endif
        return ItemRegistryCompat.id(item);
    }
    private static boolean validRawId(String value){return value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");}

    private Layout layout(){
        int panelWidth=Math.min(380,Math.max(250,width-24));
        int left=(width-panelWidth)/2,top=SharedScreenBackground.navigationTop(height);
        int innerLeft=left+10,innerWidth=panelWidth-20;
        int leftWidth=Math.max(70,(innerWidth-10)*28/100);
        int rightLeft=innerLeft+leftWidth+10,rightWidth=innerLeft+innerWidth-rightLeft;
        int selectedColumns=Math.max(1,(leftWidth+GAP)/(SLOT+GAP));
        int selectedRows=Math.max(1,(SharedScreenBackground.FOOTER_RULE_Y-3-SELECTED_Y+GAP)/(SLOT+GAP));
        int categoryColumns=Math.max(1,(rightWidth+GAP)/(CATEGORY_SIZE+GAP));
        int itemColumns=Math.max(1,(rightWidth+GAP)/(SLOT+GAP));
        int itemsOffset=ITEMS_Y;
        //#if MC >= 12111
        if(kind==ItemPickerKind.SOUND)itemsOffset=CATEGORY_Y;
        //#endif
        int itemRows=Math.max(1,(SharedScreenBackground.FOOTER_RULE_Y-3-itemsOffset+GAP)/(SLOT+GAP));
        return new Layout(top,innerLeft,leftWidth,rightLeft,rightWidth,top+SELECTED_Y,selectedColumns,selectedRows,
                top+SEARCH_Y,top+CATEGORY_Y,categoryColumns,top+itemsOffset,rightLeft,itemColumns,itemRows,
                top+SharedScreenBackground.FOOTER_ROW_Y);
    }

    /** Match the vanilla 1.14.4 GUI item-lighting setup around each picker icon. */
    private void renderPickerItem(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            ItemStack stack,int x,int y){
        //#if MC < 11500
        //$$ GlStateManager.pushLightingAttributes();
        //$$ try {
        //$$     GlStateManager.color4f(1.0F,1.0F,1.0F,1.0F);
        //$$     GlStateManager.enableRescaleNormal();
        //$$     GlStateManager.enableBlend();
        //$$     GlStateManager.blendFuncSeparate(770,771,1,0);
        //$$     Lighting.turnOnGui();
        //#endif
        ScreenRenderCompat.item(graphics,stack,x,y);
        //#if MC < 11500
        //$$ } finally {
        //$$     Lighting.turnOff();
        //$$     GlStateManager.popAttributes();
        //$$ }
        //#endif
    }

    private String fit(String value,int maxWidth){
        if(maxWidth<=0)return "";
        if(font.width(value)<=maxWidth)return value;
        String suffix="…";
        if(font.width(suffix)>=maxWidth)return suffix;
        int end=value.length();
        while(end>0&&font.width(value.substring(0,end)+suffix)>maxWidth)end=value.offsetByCodePoints(end,-1);
        return end>0?value.substring(0,end)+suffix:suffix;
    }

    private void text(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            String value,int x,int y,int color){ScreenRenderCompat.text(graphics,font,value,x,y,color,true);}

    private void centered(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            String value,int centerX,int y,int color){text(graphics,value,centerX-font.width(value)/2,y,color);}

    private static boolean inside(double x,double y,int left,int top,int width,int height){return x>=left&&x<left+width&&y>=top&&y<top+height;}
    private static String tr(String key,Object...args){return MinecraftCompat.translatable(key,args).getString();}

    private static final class SelectedEntry{
        final String id;
        final Item item;
        SelectedEntry(String id,Item item){this.id=id;this.item=item;}
    }

    private static final class Layout{
        final int top,selectedLeft,leftWidth,rightLeft,rightWidth,selectedTop,selectedColumns,selectedRows;
        final int searchTop,categoryTop,categoryColumns,itemsTop,itemLeft,itemColumns,itemRows,bottomTop;
        Layout(int top,int selectedLeft,int leftWidth,int rightLeft,int rightWidth,int selectedTop,int selectedColumns,
                int selectedRows,int searchTop,int categoryTop,int categoryColumns,int itemsTop,int itemLeft,
                int itemColumns,int itemRows,int bottomTop){
            this.top=top;this.selectedLeft=selectedLeft;this.leftWidth=leftWidth;
            this.rightLeft=rightLeft;this.rightWidth=rightWidth;this.selectedTop=selectedTop;
            this.selectedColumns=selectedColumns;this.selectedRows=selectedRows;
            this.searchTop=searchTop;this.categoryTop=categoryTop;this.categoryColumns=categoryColumns;
            this.itemsTop=itemsTop;this.itemLeft=itemLeft;this.itemColumns=itemColumns;
            this.itemRows=itemRows;this.bottomTop=bottomTop;
        }
    }
}

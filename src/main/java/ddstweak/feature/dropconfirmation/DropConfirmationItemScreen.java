package ddstweak.feature.dropconfirmation;

import ddstweak.config.Configs;
import ddstweak.feature.silentautoeat.SilentAutoEatFoodRules;
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
//#if MC >= 12109
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//#if MC >= 12111
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
//#endif
import java.util.*;
import java.util.function.Consumer;

public final class DropConfirmationItemScreen extends Screen {
    private static final int SLOT=22,GAP=2,CHIP_H=22,CATEGORY_SIZE=22,BUTTON_H=16,BOTTOM_MARGIN=6,BOTTOM_GAP=6;
    private static final int SEPARATOR_COLOR=0x40606060;
    private final Screen parent;
    private final IConfigStringList target;
    private final Consumer<String> singleSelection;
    private final Kind kind;
    private final String titleKey;
    private String singleSelectedId="";
    private final List<Item> allItems=new ArrayList<>();
    private final Map<String,Item> byId=new LinkedHashMap<>();
    private final List<Item> filtered=new ArrayList<>();
    private final List<CreativeTabCompat.Tab> categories;
    private String query="";
    private boolean searchFocused;
    private int categoryIndex;
    private int itemRowOffset;
    private int selectedRowOffset;

    public DropConfirmationItemScreen(Screen parent,IConfigStringList target,Kind kind,String titleKey){
        this(parent,Objects.requireNonNull(target,"target"),kind,titleKey,"",null);
    }

    public DropConfirmationItemScreen(Screen parent,String selectedId,Consumer<String> onSelected,String titleKey){
        this(parent,null,Kind.ITEM,titleKey,selectedId,Objects.requireNonNull(onSelected,"onSelected"));
    }

    private DropConfirmationItemScreen(Screen parent,IConfigStringList target,Kind kind,String titleKey,String selectedId,Consumer<String> singleSelection){
        super(MinecraftCompat.translatable(titleKey));
        this.parent=parent;
        this.target=target;
        this.singleSelection=singleSelection;
        this.kind=Objects.requireNonNull(kind,"kind");
        this.titleKey=Objects.requireNonNull(titleKey,"titleKey");
        this.singleSelectedId=selectedId==null?"":selectedId.trim();
        for(Item item:ItemRegistryCompat.allItems()){
            if(!isCandidate(item))continue;
            allItems.add(item);
            byId.put(entryId(item),item);
        }
        this.categories=kind==Kind.FOOD
                ?CreativeTabCompat.tabs(Minecraft.getInstance(),allItems)
                :CreativeTabCompat.tabs(Minecraft.getInstance());
        refresh();
    }

    @Override public boolean isPauseScreen(){return false;}

    //#if MC >= 260100
    //$$ @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){super.extractRenderState(graphics,mouseX,mouseY,partialTick);renderContent(graphics,mouseX,mouseY);}
    //#elseif MC >= 12000
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){super.render(graphics,mouseX,mouseY,partialTick);renderContent(graphics,mouseX,mouseY);}
    //#elseif MC >= 11600
    //$$ @Override public void render(PoseStack graphics,int mouseX,int mouseY,float partialTick){super.render(graphics,mouseX,mouseY,partialTick);renderContent(graphics,mouseX,mouseY);}
    //#else
    //$$ @Override public void render(int mouseX,int mouseY,float partialTick){super.render(mouseX,mouseY,partialTick);renderContent(null,mouseX,mouseY);}
    //#endif

    private void renderContent(
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
        ScreenRenderCompat.fill(graphics,0,0,width,height,0x90000000);
        ScreenRenderCompat.fill(graphics,l.left,l.top,l.left+l.width,l.top+l.height,0xEE101010);
        outline(graphics,l.left,l.top,l.width,l.height,0xFF707070);
        centered(graphics,tr(titleKey),l.left+l.width/2,l.top+8,0xFFFFFFFF);
        text(graphics,tr("dds-tweak.list_picker.selected_header",selectedIds().size()),l.innerLeft,l.top+22,0xFFD8D8D8);
        separator(graphics,l.innerLeft,l.selectedTop-3,l.innerWidth);

        Item hovered=renderSelected(graphics,l,mouseX,mouseY);

        String searchValue=query.isEmpty()?tr(kind==Kind.BLOCK?"dds-tweak.list_picker.search_hint.block":"dds-tweak.list_picker.search_hint.item"):query;
        int searchColor=query.isEmpty()?0xFF909090:0xFFFFFFFF;
        separator(graphics,l.innerLeft,l.searchTop-4,l.innerWidth);
        ScreenRenderCompat.fill(graphics,l.innerLeft,l.searchTop,l.innerLeft+l.innerWidth,l.searchTop+18,searchFocused?0xD0282828:0xC0202020);
        outline(graphics,l.innerLeft,l.searchTop,l.innerWidth,18,searchFocused?0xFFC0C0C0:0xFF606060);
        text(graphics,tr("dds-tweak.list_picker.search",searchValue),l.innerLeft+5,l.searchTop+5,searchColor);
        if(searchFocused&&(System.currentTimeMillis()/500L&1L)==0L){
            String prefix=tr("dds-tweak.list_picker.search","");
            int caretX=l.innerLeft+5+font.width(prefix)+(query.isEmpty()?0:font.width(query));
            ScreenRenderCompat.fill(graphics,caretX,l.searchTop+4,caretX+1,l.searchTop+14,0xFFFFFFFF);
        }

        String categoryHovered=renderCategories(graphics,l,mouseX,mouseY);

        Item gridHovered=renderItems(graphics,l,mouseX,mouseY);
        if(gridHovered!=null)hovered=gridHovered;

        separator(graphics,l.innerLeft,l.bottomTop-4,l.innerWidth);
        drawButton(graphics,l.innerLeft,l.bottomTop,64,BUTTON_H,tr("dds-tweak.list_picker.clear"),inside(mouseX,mouseY,l.innerLeft,l.bottomTop,64,BUTTON_H));
        drawButton(graphics,l.innerLeft+l.innerWidth-64,l.bottomTop,64,BUTTON_H,tr("dds-tweak.list_picker.done"),inside(mouseX,mouseY,l.innerLeft+l.innerWidth-64,l.bottomTop,64,BUTTON_H));

        if(hovered!=null)tooltip(graphics,mouseX,mouseY,hovered);
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
        List<SelectedEntry> selected=selectedEntries();
        int maxRow=Math.max(0,(selected.size()+l.selectedColumns-1)/l.selectedColumns-l.selectedRows);
        selectedRowOffset=Math.max(0,Math.min(maxRow,selectedRowOffset));
        int start=selectedRowOffset*l.selectedColumns;
        int end=Math.min(selected.size(),start+l.selectedColumns*l.selectedRows);
        Item hovered=null;
        for(int i=start;i<end;i++){
            int local=i-start,col=local%l.selectedColumns,row=local/l.selectedColumns;
            int x=l.innerLeft+col*(l.chipWidth+GAP),y=l.selectedTop+row*(CHIP_H+GAP);
            SelectedEntry entry=selected.get(i);
            Item item=entry.item;
            boolean hover=inside(mouseX,mouseY,x,y,l.chipWidth,CHIP_H);
            ScreenRenderCompat.fill(graphics,x,y,x+l.chipWidth,y+CHIP_H,hover?0xA0506050:0xA0304030);
            outline(graphics,x,y,l.chipWidth,CHIP_H,hover?0xFFFFFFFF:0xFF608060);
            ItemStack stack=item==null?ItemStack.EMPTY:ItemRegistryCompat.stack(item);
            if(!stack.isEmpty())ScreenRenderCompat.item(graphics,stack,x+3,y+3);
            String label=item==null?entry.id:ItemRegistryCompat.name(item);
            text(graphics,fit(label,l.chipWidth-(stack.isEmpty()?8:26)),x+(stack.isEmpty()?5:23),y+7,0xFFFFFFFF);
            if(hover&&item!=null)hovered=item;
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
        int active=query.isEmpty()?categoryIndex:0;
        String hovered=null;
        for(int i=0;i<categories.size();i++){
            int col=i%l.categoryColumns,row=i/l.categoryColumns;
            int x=l.innerLeft+col*(CATEGORY_SIZE+GAP),y=l.categoryTop+row*(CATEGORY_SIZE+GAP);
            CreativeTabCompat.Tab tab=categories.get(i);
            boolean selected=i==active,hover=inside(mouseX,mouseY,x,y,CATEGORY_SIZE,CATEGORY_SIZE);
            ScreenRenderCompat.fill(graphics,x,y,x+CATEGORY_SIZE,y+CATEGORY_SIZE,selected?0xA0506050:(hover?0x90505050:0x80303030));
            outline(graphics,x,y,CATEGORY_SIZE,CATEGORY_SIZE,selected?0xFF90A890:(hover?0xFFC0C0C0:0xFF606060));
            if(tab.isAll())centered(graphics,tr("dds-tweak.list_picker.category.all"),x+CATEGORY_SIZE/2,y+7,selected?0xFFFFFFFF:0xFFD0D0D0);
            else if(!tab.icon().isEmpty())ScreenRenderCompat.item(graphics,tab.icon(),x+3,y+3);
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
        int maxRow=Math.max(0,(filtered.size()+l.itemColumns-1)/l.itemColumns-l.itemRows);
        itemRowOffset=Math.max(0,Math.min(maxRow,itemRowOffset));
        int start=itemRowOffset*l.itemColumns;
        int end=Math.min(filtered.size(),start+l.itemColumns*l.itemRows);
        Item hovered=null;
        for(int i=start;i<end;i++){
            int local=i-start;
            int x=l.itemLeft+(local%l.itemColumns)*(SLOT+GAP);
            int y=l.itemsTop+(local/l.itemColumns)*(SLOT+GAP);
            Item item=filtered.get(i);
            boolean hover=inside(mouseX,mouseY,x,y,SLOT,SLOT);
            if(hover)ScreenRenderCompat.fill(graphics,x,y,x+SLOT,y+SLOT,0x50303030);
            ItemStack stack=ItemRegistryCompat.stack(item);
            if(!stack.isEmpty())ScreenRenderCompat.item(graphics,stack,x+3,y+3);
            if(hover){outline(graphics,x,y,SLOT,SLOT,0xFFFFFFFF);hovered=item;}
        }
        return hovered;
    }

    private void drawButton(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x,int y,int w,int h,String label,boolean hover){
        ScreenRenderCompat.fill(graphics,x,y,x+w,y+h,hover?0xA0505050:0xA0303030);
        outline(graphics,x,y,w,h,hover?0xFFFFFFFF:0xFF707070);
        centered(graphics,label,x+w/2,y+4,0xFFFFFFFF);
    }

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
        String name=ItemRegistryCompat.name(item),id=ItemRegistryCompat.id(item);
        String state=contains(item)?tr("dds-tweak.list_picker.selected"):tr("dds-tweak.list_picker.unselected");
        int w=Math.max(font.width(name),Math.max(font.width(id),font.width(state)))+8,h=36;
        int x=Math.max(2,Math.min(mouseX+10,width-w-2)),y=Math.max(2,Math.min(mouseY+8,height-h-2));
        ScreenRenderCompat.fill(graphics,x,y,x+w,y+h,0xF0101010);outline(graphics,x,y,w,h,0xFF707070);
        text(graphics,name,x+4,y+4,0xFFFFFFFF);text(graphics,id,x+4,y+14,0xFFD0D0D0);text(graphics,state,x+4,y+24,0xFFB8D8B8);
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
        int w=font.width(value)+8,h=16;
        int x=Math.max(2,Math.min(mouseX+10,width-w-2)),y=Math.max(2,Math.min(mouseY+8,height-h-2));
        ScreenRenderCompat.fill(graphics,x,y,x+w,y+h,0xF0101010);outline(graphics,x,y,w,h,0xFF707070);
        text(graphics,value,x+4,y+4,0xFFFFFFFF);
    }

    private void separator(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x,int y,int width){
        ScreenRenderCompat.fill(graphics,x,y,x+width,y+1,SEPARATOR_COLOR);
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
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){if(handleClick(event.x(),event.y(),event.button()))return true;return super.mouseClicked(event,doubleClick);}
    //#else
    //$$ @Override public boolean mouseClicked(double x,double y,int button){if(handleClick(x,y,button))return true;return super.mouseClicked(x,y,button);}
    //#endif

    private boolean handleClick(double mouseX,double mouseY,int button){
        if(button!=0)return false;
        Layout l=layout();
        if(inside(mouseX,mouseY,l.innerLeft,l.searchTop,l.innerWidth,18)){searchFocused=true;return true;}
        int clickedCategory=categoryAt(mouseX,mouseY,l);
        if(clickedCategory>=0){
            categoryIndex=clickedCategory;
            query="";
            searchFocused=false;
            itemRowOffset=0;
            refresh();
            return true;
        }
        searchFocused=false;
        if(inside(mouseX,mouseY,l.innerLeft,l.bottomTop,64,BUTTON_H)){clear();refresh();return true;}
        if(inside(mouseX,mouseY,l.innerLeft+l.innerWidth-64,l.bottomTop,64,BUTTON_H)){onClose();return true;}
        String selected=selectedEntryAt(mouseX,mouseY,l);
        if(selected!=null){
            if(singleSelection==null){remove(selected);refresh();}
            return true;
        }
        Item item=itemAt(mouseX,mouseY,l);
        if(item!=null){
            if(singleSelection==null){toggle(item);refresh();}
            else toggle(item);
            return true;
        }
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
        if(inside(mouseX,mouseY,l.innerLeft,l.selectedTop,l.innerWidth,l.selectedRows*(CHIP_H+GAP))){
            int rows=(selectedEntries().size()+l.selectedColumns-1)/l.selectedColumns;
            selectedRowOffset=Math.max(0,Math.min(Math.max(0,rows-l.selectedRows),selectedRowOffset+(amount<0?1:-1)));
            return true;
        }
        if(inside(mouseX,mouseY,l.itemLeft,l.itemsTop,l.itemColumns*(SLOT+GAP),l.itemRows*(SLOT+GAP))){
            int rows=(filtered.size()+l.itemColumns-1)/l.itemColumns;
            itemRowOffset=Math.max(0,Math.min(Math.max(0,rows-l.itemRows),itemRowOffset+(amount<0?1:-1)));
            return true;
        }
        return false;
    }

    //#if MC >= 12109
    @Override public boolean keyPressed(KeyEvent event){
        if(event.isEscape()){onClose();return true;}
        if(event.key()==257&&addQueryEntry())return true;
        if(event.key()==259&&!query.isEmpty()){searchFocused=true;query=query.substring(0,query.offsetByCodePoints(query.length(),-1));refreshQuery();return true;}
        return super.keyPressed(event);
    }
    @Override public boolean charTyped(CharacterEvent event){
        if(event.isAllowedChatCharacter()){searchFocused=true;query+=event.codepointAsString();refreshQuery();return true;}
        return super.charTyped(event);
    }
    //#else
    //$$ @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
    //$$     if(keyCode==256){onClose();return true;}
    //$$     if(keyCode==257&&addQueryEntry())return true;
    //$$     if(keyCode==259&&!query.isEmpty()){searchFocused=true;query=query.substring(0,query.offsetByCodePoints(query.length(),-1));refreshQuery();return true;}
    //$$     return super.keyPressed(keyCode,scanCode,modifiers);
    //$$ }
    //$$ @Override public boolean charTyped(char codePoint,int modifiers){
    //$$     if(!Character.isISOControl(codePoint)){searchFocused=true;query+=codePoint;refreshQuery();return true;}
    //$$     return super.charTyped(codePoint,modifiers);
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
        boolean searching=!q.isEmpty();
        CreativeTabCompat.Tab active=categories.get(Math.max(0,Math.min(categoryIndex,categories.size()-1)));
        for(Item item:allItems){
            String id=entryId(item);
            if(selected.contains(id))continue;
            if(!searching&&!active.contains(item))continue;
            if(!searching||id.toLowerCase(Locale.ROOT).contains(q)||ItemRegistryCompat.name(item).toLowerCase(Locale.ROOT).contains(q))filtered.add(item);
        }
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
        if(mouseX<l.innerLeft||mouseY<l.selectedTop)return null;
        int col=(int)((mouseX-l.innerLeft)/(l.chipWidth+GAP)),row=(int)((mouseY-l.selectedTop)/(CHIP_H+GAP));
        if(col<0||col>=l.selectedColumns||row<0||row>=l.selectedRows)return null;
        int x=l.innerLeft+col*(l.chipWidth+GAP),y=l.selectedTop+row*(CHIP_H+GAP);
        if(!inside(mouseX,mouseY,x,y,l.chipWidth,CHIP_H))return null;
        List<SelectedEntry> selected=selectedEntries();
        int index=(selectedRowOffset+row)*l.selectedColumns+col;
        return index>=0&&index<selected.size()?selected.get(index).id:null;
    }

    private int categoryAt(double mouseX,double mouseY,Layout l){
        if(mouseX<l.innerLeft||mouseY<l.categoryTop)return -1;
        int col=(int)((mouseX-l.innerLeft)/(CATEGORY_SIZE+GAP)),row=(int)((mouseY-l.categoryTop)/(CATEGORY_SIZE+GAP));
        if(col<0||col>=l.categoryColumns||row<0||row>=l.categoryRows)return -1;
        int x=l.innerLeft+col*(CATEGORY_SIZE+GAP),y=l.categoryTop+row*(CATEGORY_SIZE+GAP);
        if(!inside(mouseX,mouseY,x,y,CATEGORY_SIZE,CATEGORY_SIZE))return -1;
        int index=row*l.categoryColumns+col;
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
        return singleSelectedId.isEmpty()?Collections.emptyList():Collections.singletonList(singleSelectedId);
    }
    private Set<String> selectedIds(){return new LinkedHashSet<>(values());}
    private boolean contains(Item item){return selectedIds().contains(entryId(item));}
    private void toggle(Item item){
        String id=entryId(item);
        if(singleSelection!=null){
            singleSelectedId=id;
            singleSelection.accept(id);
            MinecraftCompat.setScreen(minecraft,parent);
            return;
        }
        List<String> next=new ArrayList<>(values());
        if(!next.remove(id))next.add(id);
        target.setStrings(next);
    }
    private void remove(String id){
        if(target==null)return;
        List<String> next=new ArrayList<>(values());
        if(next.remove(id))target.setStrings(next);
    }
    private void clear(){
        if(singleSelection!=null){
            singleSelectedId="";
            singleSelection.accept("");
            refresh();
        }else if(target!=null&&!values().isEmpty())target.setStrings(Collections.emptyList());
    }
    private boolean addQueryEntry(){
        if(kind!=Kind.BLOCK)return false;
        String value=query.trim().toLowerCase(Locale.ROOT);
        String id=value.startsWith("#")?value.substring(1):value;
        if(!validRawId(id))return false;
        List<String> next=new ArrayList<>(values());
        if(!next.contains(value)){next.add(value);target.setStrings(next);}
        query="";
        refreshQuery();
        return true;
    }
    private boolean isCandidate(Item item){
        if(kind==Kind.FOOD)return SilentAutoEatFoodRules.isEdible(item);
        //#if MC >= 12111
        if(kind==Kind.BLOCK)return item instanceof BlockItem;
        //#endif
        return true;
    }
    private String entryId(Item item){
        //#if MC >= 12111
        if(kind==Kind.BLOCK&&item instanceof BlockItem)return BuiltInRegistries.BLOCK.getKey(((BlockItem)item).getBlock()).toString();
        //#endif
        return ItemRegistryCompat.id(item);
    }
    private static boolean validRawId(String value){return value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");}

    private Layout layout(){
        int panelWidth=Math.min(420,Math.max(180,width-24));
        int panelHeight=Math.min(330,Math.max(200,height-24));
        int left=(width-panelWidth)/2,top=(height-panelHeight)/2;
        int innerLeft=left+10,innerWidth=panelWidth-20;
        int selectedColumns=Math.max(2,innerWidth/100);
        int selectedRows=panelHeight>=250?2:1;
        int chipWidth=(innerWidth-(selectedColumns-1)*GAP)/selectedColumns;
        int selectedTop=top+34;
        int searchTop=selectedTop+selectedRows*(CHIP_H+GAP)+5;
        int categoryTop=searchTop+22;
        int categoryColumns=Math.max(1,(innerWidth+GAP)/(CATEGORY_SIZE+GAP));
        int categoryRows=Math.max(1,(categories.size()+categoryColumns-1)/categoryColumns);
        int itemsTop=categoryTop+categoryRows*(CATEGORY_SIZE+GAP)+2;
        int bottomTop=top+panelHeight-BOTTOM_MARGIN-BUTTON_H;
        int itemColumns=Math.max(1,(innerWidth+GAP)/(SLOT+GAP));
        int rowWidth=itemColumns*SLOT+(itemColumns-1)*GAP;
        int itemLeft=left+(panelWidth-rowWidth)/2;
        int itemRows=Math.max(1,(bottomTop-itemsTop-BOTTOM_GAP+GAP)/(SLOT+GAP));
        return new Layout(left,top,panelWidth,panelHeight,innerLeft,innerWidth,selectedTop,selectedColumns,selectedRows,chipWidth,searchTop,categoryTop,categoryColumns,categoryRows,itemsTop,itemLeft,itemColumns,itemRows,bottomTop);
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

    public enum Kind{ITEM,FOOD,BLOCK}

    private static final class SelectedEntry{
        final String id;
        final Item item;
        SelectedEntry(String id,Item item){this.id=id;this.item=item;}
    }

    private static final class Layout{
        final int left,top,width,height,innerLeft,innerWidth,selectedTop,selectedColumns,selectedRows,chipWidth,searchTop,categoryTop,categoryColumns,categoryRows,itemsTop,itemLeft,itemColumns,itemRows,bottomTop;
        Layout(int left,int top,int width,int height,int innerLeft,int innerWidth,int selectedTop,int selectedColumns,int selectedRows,int chipWidth,int searchTop,int categoryTop,int categoryColumns,int categoryRows,int itemsTop,int itemLeft,int itemColumns,int itemRows,int bottomTop){
            this.left=left;this.top=top;this.width=width;this.height=height;this.innerLeft=innerLeft;this.innerWidth=innerWidth;
            this.selectedTop=selectedTop;this.selectedColumns=selectedColumns;this.selectedRows=selectedRows;this.chipWidth=chipWidth;
            this.searchTop=searchTop;this.categoryTop=categoryTop;this.categoryColumns=categoryColumns;this.categoryRows=categoryRows;
            this.itemsTop=itemsTop;this.itemLeft=itemLeft;this.itemColumns=itemColumns;this.itemRows=itemRows;this.bottomTop=bottomTop;
        }
    }
}

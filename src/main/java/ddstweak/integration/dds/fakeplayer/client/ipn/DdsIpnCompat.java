package ddstweak.integration.dds.fakeplayer.client.ipn;

import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryScreen;
import ddstweak.mixin.fakeplayer.AbstractContainerScreenAccessor;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import java.lang.reflect.*;
import java.util.*;

public final class DdsIpnCompat {
    private static final String AREA_TYPES="org.anti_ad.mc.ipnext.inventory.AreaTypes",AREA_TYPE="org.anti_ad.mc.ipnext.inventory.AreaType",ITEM_AREA="org.anti_ad.mc.ipnext.inventory.ItemArea";private static Object sortSource,sortProxy,storageSource,storageProxy;private static volatile boolean broken;
    private DdsIpnCompat(){}
    public static boolean level1Ready(){if(broken)return false;try{Class<?> types=Class.forName(AREA_TYPES),area=Class.forName(AREA_TYPE),itemArea=Class.forName(ITEM_AREA);Object instance=types.getField("INSTANCE").get(null);Method storage=types.getDeclaredMethod("getItemStorage"),sortable=types.getDeclaredMethod("getSortableItemStorage");if(storage.getReturnType()!=area||sortable.getReturnType()!=area||storage.invoke(instance)!=storageProxy||sortable.invoke(instance)!=sortProxy)return false;boolean get=false;Method factory=null;for(Method m:area.getMethods())if(m.getName().equals("getItemArea")&&m.getParameterCount()==2&&m.getReturnType()==itemArea)get=true;Object companion=itemArea.getField("Companion").get(null);for(Method m:companion.getClass().getMethods())if(m.getName().equals("invoke")&&m.getParameterCount()==3&&m.getReturnType()==itemArea){factory=m;break;}if(!get||factory==null)return false;Object empty=factory.invoke(companion,Collections.emptyList(),Collections.emptyList(),false);Method minus=itemArea.getMethod("minus",itemArea);return minus.getReturnType()==itemArea&&itemArea.isInstance(minus.invoke(empty,empty));}catch(ReflectiveOperationException|LinkageError|RuntimeException ignored){return false;}}
    public static synchronized Object wrapSortableArea(Object area){return wrapArea(area,true);}public static synchronized Object wrapStorageArea(Object area){return wrapArea(area,false);}
    private static Object wrapArea(Object area,boolean sortable){if(area==null)return null;Object source=sortable?sortSource:storageSource,cached=sortable?sortProxy:storageProxy;if(area==cached||area==source&&cached!=null)return cached;try{Class<?> type=Class.forName(AREA_TYPE);if(!type.isInstance(area))return area;Object wrapped=areaProxy(type,area,sortable);if(sortable){sortSource=area;sortProxy=wrapped;}else{storageSource=area;storageProxy=wrapped;}return wrapped;}catch(ReflectiveOperationException|LinkageError|RuntimeException e){broken=true;return area;}}
    private static Object areaProxy(Class<?> type,Object area,boolean sortable){return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->{if(isGetItemArea(method,args)&&isUnifiedStorageScreen())return fixedArea((List<?>)args[1],sortable);if(method.getName().equals("minus")&&args!=null&&args.length==1&&type.isInstance(args[0])&&isUnifiedStorageScreen()){Object original=invoke(method,area,args),other=args[0];return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(derived,child,childArgs)->{if(isGetItemArea(child,childArgs)&&isUnifiedStorageScreen()){Object left=fixedArea((List<?>)childArgs[1],sortable);return left.getClass().getMethod("minus",Class.forName(ITEM_AREA)).invoke(left,invoke(child,other,childArgs));}return invoke(child,original,childArgs);});}return invoke(method,area,args);});}
    public static boolean isFakeScreen(){return isUnifiedStorageScreen();}private static boolean isUnifiedStorageScreen(){UnifiedPlayerInventoryMirrorMenu menu=menu();return menu!=null&&(menu.kind()==PlayerInventoryKind.INVENTORY||menu.kind()==PlayerInventoryKind.ENDER_CHEST);}private static boolean isGetItemArea(Method method,Object[] args){return method.getName().equals("getItemArea")&&method.getParameterCount()==2&&args!=null&&args.length==2&&args[1] instanceof List<?>;}
    private static Object invoke(Method method,Object target,Object[] args)throws Throwable{try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}}
    private static int hoveredMenuSlot(){Object screen=MinecraftCompat.screen(Minecraft.getInstance());if(!(screen instanceof UnifiedPlayerInventoryScreen))return -1;UnifiedPlayerInventoryScreen unified=(UnifiedPlayerInventoryScreen)screen;Slot slot=((AbstractContainerScreenAccessor)(Object)unified).ddsTweak$getHoveredSlot();return slot==null?-1:unified.getMenu().slots.indexOf(slot);}
    private static List<Integer> sortIndices(UnifiedPlayerInventoryMirrorMenu menu){
        int hovered=hoveredMenuSlot();
        // Preserve IPN's no-hover default: sort the fake main or Ender storage.
        if(hovered<0)hovered=menu.targetMainStart();
        int[] range=PlayerInventorySlotPolicy.targetSortableRange(menu,hovered);
        List<Integer> ids=new ArrayList<>();
        if(range!=null)for(int id=range[0];id<range[1];id++)ids.add(id);
        return ids;
    }
    private static List<Integer> storageIndices(UnifiedPlayerInventoryMirrorMenu menu){
        return PlayerInventorySlotPolicy.targetStorageSlots(menu);
    }
    private static Object fixedArea(List<?> vanillaSlots,boolean sortable){try{UnifiedPlayerInventoryMirrorMenu menu=menu();if(menu==null)throw new IllegalStateException("unified menu missing");Class<?> itemArea=Class.forName(ITEM_AREA);Object companion=itemArea.getField("Companion").get(null);List<Integer> indices=sortable?sortIndices(menu):storageIndices(menu);for(Method m:companion.getClass().getMethods()){if(!m.getName().equals("invoke")||m.getParameterCount()!=3||m.getReturnType()!=itemArea)continue;try{return m.invoke(companion,vanillaSlots,indices,false);}catch(IllegalArgumentException ignored){}}}catch(ReflectiveOperationException|LinkageError|RuntimeException e){broken=true;throw new IllegalStateException("DDS Tweak IPN Level 1 adapter failed closed",e);}broken=true;throw new IllegalStateException("DDS Tweak IPN Level 1 adapter failed closed");}
    private static UnifiedPlayerInventoryMirrorMenu menu(){Object screen=MinecraftCompat.screen(Minecraft.getInstance());return screen instanceof UnifiedPlayerInventoryScreen?((UnifiedPlayerInventoryScreen)screen).getMenu():null;}
}

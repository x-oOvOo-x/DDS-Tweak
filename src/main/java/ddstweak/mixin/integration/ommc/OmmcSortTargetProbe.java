package ddstweak.mixin.integration.ommc;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads optional OMMC jar bytecode without ever loading OMMC classes.
 * 只检查可选 OMMC 字节码，不主动加载其类。
 */
public final class OmmcSortTargetProbe {
    private OmmcSortTargetProbe(){}
    public static final String LEGACY="com.plusls.ommc.feature.sortInventory.SortInventoryUtil";
    public static final String MODERN="com.plusls.ommc.impl.feature.sortInventory.SortInventoryHelper";

    public static boolean shouldApply(String targetName,boolean fallback,boolean tupleAdapter){
        FabricLoader loader=FabricLoader.getInstance();
        String minecraftVersion=loader.getModContainer("minecraft")
                .map(m->m.getMetadata().getVersion().getFriendlyString()).orElse("");
        OmmcSortVersionMatrix.Status versionStatus=OmmcSortVersionMatrix.status(minecraftVersion);
        String file=targetName.replace('.','/')+".class";
        for(ModContainer mod:loader.getAllMods()){
            String id=mod.getMetadata().getId();
            if(!id.equals("ommc")&&!id.startsWith("ommc-"))continue;
            Optional<Path> path=mod.findPath(file);
            if(!path.isPresent())continue;
            try(InputStream input=Files.newInputStream(path.get())){
                ClassNode node=new ClassNode();
                new ClassReader(input).accept(node,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
                boolean sort=false,rangeVanilla=false,rangeCustom=false,clicks=false;
                List<String> sorterMethods=new ArrayList<>();
                for(MethodNode method:node.methods){
                    if(method.name.equals("sort")||method.name.equals("getSortRange")
                            ||method.name.equals("doClick"))
                        sorterMethods.add(method.name+method.desc);
                    if((method.access & Opcodes.ACC_STATIC)==0)continue;
                    Type[] args=Type.getArgumentTypes(method.desc);
                    Type result=Type.getReturnType(method.desc);
                    if(method.name.equals("sort")&&args.length==0
                            &&result.equals(Type.getType(Runnable.class)))sort=true;
                    if(method.name.equals("getSortRange")&&args.length==2
                            &&is(args[0],"net.minecraft.world.inventory.AbstractContainerMenu")
                            &&is(args[1],"net.minecraft.world.inventory.Slot")
                            &&is(result,"net.minecraft.util.Tuple"))rangeVanilla=true;
                    if(method.name.equals("getSortRange")&&args.length==2
                            &&is(args[0],"net.minecraft.world.inventory.AbstractContainerMenu")
                            &&is(args[1],"net.minecraft.world.inventory.Slot")
                            &&is(result,"com.plusls.ommc.util.Tuple"))rangeCustom=true;
                    if(method.name.equals("doClick")&&args.length==5
                            &&is(args[0],"net.minecraft.world.entity.player.Player")
                            &&args[1].equals(Type.INT_TYPE)
                            &&is(args[2],"net.minecraft.client.multiplayer.MultiPlayerGameMode")
                            &&args[3].equals(Type.getType(java.util.List.class))
                            &&args[4].equals(Type.getType(java.util.List.class))
                            &&result.equals(Type.VOID_TYPE))clicks=true;
                }
                boolean permitted=OmmcSortVersionMatrix.canSort(minecraftVersion);
                boolean direct=permitted&&sort&&rangeVanilla&&clicks&&supportsTuple();
                boolean adapted=permitted&&sort&&rangeCustom&&clicks
                        &&mod.findPath("com/plusls/ommc/util/Tuple.class").isPresent();
                boolean full=direct||adapted;
                boolean enable=fallback?sort&&!full:tupleAdapter?adapted:direct;
                if(enable&&!fallback)org.apache.logging.log4j.LogManager.getLogger("ddstweak.ommc")
                        .info("OMMC sorter compatible: target={}, mod={}",targetName,id);
                if(fallback&&!full)org.apache.logging.log4j.LogManager.getLogger("ddstweak.ommc")
                        .warn("OMMC sorter unsupported: target={}, mod={}, mc={}, policy={}, sort={}, range={}, clicks={}, tuple={}, methods={}; DT mirror sorting {}",
                                targetName,id,minecraftVersion,versionStatus,sort,(rangeVanilla||rangeCustom),clicks,
                                supportsTuple(),sorterMethods,enable?"disabled":"not intercepted");
                return enable;
            }catch(Exception exception){
                org.apache.logging.log4j.LogManager.getLogger("ddstweak.ommc")
                        .warn("OMMC sorter bytecode unreadable; skipping optional injection: {}",targetName,exception);
                return false;
            }
        }
        return false;
    }

    static boolean is(Type actual,String named){
        if(actual.getSort()!=Type.OBJECT)return false;
        String actualName=actual.getClassName();
        if(actualName.equals(named))return true;
        // OMMC release JARs use Fabric intermediary class names, whereas DT is
        // compiled against Mojang names. Fabric's "named" resolver may refer
        // to Yarn names instead, so mapping Mojang names through it is not reliable.
        String intermediary=intermediaryName(named);
        if(intermediary!=null&&actualName.equals(intermediary))return true;
        try{
            String runtime=FabricLoader.getInstance().getMappingResolver().mapClassName("named",named);
            return actualName.equals(runtime);
        }catch(RuntimeException ignored){
            return false;
        }
    }

    /** Stable pre-26.1 Fabric intermediary IDs for the five OMMC sorter types. */
    static String intermediaryName(String named){
        switch(named){
            case "net.minecraft.world.inventory.AbstractContainerMenu":return "net.minecraft.class_1703";
            case "net.minecraft.world.inventory.Slot":return "net.minecraft.class_1735";
            case "net.minecraft.util.Tuple":return "net.minecraft.class_3545";
            case "net.minecraft.world.entity.player.Player":return "net.minecraft.class_1657";
            case "net.minecraft.client.multiplayer.MultiPlayerGameMode":return "net.minecraft.class_636";
            default:return null;
        }
    }
    private static boolean supportsTuple(){
        //#if MC < 260200
        return true;
        //#else
        //$$ return false;
        //#endif
    }
}

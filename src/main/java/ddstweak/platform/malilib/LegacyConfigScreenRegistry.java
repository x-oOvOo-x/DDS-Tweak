package ddstweak.platform.malilib;

import ddstweak.DDSTweakReference;
import ddstweak.api.config.*;
import ddstweak.gui.DDSTweakConfigScreen;
import fi.dy.masa.malilib.gui.GuiBase;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

public final class LegacyConfigScreenRegistry {
    private static final Logger LOGGER=LogManager.getLogger("DDS Tweak/LegacyConfigSelector");
    private static final List<Entry> ENTRIES=new ArrayList<>();
    private static boolean initialized;
    private LegacyConfigScreenRegistry(){}

    public static synchronized void initialize(){
        if(initialized)return;initialized=true;
        register(new Entry("malilib","MaLiLib",reflectiveFactory("fi.dy.masa.malilib.MaLiLibConfigGui")));
        register(new Entry(DDSTweakReference.MOD_ID,DDSTweakReference.MOD_NAME,DDSTweakConfigScreen::new));
        loadEntrypoints();
        registerOptional("litematica","fi.dy.masa.litematica.gui.GuiConfigs");
        registerOptional("minihud","fi.dy.masa.minihud.gui.GuiConfigs");
        registerOptional("tweakeroo","fi.dy.masa.tweakeroo.gui.GuiConfigs");
        registerOptional("itemscroller","fi.dy.masa.itemscroller.gui.GuiConfigs");
    }

    public static List<Entry> entries(){return Collections.unmodifiableList(new ArrayList<Entry>(ENTRIES));}
    public static Optional<Entry> find(String modId){return ENTRIES.stream().filter(e->e.modId().equals(modId)).findFirst();}

    public static boolean open(Entry entry,Screen parent){
        try{
            Screen next=entry.factory().get();if(next==null)return false;
            if(next instanceof GuiBase)((GuiBase)next).setParent(parent);
            GuiBase.openGui(next);return true;
        }catch(RuntimeException|LinkageError e){LOGGER.warn("Failed to open legacy config screen for {}",entry.modId(),e);return false;}
    }

    private static void loadEntrypoints(){
        List<EntrypointContainer<LegacyConfigScreenProvider>> providers;
        try{providers=FabricLoader.getInstance().getEntrypointContainers(LegacyConfigScreenProvider.ENTRYPOINT,LegacyConfigScreenProvider.class);}
        catch(RuntimeException|LinkageError e){LOGGER.warn("Failed to enumerate legacy config-screen providers",e);return;}
        for(EntrypointContainer<LegacyConfigScreenProvider> container:providers){
            String modId=container.getProvider().getMetadata().getId(),name=container.getProvider().getMetadata().getName();
            try{container.getEntrypoint().register(new Registrar(modId,name));}
            catch(RuntimeException|LinkageError e){LOGGER.warn("Ignoring broken legacy config-screen provider from {}",modId,e);}
        }
    }

    private static void register(Entry entry){if(!find(entry.modId()).isPresent())ENTRIES.add(entry);}
    private static void registerFromProvider(String modId,String name,Supplier<? extends Screen> factory){
        if(!FabricLoader.getInstance().isModLoaded(modId)){LOGGER.warn("Ignoring config-screen registration from unloaded mod {}",modId);return;}
        if(find(modId).isPresent()){LOGGER.warn("Ignoring duplicate legacy config-screen registration for {}",modId);return;}
        register(new Entry(modId,name,factory));
    }

    private static void registerOptional(String modId,String className){
        if(!FabricLoader.getInstance().isModLoaded(modId)||find(modId).isPresent())return;
        try{register(new Entry(modId,FabricLoader.getInstance().getModContainer(modId).map(c->c.getMetadata().getName()).orElse(modId),reflectiveFactory(className)));}
        catch(RuntimeException|LinkageError e){LOGGER.warn("Skipping legacy config selector adapter for {}",modId,e);}
    }

    private static Supplier<? extends Screen> reflectiveFactory(String className){
        try{
            Class<?> type=Class.forName(className,false,LegacyConfigScreenRegistry.class.getClassLoader());
            if(!Screen.class.isAssignableFrom(type))throw new ReflectiveOperationException(className+" is not a Screen");
            Constructor<?> ctor=type.getDeclaredConstructor();
            return ()->{try{return (Screen)ctor.newInstance();}catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot create "+className,e);}};
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot bind "+className,e);}
    }

    private static final class Registrar implements LegacyConfigScreenRegistrar{
        private final String modId,defaultName;
        private Registrar(String modId,String defaultName){this.modId=modId;this.defaultName=defaultName;}
        @Override public String modId(){return modId;}
        @Override public void register(Supplier<? extends Screen> factory){register(defaultName,factory);}
        @Override public void register(String displayName,Supplier<? extends Screen> factory){
            String name=displayName==null||displayName.trim().isEmpty()?defaultName:displayName;
            registerFromProvider(modId,name,Objects.requireNonNull(factory,"factory"));
        }
    }

    public static final class Entry{
        private final String modId,displayName;private final Supplier<? extends Screen> factory;
        public Entry(String modId,String displayName,Supplier<? extends Screen> factory){
            this.modId=Objects.requireNonNull(modId,"modId");this.displayName=Objects.requireNonNull(displayName,"displayName");this.factory=Objects.requireNonNull(factory,"factory");
        }
        public String modId(){return modId;}public String displayName(){return displayName;}public Supplier<? extends Screen> factory(){return factory;}
        @Override public String toString(){return displayName;}
        @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof Entry))return false;Entry other=(Entry)o;return modId.equals(other.modId)&&displayName.equals(other.displayName)&&factory.equals(other.factory);}
        @Override public int hashCode(){return Objects.hash(modId,displayName,factory);}
    }
}

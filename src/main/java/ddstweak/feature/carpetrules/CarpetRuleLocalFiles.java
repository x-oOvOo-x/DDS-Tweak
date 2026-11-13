package ddstweak.feature.carpetrules;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
//#if MC >= 260300
//$$ import java.nio.ByteBuffer;
//$$ import java.util.Set;
//$$ import java.util.concurrent.ConcurrentHashMap;
//$$ import org.lwjgl.sdl.SDLDialog;
//$$ import org.lwjgl.sdl.SDLError;
//$$ import org.lwjgl.sdl.SDL_DialogFileCallback;
//$$ import org.lwjgl.sdl.SDL_DialogFileFilter;
//$$ import org.lwjgl.system.MemoryUtil;
//#elseif MC >= 11500
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
//#else
//$$ import java.io.BufferedReader;
//$$ import java.io.InputStreamReader;
//$$ import java.nio.charset.StandardCharsets;
//$$ import java.util.Base64;
//#endif
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Client-only local file actions. Never accepts a file path from the server. */
final class CarpetRuleLocalFiles {
    private CarpetRuleLocalFiles(){}


    /** One selection request. Cancellation discards late native callbacks immediately. */
    static final class NativePicker {
        private final Consumer<Path> selected;
        private final Runnable unavailable;
        private final AtomicBoolean cancelled=new AtomicBoolean();
        private volatile Process process; // Used only by the 1.14.x Windows compatibility backend.

        NativePicker(Consumer<Path> selected,Runnable unavailable){
            this.selected=selected;
            this.unavailable=unavailable;
        }
        void cancel(){
            cancelled.set(true);
            Process launched=process;
            if(launched!=null)launched.destroyForcibly();
        }
        void attach(Process launched){
            process=launched;
            if(isCancelled())launched.destroyForcibly();
        }
        boolean isCancelled(){return cancelled.get();}
        void select(Path path){if(!isCancelled())selected.accept(path);}
        void fail(){if(!isCancelled())unavailable.run();}
    }

    /** Native OS picker first. Only an API/launch failure invokes the in-game fallback. */
    static NativePicker chooseNativeJson(Path directory,Consumer<Path> selected,Runnable unavailable){
        NativePicker picker=new NativePicker(selected,unavailable);
        if(directory==null){picker.fail();return picker;}
        startNativeDialog(picker,directory);
        return picker;
    }

    //#if MC >= 260300
    //$$ private static final Set<SDL_DialogFileCallback> SDL_PENDING=ConcurrentHashMap.newKeySet();
    //$$
    //$$     private static final class SdlChoice {
    //$$         final String path;
    //$$         final boolean failed;
    //$$         SdlChoice(String path,boolean failed){this.path=path;this.failed=failed;}
    //$$     }
    //$$
    //$$     /** SDL3's native dialog is asynchronous and must be opened on the SDL main thread. */
    //$$     private static void startNativeDialog(NativePicker picker,Path directory){
    //$$         ByteBuffer label=null,extension=null,initial=null;
    //$$         long filterMemory=0;
    //$$         SDL_DialogFileCallback callback=null;
    //$$         try{
    //$$             label=MemoryUtil.memUTF8("JSON files (*.json)");
    //$$             extension=MemoryUtil.memUTF8("json"); // SDL3 expects bare extensions, not *.json.
    //$$             filterMemory=MemoryUtil.nmemAlloc(SDL_DialogFileFilter.SIZEOF);
    //$$             SDL_DialogFileFilter.create(filterMemory).set(label,extension);
    //$$             SDL_DialogFileFilter.Buffer filter=SDL_DialogFileFilter.create(filterMemory,1);
    //$$             initial=MemoryUtil.memUTF8(directory.toAbsolutePath().toString());
    //$$             CompletableFuture<SdlChoice> completed=new CompletableFuture<>();
    //$$             callback=SDL_DialogFileCallback.create((userdata,fileList,selectedFilter)->{
    //$$                 String result=null;
    //$$                 boolean failed=false;
    //$$                 try{
    //$$                     if(fileList!=MemoryUtil.NULL){
    //$$                         long first=MemoryUtil.memGetAddress(fileList);
    //$$                         if(first!=MemoryUtil.NULL)result=MemoryUtil.memUTF8(first);
    //$$                     }else{
    //$$                         // SDL3: null file list means API failure; a list containing null means Cancel.
    //$$                         failed=true;
    //$$                     }
    //$$                 }catch(RuntimeException|LinkageError invalid){
    //$$                     failed=true;
    //$$                 }
    //$$                 completed.complete(new SdlChoice(result,failed));
    //$$             });
    //$$             SDL_PENDING.add(callback);
    //$$             SDLError.SDL_ClearError();
    //$$             SDLDialog.SDL_ShowOpenFileDialog(callback,MemoryUtil.NULL,
    //$$                     net.minecraft.client.Minecraft.getInstance().getWindow().handle(),
    //$$                     filter,initial,false);
    //$$             // Register after SDL_ShowOpenFileDialog returns, even if the callback fired inline.
    //$$             final SDL_DialogFileCallback ownedCallback=callback;
    //$$             final ByteBuffer ownedLabel=label,ownedExtension=extension,ownedInitial=initial;
    //$$             final long ownedFilter=filterMemory;
    //$$             // SDL may call back on any thread. Validate files off the game thread,
    //$$             // and defer native closure cleanup beyond completion notification.
    //$$             completed.thenAcceptAsync(choice->{
    //$$                 Path validated=null;
    //$$                 boolean failed=choice.failed;
    //$$                 if(!failed&&choice.path!=null){
    //$$                     try{
    //$$                         Path file=java.nio.file.Paths.get(choice.path);
    //$$                         if(Files.isRegularFile(file))validated=file;
    //$$                         else failed=true;
    //$$                     }catch(RuntimeException invalid){failed=true;}
    //$$                 }
    //$$                 final Path selected=validated;
    //$$                 final boolean unavailable=failed;
    //$$                 net.minecraft.client.Minecraft.getInstance().execute(()->{
    //$$                     try{
    //$$                         if(unavailable)picker.fail();
    //$$                         else picker.select(selected);
    //$$                     }finally{
    //$$                         SDL_PENDING.remove(ownedCallback);
    //$$                         ownedCallback.free();
    //$$                         MemoryUtil.nmemFree(ownedFilter);
    //$$                         MemoryUtil.memFree(ownedLabel);
    //$$                         MemoryUtil.memFree(ownedExtension);
    //$$                         MemoryUtil.memFree(ownedInitial);
    //$$                     }
    //$$                 });
    //$$             });
    //$$         }catch(RuntimeException|LinkageError failed){
    //$$             if(callback!=null){
    //$$                 SDL_PENDING.remove(callback);
    //$$                 callback.free();
    //$$             }
    //$$             if(filterMemory!=0)MemoryUtil.nmemFree(filterMemory);
    //$$             if(label!=null)MemoryUtil.memFree(label);
    //$$             if(extension!=null)MemoryUtil.memFree(extension);
    //$$             if(initial!=null)MemoryUtil.memFree(initial);
    //$$             picker.fail();
    //$$         }
    //$$     }
    //#elseif MC >= 11500
    /** TinyFD is bundled from Minecraft 1.15.2 through 26.2. */
    private static void startNativeDialog(NativePicker picker,Path directory){
        Thread worker=new Thread(()->{
            if(picker.isCancelled())return;
            try(MemoryStack stack=MemoryStack.stackPush()){
                PointerBuffer filters=stack.mallocPointer(1);
                filters.put(stack.UTF8("*.json")).flip();
                String chosen=TinyFileDialogs.tinyfd_openFileDialog(
                        "Import DDS Tweak plans",directory.toAbsolutePath().toString(),
                        filters,"JSON files (*.json)",false);
                if(chosen==null)picker.select(null); // User cancellation is not a failure.
                else{
                    Path file=java.nio.file.Paths.get(chosen);
                    if(Files.isRegularFile(file))picker.select(file);
                    else picker.fail();
                }
            }catch(RuntimeException|LinkageError failure){
                picker.fail();
            }
        },"dds-tweak-native-json-picker");
        worker.setDaemon(true);
        worker.start();
    }
    //#else
    //$$ /** Oldest 1.14.x targets lack TinyFD; use Windows Forms without dynamic C# compilation. */
    //$$     private static final String WINDOWS_DIALOG=
    //$$             "$ErrorActionPreference='Stop';"
    //$$             +"Add-Type -AssemblyName System.Windows.Forms;"
    //$$             +"[Console]::OutputEncoding=[Text.Encoding]::UTF8;"
    //$$             +"$d=New-Object System.Windows.Forms.OpenFileDialog;"
    //$$             +"$d.Filter='JSON files (*.json)|*.json';"
    //$$             +"$d.CheckFileExists=$true;$d.Multiselect=$false;"
    //$$             +"$p=$env:DDS_TWEAK_PICKER_START;"
    //$$             +"if([IO.Directory]::Exists($p)){$d.InitialDirectory=$p}"
    //$$             +"elseif([IO.File]::Exists($p)){$d.InitialDirectory=[IO.Path]::GetDirectoryName($p)};"
    //$$             +"$owner=New-Object System.Windows.Forms.Form;"
    //$$             +"$owner.TopMost=$true;$owner.ShowInTaskbar=$false;$null=$owner.Handle;"
    //$$             +"try{if($d.ShowDialog($owner) -eq [System.Windows.Forms.DialogResult]::OK){"
    //$$             +"[Console]::WriteLine('DDS_SELECTED:'+$d.FileName)}}"
    //$$             +"finally{$d.Dispose();$owner.Dispose()}";
    //$$ 
    //$$     private static void startNativeDialog(NativePicker picker,Path directory){
    //$$         if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).startsWith("windows")){
    //$$             picker.fail();
    //$$             return;
    //$$         }
    //$$         Thread worker=new Thread(()->{
    //$$             if(picker.isCancelled())return;
    //$$             String encoded=Base64.getEncoder().encodeToString(
    //$$                     WINDOWS_DIALOG.getBytes(StandardCharsets.UTF_16LE));
    //$$             List<String> command=Arrays.asList("powershell.exe","-NoProfile",
    //$$                     "-NonInteractive","-STA","-EncodedCommand",encoded);
    //$$             try{
    //$$                 ProcessBuilder launch=new ProcessBuilder(command).redirectErrorStream(true);
    //$$                 launch.environment().put("DDS_TWEAK_PICKER_START",
    //$$                         directory.toAbsolutePath().toString());
    //$$                 if(picker.isCancelled())return;
    //$$                 Process process=launch.start();
    //$$                 picker.attach(process);
    //$$                 if(picker.isCancelled())return;
    //$$                 String chosen=null,line;
    //$$                 try(BufferedReader reader=new BufferedReader(new InputStreamReader(
    //$$                         process.getInputStream(),StandardCharsets.UTF_8))){
    //$$                     while((line=reader.readLine())!=null){
    //$$                         if(picker.isCancelled())return;
    //$$                         if(line.startsWith("DDS_SELECTED:"))chosen=line.substring(13);
    //$$                     }
    //$$                 }
    //$$                 if(picker.isCancelled())return;
    //$$                 if(process.waitFor()!=0){picker.fail();return;}
    //$$                 if(chosen==null){picker.select(null);return;}
    //$$                 Path file=java.nio.file.Paths.get(chosen);
    //$$                 if(Files.isRegularFile(file))picker.select(file);
    //$$                 else picker.fail();
    //$$             }catch(InterruptedException interrupted){
    //$$                 Thread.currentThread().interrupt();
    //$$                 picker.fail();
    //$$             }catch(IOException|RuntimeException failed){
    //$$                 picker.fail();
    //$$             }
    //$$         },"dds-tweak-legacy-json-picker");
    //$$         worker.setDaemon(true);
    //$$         worker.start();
    //$$     }
    //#endif

    /** In-game JSON browser; never depends on AWT, Swing or desktop file associations. */
    static Path browseDirectory(Path suggested,Path fallback){
        Path[] candidates={suggested,fallback};
        for(Path candidate:candidates){
            if(candidate==null)continue;
            try{
                Path absolute=candidate.toAbsolutePath().normalize();
                if(Files.isDirectory(absolute))return absolute;
                if(absolute.getParent()!=null&&Files.isDirectory(absolute.getParent()))
                    return absolute.getParent();
            }catch(SecurityException ignored){
                // Try the other local directory.
            }
        }
        return java.nio.file.Paths.get(".").toAbsolutePath().normalize();
    }

    /** Only local directories and JSON files are offered; path typing remains supported. */
    static List<Path> jsonEntries(Path directory){
        if(directory==null||!Files.isDirectory(directory))return Collections.emptyList();
        try(Stream<Path> stream=Files.list(directory)){
            return stream.filter(path->Files.isDirectory(path)||
                            Files.isRegularFile(path)&&path.getFileName().toString()
                                    .toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing((Path path)->!Files.isDirectory(path))
                            .thenComparing(path->path.getFileName().toString(),
                                    String.CASE_INSENSITIVE_ORDER))
                    .limit(512).collect(Collectors.toList());
        }catch(IOException|SecurityException ex){
            return Collections.emptyList();
        }
    }

    /** Avoid passing user paths through a shell; Windows can open JSON in Notepad without associations. */
    static List<String> fallbackEditorCommand(Path file,String osName){
        if(file==null||osName==null||!osName.toLowerCase(Locale.ROOT).startsWith("windows"))
            return Collections.emptyList();
        return Arrays.asList("notepad.exe",file.toAbsolutePath().toString());
    }

    /** Try the OS association first, then Notepad on Windows when JSON has no association. */
    static void open(Path file,Consumer<Boolean> completed){
        Thread worker=new Thread(()->{
            boolean ok=false;
            if(file!=null&&Files.isRegularFile(file)){
                try{
                    if(Desktop.isDesktopSupported()
                            &&Desktop.getDesktop().isSupported(Desktop.Action.OPEN)){
                        Desktop.getDesktop().open(file.toFile());
                        ok=true;
                    }
                }catch(Exception|LinkageError ignored){
                    // Missing desktop support or missing JSON file association.
                }
                if(!ok){
                    List<String> command=fallbackEditorCommand(file,System.getProperty("os.name",""));
                    if(!command.isEmpty()){
                        try{
                            new ProcessBuilder(command).start();
                            ok=true;
                        }catch(Exception|LinkageError ignored){
                            // Neither the OS-associated viewer nor the local editor could launch.
                        }
                    }
                }
            }
            completed.accept(ok);
        },"dds-tweak-preset-file-open");
        worker.setDaemon(true);
        worker.start();
    }
    /** Open the folder containing one or more named exports, not a stale JSON file. */
    static List<String> fallbackDirectoryCommand(Path directory,String osName){
        if(directory==null||osName==null||!osName.toLowerCase(Locale.ROOT).startsWith("windows"))
            return Collections.emptyList();
        return Arrays.asList("explorer.exe",directory.toAbsolutePath().toString());
    }

    static void openDirectory(Path directory,Consumer<Boolean> completed){
        Thread worker=new Thread(()->{
            boolean ok=false;
            if(directory!=null&&Files.isDirectory(directory)){
                try{
                    if(Desktop.isDesktopSupported()
                            &&Desktop.getDesktop().isSupported(Desktop.Action.OPEN)){
                        Desktop.getDesktop().open(directory.toFile());
                        ok=true;
                    }
                }catch(Exception|LinkageError ignored){
                    // Desktop may not support opening directories.
                }
                if(!ok){
                    List<String> command=fallbackDirectoryCommand(
                            directory,System.getProperty("os.name",""));
                    if(!command.isEmpty()){
                        try{
                            new ProcessBuilder(command).start();
                            ok=true;
                        }catch(Exception|LinkageError ignored){
                            // No local file browser could be launched.
                        }
                    }
                }
            }
            completed.accept(ok);
        },"dds-tweak-preset-folder-open");
        worker.setDaemon(true);
        worker.start();
    }
}

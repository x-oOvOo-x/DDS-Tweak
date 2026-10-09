package ddstweak.integration.dds;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsFeaturePacketContractTest {
    private static Path root(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root not found");}
    private static String read(String p)throws IOException{return Files.readString(root().resolve(p));}
    @Test void genericNetworkContainsNoQcaUndoBusinessIds()throws IOException{String protocol=read("src/main/java/ddstweak/network/DdsProtocol.java"),network=read("src/main/java/ddstweak/network/DdsClientNetwork.java");for(String id:new String[]{"qca_open_slot_c2s","qca_storage_click_c2s","undo_c2s","redo_c2s"}){assertFalse(protocol.contains(id),id);assertFalse(network.contains(id),id);}}
    @Test void qcaUndoStayIndependentFromLocalDdsClasses()throws IOException{for(String dir:new String[]{"src/main/java/ddstweak/integration/dds/qca","src/main/java/ddstweak/integration/dds/undoredo"})try(var paths=Files.walk(root().resolve(dir))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->{try{assertFalse(Files.readString(p).contains("import carpetddsaddition."),p.toString());}catch(IOException e){throw new RuntimeException(e);}});}}
}

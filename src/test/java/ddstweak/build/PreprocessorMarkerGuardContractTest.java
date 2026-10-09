package ddstweak.build;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class PreprocessorMarkerGuardContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p)throws Exception{return Files.readString(ROOT.resolve(p));}
    private static boolean bad(String s){String t=s.stripLeading();return t.startsWith("//$")&&!t.startsWith("//$$");}

    @Test void buildVerifierRejectsSingleDollarMarker()throws Exception{
        String b=read("build.gradle");
        assertTrue(b.contains("stripped.startsWith('//$') && !stripped.startsWith('//$$')"));
        assertTrue(b.contains("Invalid single-dollar ReplayMod preprocess marker"));
    }

    @Test void trackedJavaSourcesContainNoSingleDollarMarkers()throws Exception{
        for(String root:new String[]{"src","versions"}){
            Path dir=ROOT.resolve(root);if(!Files.exists(dir))continue;
            try(var paths=Files.walk(dir)){
                for(Path p:paths.filter(x->x.toString().endsWith(".java")&&!x.toString().contains(FileSystems.getDefault().getSeparator()+"build"+FileSystems.getDefault().getSeparator())).toList()){
                    int line=0;for(String s:Files.readAllLines(p)){line++;assertFalse(bad(s),ROOT.relativize(p)+":"+line);}
                }
            }
        }
    }
}

package ddstweak.mixin.integration.ommc;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks all 21 DT development versions to the audited upstream-source policy.
 * A SOURCE_BACKED result still needs the actual installed OMMC method probe.
 */
final class OmmcSortVersionMatrixTest {
    @Test void allDevelopmentVersionsHaveAnExplicitPolicy(){
        Map<String,OmmcSortVersionMatrix.Status> expected=new LinkedHashMap<>();
        for(String version:new String[]{
                "1.14.4","1.15.2","1.16.5","1.17.1","1.18.2","1.19.4",
                "1.20.1","1.20.6","1.21.1","1.21.3","1.21.4",
                "1.21.5","1.21.8","1.21.10","1.21.11"
        })expected.put(version,OmmcSortVersionMatrix.Status.SOURCE_BACKED);
        for(String version:new String[]{"1.20.2","1.20.4","1.21.9"})
            expected.put(version,OmmcSortVersionMatrix.Status.NO_MATCHING_UPSTREAM_BUILD);
        for(String version:new String[]{"26.1.2","26.2","26.3"})
            expected.put(version,OmmcSortVersionMatrix.Status.ADAPTER_REQUIRED);

        assertEquals(21,expected.size());
        for(Map.Entry<String,OmmcSortVersionMatrix.Status> entry:expected.entrySet()){
            String version=entry.getKey();
            assertEquals(entry.getValue(),OmmcSortVersionMatrix.status(version),version);
            assertEquals(entry.getValue()==OmmcSortVersionMatrix.Status.SOURCE_BACKED
                            ||entry.getValue()==OmmcSortVersionMatrix.Status.ADAPTER_REQUIRED,
                    OmmcSortVersionMatrix.canSort(version),version);
        }
    }

    @Test void unknownVersionsNeverEnableSorting(){
        assertEquals(OmmcSortVersionMatrix.Status.UNKNOWN,OmmcSortVersionMatrix.status(null));
        assertFalse(OmmcSortVersionMatrix.canSort("1.22.0"));
        assertFalse(OmmcSortVersionMatrix.canSort("26.4"));
    }
}

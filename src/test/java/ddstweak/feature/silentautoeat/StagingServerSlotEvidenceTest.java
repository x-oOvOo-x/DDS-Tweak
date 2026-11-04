package ddstweak.feature.silentautoeat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real Minecraft registries are not initialized by the plain JUnit runner. */
final class StagingServerSlotEvidenceTest {
    private static StagingServerSlotEvidence<String> evidence(){
        return new StagingServerSlotEvidence<>(String::equals,UnaryOperator.identity());
    }

    @Test void requiresBothExactSlotsFromSameMenu(){
        StagingServerSlotEvidence<String> e=evidence();
        e.arm(0,13,37,"stone","bread");
        e.observe(1,13,"stone");
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.observe(0,13,"stone");
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.observe(0,38,"bread");
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.observe(0,37,"bread");
        assertTrue(e.pairedMatchingUpdatesSeen());
    }

    @Test void completeServerSnapshotCoversBothSlotsButNotClickIdentity(){
        StagingServerSlotEvidence<String> e=evidence();
        e.arm(0,13,37,"stone","bread");
        List<String> slots=new ArrayList<>();
        for(int i=0;i<38;i++)slots.add("empty");
        slots.set(13,"stone");
        slots.set(37,"bread");
        e.observeSnapshot(1,slots);
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.observeSnapshot(0,slots);
        assertTrue(e.pairedMatchingUpdatesSeen());
        // New reversal epoch never inherits an earlier matching packet.
        e.arm(0,13,37,"bread","stone");
        assertFalse(e.pairedMatchingUpdatesSeen());
        slots.set(13,"bread");
        slots.set(37,"stone");
        e.observeSnapshot(0,slots);
        assertTrue(e.pairedMatchingUpdatesSeen());
    }

    @Test void wrongContentsOrResetPreventPairedEvidence(){
        StagingServerSlotEvidence<String> e=evidence();
        e.arm(0,13,37,"stone","bread");
        e.observe(0,13,"dirt");
        e.observe(0,37,"bread");
        assertTrue(e.hasConflict());
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.clear();
        assertFalse(e.hasConflict());
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.arm(0,13,37,"stone","bread");
        assertFalse(e.pairedMatchingUpdatesSeen());
    }

    @Test void aServerCorrectionInvalidatesEarlierMatchingSlotsUntilRearmed(){
        StagingServerSlotEvidence<String> e=evidence();
        e.arm(0,13,37,"stone","bread");
        e.observe(0,13,"stone");
        e.observe(0,37,"bread");
        assertTrue(e.pairedMatchingUpdatesSeen());
        // A later correction must revoke the prior positive witness.
        e.observe(0,37,"pickaxe");
        assertTrue(e.hasConflict());
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.observe(0,37,"bread");
        assertFalse(e.pairedMatchingUpdatesSeen());
        // Reset only for a distinct transaction phase.
        e.clear();
        e.observe(0,37,"pickaxe");
        assertFalse(e.hasConflict());
        e.arm(0,13,37,"bread","stone");
        e.observe(0,13,"bread");
        e.observe(0,37,"stone");
        assertTrue(e.pairedMatchingUpdatesSeen());
    }

    @Test void consumptionMayChangeFoodAfterInitialLayoutWitnessIsRetired(){
        StagingServerSlotEvidence<String> e=evidence();
        e.arm(0,13,37,"pickaxe","bread x2");
        e.observe(0,13,"pickaxe");
        e.observe(0,37,"bread x2");
        assertTrue(e.pairedMatchingUpdatesSeen());
        // After the source is staged and before it is consumed, the initial
        // exact-count snapshot is no longer an applicable expectation.
        e.retainSourceDuringConsumption();
        e.observe(0,37,"bread x1");
        assertFalse(e.hasConflict());
        assertFalse(e.pairedMatchingUpdatesSeen());
        // Server source correction is still observable after a full snapshot.
        List<String> changed=new ArrayList<>();
        for(int i=0;i<38;i++)changed.add("empty");
        changed.set(13,"dirt");
        changed.set(37,"bread x1");
        e.observeSnapshot(0,changed);
        assertTrue(e.hasConflict());
        // Re-arm for the reverse SWAP with the consumed food count.
        e.arm(0,13,37,"bread x1","pickaxe");
        e.observe(0,13,"bread x1");
        e.observe(0,37,"pickaxe");
        assertTrue(e.pairedMatchingUpdatesSeen());
    }

    @Test void lateServerCorrectionAfterPredictedRestoreRevokesTheSettledLayout(){
        StagingServerSlotEvidence<String> e=evidence();
        // Real reverse SWAP was locally predicted to restore this layout.
        e.arm(0,13,37,"bread x3","pickaxe");
        e.observe(0,17,"dirt"); // Unrelated slot cannot revoke settlement.
        e.observe(1,13,"stale"); // Foreign container cannot revoke settlement.
        assertFalse(e.hasConflict());
        e.observe(0,13,"bread x3");
        e.observe(0,37,"pickaxe");
        assertTrue(e.pairedMatchingUpdatesSeen());
        // Server correction arrives after local restore looked complete.
        e.observe(0,37,"bread x3");
        assertTrue(e.hasConflict());
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.observe(0,37,"pickaxe");
        assertTrue(e.hasConflict()); // No self-clearing on a later match.
        e.clear();
        assertFalse(e.hasConflict());
    }

    @Test void lateFullContentCorrectionAlsoRevokesCompletedFoodRestore(){
        StagingServerSlotEvidence<String> e=evidence();
        e.arm(0,13,37,"empty","shield");
        List<String> slots=new ArrayList<>();
        for(int i=0;i<38;i++)slots.add("empty");
        slots.set(37,"shield");
        e.observeSnapshot(0,slots);
        assertTrue(e.pairedMatchingUpdatesSeen());
        slots.set(13,"bread x1");
        e.observeSnapshot(0,slots);
        assertTrue(e.hasConflict());
        assertFalse(e.pairedMatchingUpdatesSeen());
    }

    @Test void duplicateSlotsAndInvalidMenuCannotArm(){
        StagingServerSlotEvidence<String> e=evidence();
        e.arm(-1,13,37,"empty","empty");
        e.observe(-1,13,"empty");
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.arm(0,13,13,"empty","empty");
        e.observe(0,13,"empty");
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.arm(0,13,37,null,"bread");
        e.observe(0,13,"stone");
        e.observe(0,37,"bread");
        assertFalse(e.pairedMatchingUpdatesSeen());
    }

    @Test void missingAndNullServerSlotsNeverBecomePositiveEvidence(){
        StagingServerSlotEvidence<String> e=evidence();
        e.arm(0,13,37,"stone","bread");
        e.observe(0,13,null);
        e.observe(0,37,"bread");
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.observeSnapshot(0,java.util.Collections.singletonList("stone"));
        assertFalse(e.pairedMatchingUpdatesSeen());
        e.observe(0,13,"stone");
        assertTrue(e.pairedMatchingUpdatesSeen());
    }
}

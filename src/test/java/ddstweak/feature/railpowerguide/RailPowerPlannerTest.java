package ddstweak.feature.railpowerguide;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailPowerPlannerTest {
    @Test void emptyOrFullyPoweredNeedsNoTarget(){
        assertEquals(-1,RailPowerPlanner.nextTargetIndex(new boolean[0],true));
        boolean[] powered=new boolean[17];
        Arrays.fill(powered,true);
        assertEquals(-1,RailPowerPlanner.nextTargetIndex(powered,true));
    }

    @Test void unpoweredKnownSegmentsUseFarthestCoveringRail(){
        assertEquals(0,RailPowerPlanner.nextTargetIndex(new boolean[1],true));
        assertEquals(7,RailPowerPlanner.nextTargetIndex(new boolean[8],true));
        assertEquals(8,RailPowerPlanner.nextTargetIndex(new boolean[9],true));
        assertEquals(8,RailPowerPlanner.nextTargetIndex(new boolean[17],true));
        assertEquals(8,RailPowerPlanner.nextTargetIndex(new boolean[18],true));
    }

    @Test void canonicalThirtyFourRailBuildUsesTwoPowerPoints(){
        boolean[] powered=new boolean[34];
        assertEquals(8,RailPowerPlanner.nextTargetIndex(powered,true));

        Arrays.fill(powered,0,17,true);
        assertEquals(25,RailPowerPlanner.nextTargetIndex(powered,true));

        Arrays.fill(powered,17,34,true);
        assertEquals(-1,RailPowerPlanner.nextTargetIndex(powered,true));
    }

    @Test void nextTargetFollowsActualPoweredFront(){
        boolean[] powered=new boolean[44];
        Arrays.fill(powered,0,9,true);
        assertEquals(17,RailPowerPlanner.nextTargetIndex(powered,true));

        Arrays.fill(powered,9,26,true);
        assertEquals(34,RailPowerPlanner.nextTargetIndex(powered,true));
    }

    @Test void alreadyPoweredIdealRailMovesTargetBackToAnUnpoweredRail(){
        boolean[] powered=new boolean[35];
        Arrays.fill(powered,0,9,true);
        powered[17]=true;
        assertEquals(16,RailPowerPlanner.nextTargetIndex(powered,true));

        Arrays.fill(powered,17,35,true);
        assertEquals(16,RailPowerPlanner.nextTargetIndex(powered,true));
    }

    @Test void loadedBoundaryIsNotTreatedAsRealEnd(){
        assertEquals(-1,RailPowerPlanner.nextTargetIndex(new boolean[8],false));
        assertEquals(8,RailPowerPlanner.nextTargetIndex(new boolean[9],false));

        boolean[] partial=new boolean[9];
        partial[8]=true;
        assertEquals(7,RailPowerPlanner.nextTargetIndex(partial,false));
    }

    @Test void knownShortTailClampsToFarthestUnpoweredRail(){
        boolean[] powered=new boolean[22];
        Arrays.fill(powered,0,17,true);
        assertEquals(21,RailPowerPlanner.nextTargetIndex(powered,true));

        powered[21]=true;
        assertEquals(20,RailPowerPlanner.nextTargetIndex(powered,true));
    }

    @Test void sharedJunctionSourceWinsAcrossSegments(){
        int[][] coverage={
                {0,1,2},
                {3,4,5},
                {0,1,2,3,4,5}
        };
        int[] sharedSegments={1,1,2};
        double[] distance={4,4,9};
        assertArrayEquals(new int[]{2},RailPowerPlanner.selectSources(6,coverage,sharedSegments,distance,4));
    }

    @Test void equalCoveragePrefersNearbySource(){
        int[][] coverage={{0,1,2},{0,1,2}};
        int[] sharedSegments={1,1};
        double[] distance={25,4};
        assertArrayEquals(new int[]{1},RailPowerPlanner.selectSources(3,coverage,sharedSegments,distance,2));
    }

    @Test void redundantSourceIsRemovedAfterGreedySelection(){
        int[][] coverage={
                {0,1,2},
                {2,3,4},
                {0,1,2,3,4}
        };
        int[] sharedSegments={1,1,2};
        double[] distance={1,1,9};
        assertArrayEquals(new int[]{2},RailPowerPlanner.selectSources(5,coverage,sharedSegments,distance,3));
    }

    @Test void sourceLimitKeepsLocalPlanBounded(){
        int[][] coverage={{0},{1},{2}};
        int[] sharedSegments={1,1,1};
        double[] distance={1,2,3};
        assertArrayEquals(new int[]{0,1},RailPowerPlanner.selectSources(3,coverage,sharedSegments,distance,2));
    }
}

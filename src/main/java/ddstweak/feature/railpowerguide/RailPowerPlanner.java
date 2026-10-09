package ddstweak.feature.railpowerguide;

import java.util.ArrayList;
import java.util.List;

public final class RailPowerPlanner {
    public static final int POWER_PROPAGATION_RADIUS=8;

    private RailPowerPlanner(){}

    /**
     * Returns the next currently-unpowered rail that should receive direct power.
     * Kept as the single-line primitive used by regression tests and compatibility checks.
     */
    public static int nextTargetIndex(boolean[] powered,boolean endKnown){
        if(powered==null||powered.length==0)return -1;

        int first=-1;
        for(int i=0;i<powered.length;i++){
            if(!powered[i]){first=i;break;}
        }
        if(first<0)return -1;

        int ideal=first+POWER_PROPAGATION_RADIUS;
        if(ideal>=powered.length&&!endKnown)return -1;

        for(int i=Math.min(ideal,powered.length-1);i>=first;i--){
            if(!powered[i])return i;
        }
        return -1;
    }

    /**
     * Greedy local set-cover for nearby rail requirements.
     * Coverage gain is the primary objective; ties prefer candidates shared by more rail
     * segments and then the candidate closer to the player. A final redundancy pass drops
     * any selected source whose coverage is already provided by the remaining selection.
     */
    public static int[] selectSources(int requirementCount,int[][] coverage,int[] sharedSegmentCounts,double[] distanceSq,int limit){
        if(requirementCount<=0||coverage==null||coverage.length==0||limit<=0)return new int[0];

        boolean[] covered=new boolean[requirementCount];
        boolean[] used=new boolean[coverage.length];
        List<Integer> selected=new ArrayList<Integer>();
        int coveredCount=0;

        while(selected.size()<limit&&coveredCount<requirementCount){
            int best=-1,bestGain=0,bestShared=-1;
            double bestDistance=Double.POSITIVE_INFINITY;

            for(int i=0;i<coverage.length;i++){
                if(used[i])continue;
                int gain=uncoveredGain(coverage[i],covered);
                if(gain<=0)continue;
                int shared=sharedSegmentCounts!=null&&i<sharedSegmentCounts.length?sharedSegmentCounts[i]:1;
                double distance=distanceSq!=null&&i<distanceSq.length?distanceSq[i]:Double.POSITIVE_INFINITY;
                if(gain>bestGain||
                        gain==bestGain&&shared>bestShared||
                        gain==bestGain&&shared==bestShared&&distance<bestDistance){
                    best=i;
                    bestGain=gain;
                    bestShared=shared;
                    bestDistance=distance;
                }
            }

            if(best<0)break;
            used[best]=true;
            selected.add(best);
            coveredCount+=markCovered(coverage[best],covered);
        }

        pruneRedundant(selected,coverage,requirementCount);
        int[] result=new int[selected.size()];
        for(int i=0;i<result.length;i++)result[i]=selected.get(i);
        return result;
    }

    private static int uncoveredGain(int[] ids,boolean[] covered){
        if(ids==null)return 0;
        int gain=0;
        for(int id:ids)if(id>=0&&id<covered.length&&!covered[id])gain++;
        return gain;
    }

    private static int markCovered(int[] ids,boolean[] covered){
        if(ids==null)return 0;
        int added=0;
        for(int id:ids){
            if(id>=0&&id<covered.length&&!covered[id]){
                covered[id]=true;
                added++;
            }
        }
        return added;
    }

    private static void pruneRedundant(List<Integer> selected,int[][] coverage,int requirementCount){
        if(selected.size()<2)return;
        boolean[] baseline=new boolean[requirementCount];
        for(int index:selected)markCovered(coverage[index],baseline);

        for(int i=selected.size()-1;i>=0;i--){
            int removed=selected.remove(i);
            if(!coversBaseline(selected,coverage,baseline))selected.add(i,removed);
        }
    }

    private static boolean coversBaseline(List<Integer> selected,int[][] coverage,boolean[] baseline){
        boolean[] covered=new boolean[baseline.length];
        for(int index:selected)markCovered(coverage[index],covered);
        for(int i=0;i<baseline.length;i++)if(baseline[i]&&!covered[i])return false;
        return true;
    }
}

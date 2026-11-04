package ddstweak.feature.quickmute;

/** Original user volume, persisted separately from vanilla's temporary zero. */
final class QuickMuteVolumeState {
    private float saved=Float.NaN;

    void load(double volume){
        if(Double.isFinite(volume)&&volume>=0&&volume<=1)saved=(float)volume;
    }

    void capture(float current,boolean startup){
        if(!startup||!known()||current>0)load(current);
    }

    void prepareMute(float current,boolean startup,Runnable persist){
        capture(current,startup);
        persist.run();
    }

    float restore(float current){
        if(current>0)load(current);
        return known()?saved:current;
    }

    boolean known(){return !Float.isNaN(saved);}
    float saved(){return saved;}
}

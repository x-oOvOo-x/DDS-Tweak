package ddstweak.feature.cameraplayerselector;

import java.util.Locale;
import java.util.UUID;

final class CameraDestination {
    static final String DEFAULT_ICON_ID="minecraft:compass";
    private static final double MAX_COORDINATE=3.2E7D;
    private final String id,name,dimension,iconId;
    private final double x,y,z;

    CameraDestination(String id,String name,String dimension,double x,double y,double z,String iconId){
        this.id=id==null||id.trim().isEmpty()?UUID.randomUUID().toString():id.trim();
        this.name=name==null?"":name.trim();
        this.dimension=dimension==null?"":dimension.trim().toLowerCase(Locale.ROOT);
        this.x=x;this.y=y;this.z=z;
        this.iconId=iconId==null||iconId.trim().isEmpty()?DEFAULT_ICON_ID:iconId.trim().toLowerCase(Locale.ROOT);
    }

    String id(){return id;}
    String name(){return name;}
    String dimension(){return dimension;}
    double x(){return x;}
    double y(){return y;}
    double z(){return z;}
    String iconId(){return iconId;}

    boolean isValid(){
        return !name.isEmpty()
                &&validResourceId(dimension)
                &&Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)
                &&Math.abs(x)<=MAX_COORDINATE&&Math.abs(y)<=MAX_COORDINATE&&Math.abs(z)<=MAX_COORDINATE;
    }

    static boolean validResourceId(String value){
        return value!=null&&value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }
}

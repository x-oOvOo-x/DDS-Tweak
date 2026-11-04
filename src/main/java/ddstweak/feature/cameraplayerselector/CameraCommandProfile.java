package ddstweak.feature.cameraplayerselector;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Editable, per-server command templates; the server still owns authorization. */
final class CameraCommandProfile {
    enum Mode { TOGGLE, SEPARATE }
    enum Action {
        PLAYER_TP("playerTeleport", "/c tp {player}", "player"),
        PLAYER_FOLLOW("playerFollow", "/c follow {player}", "player"),
        PLAYER_DIRECT_TP("playerDirectTeleport", "/tp {player}", "player"),
        TOGGLE("toggleCommand", "/c"),
        ENTER("enterCommand", ""),
        EXIT("exitCommand", ""),
        BACK("backCommand", "/c back"),
        DIMENSION("dimensionTeleport", "/c tp dimension {dimension}", "dimension"),
        DESTINATION("destinationTeleport", "/c tp dimension {dimension} {x} {y} {z}", "x", "y", "z");

        final String key, defaultTemplate;
        final String[] required;
        Action(String key, String defaultTemplate, String... required) {
            this.key=key;
            this.defaultTemplate=defaultTemplate;
            this.required=required;
        }
    }

    private static final Pattern VARIABLE=Pattern.compile("\\{([^{}]+)\\}");
    private final Map<Action,String> templates=new EnumMap<>(Action.class);
    private final EnumSet<Action> overridden=EnumSet.noneOf(Action.class);
    private boolean modeOverridden;
    Mode mode=Mode.TOGGLE;

    CameraCommandProfile() {
        for (Action action:Action.values()) templates.put(action,action.defaultTemplate);
    }

    CameraCommandProfile copy() {
        CameraCommandProfile copy=new CameraCommandProfile();
        copy.mode=mode;
        copy.templates.putAll(templates);
        copy.overridden.addAll(overridden);
        copy.modeOverridden=modeOverridden;
        return copy;
    }

    String get(Action action){return templates.get(action);}
    void set(Action action,String template){
        templates.put(action,template==null?"":template.trim());
        overridden.add(action);
    }
    void setMode(Mode value){mode=value;modeOverridden=true;}
    boolean overrides(Action action){return overridden.contains(action);}
    boolean overridesMode(){return modeOverridden;}
    void clearAllOverrides(){overridden.clear();modeOverridden=false;}
    void restore(Action action,CameraCommandProfile global){
        templates.put(action,global.get(action));
        overridden.remove(action);
    }
    void restoreMode(CameraCommandProfile global){mode=global.mode;modeOverridden=false;}
    void inheritFrom(CameraCommandProfile global){
        for(Action action:Action.values())if(!overrides(action))templates.put(action,global.get(action));
        if(!modeOverridden)mode=global.mode;
    }

    static String validate(Action action,String template) {
        if (template==null || template.trim().isEmpty()) return null; // disabled
        String value=template.trim();
        if (value.startsWith("/")) value=value.substring(1);
        if (value.isEmpty() || value.charAt(0)=='/' || value.indexOf(';')>=0) return "Invalid command";
        for (int i=0;i<value.length();i++) if (Character.isISOControl(value.charAt(i))) return "Control character";
        if (!CameraCommandSyntax.validRoot(template.trim())) return "Invalid command root";
        Matcher matcher=VARIABLE.matcher(value);
        while (matcher.find()) {
            String name=matcher.group(1);
            if (!("player".equals(name)||"dimension".equals(name)||"x".equals(name)||"y".equals(name)||"z".equals(name)))
                return "Unknown variable: "+name;
        }
        if (value.replaceAll("\\{[^{}]+\\}","").indexOf('{')>=0 || value.replaceAll("\\{[^{}]+\\}","").indexOf('}')>=0)
            return "Malformed variable";
        for (String name:action.required) if (!value.contains("{"+name+"}")) return "Missing {"+name+"}";
        for (String name:new String[]{"player","dimension","x","y","z"}) {
            if (value.contains("{"+name+"}") && !allowed(action,name)) return "Unexpected {"+name+"}";
        }
        return null;
    }

    private static boolean allowed(Action action,String name) {
        if (action==Action.PLAYER_TP||action==Action.PLAYER_FOLLOW||action==Action.PLAYER_DIRECT_TP)
            return "player".equals(name);
        if (action==Action.DIMENSION) return "dimension".equals(name);
        if (action==Action.DESTINATION) return !"player".equals(name);
        return false;
    }

    String render(Action action,String player,String dimension,String x,String y,String z) {
        String template=get(action);
        if (validate(action,template)!=null || template==null || template.isEmpty()) return null;
        String command=template.startsWith("/")?template.substring(1):template;
        if (command.contains("{player}")) {
            if (player==null||player.trim().isEmpty()) return null;
            command=command.replace("{player}",StringArgumentType.escapeIfRequired(player));
        }
        if (command.contains("{dimension}")) {
            if (dimension==null||!CameraDestination.validResourceId(dimension)) return null;
            command=command.replace("{dimension}",dimension);
        }
        if (command.contains("{x}")||command.contains("{y}")||command.contains("{z}")) {
            if (x==null||y==null||z==null) return null;
            command=command.replace("{x}",x).replace("{y}",y).replace("{z}",z);
        }
        return command;
    }

    String render(Action action){return render(action,null,null,null,null,null);}
    String renderPlayer(Action action,String name){return render(action,name,null,null,null,null);}
    String renderDimension(Action action,String dimension){return render(action,null,dimension,null,null,null);}
    String renderDestination(CameraDestination dest){
        return render(Action.DESTINATION,null,dest.dimension(),CameraSelectorCompat.formatNumber(dest.x()),
                CameraSelectorCompat.formatNumber(dest.y()),CameraSelectorCompat.formatNumber(dest.z()));
    }
}
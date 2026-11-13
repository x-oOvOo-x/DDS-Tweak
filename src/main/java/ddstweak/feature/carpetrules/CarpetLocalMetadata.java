package ddstweak.feature.carpetrules;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Optional same-JVM Carpet metadata. Never adds server rules and never claims
 * a client-only rule is remotely supported. Uses reflection to keep Carpet
 * optional and to avoid a hard dependency on an extension's classes.
 */
public final class CarpetLocalMetadata {
    private CarpetLocalMetadata() {}

    public static CarpetRuleMetadata discover() {
        CarpetRuleMetadata result = new CarpetRuleMetadata();
        if (!FabricLoader.getInstance().isModLoaded("carpet")) return result;
        try {
            Class<?> server = Class.forName("carpet.CarpetServer");
            Field original = server.getField("settingsManager");
            Object extensions = server.getField("extensions").get(null);
            Map<String,String> translations=CarpetRuleTranslations.load(
                    CarpetRuleTranslations.language(),
                    extensions instanceof Iterable?(Iterable<?>)extensions:null);
            append(result,original.get(null),translations);
            if (extensions instanceof Iterable) {
                for (Object extension : (Iterable<?>)extensions) {
                    try {
                        // Use the public Carpet API so package-private extension classes work too.
                        Class<?> api=Class.forName("carpet.CarpetExtension");
                        Object manager=null;
                        for(String method:new String[]{"extensionSettingsManager","customSettingsManager"}){
                            try{
                                manager=api.getMethod(method).invoke(extension);
                                break;
                            }catch(NoSuchMethodException ignored){
                                // Older Carpet versions use customSettingsManager.
                            }
                        }
                        append(result,manager,translations);
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        // One incompatible optional extension should not hide other managers.
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Carpet is optional, and metadata failure must not disable server rule sync.
        }
        return result;
    }

    private static Object invokeManager(Object manager,String... methods)
            throws ReflectiveOperationException {
        for(String method:methods){
            try{return manager.getClass().getMethod(method).invoke(manager);}
            catch(NoSuchMethodException ignored){}
        }
        return null;
    }

    private static Object ruleProperty(Object rule,String name)
            throws ReflectiveOperationException {
        try{return CarpetRuleOwnership.invokeRule(rule,name);}
        catch(NoSuchMethodException ignored){
            // Carpet 1.4.44 ParsedRule exposes public name/categories fields.
            return rule.getClass().getField(name).get(rule);
        }
    }

    static void append(CarpetRuleMetadata out,Object manager,Map<String,String> translations) {
        if (manager == null) return;
        try {
            // SettingsManager/ParsedRule changed from getIdentifier/getRules and
            // public fields to identifier/getCarpetRules and CarpetRule methods.
            Object rawId=invokeManager(manager,"identifier","getIdentifier");
            Object rules=invokeManager(manager,"getCarpetRules","getRules");
            if(!(rawId instanceof String)||!(rules instanceof Collection))return;
            String managerId=(String)rawId;
            for(Object rule:(Collection<?>)rules){
                try{
                    Object rawName=ruleProperty(rule,"name");
                    Object tags=ruleProperty(rule,"categories");
                    if(!(rawName instanceof String))continue;
                    String name=(String)rawName;
                    java.util.List<String> categories=new java.util.ArrayList<>();
                    if(tags instanceof Collection)
                        for(Object tag:(Collection<?>)tags)if(tag instanceof String)categories.add((String)tag);
                    out.put(new CarpetRuleCatalog.RuleKey(managerId,name),
                            new CarpetRuleMetadata.Description(
                                    CarpetRuleTranslations.name(translations,managerId,name),
                                    CarpetRuleTranslations.description(translations,managerId,name),
                                    categories,"client-carpet"));
                }catch(ReflectiveOperationException|IllegalArgumentException|ClassCastException ignored){
                    // One incompatible Carpet rule must not hide other metadata.
                }
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Unknown SettingsManager API, not a server-rule discovery failure.
        }
    }
}

package ddstweak.feature.carpetrules;

import java.util.List;

/** The current value alone cannot identify a boolean Carpet rule. */
final class CarpetRuleValueTypes {
    private CarpetRuleValueTypes(){}

    static boolean booleanEditor(String current,String definitionDefault,List<String> suggestions){
        if(suggestions!=null&&!suggestions.isEmpty())
            return (definitionDefault==null||booleanToken(definitionDefault))
                    &&suggestions.size()==2&&suggestions.contains("true")
                    &&suggestions.contains("false");
        return booleanToken(current)&&booleanToken(definitionDefault);
    }

    private static boolean booleanToken(String text){
        return "true".equalsIgnoreCase(text)||"false".equalsIgnoreCase(text);
    }
}

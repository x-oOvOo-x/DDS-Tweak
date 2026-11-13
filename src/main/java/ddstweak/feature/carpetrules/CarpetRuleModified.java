package ddstweak.feature.carpetrules;

import java.math.BigDecimal;

/** Current deviation from a trusted definition default, not an edit history. */
final class CarpetRuleModified {
    enum State { UNKNOWN, UNCHANGED, MODIFIED }

    private CarpetRuleModified(){}

    static State classify(String current,String definitionDefault){
        if(current==null||definitionDefault==null)return State.UNKNOWN;
        if(CarpetRuleNumericValue.valid(current)&&CarpetRuleNumericValue.valid(definitionDefault))
            return new BigDecimal(current).compareTo(new BigDecimal(definitionDefault))==0
                    ?State.UNCHANGED:State.MODIFIED;
        if(booleanText(current)&&booleanText(definitionDefault))
            return current.equalsIgnoreCase(definitionDefault)?State.UNCHANGED:State.MODIFIED;
        return current.equals(definitionDefault)?State.UNCHANGED:State.MODIFIED;
    }

    private static boolean booleanText(String value){
        return "true".equalsIgnoreCase(value)||"false".equalsIgnoreCase(value);
    }
}

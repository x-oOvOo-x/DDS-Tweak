package ddstweak.feature.carpetrules;

import java.math.BigDecimal;

/** Local numeric editor eligibility only; server remains authoritative for bounds. */
final class CarpetRuleNumericValue {
    private CarpetRuleNumericValue(){}

    static boolean valid(String value){
        if(value==null||value.isEmpty()||value.length()>64
                ||!value.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))return false;
        try {
            new BigDecimal(value);
            return true;
        } catch(NumberFormatException ignored){
            return false;
        }
    }

    static boolean editable(String current,String defaultValue,boolean hasChoices){
        return !hasChoices&&valid(current)&&(defaultValue==null||valid(defaultValue));
    }
}

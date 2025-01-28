package com.tibudget.plugins.csv;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.*;

public class ValueFormatUtils {

    public static Set<DecimalFormat> determineValueFormat(Iterable<String> valuesString) {

        List<DecimalFormat> NUMBER_FORMATS = new ArrayList<DecimalFormat>();
        {
            DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
            symb.setDecimalSeparator('.');
            NUMBER_FORMATS.add(new DecimalFormat("#.#", symb));
        }
        {
            DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
            symb.setDecimalSeparator(',');
            NUMBER_FORMATS.add(new DecimalFormat("#.#", symb));
        }

        Set<DecimalFormat> acceptFormats = new HashSet<DecimalFormat>();
        Set<DecimalFormat> rejectFormats = new HashSet<DecimalFormat>();
        ParsePosition pp = new ParsePosition(0);
        for (String valueString : valuesString) {
            // Ignore empty strings or strings without any number
            if (valueString == null || doesNotLookLikeAValue(valueString)) {
                continue;
            }
            for (DecimalFormat format : NUMBER_FORMATS) {
                pp.setErrorIndex(-1); pp.setIndex(0);
                Number value = format.parse(valueString, pp);
                if (valueString.length() != pp.getIndex() || value == null) {
                    // Error parsing (add to rejected and remove from accepted)
                    rejectFormats.add(format);
                    acceptFormats.remove(format);
                }
                else {
                    if (!rejectFormats.contains(format)) {
                        // Add to accepted only if not already rejected
                        acceptFormats.add(format);
                    }
                }
            }
        }
        return acceptFormats;
    }

    /**
     * Checks if a string does not look like a monetary value.
     * @param val the input string
     * @return true if the string does not look like a monetary value, false otherwise
     */
    public static boolean doesNotLookLikeAValue(String val) {
        if (val == null || val.trim().isEmpty()) {
            return true; // Null or empty strings do not look like monetary values
        }

        // Updated regex to handle more generic monetary values:
        // - Optional sign (- or +) at the beginning
        // - Digits with or without separators (., space)
        // - Optional decimal part (, or .) followed by 1-2 digits
        // - May include currency symbols at the start or end
        String genericValueRegex =
                "^[\\s]*[-+]?[^a-zA-Z0-9]*([0-9]{1,3}([.,\\s][0-9]{3})*|[0-9]+)([.,][0-9]{1,2})?[^a-zA-Z0-9]*[\\s]*$";

        return !val.matches(genericValueRegex);
    }
}

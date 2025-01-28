package com.tibudget.plugins.csv;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class DateFormatUtils {

    private static final String[] DATE_FORMATS = new String[] {
            "yyyyddMM", "MMddyyyy", "yyyyMMdd", "ddMMyyyy",
            "yyyy-dd-MM", "MM-dd-yyyy", "yyyy-MM-dd", "dd-MM-yyyy",
            "yyyy/dd/MM", "MM/dd/yyyy", "yyyy/MM/dd", "dd/MM/yyyy",
            "MMM dd yyyy", "MMMM dd yyyy", "dd MMM yyyy", "dd MMMM yyyy"
    };

    /**
     * Return a set of accepted date formats for this collection of date strings.
     * @param datesString List of date strings
     * @return A set of accepted date formats for this collection of date strings
     */
    public static Set<String> determineDateFormat(Iterable<String> datesString) {
        Set<String> acceptFormats = new HashSet<>();
        Set<String> rejectFormats = new HashSet<>();
        ParsePosition pp = new ParsePosition(0);

        for (String dateString : datesString) {
            // Ignore empty strings or strings without any number
            if (dateString == null || doesNotLookLikeADate(dateString)) {
                continue;
            }

            for (String format : DATE_FORMATS) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
                // Don't automatically convert invalid dates
                simpleDateFormat.setLenient(false);
                pp.setErrorIndex(-1);
                pp.setIndex(0);

                try {
                    Date date = simpleDateFormat.parse(dateString, pp);

                    // Check if the parsing was complete and successful
                    if (pp.getIndex() == dateString.length() && date != null) {
                        if (!rejectFormats.contains(format)) {
                            acceptFormats.add(format);
                        }
                    } else {
                        rejectFormats.add(format);
                        acceptFormats.remove(format);
                    }
                } catch (Exception e) {
                    // If parsing throws an exception, reject the format
                    rejectFormats.add(format);
                    acceptFormats.remove(format);
                }
            }
        }

        return acceptFormats;
    }

    private static boolean doesNotLookLikeADate(String input) {
        return !input.matches(".*\\d.*");
    }
}

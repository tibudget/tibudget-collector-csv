package com.tibudget.plugins.csv;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DateFormatUtilsTest {

    @Test
    void testDetermineDateFormat_withValidDates() {
        Iterable<String> dates = Arrays.asList("20250128", ",,,", "", "Bank list", "19890520");
        Set<String> formats = DateFormatUtils.determineDateFormat(dates);

        assertTrue(formats.contains("yyyyMMdd"));
    }

    @Test
    void testDetermineDateFormat_withInvalidDates() {
        Iterable<String> dates = Arrays.asList("not-a-date", "", null);
        Set<String> formats = DateFormatUtils.determineDateFormat(dates);

        assertTrue(formats.isEmpty());
    }
}


package com.tibudget.plugins.csv;

import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValueFormatUtilsTest {
    @Test
    void testDetermineDateFormat_withValidDates() {
        Iterable<String> dates = Arrays.asList("14,92", ",,,", "", "Bank list", "-0,99");
        Set<DecimalFormat> formats = ValueFormatUtils.determineValueFormat(dates);

        assertFalse(formats.isEmpty());
    }

    @Test
    void testDetermineDateFormat_withInvalidDates() {
        Iterable<String> dates = Arrays.asList("not-a-date", "", null);
        Set<DecimalFormat> formats = ValueFormatUtils.determineValueFormat(dates);

        assertTrue(formats.isEmpty());
    }

    @Test
    public void testValidMonetaryValues() {
        // Valid monetary values should return false
        assertFalse(ValueFormatUtils.doesNotLookLikeAValue("1054,23"));
        assertFalse(ValueFormatUtils.doesNotLookLikeAValue("-12.231.000,12$"));
        assertFalse(ValueFormatUtils.doesNotLookLikeAValue("12,00€"));
        assertFalse(ValueFormatUtils.doesNotLookLikeAValue("+1 234 567.89"));
        assertFalse(ValueFormatUtils.doesNotLookLikeAValue("¥12345"));
        assertFalse(ValueFormatUtils.doesNotLookLikeAValue("₽ 12.34"));
        assertFalse(ValueFormatUtils.doesNotLookLikeAValue("₹ 1,234.00"));
        assertFalse(ValueFormatUtils.doesNotLookLikeAValue("1234.56₺"));
    }

    @Test
    public void testInvalidMonetaryValues() {
        // Invalid monetary values should return true
        assertTrue(ValueFormatUtils.doesNotLookLikeAValue("abcd"));
        assertTrue(ValueFormatUtils.doesNotLookLikeAValue("123,456,789.abc"));
        assertTrue(ValueFormatUtils.doesNotLookLikeAValue("This is not a value"));
        assertTrue(ValueFormatUtils.doesNotLookLikeAValue(""));
        assertTrue(ValueFormatUtils.doesNotLookLikeAValue(null));
        assertTrue(ValueFormatUtils.doesNotLookLikeAValue("12,34.56")); // Invalid format
    }
}

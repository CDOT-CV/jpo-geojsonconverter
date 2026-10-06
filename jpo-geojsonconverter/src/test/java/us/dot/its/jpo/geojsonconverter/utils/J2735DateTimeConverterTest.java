package us.dot.its.jpo.geojsonconverter.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import us.dot.its.jpo.asn.j2735.r2024.Common.DSecond;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;

public class J2735DateTimeConverterTest {

    // Test data - January 1, 2024 12:00:00 UTC
    private static final ZonedDateTime TEST_BASE_DATE = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    // Test data - June 15, 2024 15:30:45 UTC
    private static final ZonedDateTime TEST_ODE_DATE = ZonedDateTime.of(2024, 6, 15, 15, 30, 45, 0, ZoneOffset.UTC);

    // ===== generateUTCTimestamp TESTS =====

    @Test
    void testGenerateUTCTimestampWithMoyAndDSecond() {
        // Test basic MOY and DSecond conversion
        MinuteOfTheYear moy = new MinuteOfTheYear(1000); // 1000 minutes from start of year
        DSecond dSecond = new DSecond(5000); // 5 seconds (5000 milliseconds)
        Integer year = 2024;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, year);

        assertNotNull(result, "Result should not be null");
        assertEquals(2024, result.getYear(), "Year should be correct");
        assertEquals(1, result.getMonthValue(), "Month should be January");
        assertEquals(1, result.getDayOfMonth(), "Day should be 1");
        assertEquals(16, result.getHour(), "Hour should be 16 (1000 minutes = 16 hours 40 minutes)");
        assertEquals(40, result.getMinute(), "Minute should be 40");
        assertEquals(5, result.getSecond(), "Second should be 5");
        assertEquals(ZoneOffset.UTC, result.getOffset(), "Zone should be UTC");
    }

    @Test
    void testGenerateUTCTimestampWithMoyOnly() {
        // Test MOY only (no DSecond)
        MinuteOfTheYear moy = new MinuteOfTheYear(1440); // 1440 minutes = 24 hours = 1 day
        Integer year = 2024;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, null, TEST_ODE_DATE, year);

        int odeSecond = TEST_ODE_DATE.getSecond();

        assertNotNull(result, "Result should not be null");
        assertEquals(2024, result.getYear(), "Year should be correct");
        assertEquals(1, result.getMonthValue(), "Month should be January");
        assertEquals(2, result.getDayOfMonth(), "Day should be 2 (1440 minutes = 24 hours)");
        assertEquals(0, result.getHour(), "Hour should be 0");
        assertEquals(0, result.getMinute(), "Minute should be 0");
        assertEquals(odeSecond, result.getSecond(), "Second should be " + odeSecond);
    }

    @Test
    void testGenerateUTCTimestampWithNullMoy() {
        // Test with null MOY - should use ODE date
        DSecond dSecond = new DSecond(2000); // 2 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE.getYear(), result.getYear(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMonthValue(), result.getMonthValue(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getHour(), result.getHour(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMinute(), result.getMinute(), "Should use ODE date as base");
        assertEquals(2, result.getSecond(), "Second should be 2 (from DSecond)");
    }

    @Test
    void testGenerateUTCTimestampWithNullYear() {
        // Test with null year - should use ODE date year
        MinuteOfTheYear moy = new MinuteOfTheYear(100);
        DSecond dSecond = new DSecond(1000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, null);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE.getYear(), result.getYear(), "Should use ODE date year");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(value = {
            "Late MOY uses previous year;2025-01-01T00:00:05Z;527039;59000;2024-12-31T23:59:59Z",
            "Early MOY keeps current year;2025-01-01T00:00:05Z;60;null;2025-01-01T01:00:05Z",
            "Non-leap prior year uses December 31;2024-01-01T00:00:05Z;525599;59000;2023-12-31T23:59:59Z",
            "Leap prior year uses December 30;2025-01-01T00:00:05Z;525599;59000;2024-12-30T23:59:59Z"
    }, delimiter = ';', nullValues = "null")
    void testGenerateUTCTimestampChoosesYearAtNewYearsBoundary(String scenario, String odeReceivedAtText,
            long moyValue, Long dSecondValue, String expectedInstantText) {
        ZonedDateTime odeReceivedAt = ZonedDateTime.parse(odeReceivedAtText);
        MinuteOfTheYear moy = new MinuteOfTheYear(moyValue);
        DSecond dSecond = dSecondValue == null ? null : new DSecond(dSecondValue);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, odeReceivedAt);

        assertEquals(Instant.parse(expectedInstantText), result.toInstant());
    }

    @Test
    void testGenerateUTCTimestampKeepsCurrentYearForDay364MoyReceivedOnNewYearsDay() {
        // 524159 is the last minute of day 364, just below the >= 365 threshold.
        ZonedDateTime odeReceivedAt = ZonedDateTime.of(2025, 1, 1, 0, 0, 5, 0, ZoneOffset.UTC);
        MinuteOfTheYear moy = new MinuteOfTheYear(524_159);
        DSecond dSecond = new DSecond(59_000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, odeReceivedAt);

        assertEquals(Instant.parse("2025-12-30T23:59:59Z"), result.toInstant());
    }

    @Test
    void testGenerateUTCTimestampFallsBackToOdeDateForInvalidMoy() {
        ZonedDateTime odeReceivedAt = ZonedDateTime.of(2025, 6, 15, 15, 30, 45, 0, ZoneOffset.UTC);
        MinuteOfTheYear moy = new MinuteOfTheYear(527_040L);
        DSecond dSecond = new DSecond(3_000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, odeReceivedAt);

        assertEquals(ZonedDateTime.of(2025, 6, 15, 15, 30, 3, 0, ZoneOffset.UTC), result);
    }

    @Test
    void testGenerateUTCTimestampKeepsExplicitYearAtNewYearBoundary() {
        ZonedDateTime odeReceivedAt = ZonedDateTime.of(2025, 1, 1, 0, 0, 5, 0, ZoneOffset.UTC);
        MinuteOfTheYear moy = new MinuteOfTheYear(525_599);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, null, odeReceivedAt, 2026);

        assertEquals(2026, result.getYear());
    }

    @Test
    void testGenerateUTCTimestampThreeParameterOverload() {
        // Test the 3-parameter overload
        MinuteOfTheYear moy = new MinuteOfTheYear(2000);
        DSecond dSecond = new DSecond(3000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE.getYear(), result.getYear(), "Should use ODE date year");
    }

    @Test
    void testGenerateUTCTimestampTwoParameterOverload() {
        // Test the 2-parameter overload with MOY
        MinuteOfTheYear moy = new MinuteOfTheYear(500);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, TEST_ODE_DATE);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE.getYear(), result.getYear(), "Should use ODE date year");
        assertEquals(8, result.getHour(), "Should calculate correct time from MOY"); // 500 minutes = 8 hours 20 minutes
        assertEquals(20, result.getMinute(), "Should calculate correct time from MOY");
    }

    @Test
    void testGenerateUTCTimestampTwoParameterOverloadWidthNullMoy() {
        // Test the 2-parameter overload with null MOY
        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, TEST_ODE_DATE);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE, result, "Should return ODE date unchanged");
    }

    @Test
    void testGenerateUTCTimestampEdgeCaseZeroMoy() {
        // Test with zero MOY
        MinuteOfTheYear moy = new MinuteOfTheYear(0);
        DSecond dSecond = new DSecond(1000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.getMonthValue(), "Should be start of year");
        assertEquals(1, result.getDayOfMonth(), "Should be start of year");
        assertEquals(0, result.getHour(), "Should be start of year");
        assertEquals(0, result.getMinute(), "Should be start of year");
        assertEquals(1, result.getSecond(), "Should be 1 second from DSecond");
    }

    @Test
    void testGenerateUTCTimestampEdgeCaseLargeMoy() {
        // Test with large MOY (near end of year)
        MinuteOfTheYear moy = new MinuteOfTheYear(525600); // 525600 minutes = 365 days = 1 year
        DSecond dSecond = new DSecond(0);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull(result, "Result should not be null");
        assertEquals(12, result.getMonthValue(), "Should be end of year");
        assertEquals(31, result.getDayOfMonth(), "Should be end of year");
        assertEquals(0, result.getHour(), "Should be end of year");
        assertEquals(0, result.getMinute(), "Should be end of year");
    }

    @Test
    void testGenerateUTCTimestampEdgeCaseLargeDSecond() {
        // Test with large DSecond (near end of minute)
        MinuteOfTheYear moy = new MinuteOfTheYear(100);
        DSecond dSecond = new DSecond(59000); // 59 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull(result, "Result should not be null");
        assertEquals(59, result.getSecond(), "Should be 59 seconds");
    }

    @Test
    void testGenerateUTCTimestampWithNegativeValues() {
        // Test with negative values (should handle gracefully)
        MinuteOfTheYear moy = new MinuteOfTheYear(-100);
        DSecond dSecond = new DSecond(-1000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        // The method should handle negative values gracefully
        assertNotNull(result, "Result should not be null even with negative values");
    }

    @Test
    void testGenerateUTCTimestampWithLeapYear() {
        // Test with leap year
        MinuteOfTheYear moy = new MinuteOfTheYear(1440); // 1 day
        DSecond dSecond = new DSecond(0);
        Integer leapYear = 2024; // 2024 is a leap year

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, leapYear);

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.getDayOfMonth(), "Should be January 2nd");
    }

    @Test
    void testGenerateUTCTimestampWithNonLeapYear() {
        // Test with non-leap year
        MinuteOfTheYear moy = new MinuteOfTheYear(1440);
        DSecond dSecond = new DSecond(0);
        Integer nonLeapYear = 2023; // 2023 is not a leap year

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, nonLeapYear);

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.getDayOfMonth(), "Should be January 2nd");
    }

    @Test
    void testGenerateUTCTimestampConsistency() {
        // Test that different overloads produce consistent results
        MinuteOfTheYear moy = new MinuteOfTheYear(1000);
        DSecond dSecond = new DSecond(5000);
        Integer year = 2024;

        ZonedDateTime result1 = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, year);
        ZonedDateTime result2 = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE);

        assertNotNull(result1, "Result 1 should not be null");
        assertNotNull(result2, "Result 2 should not be null");
        assertEquals(result1, result2, "Results should be consistent");
    }

    @Test
    void testGenerateUTCTimestampWithNullMoyAndNullDSecond() {
        // Test with both MOY and DSecond null - should return ODE date unchanged
        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, null, TEST_ODE_DATE, 2024);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE, result, "Should return ODE date unchanged");
    }

    @Test
    void testGenerateUTCTimestampWithNullMoyAndValidDSecond() {
        // Test with null MOY but valid DSecond - should use ODE date and apply DSecond
        DSecond dSecond = new DSecond(3000); // 3 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE.getYear(), result.getYear(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMonthValue(), result.getMonthValue(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getHour(), result.getHour(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMinute(), result.getMinute(), "Should use ODE date as base");
        assertEquals(3, result.getSecond(), "Second should be 3 (from DSecond)");
        assertEquals(0, result.getNano() / 1_000_000, "Millisecond should be 0");
    }

    @Test
    void testGenerateUTCTimestampWithNullMoyAndLargeDSecond() {
        // Test with null MOY but large DSecond (near end of minute)
        DSecond dSecond = new DSecond(59000); // 59 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE.getYear(), result.getYear(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMonthValue(), result.getMonthValue(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getHour(), result.getHour(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMinute(), result.getMinute(), "Should use ODE date as base");
        assertEquals(59, result.getSecond(), "Second should be 59 (from DSecond)");
    }

    @Test
    void testGenerateUTCTimestampWithNullMoyAndZeroDSecond() {
        // Test with null MOY and zero DSecond - should return ODE date with seconds set to 0
        DSecond dSecond = new DSecond(0);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE.getYear(), result.getYear(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMonthValue(), result.getMonthValue(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getHour(), result.getHour(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMinute(), result.getMinute(), "Should use ODE date as base");
        assertEquals(0, result.getSecond(), "Second should be 0 (from DSecond)");
        assertEquals(0, result.getNano() / 1_000_000, "Millisecond should be 0");
    }

    @Test
    void testGenerateUTCTimestampWithNullMoyAndNegativeDSecond() {
        // Test with null MOY and negative DSecond - should handle gracefully
        DSecond dSecond = new DSecond(-1000); // -1 second

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        // The method should handle negative values gracefully
        assertNotNull(result, "Result should not be null even with negative DSecond");
    }

    @Test
    void testGenerateUTCTimestampTwoParameterOverloadWithNullMoyAndNullOdeDate() {
        // The 2-parameter overload returns the ODE date unchanged when MOY is null, including null.
        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, null);

        assertNull(result, "Should return null when the fallback ODE date is null");
    }

    @Test
    void testGenerateUTCTimestampThreeParameterOverloadWidthNullMoy() {
        // Test the 3-parameter overload with null MOY
        DSecond dSecond = new DSecond(1000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE);

        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ODE_DATE.getYear(), result.getYear(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMonthValue(), result.getMonthValue(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getHour(), result.getHour(), "Should use ODE date as base");
        assertEquals(TEST_ODE_DATE.getMinute(), result.getMinute(), "Should use ODE date as base");
        assertEquals(1, result.getSecond(), "Second should be 1 (from DSecond)");
    }

}

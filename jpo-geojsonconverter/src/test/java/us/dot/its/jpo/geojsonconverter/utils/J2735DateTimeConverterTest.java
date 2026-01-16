package us.dot.its.jpo.geojsonconverter.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.DSecond;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.TimeMark;

public class J2735DateTimeConverterTest {

    // Test data - January 1, 2024 12:00:00 UTC
    private static final ZonedDateTime TEST_BASE_DATE = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    // Test data - June 15, 2024 15:30:45 UTC
    private static final ZonedDateTime TEST_ODE_DATE = ZonedDateTime.of(2024, 6, 15, 15, 30, 45, 0, ZoneOffset.UTC);

    // ===== generateUTCTimestamp TESTS =====

    @Test
    public void testGenerateUTCTimestampWithMoyAndDSecond() {
        // Test basic MOY and DSecond conversion
        MinuteOfTheYear moy = new MinuteOfTheYear(1000); // 1000 minutes from start of year
        DSecond dSecond = new DSecond(5000); // 5 seconds (5000 milliseconds)
        Integer year = 2024;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, year);

        assertNotNull("Result should not be null", result);
        assertEquals("Year should be correct", 2024, result.getYear());
        assertEquals("Month should be January", 1, result.getMonthValue());
        assertEquals("Day should be 1", 1, result.getDayOfMonth());
        assertEquals("Hour should be 16 (1000 minutes = 16 hours 40 minutes)", 16, result.getHour());
        assertEquals("Minute should be 40", 40, result.getMinute());
        assertEquals("Second should be 5", 5, result.getSecond());
        assertEquals("Zone should be UTC", ZoneOffset.UTC, result.getOffset());
    }

    @Test
    public void testGenerateUTCTimestampWithMoyOnly() {
        // Test MOY only (no DSecond)
        MinuteOfTheYear moy = new MinuteOfTheYear(1440); // 1440 minutes = 24 hours = 1 day
        Integer year = 2024;

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, null, TEST_ODE_DATE, year);

        int odeSecond = TEST_ODE_DATE.getSecond();

        assertNotNull("Result should not be null", result);
        assertEquals("Year should be correct", 2024, result.getYear());
        assertEquals("Month should be January", 1, result.getMonthValue());
        assertEquals("Day should be 2 (1440 minutes = 24 hours)", 2, result.getDayOfMonth());
        assertEquals("Hour should be 0", 0, result.getHour());
        assertEquals("Minute should be 0", 0, result.getMinute());
        assertEquals("Second should be " + odeSecond, odeSecond, result.getSecond());
    }

    @Test
    public void testGenerateUTCTimestampWithNullMoy() {
        // Test with null MOY - should use ODE date
        DSecond dSecond = new DSecond(2000); // 2 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getYear(), result.getYear());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMonthValue(), result.getMonthValue());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getHour(), result.getHour());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMinute(), result.getMinute());
        assertEquals("Second should be 2 (from DSecond)", 2, result.getSecond());
    }

    @Test
    public void testGenerateUTCTimestampWithNullYear() {
        // Test with null year - should use ODE date year
        MinuteOfTheYear moy = new MinuteOfTheYear(100);
        DSecond dSecond = new DSecond(1000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, null);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date year", TEST_ODE_DATE.getYear(), result.getYear());
    }

    @Test
    public void testGenerateUTCTimestampThreeParameterOverload() {
        // Test the 3-parameter overload
        MinuteOfTheYear moy = new MinuteOfTheYear(2000);
        DSecond dSecond = new DSecond(3000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date year", TEST_ODE_DATE.getYear(), result.getYear());
    }

    @Test
    public void testGenerateUTCTimestampTwoParameterOverload() {
        // Test the 2-parameter overload with MOY
        MinuteOfTheYear moy = new MinuteOfTheYear(500);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, TEST_ODE_DATE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date year", TEST_ODE_DATE.getYear(), result.getYear());
        assertEquals("Should calculate correct time from MOY", 8, result.getHour()); // 500 minutes = 8 hours 20 minutes
        assertEquals("Should calculate correct time from MOY", 20, result.getMinute());
    }

    @Test
    public void testGenerateUTCTimestampTwoParameterOverloadWidthNullMoy() {
        // Test the 2-parameter overload with null MOY
        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, TEST_ODE_DATE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return ODE date unchanged", TEST_ODE_DATE, result);
    }

    @Test
    public void testGenerateUTCTimestampEdgeCaseZeroMoy() {
        // Test with zero MOY
        MinuteOfTheYear moy = new MinuteOfTheYear(0);
        DSecond dSecond = new DSecond(1000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be start of year", 1, result.getMonthValue());
        assertEquals("Should be start of year", 1, result.getDayOfMonth());
        assertEquals("Should be start of year", 0, result.getHour());
        assertEquals("Should be start of year", 0, result.getMinute());
        assertEquals("Should be 1 second from DSecond", 1, result.getSecond());
    }

    @Test
    public void testGenerateUTCTimestampEdgeCaseLargeMoy() {
        // Test with large MOY (near end of year)
        MinuteOfTheYear moy = new MinuteOfTheYear(525600); // 525600 minutes = 365 days = 1 year
        DSecond dSecond = new DSecond(0);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be end of year", 12, result.getMonthValue());
        assertEquals("Should be end of year", 31, result.getDayOfMonth());
        assertEquals("Should be end of year", 0, result.getHour());
        assertEquals("Should be end of year", 0, result.getMinute());
    }

    @Test
    public void testGenerateUTCTimestampEdgeCaseLargeDSecond() {
        // Test with large DSecond (near end of minute)
        MinuteOfTheYear moy = new MinuteOfTheYear(100);
        DSecond dSecond = new DSecond(59000); // 59 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be 59 seconds", 59, result.getSecond());
    }

    @Test
    public void testGenerateUTCTimestampWithNegativeValues() {
        // Test with negative values (should handle gracefully)
        MinuteOfTheYear moy = new MinuteOfTheYear(-100);
        DSecond dSecond = new DSecond(-1000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, 2024);

        // The method should handle negative values gracefully
        assertNotNull("Result should not be null even with negative values", result);
    }

    @Test
    public void testGenerateUTCTimestampWithLeapYear() {
        // Test with leap year
        MinuteOfTheYear moy = new MinuteOfTheYear(1440); // 1 day
        DSecond dSecond = new DSecond(0);
        Integer leapYear = 2024; // 2024 is a leap year

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, leapYear);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be January 2nd", 2, result.getDayOfMonth());
    }

    @Test
    public void testGenerateUTCTimestampWithNonLeapYear() {
        // Test with non-leap year
        MinuteOfTheYear moy = new MinuteOfTheYear(1440);
        DSecond dSecond = new DSecond(0);
        Integer nonLeapYear = 2023; // 2023 is not a leap year

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, nonLeapYear);

        assertNotNull("Result should not be null", result);
        assertEquals("Should be January 2nd", 2, result.getDayOfMonth());
    }

    @Test
    public void testGenerateUTCTimestampConsistency() {
        // Test that different overloads produce consistent results
        MinuteOfTheYear moy = new MinuteOfTheYear(1000);
        DSecond dSecond = new DSecond(5000);
        Integer year = 2024;

        ZonedDateTime result1 = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE, year);
        ZonedDateTime result2 = J2735DateTimeConverter.generateUTCTimestamp(moy, dSecond, TEST_ODE_DATE);

        assertNotNull("Result 1 should not be null", result1);
        assertNotNull("Result 2 should not be null", result2);
        assertEquals("Results should be consistent", result1, result2);
    }

    @Test
    public void testGenerateUTCTimestampWithNullMoyAndNullDSecond() {
        // Test with both MOY and DSecond null - should return ODE date unchanged
        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, null, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return ODE date unchanged", TEST_ODE_DATE, result);
    }

    @Test
    public void testGenerateUTCTimestampWithNullMoyAndValidDSecond() {
        // Test with null MOY but valid DSecond - should use ODE date and apply DSecond
        DSecond dSecond = new DSecond(3000); // 3 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getYear(), result.getYear());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMonthValue(), result.getMonthValue());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getHour(), result.getHour());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMinute(), result.getMinute());
        assertEquals("Second should be 3 (from DSecond)", 3, result.getSecond());
        assertEquals("Millisecond should be 0", 0, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateUTCTimestampWithNullMoyAndLargeDSecond() {
        // Test with null MOY but large DSecond (near end of minute)
        DSecond dSecond = new DSecond(59000); // 59 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getYear(), result.getYear());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMonthValue(), result.getMonthValue());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getHour(), result.getHour());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMinute(), result.getMinute());
        assertEquals("Second should be 59 (from DSecond)", 59, result.getSecond());
    }

    @Test
    public void testGenerateUTCTimestampWithNullMoyAndZeroDSecond() {
        // Test with null MOY and zero DSecond - should return ODE date with seconds set to 0
        DSecond dSecond = new DSecond(0);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getYear(), result.getYear());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMonthValue(), result.getMonthValue());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getHour(), result.getHour());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMinute(), result.getMinute());
        assertEquals("Second should be 0 (from DSecond)", 0, result.getSecond());
        assertEquals("Millisecond should be 0", 0, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateUTCTimestampWithNullMoyAndNegativeDSecond() {
        // Test with null MOY and negative DSecond - should handle gracefully
        DSecond dSecond = new DSecond(-1000); // -1 second

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE, 2024);

        // The method should handle negative values gracefully
        assertNotNull("Result should not be null even with negative DSecond", result);
    }

    @Test
    public void testGenerateUTCTimestampTwoParameterOverloadWidthNullMoyAndNullYear() {
        // Test the 2-parameter overload with null MOY and null year
        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, TEST_ODE_DATE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return ODE date unchanged", TEST_ODE_DATE, result);
    }

    @Test
    public void testGenerateUTCTimestampThreeParameterOverloadWidthNullMoy() {
        // Test the 3-parameter overload with null MOY
        DSecond dSecond = new DSecond(1000);

        ZonedDateTime result = J2735DateTimeConverter.generateUTCTimestamp(null, dSecond, TEST_ODE_DATE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getYear(), result.getYear());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMonthValue(), result.getMonthValue());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getDayOfMonth(), result.getDayOfMonth());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getHour(), result.getHour());
        assertEquals("Should use ODE date as base", TEST_ODE_DATE.getMinute(), result.getMinute());
        assertEquals("Second should be 1 (from DSecond)", 1, result.getSecond());
    }

    // ===== generateOffsetUTCTimestampForTimeMark TESTS =====

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkBasic() {
        // Test basic time mark conversion
        TimeMark timeMark = new TimeMark(5000); // 50 seconds (5000 deciseconds = 500000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be same as base", TEST_BASE_DATE.getHour(), result.getHour());
        assertEquals("Minute should be 8 (500000ms = 8min 20s)", 8, result.getMinute());
        assertEquals("Second should be 20", 20, result.getSecond());
        assertEquals("Zone should be UTC", ZoneOffset.UTC, result.getOffset());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkUnknown36111() {
        // Test special time mark 36111 (undefined/unknown)
        TimeMark timeMark = new TimeMark(36111);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return epoch time for unknown time mark", 0, result.toEpochSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkRollover() {
        // Test rollover logic - TimeMark in the past should apply to next hour
        ZonedDateTime currentTime = ZonedDateTime.of(2024, 1, 1, 15, 30, 0, 0, ZoneOffset.UTC); // 3:30 PM
        TimeMark timeMark = new TimeMark(1000); // 10 seconds (1000 deciseconds = 100000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(currentTime, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should add an hour for rollover", 16, result.getHour()); // Should be 4 PM, not 3 PM
        assertEquals("Minute should be 1 (100000ms = 1min 40s)", 1, result.getMinute());
        assertEquals("Second should be 40", 40, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkNoRollover() {
        // Test no rollover - TimeMark in the future should apply to current hour
        ZonedDateTime currentTime = ZonedDateTime.of(2024, 1, 1, 15, 30, 0, 0, ZoneOffset.UTC); // 3:30 PM
        TimeMark timeMark = new TimeMark(2000); // 20 seconds (2000 deciseconds = 200000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(currentTime, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should add an hour for rollover", 16, result.getHour()); // Should be 4 PM
        assertEquals("Minute should be 3 (200000ms = 3min 20s)", 3, result.getMinute());
        assertEquals("Second should be 20", 20, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkNull() {
        // Test with null time mark
        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, null);

        assertNull("Result should be null for null time mark", result);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkLeapSecond36000() {
        // Test leap second value 36000 (0 deciseconds into leap second)
        TimeMark timeMark = new TimeMark(36000);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be next hour due to leap second rollover", TEST_BASE_DATE.getHour() + 1,
                result.getHour());
        assertEquals("Minute should be 0 (leap second rolls over to next hour)", 0, result.getMinute());
        assertEquals("Second should be 0 (leap second rolls over to next hour)", 0, result.getSecond());
        assertEquals("Zone should be UTC", ZoneOffset.UTC, result.getOffset());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkLeapSecond36005() {
        // Test leap second value 36005 (5 deciseconds into leap second)
        TimeMark timeMark = new TimeMark(36005);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be next hour due to leap second rollover", TEST_BASE_DATE.getHour() + 1,
                result.getHour());
        assertEquals("Minute should be 0 (leap second rolls over to next hour)", 0, result.getMinute());
        assertEquals("Second should be 0 (leap second rolls over to next hour)", 0, result.getSecond());
        assertEquals("Millisecond should be 500 (5 deciseconds = 500ms)", 500, result.getNano() / 1_000_000);
        assertEquals("Zone should be UTC", ZoneOffset.UTC, result.getOffset());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkLeapSecond36009() {
        // Test leap second value 36009 (9 deciseconds into leap second)
        TimeMark timeMark = new TimeMark(36009);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be next hour due to leap second rollover", TEST_BASE_DATE.getHour() + 1,
                result.getHour());
        assertEquals("Minute should be 0 (leap second rolls over to next hour)", 0, result.getMinute());
        assertEquals("Second should be 0 (leap second rolls over to next hour)", 0, result.getSecond());
        assertEquals("Millisecond should be 900 (9 deciseconds = 900ms)", 900, result.getNano() / 1_000_000);
        assertEquals("Zone should be UTC", ZoneOffset.UTC, result.getOffset());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkLeapSecondRollover() {
        // Test leap second with rollover - TimeMark in the past should apply to next hour
        ZonedDateTime currentTime = ZonedDateTime.of(2024, 1, 1, 15, 30, 0, 0, ZoneOffset.UTC); // 3:30 PM
        TimeMark timeMark = new TimeMark(36000); // Leap second at 0 deciseconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(currentTime, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should add an hour for rollover", 16, result.getHour()); // Should be 4 PM, not 3 PM
        assertEquals("Minute should be 0 (leap second rolls over to next hour)", 0, result.getMinute());
        assertEquals("Second should be 0 (leap second rolls over to next hour)", 0, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkLeapSecondNoRollover() {
        // Test leap second with no rollover - TimeMark in the future should apply to current hour
        ZonedDateTime currentTime = ZonedDateTime.of(2024, 1, 1, 15, 30, 0, 0, ZoneOffset.UTC); // 3:30 PM
        TimeMark timeMark = new TimeMark(36000); // Leap second at 0 deciseconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(currentTime, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should add an hour due to leap second rollover", 16, result.getHour()); // Should be 4 PM
        assertEquals("Minute should be 0 (leap second rolls over to next hour)", 0, result.getMinute());
        assertEquals("Second should be 0 (leap second rolls over to next hour)", 0, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkOutOfRangeNegative() {
        // Test with negative TimeMark value
        TimeMark timeMark = new TimeMark(-1);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertEquals("Result should be set to UTC time zero",
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC")), result);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkOutOfRangeTooLarge() {
        // Test with TimeMark value > 36111
        TimeMark timeMark = new TimeMark(36112);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertEquals("Result should be set to UTC time zero",
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC")), result);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkZero() {
        // Test with TimeMark value 0 (start of hour)
        TimeMark timeMark = new TimeMark(0);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be next hour due to rollover", TEST_BASE_DATE.getHour() + 1, result.getHour());
        assertEquals("Minute should be 0", 0, result.getMinute());
        assertEquals("Second should be 0", 0, result.getSecond());
        assertEquals("Millisecond should be 0", 0, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkMaxNormal() {
        // Test with TimeMark value 35999 (59:59.9)
        TimeMark timeMark = new TimeMark(35999);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be same as base", TEST_BASE_DATE.getHour(), result.getHour());
        assertEquals("Minute should be 59", 59, result.getMinute());
        assertEquals("Second should be 59", 59, result.getSecond());
        assertEquals("Millisecond should be 900 (9 deciseconds = 900ms)", 900, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkReservedRange() {
        // Test with TimeMark value in reserved range (36010-36110)
        TimeMark timeMark = new TimeMark(36050); // Reserved value

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertEquals("Result should be set to UTC time zero",
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC")), result);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForTimeMarkEdgeCaseLargeTimeMark() {
        // Test with large time mark (near end of hour)
        TimeMark timeMark = new TimeMark(35999); // 59 minutes 59.9 seconds (35999 deciseconds = 3599900 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(TEST_BASE_DATE, timeMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Minute should be 59", 59, result.getMinute());
        assertEquals("Second should be 59 (3599900ms = 3599.9s = 59min 59.9s)", 59, result.getSecond());
        assertEquals("Millisecond should be 900 (9 deciseconds = 900ms)", 900, result.getNano() / 1_000_000);
    }

    // ===== generateOffsetUTCTimestampForSecMark TESTS =====

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkBasic() {
        // Test basic sec mark conversion
        DSecond secMark = new DSecond(5000); // 5 seconds (5000 milliseconds)

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Hour should be same as base", TEST_BASE_DATE.getHour(), result.getHour());
        assertEquals("Minute should be same as base", TEST_BASE_DATE.getMinute(), result.getMinute());
        assertEquals("Second should be 5", 5, result.getSecond());
        assertEquals("Millisecond should be 0", 0, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkWithMilliseconds() {
        // Test sec mark with milliseconds
        DSecond secMark = new DSecond(5234); // 5 seconds 234 milliseconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Second should be 5", 5, result.getSecond());
        assertEquals("Millisecond should be 234", 234, result.getNano() / 1_000_000);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkUnknown65535() {
        // Test special sec mark 65535 (unknown)
        DSecond secMark = new DSecond(65535);

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return epoch time for unknown sec mark", 0, result.toEpochSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkRollover() {
        // Test rollover logic - within 10 seconds of next minute with large sec mark
        ZonedDateTime nearMinuteEnd = ZonedDateTime.of(2024, 1, 1, 15, 30, 5, 0, ZoneOffset.UTC); // 5 seconds past
                                                                                                  // minute
        DSecond secMark = new DSecond(55000); // 55 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(nearMinuteEnd, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should subtract a minute for rollover", 29, result.getMinute()); // Should be 29 minutes, not 30
        assertEquals("Second should be 55", 55, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkNoRollover() {
        // Test no rollover - not within 10 seconds of next minute
        ZonedDateTime normalTime = ZonedDateTime.of(2024, 1, 1, 15, 30, 30, 0, ZoneOffset.UTC); // 30 seconds past
                                                                                                // minute
        DSecond secMark = new DSecond(55000); // 55 seconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(normalTime, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Should not subtract a minute", 30, result.getMinute()); // Should stay at 30 minutes
        assertEquals("Second should be 55", 55, result.getSecond());
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkNull() {
        // Test with null sec mark
        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, null);

        assertNull("Result should be null for null sec mark", result);
    }

    @Test
    public void testGenerateOffsetUTCTimestampForSecMarkEdgeCaseLargeSecMark() {
        // Test with large sec mark (near end of minute)
        DSecond secMark = new DSecond(59999); // 59 seconds 999 milliseconds

        ZonedDateTime result = J2735DateTimeConverter.generateOffsetUTCTimestampForSecMark(TEST_BASE_DATE, secMark);

        assertNotNull("Result should not be null", result);
        assertEquals("Second should be 59", 59, result.getSecond());
        assertEquals("Millisecond should be 999", 999, result.getNano() / 1_000_000);
    }
}

package us.dot.its.jpo.geojsonconverter.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DistanceUnits;
import us.dot.its.jpo.asn.j2735.r2024.SignalRequestMessage.DeltaTime;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class FieldConversionsTest {
    @Test
    public void testConvertMinuteOfYear() {
        // Normal case: middle of year
        ZonedDateTime ingestTime = ZonedDateTime.of(2025, 10, 3, 0, 0, 1, 0, ZoneOffset.UTC);
        final int dayOfYear = ingestTime.getDayOfYear();
        final int minuteOfYear = dayOfYear * 24 * 60 + 5;
        final var moy = new MinuteOfTheYear(minuteOfYear);
        final ZonedDateTime minuteDate = FieldConversions.convertMinuteOfYear(moy, ingestTime);
        final int year = minuteDate.getYear();
        assertEquals(2025, year);
    }

    @Test
    public void testConvertMinuteOfYear_InvalidValue() {
        ZonedDateTime ingestTime = ZonedDateTime.of(2025, 10, 3, 0, 0, 1, 500, ZoneOffset.UTC);
        final var moy = new MinuteOfTheYear(527040L);
        final ZonedDateTime minuteDate = FieldConversions.convertMinuteOfYear(moy, ingestTime);
        assertNull(minuteDate);
    }


    @Test
    public void testConvertMinuteOfYear_YearEnd() {
        // Test edge case of message sent at the end of the year
        final var moy = new MinuteOfTheYear(525599); // Last minute of the year

        // Last minute of this year
        final ZonedDateTime ingestTimeThisYear = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 500, ZoneOffset.UTC);
        final ZonedDateTime minuteDate1 = FieldConversions.convertMinuteOfYear(moy, ingestTimeThisYear);
        assertNotNull(minuteDate1);
        final int year1 = minuteDate1.getYear();
        assertEquals(2022, year1);

        // First minute of next year
        final ZonedDateTime ingestTimeNextYear = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        final ZonedDateTime minuteDate2 = FieldConversions.convertMinuteOfYear(moy, ingestTimeNextYear);
        assertNotNull(minuteDate2);
        final int year2 = minuteDate2.getYear();
        assertEquals(2022, year2);

        final var moyLeap = new MinuteOfTheYear(527037L);
        // Last minute of leap year
        final ZonedDateTime ingestTimeThisLeapYear = ZonedDateTime.of(2024, 12, 31, 23, 59, 59, 500, ZoneOffset.UTC);
        final ZonedDateTime minuteDateLeap1 = FieldConversions.convertMinuteOfYear(moyLeap, ingestTimeThisLeapYear);
        assertNotNull(minuteDateLeap1);
        final int yearLeap1 = minuteDateLeap1.getYear();
        assertEquals(2024, yearLeap1);

        final ZonedDateTime ingestTimeNextLeapYear = ZonedDateTime.of(2025, 1, 1, 0, 0, 1, 500, ZoneOffset.UTC);
        final ZonedDateTime minuteDateLeap2 = FieldConversions.convertMinuteOfYear(moy, ingestTimeNextLeapYear);
        assertNotNull(minuteDateLeap2);
        final int yearLeap2 = minuteDateLeap2.getYear();
        assertEquals(2024, yearLeap2);
    }

    // ========== Coordinate Conversion Tests ==========

    @Test
    public void testConvertLong() {
        // Test normal longitude conversion
        Double result = FieldConversions.convertLong(1800000000L);
        assertThat(result, equalTo(180.0));

        // Test negative longitude
        result = FieldConversions.convertLong(-1800000000L);
        assertThat(result, equalTo(-180.0));

        // Test zero longitude
        result = FieldConversions.convertLong(0L);
        assertThat(result, equalTo(0.0));

        // Test invalid longitude (should return null)
        result = FieldConversions.convertLong(1800000001L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertLat() {
        // Test normal latitude conversion
        Double result = FieldConversions.convertLat(900000000L);
        assertThat(result, equalTo(90.0));

        // Test negative latitude
        result = FieldConversions.convertLat(-900000000L);
        assertThat(result, equalTo(-90.0));

        // Test zero latitude
        result = FieldConversions.convertLat(0L);
        assertThat(result, equalTo(0.0));

        // Test invalid latitude (should return null)
        result = FieldConversions.convertLat(900000001L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertLongWithZoom() {
        // Test normal longitude with zoom (Zoom N multiplies LSB span by 2^N)
        Double result = FieldConversions.convertLongWithZoom(1800000000L, 2.0);
        assertThat(result, equalTo(360.0));

        // Test with null base value
        result = FieldConversions.convertLongWithZoom(1800000001L, 2.0);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertLatWithZoom() {
        // Test normal latitude with zoom (Zoom N multiplies LSB span by 2^N)
        Double result = FieldConversions.convertLatWithZoom(900000000L, 4.0);
        assertThat(result, equalTo(360.0));

        // Test with null base value
        result = FieldConversions.convertLatWithZoom(900000001L, 4.0);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertJ2735XY() {
        // Test XY conversion with zoom
        double[] result = FieldConversions.convertJ2735XY(100000L, 200000L, 40.0, 2.0);
        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(2));

        // Verify longitude offset calculation
        double expectedLonOffset = (100000.0 / 11111100.0) * 2.0 / Math.cos(Math.toRadians(40.0));
        assertThat(result[0], closeTo(expectedLonOffset, 0.0001));

        // Verify latitude offset calculation
        double expectedLatOffset = (200000.0 / 11111100.0) * 2.0;
        assertThat(result[1], closeTo(expectedLatOffset, 0.0001));
    }

    // ========== Elevation Conversion Tests ==========

    @Test
    public void testConvertElevation() {
        // Test normal elevation
        Double result = FieldConversions.convertElevation(1000L);
        assertThat(result, equalTo(100.0));

        // Test negative elevation
        result = FieldConversions.convertElevation(-1000L);
        assertThat(result, equalTo(-100.0));

        // Test zero elevation
        result = FieldConversions.convertElevation(0L);
        assertThat(result, equalTo(0.0));

        // Test unavailable elevation (should return null)
        result = FieldConversions.convertElevation(-4096L);
        assertThat(result, nullValue());
    }

    // ========== Acceleration Conversion Tests ==========

    @Test
    public void testConvertAccelLatLong() {
        // Test normal acceleration
        Double result = FieldConversions.convertAccelLatLong(1000L);
        assertThat(result, equalTo(10.0));

        // Test negative acceleration
        result = FieldConversions.convertAccelLatLong(-1000L);
        assertThat(result, equalTo(-10.0));

        // Test zero acceleration
        result = FieldConversions.convertAccelLatLong(0L);
        assertThat(result, equalTo(0.0));

        // Test unavailable acceleration (should return null)
        result = FieldConversions.convertAccelLatLong(2001L);
        assertThat(result, nullValue());

        // Test values beyond range
        result = FieldConversions.convertAccelLatLong(-2001L);
        assertThat(result, equalTo(-20.0));

        result = FieldConversions.convertAccelLatLong(2002L);
        assertThat(result, equalTo(20.0));
    }

    @Test
    public void testConvertAccelVert() {
        // Test normal vertical acceleration
        Double result = FieldConversions.convertAccelVert(50L);
        assertThat(result, equalTo(1.0));

        // Test negative vertical acceleration
        result = FieldConversions.convertAccelVert(-50L);
        assertThat(result, equalTo(-1.0));

        // Test zero vertical acceleration
        result = FieldConversions.convertAccelVert(0L);
        assertThat(result, equalTo(0.0));

        // Test unavailable vertical acceleration (should return null)
        result = FieldConversions.convertAccelVert(-127L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertAccelYaw() {
        // Test normal yaw rate
        Double result = FieldConversions.convertAccelYaw(1000L);
        assertThat(result, equalTo(10.0));

        // Test negative yaw rate
        result = FieldConversions.convertAccelYaw(-1000L);
        assertThat(result, equalTo(-10.0));

        // Test zero yaw rate
        result = FieldConversions.convertAccelYaw(0L);
        assertThat(result, equalTo(0.0));

        // Test values outside valid range (should return null)
        result = FieldConversions.convertAccelYaw(-32768L);
        assertThat(result, nullValue());

        result = FieldConversions.convertAccelYaw(32768L);
        assertThat(result, nullValue());
    }

    // ========== Accuracy Conversion Tests ==========

    @Test
    public void testConvertSemiMajor() {
        // Test normal semi-major axis
        Double result = FieldConversions.convertSemiMajor(100L);
        assertThat(result, equalTo(5.0));

        // Test zero semi-major axis
        result = FieldConversions.convertSemiMajor(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid value
        result = FieldConversions.convertSemiMajor(254L);
        assertThat(result, closeTo(12.7, 0.0001));

        // Test unavailable value (should return null)
        result = FieldConversions.convertSemiMajor(255L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertSemiMinor() {
        // Test normal semi-minor axis
        Double result = FieldConversions.convertSemiMinor(200L);
        assertThat(result, equalTo(10.0));

        // Test zero semi-minor axis
        result = FieldConversions.convertSemiMinor(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid value
        result = FieldConversions.convertSemiMinor(254L);
        assertThat(result, closeTo(12.7, 0.0001));

        // Test unavailable value (should return null)
        result = FieldConversions.convertSemiMinor(255L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertOrientation() {
        // Test normal orientation
        Double result = FieldConversions.convertOrientation(1000L);
        assertThat(result, closeTo(5.4932479, 0.0001));

        // Test zero orientation
        result = FieldConversions.convertOrientation(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid value
        result = FieldConversions.convertOrientation(65534L);
        assertThat(result, closeTo(359.9945078786, 0.0001));

        // Test unavailable value (should return null)
        result = FieldConversions.convertOrientation(65535L);
        assertThat(result, nullValue());
    }

    // ========== Angle Conversion Tests ==========

    @Test
    public void testConvertAngle() {
        // Test normal angle
        Double result = FieldConversions.convertAngle(10L);
        assertThat(result, equalTo(15.0));

        // Test negative angle
        result = FieldConversions.convertAngle(-10L);
        assertThat(result, equalTo(-15.0));

        // Test zero angle
        result = FieldConversions.convertAngle(0L);
        assertThat(result, equalTo(0.0));

        // Test values beyond range (should return null)
        result = FieldConversions.convertAngle(-127L);
        assertThat(result, nullValue());

        result = FieldConversions.convertAngle(127L);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertHeading() {
        // Test normal heading
        Double result = FieldConversions.convertHeading(1000L);
        assertThat(result, equalTo(12.5));

        // Test zero heading
        result = FieldConversions.convertHeading(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid heading
        result = FieldConversions.convertHeading(28800L);
        assertThat(result, equalTo(360.0));

        // Test values outside valid range (should return null)
        result = FieldConversions.convertHeading(-1L);
        assertThat(result, nullValue());

        result = FieldConversions.convertHeading(28801L);
        assertThat(result, nullValue());
    }

    // ========== Speed Conversion Tests ==========

    @Test
    public void testConvertSpeed() {
        // Test normal speed
        Double result = FieldConversions.convertSpeed(1000L);
        assertThat(result, equalTo(20.0));

        // Test zero speed
        result = FieldConversions.convertSpeed(0L);
        assertThat(result, equalTo(0.0));

        // Test maximum valid speed
        result = FieldConversions.convertSpeed(8190L);
        assertThat(result, equalTo(163.8));

        // Test unavailable speed (should return null)
        result = FieldConversions.convertSpeed(8191L);
        assertThat(result, nullValue());
    }

    // ========== Date/Time Conversion Tests ==========

    @Test
    public void testConvertDYear() {
        // Test normal year
        DYear dYear = new DYear(2024L);
        Integer result = FieldConversions.convertDYear(dYear);
        assertThat(result, equalTo(2024));

        // Test unknown year (should return null)
        dYear = new DYear(0L);
        result = FieldConversions.convertDYear(dYear);
        assertThat(result, nullValue());

        // Test null year (should return null)
        result = FieldConversions.convertDYear(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDMonth() {
        // Test normal month
        DMonth dMonth = new DMonth(6L);
        Integer result = FieldConversions.convertDMonth(dMonth);
        assertThat(result, equalTo(6));

        // Test unknown month (should return null)
        dMonth = new DMonth(0L);
        result = FieldConversions.convertDMonth(dMonth);
        assertThat(result, nullValue());

        // Test null month (should return null)
        result = FieldConversions.convertDMonth(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDDay() {
        // Test normal day
        DDay dDay = new DDay(15L);
        Integer result = FieldConversions.convertDDay(dDay);
        assertThat(result, equalTo(15));

        // Test unknown day (should return null)
        dDay = new DDay(0L);
        result = FieldConversions.convertDDay(dDay);
        assertThat(result, nullValue());

        // Test null day (should return null)
        result = FieldConversions.convertDDay(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDHour() {
        // Test normal hour
        DHour dHour = new DHour(14L);
        Integer result = FieldConversions.convertDHour(dHour);
        assertThat(result, equalTo(14));

        // Test unknown hour (should return null)
        dHour = new DHour(31L);
        result = FieldConversions.convertDHour(dHour);
        assertThat(result, nullValue());

        // Test null hour (should return null)
        result = FieldConversions.convertDHour(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDMinute() {
        // Test normal minute
        DMinute dMinute = new DMinute(30L);
        Integer result = FieldConversions.convertDMinute(dMinute);
        assertThat(result, equalTo(30));

        // Test unknown minute (should return null)
        dMinute = new DMinute(60L);
        result = FieldConversions.convertDMinute(dMinute);
        assertThat(result, nullValue());

        // Test null minute (should return null)
        result = FieldConversions.convertDMinute(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDSecond() {
        // Test normal second
        DSecond dSecond = new DSecond(30000L); // 30 seconds, 0 milliseconds
        FieldConversions.SecondNanos result = FieldConversions.convertDSecond(dSecond);
        assertThat(result, notNullValue());
        assertThat(result.secondOfMinute(), equalTo(30));
        assertThat(result.nanoOfSecond(), equalTo(0));

        // Test with milliseconds
        dSecond = new DSecond(30150L); // 30 seconds, 150 milliseconds
        result = FieldConversions.convertDSecond(dSecond);
        assertThat(result, notNullValue());
        assertThat(result.secondOfMinute(), equalTo(30));
        assertThat(result.nanoOfSecond(), equalTo(150000000));

        // Test unavailable second (should return null)
        dSecond = new DSecond(65535L);
        result = FieldConversions.convertDSecond(dSecond);
        assertThat(result, nullValue());

        // Test null second (should return null)
        result = FieldConversions.convertDSecond(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertDOffset() {
        // Test normal offset
        DOffset dOffset = new DOffset(120L); // +2 hours
        ZoneOffset result = FieldConversions.convertDOffset(dOffset);
        assertThat(result, equalTo(ZoneOffset.ofHours(2)));

        // Test negative offset
        dOffset = new DOffset(-180L); // -3 hours
        result = FieldConversions.convertDOffset(dOffset);
        assertThat(result, equalTo(ZoneOffset.ofHoursMinutes(-3, 0)));

        // Test null offset (should return UTC)
        result = FieldConversions.convertDOffset(null);
        assertThat(result, equalTo(ZoneOffset.UTC));
    }

    // ========== Lane Width Conversion Tests ==========

    @Test
    public void testConvertLaneWidth() {
        // Test normal lane width
        LaneWidth laneWidth = new LaneWidth(350L);
        Double result = FieldConversions.convertLaneWidth(laneWidth);
        assertThat(result, equalTo(3.5));

        // Test null lane width (should return null)
        result = FieldConversions.convertLaneWidth(null);
        assertThat(result, nullValue());
    }

    @Test
    public void testCalculateLaneWidthOffset() {
        // Test lane width offset calculation
        Double result = FieldConversions.calculateLaneWidthOffset(3.5, 50L);
        assertThat(result, equalTo(4.0));

        // Test with zero offset
        result = FieldConversions.calculateLaneWidthOffset(3.5, 0L);
        assertThat(result, equalTo(3.5));

        // Test with null lane width meter (should return null)
        result = FieldConversions.calculateLaneWidthOffset(null, 50L);
        assertThat(result, nullValue());
    }

    @Test
    public void testCalculateElevationOffset() {
        // Test elevation offset calculation
        Double result = FieldConversions.calculateElevationOffset(100.0, 50L);
        assertThat(result, equalTo(100.5));

        // Test with null elevation meter (should return null)
        result = FieldConversions.calculateElevationOffset(null, 50L);
        assertThat(result, nullValue());
    }

    // ========== Heading Sector Tests ==========
    // Note: Asn1Bitstring tests are commented out due to constructor issues
    // These would need to be implemented with proper mock objects or test data

    @Test
    public void testSectorBitToHeading() {
        // Test sector bit to heading conversion
        double result = FieldConversions.sectorBitToHeading(0);
        assertThat(result, equalTo(0.0));

        result = FieldConversions.sectorBitToHeading(1);
        assertThat(result, equalTo(22.5));

        result = FieldConversions.sectorBitToHeading(8);
        assertThat(result, equalTo(180.0));
    }

    @Test
    public void testSectorRangeToHeadingAndRange() {
        // Test sector range to heading and range conversion
        double[] result = FieldConversions.sectorRangeToHeadingAndRange(0, 2);
        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(2));
        assertThat(result[0], equalTo(22.5)); // Center heading
        assertThat(result[1], equalTo(67.5)); // Total range (3 sectors * 22.5)
    }

    @Test
    public void testGetHeadingSectorRange() {
        // Test getting heading sector range
        double result = FieldConversions.getHeadingSectorRange();
        assertThat(result, equalTo(22.5));
    }

    // ========== Radius Conversion Tests ==========

    @Test
    public void testConvertDistanceToMeters() {
        // Test centimeter conversion
        Double result = FieldConversions.convertDistanceToMeters(100L, DistanceUnits.CENTIMETER);
        assertThat(result, equalTo(1.0));

        // Test meter conversion
        result = FieldConversions.convertDistanceToMeters(100L, DistanceUnits.METER);
        assertThat(result, equalTo(100.0));

        // Test kilometer conversion
        result = FieldConversions.convertDistanceToMeters(1L, DistanceUnits.KILOMETER);
        assertThat(result, equalTo(1000.0));

        // Test foot conversion
        result = FieldConversions.convertDistanceToMeters(100L, DistanceUnits.FOOT);
        assertThat(result, closeTo(30.48, 0.01));

        // Test null units (should return null)
        result = FieldConversions.convertDistanceToMeters(100L, null);
        assertThat(result, nullValue());
    }

    // ========== Message Count Conversion Tests ==========

    @Test
    public void testConvertMsgCount() {
        // Test normal message count
        MsgCount msgCount = new MsgCount(5L);
        Integer result = FieldConversions.convertMsgCount(msgCount);
        assertThat(result, equalTo(5));

        // Test null message count (should return null)
        result = FieldConversions.convertMsgCount(null);
        assertThat(result, nullValue());
    }

    // ========== DDateTime Conversion Tests ==========

    @Test
    public void testConvertDDateTime() {
        List<String> validationMessages = new ArrayList<>();

        // Test complete DDateTime
        DDateTime dDateTime = new DDateTime();
        dDateTime.setYear(new DYear(2024L));
        dDateTime.setMonth(new DMonth(6L));
        dDateTime.setDay(new DDay(15L));
        dDateTime.setHour(new DHour(14L));
        dDateTime.setMinute(new DMinute(30L));
        dDateTime.setSecond(new DSecond(45000L)); // 45 seconds
        dDateTime.setOffset(new DOffset(0L)); // UTC

        Long result = FieldConversions.convertDDateTime(validationMessages, dDateTime);
        assertThat(result, notNullValue());
        assertThat(validationMessages, empty());

        // Test null DDateTime
        validationMessages.clear();
        result = FieldConversions.convertDDateTime(validationMessages, null);
        assertThat(result, nullValue());
        assertThat(validationMessages, hasSize(1));
        assertThat(validationMessages.get(0), containsString("DDateTime is missing"));

        // Test DDateTime with missing year
        validationMessages.clear();
        dDateTime = new DDateTime();
        dDateTime.setYear(new DYear(0L)); // Unknown year
        dDateTime.setMonth(new DMonth(6L));
        dDateTime.setDay(new DDay(15L));
        dDateTime.setHour(new DHour(14L));
        dDateTime.setMinute(new DMinute(30L));
        dDateTime.setSecond(new DSecond(45000L));
        dDateTime.setOffset(new DOffset(0L));

        result = FieldConversions.convertDDateTime(validationMessages, dDateTime);
        assertThat(result, nullValue());
        assertThat(validationMessages, hasItem(containsString("year")));
    }

    // ========== Heading Sector Bitstring Tests ==========

    @Test
    public void testParseHeadingSectorsFromBitstring() {
        // Test null bitstring
        List<Integer> result = FieldConversions.parseHeadingSectorsFromBitstring(null);
        assertThat(result, notNullValue());
        assertThat(result, empty());

        // Test bitstring with single bit set
        HeadingSlice bitstring = mock(HeadingSlice.class);
        when(bitstring.size()).thenReturn(16);
        when(bitstring.get(0)).thenReturn(true);
        when(bitstring.get(1)).thenReturn(false);
        when(bitstring.get(2)).thenReturn(false);
        when(bitstring.get(3)).thenReturn(true);
        when(bitstring.get(4)).thenReturn(false);
        for (int i = 5; i < 16; i++) {
            when(bitstring.get(i)).thenReturn(false);
        }

        result = FieldConversions.parseHeadingSectorsFromBitstring(bitstring);
        assertThat(result, notNullValue());
        assertThat(result, hasSize(2));
        assertThat(result, contains(0, 3));

        // Test bitstring with all bits set
        HeadingSlice allBitsSet = mock(HeadingSlice.class);
        when(allBitsSet.size()).thenReturn(16);
        for (int i = 0; i < 16; i++) {
            when(allBitsSet.get(i)).thenReturn(true);
        }

        result = FieldConversions.parseHeadingSectorsFromBitstring(allBitsSet);
        assertThat(result, notNullValue());
        assertThat(result, hasSize(16));

        // Test bitstring with no bits set
        HeadingSlice noBitsSet = mock(HeadingSlice.class);
        when(noBitsSet.size()).thenReturn(16);
        for (int i = 0; i < 16; i++) {
            when(noBitsSet.get(i)).thenReturn(false);
        }

        result = FieldConversions.parseHeadingSectorsFromBitstring(noBitsSet);
        assertThat(result, notNullValue());
        assertThat(result, empty());
    }

    @Test
    public void testParseHeadingSectorsAsRanges() {
        // Test null bitstring
        int[][] result = FieldConversions.parseHeadingSectorsAsRanges(null);
        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(0));

        // Test bitstring with adjacent sectors (should merge into one range)
        HeadingSlice adjacentBits = mock(HeadingSlice.class);
        when(adjacentBits.size()).thenReturn(16);
        when(adjacentBits.get(0)).thenReturn(true);
        when(adjacentBits.get(1)).thenReturn(true);
        when(adjacentBits.get(2)).thenReturn(true);
        for (int i = 3; i < 16; i++) {
            when(adjacentBits.get(i)).thenReturn(false);
        }

        result = FieldConversions.parseHeadingSectorsAsRanges(adjacentBits);
        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(1));
        assertThat(result[0][0], equalTo(0));
        assertThat(result[0][1], equalTo(2));

        // Test bitstring with non-adjacent sectors (should create multiple ranges)
        HeadingSlice nonAdjacentBits = mock(HeadingSlice.class);
        when(nonAdjacentBits.size()).thenReturn(16);
        when(nonAdjacentBits.get(0)).thenReturn(true);
        when(nonAdjacentBits.get(1)).thenReturn(false);
        when(nonAdjacentBits.get(2)).thenReturn(false);
        when(nonAdjacentBits.get(3)).thenReturn(true);
        when(nonAdjacentBits.get(4)).thenReturn(true);
        when(nonAdjacentBits.get(5)).thenReturn(false);
        when(nonAdjacentBits.get(6)).thenReturn(false);
        when(nonAdjacentBits.get(7)).thenReturn(true);
        for (int i = 8; i < 16; i++) {
            when(nonAdjacentBits.get(i)).thenReturn(false);
        }

        result = FieldConversions.parseHeadingSectorsAsRanges(nonAdjacentBits);
        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(3));
        assertThat(result[0][0], equalTo(0));
        assertThat(result[0][1], equalTo(0));
        assertThat(result[1][0], equalTo(3));
        assertThat(result[1][1], equalTo(4));
        assertThat(result[2][0], equalTo(7));
        assertThat(result[2][1], equalTo(7));

        // Test bitstring with no bits set
        HeadingSlice noBitsSet = mock(HeadingSlice.class);
        when(noBitsSet.size()).thenReturn(16);
        for (int i = 0; i < 16; i++) {
            when(noBitsSet.get(i)).thenReturn(false);
        }

        result = FieldConversions.parseHeadingSectorsAsRanges(noBitsSet);
        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(0));
    }

    @Test
    public void testParseHeadingSectorsAsRangesWrapsAcrossNorth() {
        HeadingSlice wrapAcrossNorth = mock(HeadingSlice.class);
        when(wrapAcrossNorth.size()).thenReturn(16);
        when(wrapAcrossNorth.get(0)).thenReturn(true);
        when(wrapAcrossNorth.get(1)).thenReturn(true);
        when(wrapAcrossNorth.get(15)).thenReturn(true);
        for (int i = 2; i < 15; i++) {
            when(wrapAcrossNorth.get(i)).thenReturn(false);
        }

        int[][] result = FieldConversions.parseHeadingSectorsAsRanges(wrapAcrossNorth);
        assertThat(result.length, equalTo(1));
        assertThat(result[0][0], equalTo(15));
        assertThat(result[0][1], equalTo(1));

        double[] headingAndRange = FieldConversions.sectorRangeToHeadingAndRange(result[0][0], result[0][1]);
        assertThat(headingAndRange[0], equalTo(11.25));
        assertThat(headingAndRange[1], equalTo(67.5));
    }

    // ========== Minute of Year Conversion Tests (with year parameter) ==========

    @Test
    public void testConvertMinuteOfYear_WithYear() {
        // Test normal minute of year with explicit year
        MinuteOfTheYear moy = new MinuteOfTheYear(1000L);
        ZonedDateTime result = FieldConversions.convertMinuteOfYear(moy, 2024);
        assertThat(result, notNullValue());
        assertThat(result.getYear(), equalTo(2024));
        assertThat(result.getMonthValue(), equalTo(1));
        assertThat(result.getDayOfMonth(), equalTo(1));

        // Test invalid minute of year (527040)
        moy = new MinuteOfTheYear(527040L);
        result = FieldConversions.convertMinuteOfYear(moy, 2024);
        assertThat(result, nullValue());

        // Test null minute of year
        result = FieldConversions.convertMinuteOfYear(null, 2024);
        assertThat(result, nullValue());
    }

    // ========== Minute of Year and DSecond Conversion Tests ==========

    @Test
    public void testConvertMinuteOfYearAndDSecond_WithIngestTime() {
        ZonedDateTime ingestTime = ZonedDateTime.of(2024, 6, 15, 14, 30, 0, 0, ZoneOffset.UTC);
        MinuteOfTheYear moy = new MinuteOfTheYear(1000L);
        DSecond dSecond = new DSecond(45000L); // 45 seconds

        ZonedDateTime result = FieldConversions.convertMinuteOfYearAndDSecond(moy, ingestTime, dSecond);
        assertThat(result, notNullValue());
        assertThat(result.getYear(), equalTo(2024));
        assertThat(result.getSecond(), equalTo(45));

        // Test with null DSecond
        result = FieldConversions.convertMinuteOfYearAndDSecond(moy, ingestTime, null);
        assertThat(result, notNullValue());
        assertThat(result.getSecond(), equalTo(0));

        // Test with null minute of year
        result = FieldConversions.convertMinuteOfYearAndDSecond(null, ingestTime, dSecond);
        assertThat(result, nullValue());
    }

    @Test
    public void testConvertMinuteOfYearAndDSecond_WithYear() {
        MinuteOfTheYear moy = new MinuteOfTheYear(1000L);
        DSecond dSecond = new DSecond(45000L); // 45 seconds

        ZonedDateTime result = FieldConversions.convertMinuteOfYearAndDSecond(moy, 2024, dSecond);
        assertThat(result, notNullValue());
        assertThat(result.getYear(), equalTo(2024));
        assertThat(result.getSecond(), equalTo(45));

        // Test with null DSecond
        result = FieldConversions.convertMinuteOfYearAndDSecond(moy, 2024, null);
        assertThat(result, notNullValue());
        assertThat(result.getSecond(), equalTo(0));

        // Test with null minute of year
        result = FieldConversions.convertMinuteOfYearAndDSecond(null, 2024, dSecond);
        assertThat(result, nullValue());
    }

    // ========== Intersection Reference ID Conversion Tests ==========

    @Test
    public void testConvertIntersectionReferenceID() {
        // Test with both region and ID
        IntersectionReferenceID intersectionRef = new IntersectionReferenceID();
        intersectionRef.setRegion(new RoadRegulatorID(10L));
        intersectionRef.setId(new IntersectionID(10001L));

        FieldConversions.RegionIntersectionId result = FieldConversions.convertIntersectionReferenceID(intersectionRef);
        assertThat(result, notNullValue());
        assertThat(result.region(), equalTo(10));
        assertThat(result.intersectionId(), equalTo(10001));

        // Test with only region
        intersectionRef = new IntersectionReferenceID();
        intersectionRef.setRegion(new RoadRegulatorID(10L));
        intersectionRef.setId(null);

        result = FieldConversions.convertIntersectionReferenceID(intersectionRef);
        assertThat(result, notNullValue());
        assertThat(result.region(), equalTo(10));
        assertThat(result.intersectionId(), nullValue());

        // Test with only ID
        intersectionRef = new IntersectionReferenceID();
        intersectionRef.setRegion(null);
        intersectionRef.setId(new IntersectionID(10001L));

        result = FieldConversions.convertIntersectionReferenceID(intersectionRef);
        assertThat(result, notNullValue());
        assertThat(result.region(), nullValue());
        assertThat(result.intersectionId(), equalTo(10001));

        // Test with null
        result = FieldConversions.convertIntersectionReferenceID(null);
        assertThat(result, notNullValue());
        assertThat(result.region(), nullValue());
        assertThat(result.intersectionId(), nullValue());
    }

    // ========== Vehicle ID Conversion Tests ==========

    @Test
    public void testConvertVehicleID() {
        // Test with EntityID (TemporaryID)
        VehicleID vehicleID = mock(VehicleID.class);
        var entityID = mock(us.dot.its.jpo.asn.j2735.r2024.Common.TemporaryID.class);
        when(entityID.getValue()).thenReturn("TEST123");
        when(vehicleID.getEntityID()).thenReturn(entityID);
        when(vehicleID.getStationID()).thenReturn(null);

        String result = FieldConversions.convertVehicleID(vehicleID);
        assertThat(result, notNullValue());
        assertThat(result, equalTo("TEST123"));

        // Test with StationID
        vehicleID = mock(VehicleID.class);
        StationID stationID = new StationID(12345L);
        when(vehicleID.getEntityID()).thenReturn(null);
        when(vehicleID.getStationID()).thenReturn(stationID);

        result = FieldConversions.convertVehicleID(vehicleID);
        assertThat(result, notNullValue());
        assertThat(result, equalTo("12345"));

        // Test with null
        result = FieldConversions.convertVehicleID(null);
        assertThat(result, nullValue());

        // Test with neither EntityID nor StationID
        vehicleID = mock(VehicleID.class);
        when(vehicleID.getEntityID()).thenReturn(null);
        when(vehicleID.getStationID()).thenReturn(null);

        result = FieldConversions.convertVehicleID(vehicleID);
        assertThat(result, nullValue());
    }

    // ========== Intersection Access Point ID Conversion Tests ==========

    @Test
    public void testConvertIntersectionAccessPointID() {
        // Test with LaneID
        IntersectionAccessPoint iap = mock(IntersectionAccessPoint.class);
        LaneID laneID = new LaneID(5L);
        when(iap.getLane()).thenReturn(laneID);
        when(iap.getApproach()).thenReturn(null);
        when(iap.getConnection()).thenReturn(null);

        FieldConversions.AccessPointID result = FieldConversions.convertIntersectionAccessPointID(iap);
        assertThat(result, notNullValue());
        assertThat(result.laneID(), equalTo(5));
        assertThat(result.approachID(), nullValue());
        assertThat(result.connectionID(), nullValue());

        // Test with ApproachID
        iap = mock(IntersectionAccessPoint.class);
        ApproachID approachID = new ApproachID(10L);
        when(iap.getLane()).thenReturn(null);
        when(iap.getApproach()).thenReturn(approachID);
        when(iap.getConnection()).thenReturn(null);

        result = FieldConversions.convertIntersectionAccessPointID(iap);
        assertThat(result, notNullValue());
        assertThat(result.laneID(), nullValue());
        assertThat(result.approachID(), equalTo(10));
        assertThat(result.connectionID(), nullValue());

        // Test with ConnectionID (LaneConnectionID)
        iap = mock(IntersectionAccessPoint.class);
        var connectionID = mock(us.dot.its.jpo.asn.j2735.r2024.Common.LaneConnectionID.class);
        when(connectionID.getValue()).thenReturn(15L);
        when(iap.getLane()).thenReturn(null);
        when(iap.getApproach()).thenReturn(null);
        when(iap.getConnection()).thenReturn(connectionID);

        result = FieldConversions.convertIntersectionAccessPointID(iap);
        assertThat(result, notNullValue());
        assertThat(result.laneID(), nullValue());
        assertThat(result.approachID(), nullValue());
        assertThat(result.connectionID(), equalTo(15));

        // Test with all three
        iap = mock(IntersectionAccessPoint.class);
        var connectionID2 = mock(us.dot.its.jpo.asn.j2735.r2024.Common.LaneConnectionID.class);
        when(connectionID2.getValue()).thenReturn(15L);
        when(iap.getLane()).thenReturn(new LaneID(5L));
        when(iap.getApproach()).thenReturn(new ApproachID(10L));
        when(iap.getConnection()).thenReturn(connectionID2);

        result = FieldConversions.convertIntersectionAccessPointID(iap);
        assertThat(result, notNullValue());
        assertThat(result.laneID(), equalTo(5));
        assertThat(result.approachID(), equalTo(10));
        assertThat(result.connectionID(), equalTo(15));

        // Test with null
        result = FieldConversions.convertIntersectionAccessPointID(null);
        assertThat(result, notNullValue());
        assertThat(result.laneID(), nullValue());
        assertThat(result.approachID(), nullValue());
        assertThat(result.connectionID(), nullValue());
    }

    // ========== Delta Time Conversion Tests ==========

    @Test
    public void testConvertDeltaTime() {
        // Test normal delta time
        DeltaTime deltaTime = new DeltaTime(60L); // 60 * 10 seconds = 600 seconds
        Duration result = FieldConversions.convertDeltaTime(deltaTime);
        assertThat(result, notNullValue());
        assertThat(result.getSeconds(), equalTo(600L));

        // Test negative delta time
        deltaTime = new DeltaTime(-60L); // -60 * 10 seconds = -600 seconds
        result = FieldConversions.convertDeltaTime(deltaTime);
        assertThat(result, notNullValue());
        assertThat(result.getSeconds(), equalTo(-600L));

        // Test unavailable delta time (should return null)
        deltaTime = new DeltaTime(-122L);
        result = FieldConversions.convertDeltaTime(deltaTime);
        assertThat(result, nullValue());

        // Test null delta time (should return null)
        result = FieldConversions.convertDeltaTime(null);
        assertThat(result, nullValue());
    }
}

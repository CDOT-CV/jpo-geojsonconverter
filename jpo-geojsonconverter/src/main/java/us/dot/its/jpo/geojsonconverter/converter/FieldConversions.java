package us.dot.its.jpo.geojsonconverter.converter;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DistanceUnits;
import us.dot.its.jpo.asn.j2735.r2024.SignalRequestMessage.DeltaTime;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedBasicVehicleRole;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedRequestImportanceLevel;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedRequestSubRole;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedVehicleType;

import java.time.*;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

@Slf4j
public class FieldConversions {

    // Heading sector constants
    private static final double HEADING_SECTOR_DEGREES = 22.5;
    private static final double HEADING_SECTOR_RANGE = 22.5;
    private static final int MAX_HEADING_SECTORS = 16;
    private static final double CENTIMETERS_PER_DEGREE_LATITUDE = 11111100.0;
    private static final double J2735_DECIMAL_CONVERSION_FACTOR = 10000000.0;
    private static final double CENTIMETERS_TO_METERS = 0.01;

    // J2735 coordinate unavailable values
    private static final long LONGITUDE_UNAVAILABLE = 1800000001L;
    private static final long LATITUDE_UNAVAILABLE = 900000001L;

    // Elevation constants
    private static final long ELEVATION_UNAVAILABLE = -4096L;
    private static final double ELEVATION_CONVERSION_FACTOR = 0.1; // LSB = 0.1 meters

    // Acceleration constants
    private static final long ACCEL_LAT_LONG_MIN = -2000L;
    private static final long ACCEL_LAT_LONG_MAX = 2001L;
    private static final double ACCEL_LAT_LONG_MIN_METERS_PER_SEC_SQ = -20.0;
    private static final double ACCEL_LAT_LONG_MAX_METERS_PER_SEC_SQ = 20.0;
    private static final double ACCEL_LAT_LONG_CONVERSION_FACTOR = 0.01; // LSB = 0.01 m/s^2

    // Vertical acceleration constants
    private static final long ACCEL_VERT_UNAVAILABLE = -127L;
    private static final double ACCEL_VERT_CONVERSION_FACTOR = 0.02; // LSB = 0.02 G

    // Yaw rate constants
    private static final long YAW_RATE_MIN = -32767L;
    private static final long YAW_RATE_MAX = 32767L;
    private static final double YAW_RATE_CONVERSION_FACTOR = 0.01; // LSB = 0.01 degrees per second

    // Semi-major/minor axis constants
    private static final long SEMI_AXIS_UNAVAILABLE = 255L;
    private static final double SEMI_AXIS_CONVERSION_FACTOR = 0.05; // LSB = 0.05 meters

    // Orientation constants
    private static final long ORIENTATION_UNAVAILABLE = 65535L;
    private static final double ORIENTATION_CONVERSION_FACTOR = 0.0054932479; // LSB = 360/65535 degrees

    // Steering wheel angle constants
    private static final long STEERING_ANGLE_MIN = -126L;
    private static final long STEERING_ANGLE_MAX = 127L;
    private static final double STEERING_ANGLE_CONVERSION_FACTOR = 1.5; // LSB = 1.5 degrees

    // Heading constants
    private static final long HEADING_MAX = 28800L;
    private static final double HEADING_CONVERSION_FACTOR = 0.0125; // LSB = 0.0125 degrees

    // Speed constants
    private static final long SPEED_UNAVAILABLE = 8191L;
    private static final double SPEED_CONVERSION_FACTOR = 0.02; // LSB = 0.02 m/s

    // Date/time constants
    private static final int MAX_HOUR = 23;
    private static final int MINUTE_UNAVAILABLE = 60;
    private static final long SECOND_RESERVED_THRESHOLD = 61000L;
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int NANOSECONDS_PER_MILLISECOND = 1000000;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int DAYS_PER_YEAR = 365;
    private static final long MINUTE_OF_YEAR_INVALID = 527040L;

    // Delta time constants
    private static final long DELTA_TIME_UNAVAILABLE = -122L;
    private static final int DELTA_TIME_SECONDS_PER_UNIT = 10; // Each unit = 10 seconds

    // Unit conversion factors
    private static final double CM2_5_TO_METERS = 0.025;
    private static final double DECIMETER_TO_METERS = 0.1;
    private static final double KILOMETER_TO_METERS = 1000.0;
    private static final double FOOT_TO_METERS = 0.3048;
    private static final double YARD_TO_METERS = 0.9144;
    private static final double MILE_TO_METERS = 1609.344;

    /**
     * Converts a J2735 longitude value to decimal degrees. Longitude ::= INTEGER (-1799999999..1800000001) LSB = 1/10
     * microdegree, providing a range of plus-minus 180 degrees.
     * 
     * @param j2735Long J2735 longitude value
     * @return Longitude in decimal degrees, or null if unavailable
     */
    public static Double convertLong(long j2735Long) {
        Double returnValue = null;
        if (j2735Long != LONGITUDE_UNAVAILABLE) {
            returnValue = j2735Long / J2735_DECIMAL_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 latitude value to decimal degrees. Latitude ::= INTEGER (-900000000..900000001) LSB = 1/10
     * microdegree, providing a range of plus-minus 90 degrees.
     * 
     * @param j2735Lat J2735 latitude value
     * @return Latitude in decimal degrees, or null if unavailable
     */
    public static Double convertLat(long j2735Lat) {
        Double returnValue = null;
        if (j2735Lat != LATITUDE_UNAVAILABLE) {
            returnValue = j2735Lat / J2735_DECIMAL_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Convert J2735 longitude value to decimal degrees with zoom scaling.
     * 
     * @param j2735Long J2735 longitude value
     * @param zoomFactor Zoom scaling factor (2^zoom)
     * @return Longitude in decimal degrees
     */
    public static Double convertLongWithZoom(long j2735Long, double zoomFactor) {
        Double baseValue = convertLong(j2735Long);
        if (baseValue != null) {
            return baseValue / zoomFactor;
        }
        return null;
    }

    /**
     * Convert J2735 latitude value to decimal degrees with zoom scaling.
     * 
     * @param j2735Lat J2735 latitude value
     * @param zoomFactor Zoom scaling factor (2^zoom)
     * @return Latitude in decimal degrees
     */
    public static Double convertLatWithZoom(long j2735Lat, double zoomFactor) {
        Double baseValue = convertLat(j2735Lat);
        if (baseValue != null) {
            return baseValue / zoomFactor;
        }
        return null;
    }

    /**
     * Convert J2735 XY coordinate to decimal degrees with zoom scaling.
     * 
     * @param j2735X J2735 X coordinate value (centimeters)
     * @param j2735Y J2735 Y coordinate value (centimeters)
     * @param currentLat Current latitude for longitude scaling
     * @param zoomFactor Zoom scaling factor (2^zoom)
     * @return Array with [longitude_offset, latitude_offset] in decimal degrees
     */
    public static double[] convertJ2735XY(long j2735X, long j2735Y, double currentLat, double zoomFactor) {
        double latOffset = (j2735Y / CENTIMETERS_PER_DEGREE_LATITUDE) / zoomFactor;
        double lonOffset =
                (j2735X / (CENTIMETERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(currentLat)))) / zoomFactor;
        return new double[] {lonOffset, latOffset};
    }

    /**
     * Converts a J2735 elevation value to meters. Providing a range of -409.5 to + 6143.9 meters. The value -4096 shall
     * be used when Unknown is to be sent.
     *
     * @param j2735Elev J2735 elevation value.
     * @return Elevation in meters, or null if unavailable.
     */
    public static Double convertElevation(long j2735Elev) {
        Double returnValue = null;
        if (j2735Elev != ELEVATION_UNAVAILABLE) {
            returnValue = j2735Elev * ELEVATION_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 lateral/longitudinal acceleration value to m/s^2. Acceleration ::= INTEGER (-2000..2001) LSB
     * units are 0.01 m/s^2. Values outside the range are clamped to ±20.0 m/s^2.
     * 
     * @param accelLatLong J2735 acceleration value
     * @return Acceleration in m/s^2, or null if unavailable
     */
    public static Double convertAccelLatLong(long accelLatLong) {
        Double returnValue = null;
        if (accelLatLong < ACCEL_LAT_LONG_MIN) {
            returnValue = ACCEL_LAT_LONG_MIN_METERS_PER_SEC_SQ;
        } else if (accelLatLong > ACCEL_LAT_LONG_MAX) {
            returnValue = ACCEL_LAT_LONG_MAX_METERS_PER_SEC_SQ;
        } else if (accelLatLong != ACCEL_LAT_LONG_MAX) {
            returnValue = accelLatLong * ACCEL_LAT_LONG_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 vertical acceleration value to G units. VerticalAcceleration ::= INTEGER (-127..127) LSB units
     * of 0.02 G steps over -2.52 to +2.54 G.
     * 
     * @param accelVert J2735 vertical acceleration value
     * @return Vertical acceleration in G units, or null if unavailable
     */
    public static Double convertAccelVert(long accelVert) {
        Double returnValue = null;
        if (accelVert != ACCEL_VERT_UNAVAILABLE) {
            returnValue = accelVert * ACCEL_VERT_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 yaw rate value to degrees per second. YawRate ::= INTEGER (-32767..32767) LSB units of 0.01
     * degrees per second (signed).
     * 
     * @param accelYaw J2735 yaw rate value
     * @return Yaw rate in degrees per second, or null if out of range
     */
    public static Double convertAccelYaw(long accelYaw) {
        Double returnValue = null;
        if (accelYaw >= YAW_RATE_MIN && accelYaw <= YAW_RATE_MAX) {
            returnValue = accelYaw * YAW_RATE_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 semi-major axis accuracy value to meters. SemiMajorAxisAccuracy ::= INTEGER (0..255) Range
     * 0-12.7 meters, LSB = 0.05m. Value 255 indicates unavailable.
     * 
     * @param semiMajor J2735 semi-major axis accuracy value
     * @return Semi-major axis accuracy in meters, or null if unavailable
     */
    public static Double convertSemiMajor(long semiMajor) {
        Double returnValue = null;
        if (semiMajor >= 0 && semiMajor < SEMI_AXIS_UNAVAILABLE) {
            returnValue = semiMajor * SEMI_AXIS_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 semi-minor axis accuracy value to meters. SemiMinorAxisAccuracy ::= INTEGER (0..255) Range
     * 0-12.7 meters, LSB = 0.05m. Value 255 indicates unavailable.
     * 
     * @param semiMinor J2735 semi-minor axis accuracy value
     * @return Semi-minor axis accuracy in meters, or null if unavailable
     */
    public static Double convertSemiMinor(long semiMinor) {
        Double returnValue = null;
        if (semiMinor >= 0 && semiMinor < SEMI_AXIS_UNAVAILABLE) {
            returnValue = semiMinor * SEMI_AXIS_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 semi-major axis orientation value to degrees. SemiMajorAxisOrientation ::= INTEGER (0..65535)
     * Orientation relative to true north (0~359.9945078786 degrees). LSB units of 360/65535 deg = 0.0054932479. Value
     * 65535 indicates unavailable.
     * 
     * @param orientation J2735 orientation value
     * @return Orientation in degrees relative to true north, or null if unavailable
     */
    public static Double convertOrientation(long orientation) {
        Double returnValue = null;
        if (orientation >= 0 && orientation < ORIENTATION_UNAVAILABLE) {
            returnValue = ORIENTATION_CONVERSION_FACTOR * orientation;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 steering wheel angle value to degrees. SteeringWheelAngle ::= INTEGER (-126..127) LSB units of
     * 1.5 degrees, a range of -189 to +189 degrees. Value 127 indicates unavailable.
     * 
     * @param angle J2735 steering wheel angle value
     * @return Steering wheel angle in degrees, or null if unavailable
     */
    public static Double convertAngle(long angle) {
        Double returnValue = null;
        if (angle >= STEERING_ANGLE_MIN && angle < STEERING_ANGLE_MAX) {
            returnValue = angle * STEERING_ANGLE_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 heading value to degrees. Heading ::= INTEGER (0..28800) LSB of 0.0125 degrees, providing a
     * range of 0 to 359.9875 degrees.
     * 
     * @param angle J2735 heading value
     * @return Heading in degrees, or null if out of range
     */
    public static Double convertHeading(long angle) {
        Double returnValue = null;
        if (angle >= 0 && angle <= HEADING_MAX) {
            returnValue = angle * HEADING_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 lane width value from centimeters to meters.
     * 
     * @param laneWidth J2735 LaneWidth value in centimeters
     * @return Lane width in meters, or null if laneWidth is null
     */
    public static Double convertLaneWidth(LaneWidth laneWidth) {
        if (laneWidth == null) {
            return null;
        }
        return laneWidth.getValue() * CENTIMETERS_TO_METERS;
    }

    /**
     * Converts a J2735 speed value to meters per second. Speed ::= INTEGER (0..8191) Units of 0.02 m/s. Value 8191
     * indicates speed is unavailable.
     * 
     * @param speed J2735 speed value
     * @return Speed in m/s, or null if unavailable
     */
    public static Double convertSpeed(long speed) {
        Double returnValue = null;
        if (speed >= 0 && speed < SPEED_UNAVAILABLE) {
            returnValue = speed * SPEED_CONVERSION_FACTOR;
        }
        return returnValue;
    }

    /**
     * Converts a J2735 DDateTime value to epoch milliseconds. Validates all required date/time components and collects
     * validation messages.
     * 
     * @param validationMessages List to collect validation error messages
     * @param dDateTime J2735 DDateTime value
     * @return Epoch milliseconds, or null if any required field is missing
     */
    public static Long convertDDateTime(List<String> validationMessages, DDateTime dDateTime) {
        if (dDateTime == null) {
            validationMessages.add("DDateTime is missing.");
            return null;
        }

        Integer year = convertDYear(dDateTime.getYear());
        if (year == null) {
            validationMessages.add("DDateTime 'year' field is missing.");
        }

        Integer month = convertDMonth(dDateTime.getMonth());
        if (month == null) {
            validationMessages.add("DDateTime 'month' field is missing.");
        }

        Integer dayOfMonth = convertDDay(dDateTime.getDay());
        if (dayOfMonth == null) {
            validationMessages.add("DDateTime 'day' field is missing.");
        }

        Integer hour = convertDHour(dDateTime.getHour());
        if (hour == null) {
            validationMessages.add("DDateTime 'hour' field is missing.");
        }

        Integer minute = convertDMinute(dDateTime.getMinute());
        if (minute == null) {
            validationMessages.add("DDateTime 'minute' field is missing.");
        }

        SecondNanos secondNanos = convertDSecond(dDateTime.getSecond());
        if (secondNanos == null) {
            validationMessages.add("DDateTime 'second' (millisecond of minute) field is missing.");
        }


        ZoneOffset offset = convertDOffset(dDateTime.getOffset());

        if (year != null && month != null && dayOfMonth != null && hour != null && minute != null
                && secondNanos != null) {
            OffsetDateTime odt = OffsetDateTime.of(year, month, dayOfMonth, hour, minute, secondNanos.secondOfMinute(),
                    secondNanos.nanoOfSecond(), offset);
            return odt.toInstant().toEpochMilli();
        }
        return null;
    }

    /**
     * Converts a J2735 DYear value to an integer. Value 0 represents unknown year.
     * 
     * @param dYear J2735 DYear value
     * @return Year as integer, or null if dYear is null or unknown
     */
    public static Integer convertDYear(DYear dYear) {
        if (dYear == null)
            return null;
        long value = dYear.getValue();
        // 0 represents unknown year
        if (value == 0)
            return null;
        return (int) value;
    }

    /**
     * Converts a J2735 DMonth value to an integer. Value 0 represents unknown month.
     * 
     * @param dMonth J2735 DMonth value
     * @return Month as integer (1-12), or null if dMonth is null or unknown
     */
    public static Integer convertDMonth(DMonth dMonth) {
        if (dMonth == null)
            return null;
        long value = dMonth.getValue();
        // 0 Represents unknown month
        if (value == 0)
            return null;
        return (int) value;
    }

    /**
     * Converts a J2735 DDay value to an integer. Value 0 represents unknown day.
     * 
     * @param dDay J2735 DDay value
     * @return Day of month as integer (1-31), or null if dDay is null or unknown
     */
    public static Integer convertDDay(DDay dDay) {
        if (dDay == null)
            return null;
        long value = dDay.getValue();
        // 0 represents unknown day
        if (value == 0)
            return null;
        return (int) value;
    }

    /**
     * Converts a J2735 DHour value to an integer. Per J2735 (2024) sec 7.34: Values 24-30 are used for schedule
     * adherence, and 31 represents unknown hours. Values > 23 are treated as unavailable.
     * 
     * @param dHour J2735 DHour value
     * @return Hour as integer (0-23), or null if dHour is null or unavailable
     */
    public static Integer convertDHour(DHour dHour) {
        if (dHour == null)
            return null;
        long value = dHour.getValue();
        // Per J2735 (2024) sec 7.34: 31 represents unknown hours and the values 24-30 are used by some applications
        // to represent schedule adherence.
        // But they are omitted here for use by the RTCM timestamp.
        if (value > MAX_HOUR)
            return null;
        return (int) value;
    }

    /**
     * Converts a J2735 DMinute value to an integer. Per J2735 (2024) sec 7.37: Value 60 represents unknown minute.
     * 
     * @param dMinute J2735 DMinute value
     * @return Minute as integer (0-59), or null if dMinute is null or unknown
     */
    public static Integer convertDMinute(DMinute dMinute) {
        if (dMinute == null)
            return null;
        long value = dMinute.getValue();
        // Per J2735 (2024) sec 7.37: 60 represents unknown hours
        if (value == MINUTE_UNAVAILABLE)
            return null;
        return (int) value;
    }

    /**
     * Millisecond of minute.
     *
     * @param dSecond DE_DSecond
     * @return second of minute, and nanosecond of second
     */
    public static SecondNanos convertDSecond(DSecond dSecond) {
        if (dSecond == null)
            return null;
        long value = dSecond.getValue();
        // Per J2735 (2024) sec. 7.43: 65535 represents unavailable, and values 61000 and above are reserved.
        if (value >= SECOND_RESERVED_THRESHOLD)
            return null;
        final int secondOfMinute = Math.floorDiv((int) value, MILLISECONDS_PER_SECOND);
        final int milliOfSecond = (int) value - (secondOfMinute * MILLISECONDS_PER_SECOND);
        final int nanoOfSecond = milliOfSecond * NANOSECONDS_PER_MILLISECOND;
        return new SecondNanos(secondOfMinute, nanoOfSecond);
    }

    public record SecondNanos(Integer secondOfMinute, Integer nanoOfSecond) {

    }


    /**
     * Convert time zone offset
     *
     * @param dOffset Offset in minutes
     * @return Java ZoneOffset
     */
    public static ZoneOffset convertDOffset(DOffset dOffset) {
        if (dOffset == null)
            return ZoneOffset.UTC;
        final int value = (int) dOffset.getValue();
        int offsetHours = Math.floorDiv(value, MINUTES_PER_HOUR);
        int offsetMinutes = value - (offsetHours * MINUTES_PER_HOUR);
        return ZoneOffset.ofHoursMinutes(offsetHours, offsetMinutes);
    }

    /**
     * Calculates the lane width based on the lane width meter and lane width offset in centimeters.
     *
     * @param laneWidthMeter J2735 lane width meter value.
     * @param laneWidthOffsetCm J2735 lane width offset in centimeters.
     * @return Lane width in meters.
     */
    public static Double calculateLaneWidthOffset(Double laneWidthMeter, long laneWidthOffsetCm) {
        Double returnValue = null;
        returnValue = laneWidthMeter + (laneWidthOffsetCm * CENTIMETERS_TO_METERS);
        return returnValue;
    }

    /**
     * Calculates the elevation based on the elevation meter and elevation offset in centimeters.
     *
     * @param elevationMeter J2735 elevation meter value.
     * @param elevationOffsetCm J2735 elevation offset in centimeters.
     * @return Elevation in meters.
     */
    public static Double calculateElevationOffset(Double elevationMeter, long elevationOffsetCm) {
        Double returnValue = null;
        if (elevationMeter != null) {
            returnValue = elevationMeter + (elevationOffsetCm * CENTIMETERS_TO_METERS);
        }
        return returnValue;
    }

    /**
     * Parse heading sectors directly from Asn1Bitstring. Each bit represents a 22.5-degree sector starting from North
     * (0°) and moving clockwise.
     *
     * @param directionBitstring The HeadingSlice containing the Asn1Bitstring direction field
     * @return List of active sector bit positions
     */
    public static List<Integer> parseHeadingSectorsFromBitstring(HeadingSlice directionBitstring) {
        if (directionBitstring == null) {
            return new ArrayList<>();
        }

        List<Integer> activeSectors = new ArrayList<>();

        // Check each bit up to the maximum number of heading sectors or the bitstring size
        int bitstringSize = directionBitstring.size();
        int maxBits = Math.min(MAX_HEADING_SECTORS, bitstringSize);

        for (int bit = 0; bit < maxBits; bit++) {
            if (directionBitstring.get(bit)) {
                activeSectors.add(bit);
            }
        }

        return activeSectors;
    }

    /**
     * Parse heading sectors as ranges from Asn1Bitstring, merging adjacent sectors into continuous ranges. Each bit
     * represents a 22.5-degree sector starting from North (0°) and moving clockwise.
     *
     * @param directionBitstring The Asn1Bitstring direction field
     * @return Array of heading ranges, where each range is represented as [startBit, endBit] (inclusive)
     */
    public static int[][] parseHeadingSectorsAsRanges(HeadingSlice directionBitstring) {
        List<Integer> activeSectors = parseHeadingSectorsFromBitstring(directionBitstring);

        if (activeSectors.isEmpty()) {
            return new int[0][];
        }

        // Group adjacent sectors into ranges
        List<int[]> ranges = new ArrayList<>();
        int rangeStart = activeSectors.get(0);
        int rangeEnd = rangeStart;

        for (int i = 1; i < activeSectors.size(); i++) {
            int currentBit = activeSectors.get(i);

            if (currentBit == rangeEnd + 1) {
                // Adjacent sector, extend the current range
                rangeEnd = currentBit;
            } else {
                // Non-adjacent sector, save the current range and start a new one
                ranges.add(new int[] {rangeStart, rangeEnd});
                rangeStart = currentBit;
                rangeEnd = currentBit;
            }
        }

        // Add the last range
        ranges.add(new int[] {rangeStart, rangeEnd});

        return ranges.toArray(new int[0][]);
    }


    /**
     * Convert sector bit position to heading degrees.
     *
     * @param sectorBit The sector bit position (0-15)
     * @return Heading in degrees
     */
    public static double sectorBitToHeading(int sectorBit) {
        return sectorBit * HEADING_SECTOR_DEGREES;
    }

    /**
     * Convert a range of sector bits to heading degrees and range.
     *
     * @param startBit The starting sector bit position (0-15)
     * @param endBit The ending sector bit position (0-15, inclusive)
     * @return Array containing [heading, range] in degrees
     */
    public static double[] sectorRangeToHeadingAndRange(int startBit, int endBit) {
        double startHeading = sectorBitToHeading(startBit);
        double endHeading = sectorBitToHeading(endBit);

        // Calculate the center heading of the range
        double centerHeading = (startHeading + endHeading) / 2.0;

        // Calculate the total range (number of sectors * 22.5 degrees)
        int numberOfSectors = endBit - startBit + 1;
        double totalRange = numberOfSectors * HEADING_SECTOR_DEGREES;

        return new double[] {centerHeading, totalRange};
    }

    /**
     * Get the range for a heading sector (22.5 degrees).
     *
     * @return Range in degrees (22.5)
     */
    public static double getHeadingSectorRange() {
        return HEADING_SECTOR_RANGE; // Each sector is exactly 22.5° wide
    }

    /**
     * Converts a distance value from the specified distance units to meters.
     * 
     * @param distance The distance value in the specified units
     * @param units The distance units enum value
     * @return Distance converted to meters, or null if units is null
     */
    public static Double convertDistanceToMeters(long distance, DistanceUnits units) {
        if (units == null) {
            return null;
        }

        return switch (units) {
            case CENTIMETER -> distance * CENTIMETERS_TO_METERS; // 1 cm = 0.01 m
            case CM2_5 -> distance * CM2_5_TO_METERS; // 1 cm2-5 = 0.025 m (2.5 cm)
            case DECIMETER -> distance * DECIMETER_TO_METERS; // 1 dm = 0.1 m
            case METER -> (double) distance; // 1 m = 1 m
            case KILOMETER -> distance * KILOMETER_TO_METERS; // 1 km = 1000 m
            case FOOT -> distance * FOOT_TO_METERS; // 1 ft = 0.3048 m
            case YARD -> distance * YARD_TO_METERS; // 1 yd = 0.9144 m
            case MILE -> distance * MILE_TO_METERS; // 1 mi = 1609.344 m
            default -> (double) distance; // Default to meters if unknown unit
        };
    }

    /**
     * Produce a ZonedDateTime from a minute of the year, and ingest time, adjusting for the edge case where the message
     * is produced on New Year's Eve and ingested on New Year's Day.
     * 
     * @param minuteOfTheYear J2735 Minute-of-the-year Integer
     * @param ingestTime The ingest time
     * @return A ZonedDateTime for the minute of the year
     */
    public static ZonedDateTime convertMinuteOfYear(final MinuteOfTheYear minuteOfTheYear,
            final ZonedDateTime ingestTime) {
        if (minuteOfTheYear == null)
            return null;
        final int moy = (int) minuteOfTheYear.getValue();
        final int dayOfYear = (moy / MINUTES_PER_DAY) + 1;
        final boolean isLastDayOfYear = dayOfYear >= DAYS_PER_YEAR; // or second to last if leap year

        // J2735 (2024) - Section 7.109: Says DE_Minute of the year is the minute of the "current year in the time
        // system being used (typically UTC time)" so we assume it is in fact UTC time.
        final ZonedDateTime utcIngestTime = ingestTime.withZoneSameInstant(ZoneOffset.UTC);
        final int ingestDayOfYear = utcIngestTime.getDayOfYear();
        final boolean isIngestFirstDayOfYear = (ingestDayOfYear == 1);

        int year = utcIngestTime.getYear();
        // If message produced on last day of year, and ingested on first day of year, it was produced last year.
        if (isLastDayOfYear && isIngestFirstDayOfYear) {
            --year;
        }

        return convertMinuteOfYear(minuteOfTheYear, year);
    }

    /**
     * Convert Minute of Year to ZonedDateTime. Requires knowing what year it is. Value of 527040 represents "invalid"
     * 
     * @param minuteOfTheYear DE_MinuteOfYear
     * @param year The year
     * @return ZonedDateTime for the year at the beginning of the minute
     */
    public static ZonedDateTime convertMinuteOfYear(final MinuteOfTheYear minuteOfTheYear, final int year) {
        if (minuteOfTheYear == null)
            return null;
        final long moy = minuteOfTheYear.getValue();
        if (moy == MINUTE_OF_YEAR_INVALID)
            return null;
        final String dateString = String.format("%d-01-01T00:00:00.00Z", year);
        final ZonedDateTime yearDate = Instant.parse(dateString).atZone(ZoneId.of("UTC"));
        return yearDate.plusMinutes(moy);
    }

    /**
     * Converts J2735 MinuteOfTheYear and DSecond to ZonedDateTime using ingest time for year determination.
     * 
     * @param minuteOfTheYear J2735 MinuteOfTheYear value
     * @param ingestTime The ingest time used to determine the year
     * @param dSecond J2735 DSecond value for sub-minute precision
     * @return ZonedDateTime representing the converted time, or null if minuteOfTheYear is null
     */
    public static ZonedDateTime convertMinuteOfYearAndDSecond(final MinuteOfTheYear minuteOfTheYear,
            final ZonedDateTime ingestTime, final DSecond dSecond) {
        ZonedDateTime minuteDate = convertMinuteOfYear(minuteOfTheYear, ingestTime);
        return convertMinuteOfYearAndDSecond(minuteDate, dSecond);
    }

    /**
     * Converts J2735 MinuteOfTheYear and DSecond to ZonedDateTime using a specified year.
     * 
     * @param minuteOfTheYear J2735 MinuteOfTheYear value
     * @param year The year to use for conversion
     * @param dSecond J2735 DSecond value for sub-minute precision
     * @return ZonedDateTime representing the converted time, or null if minuteOfTheYear is null
     */
    public static ZonedDateTime convertMinuteOfYearAndDSecond(final MinuteOfTheYear minuteOfTheYear, final int year,
            final DSecond dSecond) {
        ZonedDateTime minuteDate = convertMinuteOfYear(minuteOfTheYear, year);
        return convertMinuteOfYearAndDSecond(minuteDate, dSecond);
    }

    private static ZonedDateTime convertMinuteOfYearAndDSecond(final ZonedDateTime minuteDate, final DSecond dSecond) {
        if (minuteDate == null)
            return null;
        if (dSecond == null)
            return minuteDate;
        SecondNanos secondNanos = convertDSecond(dSecond);
        return minuteDate.withSecond(secondNanos.secondOfMinute()).withNano(secondNanos.nanoOfSecond());
    }



    /**
     * Converts a J2735 MsgCount value to an integer.
     * 
     * @param msgCount J2735 MsgCount value
     * @return Message count as integer, or null if msgCount is null
     */
    public static Integer convertMsgCount(final MsgCount msgCount) {
        if (msgCount == null)
            return null;
        return (int) msgCount.getValue();
    }

    /**
     * Converts a J2735 IntersectionReferenceID to a RegionIntersectionId record.
     * 
     * @param intersectionReferenceID J2735 IntersectionReferenceID value
     * @return RegionIntersectionId containing region and intersection ID, or both null if input is null
     */
    public static RegionIntersectionId convertIntersectionReferenceID(IntersectionReferenceID intersectionReferenceID) {
        if (intersectionReferenceID == null)
            return new RegionIntersectionId(null, null);
        var region = intersectionReferenceID.getRegion();
        Integer regionValue = region != null ? (int) region.getValue() : null;
        var id = intersectionReferenceID.getId();
        Integer idValue = id != null ? (int) id.getValue() : null;
        return new RegionIntersectionId(regionValue, idValue);
    }

    public record RegionIntersectionId(Integer region, Integer intersectionId) {
    }

    /**
     * Converts a J2735 VehicleID to a String. VehicleID is a CHOICE of EntityID (Octet string) or StationID (integer).
     * 
     * @param vehicleID J2735 VehicleID value
     * @return Vehicle ID as String, or null if vehicleID is null or has no value
     */
    public static String convertVehicleID(final VehicleID vehicleID) {
        if (vehicleID == null)
            return null;
        // CHOICE of EntityID or StationID
        // EntityID is an Octet string, StationId is an integer
        // Return a String in either case
        if (vehicleID.getEntityID() != null) {
            return vehicleID.getEntityID().getValue();
        }
        if (vehicleID.getStationID() != null) {
            return Long.toString(vehicleID.getStationID().getValue());
        }
        return null;
    }

    /**
     * Converts a J2735 IntersectionAccessPoint to an AccessPointID record. IntersectionAccessPoint is a CHOICE of
     * LaneID, ConnectionID, or ApproachID.
     * 
     * @param iap J2735 IntersectionAccessPoint value
     * @return AccessPointID containing lane, approach, and connection IDs, or all null if input is null
     */
    public static AccessPointID convertIntersectionAccessPointID(final IntersectionAccessPoint iap) {
        if (iap == null)
            return new AccessPointID(null, null, null);

        // CHOICE of LaneID, ConnectionID, or ApproachID
        Integer laneId = null;
        Integer approachId = null;
        Integer connectionId = null;
        if (iap.getLane() != null) {
            laneId = (int) iap.getLane().getValue();
        }
        if (iap.getApproach() != null) {
            approachId = (int) iap.getApproach().getValue();
        }
        if (iap.getConnection() != null) {
            connectionId = (int) iap.getConnection().getValue();
        }
        return new AccessPointID(laneId, approachId, connectionId);
    }

    public record AccessPointID(Integer laneID, Integer approachID, Integer connectionID) {
    }

    /**
     * Converts a J2735 BasicVehicleRole to a ProcessedBasicVehicleRole enum.
     * 
     * @param role J2735 BasicVehicleRole value
     * @return ProcessedBasicVehicleRole enum value, or null if role is null or has no name
     */
    public static ProcessedBasicVehicleRole convertBasicVehicleRole(final BasicVehicleRole role) {
        if (role != null && role.getName() != null) {
            return ProcessedBasicVehicleRole.fromName(role.getName());
        }
        return null;
    }

    /**
     * Converts a J2735 RequestSubRole to a ProcessedRequestSubRole enum.
     * 
     * @param role J2735 RequestSubRole value
     * @return ProcessedRequestSubRole enum value, or null if role is null or has no name
     */
    public static ProcessedRequestSubRole convertRequestSubRole(final RequestSubRole role) {
        if (role != null && role.getName() != null) {
            return ProcessedRequestSubRole.fromName(role.getName());
        }
        return null;
    }

    /**
     * Converts a J2735 VehicleType to a ProcessedVehicleType enum.
     * 
     * @param vehicleType J2735 VehicleType value
     * @return ProcessedVehicleType enum value, or null if vehicleType is null or has no name
     */
    public static ProcessedVehicleType convertVehicleType(final VehicleType vehicleType) {
        if (vehicleType != null && vehicleType.getName() != null) {
            return ProcessedVehicleType.fromName(vehicleType.getName());
        }
        return null;
    }

    /**
     * Converts a J2735 RequestImportanceLevel to a ProcessedRequestImportanceLevel enum.
     * 
     * @param importanceLevel J2735 RequestImportanceLevel value
     * @return ProcessedRequestImportanceLevel enum value, or null if importanceLevel is null or has no name
     */
    public static ProcessedRequestImportanceLevel convertRequestImportanceLevel(
            final RequestImportanceLevel importanceLevel) {
        if (importanceLevel != null && importanceLevel.getName() != null) {
            return ProcessedRequestImportanceLevel.valueOf(importanceLevel.getName());
        }
        return null;
    }

    /**
     * DeltaTime ::= INTEGER (-122 .. 121) -- Supporting a range of +/- 20 minute in steps of 10 seconds -- the value of
     * -121 shall be used when more than -20 minutes -- the value of +120 shall be used when more than +20 minutes --
     * the value -122 shall be used when the value is unavailable
     * 
     * @param deltaTime The difference from scheduled time
     * @return Duration in seconds
     */
    public static Duration convertDeltaTime(final DeltaTime deltaTime) {
        if (deltaTime == null) {
            return null;
        }
        long value = (int) deltaTime.getValue();
        if (value == DELTA_TIME_UNAVAILABLE) {
            return null;
        }
        return Duration.ofSeconds(value * DELTA_TIME_SECONDS_PER_UNIT);
    }

}

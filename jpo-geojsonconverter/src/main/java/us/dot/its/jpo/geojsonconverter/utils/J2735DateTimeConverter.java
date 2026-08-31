package us.dot.its.jpo.geojsonconverter.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.Common.DSecond;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.TimeMark;

@Slf4j
public class J2735DateTimeConverter {

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int LAST_OR_SECOND_TO_LAST_DAY_OF_YEAR = 365;
    private static final long MINUTE_OF_YEAR_INVALID = 527040L;

    /**
     * Generate UTC timestamp from Minute of Year (MOY) and optional DSecond values.
     * 
     * @param moy Minute of Year (minutes from beginning of year)
     * @param dSecond Optional DSecond value (milliseconds in current minute)
     * @param odeDate ODE received timestamp as fallback
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateUTCTimestamp(MinuteOfTheYear moy, DSecond dSecond, ZonedDateTime odeDate,
            Integer year) {
        ZonedDateTime date = null;
        try {
            boolean usableMoy = moy != null && moy.getValue() != MINUTE_OF_YEAR_INVALID;
            if (year == null) {
                year = odeDate.getYear();
                if (usableMoy && isPreviousYearMinuteOfYear(moy, odeDate)) {
                    year--;
                }
            }
            String dateString;
            long milliseconds;
            if (usableMoy) {
                long minutes = moy.getValue();
                if (dSecond != null) {
                    milliseconds = dSecond.getValue();
                } else {
                    // Use seconds and milliseconds from odeDate when dSecond is null
                    milliseconds = odeDate.getSecond() * 1000 + odeDate.getNano() / 1_000_000;
                }
                dateString = String.format("%d-01-01T00:00:00.00Z", year);
                date = Instant.parse(dateString).atZone(ZoneId.of("UTC"));
                date = date.plusMinutes(minutes);
                date = date.plus(milliseconds, ChronoUnit.MILLIS);
            } else {
                date = odeDate;
                if (dSecond != null) {
                    milliseconds = dSecond.getValue();
                    date = date.withSecond(0);
                    date = date.withNano(0);
                    date = date.plus(milliseconds, ChronoUnit.MILLIS);
                }
            }

        } catch (Exception e) {
            log.error("Failed to generate UTC Timestamp. Message: {}", e.getMessage(), e);
        }

        return date;
    }

    /**
     * Identifies a message created late in the previous year but received shortly after midnight on New Year's Day.
     *
     * <p>J2735 minute-of-year values do not include a year. Day-of-year is {@code moy / 1440 + 1}. A threshold of
     * {@code >= 365} covers the last day of a non-leap year and both of the final two days of a leap year, so no
     * separate {@code Year.isLeap} branch is required. When the ODE timestamp is January 1, those values are treated as
     * belonging to the previous year. This matches {@code FieldConversions.convertMinuteOfYear}.
     */
    private static boolean isPreviousYearMinuteOfYear(MinuteOfTheYear moy, ZonedDateTime odeDate) {
        ZonedDateTime utcOdeDate = odeDate.withZoneSameInstant(ZoneOffset.UTC);
        long dayOfYear = moy.getValue() / MINUTES_PER_DAY + 1;
        return utcOdeDate.getDayOfYear() == 1 && dayOfYear >= LAST_OR_SECOND_TO_LAST_DAY_OF_YEAR;
    }

    /**
     * Generate UTC timestamp from Minute of Year (MOY) and optional DSecond values.
     * 
     * @param moy Minute of Year (minutes from beginning of year)
     * @param dSecond Optional DSecond value (milliseconds in current minute)
     * @param odeDate ODE received timestamp as fallback
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateUTCTimestamp(MinuteOfTheYear moy, DSecond dSecond, ZonedDateTime odeDate) {
        return generateUTCTimestamp(moy, dSecond, odeDate, null);
    }

    /**
     * Generate UTC timestamp from optional Minute of Year (MOY) and ODE received timestamp.
     * 
     * @param moy Minute of Year (minutes from beginning of year)
     * @param odeDate ODE received timestamp as fallback
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateUTCTimestamp(MinuteOfTheYear moy, ZonedDateTime odeDate) {
        if (moy == null) {
            return odeDate;
        }

        return generateUTCTimestamp(moy, null, odeDate, null);
    }

    /**
     * Generate offset UTC timestamp for SPAT converter. Handles special time mark values and rollover logic.
     * 
     * TimeMark definition: - TimeMark is used to relate a moment in UTC time when a signal phase is predicted to change
     * - Precision of 1/10 of a second - Range of 60 full minutes is supported (0-35999 covers one hour) - Values
     * 36000-36009 are used when a leap second occurs - Values 36010-36110 are reserved for future use - 36111 is used
     * when the value is undefined or unknown - If value > current time, applies in current hour; if < current time,
     * applies in next hour
     *
     * @param originTimestamp Base timestamp to offset from
     * @param timeMark Time mark in deciseconds (1/10 second)
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateOffsetUTCTimestampForTimeMark(ZonedDateTime originTimestamp,
            TimeMark timeMark) {
        try {
            if (timeMark == null)
                return null;

            long value = timeMark.getValue();

            // Return UTC time zero if the Zoned Date time is marked as unknown, UTC time zero chosen so that a
            // null value can represent an empty field in the SPaT. But 36011, can represent an intentionally
            // unidentified field.
            if (value < 0 || value >= 36010) {
                log.warn("TimeMark value {} is out of valid range (0-36111)", value);
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));
            }

            long millis = value * 100;

            // Truncate to start of the current hour in UTC
            ZonedDateTime currentTime = originTimestamp.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime startOfHour = currentTime.truncatedTo(ChronoUnit.HOURS);

            // Current time in deciseconds within the hour
            long currentDecis = currentTime.getMinute() * 600 + currentTime.getSecond() * 10
                    + (currentTime.getNano() / 100_000_000);

            // Determine if TimeMark applies to current or next hour
            ZonedDateTime result = (value > currentDecis) ? startOfHour.plus(millis, ChronoUnit.MILLIS)
                    : startOfHour.plusHours(1).plus(millis, ChronoUnit.MILLIS);

            return result;
        } catch (Exception e) {
            String errMsg = String.format(
                    "Failed to generateOffsetUTCTimestampForTimeMark - J2735DateTimeConverter. Message: %s",
                    e.getMessage());
            log.error(errMsg, e);
            return null;
        }
    }

    /**
     * Generate offset UTC timestamp for BSM and PSM converters. Handles special secMark values and rollover logic.
     *
     * @param odeReceivedAt ODE received timestamp
     * @param secMark Second mark (milliseconds from beginning of minute)
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime generateOffsetUTCTimestampForSecMark(ZonedDateTime odeReceivedAt, DSecond secMark) {
        try {

            if (secMark == null)
                return null;

            long secMarkValue = secMark.getValue();
            int millis = (int) (secMarkValue % 1000);
            int seconds = (int) (secMarkValue / 1000);
            ZonedDateTime date = odeReceivedAt;
            if (secMarkValue == 65535) {
                // Return UTC time zero if the Zoned Date time is marked as unknown, UTC time zero chosen so that a
                // null value can represent an empty field in the BSM/PSM. But 65535, can represent an intentionally
                // unidentified field.
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));

            } else {
                // If we are within 10 seconds of the next minute, and the timeMark is a large number, it probably
                // means that the time rolled over before reception.
                // In this case, subtract a minute from the odeReceivedAt so that the true time represents the
                // minute in the past.
                if (odeReceivedAt.getSecond() < 10 && secMarkValue > 50000) {
                    date = date.minusMinutes(1);
                }

                date = date.withSecond(seconds);
                date = date.withNano(0);
                date = date.plus(millis, ChronoUnit.MILLIS);
                return date;
            }
        } catch (Exception e) {
            log.error("Failed to generateOffsetUTCTimestamp. Message: {}", e.getMessage(), e);
            return null;
        }
    }
}

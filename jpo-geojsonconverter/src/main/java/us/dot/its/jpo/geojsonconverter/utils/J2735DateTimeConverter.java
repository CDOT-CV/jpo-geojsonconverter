package us.dot.its.jpo.geojsonconverter.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.Common.DSecond;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;

@Slf4j
public class J2735DateTimeConverter {

    private J2735DateTimeConverter() {
        // Utility class; prevent instantiation.
    }

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
                    milliseconds = odeDate.getSecond() * 1000L + odeDate.getNano() / 1_000_000L;
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
     * <p>
     * J2735 minute-of-year values do not include a year. Day-of-year is {@code moy / 1440 + 1}. A threshold of
     * {@code >= 365} covers the last day of a non-leap year and both of the final two days of a leap year. When the ODE
     * timestamp is January 1, those values are treated as belonging to the previous year.
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

}

package com.acooldog.toolbox.location;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.utils.NmeaUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class NmeaGenerator {
    private static final double METERS_PER_SECOND_TO_KNOTS = 1.9438444924406d;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private NmeaGenerator() {
    }

    @NonNull
    public static String generateStandardSentences(
            double latitude,
            double longitude,
            double altitudeMeters,
            double speedMetersPerSecond,
            double bearingDegrees,
            long timeMillis,
            int satelliteCount
    ) {
        return NmeaUtils.joinSentences(Arrays.asList(
                generateGprmc(latitude, longitude, speedMetersPerSecond, bearingDegrees, timeMillis),
                generateGpgga(latitude, longitude, altitudeMeters, timeMillis, satelliteCount)
        ));
    }

    @NonNull
    public static String generateGprmc(
            double latitude,
            double longitude,
            double speedMetersPerSecond,
            double bearingDegrees,
            long timeMillis
    ) {
        String body = String.format(
                Locale.US,
                "GPRMC,%s,A,%s,%s,%s,%s,%.1f,%.1f,%s,,,A",
                formatUtcTime(timeMillis),
                NmeaUtils.formatLatitude(latitude),
                NmeaUtils.latitudeHemisphere(latitude),
                NmeaUtils.formatLongitude(longitude),
                NmeaUtils.longitudeHemisphere(longitude),
                Math.max(0d, speedMetersPerSecond) * METERS_PER_SECOND_TO_KNOTS,
                normalizeBearing(bearingDegrees),
                formatUtcDate(timeMillis)
        );
        return NmeaUtils.withChecksum(body);
    }

    @NonNull
    public static String generateGpgga(
            double latitude,
            double longitude,
            double altitudeMeters,
            long timeMillis,
            int satelliteCount
    ) {
        String body = String.format(
                Locale.US,
                "GPGGA,%s,%s,%s,%s,%s,1,%02d,0.9,%.1f,M,0.0,M,,",
                formatUtcTime(timeMillis),
                NmeaUtils.formatLatitude(latitude),
                NmeaUtils.latitudeHemisphere(latitude),
                NmeaUtils.formatLongitude(longitude),
                NmeaUtils.longitudeHemisphere(longitude),
                Math.max(0, Math.min(99, satelliteCount)),
                altitudeMeters
        );
        return NmeaUtils.withChecksum(body);
    }

    @NonNull
    private static String formatUtcTime(long timeMillis) {
        Calendar calendar = Calendar.getInstance(UTC, Locale.US);
        calendar.setTimeInMillis(timeMillis);
        return String.format(
                Locale.US,
                "%02d%02d%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
        );
    }

    @NonNull
    private static String formatUtcDate(long timeMillis) {
        Calendar calendar = Calendar.getInstance(UTC, Locale.US);
        calendar.setTimeInMillis(timeMillis);
        return String.format(
                Locale.US,
                "%02d%02d%02d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR) % 100
        );
    }

    private static double normalizeBearing(double bearingDegrees) {
        double normalized = bearingDegrees % 360d;
        if (normalized < 0d) {
            normalized += 360d;
        }
        return normalized;
    }
}

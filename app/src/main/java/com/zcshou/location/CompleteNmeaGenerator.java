package com.acooldog.toolbox.location;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.utils.NmeaUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class CompleteNmeaGenerator {
    public static final int SIGNAL_QUALITY_WEAK = 0;
    public static final int SIGNAL_QUALITY_MEDIUM = 1;
    public static final int SIGNAL_QUALITY_STRONG = 2;

    public static final int MIN_SATELLITE_COUNT = 1;
    public static final int MAX_SATELLITE_COUNT = 12;
    public static final float DEFAULT_HDOP = 1.5f;

    private static final double METERS_PER_SECOND_TO_KNOTS = 1.9438444924406d;
    private static final double METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR = 3.6d;
    private static final int MAX_SATELLITES_PER_GSV_SENTENCE = 4;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @NonNull
    public String generateCompleteNmea(
            double latitude,
            double longitude,
            double altitudeMeters,
            float speedMetersPerSecond,
            float bearingDegrees,
            int satelliteCount,
            int signalQuality,
            float hdop
    ) {
        return generateCompleteNmea(
                latitude,
                longitude,
                altitudeMeters,
                speedMetersPerSecond,
                bearingDegrees,
                satelliteCount,
                signalQuality,
                hdop,
                System.currentTimeMillis()
        );
    }

    @NonNull
    String generateCompleteNmea(
            double latitude,
            double longitude,
            double altitudeMeters,
            float speedMetersPerSecond,
            float bearingDegrees,
            int satelliteCount,
            int signalQuality,
            float hdop,
            long timeMillis
    ) {
        int normalizedSatelliteCount = normalizeSatelliteCount(satelliteCount);
        int normalizedSignalQuality = normalizeSignalQuality(signalQuality);
        float normalizedHdop = normalizeHdop(hdop);
        List<String> sentences = new ArrayList<>();

        sentences.add(generateGprmc(latitude, longitude, speedMetersPerSecond, bearingDegrees, timeMillis));
        sentences.add(generateGpgga(latitude, longitude, altitudeMeters, timeMillis, normalizedSatelliteCount, normalizedHdop));
        sentences.addAll(generateGpgsv(normalizedSatelliteCount, normalizedSignalQuality));
        sentences.add(generateGpgsa(normalizedSatelliteCount, normalizedHdop));
        sentences.add(generateGpvtg(speedMetersPerSecond, bearingDegrees));

        return NmeaUtils.joinSentences(sentences);
    }

    @NonNull
    String generateGprmc(
            double latitude,
            double longitude,
            float speedMetersPerSecond,
            float bearingDegrees,
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
                Math.max(0f, speedMetersPerSecond) * METERS_PER_SECOND_TO_KNOTS,
                normalizeBearing(bearingDegrees),
                formatUtcDate(timeMillis)
        );
        return NmeaUtils.withChecksum(body);
    }

    @NonNull
    String generateGpgga(
            double latitude,
            double longitude,
            double altitudeMeters,
            long timeMillis,
            int satelliteCount,
            float hdop
    ) {
        String body = String.format(
                Locale.US,
                "GPGGA,%s,%s,%s,%s,%s,1,%02d,%.1f,%.1f,M,0.0,M,,",
                formatUtcTime(timeMillis),
                NmeaUtils.formatLatitude(latitude),
                NmeaUtils.latitudeHemisphere(latitude),
                NmeaUtils.formatLongitude(longitude),
                NmeaUtils.longitudeHemisphere(longitude),
                normalizeSatelliteCount(satelliteCount),
                normalizeHdop(hdop),
                altitudeMeters
        );
        return NmeaUtils.withChecksum(body);
    }

    @NonNull
    List<String> generateGpgsv(int satelliteCount, int signalQuality) {
        int normalizedSatelliteCount = normalizeSatelliteCount(satelliteCount);
        int normalizedSignalQuality = normalizeSignalQuality(signalQuality);
        int sentenceCount = (int) Math.ceil(
                normalizedSatelliteCount / (double) MAX_SATELLITES_PER_GSV_SENTENCE
        );
        List<String> sentences = new ArrayList<>(sentenceCount);

        for (int sentenceNumber = 1; sentenceNumber <= sentenceCount; sentenceNumber++) {
            StringBuilder body = new StringBuilder("GPGSV,")
                    .append(sentenceCount)
                    .append(',')
                    .append(sentenceNumber)
                    .append(',')
                    .append(String.format(Locale.US, "%02d", normalizedSatelliteCount));

            int sentenceOffset = (sentenceNumber - 1) * MAX_SATELLITES_PER_GSV_SENTENCE;
            int satellitesInSentence = Math.min(
                    MAX_SATELLITES_PER_GSV_SENTENCE,
                    normalizedSatelliteCount - sentenceOffset
            );
            for (int index = 0; index < satellitesInSentence; index++) {
                int satelliteId = sentenceOffset + index + 1;
                body.append(String.format(
                        Locale.US,
                        ",%02d,%02d,%03d,%02d",
                        satelliteId,
                        calculateElevation(satelliteId),
                        calculateAzimuth(satelliteId),
                        calculateSnr(satelliteId, normalizedSignalQuality)
                ));
            }
            sentences.add(NmeaUtils.withChecksum(body.toString()));
        }

        return sentences;
    }

    @NonNull
    String generateGpgsa(int satelliteCount, float hdop) {
        int normalizedSatelliteCount = normalizeSatelliteCount(satelliteCount);
        float normalizedHdop = normalizeHdop(hdop);
        StringBuilder body = new StringBuilder("GPGSA,A,3,");
        for (int satelliteId = 1; satelliteId <= MAX_SATELLITE_COUNT; satelliteId++) {
            if (satelliteId <= normalizedSatelliteCount) {
                body.append(String.format(Locale.US, "%02d", satelliteId));
            }
            body.append(',');
        }

        double pdop = normalizedHdop * 1.2d;
        double vdop = Math.sqrt(Math.max(0d, pdop * pdop - normalizedHdop * normalizedHdop));
        body.append(String.format(Locale.US, "%.1f,%.1f,%.1f", pdop, normalizedHdop, vdop));

        return NmeaUtils.withChecksum(body.toString());
    }

    @NonNull
    String generateGpvtg(float speedMetersPerSecond, float bearingDegrees) {
        float normalizedSpeed = Math.max(0f, speedMetersPerSecond);
        String body = String.format(
                Locale.US,
                "GPVTG,%.1f,T,,M,%.1f,N,%.1f,K",
                normalizeBearing(bearingDegrees),
                normalizedSpeed * METERS_PER_SECOND_TO_KNOTS,
                normalizedSpeed * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR
        );
        return NmeaUtils.withChecksum(body);
    }

    public static int normalizeSatelliteCount(int satelliteCount) {
        return Math.max(MIN_SATELLITE_COUNT, Math.min(MAX_SATELLITE_COUNT, satelliteCount));
    }

    public static int normalizeSignalQuality(int signalQuality) {
        return Math.max(SIGNAL_QUALITY_WEAK, Math.min(SIGNAL_QUALITY_STRONG, signalQuality));
    }

    public static float normalizeHdop(float hdop) {
        if (Float.isNaN(hdop) || Float.isInfinite(hdop)) {
            return DEFAULT_HDOP;
        }
        return Math.max(0.5f, Math.min(99.9f, hdop));
    }

    private static int calculateElevation(int satelliteId) {
        return 10 + (satelliteId * 7) % 80;
    }

    private static int calculateAzimuth(int satelliteId) {
        return (satelliteId * 30) % 360;
    }

    private static int calculateSnr(int satelliteId, int signalQuality) {
        switch (signalQuality) {
            case SIGNAL_QUALITY_WEAK:
                return 25 + (satelliteId * 3) % 10;
            case SIGNAL_QUALITY_MEDIUM:
                return 35 + (satelliteId * 3) % 10;
            case SIGNAL_QUALITY_STRONG:
                return 45 + (satelliteId * 3) % 10;
            default:
                return 40;
        }
    }

    @NonNull
    private static String formatUtcTime(long timeMillis) {
        Calendar calendar = Calendar.getInstance(UTC, Locale.US);
        calendar.setTimeInMillis(timeMillis);
        return String.format(
                Locale.US,
                "%02d%02d%02d.00",
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

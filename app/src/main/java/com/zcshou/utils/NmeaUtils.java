package com.acooldog.toolbox.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

public final class NmeaUtils {
    private NmeaUtils() {
    }

    @NonNull
    public static String withChecksum(@NonNull String sentenceBody) {
        String normalizedBody = stripSentenceWrapper(sentenceBody);
        return "$" + normalizedBody + "*" + calculateChecksum(normalizedBody);
    }

    @NonNull
    public static String calculateChecksum(@NonNull String sentenceBody) {
        String normalizedBody = stripSentenceWrapper(sentenceBody);
        int checksum = 0;
        for (int index = 0; index < normalizedBody.length(); index++) {
            checksum ^= normalizedBody.charAt(index);
        }
        return String.format(Locale.US, "%02X", checksum);
    }

    public static boolean hasValidChecksum(@Nullable String sentence) {
        if (sentence == null || !sentence.startsWith("$")) {
            return false;
        }
        int asteriskIndex = sentence.indexOf('*');
        if (asteriskIndex < 0 || asteriskIndex + 3 > sentence.length()) {
            return false;
        }
        String body = sentence.substring(1, asteriskIndex);
        String expected = sentence.substring(asteriskIndex + 1, asteriskIndex + 3);
        return calculateChecksum(body).equalsIgnoreCase(expected);
    }

    @NonNull
    public static String joinSentences(@NonNull List<String> sentences) {
        StringBuilder builder = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence == null || sentence.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(sentence.trim());
        }
        return builder.toString();
    }

    @NonNull
    public static String formatLatitude(double latitude) {
        double absolute = Math.abs(latitude);
        int degrees = (int) absolute;
        double minutes = (absolute - degrees) * 60d;
        return String.format(Locale.US, "%02d%07.4f", degrees, minutes);
    }

    @NonNull
    public static String formatLongitude(double longitude) {
        double absolute = Math.abs(longitude);
        int degrees = (int) absolute;
        double minutes = (absolute - degrees) * 60d;
        return String.format(Locale.US, "%03d%07.4f", degrees, minutes);
    }

    @NonNull
    public static String latitudeHemisphere(double latitude) {
        return latitude >= 0d ? "N" : "S";
    }

    @NonNull
    public static String longitudeHemisphere(double longitude) {
        return longitude >= 0d ? "E" : "W";
    }

    @NonNull
    private static String stripSentenceWrapper(@NonNull String sentence) {
        String result = sentence.trim();
        if (result.startsWith("$")) {
            result = result.substring(1);
        }
        int asteriskIndex = result.indexOf('*');
        if (asteriskIndex >= 0) {
            result = result.substring(0, asteriskIndex);
        }
        return result;
    }
}

package com.acooldog.toolbox.location;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NmeaInjector {
    public static final String EXTRA_NMEA = "nmea";
    public static final int DEFAULT_SATELLITE_COUNT = 8;
    public static final int DEFAULT_SIGNAL_QUALITY = CompleteNmeaGenerator.SIGNAL_QUALITY_STRONG;
    public static final float DEFAULT_HDOP = CompleteNmeaGenerator.DEFAULT_HDOP;

    private static final CompleteNmeaGenerator COMPLETE_NMEA_GENERATOR = new CompleteNmeaGenerator();

    private NmeaInjector() {
    }

    public static void attachGeneratedNmea(@NonNull Location location) {
        attachGeneratedNmea(location, DEFAULT_SATELLITE_COUNT);
    }

    public static void attachGeneratedNmea(@NonNull Location location, int satelliteCount) {
        attachGeneratedNmea(location, satelliteCount, DEFAULT_SIGNAL_QUALITY, DEFAULT_HDOP);
    }

    public static void attachGeneratedNmea(
            @NonNull Location location,
            int satelliteCount,
            int signalQuality,
            float hdop
    ) {
        String nmea = COMPLETE_NMEA_GENERATOR.generateCompleteNmea(
                location.getLatitude(),
                location.getLongitude(),
                location.hasAltitude() ? location.getAltitude() : 0d,
                location.hasSpeed() ? location.getSpeed() : 0f,
                location.hasBearing() ? location.getBearing() : 0f,
                satelliteCount,
                signalQuality,
                hdop,
                location.getTime() > 0L ? location.getTime() : System.currentTimeMillis()
        );
        attachNmea(location, nmea);
        applyHighPrecisionMetadata(location, hdop);
    }

    public static void attachNmea(@NonNull Location location, @Nullable String nmea) {
        if (nmea == null || nmea.trim().isEmpty()) {
            return;
        }
        Bundle extras = location.getExtras() == null
                ? new Bundle()
                : new Bundle(location.getExtras());
        extras.putString(EXTRA_NMEA, nmea.trim());
        location.setExtras(extras);
    }

    private static void applyHighPrecisionMetadata(@NonNull Location location, float hdop) {
        if (!location.hasAccuracy()) {
            location.setAccuracy(CompleteNmeaGenerator.normalizeHdop(hdop) * 2.5f);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!location.hasVerticalAccuracy()) {
                location.setVerticalAccuracyMeters(CompleteNmeaGenerator.normalizeHdop(hdop) * 3f);
            }
            if (!location.hasSpeedAccuracy()) {
                location.setSpeedAccuracyMetersPerSecond(0.1f);
            }
            if (!location.hasBearingAccuracy()) {
                location.setBearingAccuracyDegrees(1f);
            }
        }
    }
}

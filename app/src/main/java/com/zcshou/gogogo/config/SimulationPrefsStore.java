package com.acooldog.toolbox.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class SimulationPrefsStore {
    public static final String ROUTE_MODE_SPEED = "speed";
    public static final String ROUTE_MODE_CADENCE = "cadence";
    public static final int DEFAULT_NMEA_SATELLITE_COUNT = 8;
    public static final int MIN_NMEA_SATELLITE_COUNT = 1;
    public static final int MAX_NMEA_SATELLITE_COUNT = 12;
    public static final int DEFAULT_NMEA_SIGNAL_QUALITY = 2;
    public static final int MIN_NMEA_SIGNAL_QUALITY = 0;
    public static final int MAX_NMEA_SIGNAL_QUALITY = 2;
    public static final float DEFAULT_NMEA_HDOP = 1.5f;
    public static final float MIN_NMEA_HDOP = 0.5f;
    public static final float MAX_NMEA_HDOP = 3.0f;
    public static final int DEFAULT_LOCATION_UPDATE_INTERVAL_MS = 200;
    public static final int MIN_LOCATION_UPDATE_INTERVAL_MS = 100;
    public static final int MAX_LOCATION_UPDATE_INTERVAL_MS = 2000;
    public static final boolean DEFAULT_NETWORK_SIMULATION_ENABLED = true;

    private static final String PREFS_NAME = "simulation_prefs_store";
    private static final String KEY_ROUTE_MODE = "route_mode";
    private static final String KEY_ROUTE_SPEED = "route_speed";
    private static final String KEY_ROUTE_CADENCE = "route_cadence";
    private static final String KEY_ROUTE_LOOP_COUNT = "route_loop_count";
    private static final String KEY_ROUTE_SPEED_FLOAT = "route_speed_float";
    private static final String KEY_ROUTE_LAST_ID = "route_last_id";
    private static final String KEY_ROUTE_LINK_RATIO_NUMERATOR = "route_link_ratio_numerator";
    private static final String KEY_ROUTE_STEPS_PER_METER = "route_steps_per_meter";
    private static final String KEY_ROUTE_REMINDER_TONE_URI = "route_reminder_tone_uri";
    private static final String KEY_ROUTE_REMINDER_TONE_TITLE = "route_reminder_tone_title";
    private static final String KEY_ROUTE_INTENSITY_RANGE = "route_intensity_range";
    private static final String KEY_ROUTE_INTENSITY_FREQUENCY = "route_intensity_frequency";
    private static final String KEY_ROUTE_PATH_VARIATION_ENABLED = "route_path_variation_enabled";
    private static final String KEY_ROUTE_PATH_VARIATION_AMPLITUDE = "route_path_variation_amplitude";
    private static final String KEY_ROUTE_ALTITUDE_VARIATION_ENABLED = "route_altitude_variation_enabled";
    private static final String KEY_ROUTE_ALTITUDE_BASE_METERS = "route_altitude_base_meters";
    private static final String KEY_ROUTE_ALTITUDE_VARIATION_RANGE = "route_altitude_variation_range";
    private static final String KEY_ROUTE_ALTITUDE_VARIATION_HEIGHT_CM = "route_altitude_variation_height_cm";
    private static final String KEY_ROUTE_ALTITUDE_VARIATION_PROBABILITY = "route_altitude_variation_probability";
    private static final String KEY_ROUTE_COMPLETION_PENDING = "route_completion_pending";
    private static final String KEY_ROUTE_FLOATING_WINDOW_ENABLED = "route_floating_window_enabled";
    private static final String KEY_ROUTE_FLOATING_WINDOW_SCALE = "route_floating_window_scale";
    private static final String KEY_ROUTE_FLOATING_WINDOW_BUTTON_SIZE = "route_floating_window_button_size";
    private static final String KEY_NMEA_SATELLITE_COUNT = "nmea_satellite_count";
    private static final String KEY_NMEA_SIGNAL_QUALITY = "nmea_signal_quality";
    private static final String KEY_NMEA_HDOP = "nmea_hdop";
    private static final String KEY_LOCATION_UPDATE_INTERVAL_MS = "location_update_interval_ms";
    private static final String KEY_NETWORK_SIMULATION_ENABLED = "network_simulation_enabled";
    private static final String KEY_NFC_URL = "nfc_url";
    private static final String KEY_NFC_PACKAGE = "nfc_package";
    private static final String KEY_NFC_SOURCE = "nfc_source";
    private static final String KEY_NFC_SAVED_CONFIGS = "nfc_saved_configs";

    private final SharedPreferences preferences;

    public SimulationPrefsStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getRouteSpeed() {
        String value = preferences.getString(KEY_ROUTE_SPEED, "15");
        if ("3.8".equals(value)) {
            return "15";
        }
        return value;
    }

    public String getRouteMode() {
        String value = preferences.getString(KEY_ROUTE_MODE, ROUTE_MODE_SPEED);
        return ROUTE_MODE_CADENCE.equals(value) ? ROUTE_MODE_CADENCE : ROUTE_MODE_SPEED;
    }

    public String getRouteLoopCount() {
        String value = preferences.getString(KEY_ROUTE_LOOP_COUNT, "100");
        if ("10".equals(value)) {
            return "100";
        }
        return value;
    }

    public String getRouteCadence() {
        return preferences.getString(KEY_ROUTE_CADENCE, "");
    }

    public boolean isRouteSpeedFloat() {
        return preferences.getBoolean(KEY_ROUTE_SPEED_FLOAT, true);
    }

    public String getRouteIntensityVariationRange() {
        return preferences.getString(KEY_ROUTE_INTENSITY_RANGE, "2.0");
    }

    public float getRouteIntensityVariationFrequency() {
        return clampFrequency(preferences.getFloat(KEY_ROUTE_INTENSITY_FREQUENCY, 0.35f));
    }

    public boolean isRouteNaturalPathVariationEnabled() {
        return preferences.getBoolean(KEY_ROUTE_PATH_VARIATION_ENABLED, false);
    }

    public String getRoutePathVariationAmplitude() {
        return preferences.getString(KEY_ROUTE_PATH_VARIATION_AMPLITUDE, "1.0");
    }

    public boolean isRouteNaturalAltitudeVariationEnabled() {
        return preferences.getBoolean(KEY_ROUTE_ALTITUDE_VARIATION_ENABLED, false);
    }

    public String getRouteAltitudeBaseMeters() {
        return preferences.getString(KEY_ROUTE_ALTITUDE_BASE_METERS, "55.0");
    }

    public String getRouteAltitudeVariationRange() {
        return preferences.getString(KEY_ROUTE_ALTITUDE_VARIATION_RANGE, "0.6");
    }

    public String getRouteAltitudeVariationHeightCm() {
        return preferences.getString(KEY_ROUTE_ALTITUDE_VARIATION_HEIGHT_CM, "170");
    }

    public float getRouteAltitudeVariationProbability() {
        return clampFrequency(preferences.getFloat(KEY_ROUTE_ALTITUDE_VARIATION_PROBABILITY, 0.35f));
    }

    public String getRouteLinkRatioNumerator() {
        return preferences.getString(KEY_ROUTE_LINK_RATIO_NUMERATOR, "1");
    }

    public String getRouteReminderToneUri() {
        return preferences.getString(KEY_ROUTE_REMINDER_TONE_URI, "");
    }

    public String getRouteReminderToneTitle() {
        return preferences.getString(KEY_ROUTE_REMINDER_TONE_TITLE, "");
    }

    public String getRouteStepsPerMeter() {
        return preferences.getString(KEY_ROUTE_STEPS_PER_METER, "1");
    }

    public String getLastRouteId() {
        return preferences.getString(KEY_ROUTE_LAST_ID, "");
    }

    public boolean isRouteCompletionPending() {
        return preferences.getBoolean(KEY_ROUTE_COMPLETION_PENDING, false);
    }

    public boolean isRouteFloatingWindowEnabled() {
        return preferences.getBoolean(KEY_ROUTE_FLOATING_WINDOW_ENABLED, false);
    }

    public float getRouteFloatingWindowScale() {
        return clampFloatingScale(preferences.getFloat(KEY_ROUTE_FLOATING_WINDOW_SCALE, 0.58f));
    }

    public float getRouteFloatingWindowButtonSizeDp() {
        return clampFloatingButtonSize(preferences.getFloat(KEY_ROUTE_FLOATING_WINDOW_BUTTON_SIZE, 44f));
    }

    public int getNmeaSatelliteCount() {
        return clampInt(
                preferences.getInt(KEY_NMEA_SATELLITE_COUNT, DEFAULT_NMEA_SATELLITE_COUNT),
                MIN_NMEA_SATELLITE_COUNT,
                MAX_NMEA_SATELLITE_COUNT
        );
    }

    public int getNmeaSignalQuality() {
        return clampInt(
                preferences.getInt(KEY_NMEA_SIGNAL_QUALITY, DEFAULT_NMEA_SIGNAL_QUALITY),
                MIN_NMEA_SIGNAL_QUALITY,
                MAX_NMEA_SIGNAL_QUALITY
        );
    }

    public float getNmeaHdop() {
        return clampFloat(
                preferences.getFloat(KEY_NMEA_HDOP, DEFAULT_NMEA_HDOP),
                MIN_NMEA_HDOP,
                MAX_NMEA_HDOP
        );
    }

    public int getLocationUpdateIntervalMillis() {
        return clampInt(
                preferences.getInt(KEY_LOCATION_UPDATE_INTERVAL_MS, DEFAULT_LOCATION_UPDATE_INTERVAL_MS),
                MIN_LOCATION_UPDATE_INTERVAL_MS,
                MAX_LOCATION_UPDATE_INTERVAL_MS
        );
    }

    public boolean isNetworkSimulationEnabled() {
        return preferences.getBoolean(KEY_NETWORK_SIMULATION_ENABLED, DEFAULT_NETWORK_SIMULATION_ENABLED);
    }

    public void saveRouteConfig(
            String routeMode,
            String speed,
            String cadence,
            String loopCount,
            boolean speedFloat,
            String routeId,
            String linkRatioNumerator
    ) {
        preferences.edit()
                .putString(KEY_ROUTE_MODE, normalizeRouteMode(routeMode))
                .putString(KEY_ROUTE_SPEED, normalize(speed, "15"))
                .putString(KEY_ROUTE_CADENCE, normalize(cadence, ""))
                .putString(KEY_ROUTE_LOOP_COUNT, normalize(loopCount, "100"))
                .putBoolean(KEY_ROUTE_SPEED_FLOAT, speedFloat)
                .putString(KEY_ROUTE_LAST_ID, normalize(routeId, ""))
                .putString(KEY_ROUTE_LINK_RATIO_NUMERATOR, normalize(linkRatioNumerator, "1"))
                .apply();
    }

    public void saveRouteReminderTone(String uri, String title) {
        preferences.edit()
                .putString(KEY_ROUTE_REMINDER_TONE_URI, normalize(uri, ""))
                .putString(KEY_ROUTE_REMINDER_TONE_TITLE, normalize(title, ""))
                .apply();
    }

    public void saveRouteIntensityVariationSettings(String intensityRange, float intensityFrequency) {
        preferences.edit()
                .putString(KEY_ROUTE_INTENSITY_RANGE, normalize(intensityRange, "2.0"))
                .putFloat(KEY_ROUTE_INTENSITY_FREQUENCY, clampFrequency(intensityFrequency))
                .apply();
    }

    public void saveRoutePathVariationSettings(boolean enabled, String amplitude) {
        preferences.edit()
                .putBoolean(KEY_ROUTE_PATH_VARIATION_ENABLED, enabled)
                .putString(KEY_ROUTE_PATH_VARIATION_AMPLITUDE, normalize(amplitude, "1.0"))
                .apply();
    }

    public void saveRouteAltitudeVariationSettings(
            boolean enabled,
            String baseMeters,
            String range,
            String heightCm,
            float probability
    ) {
        preferences.edit()
                .putBoolean(KEY_ROUTE_ALTITUDE_VARIATION_ENABLED, enabled)
                .putString(KEY_ROUTE_ALTITUDE_BASE_METERS, normalize(baseMeters, "55.0"))
                .putString(KEY_ROUTE_ALTITUDE_VARIATION_RANGE, normalize(range, "0.6"))
                .putString(KEY_ROUTE_ALTITUDE_VARIATION_HEIGHT_CM, normalize(heightCm, "170"))
                .putFloat(KEY_ROUTE_ALTITUDE_VARIATION_PROBABILITY, clampFrequency(probability))
                .apply();
    }

    public void saveRouteStepsPerMeter(String stepsPerMeter) {
        preferences.edit()
                .putString(KEY_ROUTE_STEPS_PER_METER, normalize(stepsPerMeter, "1"))
                .apply();
    }

    public void setRouteCompletionPending(boolean pending) {
        preferences.edit()
                .putBoolean(KEY_ROUTE_COMPLETION_PENDING, pending)
                .apply();
    }

    public void saveRouteFloatingWindowSettings(boolean enabled, float scale) {
        saveRouteFloatingWindowSettings(enabled, scale, getRouteFloatingWindowButtonSizeDp());
    }

    public void saveRouteFloatingWindowSettings(boolean enabled, float scale, float buttonSizeDp) {
        preferences.edit()
                .putBoolean(KEY_ROUTE_FLOATING_WINDOW_ENABLED, enabled)
                .putFloat(KEY_ROUTE_FLOATING_WINDOW_SCALE, clampFloatingScale(scale))
                .putFloat(KEY_ROUTE_FLOATING_WINDOW_BUTTON_SIZE, clampFloatingButtonSize(buttonSizeDp))
                .apply();
    }

    public void saveNmeaSettings(
            int satelliteCount,
            int signalQuality,
            float hdop,
            int updateIntervalMillis,
            boolean networkSimulationEnabled
    ) {
        preferences.edit()
                .putInt(
                        KEY_NMEA_SATELLITE_COUNT,
                        clampInt(satelliteCount, MIN_NMEA_SATELLITE_COUNT, MAX_NMEA_SATELLITE_COUNT)
                )
                .putInt(
                        KEY_NMEA_SIGNAL_QUALITY,
                        clampInt(signalQuality, MIN_NMEA_SIGNAL_QUALITY, MAX_NMEA_SIGNAL_QUALITY)
                )
                .putFloat(KEY_NMEA_HDOP, clampFloat(hdop, MIN_NMEA_HDOP, MAX_NMEA_HDOP))
                .putInt(
                        KEY_LOCATION_UPDATE_INTERVAL_MS,
                        clampInt(
                                updateIntervalMillis,
                                MIN_LOCATION_UPDATE_INTERVAL_MS,
                                MAX_LOCATION_UPDATE_INTERVAL_MS
                        )
                )
                .putBoolean(KEY_NETWORK_SIMULATION_ENABLED, networkSimulationEnabled)
                .apply();
    }

    public String getNfcUrl() {
        return preferences.getString(KEY_NFC_URL, "");
    }

    public String getNfcPackageName() {
        return preferences.getString(KEY_NFC_PACKAGE, "");
    }

    public String getNfcSource() {
        return preferences.getString(KEY_NFC_SOURCE, "manual");
    }

    public void saveNfcConfig(String url, String packageName, String source) {
        preferences.edit()
                .putString(KEY_NFC_URL, normalize(url, ""))
                .putString(KEY_NFC_PACKAGE, normalize(packageName, ""))
                .putString(KEY_NFC_SOURCE, normalize(source, "manual"))
                .apply();
    }

    public List<SavedNfcConfig> getSavedNfcConfigs() {
        List<SavedNfcConfig> configs = new ArrayList<>();
        String raw = preferences.getString(KEY_NFC_SAVED_CONFIGS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                SavedNfcConfig config = new SavedNfcConfig(
                        item.optString("name", ""),
                        item.optString("url", ""),
                        item.optString("packageName", ""),
                        item.optString("source", "manual")
                );
                if (config.isComplete()) {
                    configs.add(config);
                }
            }
        } catch (Exception ignored) {
            // Ignore invalid cached data and treat as empty.
        }
        return configs;
    }

    public void saveSavedNfcConfig(SavedNfcConfig config) {
        if (config == null || !config.isComplete()) {
            return;
        }
        List<SavedNfcConfig> configs = getSavedNfcConfigs();
        boolean replaced = false;
        for (int index = 0; index < configs.size(); index++) {
            if (configs.get(index).getName().equalsIgnoreCase(config.getName())) {
                configs.set(index, config);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            configs.add(0, config);
        }
        persistSavedNfcConfigs(configs);
    }

    private void persistSavedNfcConfigs(List<SavedNfcConfig> configs) {
        JSONArray array = new JSONArray();
        if (configs != null) {
            for (SavedNfcConfig config : configs) {
                if (config == null || !config.isComplete()) {
                    continue;
                }
                JSONObject item = new JSONObject();
                try {
                    item.put("name", config.getName());
                    item.put("url", config.getUrl());
                    item.put("packageName", config.getPackageName());
                    item.put("source", normalize(config.getSource(), "manual"));
                    array.put(item);
                } catch (Exception ignored) {
                    // Skip malformed item.
                }
            }
        }
        preferences.edit().putString(KEY_NFC_SAVED_CONFIGS, array.toString()).apply();
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private float clampFrequency(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float clampFloatingScale(float value) {
        return Math.max(0.35f, Math.min(0.90f, value));
    }

    private float clampFloatingButtonSize(float value) {
        return Math.max(32f, Math.min(72f, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clampFloat(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return DEFAULT_NMEA_HDOP;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String normalizeRouteMode(String routeMode) {
        return ROUTE_MODE_CADENCE.equals(routeMode) ? ROUTE_MODE_CADENCE : ROUTE_MODE_SPEED;
    }
}

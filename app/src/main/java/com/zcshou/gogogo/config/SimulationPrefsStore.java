package com.zcshou.gogogo.config;

import android.content.Context;
import android.content.SharedPreferences;

public final class SimulationPrefsStore {
    private static final String PREFS_NAME = "simulation_prefs_store";
    private static final String KEY_ROUTE_SPEED = "route_speed";
    private static final String KEY_ROUTE_LOOP_COUNT = "route_loop_count";
    private static final String KEY_ROUTE_SPEED_FLOAT = "route_speed_float";
    private static final String KEY_ROUTE_LAST_ID = "route_last_id";
    private static final String KEY_NFC_URL = "nfc_url";
    private static final String KEY_NFC_PACKAGE = "nfc_package";
    private static final String KEY_NFC_SOURCE = "nfc_source";

    private final SharedPreferences preferences;

    public SimulationPrefsStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getRouteSpeed() {
        return preferences.getString(KEY_ROUTE_SPEED, "3.8");
    }

    public String getRouteLoopCount() {
        return preferences.getString(KEY_ROUTE_LOOP_COUNT, "10");
    }

    public boolean isRouteSpeedFloat() {
        return preferences.getBoolean(KEY_ROUTE_SPEED_FLOAT, true);
    }

    public String getLastRouteId() {
        return preferences.getString(KEY_ROUTE_LAST_ID, "");
    }

    public void saveRouteConfig(String speed, String loopCount, boolean speedFloat, String routeId) {
        preferences.edit()
                .putString(KEY_ROUTE_SPEED, normalize(speed, "3.8"))
                .putString(KEY_ROUTE_LOOP_COUNT, normalize(loopCount, "10"))
                .putBoolean(KEY_ROUTE_SPEED_FLOAT, speedFloat)
                .putString(KEY_ROUTE_LAST_ID, normalize(routeId, ""))
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

    private String normalize(String value, String fallback) {
        return value == null ? fallback : value.trim();
    }
}

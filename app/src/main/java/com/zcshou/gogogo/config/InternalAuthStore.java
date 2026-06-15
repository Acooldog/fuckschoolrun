package com.acooldog.toolbox.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;

public final class InternalAuthStore {
    private static final String PREFS_NAME = "internal_auth_store";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REMARK = "remark";
    private static final String KEY_TESTER_TYPE = "tester_type";
    private static final String KEY_TESTER_TYPE_LABEL = "tester_type_label";
    private static final String KEY_STATUS = "status";

    private final SharedPreferences preferences;

    public InternalAuthStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, @Nullable InternalAccountProfile profile) {
        preferences.edit()
                .putString(KEY_TOKEN, normalize(token))
                .putString(KEY_ACCOUNT_ID, profile == null ? "" : normalize(profile.getId()))
                .putString(KEY_USERNAME, profile == null ? "" : normalize(profile.getUsername()))
                .putString(KEY_REMARK, profile == null ? "" : normalize(profile.getRemark()))
                .putString(KEY_TESTER_TYPE, profile == null ? "" : normalize(profile.getTesterType()))
                .putString(KEY_TESTER_TYPE_LABEL, profile == null ? "" : normalize(profile.getTesterTypeLabel()))
                .putString(KEY_STATUS, profile == null ? "" : normalize(profile.getStatus()))
                .apply();
    }

    public void saveProfile(@Nullable InternalAccountProfile profile) {
        saveSession(getToken(), profile);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, "");
    }

    public boolean isLoggedIn() {
        return !getToken().isEmpty() && getProfile() != null;
    }

    @Nullable
    public InternalAccountProfile getProfile() {
        String username = preferences.getString(KEY_USERNAME, "");
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return new InternalAccountProfile(
                preferences.getString(KEY_ACCOUNT_ID, ""),
                username,
                preferences.getString(KEY_REMARK, ""),
                preferences.getString(KEY_TESTER_TYPE, ""),
                preferences.getString(KEY_TESTER_TYPE_LABEL, ""),
                preferences.getString(KEY_STATUS, "")
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

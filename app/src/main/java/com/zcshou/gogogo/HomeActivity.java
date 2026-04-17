package com.zcshou.gogogo;

import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.zcshou.utils.GoUtils;

public class HomeActivity extends BaseActivity {
    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        bindNavigation();
    }

    private void bindNavigation() {
        findViewById(R.id.card_operation_tips).setOnClickListener(v -> open(OperationTipsActivity.class));
        findViewById(R.id.card_nfc_tools).setOnClickListener(v -> open(NfcToolsActivity.class));
        findViewById(R.id.card_route_run).setOnClickListener(v -> open(RouteRunActivity.class));
        findViewById(R.id.card_route_create).setOnClickListener(v -> open(RouteCreateActivity.class));
        findViewById(R.id.card_settings).setOnClickListener(v -> open(SettingsActivity.class));
        findViewById(R.id.card_dev).setOnClickListener(this::openDeveloperSettings);
    }

    private void open(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    private void openDeveloperSettings(View ignored) {
        if (Settings.Global.getInt(getContentResolver(), "development_settings_enabled", 0) != 1) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_dev));
            return;
        }

        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_dev));
        }
    }
}

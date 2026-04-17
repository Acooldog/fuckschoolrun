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
        findViewById(R.id.card_nfc_tools).setOnClickListener(v -> showNfcActions());
        findViewById(R.id.card_route_run).setOnClickListener(v -> open(RouteRunActivity.class));
        findViewById(R.id.card_route_create).setOnClickListener(v -> open(RouteCreateActivity.class));
        findViewById(R.id.card_settings).setOnClickListener(v -> open(SettingsActivity.class));
        findViewById(R.id.card_dev).setOnClickListener(this::openDeveloperSettings);
    }

    private void showNfcActions() {
        String[] actions = {
                getString(R.string.home_nfc_open_action),
                getString(R.string.home_nfc_share_action),
                getString(R.string.home_nfc_download_action)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.home_nfc_action_title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        open(NfcToolsActivity.class);
                    } else if (which == 1) {
                        openNfcAction(NfcToolsActivity.ACTION_SHARE);
                    } else {
                        openNfcAction(NfcToolsActivity.ACTION_DOWNLOAD);
                    }
                })
                .show();
    }

    private void open(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    private void openNfcAction(String action) {
        Intent intent = new Intent(this, NfcToolsActivity.class);
        intent.putExtra(NfcToolsActivity.EXTRA_START_ACTION, action);
        startActivity(intent);
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

package com.acooldog.toolbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.acooldog.toolbox.utils.GoUtils;
import com.acooldog.toolbox.update.GiteeReleaseChecker;
import com.acooldog.toolbox.update.GiteeReleaseInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.noties.markwon.Markwon;
import okhttp3.OkHttpClient;

public class HomeActivity extends BaseActivity {
    private static final String PREF_IGNORED_RELEASE = "pref_ignored_gitee_release";

    private ExecutorService ioExecutor;
    private SharedPreferences sharedPreferences;
    private OkHttpClient okHttpClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ioExecutor = Executors.newSingleThreadExecutor();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        okHttpClient = new OkHttpClient();

        bindNavigation();
        checkGiteeReleaseUpdate();
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
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

    private void checkGiteeReleaseUpdate() {
        if (!GoUtils.isNetworkAvailable(this)) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                GiteeReleaseInfo releaseInfo = new GiteeReleaseChecker(okHttpClient).fetchLatestRelease();
                if (releaseInfo == null) {
                    return;
                }

                String currentVersion = GoUtils.getVersionName(this);
                if (currentVersion == null) {
                    return;
                }

                String ignoredTag = sharedPreferences.getString(PREF_IGNORED_RELEASE, "");
                boolean newer = new GiteeReleaseChecker(okHttpClient).isNewerThan(releaseInfo.getTagName(), currentVersion);
                if (!newer || releaseInfo.getTagName().equals(ignoredTag)) {
                    return;
                }

                runOnUiThread(() -> showReleaseUpdateDialog(releaseInfo));
            } catch (Exception ignored) {
                // Ignore update check failures silently.
            }
        });
    }

    private void showReleaseUpdateDialog(GiteeReleaseInfo releaseInfo) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_release_update, null);
        TextView versionView = dialogView.findViewById(R.id.update_release_version);
        TextView changelogView = dialogView.findViewById(R.id.update_release_changelog);

        versionView.setText(getString(R.string.update_dialog_version, releaseInfo.getTagName()));
        Markwon.create(this).setMarkdown(
                changelogView,
                releaseInfo.getChangelog().isEmpty() ? getString(R.string.update_dialog_empty_log) : releaseInfo.getChangelog()
        );

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.update_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.update_dialog_download, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.getDownloadUrl()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.update_dialog_acknowledged, (dialog, which) ->
                        sharedPreferences.edit().putString(PREF_IGNORED_RELEASE, releaseInfo.getTagName()).apply())
                .show();
    }
}

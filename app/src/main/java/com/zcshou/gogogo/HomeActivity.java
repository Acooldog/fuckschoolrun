package com.acooldog.toolbox;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import androidx.annotation.Nullable;

import com.acooldog.toolbox.service.ServiceGo;
import com.acooldog.toolbox.ui.CupertinoDialogs;
import com.acooldog.toolbox.update.GiteeReleaseChecker;
import com.acooldog.toolbox.update.GiteeReleaseInfo;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class HomeActivity extends RouteCreateActivity {
    private static final String PREF_IGNORED_RELEASE = "pref_ignored_gitee_release";
    private static final String PREF_LAST_AUTO_CHECK_VERSION = "pref_last_auto_check_version";

    private ExecutorService ioExecutor;
    private android.content.SharedPreferences sharedPreferences;
    private OkHttpClient okHttpClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        sendBroadcast(new Intent(ServiceGo.ACTION_STOP_MOCK_LOCATION)
                .setPackage(getPackageName()));
        stopService(new Intent(this, ServiceGo.class));
        super.onCreate(savedInstanceState);
        ioExecutor = Executors.newSingleThreadExecutor();
        sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        okHttpClient = new OkHttpClient();

        checkGiteeReleaseUpdate(false);
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    public void onCheckUpdateClicked() {
        checkGiteeReleaseUpdate(true);
    }

    public void onSettingsClicked() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void onInternalLoginClicked() {
        startActivity(new Intent(this, InternalLoginActivity.class));
    }

    public void onDeveloperOptionsClicked() {
        openDeveloperSettings(null);
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

    private void checkGiteeReleaseUpdate(boolean manual) {
        String currentVersion = GoUtils.getVersionName(this);
        if (currentVersion == null) {
            return;
        }
        if (!manual) {
            String checkedVersion = sharedPreferences.getString(PREF_LAST_AUTO_CHECK_VERSION, "");
            if (currentVersion.equals(checkedVersion)) {
                return;
            }
        }
        if (!GoUtils.isNetworkAvailable(this)) {
            if (manual) {
                GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            }
            return;
        }
        if (manual) {
            GoUtils.DisplayToast(this, getString(R.string.update_checking));
        }
        ioExecutor.execute(() -> {
            try {
                GiteeReleaseChecker checker = new GiteeReleaseChecker(okHttpClient);
                GiteeReleaseInfo releaseInfo = checker.fetchLatestRelease();
                sharedPreferences.edit().putString(PREF_LAST_AUTO_CHECK_VERSION, currentVersion).apply();
                if (releaseInfo == null) {
                    if (manual) {
                        runOnUiThread(() -> GoUtils.DisplayToast(this, getString(R.string.update_check_failed)));
                    }
                    return;
                }

                String ignoredTag = sharedPreferences.getString(PREF_IGNORED_RELEASE, "");
                boolean newer = checker.isNewerThan(releaseInfo.getTagName(), currentVersion);
                if (!newer) {
                    if (manual) {
                        runOnUiThread(() -> GoUtils.DisplayToast(this, getString(R.string.update_last)));
                    }
                    return;
                }
                if (!manual && releaseInfo.getTagName().equals(ignoredTag)) {
                    return;
                }

                runOnUiThread(() -> showReleaseUpdateDialog(releaseInfo));
            } catch (Exception exception) {
                if (manual) {
                    runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.update_check_failed, exception)));
                }
            }
        });
    }

    private void showReleaseUpdateDialog(GiteeReleaseInfo releaseInfo) {
        CupertinoDialogs.showReleaseUpdate(
                this,
                getString(R.string.update_dialog_title),
                getString(R.string.update_dialog_version, releaseInfo.getTagName()),
                releaseInfo.getChangelog().isEmpty()
                        ? getString(R.string.update_dialog_empty_log)
                        : releaseInfo.getChangelog(),
                getString(R.string.update_dialog_download),
                getString(R.string.update_dialog_acknowledged),
                () -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.getDownloadUrl()));
                    startActivity(intent);
                },
                () -> sharedPreferences.edit()
                        .putString(PREF_IGNORED_RELEASE, releaseInfo.getTagName())
                        .apply()
        );
    }

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }
}

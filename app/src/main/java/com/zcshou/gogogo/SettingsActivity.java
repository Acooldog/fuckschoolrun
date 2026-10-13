package com.acooldog.toolbox;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.acooldog.toolbox.ui.CupertinoDialogs;
import com.acooldog.toolbox.ui.CupertinoSettingsView;
import com.acooldog.toolbox.update.GiteeReleaseChecker;
import com.acooldog.toolbox.update.GiteeReleaseInfo;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class SettingsActivity extends BaseActivity {
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private boolean checkingUpdate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new CupertinoSettingsView(this, this::checkForUpdate));
    }

    @Override
    protected void onDestroy() {
        ioExecutor.shutdownNow();
        okHttpClient.dispatcher().cancelAll();
        super.onDestroy();
    }

    private void checkForUpdate() {
        if (checkingUpdate) {
            return;
        }
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        checkingUpdate = true;
        GoUtils.DisplayToast(this, getString(R.string.update_checking));
        String currentVersion = GoUtils.getVersionName(this);
        ioExecutor.execute(() -> {
            try {
                GiteeReleaseChecker checker = new GiteeReleaseChecker(okHttpClient);
                GiteeReleaseInfo release = checker.fetchLatestRelease();
                runOnUiThread(() -> {
                    checkingUpdate = false;
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (release == null) {
                        GoUtils.DisplayToast(this, getString(R.string.update_check_failed));
                    } else if (!checker.isNewerThan(release.getTagName(), currentVersion)) {
                        GoUtils.DisplayToast(this, getString(R.string.update_last));
                    } else {
                        showUpdate(release);
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    checkingUpdate = false;
                    if (!isFinishing() && !isDestroyed()) {
                        GoUtils.DisplayToast(this, getString(R.string.update_check_failed));
                    }
                });
            }
        });
    }

    private void showUpdate(GiteeReleaseInfo release) {
        CupertinoDialogs.showReleaseUpdate(
                this,
                getString(R.string.update_dialog_title),
                getString(R.string.update_dialog_version, release.getTagName()),
                release.getChangelog().isEmpty()
                        ? getString(R.string.update_dialog_empty_log)
                        : release.getChangelog(),
                getString(R.string.update_dialog_download),
                getString(R.string.update_dialog_acknowledged),
                () -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(release.getDownloadUrl()))),
                () -> PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString("pref_ignored_gitee_release", release.getTagName())
                        .apply()
        );
    }
}

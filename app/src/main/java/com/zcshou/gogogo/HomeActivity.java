package com.acooldog.toolbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.acooldog.toolbox.home.HomeMineFragment;
import com.acooldog.toolbox.home.HomeStartFragment;
import com.acooldog.toolbox.update.GiteeReleaseChecker;
import com.acooldog.toolbox.update.GiteeReleaseInfo;
import com.acooldog.toolbox.utils.GoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.noties.markwon.Markwon;
import okhttp3.OkHttpClient;

public class HomeActivity extends BaseActivity implements HomeMineFragment.Actions {
    private static final String PREF_IGNORED_RELEASE = "pref_ignored_gitee_release";
    private static final String PREF_LAST_AUTO_CHECK_VERSION = "pref_last_auto_check_version";
    private static final String KEY_SELECTED_TAB = "selected_tab";

    private MaterialToolbar toolbar;
    private ExecutorService ioExecutor;
    private SharedPreferences sharedPreferences;
    private OkHttpClient okHttpClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ioExecutor = Executors.newSingleThreadExecutor();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        okHttpClient = new OkHttpClient();

        BottomNavigationView bottomNavigationView = findViewById(R.id.home_bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switchTab(item.getItemId());
            return true;
        });

        int selectedTab = savedInstanceState == null
                ? R.id.nav_home_start
                : savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_home_start);
        bottomNavigationView.setSelectedItemId(selectedTab);

        checkGiteeReleaseUpdate(false);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        BottomNavigationView bottomNavigationView = findViewById(R.id.home_bottom_nav);
        outState.putInt(KEY_SELECTED_TAB, bottomNavigationView.getSelectedItemId());
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void switchTab(@IdRes int itemId) {
        if (itemId == R.id.nav_home_mine) {
            toolbar.setTitle(R.string.home_tab_mine);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.home_fragment_container, HomeMineFragment.newInstance())
                    .commit();
            return;
        }

        toolbar.setTitle(R.string.home_tab_start);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.home_fragment_container, HomeStartFragment.newInstance())
                .commit();
    }

    private void open(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    @Override
    public void onCheckUpdateClicked() {
        checkGiteeReleaseUpdate(true);
    }

    @Override
    public void onSettingsClicked() {
        open(SettingsActivity.class);
    }

    @Override
    public void onInternalLoginClicked() {
        open(InternalLoginActivity.class);
    }

    @Override
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_release_update, null);
        android.widget.TextView versionView = dialogView.findViewById(R.id.update_release_version);
        android.widget.TextView changelogView = dialogView.findViewById(R.id.update_release_changelog);

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

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }
}

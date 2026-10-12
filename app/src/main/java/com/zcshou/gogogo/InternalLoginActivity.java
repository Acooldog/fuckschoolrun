package com.acooldog.toolbox;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.FsrBindingState;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;
import com.acooldog.toolbox.share.domain.model.InternalLoginResult;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.ui.CupertinoInternalLoginView;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternalLoginActivity extends BaseActivity {
    private InternalAuthStore authStore;
    private ExecutorService ioExecutor;
    private CupertinoInternalLoginView contentView;
    private String deviceId;
    private String deviceName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authStore = new InternalAuthStore(getApplicationContext());
        ioExecutor = Executors.newSingleThreadExecutor();
        deviceId = resolveDeviceId();
        deviceName = resolveDeviceName();

        contentView = new CupertinoInternalLoginView(this);
        contentView.setActions(new CupertinoInternalLoginView.Actions() {
            @Override
            public void onBack() {
                finish();
            }

            @Override
            public void onLogin() {
                login();
            }

            @Override
            public void onLogout() {
                logout();
            }

            @Override
            public void onRegister() {
                startActivity(new Intent(
                        InternalLoginActivity.this,
                        EmailRegisterActivity.class
                ));
            }
        });
        setContentView(contentView);
        refreshSessionState();
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void refreshSessionState() {
        InternalAccountProfile profile = authStore.getProfile();
        if (profile == null) {
            contentView.render(
                    getString(R.string.internal_login_status_logged_out),
                    contentView.getUsername(),
                    deviceId,
                    deviceName,
                    false
            );
            return;
        }
        contentView.render(
                getString(
                        R.string.internal_login_status_logged_in,
                        profile.getUsername(),
                        profile.getTesterTypeLabel()
                ),
                profile.getUsername(),
                deviceId,
                deviceName,
                true
        );
    }

    private void login() {
        String username = contentView.getUsername();
        String password = contentView.getPassword();
        if (TextUtils.isEmpty(username)) {
            GoUtils.DisplayToast(this, getString(R.string.internal_login_need_username));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            GoUtils.DisplayToast(this, getString(R.string.internal_login_need_password));
            return;
        }
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }

        contentView.setLoading(true);
        contentView.setStatus(getString(R.string.tips_loading));
        ioExecutor.execute(() -> {
            try {
                InternalLoginResult result = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .loginInternalAccount(username, password, deviceId, deviceName);
                authStore.saveSession(result.getToken(), result.getAccount());
                runOnUiThread(() -> {
                    contentView.setLoading(false);
                    contentView.setPassword("");
                    FsrBindingState binding = result.getFsrBinding();
                    if (binding != null && !TextUtils.isEmpty(binding.getCommand())) {
                        Intent intent = new Intent(this, FsrBindingActivity.class);
                        intent.putExtra(FsrBindingActivity.EXTRA_COMMAND, binding.getCommand());
                        intent.putExtra(FsrBindingActivity.EXTRA_JOB_ID, binding.getJobId());
                        startActivity(intent);
                        finish();
                        return;
                    }
                    refreshSessionState();
                    GoUtils.DisplayToast(this, getString(R.string.internal_login_success));
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    contentView.setLoading(false);
                    refreshSessionState();
                    GoUtils.DisplayToast(
                            this,
                            buildDetailedToast(R.string.internal_login_failed, exception)
                    );
                });
            }
        });
    }

    private void logout() {
        authStore.clear();
        contentView.setPassword("");
        refreshSessionState();
        GoUtils.DisplayToast(this, getString(R.string.internal_logout_success));
    }

    private String resolveDeviceId() {
        String value = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return TextUtils.isEmpty(value) ? Build.MODEL + "_unknown" : value;
    }

    private String resolveDeviceName() {
        return (Build.BRAND + " " + Build.MODEL).trim();
    }

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null
                ? ""
                : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }
}

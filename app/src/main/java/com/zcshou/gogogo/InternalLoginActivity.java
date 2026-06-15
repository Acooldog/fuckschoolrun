package com.acooldog.toolbox;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;
import com.acooldog.toolbox.share.domain.model.InternalLoginResult;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternalLoginActivity extends BaseActivity {
    private InternalAuthStore authStore;
    private ExecutorService ioExecutor;
    private TextView statusView;
    private TextView deviceIdView;
    private TextView deviceNameView;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private String deviceId;
    private String deviceName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internal_login);

        authStore = new InternalAuthStore(getApplicationContext());
        ioExecutor = Executors.newSingleThreadExecutor();
        deviceId = resolveDeviceId();
        deviceName = resolveDeviceName();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_internal_login);
        toolbar.setNavigationOnClickListener(v -> finish());

        statusView = findViewById(R.id.internal_login_status);
        deviceIdView = findViewById(R.id.internal_login_device_id);
        deviceNameView = findViewById(R.id.internal_login_device_name);
        usernameInput = findViewById(R.id.internal_login_username_input);
        passwordInput = findViewById(R.id.internal_login_password_input);

        deviceIdView.setText(deviceId);
        deviceNameView.setText(deviceName);

        findViewById(R.id.internal_login_button).setOnClickListener(v -> login());
        findViewById(R.id.internal_logout_button).setOnClickListener(v -> logout());

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
            statusView.setText(getString(R.string.internal_login_status_logged_out));
            return;
        }
        statusView.setText(getString(
                R.string.internal_login_status_logged_in,
                profile.getUsername(),
                profile.getTesterTypeLabel()
        ));
        if (usernameInput != null) {
            usernameInput.setText(profile.getUsername());
        }
    }

    private void login() {
        String username = usernameInput.getText() == null ? "" : usernameInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString().trim();
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

        statusView.setText(getString(R.string.tips_loading));
        ioExecutor.execute(() -> {
            try {
                InternalLoginResult result = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .loginInternalAccount(username, password, deviceId, deviceName);
                authStore.saveSession(result.getToken(), result.getAccount());
                runOnUiThread(() -> {
                    if (passwordInput != null) {
                        passwordInput.setText("");
                    }
                    refreshSessionState();
                    GoUtils.DisplayToast(this, getString(R.string.internal_login_success));
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    refreshSessionState();
                    GoUtils.DisplayToast(this, buildDetailedToast(R.string.internal_login_failed, exception));
                });
            }
        });
    }

    private void logout() {
        authStore.clear();
        if (passwordInput != null) {
            passwordInput.setText("");
        }
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
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }
}

package com.acooldog.toolbox;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.EmailVerificationState;
import com.acooldog.toolbox.share.domain.model.InternalLoginResult;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailRegisterActivity extends BaseActivity {
    private static final String PREFS_NAME = "email_registration_state";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DEADLINE = "deadline";

    private InternalAuthStore authStore;
    private ExecutorService ioExecutor;
    private TextInputEditText emailInput;
    private TextInputEditText codeInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button sendCodeButton;
    private TextView statusView;
    private long cooldownDeadline;
    private String cooldownEmail = "";

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            handler.postDelayed(this, 500L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_register);

        authStore = new InternalAuthStore(getApplicationContext());
        ioExecutor = Executors.newSingleThreadExecutor();
        emailInput = findViewById(R.id.email_register_email);
        codeInput = findViewById(R.id.email_register_code);
        passwordInput = findViewById(R.id.email_register_password);
        confirmPasswordInput = findViewById(R.id.email_register_password_confirm);
        sendCodeButton = findViewById(R.id.email_register_send_code);
        statusView = findViewById(R.id.email_register_status);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_email_register);
        toolbar.setNavigationOnClickListener(v -> finish());
        sendCodeButton.setOnClickListener(v -> sendCode());
        findViewById(R.id.email_register_submit).setOnClickListener(v -> register());

        restoreSavedState();
        handler.post(countdownRunnable);
        refreshRemoteCooldown();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(countdownRunnable);
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void sendCode() {
        String email = read(emailInput);
        if (TextUtils.isEmpty(email)) {
            toast(R.string.email_register_need_email);
            return;
        }
        if (email.equalsIgnoreCase(cooldownEmail) && cooldownDeadline > System.currentTimeMillis()) {
            updateCountdown();
            return;
        }
        if (!GoUtils.isNetworkAvailable(this)) {
            toast(R.string.app_error_network);
            return;
        }
        sendCodeButton.setEnabled(false);
        statusView.setText(R.string.tips_loading);
        ioExecutor.execute(() -> {
            try {
                EmailVerificationState state = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .sendEmailRegistrationCode(email);
                long deadline = System.currentTimeMillis() + state.getCooldownSeconds() * 1000L;
                saveCooldown(email, deadline);
                runOnUiThread(() -> {
                    if (state.isAccepted()) {
                        statusView.setText(R.string.email_register_code_sent);
                    } else {
                        statusView.setText(getString(
                                R.string.email_register_code_cooldown,
                                Math.max(1, state.getCooldownSeconds())
                        ));
                    }
                    updateCountdown();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    sendCodeButton.setEnabled(true);
                    statusView.setText(buildDetailedMessage(R.string.email_register_failed, exception));
                });
            }
        });
    }

    private void register() {
        String email = read(emailInput);
        String code = read(codeInput);
        String password = read(passwordInput);
        String confirm = read(confirmPasswordInput);
        if (TextUtils.isEmpty(email)) {
            toast(R.string.email_register_need_email);
            return;
        }
        if (TextUtils.isEmpty(code)) {
            toast(R.string.email_register_need_code);
            return;
        }
        if (password.length() < 8) {
            toast(R.string.email_register_need_password);
            return;
        }
        if (!password.equals(confirm)) {
            toast(R.string.email_register_password_mismatch);
            return;
        }
        if (!GoUtils.isNetworkAvailable(this)) {
            toast(R.string.app_error_network);
            return;
        }

        findViewById(R.id.email_register_submit).setEnabled(false);
        ioExecutor.execute(() -> {
            try {
                InternalLoginResult result = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .registerByEmail(email, code, password, resolveDeviceId(), resolveDeviceName());
                authStore.saveSession(result.getToken(), result.getAccount());
                runOnUiThread(() -> {
                    toast(R.string.email_register_success);
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    findViewById(R.id.email_register_submit).setEnabled(true);
                    statusView.setText(buildDetailedMessage(R.string.email_register_failed, exception));
                });
            }
        });
    }

    private void refreshRemoteCooldown() {
        String email = read(emailInput);
        if (TextUtils.isEmpty(email) || !GoUtils.isNetworkAvailable(this)) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                EmailVerificationState state = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getEmailRegistrationStatus(email);
                if (!state.canSend()) {
                    saveCooldown(email, System.currentTimeMillis() + state.getCooldownSeconds() * 1000L);
                }
                runOnUiThread(this::updateCountdown);
            } catch (Exception ignored) {
                // The local deadline remains a fallback; the server still enforces the cooldown.
            }
        });
    }

    private void restoreSavedState() {
        android.content.SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        emailInput.setText(preferences.getString(KEY_EMAIL, ""));
        cooldownEmail = preferences.getString(KEY_EMAIL, "");
        cooldownDeadline = preferences.getLong(KEY_DEADLINE, 0L);
    }

    private void saveCooldown(String email, long deadline) {
        cooldownDeadline = Math.max(0L, deadline);
        cooldownEmail = email == null ? "" : email.trim();
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_EMAIL, email)
                .putLong(KEY_DEADLINE, cooldownDeadline)
                .apply();
    }

    private void updateCountdown() {
        String email = read(emailInput);
        if (!email.equalsIgnoreCase(cooldownEmail)) {
            sendCodeButton.setEnabled(true);
            sendCodeButton.setText(R.string.email_register_send_code);
            return;
        }
        long remaining = Math.max(0L, cooldownDeadline - System.currentTimeMillis());
        int seconds = (int) ((remaining + 999L) / 1000L);
        if (seconds <= 0) {
            sendCodeButton.setEnabled(true);
            sendCodeButton.setText(R.string.email_register_send_code);
            return;
        }
        sendCodeButton.setEnabled(false);
        sendCodeButton.setText(getString(R.string.email_register_send_again, seconds));
    }

    private String read(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String resolveDeviceId() {
        String value = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return TextUtils.isEmpty(value) ? Build.MODEL + "_unknown" : value;
    }

    private String resolveDeviceName() {
        return (Build.BRAND + " " + Build.MODEL).trim();
    }

    private void toast(int resourceId) {
        GoUtils.DisplayToast(this, getString(resourceId));
    }

    private String buildDetailedMessage(int prefixId, Exception exception) {
        String detail = exception == null ? "" : exception.getMessage();
        return getString(prefixId) + (TextUtils.isEmpty(detail) ? "" : " " + detail);
    }
}

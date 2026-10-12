package com.acooldog.toolbox;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.EmailVerificationState;
import com.acooldog.toolbox.share.domain.model.FsrBindingState;
import com.acooldog.toolbox.share.domain.model.InternalLoginResult;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.ui.CupertinoEmailRegisterView;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailRegisterActivity extends BaseActivity {
    private static final String PREFS_NAME = "email_registration_state";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DEADLINE = "deadline";

    private InternalAuthStore authStore;
    private ExecutorService ioExecutor;
    private CupertinoEmailRegisterView contentView;
    private long cooldownDeadline;
    private String cooldownEmail = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
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

        authStore = new InternalAuthStore(getApplicationContext());
        ioExecutor = Executors.newSingleThreadExecutor();
        contentView = new CupertinoEmailRegisterView(this);
        contentView.setActions(new CupertinoEmailRegisterView.Actions() {
            @Override
            public void onBack() {
                finish();
            }

            @Override
            public void onSendCode() {
                sendCode();
            }

            @Override
            public void onRegister() {
                register();
            }
        });
        setContentView(contentView);

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
        String email = contentView.getEmail();
        if (TextUtils.isEmpty(email)) {
            toast(R.string.email_register_need_email);
            return;
        }
        if (email.equalsIgnoreCase(cooldownEmail)
                && cooldownDeadline > System.currentTimeMillis()) {
            updateCountdown();
            return;
        }
        if (!GoUtils.isNetworkAvailable(this)) {
            toast(R.string.app_error_network);
            return;
        }
        contentView.setSendCodeEnabled(false);
        contentView.setStatus(getString(R.string.tips_loading));
        ioExecutor.execute(() -> {
            try {
                EmailVerificationState state = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .sendEmailRegistrationCode(email);
                long deadline = System.currentTimeMillis()
                        + state.getCooldownSeconds() * 1000L;
                saveCooldown(email, deadline);
                runOnUiThread(() -> {
                    if (state.isAccepted()) {
                        contentView.setStatus(getString(R.string.email_register_code_sent));
                    } else {
                        contentView.setStatus(getString(
                                R.string.email_register_code_cooldown,
                                Math.max(1, state.getCooldownSeconds())
                        ));
                    }
                    updateCountdown();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    contentView.setSendCodeEnabled(true);
                    contentView.setStatus(
                            buildDetailedMessage(R.string.email_register_failed, exception)
                    );
                });
            }
        });
    }

    private void register() {
        String email = contentView.getEmail();
        String code = contentView.getCode();
        String password = contentView.getPassword();
        String confirm = contentView.getConfirmPassword();
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

        contentView.setRegisterEnabled(false);
        ioExecutor.execute(() -> {
            try {
                InternalLoginResult result = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .registerByEmail(
                                email,
                                code,
                                password,
                                resolveDeviceId(),
                                resolveDeviceName()
                        );
                authStore.saveSession(result.getToken(), result.getAccount());
                runOnUiThread(() -> {
                    FsrBindingState binding = result.getFsrBinding();
                    if (binding != null && !TextUtils.isEmpty(binding.getCommand())) {
                        Intent intent = new Intent(this, FsrBindingActivity.class);
                        intent.putExtra(FsrBindingActivity.EXTRA_COMMAND, binding.getCommand());
                        intent.putExtra(FsrBindingActivity.EXTRA_JOB_ID, binding.getJobId());
                        startActivity(intent);
                        finish();
                        return;
                    }
                    toast(R.string.email_register_success);
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    contentView.setRegisterEnabled(true);
                    contentView.setStatus(
                            buildDetailedMessage(R.string.email_register_failed, exception)
                    );
                });
            }
        });
    }

    private void refreshRemoteCooldown() {
        String email = contentView.getEmail();
        if (TextUtils.isEmpty(email) || !GoUtils.isNetworkAvailable(this)) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                EmailVerificationState state = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getEmailRegistrationStatus(email);
                if (!state.canSend()) {
                    saveCooldown(
                            email,
                            System.currentTimeMillis() + state.getCooldownSeconds() * 1000L
                    );
                }
                runOnUiThread(this::updateCountdown);
            } catch (Exception ignored) {
                // The local deadline remains a fallback; the server still enforces the cooldown.
            }
        });
    }

    private void restoreSavedState() {
        android.content.SharedPreferences preferences =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        contentView.setEmail(preferences.getString(KEY_EMAIL, ""));
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
        String email = contentView.getEmail();
        if (!email.equalsIgnoreCase(cooldownEmail)) {
            contentView.setSendCodeEnabled(true);
            contentView.setSendCodeLabel(getString(R.string.email_register_send_code));
            return;
        }
        long remaining = Math.max(0L, cooldownDeadline - System.currentTimeMillis());
        int seconds = (int) ((remaining + 999L) / 1000L);
        if (seconds <= 0) {
            contentView.setSendCodeEnabled(true);
            contentView.setSendCodeLabel(getString(R.string.email_register_send_code));
            return;
        }
        contentView.setSendCodeEnabled(false);
        contentView.setSendCodeLabel(
                getString(R.string.email_register_send_again, seconds)
        );
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

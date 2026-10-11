package com.acooldog.toolbox;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.AppClientConfig;
import com.acooldog.toolbox.share.domain.model.StartupDialogConfig;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class WelcomeActivity extends AppCompatActivity {
    private static final String LEGAL_PREFS_NAME = "KEY_ACCEPT_AGREEMENT";
    private static final String KEY_ACCEPT_AGREEMENT = "KEY_ACCEPT_AGREEMENT";
    private static final String KEY_ACCEPT_PRIVACY = "KEY_ACCEPT_PRIVACY";
    private static final String KEY_ACCEPT_DISCLAIMER = "KEY_ACCEPT_DISCLAIMER";
    private static final String KEY_STARTUP_DIALOG_REVISION = "KEY_STARTUP_DIALOG_REVISION";
    private static final int SDK_PERMISSION_REQUEST = 127;
    private static final long SPLASH_DURATION_MILLIS = 1000L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences preferences;
    private ExecutorService ioExecutor;
    private boolean agreementAccepted;
    private boolean disclaimerAccepted;
    private boolean startupFlowStarted;
    private View logoView;
    private View titleView;
    private View subtitleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        preferences = getSharedPreferences(LEGAL_PREFS_NAME, MODE_PRIVATE);
        ioExecutor = Executors.newSingleThreadExecutor();
        agreementAccepted = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false);
        disclaimerAccepted = preferences.getBoolean(
                KEY_ACCEPT_DISCLAIMER,
                preferences.getBoolean(KEY_ACCEPT_PRIVACY, false)
        );

        logoView = findViewById(R.id.welcome_logo);
        titleView = findViewById(R.id.welcome_title);
        subtitleView = findViewById(R.id.welcome_subtitle);

        startSplashAnimation();
        mainHandler.postDelayed(this::beginStartupFlow, SPLASH_DURATION_MILLIS);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SDK_PERMISSION_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                enterHome();
            } else {
                GoUtils.DisplayToast(this, getString(R.string.app_error_permission));
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startSplashAnimation() {
        if (logoView != null) {
            logoView.setAlpha(0f);
            logoView.setScaleX(0.82f);
            logoView.setScaleY(0.82f);
            logoView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(SPLASH_DURATION_MILLIS)
                    .start();
        }
        if (titleView != null) {
            titleView.setAlpha(0f);
            titleView.setTranslationY(36f);
            titleView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(SPLASH_DURATION_MILLIS)
                    .start();
        }
        if (subtitleView != null) {
            subtitleView.setAlpha(0f);
            subtitleView.setTranslationY(48f);
            subtitleView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(150L)
                    .setDuration(SPLASH_DURATION_MILLIS - 150L)
                    .start();
        }
    }

    private void beginStartupFlow() {
        if (startupFlowStarted || isFinishing() || isDestroyed()) {
            return;
        }
        startupFlowStarted = true;
        resolveStartupState((config, hardExit) -> {
            if (hardExit) {
                silentExit();
                return;
            }
            if (!agreementAccepted || !disclaimerAccepted) {
                showLegalConsentDialog(config);
                return;
            }
            showStartupDisclosureThenContinue(config);
        });
    }

    private void showLegalConsentDialog(@NonNull AppClientConfig config) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_legal_consent, null);
        TextView linksView = dialogView.findViewById(R.id.legal_consent_links);
        CheckBox consentCheckBox = dialogView.findViewById(R.id.legal_consent_checkbox);

        linksView.setMovementMethod(LinkMovementMethod.getInstance());
        linksView.setText(buildLegalLinkText(), TextView.BufferType.SPANNABLE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.app_error_agreement_dialog_title)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.legal_consent_confirm, null)
                .setNegativeButton(R.string.legal_consent_cancel, (ignored, which) -> finish())
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!consentCheckBox.isChecked()) {
                GoUtils.DisplayToast(this, getString(R.string.app_error_agreement));
                return;
            }
            saveAgreementAcceptance();
            dialog.dismiss();
            showStartupDisclosureThenContinue(config);
        }));
        dialog.show();
    }

    private SpannableStringBuilder buildLegalLinkText() {
        String text = getString(R.string.legal_consent_link_text);
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        applyClickableSpan(builder, text, "《用户协议》", () -> showDocumentDialog(R.string.app_agreement, R.string.app_agreement_content));
        applyClickableSpan(builder, text, "《免责声明》", () -> showDocumentDialog(R.string.app_privacy, R.string.app_privacy_content));
        return builder;
    }

    private void applyClickableSpan(
            SpannableStringBuilder builder,
            String fullText,
            String target,
            Runnable action
    ) {
        int start = fullText.indexOf(target);
        if (start < 0) {
            return;
        }
        int end = start + target.length();
        builder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                action.run();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.colorPrimary, getTheme()));
                ds.setUnderlineText(false);
            }
        }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void showDocumentDialog(@StringRes int titleResId, @StringRes int contentResId) {
        ScrollView scrollView = new ScrollView(this);
        int padding = Math.round(getResources().getDisplayMetrics().density * 20f);
        TextView contentView = new TextView(this);
        contentView.setPadding(padding, padding, padding, padding);
        contentView.setLineSpacing(0f, 1.25f);
        contentView.setText(getString(contentResId));
        scrollView.addView(contentView);

        new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setView(scrollView)
                .setPositiveButton(R.string.legal_document_close, null)
                .show();
    }

    private void saveAgreementAcceptance() {
        agreementAccepted = true;
        disclaimerAccepted = true;
        preferences.edit()
                .putBoolean(KEY_ACCEPT_AGREEMENT, true)
                .putBoolean(KEY_ACCEPT_PRIVACY, true)
                .putBoolean(KEY_ACCEPT_DISCLAIMER, true)
                .apply();
    }

    private void resolveStartupState(@NonNull StartupStateCallback callback) {
        ioExecutor.execute(() -> {
            AppClientConfig config = null;
            boolean hardExit = false;
            try {
                config = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getAppClientConfig();
                hardExit = config.isForceShutdown();
            } catch (Exception ignored) {
                // Configuration fetch failure uses the local disclosure defaults.
            }

            String token = new InternalAuthStore(getApplicationContext()).getToken();
            if (!hardExit && !token.trim().isEmpty()) {
                try {
                    ShareModule.from(getApplicationContext())
                            .shareApiClient()
                            .getInternalAccountProfile(token);
                } catch (Exception exception) {
                    hardExit = isBanFailure(exception);
                }
            }

            AppClientConfig finalConfig = config == null
                    ? new AppClientConfig("", "", "", "", "", false, "", new ArrayList<>())
                    : config;
            boolean finalHardExit = hardExit;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                callback.onReady(finalConfig, finalHardExit);
            });
        });
    }

    private void showStartupDisclosureThenContinue(@NonNull AppClientConfig remoteConfig) {
        long revision = remoteConfig.getStartupDialogRevision();
        if (preferences.getLong(KEY_STARTUP_DIALOG_REVISION, 0L) == revision) {
            ensurePermissionsThenEnterHome();
            return;
        }
        showPrimaryDialog(remoteConfig, 1);
    }

    private void showPrimaryDialog(@NonNull AppClientConfig remoteConfig, int cycleIndex) {
        StartupDialogConfig primary = findPrimary(remoteConfig, cycleIndex);
        List<StartupDialogConfig> secondaryDialogs = findSecondary(remoteConfig, cycleIndex);
        AlertDialog dialog = buildDialog(primary);
        AtomicBoolean secondaryShown = new AtomicBoolean(false);
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (secondaryShown.compareAndSet(false, true)) {
                shake(dialog.getWindow() == null ? null : dialog.getWindow().getDecorView());
                showSecondaryDialog(dialog, remoteConfig, cycleIndex, secondaryDialogs, 0);
            }
        }));
        dialog.show();
    }

    private void showSecondaryDialog(
            @NonNull AlertDialog primaryDialog,
            @NonNull AppClientConfig remoteConfig,
            int cycleIndex,
            @NonNull List<StartupDialogConfig> secondaryDialogs,
            int index
    ) {
        StartupDialogConfig secondary = secondaryDialogs.get(index);
        AlertDialog dialog = buildDialog(secondary);
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            dialog.dismiss();
            if (index + 1 < secondaryDialogs.size()) {
                showSecondaryDialog(primaryDialog, remoteConfig, cycleIndex, secondaryDialogs, index + 1);
                return;
            }
            primaryDialog.dismiss();
            if (cycleIndex < 3) {
                showPrimaryDialog(remoteConfig, cycleIndex + 1);
                return;
            }
            preferences.edit()
                    .putLong(KEY_STARTUP_DIALOG_REVISION, remoteConfig.getStartupDialogRevision())
                    .apply();
            ensurePermissionsThenEnterHome();
        }));
        dialog.show();
    }

    @NonNull
    private AlertDialog buildDialog(@NonNull StartupDialogConfig config) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setPositiveButton(config.getConfirmText(), null);
        if (!config.getTitle().isEmpty()) {
            builder.setTitle(config.getTitle());
        }
        if (!config.getMessage().isEmpty()) {
            builder.setMessage(config.getMessage());
        }
        return builder.create();
    }

    @NonNull
    private StartupDialogConfig findPrimary(@NonNull AppClientConfig remoteConfig, int cycleIndex) {
        for (StartupDialogConfig dialog : remoteConfig.getStartupDialogs()) {
            if (dialog.isPrimary() && dialog.getCycleIndex() == cycleIndex) {
                return dialog;
            }
        }
        return new StartupDialogConfig(
                "local-primary-" + cycleIndex,
                cycleIndex * 10,
                "primary",
                cycleIndex,
                getString(R.string.startup_primary_title),
                "",
                getString(R.string.startup_dialog_confirm),
                1L
        );
    }

    @NonNull
    private List<StartupDialogConfig> findSecondary(@NonNull AppClientConfig remoteConfig, int cycleIndex) {
        List<StartupDialogConfig> result = new ArrayList<>();
        for (StartupDialogConfig dialog : remoteConfig.getStartupDialogs()) {
            if (dialog.isSecondary() && dialog.getCycleIndex() == cycleIndex) {
                result.add(dialog);
            }
        }
        if (result.isEmpty()) {
            result.add(new StartupDialogConfig(
                    "local-secondary-" + cycleIndex,
                    cycleIndex * 10 + 1,
                    "secondary",
                    cycleIndex,
                    getString(R.string.startup_secondary_title),
                    getString(R.string.startup_secondary_message),
                    getString(R.string.startup_dialog_confirm),
                    1L
            ));
        }
        return result;
    }

    private void shake(@Nullable View view) {
        if (view == null) {
            return;
        }
        Animation animation = new TranslateAnimation(-12f, 12f, 0f, 0f);
        animation.setDuration(60L);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(3);
        view.startAnimation(animation);
    }

    private boolean isBanFailure(@NonNull Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("封禁") || message.toLowerCase().contains("banned");
    }

    private void silentExit() {
        finishAffinity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
    }

    private void ensurePermissionsThenEnterHome() {
        List<String> missingPermissions = getMissingPermissions();
        if (missingPermissions.isEmpty()) {
            enterHome();
            return;
        }
        requestPermissions(missingPermissions.toArray(new String[0]), SDK_PERMISSION_REQUEST);
    }

    @NonNull
    private List<String> getMissingPermissions() {
        List<String> missing = new ArrayList<>();
        collectMissingPermission(missing, Manifest.permission.ACCESS_FINE_LOCATION);
        collectMissingPermission(missing, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            collectMissingPermission(missing, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            collectMissingPermission(missing, Manifest.permission.POST_NOTIFICATIONS);
        }
        collectMissingPermission(missing, Manifest.permission.READ_PHONE_STATE);
        return missing;
    }

    private void collectMissingPermission(@NonNull List<String> missing, @NonNull String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            missing.add(permission);
        }
    }

    private boolean allPermissionsGranted(@NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void enterHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private interface StartupStateCallback {
        void onReady(@NonNull AppClientConfig config, boolean hardExit);
    }
}

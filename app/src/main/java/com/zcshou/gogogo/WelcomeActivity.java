package com.acooldog.toolbox;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.acooldog.toolbox.share.domain.model.AppClientConfig;
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
    private static final int SDK_PERMISSION_REQUEST = 127;
    private static final long SPLASH_DURATION_MILLIS = 1000L;
    private static final long REMOTE_NOTICE_TIMEOUT_MILLIS = 1500L;

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
        if (!agreementAccepted || !disclaimerAccepted) {
            showLegalConsentDialog();
            return;
        }
        showEntryNoticeThenContinue();
    }

    private void showLegalConsentDialog() {
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
            showEntryNoticeThenContinue();
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

    private void showEntryNoticeThenContinue() {
        resolveEntryNoticeContent(content -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            showEntryNoticeDialog(content);
        });
    }

    private void resolveEntryNoticeContent(@NonNull EntryNoticeCallback callback) {
        EntryNoticeContent fallback = EntryNoticeContent.local(this);
        if (!GoUtils.isNetworkAvailable(this)) {
            callback.onReady(fallback);
            return;
        }

        AtomicBoolean delivered = new AtomicBoolean(false);
        Runnable fallbackRunnable = () -> {
            if (delivered.compareAndSet(false, true)) {
                callback.onReady(fallback);
            }
        };
        mainHandler.postDelayed(fallbackRunnable, REMOTE_NOTICE_TIMEOUT_MILLIS);

        ioExecutor.execute(() -> {
            EntryNoticeContent resolved = fallback;
            try {
                AppClientConfig config = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getAppClientConfig();
                resolved = EntryNoticeContent.fromRemote(config, fallback);
            } catch (Exception ignored) {
                resolved = fallback;
            }
            EntryNoticeContent finalResolved = resolved;
            runOnUiThread(() -> {
                if (delivered.compareAndSet(false, true)) {
                    mainHandler.removeCallbacks(fallbackRunnable);
                    callback.onReady(finalResolved);
                }
            });
        });
    }

    private void showEntryNoticeDialog(@NonNull EntryNoticeContent content) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(content.title)
                .setMessage(content.buildDialogMessage())
                .setCancelable(false)
                .setPositiveButton(R.string.app_entry_notice_known, null)
                .create();
        if (!content.groupNumber.isEmpty()) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.app_entry_notice_copy_group), (d, which) -> {
            });
        }
        if (!content.bilibiliUrl.isEmpty()) {
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.app_entry_notice_open_bilibili), (d, which) -> {
            });
        }
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                dialog.dismiss();
                ensurePermissionsThenEnterHome();
            });
            if (!content.groupNumber.isEmpty()) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> copyGroupNumber(content.groupNumber));
            }
            if (!content.bilibiliUrl.isEmpty()) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> openBilibiliHomepage(content.bilibiliUrl));
            }
        });
        dialog.show();
    }

    private void copyGroupNumber(@NonNull String groupNumber) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("qq_group", groupNumber));
            GoUtils.DisplayToast(this, getString(R.string.app_entry_notice_group_copied));
        }
    }

    private void openBilibiliHomepage(@NonNull String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
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

    private interface EntryNoticeCallback {
        void onReady(@NonNull EntryNoticeContent content);
    }

    private static final class EntryNoticeContent {
        private final String title;
        private final String message;
        private final String groupNumber;
        private final String bilibiliText;
        private final String bilibiliUrl;

        private EntryNoticeContent(
                String title,
                String message,
                String groupNumber,
                String bilibiliText,
                String bilibiliUrl
        ) {
            this.title = title == null ? "" : title.trim();
            this.message = message == null ? "" : message.trim();
            this.groupNumber = groupNumber == null ? "" : groupNumber.trim();
            this.bilibiliText = bilibiliText == null ? "" : bilibiliText.trim();
            this.bilibiliUrl = bilibiliUrl == null ? "" : bilibiliUrl.trim();
        }

        @NonNull
        private static EntryNoticeContent local(@NonNull WelcomeActivity activity) {
            return new EntryNoticeContent(
                    activity.getString(R.string.app_entry_notice_title),
                    activity.getString(R.string.app_entry_notice_message),
                    activity.getString(R.string.app_entry_notice_group_number),
                    activity.getString(R.string.app_bilibili_notice_text),
                    activity.getString(R.string.app_bilibili_url)
            );
        }

        @NonNull
        private static EntryNoticeContent fromRemote(@NonNull AppClientConfig remote, @NonNull EntryNoticeContent fallback) {
            return new EntryNoticeContent(
                    firstNonEmpty(remote.getNoticeTitle(), fallback.title),
                    firstNonEmpty(remote.getNoticeMessage(), fallback.message),
                    firstNonEmpty(remote.getQqGroupNumber(), fallback.groupNumber),
                    firstNonEmpty(remote.getBilibiliText(), fallback.bilibiliText),
                    firstNonEmpty(remote.getBilibiliUrl(), fallback.bilibiliUrl)
            );
        }

        @NonNull
        private String buildDialogMessage() {
            if (bilibiliText.isEmpty()) {
                return message;
            }
            return message + "\n\n" + bilibiliText;
        }

        @NonNull
        private static String firstNonEmpty(String preferred, String fallback) {
            return preferred == null || preferred.trim().isEmpty() ? fallback : preferred.trim();
        }
    }
}

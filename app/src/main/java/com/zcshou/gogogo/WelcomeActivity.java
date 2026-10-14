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
import android.graphics.Color;
import android.view.Window;
import androidx.annotation.StringRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.acooldog.toolbox.share.domain.model.AppClientConfig;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.ui.CupertinoDialogs;
import com.acooldog.toolbox.ui.WelcomeComposeView;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class WelcomeActivity extends AppCompatActivity implements CupertinoDialogs.Listener {
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
    private EntryNoticeContent activeEntryNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
        setContentView(new WelcomeComposeView(this));

        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        preferences = getSharedPreferences(LEGAL_PREFS_NAME, MODE_PRIVATE);
        ioExecutor = Executors.newSingleThreadExecutor();
        agreementAccepted = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false);
        disclaimerAccepted = preferences.getBoolean(
                KEY_ACCEPT_DISCLAIMER,
                preferences.getBoolean(KEY_ACCEPT_PRIVACY, false)
        );

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
        CupertinoDialogs.showLegalConsent(
                this,
                getString(R.string.app_error_agreement_dialog_title),
                getString(R.string.legal_consent_message),
                getString(R.string.app_agreement),
                getString(R.string.app_agreement_content),
                getString(R.string.app_privacy),
                getString(R.string.app_privacy_content),
                getString(R.string.legal_consent_cancel),
                getString(R.string.legal_consent_confirm),
                this
        );
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
        CupertinoDialogs.showDocument(
                this,
                getString(titleResId),
                getString(contentResId),
                getString(R.string.legal_document_close)
        );
    }

    private void showDocumentDialog(@NonNull String title, @NonNull String content) {
        CupertinoDialogs.showDocument(
                this,
                title,
                content,
                getString(R.string.legal_document_close)
        );
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
        EntryNoticeContent normalFallback = EntryNoticeContent.local(this, false);
        EntryNoticeContent offlineFallback = EntryNoticeContent.local(this, true);
        if (!GoUtils.isNetworkAvailable(this)) {
            callback.onReady(offlineFallback);
            return;
        }

        AtomicBoolean delivered = new AtomicBoolean(false);
        Runnable fallbackRunnable = () -> {
            if (delivered.compareAndSet(false, true)) {
                callback.onReady(offlineFallback);
            }
        };
        mainHandler.postDelayed(fallbackRunnable, REMOTE_NOTICE_TIMEOUT_MILLIS);

        ioExecutor.execute(() -> {
            EntryNoticeContent resolved = offlineFallback;
            try {
                AppClientConfig config = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getAppClientConfig();
                resolved = EntryNoticeContent.fromRemote(config, normalFallback);
            } catch (Exception ignored) {
                resolved = offlineFallback;
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
        activeEntryNotice = content;
        CupertinoDialogs.showEntryNotice(
                this,
                content.title,
                content.buildDialogMessage(),
                content.forceShutdown
                        ? getString(R.string.app_exit)
                        : getString(R.string.app_entry_notice_known),
                content.groupNumber.isEmpty()
                        ? ""
                        : getString(R.string.app_entry_notice_copy_group),
                content.bilibiliUrl.isEmpty()
                        ? ""
                        : getString(R.string.app_entry_notice_open_bilibili),
                this
        );
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

    @Override
    public void onLegalConfirmed() {
        saveAgreementAcceptance();
        showEntryNoticeThenContinue();
    }

    @Override
    public void onLegalRejected() {
        GoUtils.DisplayToast(this, getString(R.string.app_error_agreement));
    }

    @Override
    public void onLegalCancelled() {
        finish();
    }

    @Override
    public void onOpenDocument(@NonNull String title, @NonNull String content) {
        showDocumentDialog(title, content);
    }

    @Override
    public void onEntryPrimary() {
        if (activeEntryNotice != null && activeEntryNotice.forceShutdown) {
            finish();
        } else {
            ensurePermissionsThenEnterHome();
        }
    }

    @Override
    public void onCopyGroup() {
        if (activeEntryNotice != null) {
            copyGroupNumber(activeEntryNotice.groupNumber);
        }
    }

    @Override
    public void onOpenBilibili() {
        if (activeEntryNotice != null) {
            openBilibiliHomepage(activeEntryNotice.bilibiliUrl);
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
        private final boolean forceShutdown;
        private final String forceShutdownMessage;

        private EntryNoticeContent(
                String title,
                String message,
                String groupNumber,
                String bilibiliText,
                String bilibiliUrl,
                boolean forceShutdown,
                String forceShutdownMessage
        ) {
            this.title = title == null ? "" : title.trim();
            this.message = message == null ? "" : message.trim();
            this.groupNumber = groupNumber == null ? "" : groupNumber.trim();
            this.bilibiliText = bilibiliText == null ? "" : bilibiliText.trim();
            this.bilibiliUrl = bilibiliUrl == null ? "" : bilibiliUrl.trim();
            this.forceShutdown = forceShutdown;
            this.forceShutdownMessage = forceShutdownMessage == null ? "" : forceShutdownMessage.trim();
        }

        @NonNull
        private static EntryNoticeContent local(
                @NonNull WelcomeActivity activity,
                boolean offline
        ) {
            return new EntryNoticeContent(
                    activity.getString(R.string.app_entry_notice_title),
                    activity.getString(
                            offline
                                    ? R.string.app_entry_notice_offline_message
                                    : R.string.app_entry_notice_message
                    ),
                    activity.getString(R.string.app_entry_notice_group_number),
                    offline ? "" : activity.getString(R.string.app_bilibili_notice_text),
                    activity.getString(R.string.app_bilibili_url),
                    false,
                    ""
            );
        }

        @NonNull
        private static EntryNoticeContent fromRemote(@NonNull AppClientConfig remote, @NonNull EntryNoticeContent fallback) {
            return new EntryNoticeContent(
                    firstNonEmpty(remote.getNoticeTitle(), fallback.title),
                    firstNonEmpty(remote.getNoticeMessage(), fallback.message),
                    firstNonEmpty(remote.getQqGroupNumber(), fallback.groupNumber),
                    firstNonEmpty(remote.getBilibiliText(), fallback.bilibiliText),
                    firstNonEmpty(remote.getBilibiliUrl(), fallback.bilibiliUrl),
                    remote.isForceShutdown(),
                    remote.getForceShutdownMessage()
            );
        }

        @NonNull
        private String buildDialogMessage() {
            if (bilibiliText.isEmpty()) {
                return message;
            }
            if (forceShutdown) {
                return forceShutdownMessage();
            }
            return message + "\n\n" + bilibiliText;
        }

        @NonNull
        private String forceShutdownMessage() {
            return forceShutdownMessage.isEmpty() ? "当前版本暂不可用，请等待管理员通知。" : forceShutdownMessage;
        }

        @NonNull
        private static String firstNonEmpty(String preferred, String fallback) {
            return preferred == null || preferred.trim().isEmpty() ? fallback : preferred.trim();
        }
    }
}

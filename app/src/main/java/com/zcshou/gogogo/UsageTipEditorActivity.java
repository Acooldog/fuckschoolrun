package com.acooldog.toolbox;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Switch;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.config.TipsCacheStore;
import com.acooldog.toolbox.share.domain.model.UsageTipDetail;
import com.acooldog.toolbox.share.domain.model.WordImportResult;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsageTipEditorActivity extends BaseActivity {
    public static final String EXTRA_TIP_ID = "tip_id";
    private static final int HTML_CHUNK_SIZE = 20000;

    private ExecutorService ioExecutor;
    private InternalAuthStore authStore;
    private TipsCacheStore cacheStore;
    private String tipId;
    private boolean pageReady;
    private String pendingHtmlContent = "";
    private final StringBuilder exportedHtmlBuilder = new StringBuilder();
    private String pendingSaveTitle = "";
    private String pendingSaveContributorQq = "";
    private boolean pendingSavePublished;
    private WebView editorWebView;
    private TextInputEditText titleInput;
    private TextInputEditText contributorInput;
    private Switch publishSwitch;

    private final ActivityResultLauncher<String[]> wordPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::importWordFromUri);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_tip_editor);

        authStore = new InternalAuthStore(getApplicationContext());
        cacheStore = new TipsCacheStore(getApplicationContext());
        ioExecutor = Executors.newSingleThreadExecutor();
        tipId = getIntent().getStringExtra(EXTRA_TIP_ID);

        if (!authStore.isLoggedIn()) {
            GoUtils.DisplayToast(this, getString(R.string.tips_need_login));
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar_tip_editor);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(TextUtils.isEmpty(tipId) ? R.string.tips_editor_title_new : R.string.tips_editor_title_edit);

        titleInput = findViewById(R.id.tip_editor_title_input);
        contributorInput = findViewById(R.id.tip_editor_contributor_input);
        publishSwitch = findViewById(R.id.tip_editor_publish_switch);
        editorWebView = findViewById(R.id.tip_editor_webview);
        configureEditor();

        findViewById(R.id.tip_editor_bold).setOnClickListener(v -> applyCommand("bold"));
        findViewById(R.id.tip_editor_italic).setOnClickListener(v -> applyCommand("italic"));
        findViewById(R.id.tip_editor_underline).setOnClickListener(v -> applyCommand("underline"));
        findViewById(R.id.tip_editor_h1).setOnClickListener(v -> applyCommand("formatBlock", "<h1>"));
        findViewById(R.id.tip_editor_h2).setOnClickListener(v -> applyCommand("formatBlock", "<h2>"));
        findViewById(R.id.tip_editor_list).setOnClickListener(v -> applyCommand("insertUnorderedList"));
        findViewById(R.id.tip_editor_import_word).setOnClickListener(v -> wordPickerLauncher.launch(new String[]{
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        }));
        findViewById(R.id.tip_editor_save_button).setOnClickListener(v -> saveTip());
        Button deleteButton = findViewById(R.id.tip_editor_delete_button);
        deleteButton.setVisibility(TextUtils.isEmpty(tipId) ? View.GONE : View.VISIBLE);
        deleteButton.setOnClickListener(v -> confirmDelete());

        UsageTipDetail cachedDetail = cacheStore.getDetail(tipId);
        if (cachedDetail != null) {
            bindDetail(cachedDetail);
        }
        if (!TextUtils.isEmpty(tipId)) {
            fetchDetail();
        }
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        if (editorWebView != null) {
            editorWebView.destroy();
        }
        super.onDestroy();
    }

    private void configureEditor() {
        WebSettings settings = editorWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        editorWebView.addJavascriptInterface(new EditorJavascriptBridge(), "AndroidBridge");
        editorWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                syncHtmlToEditor();
            }
        });
        editorWebView.loadUrl("file:///android_asset/tips_editor.html");
    }

    private void bindDetail(UsageTipDetail detail) {
        titleInput.setText(detail.getTitle());
        contributorInput.setText(detail.getContributorQq());
        publishSwitch.setChecked(detail.isPublished());
        pendingHtmlContent = detail.getHtmlContent();
        syncHtmlToEditor();
    }

    private void syncHtmlToEditor() {
        if (!pageReady || editorWebView == null) {
            return;
        }
        editorWebView.evaluateJavascript("window.EditorBridge.beginSetHtml();", null);
        String htmlContent = pendingHtmlContent == null ? "" : pendingHtmlContent;
        for (int index = 0; index < htmlContent.length(); index += HTML_CHUNK_SIZE) {
            String chunk = htmlContent.substring(index, Math.min(htmlContent.length(), index + HTML_CHUNK_SIZE));
            editorWebView.evaluateJavascript(
                    "window.EditorBridge.appendHtmlChunk(" + quoteForJs(chunk) + ");",
                    null
            );
        }
        editorWebView.evaluateJavascript("window.EditorBridge.finishSetHtml();", null);
    }

    private void fetchDetail() {
        ioExecutor.execute(() -> {
            try {
                UsageTipDetail detail = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getUsageTip(tipId, authStore.getToken());
                cacheStore.saveDetail(detail);
                runOnUiThread(() -> bindDetail(detail));
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_detail_load_failed, exception)));
            }
        });
    }

    private void applyCommand(String command) {
        applyCommand(command, "");
    }

    private void applyCommand(String command, String value) {
        if (!pageReady) {
            return;
        }
        editorWebView.evaluateJavascript(
                "window.EditorBridge.command(" + quoteForJs(command) + "," + quoteForJs(value) + ");",
                null
        );
    }

    private void saveTip() {
        String title = titleInput.getText() == null ? "" : titleInput.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            GoUtils.DisplayToast(this, getString(R.string.tips_editor_need_title));
            return;
        }
        pendingSaveTitle = title;
        pendingSaveContributorQq = contributorInput.getText() == null ? "" : contributorInput.getText().toString().trim();
        pendingSavePublished = publishSwitch.isChecked();
        exportedHtmlBuilder.setLength(0);
        editorWebView.evaluateJavascript("window.EditorBridge.exportHtml();", null);
    }

    private void performSave(String title, String htmlContent) {
        ioExecutor.execute(() -> {
            try {
                UsageTipDetail saved = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .saveUsageTip(tipId, authStore.getToken(), title, htmlContent, pendingSaveContributorQq, pendingSavePublished);
                cacheStore.saveDetail(saved);
                cacheStore.upsertSummary(saved);
                runOnUiThread(() -> {
                    GoUtils.DisplayToast(this, getString(R.string.tips_save_success));
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_save_failed, exception)));
            }
        });
    }

    private void confirmDelete() {
        if (TextUtils.isEmpty(tipId)) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.tips_delete_confirm_title)
                .setMessage(R.string.tips_delete_confirm_message)
                .setPositiveButton(R.string.tips_delete_button, (dialog, which) -> deleteTip())
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void deleteTip() {
        ioExecutor.execute(() -> {
            try {
                ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .deleteUsageTip(tipId, authStore.getToken());
                runOnUiThread(() -> {
                    cacheStore.removeTip(tipId);
                    GoUtils.DisplayToast(this, getString(R.string.tips_delete_success));
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_delete_failed, exception)));
            }
        });
    }

    private void importWordFromUri(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                byte[] bytes = readUriBytes(uri);
                String filename = resolveDisplayName(uri);
                WordImportResult result = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .importWord(authStore.getToken(), filename, bytes);
                runOnUiThread(() -> {
                    pendingHtmlContent = result.getHtmlContent();
                    syncHtmlToEditor();
                    if (TextUtils.isEmpty(titleInput.getText())) {
                        String plain = result.getPlainText();
                        if (!TextUtils.isEmpty(plain)) {
                            titleInput.setText(plain.length() > 20 ? plain.substring(0, 20) : plain);
                        }
                    }
                    GoUtils.DisplayToast(this, getString(R.string.tips_word_import_success));
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_word_import_failed, exception)));
            }
        });
    }

    private byte[] readUriBytes(Uri uri) throws Exception {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IllegalStateException("Unable to open selected file");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private String resolveDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "tip.docx";
    }

    private String quoteForJs(String value) {
        return value == null ? "\"\"" : org.json.JSONObject.quote(value);
    }

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }

    private final class EditorJavascriptBridge {
        @JavascriptInterface
        public void onEditorReady() {
            runOnUiThread(() -> {
                pageReady = true;
                syncHtmlToEditor();
            });
        }

        @JavascriptInterface
        public void beginHtmlExport() {
            exportedHtmlBuilder.setLength(0);
        }

        @JavascriptInterface
        public void appendHtmlExportChunk(String chunk) {
            if (chunk != null) {
                exportedHtmlBuilder.append(chunk);
            }
        }

        @JavascriptInterface
        public void finishHtmlExport() {
            String htmlContent = exportedHtmlBuilder.toString();
            exportedHtmlBuilder.setLength(0);
            performSave(pendingSaveTitle, htmlContent);
        }
    }
}

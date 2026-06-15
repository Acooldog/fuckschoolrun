package com.acooldog.toolbox;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.config.TipsCacheStore;
import com.acooldog.toolbox.share.domain.model.UsageTipDetail;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsageTipDetailActivity extends BaseActivity {
    public static final String EXTRA_TIP_ID = "tip_id";

    private ExecutorService ioExecutor;
    private TipsCacheStore cacheStore;
    private InternalAuthStore authStore;
    private String tipId;
    private UsageTipDetail currentDetail;
    private TextView titleView;
    private TextView metaView;
    private WebView webView;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchDetail();
                }
            });
    private final ActivityResultLauncher<String[]> wordUpdateLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::replaceTipContentFromUri);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_tip_detail);

        tipId = getIntent().getStringExtra(EXTRA_TIP_ID);
        ioExecutor = Executors.newSingleThreadExecutor();
        cacheStore = new TipsCacheStore(getApplicationContext());
        authStore = new InternalAuthStore(getApplicationContext());

        MaterialToolbar toolbar = findViewById(R.id.toolbar_tip_detail);
        toolbar.setNavigationOnClickListener(v -> finish());
        titleView = findViewById(R.id.tip_detail_title);
        metaView = findViewById(R.id.tip_detail_meta);
        webView = findViewById(R.id.tip_detail_webview);
        configureWebView();

        findViewById(R.id.tip_detail_edit_button).setOnClickListener(v -> openEditor());
        findViewById(R.id.tip_detail_update_button).setOnClickListener(v -> startWordReplace());
        findViewById(R.id.tip_detail_delete_button).setOnClickListener(v -> confirmDelete());

        UsageTipDetail cachedDetail = cacheStore.getDetail(tipId);
        if (cachedDetail != null) {
            bindDetail(cachedDetail);
        }
        fetchDetail();
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setBuiltInZoomControls(false);
    }

    private void fetchDetail() {
        if (TextUtils.isEmpty(tipId)) {
            finish();
            return;
        }
        if (!GoUtils.isNetworkAvailable(this) && currentDetail != null) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                UsageTipDetail detail = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getUsageTip(tipId, authStore.getToken());
                cacheStore.saveDetail(detail);
                runOnUiThread(() -> bindDetail(detail));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (currentDetail == null) {
                        GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_detail_load_failed, exception));
                    }
                });
            }
        });
    }

    private void bindDetail(UsageTipDetail detail) {
        currentDetail = detail;
        titleView.setText(detail.getTitle());
        metaView.setText(getString(R.string.tips_author_format, detail.getAuthorUsername())
                + "    "
                + getString(R.string.tips_contributor_format, detail.getContributorQq()));
        webView.loadDataWithBaseURL(
                null,
                wrapHtml(detail.getHtmlContent()),
                "text/html",
                "utf-8",
                null
        );
        findViewById(R.id.tip_detail_actions).setVisibility(detail.isEditable() ? View.VISIBLE : View.GONE);
    }

    private String wrapHtml(String htmlContent) {
        return "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />"
                + "<style>body{font-family:'Microsoft YaHei',sans-serif;padding:16px;line-height:1.7;color:#1f2937;}"
                + "p,h1,h2,h3,h4,h5,h6,ul,ol,table,blockquote{margin:0 0 12px;}"
                + "ul,ol{padding-left:22px;}li{margin:6px 0;}img{max-width:100%;height:auto;}"
                + "pre{white-space:pre-wrap;}table{border-collapse:collapse;width:100%;margin:10px 0;}"
                + "td,th{border:1px solid #d1d5db;padding:8px;}</style>"
                + "</head><body>"
                + (htmlContent == null ? "" : htmlContent)
                + "</body></html>";
    }

    private void openEditor() {
        if (currentDetail == null || !currentDetail.isEditable()) {
            return;
        }
        Intent intent = new Intent(this, UsageTipEditorActivity.class);
        intent.putExtra(UsageTipEditorActivity.EXTRA_TIP_ID, currentDetail.getId());
        editLauncher.launch(intent);
    }

    private void startWordReplace() {
        if (currentDetail == null || !currentDetail.isEditable()) {
            return;
        }
        wordUpdateLauncher.launch(new String[]{
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
    }

    private void confirmDelete() {
        if (currentDetail == null || !currentDetail.isEditable()) {
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
        if (currentDetail == null) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .deleteUsageTip(currentDetail.getId(), authStore.getToken());
                runOnUiThread(() -> {
                    cacheStore.removeTip(currentDetail.getId());
                    GoUtils.DisplayToast(this, getString(R.string.tips_delete_success));
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_delete_failed, exception)));
            }
        });
    }

    private void replaceTipContentFromUri(@Nullable Uri uri) {
        if (uri == null || currentDetail == null || !currentDetail.isEditable()) {
            return;
        }
        UsageTipDetail detail = currentDetail;
        ioExecutor.execute(() -> {
            try {
                byte[] bytes = readUriBytes(uri);
                String filename = resolveDisplayName(uri);
                ShareModule shareModule = ShareModule.from(getApplicationContext());
                String token = authStore.getToken();
                String htmlContent = shareModule
                        .shareApiClient()
                        .importWord(token, filename, bytes)
                        .getHtmlContent();
                UsageTipDetail saved = shareModule
                        .shareApiClient()
                        .saveUsageTip(
                                detail.getId(),
                                token,
                                detail.getTitle(),
                                htmlContent,
                                detail.getContributorQq(),
                                detail.isPublished()
                        );
                cacheStore.saveDetail(saved);
                cacheStore.upsertSummary(saved);
                runOnUiThread(() -> {
                    bindDetail(saved);
                    setResult(RESULT_OK);
                    GoUtils.DisplayToast(this, getString(R.string.tips_update_success));
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_update_failed, exception)));
            }
        });
    }

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
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
}

package com.acooldog.toolbox;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.config.TipsCacheStore;
import com.acooldog.toolbox.share.domain.model.UsageTipDetail;
import com.acooldog.toolbox.share.domain.model.UsageTipSummary;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsageTipsActivity extends BaseActivity {
    private final List<UsageTipSummary> allItems = new ArrayList<>();
    private final List<UsageTipSummary> visibleItems = new ArrayList<>();

    private ExecutorService ioExecutor;
    private TipsCacheStore cacheStore;
    private InternalAuthStore authStore;
    private UsageTipsAdapter adapter;
    private TextView statusView;
    private EditText searchInput;
    private boolean refreshOnResume;
    private UsageTipSummary pendingWordUpdateTip;

    private final ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshOnResume = true;
                }
            });
    private final ActivityResultLauncher<String[]> wordUpdateLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::replaceTipContentFromUri);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_tips);

        ioExecutor = Executors.newSingleThreadExecutor();
        cacheStore = new TipsCacheStore(getApplicationContext());
        authStore = new InternalAuthStore(getApplicationContext());

        MaterialToolbar toolbar = findViewById(R.id.toolbar_usage_tips);
        toolbar.setNavigationOnClickListener(v -> finish());
        statusView = findViewById(R.id.tips_status_view);
        searchInput = findViewById(R.id.tips_search_input);
        ListView listView = findViewById(R.id.tips_list_view);
        TextView emptyView = findViewById(R.id.tips_empty_view);
        listView.setEmptyView(emptyView);

        adapter = new UsageTipsAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> openTipDetail(visibleItems.get(position)));

        Button searchButton = findViewById(R.id.tips_search_button);
        Button refreshButton = findViewById(R.id.tips_refresh_button);
        View newFab = findViewById(R.id.tips_new_fab);

        searchButton.setOnClickListener(v -> searchTips());
        refreshButton.setOnClickListener(v -> fetchTips(searchQuery(), true));
        newFab.setOnClickListener(v -> openTipEditor(null));

        updateFabVisibility();
        loadCachedTips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFabVisibility();
        if (refreshOnResume) {
            refreshOnResume = false;
            fetchTips(searchQuery(), true);
        }
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void loadCachedTips() {
        List<UsageTipSummary> cachedItems = cacheStore.getList();
        if (!cachedItems.isEmpty()) {
            allItems.clear();
            allItems.addAll(cachedItems);
            applyLocalFilter(searchQuery());
            statusView.setText(getString(R.string.tips_cache_loaded));
            return;
        }
        fetchTips("", false);
    }

    private void searchTips() {
        String query = searchQuery();
        applyLocalFilter(query);
        if (GoUtils.isNetworkAvailable(this)) {
            fetchTips(query, true);
        }
    }

    private void fetchTips(String query, boolean manual) {
        if (!GoUtils.isNetworkAvailable(this)) {
            if (manual) {
                GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            }
            return;
        }
        statusView.setText(getString(R.string.tips_loading));
        String token = authStore.getToken();
        ioExecutor.execute(() -> {
            try {
                List<UsageTipSummary> items = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getUsageTips(query, 1, 100, token);
                runOnUiThread(() -> {
                    allItems.clear();
                    allItems.addAll(items);
                    if (TextUtils.isEmpty(query)) {
                        cacheStore.saveList(items);
                    }
                    applyLocalFilter(query);
                    statusView.setText(getString(R.string.tips_loading).replace("…", "") + "完成");
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_list_load_failed, exception)));
            }
        });
    }

    private void applyLocalFilter(String query) {
        visibleItems.clear();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        for (UsageTipSummary item : allItems) {
            if (TextUtils.isEmpty(normalizedQuery)
                    || item.getTitle().toLowerCase(Locale.getDefault()).contains(normalizedQuery)
                    || item.getExcerpt().toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                visibleItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateFabVisibility() {
        View fab = findViewById(R.id.tips_new_fab);
        fab.setVisibility(authStore.isLoggedIn() ? View.VISIBLE : View.GONE);
    }

    private String searchQuery() {
        return searchInput.getText() == null ? "" : searchInput.getText().toString().trim();
    }

    private void openTipDetail(UsageTipSummary summary) {
        Intent intent = new Intent(this, UsageTipDetailActivity.class);
        intent.putExtra(UsageTipDetailActivity.EXTRA_TIP_ID, summary.getId());
        startActivity(intent);
    }

    private void openTipEditor(@Nullable String tipId) {
        if (!authStore.isLoggedIn()) {
            GoUtils.DisplayToast(this, getString(R.string.tips_need_login));
            return;
        }
        Intent intent = new Intent(this, UsageTipEditorActivity.class);
        if (!TextUtils.isEmpty(tipId)) {
            intent.putExtra(UsageTipEditorActivity.EXTRA_TIP_ID, tipId);
        }
        editorLauncher.launch(intent);
    }

    private void confirmDeleteTip(UsageTipSummary summary) {
        if (summary == null || TextUtils.isEmpty(summary.getId())) {
            return;
        }
        if (!summary.isEditable()) {
            GoUtils.DisplayToast(this, getString(R.string.tips_edit_own_only));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.tips_delete_confirm_title)
                .setMessage(R.string.tips_delete_confirm_message)
                .setPositiveButton(R.string.tips_delete_button, (dialog, which) -> deleteTip(summary))
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void deleteTip(UsageTipSummary summary) {
        if (summary == null) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .deleteUsageTip(summary.getId(), authStore.getToken());
                runOnUiThread(() -> {
                    allItems.removeIf(item -> summary.getId().equals(item.getId()));
                    visibleItems.removeIf(item -> summary.getId().equals(item.getId()));
                    adapter.notifyDataSetChanged();
                    cacheStore.saveList(allItems);
                    GoUtils.DisplayToast(this, getString(R.string.tips_delete_success));
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_delete_failed, exception)));
            }
        });
    }

    private void startWordReplace(UsageTipSummary summary) {
        if (summary == null || TextUtils.isEmpty(summary.getId())) {
            return;
        }
        if (!summary.isEditable()) {
            GoUtils.DisplayToast(this, getString(R.string.tips_edit_own_only));
            return;
        }
        pendingWordUpdateTip = summary;
        wordUpdateLauncher.launch(new String[]{
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
    }

    private void replaceTipContentFromUri(@Nullable Uri uri) {
        UsageTipSummary targetSummary = pendingWordUpdateTip;
        pendingWordUpdateTip = null;
        if (uri == null || targetSummary == null) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                byte[] bytes = readUriBytes(uri);
                String filename = resolveDisplayName(uri);
                String token = authStore.getToken();
                ShareModule shareModule = ShareModule.from(getApplicationContext());
                String htmlContent = shareModule
                        .shareApiClient()
                        .importWord(token, filename, bytes)
                        .getHtmlContent();
                UsageTipDetail saved = shareModule
                        .shareApiClient()
                        .saveUsageTip(
                                targetSummary.getId(),
                                token,
                                targetSummary.getTitle(),
                                htmlContent,
                                targetSummary.getContributorQq(),
                                targetSummary.isPublished()
                        );
                cacheStore.saveDetail(saved);
                cacheStore.upsertSummary(saved);
                runOnUiThread(() -> {
                    upsertVisibleSummary(saved);
                    GoUtils.DisplayToast(this, getString(R.string.tips_update_success));
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.tips_update_failed, exception)));
            }
        });
    }

    private void upsertVisibleSummary(UsageTipDetail detail) {
        UsageTipSummary updated = new UsageTipSummary(
                detail.getId(),
                detail.getTitle(),
                buildExcerpt(detail.getPlainText()),
                detail.getContributorQq(),
                detail.getAuthorUsername(),
                detail.isPublished(),
                detail.isEditable(),
                detail.getCreatedAt(),
                detail.getUpdatedAt()
        );
        boolean replaced = false;
        for (int index = 0; index < allItems.size(); index++) {
            UsageTipSummary item = allItems.get(index);
            if (item != null && detail.getId().equals(item.getId())) {
                allItems.set(index, updated);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            allItems.add(0, updated);
        }
        applyLocalFilter(searchQuery());
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

    private String buildExcerpt(String plainText) {
        String normalized = plainText == null ? "" : plainText.trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
    }

    private final class UsageTipsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return visibleItems.size();
        }

        @Override
        public Object getItem(int position) {
            return visibleItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usage_tip, parent, false);
                holder = new ViewHolder();
                holder.titleView = convertView.findViewById(R.id.tip_item_title);
                holder.excerptView = convertView.findViewById(R.id.tip_item_excerpt);
                holder.metaView = convertView.findViewById(R.id.tip_item_meta);
                holder.statusView = convertView.findViewById(R.id.tip_item_status);
                holder.actionsLayout = convertView.findViewById(R.id.tip_item_actions);
                holder.editButton = convertView.findViewById(R.id.tip_item_edit_button);
                holder.updateButton = convertView.findViewById(R.id.tip_item_update_button);
                holder.deleteButton = convertView.findViewById(R.id.tip_item_delete_button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            UsageTipSummary item = visibleItems.get(position);
            holder.titleView.setText(item.getTitle());
            holder.excerptView.setText(item.getExcerpt());
            holder.metaView.setText(getString(R.string.tips_author_format, item.getAuthorUsername()));
            holder.statusView.setText(item.isPublished() ? R.string.tips_status_published : R.string.tips_status_draft);
            holder.actionsLayout.setVisibility(item.isEditable() ? View.VISIBLE : View.GONE);
            convertView.setOnClickListener(v -> openTipDetail(item));
            holder.editButton.setOnClickListener(v -> openTipEditor(item.getId()));
            holder.updateButton.setOnClickListener(v -> startWordReplace(item));
            holder.deleteButton.setOnClickListener(v -> confirmDeleteTip(item));
            return convertView;
        }
    }

    private static final class ViewHolder {
        private TextView titleView;
        private TextView excerptView;
        private TextView metaView;
        private TextView statusView;
        private View actionsLayout;
        private Button editButton;
        private Button updateButton;
        private Button deleteButton;
    }
}

package com.acooldog.toolbox;

import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.acooldog.toolbox.config.SavedNfcConfig;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.acooldog.toolbox.config.SimulationPrefsStore;
import com.acooldog.toolbox.nfc.data.AndroidNfcPayloadReader;
import com.acooldog.toolbox.nfc.domain.NfcPayload;
import com.acooldog.toolbox.nfc.domain.NfcPayloadParser;
import com.acooldog.toolbox.nfc.domain.ReadNfcPayloadUseCase;
import com.acooldog.toolbox.share.domain.model.SharedNfcEntry;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.acooldog.toolbox.utils.SearchSortUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NfcToolsActivity extends BaseActivity {
    public static final String EXTRA_START_ACTION = "start_action";
    public static final String ACTION_SHARE = "share";
    public static final String ACTION_DOWNLOAD = "download";

    private NfcAdapter nfcAdapter;
    private PendingIntent foregroundDispatchIntent;
    private ReadNfcPayloadUseCase readNfcPayloadUseCase;
    private TextView hardwareStatusView;
    private TextView mockStatusView;
    private TextInputEditText urlInput;
    private TextInputEditText packageInput;
    private ExecutorService ioExecutor;
    private String currentPayloadSource = "manual";
    private boolean startupActionHandled;
    private SimulationPrefsStore prefsStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_tools);

        readNfcPayloadUseCase = new ReadNfcPayloadUseCase(
                new AndroidNfcPayloadReader(),
                new NfcPayloadParser()
        );
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        foregroundDispatchIntent = createForegroundDispatchIntent();
        ioExecutor = Executors.newSingleThreadExecutor();
        prefsStore = new SimulationPrefsStore(getApplicationContext());

        bindViews();
        restoreNfcConfig();
        updateHardwareStatus();
        handleNfcIntent(getIntent());
        maybeHandleStartupAction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHardwareStatus();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.enableForegroundDispatch(this, foregroundDispatchIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        persistNfcConfig();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        startupActionHandled = false;
        handleNfcIntent(intent);
        maybeHandleStartupAction();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_nfc);
        toolbar.setNavigationOnClickListener(v -> finish());

        hardwareStatusView = findViewById(R.id.nfc_tv_hardware_status);
        mockStatusView = findViewById(R.id.nfc_tv_mock_status);
        urlInput = findViewById(R.id.nfc_et_mock_url);
        packageInput = findViewById(R.id.nfc_et_mock_package);

        findViewById(R.id.nfc_btn_copy_url).setOnClickListener(v -> copyText(
                getString(R.string.nfc_copy_link),
                getInputText(urlInput)
        ));
        findViewById(R.id.nfc_btn_copy_package).setOnClickListener(v -> copyText(
                getString(R.string.nfc_copy_package),
                getInputText(packageInput)
        ));
        findViewById(R.id.nfc_btn_save).setOnClickListener(v -> showSaveConfigDialog());
        findViewById(R.id.nfc_btn_load_local).setOnClickListener(v -> showLocalConfigPicker(false));
        findViewById(R.id.nfc_btn_mock).setOnClickListener(v -> sendMockNfc());
        findViewById(R.id.nfc_btn_share).setOnClickListener(v -> showShareDialog());
        findViewById(R.id.nfc_btn_download).setOnClickListener(v -> loadSharedNfcEntries());
    }

    private PendingIntent createForegroundDispatchIntent() {
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getActivity(this, 0, intent, flags);
    }

    private void updateHardwareStatus() {
        if (nfcAdapter == null) {
            setStatus(hardwareStatusView, getString(R.string.nfc_status_no_hardware), true);
        } else if (!nfcAdapter.isEnabled()) {
            setStatus(hardwareStatusView, getString(R.string.nfc_status_disabled), true);
        } else {
            setStatus(hardwareStatusView, getString(R.string.nfc_status_ready), false);
        }
    }

    private void handleNfcIntent(Intent intent) {
        if (!isNfcIntent(intent)) {
            return;
        }

        NfcPayload payload = readNfcPayloadUseCase.read(intent);
        if (payload.isEmpty()) {
            setStatus(mockStatusView, getString(R.string.nfc_read_empty), true);
            return;
        }

        currentPayloadSource = TextUtils.isEmpty(payload.getSource()) ? "tag" : payload.getSource();
        if (payload.hasUrl()) {
            urlInput.setText(payload.getUrl());
        }
        if (payload.hasPackageName()) {
            packageInput.setText(payload.getPackageName());
        }
        persistNfcConfig();
        setStatus(mockStatusView, getString(R.string.nfc_read_ok), false);
    }

    private void maybeHandleStartupAction() {
        if (startupActionHandled) {
            return;
        }
        startupActionHandled = true;
        String action = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_START_ACTION);
        if (TextUtils.isEmpty(action)) {
            return;
        }
        mockStatusView.post(() -> {
            if (ACTION_SHARE.equals(action)) {
                showShareDialog();
            } else if (ACTION_DOWNLOAD.equals(action)) {
                loadSharedNfcEntries();
            }
        });
    }

    private boolean isNfcIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return false;
        }
        String action = intent.getAction();
        return NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action);
    }

    private void sendMockNfc() {
        vibrate();
        String url = getInputText(urlInput);
        String packageName = getInputText(packageInput);

        if (TextUtils.isEmpty(url)) {
            setStatus(mockStatusView, getString(R.string.nfc_mock_need_url), true);
            return;
        }
        if (TextUtils.isEmpty(packageName)) {
            setStatus(mockStatusView, getString(R.string.nfc_mock_need_package), true);
            return;
        }

        persistNfcConfig();
        Intent intent = new Intent(this, RouteRunActivity.class);
        intent.putExtra(RouteRunActivity.EXTRA_PENDING_NFC_URL, url);
        intent.putExtra(RouteRunActivity.EXTRA_PENDING_NFC_PACKAGE, packageName);
        intent.putExtra(RouteRunActivity.EXTRA_PENDING_NFC_SOURCE, currentPayloadSource);
        startActivity(intent);
        setStatus(mockStatusView, getString(R.string.nfc_mock_waiting_route_start), false);
    }

    private void showShareDialog() {
        List<SavedNfcConfig> savedConfigs = prefsStore.getSavedNfcConfigs();
        if (!savedConfigs.isEmpty()) {
            String[] options = {
                    getString(R.string.nfc_share_manual_option),
                    getString(R.string.nfc_share_saved_option)
            };
            new AlertDialog.Builder(this)
                    .setTitle(R.string.nfc_share_source_title)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showManualShareDialog();
                        } else {
                            showLocalConfigPicker(true);
                        }
                    })
                    .show();
            return;
        }
        showManualShareDialog();
    }

    private void showManualShareDialog() {
        showShareDetailDialog(
                buildDefaultNfcName(),
                getInputText(urlInput),
                getInputText(packageInput),
                currentPayloadSource
        );
    }

    private void showShareDetailDialog(String defaultName, String defaultUrl, String defaultPackage, String source) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nfc_share, null);
        EditText nameInput = dialogView.findViewById(R.id.nfc_share_name_input);
        EditText urlEdit = dialogView.findViewById(R.id.nfc_share_url_input);
        EditText packageEdit = dialogView.findViewById(R.id.nfc_share_package_input);

        nameInput.setText(defaultName);
        urlEdit.setText(defaultUrl);
        packageEdit.setText(defaultPackage);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.nfc_share_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.nfc_share_confirm, null)
                .setNegativeButton(R.string.nfc_share_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            String url = urlEdit.getText() == null ? "" : urlEdit.getText().toString().trim();
            String packageName = packageEdit.getText() == null ? "" : packageEdit.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                setStatus(mockStatusView, getString(R.string.nfc_share_need_name), true);
                return;
            }
            if (TextUtils.isEmpty(url)) {
                setStatus(mockStatusView, getString(R.string.nfc_mock_need_url), true);
                return;
            }
            if (TextUtils.isEmpty(packageName)) {
                setStatus(mockStatusView, getString(R.string.nfc_mock_need_package), true);
                return;
            }

            urlInput.setText(url);
            packageInput.setText(packageName);
            currentPayloadSource = TextUtils.isEmpty(source) ? "manual" : source;
            persistNfcConfig();
            dialog.dismiss();
            uploadSharedNfc(name, url, packageName);
        }));
        dialog.show();
    }

    private void showSaveConfigDialog() {
        String url = getInputText(urlInput);
        String packageName = getInputText(packageInput);
        if (TextUtils.isEmpty(url)) {
            setStatus(mockStatusView, getString(R.string.nfc_mock_need_url), true);
            return;
        }
        if (TextUtils.isEmpty(packageName)) {
            setStatus(mockStatusView, getString(R.string.nfc_mock_need_package), true);
            return;
        }

        EditText nameInput = new EditText(this);
        nameInput.setHint(R.string.nfc_save_name_hint);
        nameInput.setText(buildDefaultLocalConfigName());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.nfc_save_dialog_title)
                .setView(nameInput)
                .setPositiveButton(R.string.nfc_save_confirm, null)
                .setNegativeButton(R.string.nfc_share_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                setStatus(mockStatusView, getString(R.string.nfc_save_need_name), true);
                return;
            }

            prefsStore.saveSavedNfcConfig(new SavedNfcConfig(name, url, packageName, currentPayloadSource));
            persistNfcConfig();
            dialog.dismiss();
            setStatus(mockStatusView, getString(R.string.nfc_save_success), false);
        }));
        dialog.show();
    }

    private void showLocalConfigPicker(boolean shareAfterPick) {
        List<SavedNfcConfig> configs = prefsStore.getSavedNfcConfigs();
        if (configs.isEmpty()) {
            setStatus(mockStatusView, getString(R.string.nfc_local_empty), true);
            return;
        }

        CharSequence[] items = new CharSequence[configs.size()];
        for (int index = 0; index < configs.size(); index++) {
            SavedNfcConfig config = configs.get(index);
            items[index] = config.getName() + " · " + config.getPackageName();
        }

        new AlertDialog.Builder(this)
                .setTitle(shareAfterPick ? R.string.nfc_share_local_pick_title : R.string.nfc_local_pick_title)
                .setItems(items, (dialog, which) -> {
                    SavedNfcConfig selected = configs.get(which);
                    if (shareAfterPick) {
                        showShareDetailDialog(
                                selected.getName(),
                                selected.getUrl(),
                                selected.getPackageName(),
                                selected.getSource()
                        );
                    } else {
                        applySavedNfcConfig(selected);
                    }
                })
                .show();
    }

    private void applySavedNfcConfig(SavedNfcConfig config) {
        urlInput.setText(config.getUrl());
        packageInput.setText(config.getPackageName());
        currentPayloadSource = TextUtils.isEmpty(config.getSource()) ? "saved" : config.getSource();
        persistNfcConfig();
        setStatus(mockStatusView, getString(R.string.nfc_local_loaded), false);
    }

    private void uploadSharedNfc(String name, String url, String packageName) {
        if (!GoUtils.isNetworkAvailable(this)) {
            setStatus(mockStatusView, getString(R.string.app_error_network), true);
            return;
        }
        String payloadSource = TextUtils.isEmpty(currentPayloadSource) ? "manual" : currentPayloadSource;
        setStatus(mockStatusView, getString(R.string.nfc_share_uploading), false);
        ioExecutor.execute(() -> {
            try {
                ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .uploadNfc(name, new NfcPayload(url, packageName, payloadSource));
                runOnUiThread(() -> {
                    currentPayloadSource = payloadSource;
                    persistNfcConfig();
                    setStatus(mockStatusView, getString(R.string.nfc_share_success), false);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> setStatus(mockStatusView, buildDetailedToast(R.string.nfc_share_failed, exception), true));
            }
        });
    }

    private void loadSharedNfcEntries() {
        if (!GoUtils.isNetworkAvailable(this)) {
            setStatus(mockStatusView, getString(R.string.app_error_network), true);
            return;
        }
        setStatus(mockStatusView, getString(R.string.nfc_download_loading), false);
        ioExecutor.execute(() -> {
            try {
                List<SharedNfcEntry> entries = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedNfcEntries();
                runOnUiThread(() -> showSharedNfcPicker(entries));
            } catch (Exception exception) {
                runOnUiThread(() -> setStatus(mockStatusView, buildDetailedToast(R.string.nfc_download_failed, exception), true));
            }
        });
    }

    private void showSharedNfcPicker(List<SharedNfcEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            setStatus(mockStatusView, getString(R.string.nfc_download_empty), true);
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_searchable_picker, null);
        EditText searchInput = dialogView.findViewById(R.id.searchable_picker_input);
        ListView listView = dialogView.findViewById(R.id.searchable_picker_list);
        List<SearchableNfcItem> allItems = buildNfcItems(entries);
        List<SearchableNfcItem> filteredItems = new ArrayList<>(allItems);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        fillNfcAdapter(adapter, filteredItems);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.nfc_download_pick_title)
                .setView(dialogView)
                .setNegativeButton(R.string.nfc_share_cancel, null)
                .create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (filteredItems.isEmpty()) {
                return;
            }
            dialog.dismiss();
            applySharedNfc(filteredItems.get(position).entry);
        });
        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                filterNfcItems(editable == null ? "" : editable.toString(), allItems, filteredItems, adapter);
            }
        });
        dialog.show();
    }

    private void applySharedNfc(SharedNfcEntry entry) {
        urlInput.setText(entry.getUrl());
        packageInput.setText(entry.getPackageName());
        currentPayloadSource = TextUtils.isEmpty(entry.getSource()) ? "shared" : entry.getSource();
        persistNfcConfig();
        setStatus(mockStatusView, getString(R.string.nfc_download_loaded), false);
    }

    private String getInputText(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void copyText(String label, String value) {
        if (TextUtils.isEmpty(value)) {
            setStatus(mockStatusView, getString(R.string.nfc_copy_empty), true);
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
            setStatus(mockStatusView, getString(R.string.nfc_copied), false);
        }
    }

    private void setStatus(TextView view, String text, boolean warning) {
        view.setText(text);
        view.setVisibility(View.VISIBLE);
        view.setTextColor(getColor(warning ? R.color.nfc_status_warning : R.color.nfc_status_success));
    }

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }

    private String buildDefaultNfcName() {
        return "nfc_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    private String buildDefaultLocalConfigName() {
        String packageName = getInputText(packageInput);
        if (TextUtils.isEmpty(packageName)) {
            return buildDefaultNfcName();
        }
        int lastSeparator = packageName.lastIndexOf('.');
        if (lastSeparator >= 0 && lastSeparator < packageName.length() - 1) {
            return packageName.substring(lastSeparator + 1);
        }
        return packageName;
    }

    private void restoreNfcConfig() {
        String cachedUrl = prefsStore.getNfcUrl();
        String cachedPackage = prefsStore.getNfcPackageName();
        if (!TextUtils.isEmpty(cachedUrl)) {
            urlInput.setText(cachedUrl);
        }
        if (!TextUtils.isEmpty(cachedPackage)) {
            packageInput.setText(cachedPackage);
        }
        currentPayloadSource = prefsStore.getNfcSource();
    }

    private void persistNfcConfig() {
        prefsStore.saveNfcConfig(getInputText(urlInput), getInputText(packageInput), currentPayloadSource);
    }

    private void vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                if (manager != null) {
                    manager.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } else {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(80L);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static final class SearchableNfcItem {
        private final SharedNfcEntry entry;
        private final String displayText;
        private final String sortKey;

        private SearchableNfcItem(SharedNfcEntry entry, String displayText, String sortKey) {
            this.entry = entry;
            this.displayText = displayText;
            this.sortKey = sortKey;
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op.
        }
    }

    private List<SearchableNfcItem> buildNfcItems(List<SharedNfcEntry> entries) {
        List<SearchableNfcItem> items = new ArrayList<>();
        for (SharedNfcEntry entry : entries) {
            String displayText = entry.getName() + " · " + entry.getPackageName();
            items.add(new SearchableNfcItem(entry, displayText, SearchSortUtils.buildSortKey(entry.getName())));
        }
        items.sort(Comparator.comparing(item -> item.sortKey));
        return items;
    }

    private void fillNfcAdapter(ArrayAdapter<String> adapter, List<SearchableNfcItem> items) {
        adapter.clear();
        if (items.isEmpty()) {
            adapter.add(getString(R.string.searchable_picker_empty));
        } else {
            for (SearchableNfcItem item : items) {
                adapter.add(item.displayText);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterNfcItems(
            String query,
            List<SearchableNfcItem> sourceItems,
            List<SearchableNfcItem> filteredItems,
            ArrayAdapter<String> adapter
    ) {
        filteredItems.clear();
        for (SearchableNfcItem item : sourceItems) {
            if (SearchSortUtils.matches(query, item.entry.getName())
                    || SearchSortUtils.matches(query, item.entry.getPackageName())) {
                filteredItems.add(item);
            }
        }
        fillNfcAdapter(adapter, filteredItems);
    }
}

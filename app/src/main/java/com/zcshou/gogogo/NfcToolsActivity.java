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
import com.acooldog.toolbox.ui.CupertinoDialogs;
import com.acooldog.toolbox.ui.CupertinoNfcToolsView;
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
    private CupertinoNfcToolsView nfcView;
    private ExecutorService ioExecutor;
    private String currentPayloadSource = "manual";
    private boolean startupActionHandled;
    private SimulationPrefsStore prefsStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcView = new CupertinoNfcToolsView(this);
        setContentView(nfcView);
        CupertinoDockInstaller.install(this, CupertinoDockInstaller.TAB_NFC);

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
        nfcView.setActions(new CupertinoNfcToolsView.Actions() {
            @Override public void onBack() { finish(); }
            @Override public void onCopyUrl(String value) { copyText(getString(R.string.nfc_copy_link), value); }
            @Override public void onCopyPackage(String value) { copyText(getString(R.string.nfc_copy_package), value); }
            @Override public void onSave() { showSaveConfigDialog(); }
            @Override public void onLoadLocal() { showLocalConfigPicker(false); }
            @Override public void onMock() { sendMockNfc(); }
            @Override public void onShare() { showShareDialog(); }
            @Override public void onDownload() { loadSharedNfcEntries(); }
        });
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
            nfcView.setHardwareStatus(getString(R.string.nfc_status_no_hardware), true);
        } else if (!nfcAdapter.isEnabled()) {
            nfcView.setHardwareStatus(getString(R.string.nfc_status_disabled), true);
        } else {
            nfcView.setHardwareStatus(getString(R.string.nfc_status_ready), false);
        }
    }

    private void handleNfcIntent(Intent intent) {
        if (!isNfcIntent(intent)) {
            return;
        }

        NfcPayload payload = readNfcPayloadUseCase.read(intent);
        if (payload.isEmpty()) {
            setStatus(getString(R.string.nfc_read_empty), true);
            return;
        }

        currentPayloadSource = TextUtils.isEmpty(payload.getSource()) ? "tag" : payload.getSource();
        if (payload.hasUrl()) {
            nfcView.setUrl(payload.getUrl());
        }
        if (payload.hasPackageName()) {
            nfcView.setPackageName(payload.getPackageName());
        }
        persistNfcConfig();
        setStatus(getString(R.string.nfc_read_ok), false);
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
        nfcView.post(() -> {
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
        String url = nfcView.getUrl();
        String packageName = nfcView.getPackageName();

        if (TextUtils.isEmpty(url)) {
            setStatus(getString(R.string.nfc_mock_need_url), true);
            return;
        }
        if (TextUtils.isEmpty(packageName)) {
            setStatus(getString(R.string.nfc_mock_need_package), true);
            return;
        }

        persistNfcConfig();
        Intent intent = new Intent(this, RouteRunActivity.class);
        intent.putExtra(RouteRunActivity.EXTRA_PENDING_NFC_URL, url);
        intent.putExtra(RouteRunActivity.EXTRA_PENDING_NFC_PACKAGE, packageName);
        intent.putExtra(RouteRunActivity.EXTRA_PENDING_NFC_SOURCE, currentPayloadSource);
        startActivity(intent);
        setStatus(getString(R.string.nfc_mock_waiting_route_start), false);
    }

    private void showShareDialog() {
        List<SavedNfcConfig> savedConfigs = prefsStore.getSavedNfcConfigs();
        if (!savedConfigs.isEmpty()) {
            String[] options = {
                    getString(R.string.nfc_share_manual_option),
                    getString(R.string.nfc_share_saved_option)
            };
            CupertinoDialogs.showChoices(
                    this,
                    getString(R.string.nfc_share_source_title),
                    null,
                    options,
                    getString(R.string.nfc_share_cancel),
                    which -> {
                        if (which == 0) {
                            showManualShareDialog();
                        } else {
                            showLocalConfigPicker(true);
                        }
                    }
            );
            return;
        }
        showManualShareDialog();
    }

    private void showManualShareDialog() {
        showShareDetailDialog(
                buildDefaultNfcName(),
                nfcView.getUrl(),
                nfcView.getPackageName(),
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

        CupertinoDialogs.showCustom(
                this,
                getString(R.string.nfc_share_dialog_title),
                dialogView,
                getString(R.string.nfc_share_cancel),
                getString(R.string.nfc_share_confirm),
                () -> {
                    String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                    String url = urlEdit.getText() == null ? "" : urlEdit.getText().toString().trim();
                    String packageName = packageEdit.getText() == null ? "" : packageEdit.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        setStatus(getString(R.string.nfc_share_need_name), true);
                        return false;
                    }
                    if (TextUtils.isEmpty(url)) {
                        setStatus(getString(R.string.nfc_mock_need_url), true);
                        return false;
                    }
                    if (TextUtils.isEmpty(packageName)) {
                        setStatus(getString(R.string.nfc_mock_need_package), true);
                        return false;
                    }

                    nfcView.setUrl(url);
                    nfcView.setPackageName(packageName);
                    currentPayloadSource = TextUtils.isEmpty(source) ? "manual" : source;
                    persistNfcConfig();
                    uploadSharedNfc(name, url, packageName);
                    return true;
                },
                null
        );
    }

    private void showSaveConfigDialog() {
        String url = nfcView.getUrl();
        String packageName = nfcView.getPackageName();
        if (TextUtils.isEmpty(url)) {
            setStatus(getString(R.string.nfc_mock_need_url), true);
            return;
        }
        if (TextUtils.isEmpty(packageName)) {
            setStatus(getString(R.string.nfc_mock_need_package), true);
            return;
        }

        CupertinoDialogs.showInput(
                this,
                getString(R.string.nfc_save_dialog_title),
                null,
                buildDefaultLocalConfigName(),
                getString(R.string.nfc_save_name_hint),
                getString(R.string.nfc_share_cancel),
                getString(R.string.nfc_save_confirm),
                value -> {
                    String name = value == null ? "" : value.trim();
                    if (TextUtils.isEmpty(name)) {
                        setStatus(getString(R.string.nfc_save_need_name), true);
                        return;
                    }
                    prefsStore.saveSavedNfcConfig(
                            new SavedNfcConfig(name, url, packageName, currentPayloadSource)
                    );
                    persistNfcConfig();
                    setStatus(getString(R.string.nfc_save_success), false);
                }
        );
    }

    private void showLocalConfigPicker(boolean shareAfterPick) {
        List<SavedNfcConfig> configs = prefsStore.getSavedNfcConfigs();
        if (configs.isEmpty()) {
            setStatus(getString(R.string.nfc_local_empty), true);
            return;
        }

        CharSequence[] items = new CharSequence[configs.size()];
        for (int index = 0; index < configs.size(); index++) {
            SavedNfcConfig config = configs.get(index);
            items[index] = config.getName() + " · " + config.getPackageName();
        }

        ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        CupertinoDialogs.DialogHandle[] dialogHandle = new CupertinoDialogs.DialogHandle[1];
        dialogHandle[0] = CupertinoDialogs.showContent(
                this,
                getString(shareAfterPick ? R.string.nfc_share_local_pick_title : R.string.nfc_local_pick_title),
                listView,
                getString(R.string.nfc_share_cancel)
        );
        listView.setOnItemClickListener((parent, view, which, id) -> {
                    SavedNfcConfig selected = configs.get(which);
                    dialogHandle[0].dismiss();
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
                });
    }

    private void applySavedNfcConfig(SavedNfcConfig config) {
        nfcView.setUrl(config.getUrl());
        nfcView.setPackageName(config.getPackageName());
        currentPayloadSource = TextUtils.isEmpty(config.getSource()) ? "saved" : config.getSource();
        persistNfcConfig();
        setStatus(getString(R.string.nfc_local_loaded), false);
    }

    private void uploadSharedNfc(String name, String url, String packageName) {
        if (!GoUtils.isNetworkAvailable(this)) {
            setStatus(getString(R.string.app_error_network), true);
            return;
        }
        String payloadSource = TextUtils.isEmpty(currentPayloadSource) ? "manual" : currentPayloadSource;
        setStatus(getString(R.string.nfc_share_uploading), false);
        ioExecutor.execute(() -> {
            try {
                ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .uploadNfc(name, new NfcPayload(url, packageName, payloadSource));
                runOnUiThread(() -> {
                    currentPayloadSource = payloadSource;
                    persistNfcConfig();
                    setStatus(getString(R.string.nfc_share_success), false);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> setStatus(buildDetailedToast(R.string.nfc_share_failed, exception), true));
            }
        });
    }

    private void loadSharedNfcEntries() {
        if (!GoUtils.isNetworkAvailable(this)) {
            setStatus(getString(R.string.app_error_network), true);
            return;
        }
        setStatus(getString(R.string.nfc_download_loading), false);
        ioExecutor.execute(() -> {
            try {
                List<SharedNfcEntry> entries = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedNfcEntries();
                runOnUiThread(() -> showSharedNfcPicker(entries));
            } catch (Exception exception) {
                runOnUiThread(() -> setStatus(buildDetailedToast(R.string.nfc_download_failed, exception), true));
            }
        });
    }

    private void showSharedNfcPicker(List<SharedNfcEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            setStatus(getString(R.string.nfc_download_empty), true);
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

        CupertinoDialogs.DialogHandle[] dialogHandle = new CupertinoDialogs.DialogHandle[1];
        dialogHandle[0] = CupertinoDialogs.showContent(
                this,
                getString(R.string.nfc_download_pick_title),
                dialogView,
                getString(R.string.nfc_share_cancel)
        );
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (filteredItems.isEmpty()) {
                return;
            }
            dialogHandle[0].dismiss();
            applySharedNfc(filteredItems.get(position).entry);
        });
        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                filterNfcItems(editable == null ? "" : editable.toString(), allItems, filteredItems, adapter);
            }
        });
    }

    private void applySharedNfc(SharedNfcEntry entry) {
        nfcView.setUrl(entry.getUrl());
        nfcView.setPackageName(entry.getPackageName());
        currentPayloadSource = TextUtils.isEmpty(entry.getSource()) ? "shared" : entry.getSource();
        persistNfcConfig();
        setStatus(getString(R.string.nfc_download_loaded), false);
    }

    private void copyText(String label, String value) {
        if (TextUtils.isEmpty(value)) {
            setStatus(getString(R.string.nfc_copy_empty), true);
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
            setStatus(getString(R.string.nfc_copied), false);
        }
    }

    private void setStatus(String text, boolean warning) {
        nfcView.setStatus(text, warning);
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
        String packageName = nfcView.getPackageName();
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
            nfcView.setUrl(cachedUrl);
        }
        if (!TextUtils.isEmpty(cachedPackage)) {
            nfcView.setPackageName(cachedPackage);
        }
        currentPayloadSource = prefsStore.getNfcSource();
    }

    private void persistNfcConfig() {
        prefsStore.saveNfcConfig(nfcView.getUrl(), nfcView.getPackageName(), currentPayloadSource);
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

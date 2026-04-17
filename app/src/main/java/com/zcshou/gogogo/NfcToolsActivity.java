package com.zcshou.gogogo;

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
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.zcshou.gogogo.config.SimulationPrefsStore;
import com.zcshou.gogogo.nfc.data.AndroidNfcPayloadDispatcher;
import com.zcshou.gogogo.nfc.data.AndroidNfcPayloadReader;
import com.zcshou.gogogo.nfc.domain.NfcPayload;
import com.zcshou.gogogo.nfc.domain.NfcPayloadDispatchResult;
import com.zcshou.gogogo.nfc.domain.NfcPayloadParser;
import com.zcshou.gogogo.nfc.domain.ReadNfcPayloadUseCase;
import com.zcshou.gogogo.nfc.domain.SendNfcPayloadUseCase;
import com.zcshou.gogogo.share.domain.model.SharedNfcEntry;
import com.zcshou.gogogo.share.presentation.ShareModule;
import com.zcshou.utils.GoUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NfcToolsActivity extends BaseActivity {
    public static final String EXTRA_START_ACTION = "start_action";
    public static final String ACTION_SHARE = "share";
    public static final String ACTION_DOWNLOAD = "download";

    private NfcAdapter nfcAdapter;
    private PendingIntent foregroundDispatchIntent;
    private ReadNfcPayloadUseCase readNfcPayloadUseCase;
    private SendNfcPayloadUseCase sendNfcPayloadUseCase;
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
        sendNfcPayloadUseCase = new SendNfcPayloadUseCase(new AndroidNfcPayloadDispatcher());

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

        NfcPayloadDispatchResult result = sendNfcPayloadUseCase.send(
                this,
                new NfcPayload(url, packageName, currentPayloadSource)
        );
        persistNfcConfig();
        if (result.getStatus() == NfcPayloadDispatchResult.Status.NDEF_SENT) {
            setStatus(mockStatusView, getString(R.string.nfc_mock_sent), false);
        } else if (result.getStatus() == NfcPayloadDispatchResult.Status.FALLBACK_VIEW_SENT) {
            setStatus(mockStatusView, getString(R.string.nfc_mock_opened_browser), false);
        } else {
            String detail = result.getDetail();
            setStatus(
                    mockStatusView,
                    getString(R.string.nfc_mock_failed) + (TextUtils.isEmpty(detail) ? "" : detail),
                    true
            );
        }
    }

    private void showShareDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nfc_share, null);
        EditText nameInput = dialogView.findViewById(R.id.nfc_share_name_input);
        EditText urlEdit = dialogView.findViewById(R.id.nfc_share_url_input);
        EditText packageEdit = dialogView.findViewById(R.id.nfc_share_package_input);

        nameInput.setText(buildDefaultNfcName());
        urlEdit.setText(getInputText(urlInput));
        packageEdit.setText(getInputText(packageInput));

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
            dialog.dismiss();
            uploadSharedNfc(name, url, packageName);
        }));
        dialog.show();
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
                    GoUtils.DisplayToast(this, getString(R.string.nfc_share_success));
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

        CharSequence[] items = new CharSequence[entries.size()];
        for (int index = 0; index < entries.size(); index++) {
            SharedNfcEntry entry = entries.get(index);
            items[index] = entry.getName() + " · " + entry.getPackageName();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.nfc_download_pick_title)
                .setItems(items, (dialog, which) -> applySharedNfc(entries.get(which)))
                .show();
    }

    private void applySharedNfc(SharedNfcEntry entry) {
        urlInput.setText(entry.getUrl());
        packageInput.setText(entry.getPackageName());
        currentPayloadSource = TextUtils.isEmpty(entry.getSource()) ? "shared" : entry.getSource();
        persistNfcConfig();
        setStatus(mockStatusView, getString(R.string.nfc_download_loaded), false);
        GoUtils.DisplayToast(this, getString(R.string.nfc_download_loaded));
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
            GoUtils.DisplayToast(this, getString(R.string.nfc_copied));
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
}

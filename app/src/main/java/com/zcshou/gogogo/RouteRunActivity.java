package com.acooldog.toolbox;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.acooldog.toolbox.nfc.data.AndroidNfcPayloadDispatcher;
import com.acooldog.toolbox.nfc.domain.NfcPayload;
import com.acooldog.toolbox.nfc.domain.NfcPayloadDispatchResult;
import com.acooldog.toolbox.nfc.domain.SendNfcPayloadUseCase;
import com.acooldog.toolbox.config.SimulationPrefsStore;
import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteSimulationConfig;
import com.acooldog.toolbox.route.domain.service.LocationSimulationGateway;
import com.acooldog.toolbox.route.presentation.RouteRunViewModel;
import com.acooldog.toolbox.share.domain.model.SharedNfcEntry;
import com.acooldog.toolbox.share.domain.model.SharedRoutePayload;
import com.acooldog.toolbox.share.domain.model.SharedRouteSummary;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.service.ServiceGo;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteRunActivity extends BaseActivity {
    public static final String EXTRA_ROUTE_ID = "route_id";

    private RouteRunViewModel viewModel;
    private MapView mapView;
    private BaiduMap baiduMap;
    private TextView currentRouteView;
    private EditText speedInput;
    private EditText loopInput;
    private CheckBox speedFloatCheckBox;
    private Button toggleButton;
    private Marker simulationMarker;
    private ServiceGo.ServiceGoBinder serviceBinder;
    private boolean bound;
    private boolean mockLocationPromptShown;
    private View privacyMaskPanel;
    private TextView privacyMaskText;
    private ExecutorService ioExecutor;
    private SimulationPrefsStore prefsStore;
    private SendNfcPayloadUseCase sendNfcPayloadUseCase;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBinder = (ServiceGo.ServiceGoBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBinder = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_run);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(RouteRunViewModel.class);
        mapView = findViewById(R.id.map_run);
        baiduMap = mapView.getMap();
        currentRouteView = findViewById(R.id.tv_current_route);
        speedInput = findViewById(R.id.et_run_speed);
        loopInput = findViewById(R.id.et_loop_count);
        speedFloatCheckBox = findViewById(R.id.cb_speed_float);
        privacyMaskPanel = findViewById(R.id.route_privacy_mask_panel);
        privacyMaskText = findViewById(R.id.route_privacy_mask_text);
        Button loadButton = findViewById(R.id.btn_run_load);
        Button nfcEntryButton = findViewById(R.id.btn_run_nfc_entry);
        toggleButton = findViewById(R.id.btn_run_toggle);
        ioExecutor = Executors.newSingleThreadExecutor();
        prefsStore = new SimulationPrefsStore(getApplicationContext());
        sendNfcPayloadUseCase = new SendNfcPayloadUseCase(new AndroidNfcPayloadDispatcher());

        restoreSimulationPrefs();

        loadButton.setOnClickListener(v -> showLoadOptions());
        nfcEntryButton.setOnClickListener(v -> simulateCampusRunEntry());
        toggleButton.setOnClickListener(v -> toggleSimulation());

        observeViewModel();
        loadRoutes();
        promptMockLocationIfNeeded();

        String preselectedRouteId = getIntent().getStringExtra(EXTRA_ROUTE_ID);
        if (!TextUtils.isEmpty(preselectedRouteId)) {
            try {
                viewModel.selectRouteById(preselectedRouteId);
            } catch (Exception ignored) {
                // Ignore and keep picker available.
            }
        } else {
            restoreLastRouteSelection();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        persistSimulationPrefs();
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        viewModel.stopSimulation();
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void observeViewModel() {
        viewModel.getSelectedRoute().observe(this, routeDefinition -> {
            if (routeDefinition == null) {
                currentRouteView.setText(getString(R.string.route_current_none));
                baiduMap.clear();
                simulationMarker = null;
                updatePrivacyMask(null);
                return;
            }
            currentRouteView.setText(getString(R.string.route_current_format, buildRouteLabel(routeDefinition)));
            drawRoute(routeDefinition);
            updatePrivacyMask(routeDefinition);
            persistSimulationPrefs();
        });

        viewModel.getSimulationFrame().observe(this, simulationFrame -> {
            if (simulationFrame == null) {
                return;
            }
            LatLng latLng = new LatLng(
                    simulationFrame.getPoint().getBdLatitude(),
                    simulationFrame.getPoint().getBdLongitude()
            );
            if (simulationMarker == null) {
                simulationMarker = (Marker) baiduMap.addOverlay(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding)));
            } else {
                simulationMarker.setPosition(latLng);
            }
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(latLng));
        });

        viewModel.isRunning().observe(this, isRunning -> {
            boolean running = isRunning != null && isRunning;
            toggleButton.setText(running ? R.string.route_stop_button : R.string.route_start_button);
        });
    }

    private void loadRoutes() {
        try {
            viewModel.refreshRoutes();
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, "读取路线失败");
        }
    }

    private void showLoadOptions() {
        String[] options = {
                getString(R.string.route_pick_local_option),
                getString(R.string.route_pick_shared_option)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_pick_source_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRoutePicker();
                    } else {
                        loadSharedRoutes();
                    }
                })
                .show();
    }

    private void showRoutePicker() {
        List<RouteDefinition> routes = viewModel.getRoutes().getValue();
        if (routes == null || routes.isEmpty()) {
            GoUtils.DisplayToast(this, "请先创建、导入或下载路线");
            return;
        }

        CharSequence[] routeNames = new CharSequence[routes.size()];
        for (int i = 0; i < routes.size(); i++) {
            routeNames[i] = buildRouteLabel(routes.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle("选择路线")
                .setItems(routeNames, (dialog, which) -> viewModel.selectRoute(routes.get(which)))
                .show();
    }

    private void loadSharedRoutes() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        GoUtils.DisplayToast(this, getString(R.string.route_shared_loading));
        ioExecutor.execute(() -> {
            try {
                List<SharedRouteSummary> routes = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedRoutes();
                runOnUiThread(() -> showSharedRoutePicker(routes));
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_shared_load_failed, exception)));
            }
        });
    }

    private void showSharedRoutePicker(List<SharedRouteSummary> routes) {
        if (routes == null || routes.isEmpty()) {
            GoUtils.DisplayToast(this, getString(R.string.route_shared_empty));
            return;
        }

        CharSequence[] routeNames = new CharSequence[routes.size()];
        for (int index = 0; index < routes.size(); index++) {
            SharedRouteSummary route = routes.get(index);
            String privacyTag = route.isPrivacyMode() ? getString(R.string.route_shared_privacy_tag) + " " : "";
            routeNames[index] = String.format(
                    Locale.getDefault(),
                    "%s%s · %d %s",
                    privacyTag,
                    route.getName(),
                    route.getPointCount(),
                    getString(R.string.route_points_unit)
            );
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.route_shared_pick_title)
                .setItems(routeNames, (dialog, which) -> downloadSharedRoute(routes.get(which)))
                .show();
    }

    private void downloadSharedRoute(SharedRouteSummary summary) {
        GoUtils.DisplayToast(this, getString(R.string.route_shared_downloading));
        ioExecutor.execute(() -> {
            try {
                SharedRoutePayload payload = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedRoute(summary.getId());
                String localName = TextUtils.isEmpty(payload.getName()) ? summary.getName() : payload.getName();
                viewModel.saveRoute(localName, payload.getPoints(), payload.toShareInfo(true));
                runOnUiThread(() -> GoUtils.DisplayToast(
                        this,
                        getString(payload.isPrivacyMode() ? R.string.route_shared_downloaded_private : R.string.route_shared_downloaded)
                ));
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_shared_download_failed, exception)));
            }
        });
    }

    private void simulateCampusRunEntry() {
        String url = prefsStore.getNfcUrl();
        String packageName = prefsStore.getNfcPackageName();
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(packageName)) {
            showNfcConfigChoiceDialog();
            return;
        }
        dispatchNfcPayload(new NfcPayload(url, packageName, prefsStore.getNfcSource()));
    }

    private void showNfcConfigChoiceDialog() {
        String[] options = {
                getString(R.string.route_nfc_download_option),
                getString(R.string.route_nfc_manual_option)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_nfc_setup_title)
                .setMessage(R.string.route_nfc_setup_message)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        loadSharedNfcEntriesForSimulation();
                    } else {
                        showManualNfcInputDialog();
                    }
                })
                .setNegativeButton(R.string.nfc_share_cancel, null)
                .show();
    }

    private void loadSharedNfcEntriesForSimulation() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        GoUtils.DisplayToast(this, getString(R.string.nfc_download_loading));
        ioExecutor.execute(() -> {
            try {
                List<SharedNfcEntry> entries = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedNfcEntries();
                runOnUiThread(() -> showSharedNfcPickerForSimulation(entries));
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.nfc_download_failed, exception)));
            }
        });
    }

    private void showSharedNfcPickerForSimulation(List<SharedNfcEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            GoUtils.DisplayToast(this, getString(R.string.nfc_download_empty));
            return;
        }

        CharSequence[] items = new CharSequence[entries.size()];
        for (int index = 0; index < entries.size(); index++) {
            SharedNfcEntry entry = entries.get(index);
            items[index] = entry.getName() + " · " + entry.getPackageName();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.nfc_download_pick_title)
                .setItems(items, (dialog, which) -> applyNfcConfigAndSimulate(entries.get(which)))
                .show();
    }

    private void showManualNfcInputDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nfc_manual_input, null);
        EditText urlEdit = dialogView.findViewById(R.id.nfc_manual_url_input);
        EditText packageEdit = dialogView.findViewById(R.id.nfc_manual_package_input);
        urlEdit.setText(prefsStore.getNfcUrl());
        packageEdit.setText(prefsStore.getNfcPackageName());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.route_nfc_manual_title)
                .setView(dialogView)
                .setPositiveButton(R.string.route_nfc_manual_confirm, null)
                .setNegativeButton(R.string.nfc_share_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String url = urlEdit.getText() == null ? "" : urlEdit.getText().toString().trim();
            String packageName = packageEdit.getText() == null ? "" : packageEdit.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                GoUtils.DisplayToast(this, getString(R.string.nfc_mock_need_url));
                return;
            }
            if (TextUtils.isEmpty(packageName)) {
                GoUtils.DisplayToast(this, getString(R.string.nfc_mock_need_package));
                return;
            }
            dialog.dismiss();
            applyNfcConfigAndSimulate(new SharedNfcEntry("", "", url, packageName, "manual", System.currentTimeMillis()));
        }));
        dialog.show();
    }

    private void applyNfcConfigAndSimulate(SharedNfcEntry entry) {
        String source = TextUtils.isEmpty(entry.getSource()) ? "manual" : entry.getSource();
        prefsStore.saveNfcConfig(entry.getUrl(), entry.getPackageName(), source);
        dispatchNfcPayload(new NfcPayload(entry.getUrl(), entry.getPackageName(), source));
    }

    private void dispatchNfcPayload(NfcPayload payload) {
        NfcPayloadDispatchResult result = sendNfcPayloadUseCase.send(this, payload);
        if (result.getStatus() == NfcPayloadDispatchResult.Status.NDEF_SENT) {
            GoUtils.DisplayToast(this, getString(R.string.nfc_mock_sent));
            return;
        }
        if (result.getStatus() == NfcPayloadDispatchResult.Status.FALLBACK_VIEW_SENT) {
            GoUtils.DisplayToast(this, getString(R.string.nfc_mock_opened_browser));
            return;
        }
        String detail = result.getDetail();
        GoUtils.DisplayToast(
                this,
                getString(R.string.nfc_mock_failed) + (TextUtils.isEmpty(detail) ? "" : detail)
        );
    }

    private void toggleSimulation() {
        Boolean running = viewModel.isRunning().getValue();
        if (running != null && running) {
            viewModel.stopSimulation();
            GoUtils.DisplayToast(this, "路线模拟已停止");
            return;
        }

        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (routeDefinition == null) {
            GoUtils.DisplayToast(this, "请先选择路线");
            return;
        }
        if (!ensureSimulationReady()) {
            return;
        }

        try {
            double speed = Double.parseDouble(speedInput.getText().toString().trim());
            int loopCount = Integer.parseInt(loopInput.getText().toString().trim());
            RouteSimulationConfig config = new RouteSimulationConfig(speed, loopCount, speedFloatCheckBox.isChecked(), 1000L);
            ensureServiceStarted(routeDefinition);
            viewModel.startSimulation(config, new ServiceGateway());
            GoUtils.DisplayToast(this, "路线模拟已启动");
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, "模拟参数无效");
        }
    }

    private boolean ensureSimulationReady() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_network));
            return false;
        }
        if (!GoUtils.isGpsOpened(this)) {
            GoUtils.showEnableGpsDialog(this);
            return false;
        }
        if (!GoUtils.isAllowMockLocation(this)) {
            GoUtils.showEnableMockLocationDialog(this);
            return false;
        }
        return true;
    }

    private void promptMockLocationIfNeeded() {
        toggleButton.post(() -> {
            if (mockLocationPromptShown || isFinishing() || isDestroyed()) {
                return;
            }
            if (!GoUtils.isAllowMockLocation(this)) {
                mockLocationPromptShown = true;
                GoUtils.showEnableMockLocationDialog(this);
            }
        });
    }

    private void ensureServiceStarted(RouteDefinition routeDefinition) {
        RoutePoint firstPoint = routeDefinition.getPoints().get(0);
        Intent serviceIntent = new Intent(this, ServiceGo.class);
        serviceIntent.putExtra(MainActivity.LNG_MSG_ID, firstPoint.getWgsLongitude());
        serviceIntent.putExtra(MainActivity.LAT_MSG_ID, firstPoint.getWgsLatitude());
        serviceIntent.putExtra(MainActivity.ALT_MSG_ID, firstPoint.getAltitude());
        startForegroundService(serviceIntent);
        if (!bound) {
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
            bound = true;
        }
    }

    private void drawRoute(RouteDefinition routeDefinition) {
        baiduMap.clear();
        simulationMarker = null;
        List<LatLng> latLngs = new ArrayList<>();
        for (RoutePoint routePoint : routeDefinition.getPoints()) {
            latLngs.add(new LatLng(routePoint.getBdLatitude(), routePoint.getBdLongitude()));
        }
        if (latLngs.size() > 1) {
            OverlayOptions polyline = new PolylineOptions()
                    .width(8)
                    .color(0xAA1565C0)
                    .points(latLngs);
            baiduMap.addOverlay(polyline);
        }
        if (!latLngs.isEmpty()) {
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLngs.get(0), 18f));
        }
    }

    private void updatePrivacyMask(@Nullable RouteDefinition routeDefinition) {
        boolean shouldMask = routeDefinition != null && routeDefinition.shouldMaskMapForSimulation();
        privacyMaskPanel.setVisibility(shouldMask ? View.VISIBLE : View.GONE);
        privacyMaskText.setText(R.string.route_privacy_overlay_message);
    }

    private String buildRouteLabel(RouteDefinition routeDefinition) {
        if (routeDefinition == null) {
            return "";
        }
        if (routeDefinition.shouldMaskMapForSimulation()) {
            return getString(R.string.route_label_shared_private, routeDefinition.getName());
        }
        if (routeDefinition.isDownloadedFromShared()) {
            return getString(R.string.route_label_shared_downloaded, routeDefinition.getName());
        }
        if (routeDefinition.isSharedRoute()) {
            return getString(R.string.route_label_shared_uploaded, routeDefinition.getName());
        }
        return routeDefinition.getName();
    }

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }

    private void restoreSimulationPrefs() {
        speedInput.setText(prefsStore.getRouteSpeed());
        loopInput.setText(prefsStore.getRouteLoopCount());
        speedFloatCheckBox.setChecked(prefsStore.isRouteSpeedFloat());
    }

    private void persistSimulationPrefs() {
        RouteDefinition selectedRoute = viewModel.getSelectedRoute().getValue();
        prefsStore.saveRouteConfig(
                speedInput.getText() == null ? "" : speedInput.getText().toString(),
                loopInput.getText() == null ? "" : loopInput.getText().toString(),
                speedFloatCheckBox.isChecked(),
                selectedRoute == null ? "" : selectedRoute.getId()
        );
    }

    private void restoreLastRouteSelection() {
        String routeId = prefsStore.getLastRouteId();
        if (TextUtils.isEmpty(routeId)) {
            return;
        }
        try {
            viewModel.selectRouteById(routeId);
        } catch (Exception ignored) {
            // Ignore when the previous route no longer exists.
        }
    }

    private final class ServiceGateway implements LocationSimulationGateway {
        @Override
        public void pushLocation(double longitude, double latitude, double altitude, float speed, float bearing) {
            if (serviceBinder != null) {
                serviceBinder.setMotion(longitude, latitude, altitude, speed, bearing);
            }
        }
    }
}

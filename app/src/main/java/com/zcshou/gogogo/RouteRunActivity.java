package com.zcshou.gogogo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
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
import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteSimulationConfig;
import com.zcshou.gogogo.route.presentation.RouteRunViewModel;
import com.zcshou.gogogo.route.domain.service.LocationSimulationGateway;
import com.zcshou.service.ServiceGo;
import com.zcshou.utils.GoUtils;

import java.util.ArrayList;
import java.util.List;

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
        Button loadButton = findViewById(R.id.btn_run_load);
        toggleButton = findViewById(R.id.btn_run_toggle);

        loadButton.setOnClickListener(v -> showRoutePicker());
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
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
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
                return;
            }
            currentRouteView.setText("当前路线: " + routeDefinition.getName());
            drawRoute(routeDefinition);
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

    private void showRoutePicker() {
        List<RouteDefinition> routes = viewModel.getRoutes().getValue();
        if (routes == null || routes.isEmpty()) {
            GoUtils.DisplayToast(this, "请先在路线管理中导入或创建路线");
            return;
        }

        CharSequence[] routeNames = new CharSequence[routes.size()];
        for (int i = 0; i < routes.size(); i++) {
            routeNames[i] = routes.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("选择路线")
                .setItems(routeNames, (dialog, which) -> viewModel.selectRoute(routes.get(which)))
                .show();
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
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            GoUtils.showEnableFloatWindowDialog(this);
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

    private final class ServiceGateway implements LocationSimulationGateway {
        @Override
        public void pushLocation(double longitude, double latitude, double altitude, float speed, float bearing) {
            if (serviceBinder != null) {
                serviceBinder.setMotion(longitude, latitude, altitude, speed, bearing);
            }
        }
    }
}

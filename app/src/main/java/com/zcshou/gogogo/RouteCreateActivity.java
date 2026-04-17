package com.zcshou.gogogo;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.presentation.RouteCreateViewModel;
import com.zcshou.gogogo.share.domain.model.SharedRoutePayload;
import com.zcshou.gogogo.share.presentation.ShareModule;
import com.zcshou.utils.GoUtils;
import com.zcshou.utils.MapUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteCreateActivity extends BaseActivity {
    private MapView mapView;
    private BaiduMap baiduMap;
    private RouteCreateViewModel viewModel;
    private LocationClient locationClient;
    private LatLng currentLocation;
    private ExecutorService ioExecutor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_create);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(RouteCreateViewModel.class);
        mapView = findViewById(R.id.map_create);
        baiduMap = mapView.getMap();
        ioExecutor = Executors.newSingleThreadExecutor();

        initMap();
        initButtons();
        initLocationClient();
        observeRoutePoints();
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
        if (locationClient != null) {
            locationClient.stop();
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

    private void initMap() {
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                double[] wgsCoordinates = MapUtils.bd2wgs(latLng.longitude, latLng.latitude);
                viewModel.addPoint(new RoutePoint(
                        latLng.longitude,
                        latLng.latitude,
                        wgsCoordinates[0],
                        wgsCoordinates[1],
                        55d
                ));
            }

            @Override
            public void onMapPoiClick(com.baidu.mapapi.map.MapPoi mapPoi) {
                // No-op.
            }
        });
    }

    private void initButtons() {
        Button clearButton = findViewById(R.id.btn_create_clear);
        Button saveButton = findViewById(R.id.btn_create_save);
        Button shareButton = findViewById(R.id.btn_create_share);
        ImageButton locationButton = findViewById(R.id.btn_create_location);

        clearButton.setOnClickListener(v -> viewModel.clear());
        saveButton.setOnClickListener(v -> showSaveDialog());
        shareButton.setOnClickListener(v -> showShareDialog());
        locationButton.setOnClickListener(v -> moveToCurrentLocation());
    }

    private void initLocationClient() {
        try {
            locationClient = new LocationClient(getApplicationContext());
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, "定位组件初始化失败");
            return;
        }
        locationClient.registerLocationListener(new BDAbstractLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (bdLocation == null || bdLocation.getLatitude() == 0 || bdLocation.getLongitude() == 0) {
                    return;
                }
                currentLocation = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLocation, 18f));
            }
        });
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        option.setScanSpan(0);
        locationClient.setLocOption(option);
        locationClient.start();
    }

    private void observeRoutePoints() {
        viewModel.getRoutePoints().observe(this, routePoints -> {
            baiduMap.clear();
            if (routePoints == null || routePoints.isEmpty()) {
                return;
            }

            List<LatLng> latLngs = new ArrayList<>();
            for (RoutePoint routePoint : routePoints) {
                LatLng latLng = new LatLng(routePoint.getBdLatitude(), routePoint.getBdLongitude());
                latLngs.add(latLng);
                baiduMap.addOverlay(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding)));
            }

            if (latLngs.size() > 1) {
                OverlayOptions polyline = new PolylineOptions()
                        .width(8)
                        .color(0xAA2E7D32)
                        .points(latLngs);
                baiduMap.addOverlay(polyline);
            }
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(latLngs.get(latLngs.size() - 1)));
        });
    }

    private void moveToCurrentLocation() {
        if (currentLocation == null) {
            GoUtils.DisplayToast(this, "正在定位，请稍后重试");
            return;
        }
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLocation, 18f));
    }

    private void showSaveDialog() {
        if (!viewModel.canSave()) {
            GoUtils.DisplayToast(this, "至少绘制两个点后才能保存");
            return;
        }

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(buildDefaultRouteName());

        new AlertDialog.Builder(this)
                .setTitle("保存路线")
                .setView(input)
                .setPositiveButton("保存", (dialog, which) -> {
                    String routeName = input.getText() == null ? "" : input.getText().toString().trim();
                    if (routeName.isEmpty()) {
                        GoUtils.DisplayToast(this, "路线名称不能为空");
                        return;
                    }
                    try {
                        viewModel.saveRoute(routeName);
                        GoUtils.DisplayToast(this, "路线已保存");
                        finish();
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, "保存路线失败");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showShareDialog() {
        if (!viewModel.canSave()) {
            GoUtils.DisplayToast(this, getString(R.string.route_share_need_points));
            return;
        }

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_route_share, null);
        EditText nameInput = dialogView.findViewById(R.id.route_share_name_input);
        CheckBox privacyCheckBox = dialogView.findViewById(R.id.route_share_privacy_checkbox);
        nameInput.setText(buildDefaultRouteName());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.route_share_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.route_share_confirm, null)
                .setNegativeButton(R.string.route_share_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String routeName = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(routeName)) {
                GoUtils.DisplayToast(this, getString(R.string.route_share_name_empty));
                return;
            }
            dialog.dismiss();
            uploadSharedRoute(routeName, privacyCheckBox.isChecked());
        }));
        dialog.show();
    }

    private void uploadSharedRoute(String routeName, boolean privacyMode) {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        List<RoutePoint> points = new ArrayList<>(viewModel.getCurrentPoints());
        GoUtils.DisplayToast(this, getString(R.string.route_share_uploading));
        ioExecutor.execute(() -> {
            try {
                SharedRoutePayload payload = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .uploadRoute(routeName, privacyMode, points);
                String localName = TextUtils.isEmpty(payload.getName()) ? routeName : payload.getName();
                viewModel.saveRoute(localName, points, payload.toShareInfo(false));
                runOnUiThread(() -> {
                    GoUtils.DisplayToast(
                            this,
                            getString(privacyMode ? R.string.route_share_success_private : R.string.route_share_success)
                    );
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_share_failed, exception)));
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

    private String buildDefaultRouteName() {
        return "route_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }
}

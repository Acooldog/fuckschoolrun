package com.acooldog.toolbox;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.presentation.RouteCreateViewModel;
import com.acooldog.toolbox.route.presentation.RouteModule;
import com.acooldog.toolbox.share.domain.model.SharedRoutePayload;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.acooldog.toolbox.utils.MapUtils;
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
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteCreateActivity extends BaseActivity {
    private static final String POI_NAME = "poi_name";
    private static final String POI_ADDRESS = "poi_address";
    private static final String POI_LONGITUDE = "poi_longitude";
    private static final String POI_LATITUDE = "poi_latitude";

    private MapView mapView;
    private BaiduMap baiduMap;
    private RouteCreateViewModel viewModel;
    private LocationClient locationClient;
    private LatLng currentLocation;
    private ExecutorService ioExecutor;
    private PoiSearch poiSearch;
    private String currentCity = "";
    private LinearLayout searchResultContainer;
    private ListView searchResultList;

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
        initSearch();
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
        if (poiSearch != null) {
            poiSearch.destroy();
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
                searchResultContainer.setVisibility(View.GONE);
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

    private void initSearch() {
        SearchView searchView = findViewById(R.id.route_create_search_view);
        searchResultContainer = findViewById(R.id.route_search_result_container);
        searchResultList = findViewById(R.id.route_search_result_list);
        poiSearch = PoiSearch.newInstance();
        poiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult poiResult) {
                if (poiResult == null || poiResult.getAllPoi() == null) {
                    GoUtils.DisplayToast(RouteCreateActivity.this, getString(R.string.route_search_empty));
                    searchResultContainer.setVisibility(View.GONE);
                    return;
                }

                List<Map<String, Object>> data = getPoiResultList(poiResult.getAllPoi());
                if (data.isEmpty()) {
                    GoUtils.DisplayToast(RouteCreateActivity.this, getString(R.string.route_search_empty));
                    searchResultContainer.setVisibility(View.GONE);
                    return;
                }

                SimpleAdapter adapter = new SimpleAdapter(
                        RouteCreateActivity.this,
                        data,
                        R.layout.search_poi_item,
                        new String[]{POI_NAME, POI_ADDRESS, POI_LONGITUDE, POI_LATITUDE},
                        new int[]{R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude}
                );
                searchResultList.setAdapter(adapter);
                searchResultContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
                // No-op.
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {
                // No-op.
            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
                // No-op.
            }
        });

        searchResultList.setOnItemClickListener((parent, view, position, id) -> {
            String lng = ((android.widget.TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((android.widget.TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            LatLng target = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(target, 18f));
            searchResultContainer.setVisibility(View.GONE);
            GoUtils.DisplayToast(this, getString(R.string.route_search_use_result));
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                requestPoiSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    searchResultContainer.setVisibility(View.GONE);
                    return true;
                }
                requestPoiSearch(newText);
                return true;
            }
        });
    }

    private void requestPoiSearch(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        try {
            String city = TextUtils.isEmpty(currentCity) ? "北京" : currentCity;
            poiSearch.searchInCity(
                    new PoiCitySearchOption()
                            .city(city)
                            .keyword(keyword)
                            .pageCapacity(20)
                            .pageNum(0)
                            .cityLimit(false)
                            .isReturnAddr(true)
            );
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_search));
        }
    }

    @NonNull
    private List<Map<String, Object>> getPoiResultList(List<PoiInfo> poiInfos) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (PoiInfo info : poiInfos) {
            if (info == null || info.location == null) {
                continue;
            }

            Map<String, Object> poiItem = new HashMap<>();
            poiItem.put(POI_NAME, info.name == null ? "" : info.name);
            String city = info.city == null ? "" : info.city;
            String area = info.area == null ? "" : info.area;
            String address = info.address == null ? "" : info.address;
            poiItem.put(POI_ADDRESS, (city + " " + area + " " + address).trim());
            poiItem.put(POI_LONGITUDE, String.valueOf(info.location.longitude));
            poiItem.put(POI_LATITUDE, String.valueOf(info.location.latitude));
            data.add(poiItem);
        }
        return data;
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
                if (!TextUtils.isEmpty(bdLocation.getCity())) {
                    currentCity = bdLocation.getCity();
                }
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
        boolean hasCurrentPoints = viewModel.canSave();
        List<RouteDefinition> localRoutes = getLocalRoutes();
        boolean hasLocalRoutes = !localRoutes.isEmpty();
        if (!hasCurrentPoints && !hasLocalRoutes) {
            GoUtils.DisplayToast(this, getString(R.string.route_share_need_points));
            return;
        }

        if (hasCurrentPoints && hasLocalRoutes) {
            String[] options = {
                    getString(R.string.route_share_current_option),
                    getString(R.string.route_share_local_option)
            };
            new AlertDialog.Builder(this)
                    .setTitle(R.string.route_share_dialog_title)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showShareDetailDialog(buildDefaultRouteName(), new ArrayList<>(viewModel.getCurrentPoints()));
                        } else {
                            showLocalRouteSelector(localRoutes);
                        }
                    })
                    .show();
            return;
        }

        if (hasCurrentPoints) {
            showShareDetailDialog(buildDefaultRouteName(), new ArrayList<>(viewModel.getCurrentPoints()));
            return;
        }

        showLocalRouteSelector(localRoutes);
    }

    private void showLocalRouteSelector(List<RouteDefinition> routes) {
        CharSequence[] routeNames = new CharSequence[routes.size()];
        for (int index = 0; index < routes.size(); index++) {
            routeNames[index] = routes.get(index).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.route_share_select_local_title)
                .setItems(routeNames, (dialog, which) -> {
                    RouteDefinition selected = routes.get(which);
                    showShareDetailDialog(selected.getName(), new ArrayList<>(selected.getPoints()));
                })
                .show();
    }

    private void showShareDetailDialog(String defaultName, List<RoutePoint> points) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_route_share, null);
        EditText nameInput = dialogView.findViewById(R.id.route_share_name_input);
        CheckBox privacyCheckBox = dialogView.findViewById(R.id.route_share_privacy_checkbox);
        nameInput.setText(defaultName);

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
            uploadSharedRoute(routeName, privacyCheckBox.isChecked(), points);
        }));
        dialog.show();
    }

    private void uploadSharedRoute(String routeName, boolean privacyMode, List<RoutePoint> points) {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
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

    @NonNull
    private List<RouteDefinition> getLocalRoutes() {
        try {
            return RouteModule.from(getApplicationContext()).getRoutesUseCase().execute();
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
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

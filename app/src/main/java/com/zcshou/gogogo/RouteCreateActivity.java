package com.acooldog.toolbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteShareInfo;
import com.acooldog.toolbox.route.domain.service.RouteEditUtils;
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
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
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
    public static final String EXTRA_EDIT_ROUTE_ID = "edit_route_id";

    private static final String POI_NAME = "poi_name";
    private static final String POI_ADDRESS = "poi_address";
    private static final String POI_LONGITUDE = "poi_longitude";
    private static final String POI_LATITUDE = "poi_latitude";
    private static final String PREF_ROUTE_RECORD_INTERVAL_SECONDS = "pref_route_record_interval_seconds";
    private static final String PREF_ROUTE_CREATE = "route_create_prefs";
    private static final String MARKER_INDEX_KEY = "marker_index";
    private static final String MARKER_TYPE_KEY = "marker_type";
    private static final String MARKER_TYPE_START = "start";
    private static final String MARKER_TYPE_VERTEX = "vertex";
    private static final double DEFAULT_ALTITUDE = 55d;
    private static final float DEFAULT_ZOOM_LEVEL = 18f;
    private static final double ROUTE_CLOSED_TOLERANCE = 0.0000001d;
    private static final int DEFAULT_RECORD_INTERVAL_SECONDS = 3;
    private static final int MIN_RECORD_INTERVAL_SECONDS = 1;
    private static final int LOCATION_REFRESH_INTERVAL_MS = 1000;
    private static final int TOOL_MODE_DRAW = 0;
    private static final int TOOL_MODE_EDIT = 1;
    private static final double ROUTE_INSERT_MAX_DISTANCE_METERS = 20d;

    private MapView mapView;
    private BaiduMap baiduMap;
    private RouteCreateViewModel viewModel;
    private LocationClient locationClient;
    private LatLng currentLocation;
    private BDLocation lastLocationFix;
    private ExecutorService ioExecutor;
    private PoiSearch poiSearch;
    private String currentCity = "";
    private LinearLayout searchResultContainer;
    private ListView searchResultList;
    private TextView routeHintView;
    private TextView selectedPointView;
    private Button clearButton;
    private Button saveButton;
    private Button shareButton;
    private Button recordButton;
    private Button closeRouteButton;
    private Button drawToolButton;
    private Button editToolButton;
    private Button autoAddToolButton;
    private Button autoReduceToolButton;
    private Button smoothCornerToolButton;
    private Button undoToolButton;
    private Button redoToolButton;
    private Button keepToolButton;
    private Button deleteToolButton;
    private ImageButton toggleToolButton;
    private LinearLayout toolboxPanel;
    private SwitchCompat recordModeSwitch;
    private boolean updatingRecordModeSwitch;
    private boolean isRecording;
    private boolean hasCenteredOnLocation;
    private int recordIntervalSeconds;
    private int toolMode = TOOL_MODE_DRAW;
    private int selectedPointIndex = -1;
    private boolean toolboxCollapsed;
    private boolean closeRoutePromptVisible;
    private String editingRouteId;
    private String editingRouteName;
    private RouteShareInfo editingShareInfo = RouteShareInfo.NONE;
    private BitmapDescriptor routeVertexDescriptor;
    private BitmapDescriptor routeVertexSelectedDescriptor;
    private final Handler recordHandler = new Handler(Looper.getMainLooper());
    private final Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRecording) {
                return;
            }
            appendRecordedLocation(lastLocationFix);
            recordHandler.postDelayed(this, recordIntervalSeconds * 1000L);
        }
    };

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
        recordIntervalSeconds = getSharedPreferences(PREF_ROUTE_CREATE, MODE_PRIVATE)
                .getInt(PREF_ROUTE_RECORD_INTERVAL_SECONDS, DEFAULT_RECORD_INTERVAL_SECONDS);

        initMap();
        initButtons();
        initSearch();
        initLocationClient();
        observeRoutePoints();
        loadEditingRouteIfNeeded();
        updateCreateModeUi();
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
        stopRecordScheduler();
        if (locationClient != null) {
            locationClient.stop();
        }
        if (poiSearch != null) {
            poiSearch.destroy();
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        recycleMarkerDescriptors();
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (isRecording) {
            GoUtils.DisplayToast(this, getString(R.string.route_record_exit_while_recording));
            return true;
        }
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isRecording) {
            GoUtils.DisplayToast(this, getString(R.string.route_record_exit_while_recording));
            return;
        }
        super.onBackPressed();
    }

    private void initMap() {
        mapView.showZoomControls(false);
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (searchResultContainer != null) {
                    searchResultContainer.setVisibility(View.GONE);
                }
                if (isRecordModeEnabled()) {
                    return;
                }
                if (toolMode == TOOL_MODE_EDIT) {
                    if (selectedPointIndex >= 0) {
                        viewModel.replacePointAt(selectedPointIndex, buildMovedRoutePoint(
                                viewModel.getCurrentPoints().get(selectedPointIndex),
                                latLng
                        ));
                        return;
                    }
                    if (insertRoutePointNearClick(latLng)) {
                        return;
                    }
                    selectedPointIndex = -1;
                    closeRoutePromptVisible = false;
                    updateCreateModeUi();
                    drawRoute(viewModel.getCurrentPoints());
                    return;
                }
                closeRoutePromptVisible = false;
                viewModel.addPoint(buildRoutePoint(latLng, DEFAULT_ALTITUDE));
            }

            @Override
            public void onMapPoiClick(com.baidu.mapapi.map.MapPoi mapPoi) {
                // No-op.
            }
        });
        baiduMap.setOnMarkerClickListener(this::handleMarkerClick);
    }

    private boolean handleMarkerClick(Marker marker) {
        if (isRecordModeEnabled()) {
            return false;
        }
        if (marker == null || marker.getExtraInfo() == null) {
            return false;
        }
        String markerType = marker.getExtraInfo().getString(MARKER_TYPE_KEY, MARKER_TYPE_VERTEX);
        if (toolMode == TOOL_MODE_EDIT) {
            int tappedIndex = marker.getExtraInfo().getInt(MARKER_INDEX_KEY, -1);
            selectedPointIndex = tappedIndex == selectedPointIndex ? -1 : tappedIndex;
            closeRoutePromptVisible = false;
            updateCreateModeUi();
            drawRoute(viewModel.getCurrentPoints());
            return true;
        }
        if (MARKER_TYPE_START.equals(markerType) && canCloseCurrentRoute()) {
            closeRoutePromptVisible = true;
            selectedPointIndex = -1;
            updateCreateModeUi();
            return true;
        }
        closeRoutePromptVisible = false;
        updateCreateModeUi();
        return false;
    }

    private void initButtons() {
        routeHintView = findViewById(R.id.route_create_hint);
        selectedPointView = findViewById(R.id.route_create_selected_point);
        clearButton = findViewById(R.id.btn_create_clear);
        saveButton = findViewById(R.id.btn_create_save);
        shareButton = findViewById(R.id.btn_create_share);
        recordButton = findViewById(R.id.btn_create_record);
        closeRouteButton = findViewById(R.id.btn_create_close_route);
        recordModeSwitch = findViewById(R.id.switch_create_record_mode);
        drawToolButton = findViewById(R.id.btn_create_tool_draw);
        editToolButton = findViewById(R.id.btn_create_tool_edit);
        autoAddToolButton = findViewById(R.id.btn_create_tool_auto_add);
        autoReduceToolButton = findViewById(R.id.btn_create_tool_auto_reduce);
        smoothCornerToolButton = findViewById(R.id.btn_create_tool_smooth_corner);
        undoToolButton = findViewById(R.id.btn_create_tool_undo);
        redoToolButton = findViewById(R.id.btn_create_tool_redo);
        keepToolButton = findViewById(R.id.btn_create_tool_keep);
        deleteToolButton = findViewById(R.id.btn_create_tool_delete);
        toggleToolButton = findViewById(R.id.btn_create_tool_toggle);
        toolboxPanel = findViewById(R.id.route_create_toolbox_panel);
        ImageButton recordSettingsButton = findViewById(R.id.btn_create_record_settings);
        ImageButton locationButton = findViewById(R.id.btn_create_location);
        RadioGroup mapTypeGroup = findViewById(R.id.route_create_map_type_group);

        mapTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.route_create_map_normal) {
                baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            } else if (checkedId == R.id.route_create_map_satellite) {
                baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        });

        clearButton.setOnClickListener(v -> {
            if (isRecording) {
                return;
            }
            selectedPointIndex = -1;
            closeRoutePromptVisible = false;
            viewModel.clear();
            updateCreateModeUi();
        });
        saveButton.setOnClickListener(v -> showSaveDialog());
        shareButton.setOnClickListener(v -> showShareDialog());
        recordButton.setOnClickListener(v -> toggleRecording());
        closeRouteButton.setOnClickListener(v -> closeCurrentRoute());
        recordSettingsButton.setOnClickListener(v -> showRecordSettingsDialog());
        locationButton.setOnClickListener(v -> moveToCurrentLocation());
        drawToolButton.setOnClickListener(v -> switchToolMode(TOOL_MODE_DRAW));
        editToolButton.setOnClickListener(v -> switchToolMode(TOOL_MODE_EDIT));
        autoAddToolButton.setOnClickListener(v -> showAutoAddPointsDialog());
        autoReduceToolButton.setOnClickListener(v -> showAutoReducePointsDialog());
        smoothCornerToolButton.setOnClickListener(v -> showSmoothCornersDialog());
        undoToolButton.setOnClickListener(v -> {
            selectedPointIndex = -1;
            closeRoutePromptVisible = false;
            viewModel.undo();
        });
        redoToolButton.setOnClickListener(v -> {
            selectedPointIndex = -1;
            closeRoutePromptVisible = false;
            viewModel.redo();
        });
        keepToolButton.setOnClickListener(v -> clearSelectedPoint());
        deleteToolButton.setOnClickListener(v -> deleteSelectedPoint());
        toggleToolButton.setOnClickListener(v -> {
            toolboxCollapsed = !toolboxCollapsed;
            updateCreateModeUi();
        });
        recordModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingRecordModeSwitch) {
                return;
            }
            if (isRecording && !isChecked) {
                updatingRecordModeSwitch = true;
                recordModeSwitch.setChecked(true);
                updatingRecordModeSwitch = false;
                GoUtils.DisplayToast(this, getString(R.string.route_record_exit_while_recording));
                return;
            }
            if (isChecked) {
                switchToolMode(TOOL_MODE_DRAW);
            } else {
                updateCreateModeUi();
            }
        });
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
                    searchResultContainer.setVisibility(View.GONE);
                    return;
                }

                List<Map<String, Object>> data = getPoiResultList(poiResult.getAllPoi());
                if (data.isEmpty()) {
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
            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            LatLng target = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(target, DEFAULT_ZOOM_LEVEL));
            searchResultContainer.setVisibility(View.GONE);
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
        } catch (Exception ignored) {
            searchResultContainer.setVisibility(View.GONE);
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
                lastLocationFix = bdLocation;
                currentLocation = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                if (!TextUtils.isEmpty(bdLocation.getCity())) {
                    currentCity = bdLocation.getCity();
                }
                if (!hasCenteredOnLocation) {
                    hasCenteredOnLocation = true;
                    baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM_LEVEL));
                } else if (isRecording) {
                    baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(currentLocation));
                }
            }
        });
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setOpenGnss(true);
        option.setCoorType("bd09ll");
        option.setScanSpan(LOCATION_REFRESH_INTERVAL_MS);
        option.setIsNeedAddress(true);
        option.setLocationNotify(true);
        option.setIgnoreKillProcess(true);
        option.SetIgnoreCacheException(true);
        option.setIsNeedAltitude(true);
        locationClient.setLocOption(option);
        locationClient.start();
    }

    private void observeRoutePoints() {
        viewModel.getRoutePoints().observe(this, routePoints -> {
            if (selectedPointIndex >= routePoints.size()) {
                selectedPointIndex = -1;
            }
            if (!canCloseCurrentRoute()) {
                closeRoutePromptVisible = false;
            }
            drawRoute(routePoints);
            updateCreateModeUi();
        });
    }

    private void loadEditingRouteIfNeeded() {
        String routeId = getIntent().getStringExtra(EXTRA_EDIT_ROUTE_ID);
        if (TextUtils.isEmpty(routeId)) {
            return;
        }
        RouteDefinition routeDefinition = findLocalRoute(routeId);
        if (routeDefinition == null) {
            GoUtils.DisplayToast(this, getString(R.string.route_edit_missing));
            finish();
            return;
        }
        editingRouteId = routeDefinition.getId();
        editingRouteName = routeDefinition.getName();
        editingShareInfo = routeDefinition.getShareInfo();
        viewModel.loadPoints(routeDefinition.getPoints());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.route_edit_title);
        }
        routeHintView.setText(R.string.route_edit_loaded);
    }

    @Nullable
    private RouteDefinition findLocalRoute(@NonNull String routeId) {
        for (RouteDefinition routeDefinition : getLocalRoutes()) {
            if (routeId.equals(routeDefinition.getId())) {
                return routeDefinition;
            }
        }
        return null;
    }

    private void drawRoute(@Nullable List<RoutePoint> routePoints) {
        baiduMap.clear();
        if (routePoints == null || routePoints.isEmpty()) {
            return;
        }

        List<LatLng> latLngs = new ArrayList<>(routePoints.size());
        for (RoutePoint routePoint : routePoints) {
            latLngs.add(new LatLng(routePoint.getBdLatitude(), routePoint.getBdLongitude()));
        }

        if (latLngs.size() > 1) {
            OverlayOptions polyline = new PolylineOptions()
                    .width(8)
                    .color(0xAA2E7D32)
                    .points(latLngs);
            baiduMap.addOverlay(polyline);
        }

        if (toolMode == TOOL_MODE_EDIT && !isRecordModeEnabled()) {
            for (int index = 0; index < latLngs.size(); index++) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLngs.get(index))
                        .icon(index == selectedPointIndex
                                ? getSelectedRouteVertexDescriptor()
                                : getRouteVertexDescriptor());
                android.os.Bundle bundle = new android.os.Bundle();
                bundle.putInt(MARKER_INDEX_KEY, index);
                bundle.putString(MARKER_TYPE_KEY, MARKER_TYPE_VERTEX);
                markerOptions.extraInfo(bundle);
                baiduMap.addOverlay(markerOptions);
            }
        } else {
            LatLng firstPoint = latLngs.get(0);
            LatLng lastPoint = latLngs.get(latLngs.size() - 1);
            MarkerOptions startMarker = new MarkerOptions()
                    .position(firstPoint)
                    .icon(getRouteVertexDescriptor());
            android.os.Bundle startBundle = new android.os.Bundle();
            startBundle.putInt(MARKER_INDEX_KEY, 0);
            startBundle.putString(MARKER_TYPE_KEY, MARKER_TYPE_START);
            startMarker.extraInfo(startBundle);
            baiduMap.addOverlay(startMarker);
            if (latLngs.size() > 1) {
                MarkerOptions endMarker = new MarkerOptions()
                        .position(lastPoint)
                        .icon(getSelectedRouteVertexDescriptor());
                android.os.Bundle endBundle = new android.os.Bundle();
                endBundle.putInt(MARKER_INDEX_KEY, latLngs.size() - 1);
                endBundle.putString(MARKER_TYPE_KEY, MARKER_TYPE_VERTEX);
                endMarker.extraInfo(endBundle);
                baiduMap.addOverlay(endMarker);
            }
        }

        LatLng target = selectedPointIndex >= 0 && selectedPointIndex < latLngs.size()
                ? latLngs.get(selectedPointIndex)
                : latLngs.get(latLngs.size() - 1);
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(target));
    }

    private void moveToCurrentLocation() {
        if (currentLocation == null) {
            GoUtils.DisplayToast(this, "正在定位，请稍后重试");
            return;
        }
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM_LEVEL));
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
            return;
        }
        if (recordModeSwitch == null || !recordModeSwitch.isChecked()) {
            return;
        }

        isRecording = true;
        appendRecordedLocation(lastLocationFix);
        startRecordScheduler();
        updateCreateModeUi();
        GoUtils.DisplayToast(
                this,
                getString(lastLocationFix == null ? R.string.route_record_waiting_location : R.string.route_record_started)
        );
    }

    private void stopRecording() {
        isRecording = false;
        stopRecordScheduler();
        updateCreateModeUi();
        if (!viewModel.canSave()) {
            GoUtils.DisplayToast(this, getString(R.string.route_record_need_points));
            return;
        }
        showRecordingStopDialog();
    }

    private void showRecordingStopDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_record_stop_dialog_title)
                .setMessage(R.string.route_record_stop_dialog_message)
                .setPositiveButton(R.string.route_record_stop_save_option, (dialog, which) -> showSaveDialog())
                .setNeutralButton(
                        R.string.route_record_stop_share_option,
                        (dialog, which) -> showShareDetailDialog(getCurrentRouteName(), new ArrayList<>(viewModel.getCurrentPoints()))
                )
                .setNegativeButton(R.string.route_share_cancel, null)
                .show();
    }

    private boolean appendRecordedLocation(@Nullable BDLocation bdLocation) {
        if (bdLocation == null || bdLocation.getLatitude() == 0 || bdLocation.getLongitude() == 0) {
            return false;
        }
        LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
        viewModel.addPoint(buildRoutePoint(latLng, sanitizeAltitude(bdLocation)), false);
        return true;
    }

    private void startRecordScheduler() {
        stopRecordScheduler();
        recordHandler.postDelayed(recordRunnable, recordIntervalSeconds * 1000L);
    }

    private void stopRecordScheduler() {
        recordHandler.removeCallbacks(recordRunnable);
    }

    private void showRecordSettingsDialog() {
        if (isRecording) {
            GoUtils.DisplayToast(this, getString(R.string.route_record_setting_stop_first));
            return;
        }
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(recordIntervalSeconds));
        input.setHint(String.valueOf(DEFAULT_RECORD_INTERVAL_SECONDS));

        new AlertDialog.Builder(this)
                .setTitle(R.string.route_record_setting_title)
                .setMessage(R.string.route_record_setting_message)
                .setView(input)
                .setPositiveButton(R.string.route_record_setting_confirm, (dialog, which) -> {
                    String rawValue = input.getText() == null ? "" : input.getText().toString().trim();
                    if (TextUtils.isEmpty(rawValue)) {
                        GoUtils.DisplayToast(this, getString(R.string.route_record_setting_invalid));
                        return;
                    }
                    try {
                        int value = Integer.parseInt(rawValue);
                        if (value < MIN_RECORD_INTERVAL_SECONDS) {
                            throw new IllegalArgumentException("interval too small");
                        }
                        recordIntervalSeconds = value;
                        getSharedPreferences(PREF_ROUTE_CREATE, MODE_PRIVATE)
                                .edit()
                                .putInt(PREF_ROUTE_RECORD_INTERVAL_SECONDS, recordIntervalSeconds)
                                .apply();
                        updateCreateModeUi();
                        GoUtils.DisplayToast(this, getString(R.string.route_record_setting_saved, recordIntervalSeconds));
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, getString(R.string.route_record_setting_invalid));
                    }
                })
                .setNegativeButton(R.string.route_share_cancel, null)
                .show();
    }

    private void switchToolMode(int newMode) {
        if (isRecordModeEnabled()) {
            return;
        }
        toolMode = newMode;
        closeRoutePromptVisible = false;
        if (toolMode != TOOL_MODE_EDIT) {
            selectedPointIndex = -1;
        }
        drawRoute(viewModel.getCurrentPoints());
        updateCreateModeUi();
    }

    private void showAutoAddPointsDialog() {
        if (isRecording) {
            return;
        }
        if (viewModel.getCurrentPoints().size() < 2) {
            GoUtils.DisplayToast(this, getString(R.string.route_auto_add_need_points));
            return;
        }
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText("10");
        input.setHint(getString(R.string.route_auto_add_hint));
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_auto_add_title)
                .setMessage(R.string.route_auto_add_message)
                .setView(input)
                .setPositiveButton(R.string.route_auto_add_confirm, (dialog, which) -> {
                    String rawValue = input.getText() == null ? "" : input.getText().toString().trim();
                    if (TextUtils.isEmpty(rawValue)) {
                        GoUtils.DisplayToast(this, getString(R.string.route_auto_add_invalid));
                        return;
                    }
                    try {
                        int count = Integer.parseInt(rawValue);
                        if (count <= 0) {
                            throw new IllegalArgumentException("count must be positive");
                        }
                        selectedPointIndex = -1;
                        closeRoutePromptVisible = false;
                        viewModel.setPoints(RouteEditUtils.insertEvenlySpacedPoints(viewModel.getCurrentPoints(), count));
                        GoUtils.DisplayToast(this, getString(R.string.route_auto_add_success, count));
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, getString(R.string.route_auto_add_invalid));
                    }
                })
                .setNegativeButton(R.string.route_share_cancel, null)
                .show();
    }

    private void showAutoReducePointsDialog() {
        if (isRecording) {
            return;
        }
        List<RoutePoint> currentPoints = viewModel.getCurrentPoints();
        if (currentPoints.size() < 3) {
            GoUtils.DisplayToast(this, getString(R.string.route_auto_reduce_need_points));
            return;
        }
        int keyPointCount = RouteEditUtils.countKeyRoutePoints(currentPoints);
        int maxRemoveCount = RouteEditUtils.countRemovableRoutePoints(currentPoints);
        if (maxRemoveCount <= 0) {
            GoUtils.DisplayToast(this, getString(R.string.route_auto_reduce_nothing));
            return;
        }
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(Math.min(10, maxRemoveCount)));
        input.setHint(getString(R.string.route_auto_reduce_hint));
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_auto_reduce_title)
                .setMessage(getString(
                        R.string.route_auto_reduce_message,
                        currentPoints.size(),
                        keyPointCount,
                        maxRemoveCount
                ))
                .setView(input)
                .setPositiveButton(R.string.route_auto_reduce_confirm, (dialog, which) -> {
                    String rawValue = input.getText() == null ? "" : input.getText().toString().trim();
                    if (TextUtils.isEmpty(rawValue)) {
                        GoUtils.DisplayToast(this, getString(R.string.route_auto_reduce_invalid, maxRemoveCount));
                        return;
                    }
                    try {
                        int count = Integer.parseInt(rawValue);
                        if (count <= 0 || count > maxRemoveCount) {
                            throw new IllegalArgumentException("count out of range");
                        }
                        selectedPointIndex = -1;
                        closeRoutePromptVisible = false;
                        viewModel.setPoints(RouteEditUtils.removeEvenlySpacedNonKeyPoints(currentPoints, count));
                        GoUtils.DisplayToast(this, getString(R.string.route_auto_reduce_success, count));
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, getString(R.string.route_auto_reduce_invalid, maxRemoveCount));
                    }
                })
                .setNegativeButton(R.string.route_share_cancel, null)
                .show();
    }

    private void showSmoothCornersDialog() {
        if (isRecording) {
            return;
        }
        if (viewModel.getCurrentPoints().size() < 3) {
            GoUtils.DisplayToast(this, getString(R.string.route_smooth_corner_need_points));
            return;
        }
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText("3.0");
        input.setHint(getString(R.string.route_smooth_corner_hint));
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_smooth_corner_title)
                .setMessage(R.string.route_smooth_corner_message)
                .setView(input)
                .setPositiveButton(R.string.route_smooth_corner_confirm, (dialog, which) -> {
                    String rawValue = input.getText() == null ? "" : input.getText().toString().trim();
                    if (TextUtils.isEmpty(rawValue)) {
                        GoUtils.DisplayToast(this, getString(R.string.route_smooth_corner_invalid));
                        return;
                    }
                    try {
                        double radiusMeters = Double.parseDouble(rawValue);
                        if (radiusMeters <= 0d) {
                            throw new IllegalArgumentException("radius must be positive");
                        }
                        List<RoutePoint> smoothedPoints = RouteEditUtils.smoothRightAngles(
                                viewModel.getCurrentPoints(),
                                radiusMeters,
                                85d,
                                95d
                        );
                        if (smoothedPoints.size() == viewModel.getCurrentPoints().size()) {
                            GoUtils.DisplayToast(this, getString(R.string.route_smooth_corner_no_match));
                            return;
                        }
                        selectedPointIndex = -1;
                        closeRoutePromptVisible = false;
                        viewModel.setPoints(smoothedPoints);
                        GoUtils.DisplayToast(this, getString(R.string.route_smooth_corner_success));
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, getString(R.string.route_smooth_corner_invalid));
                    }
                })
                .setNegativeButton(R.string.route_share_cancel, null)
                .show();
    }

    private void clearSelectedPoint() {
        selectedPointIndex = -1;
        closeRoutePromptVisible = false;
        updateCreateModeUi();
        drawRoute(viewModel.getCurrentPoints());
    }

    private void deleteSelectedPoint() {
        if (selectedPointIndex < 0) {
            return;
        }
        viewModel.removePointAt(selectedPointIndex);
        selectedPointIndex = -1;
        closeRoutePromptVisible = false;
        GoUtils.DisplayToast(this, getString(R.string.route_selected_point_deleted));
    }

    private void closeCurrentRoute() {
        if (!canCloseCurrentRoute()) {
            closeRoutePromptVisible = false;
            updateCreateModeUi();
            return;
        }
        List<RoutePoint> points = viewModel.getCurrentPoints();
        RoutePoint startPoint = points.get(0);
        viewModel.addPoint(new RoutePoint(
                startPoint.getBdLongitude(),
                startPoint.getBdLatitude(),
                startPoint.getWgsLongitude(),
                startPoint.getWgsLatitude(),
                startPoint.getAltitude()
        ));
        closeRoutePromptVisible = false;
        GoUtils.DisplayToast(this, getString(R.string.route_closed_success));
    }

    private boolean canCloseCurrentRoute() {
        List<RoutePoint> points = viewModel.getCurrentPoints();
        return points.size() >= 3 && !isRouteClosed(points);
    }

    private boolean isRouteClosed(@Nullable List<RoutePoint> points) {
        if (points == null || points.size() < 2) {
            return false;
        }
        RoutePoint firstPoint = points.get(0);
        RoutePoint lastPoint = points.get(points.size() - 1);
        return Math.abs(firstPoint.getBdLongitude() - lastPoint.getBdLongitude()) <= ROUTE_CLOSED_TOLERANCE
                && Math.abs(firstPoint.getBdLatitude() - lastPoint.getBdLatitude()) <= ROUTE_CLOSED_TOLERANCE;
    }

    private double sanitizeAltitude(@NonNull BDLocation bdLocation) {
        double altitude = bdLocation.getAltitude();
        if (Double.isNaN(altitude) || Double.isInfinite(altitude)) {
            return DEFAULT_ALTITUDE;
        }
        return altitude;
    }

    @NonNull
    private RoutePoint buildRoutePoint(@NonNull LatLng latLng, double altitude) {
        double[] wgsCoordinates = MapUtils.bd2wgs(latLng.longitude, latLng.latitude);
        return new RoutePoint(
                latLng.longitude,
                latLng.latitude,
                wgsCoordinates[0],
                wgsCoordinates[1],
                altitude
        );
    }

    @NonNull
    private RoutePoint buildMovedRoutePoint(@NonNull RoutePoint sourcePoint, @NonNull LatLng latLng) {
        return buildRoutePoint(latLng, sourcePoint.getAltitude());
    }

    private boolean insertRoutePointNearClick(@NonNull LatLng latLng) {
        List<RoutePoint> currentPoints = viewModel.getCurrentPoints();
        if (currentPoints.size() < 2) {
            return false;
        }
        RouteEditUtils.ProjectionResult projectionResult = RouteEditUtils.projectPointOntoRoute(
                currentPoints,
                buildRoutePoint(latLng, DEFAULT_ALTITUDE)
        );
        if (!projectionResult.isValid()
                || projectionResult.getDistanceToRouteMeters() > ROUTE_INSERT_MAX_DISTANCE_METERS) {
            return false;
        }
        viewModel.setPoints(RouteEditUtils.insertPointOnRoute(currentPoints, projectionResult));
        selectedPointIndex = projectionResult.getSegmentStartIndex() + 1;
        closeRoutePromptVisible = false;
        return true;
    }

    private void updateCreateModeUi() {
        boolean recordModeEnabled = isRecordModeEnabled();
        if (recordButton != null) {
            recordButton.setEnabled(recordModeEnabled || isRecording);
            recordButton.setText(isRecording ? R.string.route_record_stop_button : R.string.route_record_start_button);
        }
        if (clearButton != null) {
            clearButton.setEnabled(!isRecording);
        }
        if (saveButton != null) {
            saveButton.setEnabled(!isRecording);
        }
        if (shareButton != null) {
            shareButton.setEnabled(!isRecording);
        }
        if (closeRouteButton != null) {
            boolean showCloseRoute = !isRecording
                    && !recordModeEnabled
                    && toolMode == TOOL_MODE_DRAW
                    && closeRoutePromptVisible
                    && canCloseCurrentRoute();
            closeRouteButton.setVisibility(showCloseRoute ? View.VISIBLE : View.GONE);
        }

        boolean showToolbox = !recordModeEnabled && !isRecording && !toolboxCollapsed;
        if (toolboxPanel != null) {
            toolboxPanel.setVisibility(showToolbox ? View.VISIBLE : View.GONE);
        }
        if (toggleToolButton != null) {
            toggleToolButton.setImageResource(toolboxCollapsed ? R.drawable.ic_right : R.drawable.ic_left);
        }
        if (drawToolButton != null) {
            drawToolButton.setEnabled(toolMode != TOOL_MODE_DRAW);
        }
        if (editToolButton != null) {
            editToolButton.setEnabled(toolMode != TOOL_MODE_EDIT && !viewModel.getCurrentPoints().isEmpty());
        }
        if (autoAddToolButton != null) {
            autoAddToolButton.setEnabled(!isRecording && viewModel.getCurrentPoints().size() >= 2);
        }
        if (autoReduceToolButton != null) {
            autoReduceToolButton.setEnabled(!isRecording
                    && viewModel.getCurrentPoints().size() >= 3
                    && RouteEditUtils.countRemovableRoutePoints(viewModel.getCurrentPoints()) > 0);
        }
        if (smoothCornerToolButton != null) {
            smoothCornerToolButton.setEnabled(!isRecording && viewModel.getCurrentPoints().size() >= 3);
        }
        if (undoToolButton != null) {
            undoToolButton.setEnabled(!isRecording && viewModel.canUndo());
        }
        if (redoToolButton != null) {
            redoToolButton.setEnabled(!isRecording && viewModel.canRedo());
        }
        boolean showSelectedActions = showToolbox && toolMode == TOOL_MODE_EDIT && selectedPointIndex >= 0;
        if (keepToolButton != null) {
            keepToolButton.setVisibility(showSelectedActions ? View.VISIBLE : View.GONE);
        }
        if (deleteToolButton != null) {
            deleteToolButton.setVisibility(showSelectedActions ? View.VISIBLE : View.GONE);
        }
        if (selectedPointView != null) {
            if (showSelectedActions) {
                selectedPointView.setVisibility(View.VISIBLE);
                selectedPointView.setText(getString(R.string.route_selected_point_format, selectedPointIndex + 1));
            } else {
                selectedPointView.setVisibility(View.GONE);
            }
        }

        if (routeHintView == null) {
            return;
        }
        if (isRecording) {
            routeHintView.setText(getString(R.string.route_record_hint_active, viewModel.getCurrentPoints().size()));
        } else if (recordModeEnabled) {
            routeHintView.setText(getString(R.string.route_record_hint_idle, recordIntervalSeconds));
        } else if (toolMode == TOOL_MODE_EDIT) {
            routeHintView.setText(selectedPointIndex >= 0
                    ? getString(R.string.route_edit_selected_click_hint, selectedPointIndex + 1)
                    : getString(R.string.route_edit_mode_click_hint));
        } else if (closeRoutePromptVisible && canCloseCurrentRoute()) {
            routeHintView.setText(R.string.route_close_hint);
        } else if (!TextUtils.isEmpty(editingRouteId)) {
            routeHintView.setText(R.string.route_draw_mode_hint);
        } else {
            routeHintView.setText(R.string.route_draw_hint);
        }
    }

    private boolean isRecordModeEnabled() {
        return recordModeSwitch != null && recordModeSwitch.isChecked();
    }

    private void showSaveDialog() {
        if (isRecording) {
            return;
        }
        if (!viewModel.canSave()) {
            GoUtils.DisplayToast(this, "至少绘制两个点后才能保存");
            return;
        }

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(getCurrentRouteName());

        new AlertDialog.Builder(this)
                .setTitle(TextUtils.isEmpty(editingRouteId) ? "保存路线" : getString(R.string.route_edit_title))
                .setView(input)
                .setPositiveButton("保存", (dialog, which) -> {
                    String routeName = input.getText() == null ? "" : input.getText().toString().trim();
                    if (routeName.isEmpty()) {
                        GoUtils.DisplayToast(this, "路线名称不能为空");
                        return;
                    }
                    try {
                        RouteDefinition routeDefinition;
                        if (TextUtils.isEmpty(editingRouteId)) {
                            routeDefinition = viewModel.saveRoute(routeName);
                        } else {
                            routeDefinition = viewModel.updateRoute(
                                    editingRouteId,
                                    routeName,
                                    new ArrayList<>(viewModel.getCurrentPoints()),
                                    editingShareInfo
                            );
                            editingRouteId = routeDefinition.getId();
                        }
                        editingRouteName = routeDefinition.getName();
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
        if (isRecording) {
            return;
        }
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
                            showShareDetailDialog(getCurrentRouteName(), new ArrayList<>(viewModel.getCurrentPoints()));
                        } else {
                            showLocalRouteSelector(localRoutes);
                        }
                    })
                    .show();
            return;
        }

        if (hasCurrentPoints) {
            showShareDetailDialog(getCurrentRouteName(), new ArrayList<>(viewModel.getCurrentPoints()));
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
                RouteDefinition routeDefinition;
                if (TextUtils.isEmpty(editingRouteId)) {
                    routeDefinition = viewModel.saveRoute(localName, points, payload.toShareInfo(false));
                } else {
                    routeDefinition = viewModel.updateRoute(editingRouteId, localName, points, payload.toShareInfo(false));
                    editingRouteId = routeDefinition.getId();
                }
                editingRouteName = routeDefinition.getName();
                editingShareInfo = routeDefinition.getShareInfo();
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

    private String getCurrentRouteName() {
        return TextUtils.isEmpty(editingRouteName) ? buildDefaultRouteName() : editingRouteName;
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

    @NonNull
    private BitmapDescriptor getRouteVertexDescriptor() {
        if (routeVertexDescriptor == null) {
            routeVertexDescriptor = createCircleMarkerDescriptor(14, "#2E7D32");
        }
        return routeVertexDescriptor;
    }

    @NonNull
    private BitmapDescriptor getSelectedRouteVertexDescriptor() {
        if (routeVertexSelectedDescriptor == null) {
            routeVertexSelectedDescriptor = createCircleMarkerDescriptor(18, "#F57C00");
        }
        return routeVertexSelectedDescriptor;
    }

    @NonNull
    private BitmapDescriptor createCircleMarkerDescriptor(int sizeDp, @NonNull String fillColor) {
        int sizePx = Math.max(1, Math.round(getResources().getDisplayMetrics().density * sizeDp));
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float center = sizePx / 2f;
        float outerRadius = center;
        float innerRadius = Math.max(1f, outerRadius - (getResources().getDisplayMetrics().density * 2f));

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center, center, outerRadius, strokePaint);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor(fillColor));
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center, center, innerRadius, fillPaint);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void recycleMarkerDescriptors() {
        if (routeVertexDescriptor != null) {
            routeVertexDescriptor.recycle();
            routeVertexDescriptor = null;
        }
        if (routeVertexSelectedDescriptor != null) {
            routeVertexSelectedDescriptor.recycle();
            routeVertexSelectedDescriptor = null;
        }
    }
}

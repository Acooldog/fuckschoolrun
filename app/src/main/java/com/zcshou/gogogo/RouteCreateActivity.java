package com.acooldog.toolbox;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteShareInfo;
import com.acooldog.toolbox.route.domain.service.RouteEditUtils;
import com.acooldog.toolbox.route.presentation.RouteCreateViewModel;
import com.acooldog.toolbox.route.presentation.RouteModule;
import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.MapSearchResult;
import com.acooldog.toolbox.share.domain.model.SharedRoutePayload;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.acooldog.toolbox.utils.MapUtils;
import com.acooldog.toolbox.ui.CupertinoDialogs;
import com.acooldog.toolbox.ui.CupertinoRouteCreateOverlayView;
import com.acooldog.toolbox.ui.RouteCreateMapTransition;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteCreateActivity extends BaseActivity {
    public static final String EXTRA_EDIT_ROUTE_ID = "edit_route_id";

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

    private TextureMapView mapView;
    private BaiduMap baiduMap;
    private RouteCreateMapTransition mapTransition;
    private RouteCreateViewModel viewModel;
    private LocationClient locationClient;
    private LatLng currentLocation;
    private BDLocation lastLocationFix;
    private ExecutorService ioExecutor;
    private InternalAuthStore authStore;
    private String currentCity = "";
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
    private View toolboxContainer;
    private LinearLayout toolboxPanel;
    private View toolboxScrollView;
    private CupertinoRouteCreateOverlayView createOverlayView;
    private boolean satelliteMapSelected;
    private SwitchCompat recordModeSwitch;
    private boolean updatingRecordModeSwitch;
    private boolean isRecording;
    private boolean hasCenteredOnLocation;
    private int recordIntervalSeconds;
    private int toolMode = TOOL_MODE_DRAW;
    private int selectedPointIndex = -1;
    private boolean toolboxCollapsed = true;
    private boolean closeRoutePromptVisible;
    private boolean loginPromptVisible;
    private List<CupertinoRouteCreateOverlayView.SearchResult> searchResults = new ArrayList<>();
    private long searchRequestSequence;
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
        installCreateOverlay();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        CupertinoDockInstaller.reserveBottomSpace(findViewById(R.id.layout_bottom_control));
        CupertinoDockInstaller.install(this, CupertinoDockInstaller.TAB_DRAW);

        viewModel = new ViewModelProvider(this).get(RouteCreateViewModel.class);
        mapView = findViewById(R.id.map_create);
        baiduMap = mapView.getMap();
        ioExecutor = Executors.newSingleThreadExecutor();
        authStore = new InternalAuthStore(getApplicationContext());
        recordIntervalSeconds = getSharedPreferences(PREF_ROUTE_CREATE, MODE_PRIVATE)
                .getInt(PREF_ROUTE_RECORD_INTERVAL_SECONDS, DEFAULT_RECORD_INTERVAL_SECONDS);

        mapTransition = new RouteCreateMapTransition(
                (FrameLayout) findViewById(R.id.route_create_map_host),
                new RouteCreateMapTransition.Listener() {
                    @Override
                    public void prepareMap(BaiduMap map) {
                        configureLocationLayer(map);
                        MyLocationData locationData = baiduMap.getLocationData();
                        if (locationData != null) {
                            map.setMyLocationData(locationData);
                        }
                        drawRouteOverlays(map, viewModel.getCurrentPoints());
                    }

                    @Override
                    public void onMapChanged(TextureMapView view) {
                        mapView = view;
                        baiduMap = view.getMap();
                        initMap();
                    }

                    @Override
                    public void onLoadFailed() {
                        satelliteMapSelected = baiduMap.getMapType() == BaiduMap.MAP_TYPE_SATELLITE;
                        renderCreateOverlay();
                        GoUtils.DisplayToast(RouteCreateActivity.this, "地图加载超时，请重试");
                    }

                    @Override
                    public void onMapInteraction() {
                        createOverlayView.collapseSearch();
                    }
                });
        initMap();
        initButtons();
        initLocationClient();
        observeRoutePoints();
        loadEditingRouteIfNeeded();
        updateCreateModeUi();
    }

    private void installCreateOverlay() {
        createOverlayView = new CupertinoRouteCreateOverlayView(this);
        createOverlayView.setActions(new CupertinoRouteCreateOverlayView.Actions() {
            @Override
            public void onSearchOpened() {
                createOverlayView.post(() -> {
                    if (authStore == null || TextUtils.isEmpty(authStore.getToken())) {
                        showSearchLoginPrompt();
                    }
                });
            }

            @Override
            public void onSearchChanged(String query) {
                if (TextUtils.isEmpty(query)) {
                    searchRequestSequence++;
                    searchResults.clear();
                    renderCreateOverlay();
                } else {
                    requestPoiSearch(query);
                }
            }

            @Override
            public void onSearchSubmit(String query) {
                if (TextUtils.isEmpty(query)) {
                    searchRequestSequence++;
                    searchResults.clear();
                    renderCreateOverlay();
                } else {
                    requestPoiSearch(query);
                }
            }

            @Override
            public boolean onSearchResultSelected(double latitude, double longitude) {
                if (isRecording || isRecordModeEnabled()) {
                    return false;
                }
                searchRequestSequence++;
                LatLng target = new LatLng(latitude, longitude);
                closeRoutePromptVisible = false;
                selectedPointIndex = -1;
                searchResults.clear();
                viewModel.addPoint(buildRoutePoint(target, DEFAULT_ALTITUDE));
                baiduMap.animateMapStatus(
                        MapStatusUpdateFactory.newLatLngZoom(target, DEFAULT_ZOOM_LEVEL)
                );
                return true;
            }

            @Override
            public void onSelectMapType(boolean satellite) {
                selectMapType(satellite);
            }

            @Override
            public void onSave() {
                showSaveDialog();
            }

            @Override
            public void onClear() {
                if (!isRecording) {
                    selectedPointIndex = -1;
                    closeRoutePromptVisible = false;
                    viewModel.clear();
                    updateCreateModeUi();
                }
            }

            @Override
            public void onShare() {
                showShareDialog();
            }

            @Override
            public void onRecord() {
                toggleRecording();
            }

            @Override
            public void onRecordSettings() {
                showRecordSettingsDialog();
            }

            @Override
            public void onLocation() {
                moveToCurrentLocation();
            }

            @Override
            public void onCloseRoute() {
                closeCurrentRoute();
            }

            @Override
            public void onToggleToolbox() {
                toolboxCollapsed = !toolboxCollapsed;
                updateCreateModeUi();
            }

            @Override
            public void onDrawTool() {
                switchToolMode(TOOL_MODE_DRAW);
            }

            @Override
            public void onEditTool() {
                switchToolMode(TOOL_MODE_EDIT);
            }

            @Override
            public void onAutoAdd() {
                showAutoAddPointsDialog();
            }

            @Override
            public void onAutoReduce() {
                showAutoReducePointsDialog();
            }

            @Override
            public void onSmoothCorner() {
                showSmoothCornersDialog();
            }

            @Override
            public void onUndo() {
                selectedPointIndex = -1;
                closeRoutePromptVisible = false;
                viewModel.undo();
            }

            @Override
            public void onRedo() {
                selectedPointIndex = -1;
                closeRoutePromptVisible = false;
                viewModel.redo();
            }

            @Override
            public void onKeep() {
                clearSelectedPoint();
            }

            @Override
            public void onDelete() {
                deleteSelectedPoint();
            }

            @Override
            public void onRecordModeChanged(boolean enabled) {
                if (isRecording && !enabled) {
                    GoUtils.DisplayToast(RouteCreateActivity.this,
                            getString(R.string.route_record_exit_while_recording));
                    return;
                }
                updatingRecordModeSwitch = true;
                recordModeSwitch.setChecked(enabled);
                updatingRecordModeSwitch = false;
                if (enabled) {
                    switchToolMode(TOOL_MODE_DRAW);
                } else {
                    updateCreateModeUi();
                }
            }
        });
        ViewGroup content = findViewById(android.R.id.content);
        content.addView(
                createOverlayView,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapTransition.cancel();
        satelliteMapSelected = baiduMap.getMapType() == BaiduMap.MAP_TYPE_SATELLITE;
        renderCreateOverlay();
        createOverlayView.collapseSearch();
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopRecordScheduler();
        if (locationClient != null) {
            locationClient.stop();
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        mapTransition.cancel();
        if (baiduMap != null) {
            baiduMap.setMyLocationEnabled(false);
        }
        mapView.onDestroy();
        recycleMarkerDescriptors();
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
        configureLocationLayer(baiduMap);
        baiduMap.setOnMapTouchListener(event -> {
            mapTransition.onMapTouch(event);
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                createOverlayView.collapseSearch();
            }
        });
        baiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus status) {
                mapTransition.syncMapStatus(status);
            }

            @Override
            public void onMapStatusChangeStart(MapStatus status, int reason) {
                mapTransition.syncMapStatus(status);
            }

            @Override
            public void onMapStatusChange(MapStatus status) {
                mapTransition.syncMapStatus(status);
            }

            @Override
            public void onMapStatusChangeFinish(MapStatus status) {
                mapTransition.syncMapStatus(status);
            }
        });
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                createOverlayView.collapseSearch();
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
                createOverlayView.collapseSearch();
            }
        });
        baiduMap.setOnMarkerClickListener(this::handleMarkerClick);
    }

    private void configureLocationLayer(BaiduMap map) {
        map.setMyLocationEnabled(true);
        map.setMyLocationConfiguration(new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.NORMAL, true, null));
    }

    private void selectMapType(boolean satellite) {
        if (satelliteMapSelected == satellite) {
            return;
        }
        satelliteMapSelected = satellite;
        if (satellite == (baiduMap.getMapType() == BaiduMap.MAP_TYPE_SATELLITE)) {
            mapTransition.cancel();
        } else {
            mapTransition.start(mapView, satellite);
        }
        renderCreateOverlay();
    }

    private boolean handleMarkerClick(Marker marker) {
        createOverlayView.collapseSearch();
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
        toolboxContainer = findViewById(R.id.route_create_toolbox_container);
        toolboxPanel = findViewById(R.id.route_create_toolbox_panel);
        toolboxScrollView = findViewById(R.id.route_create_toolbox_scroll);
        ImageButton recordSettingsButton = findViewById(R.id.btn_create_record_settings);
        ImageButton locationButton = findViewById(R.id.btn_create_location);
        RadioGroup mapTypeGroup = findViewById(R.id.route_create_map_type_group);
        hideLegacyCreateControls();

        mapTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.route_create_map_normal) {
                selectMapType(false);
            } else if (checkedId == R.id.route_create_map_satellite) {
                selectMapType(true);
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
            animateToolbox(toolboxCollapsed);
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

    private void hideLegacyCreateControls() {
        int[] legacyIds = new int[]{
                R.id.route_search_container,
                R.id.route_create_map_type_group,
                R.id.route_create_hint,
                R.id.route_create_toolbox_container,
                R.id.route_create_selected_point,
                R.id.btn_create_close_route,
                R.id.btn_create_location,
                R.id.layout_bottom_control
        };
        for (int id : legacyIds) {
            View legacyView = findViewById(id);
            if (legacyView != null) {
                legacyView.setVisibility(View.GONE);
            }
        }
    }

    private void requestPoiSearch(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        long requestSequence = ++searchRequestSequence;
        String token = authStore == null ? "" : authStore.getToken();
        if (TextUtils.isEmpty(token)) {
            searchResults.clear();
            renderCreateOverlay();
            showSearchLoginPrompt();
            return;
        }
        ioExecutor.execute(() -> {
            try {
                List<MapSearchResult> results = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .searchMap(keyword, currentCity, token);
                runOnUiThread(() -> {
                    if (requestSequence != searchRequestSequence) {
                        return;
                    }
                    searchResults = toOverlaySearchResults(results);
                    renderCreateOverlay();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (requestSequence != searchRequestSequence) {
                        return;
                    }
                    searchResults.clear();
                    renderCreateOverlay();
                    if (!TextUtils.isEmpty(exception.getMessage())) {
                        GoUtils.DisplayToast(this, exception.getMessage());
                    }
                });
            }
        });
    }

    @NonNull
    private List<CupertinoRouteCreateOverlayView.SearchResult> toOverlaySearchResults(
            @Nullable List<MapSearchResult> results
    ) {
        List<CupertinoRouteCreateOverlayView.SearchResult> overlayResults = new ArrayList<>();
        if (results == null) {
            return overlayResults;
        }
        for (MapSearchResult result : results) {
            if (result == null) {
                continue;
            }
            overlayResults.add(new CupertinoRouteCreateOverlayView.SearchResult(
                    result.getName(),
                    result.getAddress(),
                    result.getLatitude(),
                    result.getLongitude()
            ));
        }
        return overlayResults;
    }

    private void showSearchLoginPrompt() {
        if (loginPromptVisible || isFinishing() || isDestroyed()) {
            return;
        }
        loginPromptVisible = true;
        CupertinoDialogs.showConfirm(
                this,
                getString(R.string.route_search_need_login_title),
                getString(R.string.route_search_need_login_backend),
                getString(R.string.route_search_login_cancel),
                getString(R.string.route_search_login_action),
                () -> loginPromptVisible = false,
                () -> {
                    startActivity(new android.content.Intent(this, InternalLoginActivity.class));
                    loginPromptVisible = false;
                }
        );
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
                MyLocationData locationData = new MyLocationData.Builder()
                        .accuracy(bdLocation.getRadius())
                        .direction(bdLocation.getDirection())
                        .latitude(bdLocation.getLatitude())
                        .longitude(bdLocation.getLongitude())
                        .build();
                baiduMap.setMyLocationData(locationData);
                BaiduMap incomingMap = mapTransition.getIncomingMap();
                if (incomingMap != null) {
                    incomingMap.setMyLocationData(locationData);
                }
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
        drawRouteOverlays(baiduMap, routePoints);
        BaiduMap incomingMap = mapTransition.getIncomingMap();
        if (incomingMap != null) {
            drawRouteOverlays(incomingMap, routePoints);
        }
        if (routePoints == null || routePoints.isEmpty()) {
            return;
        }
        RoutePoint target = selectedPointIndex >= 0 && selectedPointIndex < routePoints.size()
                ? routePoints.get(selectedPointIndex)
                : routePoints.get(routePoints.size() - 1);
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(
                new LatLng(target.getBdLatitude(), target.getBdLongitude())));
    }

    private void drawRouteOverlays(BaiduMap map, @Nullable List<RoutePoint> routePoints) {
        map.clear();
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
            map.addOverlay(polyline);
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
                map.addOverlay(markerOptions);
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
            map.addOverlay(startMarker);
            if (latLngs.size() > 1) {
                MarkerOptions endMarker = new MarkerOptions()
                        .position(lastPoint)
                        .icon(getSelectedRouteVertexDescriptor());
                android.os.Bundle endBundle = new android.os.Bundle();
                endBundle.putInt(MARKER_INDEX_KEY, latLngs.size() - 1);
                endBundle.putString(MARKER_TYPE_KEY, MARKER_TYPE_VERTEX);
                endMarker.extraInfo(endBundle);
                map.addOverlay(endMarker);
            }
        }

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
        CupertinoDialogs.showChoices(
                this,
                getString(R.string.route_record_stop_dialog_title),
                getString(R.string.route_record_stop_dialog_message),
                new String[]{
                        getString(R.string.route_record_stop_save_option),
                        getString(R.string.route_record_stop_share_option)
                },
                getString(R.string.route_share_cancel),
                which -> {
                    if (which == 0) {
                        showSaveDialog();
                    } else {
                        showShareDetailDialog(
                                getCurrentRouteName(),
                                new ArrayList<>(viewModel.getCurrentPoints())
                        );
                    }
                }
        );
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
        CupertinoDialogs.showInput(
                this,
                getString(R.string.route_record_setting_title),
                getString(R.string.route_record_setting_message),
                String.valueOf(recordIntervalSeconds),
                String.valueOf(DEFAULT_RECORD_INTERVAL_SECONDS),
                getString(R.string.route_share_cancel),
                getString(R.string.route_record_setting_confirm),
                true,
                rawInput -> {
                    String rawValue = rawInput == null ? "" : rawInput.trim();
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
                }
        );
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
        CupertinoDialogs.showInput(
                this,
                getString(R.string.route_auto_add_title),
                getString(R.string.route_auto_add_message),
                "10",
                getString(R.string.route_auto_add_hint),
                getString(R.string.route_share_cancel),
                getString(R.string.route_auto_add_confirm),
                rawInput -> {
                    String rawValue = rawInput == null ? "" : rawInput.trim();
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
                }
        );
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
        CupertinoDialogs.showInput(
                this,
                getString(R.string.route_auto_reduce_title),
                getString(
                        R.string.route_auto_reduce_message,
                        currentPoints.size(),
                        keyPointCount,
                        maxRemoveCount
                ),
                String.valueOf(Math.min(10, maxRemoveCount)),
                getString(R.string.route_auto_reduce_hint),
                getString(R.string.route_share_cancel),
                getString(R.string.route_auto_reduce_confirm),
                rawInput -> {
                    String rawValue = rawInput == null ? "" : rawInput.trim();
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
                }
        );
    }

    private void showSmoothCornersDialog() {
        if (isRecording) {
            return;
        }
        if (viewModel.getCurrentPoints().size() < 3) {
            GoUtils.DisplayToast(this, getString(R.string.route_smooth_corner_need_points));
            return;
        }
        CupertinoDialogs.showInput(
                this,
                getString(R.string.route_smooth_corner_title),
                getString(R.string.route_smooth_corner_message),
                "3.0",
                getString(R.string.route_smooth_corner_hint),
                getString(R.string.route_share_cancel),
                getString(R.string.route_smooth_corner_confirm),
                rawInput -> {
                    String rawValue = rawInput == null ? "" : rawInput.trim();
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
                }
        );
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
            renderCreateOverlay();
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
        renderCreateOverlay();
    }

    private void renderCreateOverlay() {
        if (createOverlayView == null || viewModel == null) {
            return;
        }
        boolean recordModeEnabled = isRecordModeEnabled();
        List<RoutePoint> points = viewModel.getCurrentPoints();
        createOverlayView.render(
                recordModeEnabled,
                isRecording,
                toolboxCollapsed,
                toolMode,
                selectedPointIndex,
                !isRecording && !points.isEmpty(),
                !isRecording && points.size() >= 2,
                !isRecording
                        && points.size() >= 3
                        && RouteEditUtils.countRemovableRoutePoints(points) > 0,
                !isRecording && points.size() >= 3,
                !isRecording && viewModel.canUndo(),
                !isRecording && viewModel.canRedo(),
                !isRecording
                        && !recordModeEnabled
                        && toolMode == TOOL_MODE_DRAW
                        && closeRoutePromptVisible
                        && canCloseCurrentRoute(),
                routeHintView == null ? "" : routeHintView.getText().toString(),
                satelliteMapSelected,
                searchResults
        );
    }

    private void animateToolbox(boolean collapsed) {
        if (toolboxContainer == null) {
            return;
        }
        int startWidth = toolboxContainer.getWidth();
        int targetWidth = Math.round(dp(collapsed ? 56f : 224f));
        if (startWidth <= 0) {
            startWidth = Math.round(dp(collapsed ? 224f : 56f));
        }
        ValueAnimator animator = ValueAnimator.ofInt(startWidth, targetWidth);
        animator.setDuration(240L);
        animator.addUpdateListener(valueAnimator -> {
            ViewGroup.LayoutParams params = toolboxContainer.getLayoutParams();
            params.width = (Integer) valueAnimator.getAnimatedValue();
            toolboxContainer.setLayoutParams(params);
        });
        animator.start();
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

        CupertinoDialogs.showInput(
                this,
                TextUtils.isEmpty(editingRouteId) ? "保存路线" : getString(R.string.route_edit_title),
                null,
                getCurrentRouteName(),
                getCurrentRouteName(),
                "取消",
                "保存",
                rawInput -> {
                    String routeName = rawInput == null ? "" : rawInput.trim();
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
                }
        );
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
            CupertinoDialogs.showChoices(
                    this,
                    getString(R.string.route_share_dialog_title),
                    null,
                    options,
                    getString(R.string.route_share_cancel),
                    which -> {
                        if (which == 0) {
                            showShareDetailDialog(getCurrentRouteName(), new ArrayList<>(viewModel.getCurrentPoints()));
                        } else {
                            showLocalRouteSelector(localRoutes);
                        }
                    }
            );
            return;
        }

        if (hasCurrentPoints) {
            showShareDetailDialog(getCurrentRouteName(), new ArrayList<>(viewModel.getCurrentPoints()));
            return;
        }

        showLocalRouteSelector(localRoutes);
    }

    private void showLocalRouteSelector(List<RouteDefinition> routes) {
        String[] routeNames = new String[routes.size()];
        for (int index = 0; index < routes.size(); index++) {
            routeNames[index] = routes.get(index).getName();
        }

        CupertinoDialogs.showChoices(
                this,
                getString(R.string.route_share_select_local_title),
                null,
                routeNames,
                getString(R.string.route_share_cancel),
                which -> {
                    RouteDefinition selected = routes.get(which);
                    showShareDetailDialog(selected.getName(), new ArrayList<>(selected.getPoints()));
                }
        );
    }

    private void showShareDetailDialog(String defaultName, List<RoutePoint> points) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_route_share, null);
        EditText nameInput = dialogView.findViewById(R.id.route_share_name_input);
        CheckBox privacyCheckBox = dialogView.findViewById(R.id.route_share_privacy_checkbox);
        nameInput.setText(defaultName);

        CupertinoDialogs.showCustom(
                this,
                getString(R.string.route_share_dialog_title),
                dialogView,
                getString(R.string.route_share_cancel),
                getString(R.string.route_share_confirm),
                () -> {
            String routeName = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(routeName)) {
                GoUtils.DisplayToast(this, getString(R.string.route_share_name_empty));
                return false;
            }
            uploadSharedRoute(routeName, privacyCheckBox.isChecked(), points);
            return true;
                },
                null
        );
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

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}

package com.acooldog.toolbox.service;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import com.elvishew.xlog.XLog;
import com.acooldog.toolbox.MainActivity;
import com.acooldog.toolbox.R;
import com.acooldog.toolbox.config.SimulationPrefsStore;
import com.acooldog.toolbox.location.MockLocationPermissionManager;
import com.acooldog.toolbox.location.NmeaInjector;

public class ServiceGo extends Service {
    public static final double DEFAULT_LAT = 36.667662;
    public static final double DEFAULT_LNG = 117.027707;
    public static final double DEFAULT_ALT = 55.0D;
    public static final float DEFAULT_BEA = 0.0F;

    private static final int HANDLER_MSG_ID = 0;
    private static final String SERVICE_GO_HANDLER_NAME = "ServiceGoLocation";
    private static final int SERVICE_GO_NOTE_ID = 1;
    private static final String SERVICE_GO_NOTE_CHANNEL_ID = "SERVICE_GO_NOTE";
    private static final String SERVICE_GO_NOTE_CHANNEL_NAME = "SERVICE_GO_NOTE";
    private static final long GPS_KEEPALIVE_MIN_TIME_MS = 200L;
    private static final long NETWORK_KEEPALIVE_MIN_TIME_MS = 800L;
    private static final float GPS_KEEPALIVE_MIN_DISTANCE_METERS = 0f;
    private static final float NETWORK_KEEPALIVE_MIN_DISTANCE_METERS = 0f;

    private double mCurLat = DEFAULT_LAT;
    private double mCurLng = DEFAULT_LNG;
    private double mCurAlt = DEFAULT_ALT;
    private float mCurBea = DEFAULT_BEA;
    private double mSpeed = 1.2;
    private boolean isStop = false;
    private int satelliteCount = SimulationPrefsStore.DEFAULT_NMEA_SATELLITE_COUNT;
    private int signalQuality = SimulationPrefsStore.DEFAULT_NMEA_SIGNAL_QUALITY;
    private float hdop = SimulationPrefsStore.DEFAULT_NMEA_HDOP;
    private long updateIntervalMillis = SimulationPrefsStore.DEFAULT_LOCATION_UPDATE_INTERVAL_MS;
    private boolean networkSimulationEnabled = SimulationPrefsStore.DEFAULT_NETWORK_SIMULATION_ENABLED;

    private LocationManager mLocManager;
    private SimulationPrefsStore simulationPrefsStore;
    private MockLocationPermissionManager permissionManager;
    private HandlerThread mLocHandlerThread;
    private Handler mLocHandler;
    private PowerManager.WakeLock mWakeLock;
    private boolean gpsKeepaliveRegistered;
    private boolean networkKeepaliveRegistered;

    private final LocationListener gpsKeepaliveListener = new SimpleLocationListener();
    private final LocationListener networkKeepaliveListener = new SimpleLocationListener();

    private final ServiceGoBinder mBinder = new ServiceGoBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        simulationPrefsStore = new SimulationPrefsStore(getApplicationContext());
        permissionManager = new MockLocationPermissionManager(this);
        reloadNmeaSimulationSettings();
        if (!permissionManager.isMockLocationEnabled(this)) {
            XLog.w("SERVICEGO: mock location permission is not enabled");
        }

        removeTestProviderNetwork();
        addTestProviderNetwork();

        removeTestProviderGPS();
        addTestProviderGPS();

        initGoLocation();
        initGpsKeepAlive();
        initWakeLock();
        initNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mCurLng = intent.getDoubleExtra(MainActivity.LNG_MSG_ID, DEFAULT_LNG);
            mCurLat = intent.getDoubleExtra(MainActivity.LAT_MSG_ID, DEFAULT_LAT);
            mCurAlt = intent.getDoubleExtra(MainActivity.ALT_MSG_ID, DEFAULT_ALT);
        }
        reloadNmeaSimulationSettings();
        stopGpsKeepAlive();
        initGpsKeepAlive();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            XLog.w("SERVICEGO: skip foreground-service self restart after task removal on Android 12+");
            super.onTaskRemoved(rootIntent);
            return;
        }
        Intent restartIntent = new Intent(getApplicationContext(), ServiceGo.class);
        restartIntent.putExtra(MainActivity.LNG_MSG_ID, mCurLng);
        restartIntent.putExtra(MainActivity.LAT_MSG_ID, mCurLat);
        restartIntent.putExtra(MainActivity.ALT_MSG_ID, mCurAlt);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        isStop = true;
        if (mLocHandler != null) {
            mLocHandler.removeMessages(HANDLER_MSG_ID);
        }
        if (mLocHandlerThread != null) {
            mLocHandlerThread.quit();
        }

        removeTestProviderNetwork();
        removeTestProviderGPS();
        stopGpsKeepAlive();

        stopForeground(STOP_FOREGROUND_REMOVE);
        releaseWakeLock();
        super.onDestroy();
    }

    private void initWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager == null) {
                return;
            }
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":MockLocationWakeLock");
            mWakeLock.setReferenceCounted(false);
            mWakeLock.acquire();
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - initWakeLock");
        }
    }

    private void releaseWakeLock() {
        try {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - releaseWakeLock");
        }
    }

    private void initNotification() {
        NotificationChannel channel = new NotificationChannel(
                SERVICE_GO_NOTE_CHANNEL_ID,
                SERVICE_GO_NOTE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        Intent clickIntent = new Intent(this, MainActivity.class);
        PendingIntent clickPI = PendingIntent.getActivity(this, 1, clickIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, SERVICE_GO_NOTE_CHANNEL_ID)
                .setChannelId(SERVICE_GO_NOTE_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_service_tips))
                .setContentIntent(clickPI)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(SERVICE_GO_NOTE_ID, notification);
    }

    private void initGoLocation() {
        mLocHandlerThread = new HandlerThread(SERVICE_GO_HANDLER_NAME, Process.THREAD_PRIORITY_FOREGROUND);
        mLocHandlerThread.start();
        mLocHandler = new Handler(mLocHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull android.os.Message msg) {
                if (isStop) {
                    return;
                }
                reloadNmeaSimulationSettings();
                injectEnhancedLocation(new LocationPoint(mCurLng, mCurLat, mCurAlt, (float) mSpeed, mCurBea));
                sendEmptyMessageDelayed(HANDLER_MSG_ID, updateIntervalMillis);
            }
        };

        mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
    }

    private void initGpsKeepAlive() {
        if (mLocManager == null || !hasLocationPermission()) {
            XLog.d("SERVICEGO: skip gps keepalive, location permission missing");
            return;
        }
        try {
            if (mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        GPS_KEEPALIVE_MIN_TIME_MS,
                        GPS_KEEPALIVE_MIN_DISTANCE_METERS,
                        gpsKeepaliveListener,
                        mLocHandlerThread.getLooper()
                );
                gpsKeepaliveRegistered = true;
                XLog.d("SERVICEGO: gps keepalive registered");
            }
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - register gps keepalive");
        }
        try {
            if (mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        NETWORK_KEEPALIVE_MIN_TIME_MS,
                        NETWORK_KEEPALIVE_MIN_DISTANCE_METERS,
                        networkKeepaliveListener,
                        mLocHandlerThread.getLooper()
                );
                networkKeepaliveRegistered = true;
                XLog.d("SERVICEGO: network keepalive registered");
            }
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - register network keepalive");
        }
    }

    private void stopGpsKeepAlive() {
        if (mLocManager == null) {
            return;
        }
        try {
            if (gpsKeepaliveRegistered) {
                mLocManager.removeUpdates(gpsKeepaliveListener);
                gpsKeepaliveRegistered = false;
            }
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - remove gps keepalive");
        }
        try {
            if (networkKeepaliveRegistered) {
                mLocManager.removeUpdates(networkKeepaliveListener);
                networkKeepaliveRegistered = false;
            }
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - remove network keepalive");
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void reloadNmeaSimulationSettings() {
        if (simulationPrefsStore == null) {
            return;
        }
        satelliteCount = simulationPrefsStore.getNmeaSatelliteCount();
        signalQuality = simulationPrefsStore.getNmeaSignalQuality();
        hdop = simulationPrefsStore.getNmeaHdop();
        updateIntervalMillis = simulationPrefsStore.getLocationUpdateIntervalMillis();
        networkSimulationEnabled = simulationPrefsStore.isNetworkSimulationEnabled();
    }

    private void removeTestProviderGPS() {
        try {
            if (mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                mLocManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - removeTestProviderGPS");
        }
    }

    @SuppressLint("wrongconstant")
    private void addTestProviderGPS() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mLocManager.addTestProvider(
                        LocationManager.GPS_PROVIDER,
                        false,
                        true,
                        false,
                        false,
                        true,
                        true,
                        true,
                        ProviderProperties.POWER_USAGE_HIGH,
                        ProviderProperties.ACCURACY_FINE
                );
            } else {
                mLocManager.addTestProvider(
                        LocationManager.GPS_PROVIDER,
                        false,
                        true,
                        false,
                        false,
                        true,
                        true,
                        true,
                        Criteria.POWER_HIGH,
                        Criteria.ACCURACY_FINE
                );
            }
            if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - addTestProviderGPS");
        }
    }

    private void injectEnhancedLocation(@NonNull LocationPoint point) {
        if (mLocManager == null) {
            return;
        }
        if (permissionManager != null && !permissionManager.isMockLocationEnabled(this)) {
            XLog.e("SERVICEGO: mock location permission is not enabled, skip location injection");
            return;
        }
        injectGPSLocation(point);
        if (isNetworkSimulationEnabled()) {
            injectNetworkLocation(point);
        }
    }

    private boolean isNetworkSimulationEnabled() {
        return networkSimulationEnabled;
    }

    private void injectGPSLocation(@NonNull LocationPoint point) {
        try {
            Location loc = createLocation(LocationManager.GPS_PROVIDER, point, hdop * 2.5f);
            Bundle bundle = new Bundle();
            bundle.putInt("satellites", satelliteCount);
            bundle.putInt("signalQuality", signalQuality);
            bundle.putFloat("hdop", hdop);
            bundle.putString("source", LocationManager.GPS_PROVIDER);
            loc.setExtras(bundle);
            NmeaInjector.attachGeneratedNmea(loc, satelliteCount, signalQuality, hdop);
            mLocManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
        } catch (SecurityException exception) {
            XLog.e("SERVICEGO: ERROR - injectGPSLocation, mock location permission denied");
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - injectGPSLocation");
        }
    }

    private void removeTestProviderNetwork() {
        try {
            if (mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false);
                mLocManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - removeTestProviderNetwork");
        }
    }

    @SuppressLint("wrongconstant")
    private void addTestProviderNetwork() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mLocManager.addTestProvider(
                        LocationManager.NETWORK_PROVIDER,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        ProviderProperties.POWER_USAGE_LOW,
                        ProviderProperties.ACCURACY_COARSE
                );
            } else {
                mLocManager.addTestProvider(
                        LocationManager.NETWORK_PROVIDER,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        Criteria.POWER_LOW,
                        Criteria.ACCURACY_COARSE
                );
            }
            if (!mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
            }
        } catch (SecurityException exception) {
            XLog.e("SERVICEGO: ERROR - addTestProviderNetwork");
        }
    }

    private void injectNetworkLocation(@NonNull LocationPoint point) {
        try {
            Location loc = createLocation(LocationManager.NETWORK_PROVIDER, point, Math.max(25f, hdop * 10f));
            Bundle bundle = new Bundle();
            bundle.putString("source", LocationManager.NETWORK_PROVIDER);
            loc.setExtras(bundle);
            mLocManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, loc);
        } catch (SecurityException exception) {
            XLog.e("SERVICEGO: ERROR - injectNetworkLocation, mock location permission denied");
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - injectNetworkLocation");
        }
    }

    @NonNull
    private Location createLocation(@NonNull String provider, @NonNull LocationPoint point, float accuracyMeters) {
        Location loc = new Location(provider);
        loc.setAccuracy(accuracyMeters);
        loc.setAltitude(point.altitude);
        loc.setBearing(point.bearing);
        loc.setLatitude(point.latitude);
        loc.setLongitude(point.longitude);
        loc.setTime(System.currentTimeMillis());
        loc.setSpeed(point.speed);
        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return loc;
    }

    public class ServiceGoBinder extends Binder {
        public void setPosition(double lng, double lat, double alt) {
            setMotion(lng, lat, alt, (float) mSpeed, mCurBea);
        }

        public void setMotion(double lng, double lat, double alt, float speed, float bearing) {
            mLocHandler.removeMessages(HANDLER_MSG_ID);
            reloadNmeaSimulationSettings();
            mCurLng = lng;
            mCurLat = lat;
            mCurAlt = alt;
            mSpeed = speed;
            mCurBea = bearing;
            mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
        }

        public void reloadSimulationSettings() {
            reloadNmeaSimulationSettings();
        }
    }

    private static final class SimpleLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            // Keep GNSS active without overriding the injected mock location.
        }
    }

    private static final class LocationPoint {
        private final double longitude;
        private final double latitude;
        private final double altitude;
        private final float speed;
        private final float bearing;

        private LocationPoint(double longitude, double latitude, double altitude, float speed, float bearing) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.altitude = altitude;
            this.speed = speed;
            this.bearing = bearing;
        }
    }
}

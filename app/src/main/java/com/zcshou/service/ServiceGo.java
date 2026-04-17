package com.zcshou.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.elvishew.xlog.XLog;
import com.zcshou.gogogo.MainActivity;
import com.zcshou.gogogo.R;

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

    private double mCurLat = DEFAULT_LAT;
    private double mCurLng = DEFAULT_LNG;
    private double mCurAlt = DEFAULT_ALT;
    private float mCurBea = DEFAULT_BEA;
    private double mSpeed = 1.2;
    private boolean isStop = false;

    private LocationManager mLocManager;
    private HandlerThread mLocHandlerThread;
    private Handler mLocHandler;

    private final ServiceGoBinder mBinder = new ServiceGoBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        removeTestProviderNetwork();
        addTestProviderNetwork();

        removeTestProviderGPS();
        addTestProviderGPS();

        initGoLocation();
        initNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mCurLng = intent.getDoubleExtra(MainActivity.LNG_MSG_ID, DEFAULT_LNG);
            mCurLat = intent.getDoubleExtra(MainActivity.LAT_MSG_ID, DEFAULT_LAT);
            mCurAlt = intent.getDoubleExtra(MainActivity.ALT_MSG_ID, DEFAULT_ALT);
        }
        return super.onStartCommand(intent, flags, startId);
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

        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
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
                try {
                    Thread.sleep(100);
                    if (!isStop) {
                        setLocationNetwork();
                        setLocationGPS();
                        sendEmptyMessage(HANDLER_MSG_ID);
                    }
                } catch (InterruptedException exception) {
                    XLog.e("SERVICEGO: ERROR - handleMessage");
                    Thread.currentThread().interrupt();
                }
            }
        };

        mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
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

    private void setLocationGPS() {
        try {
            Location loc = new Location(LocationManager.GPS_PROVIDER);
            loc.setAccuracy(Criteria.ACCURACY_FINE);
            loc.setAltitude(mCurAlt);
            loc.setBearing(mCurBea);
            loc.setLatitude(mCurLat);
            loc.setLongitude(mCurLng);
            loc.setTime(System.currentTimeMillis());
            loc.setSpeed((float) mSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            Bundle bundle = new Bundle();
            bundle.putInt("satellites", 7);
            loc.setExtras(bundle);
            mLocManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - setLocationGPS");
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

    private void setLocationNetwork() {
        try {
            Location loc = new Location(LocationManager.NETWORK_PROVIDER);
            loc.setAccuracy(Criteria.ACCURACY_COARSE);
            loc.setAltitude(mCurAlt);
            loc.setBearing(mCurBea);
            loc.setLatitude(mCurLat);
            loc.setLongitude(mCurLng);
            loc.setTime(System.currentTimeMillis());
            loc.setSpeed((float) mSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            mLocManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, loc);
        } catch (Exception exception) {
            XLog.e("SERVICEGO: ERROR - setLocationNetwork");
        }
    }

    public class ServiceGoBinder extends Binder {
        public void setPosition(double lng, double lat, double alt) {
            setMotion(lng, lat, alt, (float) mSpeed, mCurBea);
        }

        public void setMotion(double lng, double lat, double alt, float speed, float bearing) {
            mLocHandler.removeMessages(HANDLER_MSG_ID);
            mCurLng = lng;
            mCurLat = lat;
            mCurAlt = alt;
            mSpeed = speed;
            mCurBea = bearing;
            mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
        }
    }
}

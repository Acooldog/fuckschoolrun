package com.acooldog.toolbox.location;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.utils.GoUtils;

public final class MockLocationPermissionManager {
    private static final long CHECK_CACHE_MILLIS = 1000L;

    private final Context appContext;
    private long lastCheckMillis = -CHECK_CACHE_MILLIS;
    private boolean lastResult;

    public MockLocationPermissionManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    public boolean isMockLocationEnabled() {
        return isMockLocationEnabled(appContext);
    }

    public boolean isMockLocationEnabled(@NonNull Context context) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastCheckMillis < CHECK_CACHE_MILLIS) {
            return lastResult;
        }
        lastResult = GoUtils.isAllowMockLocation(context);
        lastCheckMillis = now;
        return lastResult;
    }
}

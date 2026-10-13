package com.acooldog.toolbox.ui;

public final class MineRoutePoint {
    private final double latitude;
    private final double longitude;

    public MineRoutePoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

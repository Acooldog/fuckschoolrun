package com.zcshou.gogogo.route.domain.model;

public final class RoutePoint {
    private final double bdLongitude;
    private final double bdLatitude;
    private final double wgsLongitude;
    private final double wgsLatitude;
    private final double altitude;

    public RoutePoint(double bdLongitude, double bdLatitude, double wgsLongitude, double wgsLatitude, double altitude) {
        this.bdLongitude = bdLongitude;
        this.bdLatitude = bdLatitude;
        this.wgsLongitude = wgsLongitude;
        this.wgsLatitude = wgsLatitude;
        this.altitude = altitude;
    }

    public double getBdLongitude() {
        return bdLongitude;
    }

    public double getBdLatitude() {
        return bdLatitude;
    }

    public double getWgsLongitude() {
        return wgsLongitude;
    }

    public double getWgsLatitude() {
        return wgsLatitude;
    }

    public double getAltitude() {
        return altitude;
    }
}

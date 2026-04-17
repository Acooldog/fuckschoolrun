package com.zcshou.gogogo.route.domain.model;

public final class SimulationFrame {
    private final RoutePoint point;
    private final float bearing;
    private final float speedMetersPerSecond;
    private final boolean finished;
    private final int completedLoops;

    public SimulationFrame(RoutePoint point, float bearing, float speedMetersPerSecond, boolean finished, int completedLoops) {
        this.point = point;
        this.bearing = bearing;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.finished = finished;
        this.completedLoops = completedLoops;
    }

    public RoutePoint getPoint() {
        return point;
    }

    public float getBearing() {
        return bearing;
    }

    public float getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getCompletedLoops() {
        return completedLoops;
    }
}
